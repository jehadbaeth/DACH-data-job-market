package com.dachjobs.pipeline.skills;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Same traps as tests/test_matcher.py and the inline checks in
 * notebooks/04_skills_extract.py, run against the data-driven SkillMatcher.
 * Keep this dictionary in sync with
 * backend/src/main/resources/db/migration/V2__seed_data_ai_ruleset.sql.
 */
class SkillMatcherTest {

    private static SkillMatcher matcher;

    @BeforeAll
    static void setUp() {
        List<SkillDef> defs = new ArrayList<>();
        defs.add(new SkillDef("sql", "language", List.of("\\bsql\\b"), null));
        defs.add(new SkillDef("python", "language", List.of("\\bpython\\b"), null));
        defs.add(new SkillDef("scala", "language", List.of("\\bscala\\b"), null));
        defs.add(new SkillDef("java", "language", List.of("\\bjava\\b(?!script)"), null));
        defs.add(new SkillDef("javascript", "language",
                List.of("\\bjavascript\\b", "\\btypescript\\b"), null));
        defs.add(new SkillDef("php", "language", List.of("\\bphp\\b"), null));
        defs.add(new SkillDef("r_lang", "language",
                List.of("(?<=[ ,/(])r(?=[ ,/)])"),
                "(python|sql|statist|analys|sprachen|languages|matlab|sas)"));

        defs.add(new SkillDef("postgres", "database",
                List.of("\\bpostgresql\\b", "\\bpostgres\\b"), null));
        defs.add(new SkillDef("mysql", "database", List.of("\\bmysql\\b"), null));

        defs.add(new SkillDef("spark", "processing",
                List.of("\\bapache spark\\b", "\\bpyspark\\b", "\\bspark\\b"), null));

        defs.add(new SkillDef("kubernetes", "devops",
                List.of("\\bkubernetes\\b", "\\bk8s\\b"), null));
        defs.add(new SkillDef("azure", "cloud", List.of("\\bazure\\b", "\\bdata factory\\b"), null));
        defs.add(new SkillDef("azureml", "mlops",
                List.of("\\bazure ml\\b", "\\bazure machine learning\\b", "\\bazure ai foundry\\b"), null));
        defs.add(new SkillDef("mlflow", "mlops", List.of("\\bmlflow\\b"), null));
        defs.add(new SkillDef("kubeflow", "mlops", List.of("\\bkubeflow\\b"), null));

        defs.add(new SkillDef("llm", "genai",
                List.of("\\bllms?\\b", "\\blarge language models?\\b", "\\bsprachmodell\\w*\\b",
                        "\\bfoundation models?\\b"), null));
        defs.add(new SkillDef("genai", "genai",
                List.of("\\bgenai\\b", "\\bgen[\\s\\-]?ai\\b", "\\bgenerative ai\\b", "\\bgenerative ki\\b"),
                null));
        defs.add(new SkillDef("rag", "genai",
                List.of("\\bretrieval[\\s\\-]augmented\\w*\\b", "\\brag\\b"), null));
        defs.add(new SkillDef("openai_api", "genai",
                List.of("\\bopenai\\b", "\\bgpt-?[45]\\b", "\\banthropic\\b",
                        "\\bclaude (api|sonnet|opus|code)\\b", "\\bazure openai\\b", "\\bmistral\\b", "\\bgemini\\b"),
                null));
        defs.add(new SkillDef("vectordb", "genai",
                List.of("\\bvector (db|database|store)\\b", "\\bvektordatenbank\\b", "\\bpinecone\\b",
                        "\\bweaviate\\b", "\\bqdrant\\b", "\\bmilvus\\b", "\\bchroma\\b", "\\bpgvector\\b",
                        "\\bfaiss\\b"), null));
        defs.add(new SkillDef("finetuning", "genai",
                List.of("\\bfine[\\s\\-]?tun\\w*\\b", "\\blora\\b(?!wan)", "\\bpeft\\b", "\\brlhf\\b",
                        "\\bembeddings?\\b"), null));
        defs.add(new SkillDef("huggingface", "genai",
                List.of("\\bhugging ?face\\b", "\\btransformers library\\b"), null));

        defs.add(new SkillDef("deeplearning", "nlp_cv",
                List.of("\\bdeep learning\\b", "\\bneural network\\w*\\b", "\\bneuronale netze\\b",
                        "\\btransformer\\b", "\\bdiffusion model\\w*\\b"), null));

        matcher = new SkillMatcher(defs);
    }

    private static Set<String> extract(String text) {
        return new TreeSet<>(matcher.extract(text));
    }

    @Test
    void sqlNotMatchedInsidePostgresql() {
        assertThat(matcher.extract("We use PostgreSQL daily")).containsExactly("postgres");
    }

    @Test
    void sqlNotMatchedInsideMysql() {
        assertThat(matcher.extract("Legacy stack on MySQL")).containsExactly("mysql");
    }

    @Test
    void javaNotMatchedInsideJavascript() {
        assertThat(matcher.extract("Strong JavaScript skills required")).doesNotContain("java");
    }

    @Test
    void javaStillMatchedOnItsOwn() {
        assertThat(matcher.extract("Java and Scala experience")).contains("java");
    }

    @Test
    void rNotMatchedInRnd() {
        assertThat(matcher.extract("Our R&D team in Berlin")).doesNotContain("r_lang");
    }

    @Test
    void rNotMatchedInAName() {
        assertThat(matcher.extract("Contact R. Mueller for details")).doesNotContain("r_lang");
    }

    @Test
    void rMatchedWithTechContext() {
        assertThat(matcher.extract("Sprachen: Python, R, SQL")).contains("r_lang");
    }

    @Test
    void germanPostingParsed() {
        assertThat(extract("Kenntnisse in Python und Erfahrung mit Apache Spark"))
                .isEqualTo(new TreeSet<>(Set.of("python", "spark")));
    }

    @Test
    void loraWanIsNotFineTuning() {
        assertThat(matcher.extract("LoRaWAN Sensorik im Feld")).doesNotContain("finetuning");
    }

    @Test
    void loraIsFineTuning() {
        assertThat(matcher.extract("LoRA fine-tuning of open models")).contains("finetuning");
    }

    @Test
    void claudeTheFirstNameIsNotAVendor() {
        assertThat(matcher.extract("Ansprechpartner: Claude Meier")).doesNotContain("openai_api");
    }

    @Test
    void claudeApiIsAVendor() {
        assertThat(matcher.extract("Erfahrung mit der Claude API")).contains("openai_api");
    }

    @Test
    void ragAndLlmAreSeparateSkills() {
        assertThat(extract("RAG pipelines on top of LLMs"))
                .isEqualTo(new TreeSet<>(Set.of("rag", "llm")));
    }

    @Test
    void azureMlDoesNotSwallowTheAzureCloudSkill() {
        assertThat(matcher.extract("Azure ML")).containsExactly("azureml");
    }

    @Test
    void mlopsStackParsed() {
        assertThat(extract("MLflow und Kubeflow auf Kubernetes"))
                .isEqualTo(new TreeSet<>(Set.of("mlflow", "kubeflow", "kubernetes")));
    }

    @Test
    void vectorDbRecognised() {
        assertThat(matcher.extract("Wir nutzen pgvector fuer Suche")).contains("vectordb");
    }

    @Test
    void transformersLibraryGoesToHuggingfaceNotDeepLearning() {
        assertThat(matcher.extract("Hugging Face transformers library")).contains("huggingface");
    }
}
