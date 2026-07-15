#!/bin/bash
# Proves path-based routing from the access logs of each tier.
echo "=== Sending traffic to port 80 only ==="
curl -s http://localhost/            | grep "Served by"
curl -s http://localhost/web/hello   | grep "Served by"
curl -s http://localhost/api/orders  | grep tier
curl -s http://localhost/api/customers/42 | grep tier

echo
echo "=== WEB TIER access log (last 4 lines) ==="
sudo tail -4 /srv/web/logs/web_access.$(date +%F).log 2>/dev/null

echo
echo "=== APP TIER access log (last 4 lines) ==="
sudo tail -4 /srv/app/logs/app_access.$(date +%F).log 2>/dev/null

echo
echo "The proof: /api requests appear ONLY in the APP TIER log."
echo "The WEB TIER log stays silent for them. That silence is the point."
