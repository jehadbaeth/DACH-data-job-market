-- Adds the software-dev vertical without touching any already-applied
-- migration (editing V1/V2 in place breaks Flyway checksum validation on any
-- environment that already ran them - see V1/V2 for the original data-ai
-- rows, left in place here, unused but harmless).
--
-- skill_definition becomes ruleset-scoped: a vertical's skill dictionary
-- (e.g. software languages/frameworks) must not be scanned against another
-- vertical's postings. Existing rows (data-ai's dictionary) are backfilled
-- onto the data-ai ruleset so the column can be NOT NULL.

ALTER TABLE skill_definition ADD COLUMN ruleset_id BIGINT REFERENCES ruleset(id);

UPDATE skill_definition
SET ruleset_id = (SELECT id FROM ruleset WHERE key = 'data-ai')
WHERE ruleset_id IS NULL;

ALTER TABLE skill_definition ALTER COLUMN ruleset_id SET NOT NULL;
-- the old single-column UNIQUE(key) constraint from V1 was dropped by the
-- preceding V3 Java migration (its Postgres/H2-generated name isn't
-- guessable in plain SQL, see V3__DropSkillDefinitionKeyUniqueConstraint).
ALTER TABLE skill_definition ADD CONSTRAINT skill_definition_ruleset_key_key UNIQUE (ruleset_id, key);

INSERT INTO ruleset (key, label, description) VALUES
    ('software-dev', 'DACH Software Development jobs',
     'Software engineering role families and language/framework skill demand.');

-- role families ---------------------------------------------------------

INSERT INTO role_family (ruleset_id, key, label, group_name, sort_order, published)
SELECT r.id, v.key, v.label, v.group_name, v.sort_order, v.published
FROM ruleset r, (VALUES
    ('invalid',            'Invalid / junk',          'excluded', 0,  false),
    ('entry programme',    'Entry programme',         'excluded', 1,  false),
    ('other',              'Other (unclassified)',    'excluded', 99, false),

    ('backend',            'Backend Developer',       'software', 10, true),
    ('frontend',           'Frontend Developer',      'software', 11, true),
    ('fullstack',          'Full Stack Developer',    'software', 12, true),
    ('mobile',             'Mobile Developer',        'software', 13, true),
    ('devops / sre',       'DevOps / SRE',             'software', 14, true),
    ('qa / test',          'QA / Test Automation',    'software', 15, true),
    ('embedded',           'Embedded / Firmware',      'software', 16, true),
    ('security engineer',  'Security Engineer',        'software', 17, true),
    ('gamedev',            'Game Developer',           'software', 18, true),
    ('cloud / platform',   'Cloud / Platform Engineer', 'software', 19, true)
) AS v(key, label, group_name, sort_order, published)
WHERE r.key = 'software-dev';

-- classification rules, evaluated in ascending priority, first match wins.
-- patterns are Java regex, matched against the lowercased, umlaut-folded
-- title (TitleNormalizer), same convention as the data-ai ruleset.

INSERT INTO classification_rule (ruleset_id, priority, family_key, pattern, description)
SELECT r.id, v.priority, v.family_key, v.pattern, v.description
FROM ruleset r, (VALUES
    (0, 'invalid',
     '^initiativbewerbung|initiativbewerbung|talentpool|^deine aufgaben|^zum \d|^wachstum durch',
     'Parse artefacts and speculative applications, not real postings.'),

    (1, 'entry programme',
     '\bausbildung\b|duales? studium|dualer bachelor|\bwerkstudent|werkstudierend|\bpraktikum\b|\bpraktikant|\btrainee\b|traineeprogramm|bachelor thesis|master student|studienkolleg|bachelor of science|\bdhbw\b|\bhwr\b|b\.\s?a\.\s+in\b|^b\.\s?a\.|abschlussarbeit',
     'Education programmes; excluded because they distort posting-age figures.'),

    (2, 'security engineer',
     'security engineer|application security|\bappsec\b|security architect|penetration tester|\bpentester\b|\bpentest\b|it[\s\-]security|cyber ?security|\bciso\b|security consultant',
     'Security-specific track, checked before generic backend/devops.'),

    (3, 'embedded',
     'embedded (software|systems?)\s*(engineer|developer)?|firmware (engineer|developer)|\bfpga\b|embedded c\+\+?|mikrocontroller|microcontroller',
     'Firmware/hardware-adjacent, distinct hiring track from web backend.'),

    (4, 'gamedev',
     'game (developer|programmer|engineer)|gameplay (developer|programmer|engineer)|\bunity3?d?\s+(developer|engineer)|\bunreal\s+(developer|engineer)|spieleentwickl',
     null),

    (5, 'qa / test',
     '\bqa\s+(engineer|automation|analyst)|test automation|automation (engineer|tester)|software (test|qa) engineer|\bsdet\b|testingenieur|qualitaetssicherung',
     null),

    (6, 'devops / sre',
     '\bdevops\b|site reliability|\bsre\b|platform engineer|infrastructure engineer|release engineer|systems? engineer.*(ci/cd|kubernetes|automation)',
     'Checked before cloud/platform: devops/sre is the more specific title when both appear.'),

    (7, 'cloud / platform',
     'cloud (engineer|architect|developer)|cloud[\s\-]native (engineer|developer)|\baws\b.*(engineer|architect)|\bazure\b.*(engineer|architect)|\bgcp\b.*(engineer|architect)',
     null),

    (8, 'mobile',
     '\bios\s+(developer|engineer)|\bandroid\s+(developer|engineer)|mobile (developer|engineer|app)|swift developer|kotlin developer|\bflutter\b|react native',
     null),

    (9, 'frontend',
     'frontend|front[\s\-]end|\bui\s+(developer|engineer)|web (developer|engineer)\s*\(?frontend|javascript (developer|engineer)(?!.*backend)|react developer|angular developer|vue(\.js)? developer',
     null),

    (10, 'fullstack',
     'full[\s\-]?stack',
     'Checked before backend so an explicit fullstack title is not swallowed by the generic backend regex.'),

    (11, 'backend',
     'backend|back[\s\-]end|server[\s\-]side (developer|engineer)|java (developer|engineer)|\.net (developer|engineer)|python (developer|engineer)|golang? (developer|engineer)|\bphp\s+(developer|engineer)\b|\bruby\b.*(developer|engineer)|c\+\+\s+(developer|engineer)|software (developer|engineer)|softwareentwickl|\bapplication developer\b',
     'Catch-all for generic "software/application developer" titles that name a language but no more specific track.')
) AS v(priority, family_key, pattern, description)
WHERE r.key = 'software-dev';

