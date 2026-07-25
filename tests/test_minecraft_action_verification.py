#20260725_kpopmodder: Covers inventory verification attached to counted Minecraft actions.
import unittest
from pathlib import Path

from plugins.Minecraft.minecraft_core import (
    MinecraftConfig,
    MinecraftCraftAction,
    MinecraftVerifiedItemActionRunner,
)


PROJECT_ROOT = Path(__file__).resolve().parents[1]


class FakeClientProvider:
    def __init__(self, client):
        self.client = client


class FakeVerifiedActionClient:
    def __init__(
        self,
        *,
        before_count=0,
        after_count=0,
        action_status="succeeded",
    ):
        self.inventory_counts = [before_count, after_count]
        self.action_status = action_status
        self.craft_calls = []
        self.current_action_calls = 0

    def inventory(self):
        count = self.inventory_counts.pop(0)
        return {"ok": True, "counts": {"stick": count}}

    def current_action(self):
        self.current_action_calls += 1
        return {
            "ok": True,
            "action": {
                "action_id": "lavi-test",
                "type": "craft",
                "status": self.action_status,
            },
        }

    def craft(self, item, count):
        self.craft_calls.append((item, count))
        return {
            "ok": True,
            "accepted": True,
            "action": {
                "action_id": "lavi-test",
                "type": "craft",
                "status": "running",
            },
        }


class FakeLegacyActionClient:
    def __init__(self):
        self.craft_calls = []

    def craft(self, item, count):
        self.craft_calls.append((item, count))
        return {"ok": True, "accepted": True}


class FakeSnapshotReader:
    def __init__(self, count):
        self.count = count

    def read(self, item):
        return {"ok": True, "item": item, "count": self.count}


class FakeTimeoutCompletionPoller:
    def wait(self, action_id, *, timeout_sec, poll_interval_sec):
        return {
            "ok": False,
            "completion_status": "timeout",
            "action_status": "running",
            "message": "Minecraft action verification timed out.",
            "action": {
                "action_id": action_id,
                "type": "craft",
                "status": "running",
            },
        }


class MinecraftActionVerificationTests(unittest.TestCase):
    def test_craft_attaches_verified_inventory_delta(self):
        client = FakeVerifiedActionClient(before_count=20, after_count=24)
        action = MinecraftCraftAction(
            self._config(),
            FakeClientProvider(client),
        )

        result = action.run("stick", 4)

        self.assertTrue(result["ok"])
        self.assertTrue(result["verified"])
        self.assertEqual("verified", result["verification"]["status"])
        self.assertEqual(20, result["verification"]["before_count"])
        self.assertEqual(24, result["verification"]["after_count"])
        self.assertEqual(4, result["verification"]["actual_delta"])
        self.assertEqual([("stick", 4)], client.craft_calls)

    def test_craft_marks_result_failed_when_inventory_delta_is_too_small(self):
        client = FakeVerifiedActionClient(before_count=20, after_count=21)
        action = MinecraftCraftAction(
            self._config(),
            FakeClientProvider(client),
        )

        result = action.run("stick", 4)

        self.assertFalse(result["ok"])
        self.assertFalse(result["verified"])
        self.assertEqual("mismatch", result["verification"]["status"])
        self.assertEqual("inventory_verification_failed", result["error"])
        self.assertEqual(1, result["verification"]["actual_delta"])

    def test_craft_skips_inventory_calls_when_verification_is_disabled(self):
        client = FakeVerifiedActionClient(before_count=20, after_count=24)
        config = self._config()
        config.config["action_verification"]["enabled"] = False
        action = MinecraftCraftAction(config, FakeClientProvider(client))

        result = action.run("stick", 4)

        self.assertTrue(result["ok"])
        self.assertEqual("disabled", result["verification"]["status"])
        self.assertEqual([("stick", 4)], client.craft_calls)
        self.assertEqual(0, client.current_action_calls)
        self.assertEqual([20, 24], client.inventory_counts)

    def test_craft_skips_verification_when_action_id_is_missing(self):
        client = FakeLegacyActionClient()
        action = MinecraftCraftAction(self._config(), FakeClientProvider(client))

        result = action.run("stick", 4)

        self.assertTrue(result["ok"])
        self.assertFalse(result["verified"])
        self.assertEqual("skipped", result["verification"]["status"])
        self.assertEqual("action_not_accepted", result["verification"]["reason"])
        self.assertEqual([("stick", 4)], client.craft_calls)

    def test_verification_timeout_does_not_mark_accepted_action_failed(self):
        client = FakeVerifiedActionClient(before_count=20, after_count=24)
        runner = MinecraftVerifiedItemActionRunner(
            self._config(),
            FakeClientProvider(client),
            snapshot_reader=FakeSnapshotReader(20),
            completion_poller=FakeTimeoutCompletionPoller(),
        )

        result = runner.run(
            action="craft",
            item="stick",
            count=4,
            submit=lambda: client.craft("stick", 4),
        )

        self.assertTrue(result["ok"])
        self.assertFalse(result["verified"])
        self.assertEqual("timeout", result["verification"]["status"])
        self.assertEqual("running", result["completion"]["action_status"])
        self.assertNotIn("error", result)

    def _config(self):
        config = MinecraftConfig(
            str(PROJECT_ROOT / "plugins" / "Minecraft"),
            config_path=str(PROJECT_ROOT / "missing_minecraft_config.json"),
        )
        config.config["action_verification"]["timeout_sec"] = 1.0
        config.config["action_verification"]["poll_interval_sec"] = 0.1
        return config


if __name__ == "__main__":
    unittest.main()
