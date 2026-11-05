-- Optimistic locking for saving a process model's diagram: a save must name the
-- version it was based on and bumps it. Only diagram saves bump it (not analysis
-- status changes or renames), so two editors of the same model can't silently
-- overwrite each other without triggering false conflicts.
alter table process_model
    add column bpmn_version integer not null default 0;
