-- Links global labels to individual BPMN elements in a test case.
-- An element can have multiple labels and a label can be assigned to many elements.
create table evaluation_data_label
(
    evaluation_data_id bigint not null
        references evaluation_data(id) on delete cascade,

    element_id text not null,

    label_id bigint not null
        references labels(id) on delete restrict,

    primary key (evaluation_data_id, element_id, label_id)
);

-- Supports later checks of where a label is used.
create index idx_evaluation_data_label_label_id
    on evaluation_data_label (label_id);

-- Migrates the existing predefined GDPR classifications from the JSON field
-- to the new relational label assignments.
insert into evaluation_data_label (
    evaluation_data_id,
    element_id,
    label_id
)
select
    evaluation_data.id,
    expected_value ->> 'value',
    labels.id
from evaluation_data
cross join lateral jsonb_array_elements(evaluation_data.expected_values) as expected_value
cross join lateral jsonb_array_elements_text(
    coalesce(expected_value -> 'classification', '[]'::jsonb)
) as classification(name)
join labels
    on labels.name = classification.name
   and labels.predefined = true
on conflict do nothing;