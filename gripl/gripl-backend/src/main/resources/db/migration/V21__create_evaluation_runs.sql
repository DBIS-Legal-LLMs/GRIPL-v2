-- Evaluation runs are backend jobs: they keep running when the browser
-- disconnects, and every finished test case is stored, so partial results
-- survive a cancel or a restart and an interrupted run can be resumed.
-- The OpenRouter API key is never stored here (see V20); the request is
-- persisted with the key removed.

create table evaluation_run (
    id BIGSERIAL PRIMARY KEY,
    owner_user_id text not null,
    status varchar(20) not null default 'QUEUED',
    request jsonb not null,
    metadata jsonb,
    total_test_cases integer,
    error_message text,
    created_at timestamp with time zone not null default now(),
    started_at timestamp with time zone,
    finished_at timestamp with time zone,
    updated_at timestamp with time zone not null default now()
);

create index idx_evaluation_run_owner on evaluation_run(owner_user_id, created_at desc);

-- At most one active run per user.
create unique index uq_evaluation_run_active_per_owner
    on evaluation_run(owner_user_id)
    where status in ('QUEUED', 'RUNNING');

-- testCase, error and summary events in the same JSON form the NDJSON stream
-- sends. stepInfo events are progress only and are not stored.
create table evaluation_run_event (
    id BIGSERIAL PRIMARY KEY,
    run_id bigint not null references evaluation_run(id) on delete cascade,
    run_number integer not null,
    model_label text not null,
    type varchar(20) not null,
    test_case_id bigint,
    payload jsonb not null,
    -- EvaluationMetrics of a testCase event; needed to rebuild the summary when resuming.
    metrics jsonb,
    created_at timestamp with time zone not null default now()
);

create index idx_evaluation_run_event_run on evaluation_run_event(run_id, id);

create unique index uq_evaluation_run_event_test_case
    on evaluation_run_event(run_id, run_number, model_label, test_case_id)
    where type = 'testCase';