-- skill dictionary --------------------------------------------------------

INSERT INTO skill_definition (ruleset_id, key, category, label, context_pattern)
SELECT r.id, v.key, v.category, v.label, v.context_pattern
FROM ruleset r, (VALUES
    ('java',       'language', 'Java', null),
    ('python',     'language', 'Python', null),
    ('javascript', 'language', 'JavaScript', null),
    ('typescript', 'language', 'TypeScript', null),
    ('csharp',     'language', 'C#', null),
    ('go',         'language', 'Go', null),
    ('rust',       'language', 'Rust', null),
    ('kotlin',     'language', 'Kotlin', null),
    ('swift',      'language', 'Swift', null),
    ('php',        'language', 'PHP', null),
    ('ruby',       'language', 'Ruby', null),
    ('c_cpp',      'language', 'C / C++', null),

    ('spring',     'framework', 'Spring / Spring Boot', null),
    ('react',      'framework', 'React', null),
    ('angular',    'framework', 'Angular', null),
    ('vue',        'framework', 'Vue.js', null),
    ('dotnet',     'framework', '.NET', null),
    ('django',     'framework', 'Django', null),
    ('flask',      'framework', 'Flask', null),
    ('nodejs',     'framework', 'Node.js', null),
    ('laravel',    'framework', 'Laravel', null),
    ('rails',      'framework', 'Ruby on Rails', null),

    ('docker',      'devops', 'Docker', null),
    ('kubernetes',  'devops', 'Kubernetes', null),
    ('terraform',   'devops', 'Terraform', null),
    ('git',         'devops', 'Git', null),
    ('cicd',        'devops', 'CI/CD', null),

    ('aws',    'cloud', 'AWS', null),
    ('azure',  'cloud', 'Azure', null),
    ('gcp',    'cloud', 'GCP', null),

    ('postgres', 'database', 'PostgreSQL', null),
    ('mysql',    'database', 'MySQL', null),
    ('mssql',    'database', 'SQL Server', null),
    ('mongodb',  'database', 'MongoDB', null),
    ('redis',    'database', 'Redis', null)
) AS v(key, category, label, context_pattern)
WHERE r.key = 'software-dev';

INSERT INTO skill_alias (skill_id, pattern)
SELECT s.id, a.pattern FROM skill_definition s, (VALUES
    ('java', '\bjava\b(?!script)'),
    ('python', '\bpython\b'),
    ('javascript', '\bjavascript\b'), ('javascript', '\bjs\b'),
    ('typescript', '\btypescript\b'), ('typescript', '\bts\b'),
    ('csharp', '\bc#\b'), ('csharp', '\bc[\s\-]?sharp\b'),
    ('go', '\bgolang\b'), ('go', '\bgo(?=[\s,/.;])'),
    ('rust', '\brust\b'),
    ('kotlin', '\bkotlin\b'),
    ('swift', '\bswift\b'),
    ('php', '\bphp\b'),
    ('ruby', '\bruby\b'),
    ('c_cpp', '\bc\+\+\b'), ('c_cpp', '\bc\b(?!#)'),

    ('spring', '\bspring boot\b'), ('spring', '\bspring\b'),
    ('react', '\breact\.?js\b'), ('react', '\breact\b'),
    ('angular', '\bangular\b'),
    ('vue', '\bvue\.?js\b'), ('vue', '\bvue\b'),
    ('dotnet', '\.net\b'), ('dotnet', '\bdotnet\b'),
    ('django', '\bdjango\b'),
    ('flask', '\bflask\b'),
    ('nodejs', '\bnode\.?js\b'),
    ('laravel', '\blaravel\b'),
    ('rails', '\bruby on rails\b'), ('rails', '\brails\b'),

    ('docker', '\bdocker\b'),
    ('kubernetes', '\bkubernetes\b'), ('kubernetes', '\bk8s\b'),
    ('terraform', '\bterraform\b'),
    ('git', '\bgithub\b'), ('git', '\bgitlab\b'), ('git', '\bgit\b'),
    ('cicd', '\bci/cd\b'), ('cicd', '\bjenkins\b'),

    ('aws', '\baws\b'), ('aws', '\bamazon web services\b'),
    ('azure', '\bazure\b'),
    ('gcp', '\bgcp\b'), ('gcp', '\bgoogle cloud\b'),

    ('postgres', '\bpostgresql\b'), ('postgres', '\bpostgres\b'),
    ('mysql', '\bmysql\b'),
    ('mssql', '\bsql server\b'), ('mssql', '\bt-sql\b'), ('mssql', '\bms sql\b'),
    ('mongodb', '\bmongodb\b'), ('mongodb', '\bmongo\b'),
    ('redis', '\bredis\b')
) AS a(skill_key, pattern)
WHERE s.key = a.skill_key AND s.ruleset_id = (SELECT id FROM ruleset WHERE key = 'software-dev');
