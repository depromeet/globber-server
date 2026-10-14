#!/usr/bin/env bash
set -euo pipefail

mode=${1:?Usage: refresh-deployment-proxy.sh preflight|refresh}

# Container names differ between self-hosted runners. Require one unambiguous NPM.
proxies=()
while IFS= read -r id; do proxies+=("$id"); done < <(docker ps --format '{{.ID}} {{.Image}}' |
  awk '$2 ~ /^jc21\/nginx-proxy-manager(:|@|$)/ {print $1}')
if (( ${#proxies[@]} != 1 )); then
  echo "Expected exactly one running Nginx Proxy Manager, found ${#proxies[@]}" >&2
  exit 1
fi
proxy=${proxies[0]}
docker exec "$proxy" nginx -t

if [[ "$mode" == preflight ]]; then
  exit 0
fi
if [[ "$mode" != refresh ]]; then
  echo "Unknown mode: $mode" >&2
  exit 1
fi

wait_for_health() {
  local url=$1 attempts=$2 body attempt
  for ((attempt=1; attempt<=attempts; attempt++)); do
    # Reject redirects, HTML login pages and non-UP health responses.
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

: "${PUBLIC_HEALTH_URL:?PUBLIC_HEALTH_URL is required}"
wait_for_health http://localhost:8080/actuator/health 24
# Graceful reload re-resolves static upstream names after container replacement.
docker exec "$proxy" nginx -t
docker exec "$proxy" nginx -s reload
wait_for_health "${PUBLIC_HEALTH_URL:?PUBLIC_HEALTH_URL is required}" 12
echo "Backend and public proxy health checks passed"
