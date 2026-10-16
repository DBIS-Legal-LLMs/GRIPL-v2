-- Store the effective defaults instead of NULL for the LLM base URL, model,
-- temperature and top-p of user_analysis_settings (V11). Rows saved before
-- this migration get the defaults filled in; the seed stays nullable
-- (null = not seeded). Keep in sync with UserAnalysisSettings' defaults.
update user_analysis_settings
set llm_base_url = coalesce(llm_base_url, 'https://openrouter.ai/api/v1'),
    model_name   = coalesce(model_name, 'openai/gpt-oss-20b'),
    temperature  = coalesce(temperature, 1.0),
    top_p        = coalesce(top_p, 1.0);

alter table user_analysis_settings
    alter column llm_base_url set default 'https://openrouter.ai/api/v1',
    alter column llm_base_url set not null,
    alter column model_name set default 'openai/gpt-oss-20b',
    alter column model_name set not null,
    alter column temperature set default 1.0,
    alter column temperature set not null,
    alter column top_p set default 1.0,
    alter column top_p set not null;
