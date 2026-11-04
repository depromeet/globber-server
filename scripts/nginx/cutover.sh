#!/usr/bin/env bash
set -euo pipefail
(( EUID == 0 )) || { echo 'Run as root' >&2; exit 1; }
state_dir=/etc/globber-nginx
test -x "$state_dir/rollback.sh"
test -x "$state_dir/recover-if-failed.sh"
if [[ "${1:-}" != --phase ]]; then
test ! -f /run/globber-nginx-trial.pid
if systemctl is-active --quiet nginx; then
  echo 'Host Nginx is already active; no cutover performed'
  exit 0
fi
# A future checkout of the old CD script would fail after NPM is stopped.
deployed_script=$(mktemp)
trap 'rm -f "$deployed_script"' EXIT
curl -fsS --max-time 15 https://raw.githubusercontent.com/depromeet/globber-server/main/scripts/refresh-deployment-proxy.sh > "$deployed_script"
if ! cmp -s "$deployed_script" "$state_dir/refresh-deployment-proxy.sh"; then
  echo 'Merge the prepared deployment-proxy script into main before cutover.' >&2
  exit 1
fi
rm -f "$deployed_script"
trap - EXIT
/usr/sbin/nginx -t
test -s /etc/letsencrypt/renewal/globber.world.conf
systemctl is-active --quiet certbot.timer
cd /home/minug0320/17th-team1-client
docker compose -f docker-compose-prod.yml -f docker-compose.secrets.yml config --quiet
# The prepared server-side Compose file must keep this mapping on later deployments.
grep -q '127.0.0.1:3000:80' docker-compose-prod.yml
systemd-run --unit=globber-nginx-cutover --collect --wait \
  --property=Type=exec --property=RuntimeMaxSec=240 \
  --property=KillSignal=SIGKILL --property=TimeoutStopSec=300 --property=KillMode=control-group \
  --property="ExecStopPost=$state_dir/recover-if-failed.sh" \
  /bin/bash "$state_dir/cutover.sh" --phase
exit 0
fi
cd /home/minug0320/17th-team1-client
docker compose -f docker-compose-prod.yml -f docker-compose.secrets.yml up -d --no-deps --no-build --pull never frontend
for attempt in $(seq 1 30); do
  if curl --http1.1 -fsS --max-time 3 http://127.0.0.1:3000/login -o /dev/null; then break; fi
  sleep 1
done
curl --http1.1 -fsS --max-time 5 http://127.0.0.1:3000/login -o /dev/null
curl --http1.1 -fsS --max-time 5 http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"'
docker stop --timeout 10 minug0320-app-1
systemctl unmask nginx
systemctl start nginx
for domain in globber.world www.globber.world; do
  curl --http1.1 -fsS --max-time 10 --resolve "$domain:443:127.0.0.1" "https://$domain/login" -o /dev/null
  curl --http1.1 -fsS --max-time 10 --resolve "$domain:443:127.0.0.1" "https://$domain/api/actuator/health" | grep -q '"status":"UP"'
done
for path in /api/places /api/geocode; do
  status=$(curl --http1.1 -sS --max-time 10 --resolve globber.world:443:127.0.0.1 -o /dev/null -w '%{http_code}' "https://globber.world$path")
  [[ "$status" == 400 ]]
done
# This verifies the final host webroot through public port 80 with the ACME CA.
timeout 150 certbot renew --cert-name globber.world --webroot-path /var/www/letsencrypt --dry-run --non-interactive --no-random-sleep-on-renew
python3 - <<'PY'
from configobj import ConfigObj
config = ConfigObj('/etc/letsencrypt/renewal/globber.world.conf')
params = config['renewalparams']
params['webroot_path'] = ['/var/www/letsencrypt']
params['webroot_map'] = {domain: '/var/www/letsencrypt' for domain in ('globber.world', 'www.globber.world')}
config.write()
PY
systemctl enable nginx
docker update --restart=no minug0320-app-1 >/dev/null
echo 'Host Nginx cutover and certificate renewal validation passed'
