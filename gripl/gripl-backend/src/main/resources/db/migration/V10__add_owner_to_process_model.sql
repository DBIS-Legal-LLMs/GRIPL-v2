-- Process Analysis models are private to the auth-service user (JWT `sub`)
-- that uploaded them, like datasets (V8) — previously every user could list,
-- read, analyze and delete every process model.
--
-- Nullable, no backfill: rows that predate this migration have owner_user_id
-- NULL and are owned by nobody, so they no longer appear in any owner-scoped
-- endpoint until an owner is assigned by hand, e.g.
--   UPDATE process_model SET owner_user_id = '<auth-service user id>'
--   WHERE owner_user_id IS NULL;
alter table process_model
    add column owner_user_id text;

create index idx_process_model_owner_user_id on process_model (owner_user_id);
