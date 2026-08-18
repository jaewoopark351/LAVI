#20260819_kpopmodder: Reject status payloads with competing bridge objects.
from __future__ import annotations

import unittest

from .runtime_bridge_snapshot import runtime_bridge_snapshot


class RuntimeBridgeSnapshotTests(unittest.TestCase):
    def test_direct_bridge_is_selected(self):
        bridge = {"backend_id": "fabric_chatclef", "details": {"commands": {}}}

        observed, error = runtime_bridge_snapshot(bridge)

        self.assertEqual("", error)
        self.assertEqual(bridge, observed)

    def test_nested_extension_bridge_is_selected(self):
        bridge = {"backend_id": "fabric_chatclef", "details": {"commands": {}}}

        observed, error = runtime_bridge_snapshot({"name": "plugin", "details": bridge})

        self.assertEqual("", error)
        self.assertEqual(bridge, observed)

    def test_direct_and_nested_bridge_objects_are_ambiguous(self):
        observed, error = runtime_bridge_snapshot(
            {
                "backend_id": "fabric_chatclef",
                "details": {
                    "backend_id": "fabric_chatclef",
                    "details": {"commands": {}},
                },
            }
        )

        self.assertEqual({}, observed)
        self.assertIn("ambiguous", error)


if __name__ == "__main__":
    unittest.main()
