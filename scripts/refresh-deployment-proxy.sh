#!/usr/bin/env bash
set -euo pipefail

mode=${1:?Usage: refresh-deployment-proxy.sh preflight|refresh}
case "$mode" in
  preflight|refresh) ;;
  *) echo "Unknown mode: $mode" >&2; exit 1 ;;
esac

proxy_kind=${DEPLOYMENT_PROXY:-auto}
if [[ "$proxy_kind" == auto ]]; then
  if command -v systemctl >/dev/null && systemctl is-active --quiet nginx; then
    proxy_kind=host
  else
    proxy_kind=npm
  fi
fi
case "$proxy_kind" in
  host) ;;
  npm)
    # Container names differ between self-hosted runners.
    proxies=()
    while IFS= read -r id; do proxies+=("$id"); done < <(docker ps --format '{{.ID}} {{.Image}}' |
      awk '$2 ~ /^jc21\/nginx-proxy-manager(:|@|$)/ {print $1}')
    if (( ${#proxies[@]} != 1 )); then
      echo "Expected exactly one running Nginx Proxy Manager, found ${#proxies[@]}" >&2
      exit 1
    fi
    proxy=${proxies[0]}
    ;;
  *) echo "Unknown DEPLOYMENT_PROXY: $proxy_kind" >&2; exit 1 ;;
esac

check_proxy() {
  if [[ "$proxy_kind" == host ]]; then
    sudo -n /usr/sbin/nginx -t
  else
    docker exec "$proxy" nginx -t
  fi
}

check_proxy

if [[ "$mode" == preflight ]]; then
  exit 0
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
check_proxy
if [[ "$proxy_kind" == host ]]; then
  sudo -n /usr/bin/systemctl reload nginx
else
  docker exec "$proxy" nginx -s reload
fi
wait_for_health "${PUBLIC_HEALTH_URL:?PUBLIC_HEALTH_URL is required}" 12
echo "Backend and public proxy health checks passed"
