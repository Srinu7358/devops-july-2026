#!/usr/bin/env bash
# Verify both tiers are healthy and the gate enforces the pushed allow-list.
set -u
WEB=9091
APP=9092

code() { curl -s -o /dev/null -w "%{http_code}" "$1"; }

echo "== allow-list the web tier holds =="
curl -s http://127.0.0.1:$WEB/admin/allowlist

echo
echo "== approved paths through the web tier (expect 200) =="
echo "  /api/products : $(code http://127.0.0.1:$WEB/api/products)"
echo "  /api/orders   : $(code http://127.0.0.1:$WEB/api/orders)"

echo "== hosted-but-unapproved path =="
echo "  app tier direct  (expect 200): $(code http://127.0.0.1:$APP/api/internal/metrics)"
echo "  web tier gateway (expect 403): $(code http://127.0.0.1:$WEB/api/internal/metrics)"
