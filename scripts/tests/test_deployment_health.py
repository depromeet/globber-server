import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest


SCRIPT = Path(__file__).resolve().parents[1] / 'check-deployment-health.sh'


class DeploymentHealthTests(unittest.TestCase):
    def run_check(self, scenario='healthy', public_url='https://example.test/health'):
        self.assertTrue(SCRIPT.exists(), 'Deployment health checker is not implemented')
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            fake = root / 'fake'
            fake.write_text(f'#!{sys.executable}\n' + '''
import json, os, sys
from pathlib import Path
name=Path(sys.argv[0]).name
with open(os.environ['TEST_TRACE'], 'a') as f:
    f.write(json.dumps([name, *sys.argv[1:]])+'\\n')
if name=='curl':
    url=sys.argv[-1]
    scenario=os.environ['TEST_SCENARIO']
    if scenario=='local_down' and 'localhost' in url:
        print('{"status":"DOWN"}\\n200')
    elif scenario=='public_html' and 'example.test' in url:
        print('<html>Login</html>\\n200')
    elif scenario=='public_redirect' and 'example.test' in url:
        print('{"status":"UP"}\\n302')
    else:
        print('{"status":"UP"}\\n200')
''')
            fake.chmod(0o755)
            for name in ('curl', 'sleep', 'docker', 'sudo', 'systemctl'):
                (root / name).symlink_to(fake)
            trace = root / 'trace'
            env = {**os.environ, 'PATH': str(root) + ':' + os.environ['PATH'],
                   'TEST_TRACE': str(trace), 'TEST_SCENARIO': scenario,
                   'PUBLIC_HEALTH_URL': public_url}
            result = subprocess.run(['bash', str(SCRIPT)], env=env,
                                    capture_output=True, text=True, timeout=15)
            actions = [json.loads(line) for line in trace.read_text().splitlines()] if trace.exists() else []
            self.assertFalse(any(action[0] in ('docker', 'sudo', 'systemctl') for action in actions))
            return result, actions

    def test_checks_local_then_public_without_controlling_proxy(self):
        result, actions = self.run_check()
        self.assertEqual(result.returncode, 0, result.stderr)
        urls = [action[-1] for action in actions if action[0] == 'curl']
        self.assertEqual(urls, ['http://localhost:8080/actuator/health', 'https://example.test/health'])

    def test_local_down_fails_before_public_check(self):
        result, actions = self.run_check('local_down')
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse(any('example.test' in action[-1] for action in actions))

    def test_public_html_is_not_healthy(self):
        result, _ = self.run_check('public_html')
        self.assertNotEqual(result.returncode, 0)

    def test_redirect_is_not_healthy(self):
        result, _ = self.run_check('public_redirect')
        self.assertNotEqual(result.returncode, 0)

    def test_missing_public_url_fails_before_any_requests(self):
        result, actions = self.run_check(public_url='')
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(actions, [])


if __name__ == '__main__':
    unittest.main()
