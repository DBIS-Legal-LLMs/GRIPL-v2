-- Process Analysis settings (LLM base URL / model, sampling params, RAG) are
-- per auth-service user (JWT `sub`) — previously they lived in the browser's
-- localStorage under one global key, so they were shared by everyone using
-- that browser and lost on any other device.
--
-- One row per user; a row existing is what "configured" means. NULL columns
-- mean "use the server default".
create table user_analysis_settings
(
    owner_user_id text primary key,
    llm_base_url  text,
    model_name    text,
    seed          integer,
    temperature   double precision,
    top_p         double precision,
    use_rag       boolean     not null default false,
    rag_mode      text        not null default 'hybrid',
    updated_at    timestamptz not null default now()
);
