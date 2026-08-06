package com.dachjobs.pipeline.skills;

import java.util.List;

/**
 * Plain, DB-agnostic view of a skill_definition row plus its aliases, so
 * {@link SkillMatcher} can be unit tested without a Spring context.
 *
 * @param contextGuard optional regex; when set, a match on this skill is
 *                      only accepted if the guard also matches somewhere in
 *                      the original (non-blanked) text. Ports the bare-"R"
 *                      handling from src/matcher.py.
 */
public record SkillDef(String key, String category, List<String> aliasPatterns, String contextGuard) {
}
