"""Minimal MCP client: spawn the server over stdio, discover tools, call them.
This is the mechanical core of what an AI client does when the model asks for a tool."""
import asyncio
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client


async def main():
    params = StdioServerParameters(command="python3", args=["server.py"])
    async with stdio_client(params) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()

            # 1. discover
            tools = await session.list_tools()
            print("=== tools discovered ===")
            for t in tools.tools:
                params_schema = t.inputSchema.get("properties", {})
                print(f"  {t.name}({', '.join(params_schema)}) - {t.description.splitlines()[0]}")

            # 2. invoke get_build_status
            print("\n=== call: get_build_status(job='web-tier-build') ===")
            r1 = await session.call_tool("get_build_status", {"job": "web-tier-build"})
            print(r1.content[0].text)

            # 3. invoke list_deployments
            print("\n=== call: list_deployments(env='prod') ===")
            r2 = await session.call_tool("list_deployments", {"env": "prod"})
            print(r2.content[0].text)


if __name__ == "__main__":
    asyncio.run(main())
