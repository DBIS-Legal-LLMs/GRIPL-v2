def get_db_schema_string() -> str:

    return """\
Table: category
  - id (Integer, primary key)
  - name (String(255), unique, not null)

Table: reason
  - id (Integer, primary key)
  - reason (Text, not null, unique)

Table: category_reason_association
  - category_id (Integer, FK -> category.id, primary key)
  - reason_id (Integer, FK -> reason.id, primary key)

Relationships:
  - category.reasons <-> reason.categories (many-to-many via category_reason_association)
"""