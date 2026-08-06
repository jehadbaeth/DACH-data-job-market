-- Ports the existing DACH data/AI classifier from notebooks/03_silver_clean.py
-- verbatim (same regexes, same order) into data. This is the proof that the
-- rule-engine abstraction can reproduce the original pipeline's behaviour
-- before any new vertical (e.g. software engineering) is added.

INSERT INTO ruleset (key, label, description) VALUES
    ('data-ai', 'DACH Data & AI jobs',
     'Original DACH data-job-market classifier: data and AI role families.');

-- role families ---------------------------------------------------------

INSERT INTO role_family (ruleset_id, key, label, group_name, sort_order, published)
SELECT r.id, v.key, v.label, v.group_name, v.sort_order, v.published
FROM ruleset r, (VALUES
    ('invalid',            'Invalid / junk',        'excluded', 0,  false),
    ('entry programme',    'Entry programme',       'excluded', 1,  false),
    ('data centre',        'Data centre',            'excluded', 2,  false),
    ('finance',            'Finance / Controlling',  'excluded', 3,  false),

    ('data architect',     'Data Architect',         'data', 10, true),
    ('analytics engineer', 'Analytics Engineer',     'data', 11, true),
    ('data engineer',      'Data Engineer',           'data', 12, true),
    ('dwh / etl',          'DWH / ETL',               'data', 13, true),
    ('data governance',    'Data Governance',         'data', 14, true),
    ('data scientist',     'Data Scientist',          'data', 15, true),
    ('data analyst',       'Data Analyst',            'data', 16, true),
    ('data consultant',    'Data Consultant',         'data', 17, true),
    ('bi developer',       'BI Developer',            'data', 18, true),

    ('ai research',        'AI Research',             'ai', 20, true),
    ('genai / llm',        'GenAI / LLM',             'ai', 21, true),
    ('mlops',              'MLOps',                   'ai', 22, true),
    ('ml engineer',        'ML Engineer',             'ai', 23, true),
    ('ai consultant',      'AI Consultant',           'ai', 24, true),
    ('ai engineer',        'AI Engineer',             'ai', 25, true),

    -- published = false: still classified so the drift diagnostic works,
    -- never reported. See 03_silver_clean.py's VAGUE_FAMILY discussion.
    ('ai (other)',         'AI (other, vague)',       'excluded', 26, false),
    ('other',              'Other (unclassified)',    'excluded', 99, false)
) AS v(key, label, group_name, sort_order, published)
WHERE r.key = 'data-ai';

-- classification rules, in the exact order _ORDER used in 03_silver_clean.py
-- patterns are Java regex, matched against the already-lowercased,
-- umlaut-folded title (fold() ported in TitleNormalizer), so no inline
-- case-insensitive flag is needed here.

