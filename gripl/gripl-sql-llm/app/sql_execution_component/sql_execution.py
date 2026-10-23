import sqlite3
from pathlib import Path

class SQLExecution:

    def __init__(self):
        project_root = Path(__file__).resolve().parents[2]
        self.db_path = project_root / "test.db"


    def get_sql_query_results(self, sql):
        try:
            with sqlite3.connect(self.db_path) as conn:
                conn.row_factory = sqlite3.Row
                cursor = conn.cursor()
                cursor.execute(sql)

                rows = cursor.fetchall()

                return [dict(row) for row in rows]

        except sqlite3.Error as e:
            return {"error": str(e)}

    def insert_sql(self, sql, params=None):

        try:
            with sqlite3.connect(self.db_path) as conn:
                cursor = conn.cursor()
                if params:
                    cursor.execute(sql, params)
                else:
                    cursor.execute(sql)
                conn.commit()
        except sqlite3.Error as e:
            return {"error": str(e)}

    def delete_sql(self, sql, params=None):
        try:
            with sqlite3.connect(self.db_path) as conn:
                cursor = conn.cursor()

                if params:
                    cursor.execute(sql, params)
                else:
                    cursor.execute(sql)

                conn.commit()

                return {
                    "success": True,
                    "deleted_rows": cursor.rowcount
                }
        except sqlite3.Error as e:
            return {
                "error": str(e)
            }