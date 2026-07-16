"""
MCP server exposing two READ-ONLY Jenkins tools:
  get_build_status(job)      -> last build result for a job
  list_deployments(env)      -> recent deployments to an environment

Read-only by design: it only issues HTTP GETs to Jenkins. There is no deploy
tool here. Config via environment:
  JENKINS_URL   e.g. http://localhost:8080
  JENKINS_USER, JENKINS_TOKEN   (API token, not password)
If JENKINS_URL is unset or unreachable, the tools return clearly-labelled MOCK
data so the lab runs anywhere.
"""
import os
import json
import urllib.request
import urllib.error
import base64

from mcp.server.fastmcp import FastMCP

JENKINS_URL = os.environ.get("JENKINS_URL", "").rstrip("/")
JENKINS_USER = os.environ.get("JENKINS_USER", "")
JENKINS_TOKEN = os.environ.get("JENKINS_TOKEN", "")

mcp = FastMCP("jenkins-readonly")


def _get(path: str):
    """Issue a single authenticated GET to Jenkins. Returns parsed JSON or None."""
    if not JENKINS_URL:
        return None
    url = f"{JENKINS_URL}{path}"
    req = urllib.request.Request(url, method="GET")
    if JENKINS_USER and JENKINS_TOKEN:
        raw = f"{JENKINS_USER}:{JENKINS_TOKEN}".encode()
        req.add_header("Authorization", "Basic " + base64.b64encode(raw).decode())
    try:
        with urllib.request.urlopen(req, timeout=5) as r:
            return json.loads(r.read().decode())
    except (urllib.error.URLError, ValueError, TimeoutError):
        return None


@mcp.tool()
def get_build_status(job: str) -> dict:
    """Return the status of the most recent build for a Jenkins job.

    Args:
        job: the Jenkins job name, e.g. "web-tier-build".
    """
    data = _get(f"/job/{job}/lastBuild/api/json")
    if data is None:
        # mock fallback so the lab runs without a live Jenkins
        return {
            "job": job,
            "build": 42,
            "result": "SUCCESS",
            "building": False,
            "source": "MOCK (set JENKINS_URL for live data)",
        }
    return {
        "job": job,
        "build": data.get("number"),
        "result": data.get("result"),
        "building": data.get("building"),
        "url": data.get("url"),
        "source": "jenkins",
    }


@mcp.tool()
def list_deployments(env: str) -> dict:
    """List recent deployments to an environment.

    Args:
        env: target environment, one of "dev", "staging", "prod".
    """
    # models a deploy job per environment: deploy-<env>
    data = _get(f"/job/deploy-{env}/api/json?tree=builds[number,result,timestamp]")
    if data is None:
        return {
            "env": env,
            "deployments": [
                {"build": 18, "result": "SUCCESS"},
                {"build": 17, "result": "SUCCESS"},
                {"build": 16, "result": "FAILURE"},
            ],
            "source": "MOCK (set JENKINS_URL for live data)",
        }
    builds = data.get("builds", [])[:5]
    return {
        "env": env,
        "deployments": [
            {"build": b.get("number"), "result": b.get("result")} for b in builds
        ],
        "source": "jenkins",
    }


if __name__ == "__main__":
    mcp.run()   # stdio transport by default
