#20260716_kpopmodder: Focused tests for the P0-B startup smoke contracts.
import subprocess
import sys
import time
import unittest
from unittest import mock
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]


class SmokeStartupTests(unittest.TestCase):
    def test_production_config_smoke_uses_root_modules_json_without_starting_resources(self):
        from scripts.smoke_startup import run_production_config_smoke

        result = run_production_config_smoke(PROJECT_ROOT, timeout_sec=30)

        self.assertEqual("production", result["modules_config_source"])
        self.assertEqual(PROJECT_ROOT / "modules.json", Path(result["modules_config_path"]))
        self.assertFalse(result["resource_start_attempted"])
        self.assertTrue(result["shutdown_completed"])
        self.assertEqual(
            result["modules_json_hash_before"],
            result["modules_json_hash_after"],
        )

    def test_side_effect_guard_blocks_network_attempts(self):
        from scripts.smoke_startup import (
            AttemptCounters,
            SideEffectAttempt,
            SmokeSideEffectGuard,
        )

        counters = AttemptCounters()
        with SmokeSideEffectGuard(counters):
            import socket

            with self.assertRaises(SideEffectAttempt):
                socket.create_connection(("127.0.0.1", 9))

        self.assertEqual(1, counters.network_attempts)

    def test_side_effect_guard_preserves_popen_class_api_for_real_gradio_import(self):
        from scripts.smoke_startup import AttemptCounters, SmokeSideEffectGuard

        counters = AttemptCounters()
        with SmokeSideEffectGuard(counters):
            self.assertIsNotNone(subprocess.Popen[bytes])
            import gradio  # noqa: F401

        self.assertEqual(0, counters.external_process_attempts)

    def test_side_effect_guard_blocks_real_popen_calls(self):
        from scripts.smoke_startup import (
            AttemptCounters,
            SideEffectAttempt,
            SmokeSideEffectGuard,
        )

        counters = AttemptCounters()
        with SmokeSideEffectGuard(counters):
            with self.assertRaises(SideEffectAttempt):
                subprocess.Popen([sys.executable, "-c", "print('blocked')"])

        self.assertEqual(1, counters.external_process_attempts)

    def test_core_smoke_timeout_interrupts_startup_work(self):
        from scripts import smoke_startup
        from scripts.smoke_startup import SmokeTimeout

        def hang(*_args, **_kwargs):
            time.sleep(10)

        with mock.patch.object(smoke_startup, "load_module_settings", side_effect=hang):
            started_at = time.monotonic()
            with self.assertRaises(SmokeTimeout):
                smoke_startup.run_core_offline_smoke(
                    PROJECT_ROOT,
                    profile="Core",
                    modules_config="",
                    accelerator="CPU",
                    offline=True,
                    timeout_sec=0.05,
                )

        self.assertLess(time.monotonic() - started_at, 2.0)

    def test_core_provider_contract_rejects_non_null_provider(self):
        from scripts.smoke_startup import SmokeError, _assert_core_providers

        with self.assertRaises(SmokeError):
            _assert_core_providers({
                "input": "VoiceInput",
                "llm": "NullLLM",
                "translation": "NoTranslate",
                "tts": "NullTTS",
                "vtuber": "NullVtuber",
            })


if __name__ == "__main__":
    unittest.main()
