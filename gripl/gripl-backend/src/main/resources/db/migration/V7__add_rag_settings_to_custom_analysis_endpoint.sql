alter table custom_analysis_endpoint
    add column rag_enabled boolean not null default false,
    add column rag_mode varchar(20);
