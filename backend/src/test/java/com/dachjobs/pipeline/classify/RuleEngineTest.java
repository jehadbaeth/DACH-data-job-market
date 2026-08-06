package com.dachjobs.pipeline.classify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Same CASES table as the classify() mirror in notebooks/03_silver_clean.py,
 * run against the data-driven RuleEngine instead of the Python re chain. If
 * this passes, the rule-engine abstraction reproduces the original
 * classifier's behaviour exactly, which is the whole premise of moving the
 * regexes from code into data.
 *
 * Keep this list in sync with
 * backend/src/main/resources/db/migration/V2__seed_data_ai_ruleset.sql -
 * same warning the original file gives about its own two copies.
 */
class RuleEngineTest {

    private static final List<ClassificationRuleDef> DATA_AI_RULES = List.of(
            new ClassificationRuleDef(0, "invalid",
                    "^initiativbewerbung|initiativbewerbung|talentpool|^deine aufgaben|^zum \\d|^wachstum durch"),
            new ClassificationRuleDef(1, "entry programme",
                    "\\bausbildung\\b|duales? studium|dualer bachelor|\\bwerkstudent|werkstudierend|\\bpraktikum\\b|\\bpraktikant|\\btrainee\\b|traineeprogramm|bachelor thesis|master student|studienkolleg|bachelor of science|\\bdhbw\\b|\\bhwr\\b|b\\.\\s?a\\.\\s+in\\b|^b\\.\\s?a\\.|abschlussarbeit"),
            new ClassificationRuleDef(2, "data centre",
                    "data\\s*cent(er|re)|rechenzentrum|\\bdceo\\b"),
            new ClassificationRuleDef(3, "finance",
                    "controlling|\\bcontroller\\b|finanzen|buchhalt|\\bfp&a\\b|financial planning|finance transformation|finance\\s*&\\s*accounting|finance business partner|kaufmaennisch|vertriebscontrolling|finance-consulting|finance solutions|transaction advisory"),
            new ClassificationRuleDef(4, "data architect",
                    "data\\s+[\\w\\s\\-&/]*architect|datenarchitekt"),
            new ClassificationRuleDef(5, "analytics engineer", "analytics engineer"),
            new ClassificationRuleDef(6, "data engineer",
                    "data engineer|dateningenieur|\\bbig data\\b|data platform|data scraping|data processing|data\\s*&\\s*ai[\\s\\-]*engineer|data\\s+(migration|integration|pipeline)|datenbankadministrator|database administrator|\\bdba\\b"),
            new ClassificationRuleDef(7, "dwh / etl",
                    "data warehouse|\\bdwh\\b|\\betl\\b|\\bdbt\\b|sap bw|datasphere|data.?lake|data vault|data modeler|data mesh|business data cloud"),
            new ClassificationRuleDef(8, "data governance",
                    "data governance|data quality|data privacy|master data|data steward|data management|data strategy|data protection"),
            new ClassificationRuleDef(9, "data scientist", "data scientist|data science"),
            new ClassificationRuleDef(10, "data analyst", "data analyst|datenanalyst|\\bbi analyst"),
            new ClassificationRuleDef(11, "data consultant",
                    "data\\s*&\\s*ai|data and ai|data\\s*&\\s*analytics|data analytics|data consultant|daten- und prozessanalyse|\\bdata expert\\b|data insights|\\bit\\s*&\\s*data\\b|business data"),
            new ClassificationRuleDef(12, "bi developer",
                    "\\bbi\\b|business intelligence|power ?bi|\\btableau\\b|\\bqlik\\b|\\bcelonis\\b|sap analytics|process mining|process intelligence|\\bsac\\b|reporting analyst|\\bjedox\\b"),
            new ClassificationRuleDef(13, "ai research",
                    "applied scientist|research scientist|research engineer|forschungsingenieur|\\bai research|\\bki[\\s\\-]forsch|machine learning (researcher|scientist)|\\bphd\\b.*\\b(ai|ml)\\b"),
            new ClassificationRuleDef(14, "genai / llm",
                    "\\bgenai\\b|\\bgen[\\s\\-]?ai\\b|generative ai|generative ki|\\bllms?\\b|\\bllmops\\b|large language model|sprachmodell|foundation model|grundlagenmodell|\\brag\\b|retrieval[\\s\\-]augmented|prompt[\\s\\-]?engineer|agentic|\\bai agent|\\bki[\\s\\-]agent|conversational ai|chatbot engineer|\\bai[\\s\\-]native|copilot engineer"),
            new ClassificationRuleDef(15, "mlops",
                    "\\bmlops\\b|\\bml[\\s\\-]?ops\\b|\\bmodel ops\\b|ml[\\s\\-](platform|infrastructure|infra|engineer[\\s\\-]platform)|machine learning (platform|infrastructure|infra|operations)|model (serving|deployment|monitoring)|\\bai (platform|infrastructure|infra)\\b|\\bki[\\s\\-]plattform\\b|feature store"),
            new ClassificationRuleDef(16, "ml engineer",
                    "mas?chine\\s*learning|\\bml[\\s\\-]?engineer|\\bml[\\s\\-]?ingenieur|maschinelles lernen|deep learning|\\bdl engineer\\b|reinforcement learning|bestaerkendes lernen|computer vision|bildverarbeitung|bilderkennung|\\bnlp\\b|natural language processing|sprachverarbeitung|speech recognition|spracherkennung|\\bai/ml\\b|\\bai\\s*&\\s*ml\\b|recommender|predictive model|forecasting engineer"),
            new ClassificationRuleDef(17, "ai consultant",
                    "\\bai\\s+(consultant|consulting|architect|advisor|strategy|strateg|transformation|solution|presales|sales engineer|product manager|product owner|governance|ethics|compliance)|\\bki[\\s\\-]?(berater|architekt|strategie|transformation)|artificial intelligence (consultant|architect|strategy|advisor)|(consultant|architect|berater)\\s+(fuer\\s+)?(ai|ki|artificial intelligence)\\b"),
            new ClassificationRuleDef(18, "ai engineer",
                    "\\bai\\s*[\\-/]?\\s*(engineer|developer|entwickler|specialist|spezialist|expert)|\\bki\\s*[\\-/]?\\s*(engineer|developer|entwickler|spezialist|experte)|artificial intelligence engineer|\\bai software engineer\\b"),
            new ClassificationRuleDef(19, "ai (other)",
                    "\\bai\\b|\\bki\\b|artificial intelligence|kuenstliche intelligenz")
    );

