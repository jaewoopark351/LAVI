import json
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

#20260626_kpopmodder: Verify multi-GPU config fallback without loading real models.

PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))


from core.gpu_device_manager import GPUDeviceManager


class GPUDeviceManagerTests(unittest.TestCase):#20260626_kpopmodder
    class _FakeCuda:
        def __init__(self, available, count, free_bytes=None):
            self.available = available
            self.count = count
            self.free_bytes = free_bytes

        def is_available(self):
            return self.available

        def device_count(self):
            return self.count

        def get_device_name(self, index):
            return f"Fake GPU {index}"

        def mem_get_info(self, index):
            free_bytes = self.free_bytes
            if free_bytes is None:
                free_bytes = 8 * 1024 ** 3
            return free_bytes, 16 * 1024 ** 3

    class _FakeTorch:
        def __init__(self, available, count, free_bytes=None):
            self.cuda = GPUDeviceManagerTests._FakeCuda(
                available,
                count,
                free_bytes=free_bytes,
            )

    def _patch_torch_cuda(
        self,
        manager,
        available=True,
        count=1,
        free_bytes=None,
    ):
        return patch.object(
            manager,
            "_get_torch",
            return_value=self._FakeTorch(
                available,
                count,
                free_bytes=free_bytes,
            ),
        )

    def _manager_with_config(self, config):
        temp_dir = tempfile.TemporaryDirectory()
        self.addCleanup(temp_dir.cleanup)
        config_path = Path(temp_dir.name) / "gpu_device_config.json"
        config_path.write_text(
            json.dumps(config),
            encoding="utf-8",
        )
        manager = GPUDeviceManager(config_path=str(config_path))
        manager._gpu_log_done = True
        return manager

    def test_default_voice_input_device_is_cuda_zero(self):#20260627_kpopmodder
        temp_dir = tempfile.TemporaryDirectory()
        self.addCleanup(temp_dir.cleanup)
        manager = GPUDeviceManager(
            config_path=str(Path(temp_dir.name) / "missing.json")
        )
        manager._gpu_log_done = True

        with self._patch_torch_cuda(manager, available=True, count=2):
            self.assertEqual(manager.get_device("VoiceInput"), "cuda:0")

    def test_configured_device_map_and_max_memory_are_sanitized(self):#20260626_kpopmodder
        manager = self._manager_with_config({
            "ScreenVision": {
                "device": "cuda:1",
                "device_map": {"": 1},
                "max_memory": {"1": "14GiB"},
            }
        })

        with self._patch_torch_cuda(manager, available=True, count=2):
            self.assertEqual(manager.get_device("ScreenVision"), "cuda:1")
            self.assertEqual(manager.get_device_map("ScreenVision"), {"": 1})
            self.assertEqual(
                manager.get_max_memory("ScreenVision"),
                {1: "14GiB"},
            )

    def test_missing_cuda_index_falls_back_without_crashing(self):#20260626_kpopmodder
        manager = self._manager_with_config({
            "ScreenVision": {
                "device": "cuda:1",
                "device_map": {"": 1},
                "max_memory": {"1": "14GiB"},
            }
        })

        with self._patch_torch_cuda(manager, available=True, count=1):
            self.assertEqual(manager.get_device("ScreenVision"), "cuda:0")
            self.assertEqual(manager.get_device_map("ScreenVision"), {"": 0})
            self.assertIsNone(manager.get_max_memory("ScreenVision"))

    def test_missing_config_keeps_caller_device_map_default(self):#20260626_kpopmodder
        temp_dir = tempfile.TemporaryDirectory()
        self.addCleanup(temp_dir.cleanup)
        manager = GPUDeviceManager(
            config_path=str(Path(temp_dir.name) / "missing.json")
        )
        manager._gpu_log_done = True

        self.assertEqual(
            manager.get_device_map("External_LLM", default="auto"),
            "auto",
        )

    def test_invalid_cuda_visible_devices_are_filtered(self):#20260626_kpopmodder
        manager = self._manager_with_config({
            "GPTSoVITS": {
                "cuda_visible_devices": "0,1,abc",
            }
        })

        with self._patch_torch_cuda(manager, available=True, count=1):
            self.assertEqual(manager.get_cuda_visible_devices("GPTSoVITS"), "0")

    def test_invalid_cuda_visible_devices_falls_back_to_none(self):#20260626_kpopmodder
        manager = self._manager_with_config({
            "GPTSoVITS": {
                "cuda_visible_devices": "1",
            }
        })

        with self._patch_torch_cuda(manager, available=True, count=1):
            self.assertIsNone(manager.get_cuda_visible_devices("GPTSoVITS"))

    def test_apply_cuda_visible_devices_updates_child_env(self):#20260717_kpopmodder
        manager = self._manager_with_config({})
        env = {}

        with self._patch_torch_cuda(manager, available=True, count=2):
            resolved = manager.apply_cuda_visible_devices(
                env,
                "Chess",
                "1",
            )

        self.assertEqual("1", resolved)
        self.assertEqual("1", env["CUDA_VISIBLE_DEVICES"])

        env = {}
        resolved = manager.apply_cuda_visible_devices(
            env,
            "GPTSoVITS",
            "1",
            validate=False,
        )

        self.assertEqual("1", resolved)
        self.assertEqual("1", env["CUDA_VISIBLE_DEVICES"])

    def test_cuda_unavailable_uses_cpu_safe_fallbacks(self):#20260626_kpopmodder
        manager = self._manager_with_config({
            "VoiceInput": {
                "device": "cuda:1",
            },
            "ScreenVision": {
                "device_map": {"": 1},
                "max_memory": {"1": "14GiB"},
            },
            "GPTSoVITS": {
                "cuda_visible_devices": "1",
            },
        })

        with self._patch_torch_cuda(manager, available=False, count=0):
            self.assertEqual(manager.get_device("VoiceInput"), "cpu")
            self.assertEqual(manager.get_device_map("ScreenVision"), {"": "cpu"})
            self.assertIsNone(manager.get_max_memory("ScreenVision"))
            self.assertIsNone(manager.get_cuda_visible_devices("GPTSoVITS"))

    def test_external_plugin_gpu_config_regression(self):#20260626_kpopmodder
        manager = self._manager_with_config({
            "External_LLM": {
                "device_map": {"": 0},
                "max_memory": {"0": "14GiB"},
            }
        })

        with self._patch_torch_cuda(manager, available=True, count=1):
            self.assertEqual(
                manager.get_device_map("External_LLM"),
                {"": 0},
            )
            self.assertEqual(
                manager.get_max_memory("External_LLM"),
                {0: "14GiB"},
            )

    def test_startup_vram_preflight_delays_screenvision_when_shared_gpu_low(self):#20260627_kpopmodder
        manager = self._manager_with_config({
            "VoiceInput": {"device": "cuda:1"},
            "ScreenVision": {"device": "cuda:1"},
            "GPTSoVITS": {"cuda_visible_devices": "1"},
            "startup_vram_preflight": {
                "enabled": True,
                "screenvision_min_free_gib": 6.0,
                "delay_screenvision_auto_load": True,
            },
        })
        free_bytes = 4 * 1024 ** 3

        with self._patch_torch_cuda(
            manager,
            available=True,
            count=2,
            free_bytes=free_bytes,
        ):
            result = manager.log_startup_vram_preflight(force=True)

        self.assertEqual(
            ["VoiceInput", "ScreenVision", "GPTSoVITS"],
            result["shared_gpu_plugins"][1],
        )
        self.assertFalse(result["screenvision_auto_load_allowed"])
        self.assertIn("cuda:1", result["screenvision_delay_reason"])

    def test_startup_vram_preflight_allows_screenvision_when_free_vram_ok(self):#20260627_kpopmodder
        manager = self._manager_with_config({
            "VoiceInput": {"device": "cuda:1"},
            "ScreenVision": {"device": "cuda:1"},
            "GPTSoVITS": {"cuda_visible_devices": "1"},
            "startup_vram_preflight": {
                "enabled": True,
                "screenvision_min_free_gib": 6.0,
                "delay_screenvision_auto_load": True,
            },
        })
        free_bytes = 8 * 1024 ** 3

        with self._patch_torch_cuda(
            manager,
            available=True,
            count=2,
            free_bytes=free_bytes,
        ):
            result = manager.log_startup_vram_preflight(force=True)

        self.assertTrue(result["screenvision_auto_load_allowed"])
        self.assertEqual("", result["screenvision_delay_reason"])

    def test_preflight_block_missing_uses_warn_only_defaults(self):#20260628_kpopmodder
        manager = self._manager_with_config({
            "VoiceInput": {"device": "cuda:1"},
            "ScreenVision": {"device": "cuda:1"},
            "GPTSoVITS": {"cuda_visible_devices": "1"},
        })
        free_bytes = 1024 ** 3

        with self._patch_torch_cuda(
            manager,
            available=True,
            count=2,
            free_bytes=free_bytes,
        ):
            result = manager.log_startup_vram_preflight(force=True)

        self.assertTrue(result["enabled"])
        self.assertTrue(result["warn_only"])
        self.assertEqual(3000, result["min_free_vram_mb"])
        self.assertEqual(1024, result["free_vram_mb"][1])
        self.assertTrue(result["screenvision_auto_load_allowed"])
        self.assertEqual("", result["screenvision_delay_reason"])
        self.assertTrue(result["warnings"])

    def test_preflight_explicit_warn_only_does_not_delay_screenvision(self):#20260628_kpopmodder
        manager = self._manager_with_config({
            "VoiceInput": {"device": "cuda:1"},
            "ScreenVision": {"device": "cuda:1"},
            "GPTSoVITS": {"cuda_visible_devices": "1"},
            "preflight": {
                "enabled": True,
                "check_cuda_available": True,
                "check_device_exists": True,
                "check_vram": True,
                "min_free_vram_mb": 6000,
                "warn_only": True,
            },
        })
        free_bytes = 4 * 1024 ** 3

        with self._patch_torch_cuda(
            manager,
            available=True,
            count=2,
            free_bytes=free_bytes,
        ):
            result = manager.log_startup_vram_preflight(force=True)

        self.assertTrue(result["screenvision_auto_load_allowed"])
        self.assertEqual("", result["screenvision_delay_reason"])
        self.assertTrue(any("free VRAM" in item for item in result["warnings"]))

    def test_preflight_cuda_unavailable_warns_without_crashing(self):#20260628_kpopmodder
        manager = self._manager_with_config({
            "VoiceInput": {"device": "cuda:1"},
            "ScreenVision": {"device": "cuda:1"},
            "GPTSoVITS": {"cuda_visible_devices": "1"},
            "preflight": {
                "enabled": True,
                "check_cuda_available": True,
                "check_device_exists": True,
                "check_vram": True,
                "min_free_vram_mb": 3000,
                "warn_only": True,
            },
        })

        with self._patch_torch_cuda(manager, available=False, count=0):
            result = manager.log_startup_vram_preflight(force=True)

        self.assertFalse(result["cuda_available"])
        self.assertEqual(0, result["device_count"])
        self.assertTrue(result["screenvision_auto_load_allowed"])
        self.assertTrue(result["warnings"])

    def test_preflight_logs_clear_startup_lines(self):#20260628_kpopmodder
        manager = self._manager_with_config({
            "VoiceInput": {"device": "cuda:1"},
            "ScreenVision": {"device": "cuda:1"},
            "GPTSoVITS": {"cuda_visible_devices": "1"},
            "preflight": {
                "enabled": True,
                "check_cuda_available": True,
                "check_device_exists": True,
                "check_vram": True,
                "min_free_vram_mb": 3000,
                "warn_only": True,
            },
        })

        with self._patch_torch_cuda(manager, available=True, count=2):
            with patch("core.gpu_device_manager.log_print") as log_print_mock:
                manager.log_startup_vram_preflight(force=True)

        messages = [
            str(call.args[0])
            for call in log_print_mock.call_args_list
            if call.args
        ]
        self.assertTrue(any(
            "preflight enabled=true warn_only=true" in message
            for message in messages
        ))
        self.assertTrue(any(
            "preflight CUDA available=true device_count=2" in message
            for message in messages
        ))
        self.assertTrue(any(
            "preflight VoiceInput requested='cuda:1' resolved='cuda:1' status=ok"
            in message
            for message in messages
        ))
        self.assertTrue(any(
            "preflight summary min_free_vram_mb=3000" in message
            for message in messages
        ))#20260630_kpopmodder
        self.assertTrue(any(
            "VoiceInput->cuda:1" in message
            and "ScreenVision->cuda:1" in message
            and "GPTSoVITS->cuda:1" in message
            for message in messages
        ))#20260630_kpopmodder


if __name__ == "__main__":
    unittest.main()
