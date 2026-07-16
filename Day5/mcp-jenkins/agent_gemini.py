"""
agent_gemini.py - FREE hands-on version for trainees.

Same MCP server (server.py, unchanged). The brain is Google Gemini's free tier
instead of a paid Anthropic key. Get a free key at https://aistudio.google.com
(sign in with a Google account, no credit card), then:

    export GEMINI_API_KEY=<your-free-key>
    python3 agent_gemini.py "Did the last build of web-tier-build pass?"

Note: on Google's FREE tier, prompts may be used to improve Google's models.
Fine for this lab; don't paste anything sensitive.
"""
import os
import sys
import asyncio

from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client
from google import genai
from google.genai import types

MODEL = "gemini-2.5-flash"   # free-tier, supports function calling


def mcp_tools_to_gemini(mcp_tools):
    """Translate MCP tool schemas into Gemini function declarations."""
    decls = []
    for t in mcp_tools:
        schema = t.inputSchema or {"type": "object", "properties": {}}
        decls.append(types.FunctionDeclaration(
            name=t.name,
            description=(t.description or "").splitlines()[0],
            parameters=schema,
        ))
    return [types.Tool(function_declarations=decls)]


async def run(question: str):
    key = os.environ.get("GEMINI_API_KEY")
    if not key:
        print("set GEMINI_API_KEY to run the agent.\n"
              "Get a free key at https://aistudio.google.com "
              "(Google account, no credit card).")
        return

    server = StdioServerParameters(command="python3", args=["server.py"])
    async with stdio_client(server) as (read, write):
        async with ClientSession(read, write) as session:
            await session.initialize()
            listed = await session.list_tools()
            tools = mcp_tools_to_gemini(listed.tools)
            names = [t.name for t in listed.tools]
            print(f"[discovered MCP tools: {', '.join(names)}]\n")

            client = genai.Client(api_key=key)
            contents = [types.Content(role="user",
                                      parts=[types.Part(text=question)])]

            while True:
                resp = client.models.generate_content(
                    model=MODEL, contents=contents,
                    config=types.GenerateContentConfig(tools=tools),
                )
                parts = resp.candidates[0].content.parts
                calls = [p.function_call for p in parts if p.function_call]

                if not calls:
                    print("ANSWER:", (resp.text or "").strip())
                    return

                contents.append(resp.candidates[0].content)  # model's turn
                for fc in calls:
                    args = dict(fc.args)
                    print(f"[model calls {fc.name}({args})]")
                    result = await session.call_tool(fc.name, args)
                    payload = result.content[0].text
                    print(f"[tool returned] {payload}\n")
                    contents.append(types.Content(role="user", parts=[
                        types.Part(function_response=types.FunctionResponse(
                            name=fc.name, response={"result": payload}))]))


if __name__ == "__main__":
    q = " ".join(sys.argv[1:]) or "Did the last build of web-tier-build pass?"
    print(f"QUESTION: {q}\n")
    asyncio.run(run(q))