INSERT INTO classification_rule (ruleset_id, priority, family_key, pattern, description)
SELECT r.id, v.priority, v.family_key, v.pattern, v.description
FROM ruleset r, (VALUES
    (0, 'invalid',
     '^initiativbewerbung|initiativbewerbung|talentpool|^deine aufgaben|^zum \d|^wachstum durch',
     'Parse artefacts and speculative applications, not real postings.'),

    (1, 'entry programme',
     '\bausbildung\b|duales? studium|dualer bachelor|\bwerkstudent|werkstudierend|\bpraktikum\b|\bpraktikant|\btrainee\b|traineeprogramm|bachelor thesis|master student|studienkolleg|bachelor of science|\bdhbw\b|\bhwr\b|b\.\s?a\.\s+in\b|^b\.\s?a\.|abschlussarbeit',
     'Education programmes; excluded because they distort posting-age figures.'),

    (2, 'data centre',
     'data\s*cent(er|re)|rechenzentrum|\bdceo\b',
     'False friend: physical infrastructure, not a data role.'),

    (3, 'finance',
     'controlling|\bcontroller\b|finanzen|buchhalt|\bfp&a\b|financial planning|finance transformation|finance\s*&\s*accounting|finance business partner|kaufmaennisch|vertriebscontrolling|finance-consulting|finance solutions|transaction advisory',
     'German Controlling / FP&A / finance; a separate profession.'),

    (4, 'data architect',
     'data\s+[\w\s\-&/]*architect|datenarchitekt', null),
    (5, 'analytics engineer',
     'analytics engineer', null),
    (6, 'data engineer',
     'data engineer|dateningenieur|\bbig data\b|data platform|data scraping|data processing|data\s*&\s*ai[\s\-]*engineer|data\s+(migration|integration|pipeline)|datenbankadministrator|database administrator|\bdba\b',
     null),
    (7, 'dwh / etl',
     'data warehouse|\bdwh\b|\betl\b|\bdbt\b|sap bw|datasphere|data.?lake|data vault|data modeler|data mesh|business data cloud',
     null),
    (8, 'data governance',
     'data governance|data quality|data privacy|master data|data steward|data management|data strategy|data protection',
     null),
    (9, 'data scientist',
     'data scientist|data science', null),
    (10, 'data analyst',
     'data analyst|datenanalyst|\bbi analyst', null),
    (11, 'data consultant',
     'data\s*&\s*ai|data and ai|data\s*&\s*analytics|data analytics|data consultant|daten- und prozessanalyse|\bdata expert\b|data insights|\bit\s*&\s*data\b|business data',
     null),
    (12, 'bi developer',
     '\bbi\b|business intelligence|power ?bi|\btableau\b|\bqlik\b|\bcelonis\b|sap analytics|process mining|process intelligence|\bsac\b|reporting analyst|\bjedox\b',
     null),

    -- AI families: research first (narrowest, a separate hiring track),
    -- vague catch-all ("ai (other)") last, on purpose.
    (13, 'ai research',
     'applied scientist|research scientist|research engineer|forschungsingenieur|\bai research|\bki[\s\-]forsch|machine learning (researcher|scientist)|\bphd\b.*\b(ai|ml)\b',
     null),
    (14, 'genai / llm',
     '\bgenai\b|\bgen[\s\-]?ai\b|generative ai|generative ki|\bllms?\b|\bllmops\b|large language model|sprachmodell|foundation model|grundlagenmodell|\brag\b|retrieval[\s\-]augmented|prompt[\s\-]?engineer|agentic|\bai agent|\bki[\s\-]agent|conversational ai|chatbot engineer|\bai[\s\-]native|copilot engineer',
     null),
    (15, 'mlops',
     '\bmlops\b|\bml[\s\-]?ops\b|\bmodel ops\b|ml[\s\-](platform|infrastructure|infra|engineer[\s\-]platform)|machine learning (platform|infrastructure|infra|operations)|model (serving|deployment|monitoring)|\bai (platform|infrastructure|infra)\b|\bki[\s\-]plattform\b|feature store',
     null),
    (16, 'ml engineer',
     'mas?chine\s*learning|\bml[\s\-]?engineer|\bml[\s\-]?ingenieur|maschinelles lernen|deep learning|\bdl engineer\b|reinforcement learning|bestaerkendes lernen|computer vision|bildverarbeitung|bilderkennung|\bnlp\b|natural language processing|sprachverarbeitung|speech recognition|spracherkennung|\bai/ml\b|\bai\s*&\s*ml\b|recommender|predictive model|forecasting engineer',
     null),
    (17, 'ai consultant',
     '\bai\s+(consultant|consulting|architect|advisor|strategy|strateg|transformation|solution|presales|sales engineer|product manager|product owner|governance|ethics|compliance)|\bki[\s\-]?(berater|architekt|strategie|transformation)|artificial intelligence (consultant|architect|strategy|advisor)|(consultant|architect|berater)\s+(fuer\s+)?(ai|ki|artificial intelligence)\b',
     null),
    (18, 'ai engineer',
     '\bai\s*[\-/]?\s*(engineer|developer|entwickler|specialist|spezialist|expert)|\bki\s*[\-/]?\s*(engineer|developer|entwickler|spezialist|experte)|artificial intelligence engineer|\bai software engineer\b',
     null),
    (19, 'ai (other)',
     '\bai\b|\bki\b|artificial intelligence|kuenstliche intelligenz',
     'Last resort: mentions AI, names no role. Diagnostic only, never published.')
) AS v(priority, family_key, pattern, description)
WHERE r.key = 'data-ai';