    private static final RuleEngine ENGINE = new RuleEngine(DATA_AI_RULES);

    private static String classify(String title) {
        return ENGINE.classify(TitleNormalizer.normalize(title));
    }

    static Stream<Arguments> cases() {
        return Stream.of(
                // the seven AI families
                Arguments.of("Machine Learning Engineer (m/w/d)", "ml engineer"),
                Arguments.of("Senior ML Engineer - Computer Vision", "ml engineer"),
                Arguments.of("Deep Learning Engineer", "ml engineer"),
                Arguments.of("NLP Engineer", "ml engineer"),
                Arguments.of("MLOps Engineer", "mlops"),
                Arguments.of("ML Platform Engineer (f/m/x)", "mlops"),
                Arguments.of("Model Deployment Engineer", "mlops"),
                Arguments.of("GenAI Engineer", "genai / llm"),
                Arguments.of("LLM Engineer / RAG Specialist", "genai / llm"),
                Arguments.of("Prompt Engineer (Remote)", "genai / llm"),
                Arguments.of("Agentic AI Developer", "genai / llm"),
                Arguments.of("Generative KI Spezialist", "genai / llm"),
                Arguments.of("Applied Scientist, Machine Learning", "ai research"),
                Arguments.of("AI Research Engineer", "ai research"),
                Arguments.of("Research Scientist Deep Learning", "ai research"),
                Arguments.of("AI Consultant (m/w/d)", "ai consultant"),
                Arguments.of("KI-Berater Digitalisierung", "ai consultant"),
                Arguments.of("AI Solution Architect", "ai consultant"),
                Arguments.of("AI Product Manager", "ai consultant"),
                Arguments.of("AI Engineer", "ai engineer"),
                Arguments.of("KI-Entwickler (m/w/d)", "ai engineer"),
                Arguments.of("Artificial Intelligence Engineer", "ai engineer"),
                Arguments.of("AI Specialist", "ai engineer"),
                // the vague bucket
                Arguments.of("Software Engineer with AI focus", "ai (other)"),
                Arguments.of("Projektmanager KI", "ai (other)"),
                Arguments.of("Fullstack Developer (AI Startup)", "ai (other)"),
                // data still beats AI
                Arguments.of("Data & AI Consultant", "data consultant"),
                Arguments.of("Data Engineer AI Platform", "data engineer"),
                Arguments.of("Data Scientist NLP", "data scientist"),
                Arguments.of("Senior Data Analyst", "data analyst"),
                Arguments.of("Business Intelligence Developer", "bi developer"),
                Arguments.of("Analytics Engineer", "analytics engineer"),
                // excludes still beat everything
                Arguments.of("Werkstudent AI Engineering", "entry programme"),
                Arguments.of("Controlling Specialist AI", "finance"),
                Arguments.of("Rechenzentrum Techniker", "data centre"),
                Arguments.of("Versicherungsagent", "other")
        );
    }

    @ParameterizedTest
    @MethodSource("cases")
    void classifiesLikeTheOriginalPipeline(String title, String expectedFamily) {
        assertThat(classify(title)).isEqualTo(expectedFamily);
    }

    @Test
    void unmatchedRulesetFallsBackToOtherKeyRatherThanThrowing() {
        assertThat(classify("Totally unrelated title")).isEqualTo(RuleEngine.DEFAULT_FAMILY_KEY);
    }
}
