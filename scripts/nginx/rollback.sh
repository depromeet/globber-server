#!/usr/bin/env bash
set -euo pipefail
restore_proxy() {
  local ancillary_error=0
  systemctl stop nginx || ancillary_error=1
  # Restore traffic before attempting certificate or boot-policy bookkeeping.
  docker update --restart=unless-stopped minug0320-app-1 >/dev/null || ancillary_error=1
  docker start minug0320-app-1 >/dev/null || return 1
  for attempt in $(seq 1 90); do
    if docker exec minug0320-app-1 test -s /run/nginx/nginx.pid; then break; fi
    sleep 1
  done
  docker exec minug0320-app-1 test -s /run/nginx/nginx.pid || return 1
  docker exec minug0320-app-1 nginx -t || return 1
  docker exec minug0320-app-1 nginx -s reload || return 1
  for attempt in $(seq 1 30); do
    if curl --http1.1 -fsS --max-time 3 --resolve globber.world:443:127.0.0.1 https://globber.world/login -o /dev/null; then break; fi
    sleep 1
  done
  curl --http1.1 -fsS --max-time 5 --resolve globber.world:443:127.0.0.1 https://globber.world/login -o /dev/null || return 1
  curl --http1.1 -fsS --max-time 5 --resolve globber.world:443:127.0.0.1 https://globber.world/api/actuator/health | grep -q '"status":"UP"' || return 1
  systemctl disable nginx || ancillary_error=1
  systemctl mask nginx || ancillary_error=1
  python3 - <<'PY' || ancillary_error=1
from configobj import ConfigObj
config = ConfigObj('/etc/letsencrypt/renewal/globber.world.conf')
root = '/home/minug0320/data/letsencrypt-acme-challenge'
params = config['renewalparams']
params['webroot_path'] = [root]
params['webroot_map'] = {domain: root for domain in ('globber.world', 'www.globber.world')}
config.write()
PY
  echo 'NPM traffic restoration verified'
  if (( ancillary_error )); then
    echo 'Traffic restored; certificate or boot-policy cleanup needs attention' >&2
    return 1
  fi
}

if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
  (( EUID == 0 )) || { echo 'Run as root' >&2; exit 1; }
  # Manual recovery first stops the whole supervised cutover phase.
  # The callback must not synchronously stop the service that is invoking it.
  if [[ "${1:-}" != --from-systemd ]] && systemctl is-active --quiet globber-nginx-cutover.service; then
    systemctl stop globber-nginx-cutover.service
  fi
  exec 9>/run/globber-nginx-rollback.lock
  flock 9
  restore_proxy
fi
