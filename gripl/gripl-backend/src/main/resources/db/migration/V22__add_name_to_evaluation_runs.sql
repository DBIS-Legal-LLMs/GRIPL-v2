-- A user-given name for an evaluation run, shown in the run list and used
-- for the file names of downloaded reports. Optional and can be changed later.
alter table evaluation_run add column name text;
