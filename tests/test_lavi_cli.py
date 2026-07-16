#20260716_kpopmodder: Verify python -m lavi dispatch without launching Gradio.
import sys
import unittest
from unittest import mock

from lavi import cli


class LaviCliTests(unittest.TestCase):
    def test_default_command_runs_app(self):
        with mock.patch("lavi.cli._run_app", return_value=0) as run_app:
            self.assertEqual(0, cli.main([]))

        run_app.assert_called_once_with()

    def test_app_command_runs_app(self):
        with mock.patch("lavi.cli._run_app", return_value=0) as run_app:
            self.assertEqual(0, cli.main(["app"]))

        run_app.assert_called_once_with()

    def test_smoke_command_delegates_remaining_args(self):
        with mock.patch("lavi.cli._run_smoke", return_value=3) as run_smoke:
            self.assertEqual(
                3,
                cli.main(["smoke", "--profile", "Core", "--offline"]),
            )

        run_smoke.assert_called_once_with(["--profile", "Core", "--offline"])

    def test_none_argv_uses_process_arguments(self):
        with mock.patch.object(
            sys,
            "argv",
            ["python", "smoke", "--profile", "Core"],
        ):
            with mock.patch("lavi.cli._run_smoke", return_value=5) as run_smoke:
                self.assertEqual(5, cli.main())

        run_smoke.assert_called_once_with(["--profile", "Core"])

    def test_doctor_command_strips_separator(self):
        with mock.patch("lavi.cli._run_preflight", return_value=4) as run_preflight:
            self.assertEqual(
                4,
                cli.main(["doctor", "--", "--accelerator", "CPU"]),
            )

        run_preflight.assert_called_once_with(["--accelerator", "CPU"])


if __name__ == "__main__":
    unittest.main()
