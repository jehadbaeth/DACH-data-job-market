-- The 'data-ai' ruleset (V2) was ported verbatim from the original Python
-- pipeline as a correctness proof that the rule-engine abstraction could
-- reproduce its behaviour. It never had a matching Adzuna search vertical
-- (roles are software-dev terms only, see application.yml), so it fails
-- its own "> 500 postings" quality gate on every scheduled run and just
-- logs noise. Removing it now that the proof has served its purpose.

DELETE FROM posting_skill
WHERE posting_id IN (
    SELECT p.id FROM posting p
    JOIN ruleset r ON r.id = p.ruleset_id
    WHERE r.key = 'data-ai'
);

DELETE FROM history_metric
WHERE ruleset_id IN (SELECT id FROM ruleset WHERE key = 'data-ai');

DELETE FROM skill_alias
WHERE skill_id IN (
    SELECT s.id FROM skill_definition s
    JOIN ruleset r ON r.id = s.ruleset_id
    WHERE r.key = 'data-ai'
);

DELETE FROM skill_definition
WHERE ruleset_id IN (SELECT id FROM ruleset WHERE key = 'data-ai');

DELETE FROM posting
WHERE ruleset_id IN (SELECT id FROM ruleset WHERE key = 'data-ai');

DELETE FROM classification_rule
WHERE ruleset_id IN (SELECT id FROM ruleset WHERE key = 'data-ai');

DELETE FROM role_family
WHERE ruleset_id IN (SELECT id FROM ruleset WHERE key = 'data-ai');

DELETE FROM ruleset WHERE key = 'data-ai';
