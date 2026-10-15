-- Custom analysis endpoints are private to the auth-service user (JWT `sub`)
-- that created them, like datasets (V8) and process models (V9) — previously
-- every user saw, ran and could delete everyone else's.
--
-- Nullable, no backfill: rows that predate this migration have owner_user_id
-- NULL and are owned by nobody, so they disappear from every owner-scoped
-- endpoint until assigned by hand, e.g.
--   UPDATE custom_analysis_endpoint SET owner_user_id = '<auth-service user id>'
--   WHERE owner_user_id IS NULL;
alter table custom_analysis_endpoint
    add column owner_user_id text;

create index idx_custom_analysis_endpoint_owner_user_id on custom_analysis_endpoint (owner_user_id);
