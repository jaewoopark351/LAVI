#20260707_kpopmodder: Added StarCraft2 external process adapter tests without launching real bots.
import unittest
import os
from unittest import mock

from plugins.StarCraft2.starcraft2_core.ares_sc2_bot_engine import (
    AresSC2BotEngine,
)
from plugins.StarCraft2.starcraft2_core.external_exe_bot_engine import (
    ExternalExeBotEngine,
)
from plugins.StarCraft2.starcraft2_core.external_jar_bot_engine import (
    ExternalJarBotEngine,
)
from plugins.StarCraft2.starcraft2_core.micromachine_bot_engine import (
    MicroMachineBotEngine,
)
from plugins.StarCraft2.starcraft2_core.starcraft2_contracts import (
    EngineResultDTO,
    EngineStatusDTO,
    StarCraft2Event,
)


class FakeProcess:
    def __init__(self, pid=4242):
        self.pid = pid
        self.stdout = []
        self.stderr = []
        self.returncode = None
        self.terminated = False
        self.killed = False

    def poll(self):
        return None if self.returncode is None else self.returncode

    def terminate(self):
        self.terminated = True
        self.returncode = 0

    def wait(self, timeout=None):
        self.returncode = 0
        return self.returncode

    def kill(self):
        self.killed = True
        self.returncode = -9


