#!/usr/bin/env bash
set -euo pipefail
# ExecStopPost runs after systemd has terminated the phase's entire cgroup.
if [[ "${SERVICE_RESULT:-unknown}" != success ]]; then
  script_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
  exec "$script_dir/rollback.sh" --from-systemd
fi
