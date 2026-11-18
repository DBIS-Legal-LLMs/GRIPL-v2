alter table labels
    add column color text;

comment on column labels.color is 'Custom hex color for custom labels; predefined labels use their GDPR class color instead.';
