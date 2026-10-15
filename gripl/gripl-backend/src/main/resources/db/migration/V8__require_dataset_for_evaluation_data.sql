-- Every test case must belong to a dataset. Existing test cases without a dataset are moved into
-- a dedicated dataset instead of being deleted, so they can be reviewed (and deleted) in the UI.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM evaluation_data WHERE dataset_id IS NULL) THEN
        WITH unassigned AS (
            INSERT INTO dataset (name, description)
            VALUES ('Unassigned (migrated)', 'Test cases that were not assigned to any dataset before migration V8.')
            RETURNING id
        )
        UPDATE evaluation_data SET dataset_id = (SELECT id FROM unassigned) WHERE dataset_id IS NULL;
    END IF;
END $$;

ALTER TABLE evaluation_data ALTER COLUMN dataset_id SET NOT NULL;

-- Deleting a dataset deletes its test cases (previously: ON DELETE SET NULL)
ALTER TABLE evaluation_data DROP CONSTRAINT IF EXISTS evaluation_data_dataset_id_fkey;
ALTER TABLE evaluation_data
    ADD CONSTRAINT evaluation_data_dataset_id_fkey
        FOREIGN KEY (dataset_id) REFERENCES dataset (id) ON DELETE CASCADE;