-- skill dictionary --------------------------------------------------------
-- ported from notebooks/04_skills_extract.py (the richer, four-way AI split;
-- src/skills_dictionary.py is the older copy and should be retired once this
-- ships, see the follow-up note in the PR description).

INSERT INTO skill_definition (key, category, label, context_pattern)
VALUES
    ('sql', 'language', 'SQL', null),
    ('python', 'language', 'Python', null),
    ('scala', 'language', 'Scala', null),
    ('java', 'language', 'Java', null),
    ('javascript', 'language', 'JavaScript / TypeScript', null),
    ('php', 'language', 'PHP', null),
    ('r_lang', 'language', 'R',
     '(python|sql|statist|analys|sprachen|languages|matlab|sas)'),

    ('spark', 'processing', 'Apache Spark', null),
    ('hadoop', 'legacy', 'Hadoop', null),
    ('kafka', 'streaming', 'Kafka', null),
    ('flink', 'streaming', 'Flink', null),

    ('databricks', 'platform', 'Databricks', null),
    ('snowflake', 'warehouse', 'Snowflake', null),
    ('bigquery', 'warehouse', 'BigQuery', null),
    ('redshift', 'warehouse', 'Redshift', null),
    ('synapse', 'warehouse', 'Synapse', null),

    ('airflow', 'orchestr', 'Airflow', null),
    ('dbt', 'transform', 'dbt', null),
    ('ssis', 'legacy', 'SSIS', null),
    ('talend', 'legacy', 'Talend', null),

    ('postgres', 'database', 'PostgreSQL', null),
    ('mysql', 'database', 'MySQL', null),
    ('mssql', 'database', 'SQL Server', null),
    ('oracle', 'database', 'Oracle', null),
    ('mongodb', 'database', 'MongoDB', null),
    ('elastic', 'database', 'Elasticsearch', null),

    ('aws', 'cloud', 'AWS', null),
    ('azure', 'cloud', 'Azure', null),
    ('gcp', 'cloud', 'GCP', null),

    ('docker', 'devops', 'Docker', null),
    ('kubernetes', 'devops', 'Kubernetes', null),
    ('terraform', 'devops', 'Terraform', null),
    ('git', 'devops', 'Git', null),
    ('cicd', 'devops', 'CI/CD', null),

    ('powerbi', 'bi', 'Power BI', null),
    ('tableau', 'bi', 'Tableau', null),
    ('looker', 'bi', 'Looker', null),
    ('qlik', 'bi', 'Qlik', null),
    ('excel', 'bi', 'Excel', null),

    ('pytorch', 'ml_framework', 'PyTorch', null),
    ('tensorflow', 'ml_framework', 'TensorFlow', null),
    ('keras', 'ml_framework', 'Keras', null),
    ('sklearn', 'ml_framework', 'scikit-learn', null),
    ('xgboost', 'ml_framework', 'XGBoost / LightGBM / CatBoost', null),
    ('jax', 'ml_framework', 'JAX', null),

    ('mlflow', 'mlops', 'MLflow', null),
    ('kubeflow', 'mlops', 'Kubeflow', null),
    ('sagemaker', 'mlops', 'SageMaker', null),
    ('vertexai', 'mlops', 'Vertex AI', null),
    ('azureml', 'mlops', 'Azure ML', null),
    ('ray', 'mlops', 'Ray / Anyscale', null),
    ('wandb', 'mlops', 'Weights & Biases', null),
    ('bentoml', 'mlops', 'BentoML / Seldon / Triton / KServe', null),
    ('featurestore', 'mlops', 'Feature store', null),

    ('llm', 'genai', 'LLM', null),
    ('genai', 'genai', 'GenAI', null),
    ('rag', 'genai', 'RAG', null),
    ('ai_agents', 'genai', 'AI agents', null),
    ('prompting', 'genai', 'Prompt engineering', null),
    ('langchain', 'genai', 'LangChain / LlamaIndex / Haystack', null),
    ('huggingface', 'genai', 'Hugging Face', null),
    ('openai_api', 'genai', 'OpenAI / Anthropic / vendor APIs', null),
    ('vectordb', 'genai', 'Vector database', null),
    ('finetuning', 'genai', 'Fine-tuning', null),
    ('ai_eval', 'genai', 'AI eval / safety', null),

    ('nlp', 'nlp_cv', 'NLP', null),
    ('cv', 'nlp_cv', 'Computer vision', null),
    ('deeplearning', 'nlp_cv', 'Deep learning', null),

    ('sap', 'enterprise', 'SAP', null),
    ('etl', 'practice', 'ETL / ELT', null),
    ('datawarehouse', 'practice', 'Data warehouse / lake', null);

