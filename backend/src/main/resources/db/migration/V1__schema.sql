-- Core config tables: rulesets, role families and classification rules are
-- DATA, not code. Adding a new vertical (e.g. "software engineering") means
-- inserting rows here plus writing the matching test cases, not editing a
-- regex chain baked into a pipeline file.

CREATE TABLE ruleset (
    id          BIGSERIAL PRIMARY KEY,
    key         TEXT NOT NULL UNIQUE,
    label       TEXT NOT NULL,
    description TEXT
);

CREATE TABLE role_family (
    id          BIGSERIAL PRIMARY KEY,
    ruleset_id  BIGINT NOT NULL REFERENCES ruleset(id),
    key         TEXT NOT NULL,
    label       TEXT NOT NULL,
    -- coarse grouping the frontend rolls families up into, e.g. 'data',
    -- 'ai', 'excluded'. Purely presentational.
    group_name  TEXT NOT NULL,
    sort_order  INT NOT NULL,
    -- families with published = false still classify postings (so the
    -- rule ordering behaves correctly and drift can be measured) but are
    -- never surfaced in an aggregate or on the site. Mirrors the
    -- 'ai (other)' / 'entry programme' / 'invalid' treatment in the
    -- original pipeline.
    published   BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (ruleset_id, key)
);

CREATE TABLE classification_rule (
    id           BIGSERIAL PRIMARY KEY,
    ruleset_id   BIGINT NOT NULL REFERENCES ruleset(id),
    -- evaluated in ascending order, first match wins. Most specific
    -- rules must sort before vague catch-alls, exactly as the comments
    -- in 03_silver_clean.py warn.
    priority     INT NOT NULL,
    family_key   TEXT NOT NULL,
    pattern      TEXT NOT NULL,
    description  TEXT,
    UNIQUE (ruleset_id, priority)
);

CREATE TABLE skill_definition (
    id               BIGSERIAL PRIMARY KEY,
    key              TEXT NOT NULL UNIQUE,
    category         TEXT NOT NULL,
    label            TEXT NOT NULL,
    -- a skill-wide guard applied only when at least one alias matched,
    -- e.g. the bare "R" alias is only accepted when the surrounding text
    -- also looks like a technology list. NULL means no guard needed.
    context_pattern  TEXT
);

CREATE TABLE skill_alias (
    id            BIGSERIAL PRIMARY KEY,
    skill_id      BIGINT NOT NULL REFERENCES skill_definition(id),
    pattern       TEXT NOT NULL
);

-- Bronze: every API pull, verbatim, append-only. Nothing here is ever
-- updated or deleted.
CREATE TABLE raw_posting (
    id                   BIGSERIAL PRIMARY KEY,
    adzuna_id            TEXT NOT NULL,
    country              TEXT NOT NULL,
    query_role           TEXT NOT NULL,
    query_page           INT,
    pull_date            DATE NOT NULL,
    title                TEXT,
    company              TEXT,
    city_raw             TEXT,
    area2                TEXT,
    area3                TEXT,
    description          TEXT,
    redirect_url         TEXT,
    salary_min           NUMERIC,
    salary_max           NUMERIC,
    salary_is_predicted  BOOLEAN,
    contract_type        TEXT,
    contract_time        TEXT,
    category             TEXT,
    created              TIMESTAMP,
    ingest_ts            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_raw_posting_pull_date ON raw_posting (pull_date);
CREATE INDEX idx_raw_posting_country_role ON raw_posting (country, query_role);

-- Silver: one row per deduplicated posting, per ruleset, for the most
-- recent snapshot. status carries what the original project split across
-- three separate tables (postings / excluded / quarantine); a status
-- column is simpler to query generically across rulesets and keeps the
-- audit trail (why was this row dropped) in one place.
CREATE TABLE posting (
    id                   BIGSERIAL PRIMARY KEY,
    ruleset_id           BIGINT NOT NULL REFERENCES ruleset(id),
    posting_hash         TEXT NOT NULL,
    raw_posting_id        BIGINT NOT NULL REFERENCES raw_posting(id),
    adzuna_id            TEXT NOT NULL,
    country              TEXT NOT NULL,
    title_raw            TEXT NOT NULL,
    title_norm           TEXT NOT NULL,
    role_family_key      TEXT NOT NULL,
    role_group           TEXT NOT NULL,
    seniority            TEXT NOT NULL,
    gendered_tag         BOOLEAN NOT NULL,
    company              TEXT,
    company_norm         TEXT,
    is_agency            BOOLEAN NOT NULL,
    city                 TEXT,
    city_raw             TEXT,
    region               TEXT,
    category             TEXT,
    language             TEXT NOT NULL,
    redirect_url         TEXT,
    description          TEXT,
    desc_chars           INT,
    desc_truncated       BOOLEAN,
    salary_min           NUMERIC,
    salary_max           NUMERIC,
    salary_is_predicted  BOOLEAN,
    contract_type        TEXT,
    contract_time        TEXT,
    created_date         DATE,
    age_days             INT,
    snapshot_date        DATE NOT NULL,
    query_role           TEXT,
    -- KEPT: published family, passed quality gate, use in aggregates.
    -- EXCLUDED: passed quality gate but family not published (scope
    --           filter), auditable but not reported.
    -- QUARANTINED: failed a quality rule, never classified as data.
    status               TEXT NOT NULL,
    exclusion_reason     TEXT,
    -- Same as posting_hash for KEPT/EXCLUDED rows, NULL for QUARANTINED.
    -- KEPT/EXCLUDED are deduplicated by posting_hash before being persisted
    -- (see PostingClassificationService#dedupKeepingEarliest), so the
    -- uniqueness guarantee only needs to hold for them. QUARANTINED rows are
    -- deliberately never deduplicated - two distinct raw postings that hash
    -- the same (e.g. an identical listing matched by more than one query
    -- role) and both fail the quality gate must be able to coexist as
    -- separate audit rows, so their dedup_hash is NULL and a standard SQL
    -- unique constraint treats NULLs as distinct from one another.
    dedup_hash           TEXT,
    ingest_ts            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE (ruleset_id, dedup_hash, snapshot_date)
);

CREATE INDEX idx_posting_snapshot ON posting (ruleset_id, snapshot_date, status);
CREATE INDEX idx_posting_family ON posting (ruleset_id, role_family_key);

CREATE TABLE posting_skill (
    id              BIGSERIAL PRIMARY KEY,
    posting_id      BIGINT NOT NULL REFERENCES posting(id) ON DELETE CASCADE,
    skill_key       TEXT NOT NULL,
    skill_category  TEXT NOT NULL
);

CREATE INDEX idx_posting_skill_posting ON posting_skill (posting_id);
CREATE INDEX idx_posting_skill_skill ON posting_skill (skill_key);

-- Gold-equivalent history: everything else in gold is recomputed from
-- `posting` at export time (it is cheap at this data volume), but the
-- trend line needs a true append-only record across snapshots.
CREATE TABLE history_metric (
    id             BIGSERIAL PRIMARY KEY,
    ruleset_id     BIGINT NOT NULL REFERENCES ruleset(id),
    snapshot_date  DATE NOT NULL,
    metric         TEXT NOT NULL,
    dimension      TEXT NOT NULL,
    value          DOUBLE PRECISION NOT NULL,
    UNIQUE (ruleset_id, snapshot_date, metric, dimension)
);
