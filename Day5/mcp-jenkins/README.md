# MCP Jenkins (read-only) lab

Two read-only tools over MCP stdio, backed by Jenkins GET calls with a mock
fallback so it runs anywhere.

- server.py       the MCP server: get_build_status(job), list_deployments(env)
- client_test.py  a minimal MCP client that discovers and calls the tools

## Run
    pip install mcp --break-system-packages
    cd mcp-jenkins
    python3 client_test.py            # uses MOCK data

## Live Jenkins (read-only API token)
    export JENKINS_URL=http://localhost:8080
    export JENKINS_USER=jegan
    export JENKINS_TOKEN=<api-token>  # Jenkins > user > Configure > API Token
    python3 client_test.py

## Wire into an AI client (Claude Desktop / any MCP client)
Add to the client's MCP server config:
    {
      "mcpServers": {
        "jenkins": {
          "command": "python3",
          "args": ["/home/jegan/mcp-jenkins/server.py"],
          "env": { "JENKINS_URL": "http://localhost:8080",
                   "JENKINS_USER": "jegan", "JENKINS_TOKEN": "<token>" }
        }
      }
    }

## Full agent loop (needs ANTHROPIC_API_KEY)
    pip install anthropic --break-system-packages
    export ANTHROPIC_API_KEY=...        # already in your .bashrc
    cd mcp-jenkins
    python3 agent.py "Did the last build of web-tier-build pass?"
    python3 agent.py "What are the last deployments to prod?"

The model receives the MCP tools, decides which to call, the client executes the
real MCP tool, and the model answers from the result. The API key lives only in
this client; the MCP server never sees it.

## FREE hands-on version (trainees) - Google Gemini
No paid key needed. Each trainee:
  1. go to https://aistudio.google.com, sign in with a Google account (no card)
  2. create an API key
  3. export it and run:
       pip install google-genai mcp --break-system-packages
       export GEMINI_API_KEY=<free-key>
       cd mcp-jenkins
       python3 agent_gemini.py "Did the last build of web-tier-build pass?"

Same server.py, different brain. Free tier ~1500 requests/day, plenty for a lab.
Note: on the free tier Google may use prompts to improve its models; don't paste
anything sensitive.

## Three brains, one server (teaching point)
  client_test.py    no AI  - hardcoded calls, proves MCP plumbing
  agent_gemini.py   FREE   - Google Gemini free tier (trainees)
  agent.py          PAID   - Anthropic (instructor demo)
server.py is byte-for-byte identical for all three: MCP servers are model-agnostic.
