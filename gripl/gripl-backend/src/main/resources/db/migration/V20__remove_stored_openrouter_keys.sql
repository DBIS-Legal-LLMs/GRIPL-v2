-- The user's OpenRouter API key used to be copied into the analysis options of a queued
-- process model and stayed there (GRIPL#62). The key is now only held in memory while the
-- job is queued or running; remove the ones already stored.
update process_model
set analysis_options = analysis_options #- '{llmProps,apiKey}'
where analysis_options #> '{llmProps,apiKey}' is not null;
