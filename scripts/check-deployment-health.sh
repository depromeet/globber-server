#!/usr/bin/env bash
set -euo pipefail

: "${PUBLIC_HEALTH_URL:?PUBLIC_HEALTH_URL is required}"

wait_for_health() {
  local url=$1 attempts=$2 body attempt
  for ((attempt=1; attempt<=attempts; attempt++)); do
    # Redirects, HTML login pages and non-UP responses must fail deployment.
    if body=$(curl --fail --silent --show-error --connect-timeout 2 --max-time 5 --write-out '\n%{http_code}' "$url") &&
       [[ "${body##*$'\n'}" == 200 ]] &&
       printf '%s' "$body" | grep -Eq '"status"[[:space:]]*:[[:space:]]*"UP"'; then
      return 0
    fi
    if (( attempt < attempts )); then sleep 5; fi
  done
  echo "Health check failed: $url" >&2
  return 1
}

wait_for_health http://localhost:8080/actuator/health 24
wait_for_health "$PUBLIC_HEALTH_URL" 12
echo 'Backend local and public health checks passed'
