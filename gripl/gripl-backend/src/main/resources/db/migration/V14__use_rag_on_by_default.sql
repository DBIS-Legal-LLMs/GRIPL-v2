-- "Use RAG" is now on by default for Process Analysis settings.
--
-- Only the column default changes. Rows that already exist hold what the user
-- saved (an explicit choice, including "off") and are left alone.
-- Keep in sync with UserAnalysisSettings.DEFAULT_USE_RAG.
alter table user_analysis_settings
    alter column use_rag set default true;
