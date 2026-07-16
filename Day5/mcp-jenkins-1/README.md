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
