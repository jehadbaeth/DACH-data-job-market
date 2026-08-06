package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * V1's inline `key TEXT NOT NULL UNIQUE` constraint on skill_definition is
 * getting replaced by a per-ruleset UNIQUE(ruleset_id, key) in the next
 * migration, so verticals can reuse the same skill key (e.g. "python").
 *
 * Postgres and H2 auto-name that inline constraint differently (Postgres:
 * skill_definition_key_key; H2: an opaque CONSTRAINT_<n>_INDEX_C), so a
 * hardcoded DROP CONSTRAINT name silently no-ops on whichever engine it
 * guessed wrong. Looking the name up via information_schema instead works
 * on both without guessing.
 */
public class V3__DropSkillDefinitionKeyUniqueConstraint extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String constraintName = null;

        try (Statement lookup = connection.createStatement();
             ResultSet rs = lookup.executeQuery("""
                     SELECT tc.constraint_name
                     FROM information_schema.table_constraints tc
                     JOIN information_schema.key_column_usage kcu
                       ON tc.constraint_name = kcu.constraint_name
                      AND tc.table_name = kcu.table_name
                     WHERE UPPER(tc.table_name) = 'SKILL_DEFINITION'
                       AND tc.constraint_type = 'UNIQUE'
                       AND UPPER(kcu.column_name) = 'KEY'
                     """)) {
            if (rs.next()) {
                constraintName = rs.getString(1);
            }
        }

        if (constraintName != null) {
            try (Statement drop = connection.createStatement()) {
                drop.execute("ALTER TABLE skill_definition DROP CONSTRAINT \"" + constraintName + "\"");
            }
        }
    }
}
