#20260819_kpopmodder: Lock exact bridge connection evidence for terminal observation.
from __future__ import annotations

import unittest

from .runtime_connection_state import runtime_connection_error


class RuntimeConnectionStateTests(unittest.TestCase):
    def test_direct_connected_bridge_is_valid(self):
        self.assertEqual(
            "",
            runtime_connection_error(
                {
                    "backend_id": "fabric_chatclef",
                    "enabled": True,
                    "connected": True,
                    "lifecycle_state": "connected",
                    "details": {"commands": {}},
                }
            ),
        )

    def test_truthy_or_wrong_connection_fields_fail_closed(self):
        cases = {
            "enabled_string": {"enabled": "true"},
            "connected_integer": {"connected": 1},
            "lifecycle_padded": {"lifecycle_state": " connected"},
            "wrong_backend": {"backend_id": "forge_minemind"},
        }
        for name, override in cases.items():
            with self.subTest(name=name):
                bridge = {
                    "backend_id": "fabric_chatclef",
                    "enabled": True,
                    "connected": True,
                    "lifecycle_state": "connected",
                }
                bridge.update(override)
                self.assertTrue(runtime_connection_error(bridge))

    def test_competing_direct_and_nested_bridges_fail_closed(self):
        error = runtime_connection_error(
            {
                "backend_id": "fabric_chatclef",
                "enabled": True,
                "connected": True,
                "lifecycle_state": "connected",
                "details": {
                    "backend_id": "fabric_chatclef",
                    "enabled": True,
                    "connected": False,
                    "lifecycle_state": "disconnected",
                    "details": {"commands": {}},
                },
            }
        )

        self.assertIn("ambiguous", error)


if __name__ == "__main__":
    unittest.main()
