import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


SCRIPTS = Path(__file__).resolve().parents[1] / 'nginx'


class RecoveryHookTests(unittest.TestCase):
    def invoke(self, result):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            self.assertTrue((SCRIPTS / 'recover-if-failed.sh').exists(), 'Recovery callback is not implemented')
            shutil.copyfile(SCRIPTS / 'recover-if-failed.sh', root / 'recover-if-failed.sh')
            marker = root / 'rollback-ran'
            rollback = root / 'rollback.sh'
            rollback.write_text('#!/bin/sh\ntouch "$TEST_MARKER"\n')
            rollback.chmod(0o755)
            proc = subprocess.run(['bash', str(root / 'recover-if-failed.sh')],
                                  env={**os.environ, 'SERVICE_RESULT': result, 'TEST_MARKER': str(marker)},
                                  capture_output=True, text=True)
            self.assertEqual(proc.returncode, 0, proc.stderr)
            return marker.exists()

    def test_successful_transition_is_kept(self):
        self.assertFalse(self.invoke('success'))

    def test_timed_out_transition_is_rolled_back(self):
        self.assertTrue(self.invoke('timeout'))

    def test_failed_transition_is_rolled_back(self):
        self.assertTrue(self.invoke('exit-code'))

    def test_certificate_or_disable_failure_does_not_skip_npm_restore(self):
        for failure in ('python3', 'disable'):
            with self.subTest(failure=failure), tempfile.TemporaryDirectory() as directory:
                root = Path(directory)
                trace = root / 'trace'
                command = root / 'fake'
                command.write_text('''#!/bin/sh
name=${0##*/}
printf '%s %s\\n' "$name" "$*" >> "$TEST_TRACE"
if [ "$name" = "$TEST_FAILURE" ] || [ "$1" = "$TEST_FAILURE" ]; then exit 1; fi
if [ "$name" = curl ]; then printf '{"status":"UP"}'; fi
''')
                command.chmod(0o755)
                for name in ('systemctl', 'docker', 'curl', 'python3', 'sleep'):
                    (root / name).symlink_to(command)
                proc = subprocess.run(['bash', '-c', 'source "$1"; restore_proxy', 'bash', str(SCRIPTS / 'rollback.sh')],
                                      env={**os.environ, 'PATH': str(root) + ':' + os.environ['PATH'],
                                           'TEST_TRACE': str(trace), 'TEST_FAILURE': failure},
                                      capture_output=True, text=True)
                actions = trace.read_text().splitlines() if trace.exists() else []
                self.assertNotEqual(proc.returncode, 0)
                self.assertIn('docker start minug0320-app-1', actions)
                self.assertTrue(any(action.startswith('curl ') for action in actions))


if __name__ == '__main__':
    unittest.main()
