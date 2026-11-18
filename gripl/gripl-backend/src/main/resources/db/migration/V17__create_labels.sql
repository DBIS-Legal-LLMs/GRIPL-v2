create table labels
(
    id          bigserial primary key,
    name        text        not null,
    description text        not null,
    predefined  boolean     not null default false,

    -- User IDs from auth-service (JWT sub).
    -- Predefined labels have no creating user.
    created_by  text,
    created_at  timestamptz not null default now(),
    updated_by  text,
    updated_at  timestamptz not null default now(),
    deleted_by  text,
    deleted_at  timestamptz,

    constraint labels_name_not_blank
        check (length(trim(name)) > 0),

    constraint labels_description_not_blank
        check (length(trim(description)) > 0),

    constraint labels_predefined_not_deleted
        check (not predefined or deleted_at is null),

    constraint labels_deletion_audit
        check (
            (deleted_at is null and deleted_by is null)
            or
            (deleted_at is not null and deleted_by is not null)
        )
);

-- Active label names must be unique, ignoring case and outer spaces.
-- Deleted labels retain their IDs, their names may be reused.
create unique index idx_labels_active_name
    on labels (lower(trim(name)))
    where deleted_at is null;

insert into labels (name, description, predefined)
values
    (
        'COLLECTION',
        'Gathers personal information (e.g., names, emails, health data)',
        true
    ),
    (
        'STORAGE',
        'Saves personal data in databases, systems, or files',
        true
    ),
    (
        'USAGE',
        'Employs personal data for operational or analytical purposes',
        true
    ),
    (
        'TRANSFERAL',
        'Shares personal data with third parties or externally',
        true
    ),
    (
        'MODIFICATION',
        'Updates, corrects, or alters previously stored personal data',
        true
    ),
    (
        'DELETION',
        'Removes or anonymizes personal data from storage',
        true
    ),
    (
        'ACCESS',
        'Retrieves or makes personal data available to users or systems',
        true
    );