-- Process models can be edited and saved after they were analyzed. The stored
-- analysis result is kept until the next run, but flagged as outdated so the
-- UI can warn that it no longer matches the saved diagram.
alter table process_model
    add column analysis_outdated boolean not null default false;