INSERT INTO skill_alias (skill_id, pattern)
SELECT s.id, a.pattern FROM skill_definition s, (VALUES
    ('sql', '\bsql\b'),
    ('python', '\bpython\b'),
    ('scala', '\bscala\b'),
    ('java', '\bjava\b(?!script)'),
    ('javascript', '\bjavascript\b'), ('javascript', '\btypescript\b'),
    ('php', '\bphp\b'),
    ('r_lang', '(?<=[ ,/(])r(?=[ ,/)])'),

    ('spark', '\bapache spark\b'), ('spark', '\bpyspark\b'), ('spark', '\bspark\b'),
    ('hadoop', '\bhadoop\b'), ('hadoop', '\bhdfs\b'), ('hadoop', '\bmapreduce\b'), ('hadoop', '\bhive\b'),
    ('kafka', '\bkafka\b'),
    ('flink', '\bflink\b'),

    ('databricks', '\bdatabricks\b'),
    ('snowflake', '\bsnowflake\b'),
    ('bigquery', '\bbigquery\b'),
    ('redshift', '\bredshift\b'),
    ('synapse', '\bsynapse\b'),

    ('airflow', '\bapache airflow\b'), ('airflow', '\bairflow\b'),
    ('dbt', '\bdbt\b'),
    ('ssis', '\bssis\b'),
    ('talend', '\btalend\b'),

    ('postgres', '\bpostgresql\b'), ('postgres', '\bpostgres\b'),
    ('mysql', '\bmysql\b'),
    ('mssql', '\bsql server\b'), ('mssql', '\bt-sql\b'), ('mssql', '\bms sql\b'),
    ('oracle', '\boracle\b'),
    ('mongodb', '\bmongodb\b'), ('mongodb', '\bmongo\b'),
    ('elastic', '\belasticsearch\b'),

    ('aws', '\baws\b'), ('aws', '\bamazon web services\b'),
    ('azure', '\bazure\b'), ('azure', '\bdata factory\b'),
    ('gcp', '\bgcp\b'), ('gcp', '\bgoogle cloud\b'),

    ('docker', '\bdocker\b'),
    ('kubernetes', '\bkubernetes\b'), ('kubernetes', '\bk8s\b'),
    ('terraform', '\bterraform\b'),
    ('git', '\bgithub\b'), ('git', '\bgitlab\b'), ('git', '\bgit\b'),
    ('cicd', '\bci/cd\b'), ('cicd', '\bjenkins\b'),

    ('powerbi', '\bpower ?bi\b'),
    ('tableau', '\btableau\b'),
    ('looker', '\blooker\b'),
    ('qlik', '\bqlik\b'),
    ('excel', '\bexcel\b'),

    ('pytorch', '\bpytorch\b'),
    ('tensorflow', '\btensorflow\b'),
    ('keras', '\bkeras\b'),
    ('sklearn', '\bscikit-?learn\b'), ('sklearn', '\bsklearn\b'),
    ('xgboost', '\bxgboost\b'), ('xgboost', '\blightgbm\b'), ('xgboost', '\bcatboost\b'),
    ('jax', '\bjax\b'),

    ('mlflow', '\bmlflow\b'),
    ('kubeflow', '\bkubeflow\b'),
    ('sagemaker', '\bsagemaker\b'), ('sagemaker', '\bsage maker\b'),
    ('vertexai', '\bvertex ai\b'), ('vertexai', '\bvertex\b'),
    ('azureml', '\bazure ml\b'), ('azureml', '\bazure machine learning\b'), ('azureml', '\bazure ai foundry\b'),
    ('ray', '\bray serve\b'), ('ray', '\bray\.io\b'), ('ray', '\banyscale\b'),
    ('wandb', '\bweights ?& ?biases\b'), ('wandb', '\bwandb\b'),
    ('bentoml', '\bbentoml\b'), ('bentoml', '\bseldon\b'), ('bentoml', '\btriton\b'), ('bentoml', '\bkserve\b'),
    ('featurestore', '\bfeature store\b'), ('featurestore', '\bfeast\b'), ('featurestore', '\btecton\b'),

    ('llm', '\bllms?\b'), ('llm', '\blarge language models?\b'), ('llm', '\bsprachmodell\w*\b'), ('llm', '\bfoundation models?\b'),
    ('genai', '\bgenai\b'), ('genai', '\bgen[\s\-]?ai\b'), ('genai', '\bgenerative ai\b'), ('genai', '\bgenerative ki\b'),
    ('rag', '\bretrieval[\s\-]augmented\w*\b'), ('rag', '\brag\b'),
    ('ai_agents', '\bagentic\b'), ('ai_agents', '\bai agents?\b'), ('ai_agents', '\bki[\s\-]agenten?\b'), ('ai_agents', '\bmulti[\s\-]agent\b'), ('ai_agents', '\bmcp\b'),
    ('prompting', '\bprompt[\s\-]?engineer\w*\b'), ('prompting', '\bprompting\b'),
    ('langchain', '\blangchain\b'), ('langchain', '\blanggraph\b'), ('langchain', '\bllama[\s\-]?index\b'), ('langchain', '\bsemantic kernel\b'), ('langchain', '\bhaystack\b'),
    ('huggingface', '\bhugging ?face\b'), ('huggingface', '\btransformers library\b'),
    ('openai_api', '\bopenai\b'), ('openai_api', '\bgpt-?[45]\b'), ('openai_api', '\banthropic\b'),
    ('openai_api', '\bclaude (api|sonnet|opus|code)\b'), ('openai_api', '\bazure openai\b'), ('openai_api', '\bmistral\b'), ('openai_api', '\bgemini\b'),
    ('vectordb', '\bvector (db|database|store)\b'), ('vectordb', '\bvektordatenbank\b'), ('vectordb', '\bpinecone\b'), ('vectordb', '\bweaviate\b'), ('vectordb', '\bqdrant\b'), ('vectordb', '\bmilvus\b'), ('vectordb', '\bchroma\b'), ('vectordb', '\bpgvector\b'), ('vectordb', '\bfaiss\b'),
    ('finetuning', '\bfine[\s\-]?tun\w*\b'), ('finetuning', '\blora\b(?!wan)'), ('finetuning', '\bpeft\b'), ('finetuning', '\brlhf\b'), ('finetuning', '\bembeddings?\b'),
    ('ai_eval', '\bllm[\s\-]?as[\s\-]?a[\s\-]?judge\b'), ('ai_eval', '\bhallucination\w*\b'), ('ai_eval', '\bguardrails?\b'), ('ai_eval', '\bevals?\b'), ('ai_eval', '\bai safety\b'), ('ai_eval', '\bai alignment\b'),

    ('nlp', '\bnlp\b'), ('nlp', '\bnatural language processing\b'), ('nlp', '\bsprachverarbeitung\b'), ('nlp', '\bspacy\b'), ('nlp', '\bnltk\b'),
    ('cv', '\bcomputer vision\b'), ('cv', '\bbildverarbeitung\b'), ('cv', '\bbilderkennung\b'), ('cv', '\bopencv\b'), ('cv', '\byolo\b'), ('cv', '\bobject detection\b'),
    ('deeplearning', '\bdeep learning\b'), ('deeplearning', '\bneural network\w*\b'), ('deeplearning', '\bneuronale netze\b'), ('deeplearning', '\btransformer\b'), ('deeplearning', '\bdiffusion model\w*\b'),

    ('sap', '\bsap\b'),
    ('etl', '\betl\b'), ('etl', '\belt\b'),
    ('datawarehouse', '\bdata warehouse\b'), ('datawarehouse', '\bdwh\b'), ('datawarehouse', '\bdata lakehouse\b'), ('datawarehouse', '\bdata lake\b')
) AS a(skill_key, pattern)
WHERE s.key = a.skill_key;
