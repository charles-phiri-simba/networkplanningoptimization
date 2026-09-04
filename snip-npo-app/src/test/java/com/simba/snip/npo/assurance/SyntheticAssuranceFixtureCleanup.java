package com.simba.snip.npo.assurance;

import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test-only cleanup for synthetic DEGRADING_RADIO_QUALITY rows created by fixture
 * telemetry seeding on the shared Testcontainers database.
 */
public final class SyntheticAssuranceFixtureCleanup {

    private SyntheticAssuranceFixtureCleanup() {
    }

    public static void deleteSyntheticDegradingCases(JdbcTemplate jdbc, String cellId) {
        jdbc.update(
                """
                DELETE FROM assurance_evidence WHERE assurance_case_id IN (
                    SELECT id FROM assurance_case
                    WHERE affected_entity_id = ? AND synthetic = TRUE AND rule_id = ?
                )
                """,
                cellId,
                AssuranceRules.DEGRADING_RADIO_QUALITY_RULE_ID);
        jdbc.update(
                """
                DELETE FROM assurance_case
                WHERE affected_entity_id = ? AND synthetic = TRUE AND rule_id = ?
                """,
                cellId,
                AssuranceRules.DEGRADING_RADIO_QUALITY_RULE_ID);
    }

    public static void assertNoSyntheticDegradingCases(JdbcTemplate jdbc, String cellId) {
        Integer cases = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM assurance_case
                WHERE affected_entity_id = ? AND synthetic = TRUE AND rule_id = ?
                """,
                Integer.class,
                cellId,
                AssuranceRules.DEGRADING_RADIO_QUALITY_RULE_ID);
        Integer evidence = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM assurance_evidence e
                JOIN assurance_case c ON c.id = e.assurance_case_id
                WHERE c.affected_entity_id = ? AND c.synthetic = TRUE AND c.rule_id = ?
                """,
                Integer.class,
                cellId,
                AssuranceRules.DEGRADING_RADIO_QUALITY_RULE_ID);
        assertEquals(0, cases, "synthetic assurance_case rows remain for " + cellId);
        assertEquals(0, evidence, "synthetic assurance_evidence rows remain for " + cellId);
    }

    public static void deleteAndAssertSyntheticDegradingCases(JdbcTemplate jdbc, String cellId) {
        deleteSyntheticDegradingCases(jdbc, cellId);
        assertNoSyntheticDegradingCases(jdbc, cellId);
    }
}
