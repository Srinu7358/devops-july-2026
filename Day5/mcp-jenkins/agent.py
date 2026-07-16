"""
agent.py - the piece that needs ANTHROPIC_API_KEY.

Flow:
  1. spawn the MCP server (server.py) over stdio
  2. discover its tools, convert MCP tool schemas -> Anthropic tool format
  3. send the user's natural-language question + tools to the model
  4. when the model returns a tool_use, call the real MCP tool, feed the result
     back, and let the model answer from it

The API key is read from the environment (your .bashrc export). The MCP server
itself never sees the key; only this client does.
"""
import os
import sys
import asyncio

from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client
import anthropic

MODEL = "claude-sonnet-4-5"   # adjust to a model your key can access


async def run(question: str):
    if not os.environ.get("ANTHROPIC_API_KEY"):
        print("set ANTHROPIC_API_KEY to run the agent (it's in your .bashrc; "
              "start a new shell or `source ~/.bashrc`).")
        return

    server = StdioServerParameters(command="python3", args=["server.py"])
    async with stdio_client(server) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()

            # discover MCP tools and translate to Anthropic tool schema
            listed = await session.list_tools()
            tools = [{
                "name": t.name,
                "description": (t.description or "").splitlines()[0],
                "input_schema": t.inputSchema,
            } for t in listed.tools]
            print(f"[discovered {len(tools)} MCP tools: "
                  f"{', '.join(t['name'] for t in tools)}]\n")

            client = anthropic.Anthropic()
            messages = [{"role": "user", "content": question}]

            # agent loop: keep going while the model asks for tools
            while True:
                resp = client.messages.create(
                    model=MODEL, max_tokens=1024,
                    tools=tools, messages=messages,
                )
                messages.append({"role": "assistant", "content": resp.content})

                tool_uses = [b for b in resp.content if b.type == "tool_use"]
                if not tool_uses:
                    text = "".join(b.text for b in resp.content if b.type == "text")
                    print("ANSWER:", text.strip())
                    return

                tool_results = []
                for tu in tool_uses:
                    print(f"[model calls {tu.name}({tu.input})]")
                    result = await session.call_tool(tu.name, tu.input)
                    payload = result.content[0].text
                    print(f"[tool returned] {payload}\n")
                    tool_results.append({
                        "type": "tool_result",
                        "tool_use_id": tu.id,
                        "content": payload,
                    })
                messages.append({"role": "user", "content": tool_results})


if __name__ == "__main__":
    q = " ".join(sys.argv[1:]) or "Did the last build of web-tier-build pass?"
    print(f"QUESTION: {q}\n")
    asyncio.run(run(q))
