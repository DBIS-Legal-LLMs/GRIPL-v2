create table custom_analysis_endpoint (
    id SERIAL PRIMARY KEY NOT NULL,
    name varchar(255) not null,
    prompt_text text not null,
    response_type varchar(20) not null,
    created_at timestamp with time zone default now(),
    updated_at timestamp with time zone default now()
);
