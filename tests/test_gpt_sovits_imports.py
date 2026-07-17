import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import Mock, patch

#20260626_kpopmodder: Keep GPT-SoVITS child-process GPU env behavior covered.

PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))


from plugin_system.interfaces import TTSPluginInterface
from plugins.GPTSoVITS.GPTSoVITS import GPTSoVITS
from plugins.GPTSoVITS.GPTSoVITS_TTS import GPTSoVITSTTS
from plugins.GPTSoVITS.gpt_sovits_core.gpt_sovits_api_client import (
    GPTSoVITSApiClient,
)
from plugins.GPTSoVITS.gpt_sovits_core.gpt_sovits_client import (
    GPTSoVITSClient,
)
from plugins.GPTSoVITS.gpt_sovits_core.gpt_sovits_config import (
    GPTSoVITSConfig,
)
from plugins.GPTSoVITS.gpt_sovits_core.gpt_sovits_config_manager import (
    GPTSoVITSConfigManager,
)
from plugins.GPTSoVITS.gpt_sovits_core.gpt_sovits_model_manager import (
    GPTSoVITSModelManager,
)
from plugins.GPTSoVITS.gpt_sovits_core.gpt_sovits_process_manager import (
    GPTSoVITSProcessManager,
)
from plugins.GPTSoVITS.gpt_sovits_core.gpt_sovits_server_manager import (
    GPTSoVITSServerManager,
)
from plugins.GPTSoVITS.gpt_sovits_core.gpt_sovits_tts_provider import (
    GPTSoVITSTTSProvider,
)


