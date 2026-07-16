#!/usr/bin/env bash
# Verify the gateway enforces the reloaded allow-list.
WEB=8080; APP=9090
code() { curl -s -o /dev/null -w "%{http_code}" "$1"; }

echo "== active allow-list (in memory) =="
curl -s http://127.0.0.1:$WEB/gateway/active

echo "== on-disk plugin-allow.conf =="
curl -s http://127.0.0.1:$WEB/gateway/conf

echo "== through the gateway (web tier) =="
echo "  /shop    : $(code http://127.0.0.1:$WEB/shop)"
echo "  /reports : $(code http://127.0.0.1:$WEB/reports)"
echo "  /admin   : $(code http://127.0.0.1:$WEB/admin)"

echo "== app tier direct (all hosted, all 200) =="
echo "  /shop    : $(code http://127.0.0.1:$APP/shop)"
echo "  /reports : $(code http://127.0.0.1:$APP/reports)"
echo "  /admin   : $(code http://127.0.0.1:$APP/admin)"
