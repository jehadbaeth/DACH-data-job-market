# What's up with the software-dev job market in Austria/Germany/Switzerland?

**[Open the live tracker](https://carl-latitudee5570.tail5b4e29.ts.net/)** · updated every Monday at 06:00

A weekly snapshot of the DACH software development job market: live postings from Germany, Austria and Switzerland, classified into ten engineering role families and scanned against a language/framework/devops/cloud/database skill dictionary. A Spring Boot backend queries the Adzuna API, classifies and aggregates every posting in Postgres, and exports static JSON that a single-file Chart.js dashboard (`docs/index.html`) reads directly — no server needed to view it.

This project started as a fork of [Christoph Ruckensteiner](https://www.linkedin.com/in/cruckensteiner112)'s original data-job-market dashboard, and keeps its UI layout and overall concept. Everything underneath it has since been rebuilt: the original was a Python/Databricks (Delta Lake, PySpark, Unity Catalog) pipeline tracking data/AI roles. This version is a Java/Spring Boot/Postgres pipeline, and it tracks a different vertical entirely — software engineering roles, not data/AI roles.

## What you can do with it

- **See the shape of the market** — volumes by role, city and country, how long postings stay open, seniority mix, tool mentions, English-language share, salary disclosure.
- **Read the postings behind any number.** Every chart is a count of real adverts, and those adverts are browsable: search by title or employer, filter by role family, country, city and seniority, sort by newest or longest-open. Titles link out to the aggregator.
- **Drill down from a chart.** Click a city on the map or a cell in the country/role table and the postings list below filters to it.
- **Compare two cities head to head**, or one role's tool demand against the DACH-wide baseline.
- **Watch it move.** Every run appends to the history table rather than overwriting it, so posting counts and average ages gain a time dimension.

## Role families

Ten published families, in the priority order the classifier checks them (first match on the job title wins):

1. Security Engineer
2. Embedded / Firmware
3. Game Developer
4. QA / Test Automation
5. DevOps / SRE
6. Cloud / Platform Engineer
7. Mobile Developer
8. Frontend Developer
9. Full Stack Developer
10. Backend Developer — the catch-all for generic "software/application developer" titles that name a language but no more specific track

Three more families exist but are excluded from every chart: `invalid` (parse artefacts, speculative applications), `entry programme` (Ausbildung, Werkstudent, Praktikum, Trainee — these distort posting-age figures if left in), and `other` (unclassified).

Judgement calls worth knowing about: a title naming both "full stack" and a backend language is counted as full-stack, checked before the backend catch-all; "cloud engineer" and "DevOps/SRE" overlap in practice even though they're separate rules, and reasonable people would draw that line differently.

## Skill dictionary

Skills are grouped into five categories — language, framework, devops, cloud, database — and matched against posting titles/descriptions via regex, not an LLM, so every match points at a specific string and the counts are reproducible. The dictionary currently covers common languages (Java, Python, JavaScript/TypeScript, C#, Go, Rust, Kotlin, Swift, PHP, Ruby, C/C++), frameworks (Spring, React, Angular, Vue, .NET, Django, Flask, Node.js, Laravel, Rails), devops tooling (Docker, Kubernetes, Terraform, Git, CI/CD), cloud platforms (AWS, Azure, GCP) and databases (PostgreSQL, MySQL, SQL Server, MongoDB, Redis).

It does not yet cover security-specific tooling (SIEM, OWASP, pentest frameworks, IAM, compliance standards), which is why the Security Engineer family shows almost no skill data — there's very little in the current dictionary for those postings to match against.

## Updating the rules and the list of monitored positions

Everything that defines the classification behaviour lives in Postgres, seeded by Flyway migrations under `backend/src/main/resources/db/migration/`. **Never edit an already-applied migration** — Flyway checksums it on every boot and refuses to start if the file changed underneath it. Always add a new migration file instead (`V5__...sql`, `V6__...sql`, and so on). This makes every change additive: nothing is ever wiped, and rolling forward is just a redeploy.

**To change which Adzuna search terms get pulled in:** edit the `dachjobs.adzuna.roles` list in `backend/src/main/resources/application.yml`. No code change, no migration — just a redeploy. This only decides what gets *ingested*; it does not affect how postings already ingested get classified.

**To add, remove or re-prioritize a role family or its classification regex:** write a new migration that inserts into `role_family` and `classification_rule` for the `software-dev` ruleset (see `V4__software_dev_ruleset.sql` for the exact shape — `priority` controls check order, lower runs first, first match wins). To retire a family without breaking history, mark it `published = false` rather than deleting the row; existing postings keep their classification either way.

**To add or change a skill:** insert into `skill_definition` (key, category, label) and `skill_alias` (regex patterns that map to that skill) in a new migration, scoped to the `software-dev` ruleset's `ruleset_id`. Skills are ruleset-scoped by design (`skill_definition.ruleset_id` + a composite unique constraint on `(ruleset_id, key)`), so adding a term here never touches any other vertical's dictionary.

**To add a whole new vertical** (a different job-market slice entirely): insert a new row into `ruleset`, then role families, classification rules and a skill dictionary scoped to its id, the same way `V4__software_dev_ruleset.sql` did for software-dev. The pipeline loops over every row in `ruleset` automatically (`PipelineRunner.run()`), so a new ruleset starts getting classified and exported the moment its migration lands — no other code changes needed.

## Architecture

```
Adzuna API (de, at, ch)
  -> raw_posting (Postgres, one row per pull per posting)
  -> classification (title-regex rules, per ruleset, priority order)
  -> posting + posting_skill (Postgres)
  -> gold aggregation (JPA/SQL queries over posting + posting_skill)
  -> JSON export to docs/data/
  -> docs/index.html (static Chart.js dashboard, reads the JSON directly)
```

The whole pipeline (ingest → classify → aggregate → export) runs weekly via a Spring `@Scheduled` job, and can be triggered manually:

```bash
curl -X POST "http://localhost:8080/api/pipeline/run?skipIngest=false"
```

Or per stage, per ruleset:

```bash
curl -X POST http://localhost:8080/api/pipeline/classify/software-dev
curl -X POST http://localhost:8080/api/pipeline/export/software-dev
```

**Browsing the postings.** `docs/data/postings.json` backs the searchable list. It carries title, employer, city, role family, seniority, age and the aggregator's redirect URL — not descriptions (not ours to redistribute, and mostly truncated anyway) and not salary.

**Deduplication.** Postings are hashed on title, company, city and the first 200 characters of the description; the earliest listing wins.

**Posting age.** Days between the API's `created` date and the snapshot date, for postings still returned as live.

## Limitations

- **`created` is the aggregator's date**, possibly when it indexed the posting rather than when the employer published it.
- **Descriptions are capped at 500 characters**, and most are truncated. Skill counts measure *mentioned in the title or opening paragraph* — a floor, not a requirement rate. The window is identical for every posting, so comparing tools holds; reading one percentage as a real requirement rate does not.
- **A posting in the browser is not necessarily open.** It was live at the last refresh. Nothing re-checks whether it has since been filled or withdrawn.
- **Roles and seniority are inferred from titles**, so both carry classification error.
- **A share of records are rejected** by quality rules each run and quarantined rather than silently dropped — see the "This pipeline's own error rates" panel on the site for the current rate.
- **One aggregator is not the whole market**, and its coverage is not equally deep in all three countries. Cross-country comparisons should be read as indicative.

## Running it yourself

1. Get an `app_id` and `app_key` from `developer.adzuna.com`.
2. Copy `.env.example` to `.env` and fill in `DB_PASSWORD`, `ADZUNA_APP_ID`, `ADZUNA_APP_KEY` (and optionally `TZ`, `WEB_PORT`). `.env` is gitignored — never commit it.
3. `docker compose up -d --build` — this starts Postgres, the Spring Boot app, and an nginx container (`web`) that serves `docs/` and proxies `/api/` to the app, all behind a single port (`WEB_PORT`, default `8087`). Flyway applies all migrations automatically on boot.
4. Trigger a run (see the `curl` commands above), or wait for the Monday 03:00 scheduled job.
5. Open `http://<host>:<WEB_PORT>/` — the dashboard fetches everything it needs from `docs/data/*.json` produced by the run. To expose it beyond your LAN, point a reverse proxy or Tailscale Funnel at that port.

Backend tests:

```bash
cd backend && ./gradlew test
```

## Tech stack

Spring Boot 3.3.5, Java 17, Gradle, Spring Data JPA, Flyway, PostgreSQL 16, Docker Compose, Chart.js.

## Licence

MIT. The underlying job data belongs to Adzuna and its source boards.
