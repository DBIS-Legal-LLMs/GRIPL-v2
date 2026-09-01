create table process_model (
    id SERIAL PRIMARY KEY NOT NULL,
    name varchar(255) not null,
    bpmn_xml text not null,
    status varchar(20) not null default 'PENDING',
    analysis_endpoint varchar(255),
    analysis_options jsonb,
    analysis_result jsonb,
    amount_of_retries integer,
    total_elements integer,
    critical_element_count integer,
    error_message text,
    created_at timestamp with time zone default now(),
    updated_at timestamp with time zone default now()
);

create index idx_process_model_status on process_model(status);
