-- GRIPL-v2#33: private per-user dataset ownership.
--
-- Datasets (and, through their parent dataset_id, test cases) are now scoped to
-- the auth-service user id (JWT `sub`) that created them, instead of being
-- globally visible/editable by anyone with API access.
--
-- Nullable, no backfill: rows that predate this migration have owner_user_id
-- NULL and are therefore owned by nobody -- they will not appear in the
-- owner-scoped list/get endpoints until an owner is assigned (e.g. a one-off
--   UPDATE dataset SET owner_user_id = '<your auth-service user id>'
--   WHERE owner_user_id IS NULL;
-- run by hand against the DB). The evaluation-run code paths still read datasets
-- unfiltered; endpoint-level role gating for those is GRIPL-v2#40.
alter table dataset
    add column owner_user_id text;

create index idx_dataset_owner_user_id on dataset (owner_user_id);
