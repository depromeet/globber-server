#!/usr/bin/env bash
set -euo pipefail
# Certbot executes deploy hooks as root. NPM remains active until cutover.
if systemctl is-active --quiet nginx; then
  /usr/sbin/nginx -t
  systemctl reload nginx
fi
