package com.dachjobs.pipeline.skills;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Direct port of src/matcher.py / notebooks/04_skills_extract.py.
 *
 * Two behaviours preserved on purpose because they are the whole reason the
 * original matcher is trustworthy:
 *
 * <ol>
 *   <li><b>Longest alias pattern first, then blank the matched span</b> -
 *       otherwise "sql" re-matches inside "postgresql" and inflates the SQL
 *       count by every Postgres posting.</li>
 *   <li><b>Context-guarded skills</b> (e.g. a bare "R") are only accepted
 *       when a guard regex also matches the original, non-blanked text -
 *       a naive \bR\b matches "R&D", "R. Mueller" and German address
 *       blocks.</li>
 * </ol>
 */
public final class SkillMatcher {

    private record CompiledAlias(String skillKey, String category, Pattern pattern, Pattern contextGuard) {
    }

    private final List<CompiledAlias> compiled;

    public SkillMatcher(List<SkillDef> skills) {
        List<CompiledAlias> all = new ArrayList<>();
        for (SkillDef skill : skills) {
            Pattern guard = skill.contextGuard() == null
                    ? null
                    : Pattern.compile(skill.contextGuard(), Pattern.CASE_INSENSITIVE);
            for (String alias : skill.aliasPatterns()) {
                all.add(new CompiledAlias(skill.key(), skill.category(),
                        Pattern.compile(alias, Pattern.CASE_INSENSITIVE), guard));
            }
        }
        // longest alias pattern text first, mirroring PATTERNS sorted by -len(alias)
        all.sort((a, b) -> Integer.compare(
                b.pattern.pattern().length(), a.pattern.pattern().length()));
        this.compiled = List.copyOf(all);
    }

    /** Sorted, deduplicated skill keys mentioned in {@code text}. */
    public List<String> extract(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        StringBuilder remaining = new StringBuilder(text.toLowerCase());
        TreeSet<String> found = new TreeSet<>();

        for (CompiledAlias alias : compiled) {
            Matcher m = alias.pattern.matcher(remaining);
            if (!m.find()) {
                continue;
            }
            if (alias.contextGuard != null && !alias.contextGuard.matcher(text).find()) {
                continue;
            }
            found.add(alias.skillKey);
            // blank the matched span so shorter aliases cannot re-match inside it
            for (int i = m.start(); i < m.end(); i++) {
                remaining.setCharAt(i, ' ');
            }
        }

        return List.copyOf(found);
    }

    /** Skill key -> category, for whichever skills matched. */
    public Map<String, String> categoriesOf(List<String> skillKeys) {
        Map<String, String> byKey = new LinkedHashMap<>();
        for (CompiledAlias alias : compiled) {
            byKey.putIfAbsent(alias.skillKey, alias.category);
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (String key : skillKeys) {
            result.put(key, byKey.get(key));
        }
        return result;
    }
}