class StarCraft2ExternalEnginesTests(unittest.TestCase):
    def test_external_exe_start_stop_status_with_mock_process(self):
        fake_process = FakeProcess()
        engine = ExternalExeBotEngine()
        config = {
            "external_exe": {
                "path": "C:\\Bots\\MicroMachine.exe",
                "working_directory": "",
                "args": ["--ladder"],
            }
        }

        with mock.patch(
            "plugins.StarCraft2.starcraft2_core.external_exe_bot_engine.subprocess.Popen",
            return_value=fake_process,
        ) as popen:
            start_result = engine.start(config)
            status = engine.get_status()
            stop_result = engine.stop()

        self.assertIsInstance(start_result, EngineResultDTO)
        self.assertIsInstance(status, EngineStatusDTO)
        self.assertIsInstance(stop_result, EngineResultDTO)
        self.assertTrue(start_result.ok)
        self.assertTrue(status.running)
        self.assertEqual(4242, status.to_dict()["process_pid"])
        self.assertTrue(stop_result.ok)
        self.assertTrue(fake_process.terminated)
        popen.assert_called_once()
        self.assertEqual(
            ["C:\\Bots\\MicroMachine.exe", "--ladder"],
            popen.call_args.args[0],
        )

    def test_external_jar_start_stop_status_with_mock_process(self):
        fake_process = FakeProcess(pid=5151)
        engine = ExternalJarBotEngine()
        config = {
            "external_jar": {
                "java_path": "java",
                "jar_path": "C:\\Bots\\KetrocBot.jar",
                "working_directory": "",
                "args": ["--OpponentId", "LAV"],
            }
        }

        with mock.patch(
            "plugins.StarCraft2.starcraft2_core.external_exe_bot_engine.subprocess.Popen",
            return_value=fake_process,
        ) as popen:
            start_result = engine.start(config)
            status = engine.get_status()
            stop_result = engine.stop()

        self.assertIsInstance(start_result, EngineResultDTO)
        self.assertIsInstance(status, EngineStatusDTO)
        self.assertIsInstance(stop_result, EngineResultDTO)
        self.assertTrue(start_result.ok)
        self.assertTrue(status.running)
        self.assertEqual(5151, status.to_dict()["process_pid"])
        self.assertTrue(stop_result.ok)
        self.assertEqual(
            ["java", "-jar", "C:\\Bots\\KetrocBot.jar", "--OpponentId", "LAV"],
            popen.call_args.args[0],
        )

    def test_micromachine_uses_exe_folder_and_requires_bot_config(self):
        fake_process = FakeProcess(pid=6161)
        engine = MicroMachineBotEngine()
        exe_path = os.path.normpath(
            "C:\\Bots\\MicroMachine\\CommandCenter_API.exe"
        )
        working_directory = os.path.dirname(exe_path)
        bot_config_path = os.path.join(working_directory, "BotConfig.txt")
        config = {
            "micromachine": {
                "path": exe_path,
                "args": ["--ladder"],
                "auto_add_executable_arg": False,
            }
        }

        def fake_isfile(path):
            return os.path.normpath(path) in {exe_path, bot_config_path}

        with mock.patch(
            "plugins.StarCraft2.starcraft2_core.external_exe_bot_engine.subprocess.Popen",
            return_value=fake_process,
        ) as popen, mock.patch(
            "plugins.StarCraft2.starcraft2_core.micromachine_bot_engine.os.path.isfile",
            side_effect=fake_isfile,
        ), mock.patch(
            "plugins.StarCraft2.starcraft2_core.micromachine_bot_engine.os.path.isdir",
            return_value=True,
        ):
            start_result = engine.start(config)
            status = engine.get_status()
            stop_result = engine.stop()

        self.assertIsInstance(start_result, EngineResultDTO)
        self.assertIsInstance(status, EngineStatusDTO)
        self.assertIsInstance(stop_result, EngineResultDTO)
        self.assertTrue(start_result.ok)
        self.assertTrue(status.running)
        self.assertEqual(6161, status.to_dict()["process_pid"])
        self.assertTrue(stop_result.ok)
        self.assertEqual([exe_path, "--ladder"], popen.call_args.args[0])
        self.assertEqual(working_directory, popen.call_args.kwargs["cwd"])

    def test_micromachine_adds_starcraft2_executable_arg_from_versions(self):
        fake_process = FakeProcess(pid=6262)
        engine = MicroMachineBotEngine()
        exe_path = os.path.normpath(
            "C:\\Bots\\MicroMachine\\CommandCenter.exe"
        )
        working_directory = os.path.dirname(exe_path)
        bot_config_path = os.path.join(working_directory, "BotConfig.txt")
        starcraft2_path = os.path.normpath("C:\\Program Files (x86)\\StarCraft II")
        versions_dir = os.path.join(starcraft2_path, "Versions")
        base_older = os.path.join(versions_dir, "Base90000")
        base_newer = os.path.join(versions_dir, "Base97425")
        sc2_executable = os.path.join(base_newer, "SC2_x64.exe")
        config = {
            "starcraft2_path": starcraft2_path,
            "micromachine": {
                "path": exe_path,
                "args": ["--ladder"],
            },
        }

        directories = {working_directory, starcraft2_path, versions_dir, base_older, base_newer}
        files = {
            exe_path,
            bot_config_path,
            os.path.join(base_older, "SC2_x64.exe"),
            sc2_executable,
        }

        def fake_isdir(path):
            return os.path.normpath(path) in directories

        def fake_isfile(path):
            return os.path.normpath(path) in files

        def fake_listdir(path):
            if os.path.normpath(path) == versions_dir:
                return ["Base90000", "Base97425"]
            return []

        with mock.patch(
            "plugins.StarCraft2.starcraft2_core.external_exe_bot_engine.subprocess.Popen",
            return_value=fake_process,
        ) as popen, mock.patch(
            "plugins.StarCraft2.starcraft2_core.micromachine_bot_engine.os.path.isfile",
            side_effect=fake_isfile,
        ), mock.patch(
            "plugins.StarCraft2.starcraft2_core.micromachine_bot_engine.os.path.isdir",
            side_effect=fake_isdir,
        ), mock.patch(
            "plugins.StarCraft2.starcraft2_core.micromachine_bot_engine.os.listdir",
            side_effect=fake_listdir,
        ):
            start_result = engine.start(config)
            stop_result = engine.stop()

        self.assertIsInstance(start_result, EngineResultDTO)
        self.assertIsInstance(stop_result, EngineResultDTO)
        self.assertTrue(start_result.ok)
        self.assertTrue(stop_result.ok)
        self.assertEqual(
            [exe_path, "-e", sc2_executable, "--ladder"],
            popen.call_args.args[0],
        )

    def test_micromachine_reports_missing_bot_config_before_launch(self):
        engine = MicroMachineBotEngine()
        exe_path = os.path.normpath(
            "C:\\Bots\\MicroMachine\\CommandCenter_API.exe"
        )
        config = {"micromachine": {"path": exe_path}}

        def fake_isfile(path):
            return os.path.normpath(path) == exe_path

        with mock.patch(
            "plugins.StarCraft2.starcraft2_core.external_exe_bot_engine.subprocess.Popen",
        ) as popen, mock.patch(
            "plugins.StarCraft2.starcraft2_core.micromachine_bot_engine.os.path.isfile",
            side_effect=fake_isfile,
        ), mock.patch(
            "plugins.StarCraft2.starcraft2_core.micromachine_bot_engine.os.path.isdir",
            return_value=True,
        ):
            result = engine.start(config)

        self.assertIsInstance(result, EngineResultDTO)
        self.assertFalse(result.ok)
        self.assertIn("micromachine_bot_config_missing", result.error)
        popen.assert_not_called()

    def test_ares_sc2_runs_python_script_from_script_folder(self):
        fake_process = FakeProcess(pid=7171)
        engine = AresSC2BotEngine()
        script_path = os.path.normpath("C:\\Bots\\AresBot\\run.py")
        python_path = os.path.normpath("C:\\Bots\\AresBot\\.venv\\Scripts\\python.exe")
        config = {
            "map_name": "outsider",
            "race": "Terran",
            "enemy_race": "Zerg",
            "enemy_difficulty": "Easy",
            "realtime": True,
            "ares_sc2": {
                "script_path": script_path,
                "python_path": python_path,
                "args": ["--GamePort", "5000"],
            }
        }

        def fake_isfile(path):
            return os.path.normpath(path) in {script_path, python_path}

        with mock.patch(
            "plugins.StarCraft2.starcraft2_core.external_exe_bot_engine.subprocess.Popen",
            return_value=fake_process,
        ) as popen, mock.patch(
            "plugins.StarCraft2.starcraft2_core.ares_sc2_bot_engine.os.path.isfile",
            side_effect=fake_isfile,
        ), mock.patch(
            "plugins.StarCraft2.starcraft2_core.external_exe_bot_engine.os.path.isdir",
            return_value=True,
        ):
            start_result = engine.start(config)
            stop_result = engine.stop()

        self.assertIsInstance(start_result, EngineResultDTO)
        self.assertIsInstance(stop_result, EngineResultDTO)
        self.assertTrue(start_result.ok)
        self.assertTrue(stop_result.ok)
        self.assertEqual(
            [
                python_path,
                script_path,
                "--map",
                "outsider",
                "--race",
                "Terran",
                "--enemy-race",
                "Zerg",
                "--enemy-difficulty",
                "Easy",
                "--realtime",
                "--GamePort",
                "5000",
            ],
            popen.call_args.args[0],
        )
        self.assertEqual(os.path.dirname(script_path), popen.call_args.kwargs["cwd"])

    def test_ares_sc2_reports_missing_script_before_launch(self):
        engine = AresSC2BotEngine()
        config = {"ares_sc2": {"script_path": "C:\\Bots\\AresBot\\run.py"}}

        with mock.patch(
            "plugins.StarCraft2.starcraft2_core.external_exe_bot_engine.subprocess.Popen",
        ) as popen, mock.patch(
            "plugins.StarCraft2.starcraft2_core.ares_sc2_bot_engine.os.path.isfile",
            return_value=False,
        ):
            result = engine.start(config)

        self.assertIsInstance(result, EngineResultDTO)
        self.assertFalse(result.ok)
        self.assertIn("ares_sc2_script_not_found", result.error)
        popen.assert_not_called()

    def test_ares_sc2_reports_missing_poetry_before_launch(self):
        engine = AresSC2BotEngine()
        script_path = os.path.normpath("C:\\Bots\\AresBot\\run.py")
        config = {
            "ares_sc2": {
                "script_path": script_path,
                "use_poetry": True,
                "poetry_path": "missing-poetry",
            }
        }

        with mock.patch(
            "plugins.StarCraft2.starcraft2_core.external_exe_bot_engine.subprocess.Popen",
        ) as popen, mock.patch(
            "plugins.StarCraft2.starcraft2_core.ares_sc2_bot_engine.os.path.isfile",
            return_value=True,
        ), mock.patch(
            "plugins.StarCraft2.starcraft2_core.ares_sc2_bot_engine.os.path.isdir",
            return_value=True,
        ), mock.patch(
            "plugins.StarCraft2.starcraft2_core.ares_sc2_bot_engine.shutil.which",
            return_value=None,
        ):
            result = engine.start(config)

        self.assertIsInstance(result, EngineResultDTO)
        self.assertFalse(result.ok)
        self.assertIn("ares_sc2_poetry_not_found", result.error)
        popen.assert_not_called()

    #20260715_kpopmodder: External process callbacks preserve the typed event path.
    def test_external_process_event_callback_receives_typed_event(self):
        fake_process = FakeProcess(pid=5252)
        engine = ExternalExeBotEngine()
        events = []
        config = {
            "external_exe": {
                "path": "C:\\Bots\\MicroMachine.exe",
                "working_directory": "",
            }
        }

        with mock.patch(
            "plugins.StarCraft2.starcraft2_core.external_exe_bot_engine.subprocess.Popen",
            return_value=fake_process,
        ):
            result = engine.start(config, event_callback=events.append)
            engine.stop()

        self.assertIsInstance(result, EngineResultDTO)
        self.assertIsInstance(events[0], StarCraft2Event)
        self.assertEqual("process_started", events[0].event_type)
        self.assertEqual(5252, events[0].details["pid"])


if __name__ == "__main__":
    unittest.main()