class GPTSoVITSImportTests(unittest.TestCase):
    def test_plugin_entry_class_imports(self):
        self.assertTrue(issubclass(GPTSoVITS, TTSPluginInterface))

    def test_tts_uses_core_helper_classes(self):
        tts = GPTSoVITSTTS()

        self.assertIsInstance(tts.config, GPTSoVITSConfig)
        self.assertIsInstance(tts.config_manager, GPTSoVITSConfigManager)
        self.assertIsInstance(tts.process_manager, GPTSoVITSProcessManager)
        self.assertIs(tts.server_manager, tts.process_manager)
        self.assertIsInstance(tts.model_manager, GPTSoVITSModelManager)
        self.assertIsInstance(tts.api_client, GPTSoVITSClient)
        self.assertIsInstance(tts.api_client, GPTSoVITSApiClient)
        self.assertEqual(tts.server_manager.cuda_visible_devices, "1")

    def test_legacy_server_manager_name_wraps_process_manager(self):
        manager = GPTSoVITSServerManager(
            config_manager=Mock(),
            gpt_sovits_url="http://127.0.0.1:9880/tts",
            cuda_visible_devices="1",
        )

        self.assertIsInstance(manager, GPTSoVITSProcessManager)

    def test_tts_construction_does_not_probe_or_launch_server(self):
        with patch(
            "plugins.GPTSoVITS.gpt_sovits_core.gpt_sovits_process_manager.requests.get",
        ) as requests_get:
            with patch(
                "plugins.GPTSoVITS.gpt_sovits_core.gpt_sovits_process_manager.launch_process",
            ) as launch_process:
                GPTSoVITSTTS()

        requests_get.assert_not_called()
        launch_process.assert_not_called()

    def test_url_change_is_propagated_without_starting_server(self):
        tts = GPTSoVITSTTS()
        new_url = "http://127.0.0.1:19999/tts"

        tts.gpt_sovits_url = new_url

        self.assertEqual(tts.server_manager.gpt_sovits_url, new_url)
        self.assertEqual(tts.model_manager.gpt_sovits_url, new_url)
        self.assertEqual(tts.api_client.gpt_sovits_url, new_url)

    def test_server_manager_passes_cuda_visible_devices_to_child_env(self):#20260626_kpopmodder
        config_manager = Mock()
        config_manager.check_install.return_value = True
        manager = GPTSoVITSProcessManager(
            config_manager=config_manager,
            gpt_sovits_url="http://127.0.0.1:9880/tts",
            cuda_visible_devices="1",
        )

        response = Mock()
        response.status_code = 200

        with patch.object(manager, "is_server_alive", return_value=False):
            with patch(
                "plugins.GPTSoVITS.gpt_sovits_core.gpt_sovits_process_manager.requests.get",
                return_value=response,
            ):
                with patch(
                    "plugins.GPTSoVITS.gpt_sovits_core.gpt_sovits_process_manager.launch_process"
                ) as launch_mock:
                    manager.start_server("C:\\GPT-SoVITS")

        env = launch_mock.call_args.kwargs["env"]
        self.assertEqual(env["CUDA_VISIBLE_DEVICES"], "1")

    def test_server_manager_blocks_start_without_cuda_visible_devices(self):#20260627_kpopmodder
        config_manager = Mock()
        config_manager.check_install.return_value = True
        manager = GPTSoVITSProcessManager(
            config_manager=config_manager,
            gpt_sovits_url="http://127.0.0.1:9880/tts",
            cuda_visible_devices=None,
        )

        with patch.object(manager, "is_server_alive") as alive_mock:
            with patch(
                "plugins.GPTSoVITS.gpt_sovits_core."
                "gpt_sovits_process_manager.log_print"
            ) as log_mock:
                with patch(
                    "plugins.GPTSoVITS.gpt_sovits_core."
                    "gpt_sovits_process_manager.launch_process"
                ) as launch_mock:
                    manager.start_server("C:\\GPT-SoVITS")

        alive_mock.assert_not_called()
        launch_mock.assert_not_called()
        self.assertIsNone(manager.gpt_sovits_process)
        logged = "\n".join(str(call.args[0]) for call in log_mock.call_args_list)
        self.assertIn("ERROR", logged)
        self.assertIn("GPU 0", logged)

    def test_tts_provider_delegates_runtime_lifecycle(self):
        runtime = Mock()
        runtime.synthesize_to_bytes.return_value = b"wav"
        runtime.gpt_sovits_url = "http://127.0.0.1:9880/tts"
        runtime.gpt_sovits_root = "C:\\GPT-SoVITS"
        runtime.process_manager = Mock()
        runtime.process_manager.is_process_running.return_value = True
        runtime.process_manager.cuda_visible_devices = "1"
        provider = GPTSoVITSTTSProvider(runtime)

        provider.init()
        result = provider.synthesize("hello")
        diagnostics = provider.diagnostics()
        provider.shutdown()

        runtime.init.assert_called_once()
        runtime.synthesize_to_bytes.assert_called_once_with("hello")
        runtime.stop_server.assert_called_once()
        self.assertEqual(b"wav", result)
        self.assertTrue(diagnostics["initialized"])
        self.assertTrue(diagnostics["process_running"])

    def test_config_manager_validates_local_cuda_visible_devices(self):#20260626_kpopmodder
        temp_dir = tempfile.TemporaryDirectory()
        self.addCleanup(temp_dir.cleanup)
        config_dir = Path(temp_dir.name)
        config_path = config_dir / "gpt_sovits_config.json"
        config_path.write_text(
            '{"gpt_sovits_root": "", "cuda_visible_devices": "1"}',
            encoding="utf-8",
        )
        manager = GPTSoVITSConfigManager(
            config_dir=str(config_dir),
            config_path=str(config_path),
            default_config={
                "gpt_sovits_root": "",
                "cuda_visible_devices": "1",
            },
        )

        with patch(
            "plugins.GPTSoVITS.gpt_sovits_core.gpt_sovits_config_manager."
            "gpu_device_manager.get_cuda_visible_devices",
            return_value=None,
        ):
            with patch(
                "plugins.GPTSoVITS.gpt_sovits_core.gpt_sovits_config_manager."
                "gpu_device_manager.validate_cuda_visible_devices",
                return_value=None,
            ) as validate_mock:
                self.assertIsNone(manager.load_cuda_visible_devices())

        validate_mock.assert_called_once_with(
            "1",
            "GPTSoVITS",
            default=None,
        )

    def test_config_manager_prefers_gpt_sovits_root_env(self):#20260627_kpopmodder
        temp_dir = tempfile.TemporaryDirectory()
        self.addCleanup(temp_dir.cleanup)
        config_dir = Path(temp_dir.name)
        config_path = config_dir / "gpt_sovits_config.json"
        config_path.write_text(
            '{"gpt_sovits_root": "", "cuda_visible_devices": "1"}',
            encoding="utf-8",
        )
        manager = GPTSoVITSConfigManager(
            config_dir=str(config_dir),
            config_path=str(config_path),
            default_config={
                "gpt_sovits_root": "",
                "cuda_visible_devices": "1",
            },
        )

        with patch.dict(
            "os.environ",
            {"GPT_SOVITS_ROOT": "C:\\GPT-SoVITS"},
        ):
            self.assertEqual(
                "C:\\GPT-SoVITS",
                manager.load_root_path(),
            )


if __name__ == "__main__":
    unittest.main()
