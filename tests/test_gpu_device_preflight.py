import json
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

#20260628_kpopmodder: Lock GPU preflight behavior without requiring real CUDA.

PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))


from core.gpu_device_manager import GPUDeviceManager


class GPUDevicePreflightTests(unittest.TestCase):
    class _FakeCuda:
        def __init__(self, available=True, count=2, free_mb_by_index=None):
            self.available = available
            self.count = count
            self.free_mb_by_index = dict(free_mb_by_index or {})

        def is_available(self):
            return self.available

        def device_count(self):
            return self.count

        def get_device_name(self, index):
            return f"Fake GPU {index}"

        def mem_get_info(self, index):
            free_mb = self.free_mb_by_index.get(index, 8192)
            return int(free_mb * 1024 ** 2), 16 * 1024 ** 3

    class _FakeTorch:
        def __init__(self, available=True, count=2, free_mb_by_index=None):
            self.cuda = GPUDevicePreflightTests._FakeCuda(
                available=available,
                count=count,
                free_mb_by_index=free_mb_by_index,
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

    def _patch_torch(
        self,
        manager,
        available=True,
        count=2,
        free_mb_by_index=None,
    ):
        fake_torch = self._FakeTorch(
            available=available,
            count=count,
            free_mb_by_index=free_mb_by_index,
        )
        return fake_torch, patch.object(
            manager,
            "_get_torch",
            return_value=fake_torch,
        )

    def _shared_cuda_one_config(self, preflight):
        return {
            "VoiceInput": {"device": "cuda:1"},
            "ScreenVision": {"device": "cuda:1"},
            "GPTSoVITS": {"cuda_visible_devices": "1"},
            "preflight": preflight,
        }

    def test_preflight_disabled_never_delays_screenvision_auto_load(self):
        manager = self._manager_with_config(
            self._shared_cuda_one_config({
                "enabled": False,
                "check_cuda_available": True,
                "check_device_exists": True,
                "check_vram": True,
                "min_free_vram_mb": 6000,
                "warn_only": False,
            })
        )

        _fake_torch, torch_patch = self._patch_torch(
            manager,
            available=True,
            count=2,
            free_mb_by_index={1: 1024},
        )
        with torch_patch:
            result = manager.log_startup_vram_preflight(force=True)

        self.assertFalse(result["enabled"])
        self.assertTrue(result["screenvision_auto_load_allowed"])
        self.assertFalse(manager.should_delay_screenvision_auto_load())

    def test_warn_only_true_low_screenvision_vram_does_not_delay(self):
        manager = self._manager_with_config(
            self._shared_cuda_one_config({
                "enabled": True,
                "check_cuda_available": True,
                "check_device_exists": True,
                "check_vram": True,
                "min_free_vram_mb": 6000,
                "warn_only": True,
            })
        )

        _fake_torch, torch_patch = self._patch_torch(
            manager,
            available=True,
            count=2,
            free_mb_by_index={1: 1024},
        )
        with torch_patch:
            result = manager.log_startup_vram_preflight(force=True)

        self.assertTrue(result["cuda_available"])
        self.assertGreaterEqual(result["device_count"], 2)
        self.assertEqual(1, result["placements"]["ScreenVision"])
        self.assertEqual(1024, result["free_vram_mb"][1])
        self.assertTrue(result["screenvision_auto_load_allowed"])
        self.assertFalse(manager.should_delay_screenvision_auto_load())
        self.assertTrue(any("free VRAM" in item for item in result["warnings"]))

    def test_warn_only_false_low_screenvision_vram_delays_auto_load(self):
        manager = self._manager_with_config(
            self._shared_cuda_one_config({
                "enabled": True,
                "check_cuda_available": True,
                "check_device_exists": True,
                "check_vram": True,
                "min_free_vram_mb": 6000,
                "warn_only": False,
            })
        )

        _fake_torch, torch_patch = self._patch_torch(
            manager,
            available=True,
            count=2,
            free_mb_by_index={1: 1024},
        )
        with torch_patch:
            result = manager.log_startup_vram_preflight(force=True)

        self.assertFalse(result["screenvision_auto_load_allowed"])
        self.assertTrue(manager.should_delay_screenvision_auto_load())
        self.assertIn("free VRAM", result["screenvision_delay_reason"])
        self.assertIn("cuda:1", result["screenvision_delay_reason"])
        self.assertIn("1024MB < 6000MB", result["screenvision_delay_reason"])

    def test_missing_preflight_block_uses_default_config_without_crashing(self):
        manager = self._manager_with_config({
            "VoiceInput": {"device": "cuda:1"},
            "ScreenVision": {"device": "cuda:1"},
            "GPTSoVITS": {"cuda_visible_devices": "1"},
        })

        _fake_torch, torch_patch = self._patch_torch(
            manager,
            available=True,
            count=2,
            free_mb_by_index={1: 1024},
        )
        with torch_patch:
            result = manager.log_startup_vram_preflight(force=True)

        self.assertIsInstance(result, dict)
        self.assertTrue(result["enabled"])
        self.assertTrue(result["warn_only"])
        self.assertEqual(3000, result["min_free_vram_mb"])
        self.assertTrue(result["screenvision_auto_load_allowed"])

    def test_legacy_startup_vram_preflight_migrates_to_preflight_config(self):
        manager = self._manager_with_config({
            "VoiceInput": {"device": "cuda:1"},
            "ScreenVision": {"device": "cuda:1"},
            "GPTSoVITS": {"cuda_visible_devices": "1"},
            "startup_vram_preflight": {
                "enabled": True,
                "screenvision_min_free_gib": 5.5,
                "delay_screenvision_auto_load": True,
            },
        })

        _fake_torch, torch_patch = self._patch_torch(
            manager,
            available=True,
            count=2,
            free_mb_by_index={1: 4096},
        )
        with torch_patch:
            result = manager.log_startup_vram_preflight(force=True)

        self.assertFalse(result["warn_only"])
        self.assertEqual(5632, result["min_free_vram_mb"])
        self.assertFalse(result["screenvision_auto_load_allowed"])

    def test_force_true_rechecks_vram_instead_of_returning_cached_result(self):
        manager = self._manager_with_config(
            self._shared_cuda_one_config({
                "enabled": True,
                "check_cuda_available": True,
                "check_device_exists": True,
                "check_vram": True,
                "min_free_vram_mb": 3000,
                "warn_only": False,
            })
        )

        fake_torch, torch_patch = self._patch_torch(
            manager,
            available=True,
            count=2,
            free_mb_by_index={1: 5000},
        )
        with torch_patch:
            first_result = manager.log_startup_vram_preflight(force=True)
            fake_torch.cuda.free_mb_by_index[1] = 1000
            cached_result = manager.log_startup_vram_preflight()
            refreshed_result = manager.log_startup_vram_preflight(force=True)

        self.assertEqual(5000, first_result["free_vram_mb"][1])
        self.assertEqual(5000, cached_result["free_vram_mb"][1])
        self.assertTrue(cached_result["screenvision_auto_load_allowed"])
        self.assertEqual(1000, refreshed_result["free_vram_mb"][1])
        self.assertFalse(refreshed_result["screenvision_auto_load_allowed"])


if __name__ == "__main__":
    unittest.main()
