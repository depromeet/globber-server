import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest


SCRIPT = Path(__file__).resolve().parents[1] / 'refresh-deployment-proxy.sh'
FAKE_COMMAND = '''#!/usr/bin/env python3
import json, os, sys
from pathlib import Path
name=Path(sys.argv[0]).name
args=sys.argv[1:]
with open(os.environ['TEST_TRACE'], 'a') as f:
    f.write(json.dumps([name, *args])+'\\n')
if name=='systemctl':
    sys.exit(0 if os.environ.get('TEST_HOST_ACTIVE')=='1' else 3)
if name=='docker' and args[0]=='ps':
    print(os.environ.get('TEST_NPM', 'proxy-id jc21/nginx-proxy-manager:latest'))
if name=='sudo' and args[-1]=='-t':
    sys.exit(int(os.environ.get('TEST_NGINX_FAILURE', '0')))
if name=='curl':
    print(os.environ.get('TEST_HEALTH', '{"status":"UP"}'))
    print('200')
'''


class DeploymentProxyTests(unittest.TestCase):
    def run_script(self, mode, **variables):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            command = root / 'fake'
            command.write_text(FAKE_COMMAND)
            command.chmod(0o755)
            for name in ('docker', 'systemctl', 'sudo', 'curl', 'sleep'):
                (root / name).symlink_to(command)
            trace = root / 'trace'
            env = {**os.environ, 'PATH': str(root) + ':' + os.environ['PATH'],
                   'TEST_TRACE': str(trace), 'DEPLOYMENT_PROXY': 'auto',
                   'PUBLIC_HEALTH_URL': 'https://example.test/health', **variables}
            result = subprocess.run(['bash', str(SCRIPT), mode], env=env,
                                    capture_output=True, text=True, timeout=15)
            actions = [json.loads(line) for line in trace.read_text().splitlines()] if trace.exists() else []
            return result, actions

    def test_auto_uses_active_host_nginx_without_requiring_npm(self):
        result, actions = self.run_script('preflight', TEST_HOST_ACTIVE='1', TEST_NPM='')
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn(['sudo', '-n', '/usr/sbin/nginx', '-t'], actions)
        self.assertFalse(any(action[0] == 'docker' for action in actions))

    def test_host_refresh_checks_backend_then_reloads_then_checks_public_url(self):
        result, actions = self.run_script('refresh', DEPLOYMENT_PROXY='host')
        self.assertEqual(result.returncode, 0, result.stderr)
        checks = [i for i, action in enumerate(actions) if action[0] == 'curl']
        reload_action = ['sudo', '-n', '/usr/bin/systemctl', 'reload', 'nginx']
        self.assertIn(reload_action, actions)
        reload_index = actions.index(reload_action)
        self.assertEqual(len(checks), 2)
        self.assertLess(checks[0], reload_index)
        self.assertLess(reload_index, checks[1])
        self.assertEqual(actions[checks[0]][-1], 'http://localhost:8080/actuator/health')
        self.assertEqual(actions[checks[1]][-1], 'https://example.test/health')

    def test_invalid_host_configuration_fails_preflight_without_reload(self):
        result, actions = self.run_script('preflight', DEPLOYMENT_PROXY='host', TEST_NGINX_FAILURE='1')
        self.assertNotEqual(result.returncode, 0)
        self.assertIn(['sudo', '-n', '/usr/sbin/nginx', '-t'], actions)
        self.assertFalse(any('reload' in action for action in actions))

    def test_unhealthy_backend_does_not_reload_proxy(self):
        result, actions = self.run_script('refresh', DEPLOYMENT_PROXY='host', TEST_HEALTH='{"status":"DOWN"}')
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse(any('reload' in action for action in actions))

    def test_auto_preserves_existing_npm_deployment(self):
        result, actions = self.run_script('refresh')
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn(['docker', 'exec', 'proxy-id', 'nginx', '-s', 'reload'], actions)

    def test_invalid_mode_performs_no_external_operations(self):
        result, actions = self.run_script('typo')
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(actions, [])

    def test_explicit_npm_does_not_select_active_host(self):
        result, actions = self.run_script('preflight', DEPLOYMENT_PROXY='npm', TEST_HOST_ACTIVE='1')
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn(['docker', 'exec', 'proxy-id', 'nginx', '-t'], actions)

    def test_missing_npm_fails_before_any_reload(self):
        result, actions = self.run_script('preflight', DEPLOYMENT_PROXY='npm', TEST_NPM='')
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse(any('reload' in action for action in actions))


if __name__ == '__main__':
    unittest.main()
