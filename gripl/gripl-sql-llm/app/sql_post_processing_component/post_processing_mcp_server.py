from mcp.server.fastmcp import FastMCP
from app.sql_execution_component.sql_execution import SQLExecution
import uvicorn


class MCPServer:

    def __init__(self,
                 sql_execution: SQLExecution,
                 ):
        self.mcp = FastMCP("sql-post-processing-mcp",
                           host="0.0.0.0",
                           port=7000,
                           )
        self.register_tools()
        self.execution_component = sql_execution

    def register_tools(self):
        @self.mcp.tool()
        def get_all_intentions() -> list[str]:
            """
            Get all intentions.

            :return: list of all intentions.
            """
            return [category_result.get("name", "") for category_result in
                    self.execution_component.get_sql_query_results(
                        "SELECT name  FROM category"
                    )]

        @self.mcp.tool()
        def get_all_reasons_of_category(category_name: str) -> list[str]:
            """
                        Get of one intention all possible reasons.

                        :return: list of all reasons of one intention.
                        """

            return [reason_result.get("reason", "") for reason_result in self.execution_component.get_sql_query_results(
                f"""
                                                                                        SELECT  r.reason
                                                                                        FROM reason r
                                                                                                 JOIN category_reason_association cra
                                                                                                      ON r.id = cra.reason_id
                                                                                                 JOIN category c
                                                                                                      ON c.id = cra.category_id
                                                                                        WHERE c.name = '{category_name}';
                                                                                        """
            )]

    def start(self):
        self.mcp.run(transport="sse")


if __name__ == "__main__":
    sql_execution_component = SQLExecution()
    server = MCPServer(sql_execution_component)
    server.start()
