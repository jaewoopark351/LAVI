#20260907_kpopmodder: Fix lifecycle ownership outside the legacy TTS component.
from __future__ import annotations

import ast
import inspect
import textwrap
import unittest
from pathlib import Path

from tts_core import TTS
from tts_core.delivery.lifecycle_response.identity import (
    TtsLifecycleResponseIdentityClassifier,
)
from tts_core.delivery.lifecycle_response.input import (
    TtsLifecycleResponseInputOutcome,
)


class TtsLifecycleResponseStructureTests(unittest.TestCase):
    def test_receive_input_has_one_lifecycle_facade_call(self):
        tree = ast.parse(textwrap.dedent(inspect.getsource(TTS.receive_input)))
        method_names = [
            node.func.attr
            for node in ast.walk(tree)
            if isinstance(node, ast.Call)
            and isinstance(node.func, ast.Attribute)
        ]

        self.assertEqual(1, method_names.count("receive"))
        self.assertNotIn("get_lifecycle_response_identity", method_names)
        self.assertNotIn(
            "_get_lifecycle_response_delivery_adapter",
            method_names,
        )
        self.assertNotIn(
            "notify_lifecycle_response_enqueue_receipt",
            method_names,
        )
        self.assertNotIn(
            "notify_lifecycle_response_playback_receipt",
            method_names,
        )

    def test_public_lifecycle_methods_are_compatibility_delegates(self):
        for method_name in (
            "get_lifecycle_response_identity",
            "_get_lifecycle_response_delivery_adapter",
            "add_lifecycle_response_enqueue_receipt_listener",
            "add_lifecycle_response_playback_receipt_listener",
            "notify_lifecycle_response_enqueue_receipt",
            "observe_lifecycle_response_playback",
            "notify_lifecycle_response_playback_receipt",
        ):
            with self.subTest(method_name=method_name):
                source = inspect.getsource(getattr(TTS, method_name))
                self.assertIn("_get_lifecycle_response_facade", source)
                self.assertLessEqual(len(source.splitlines()), 22)

    def test_classifier_preserves_closed_identity_matrix(self):
        classifier = TtsLifecycleResponseIdentityClassifier()
        valid = {
            "source": "minecraft_chatclef",
            "event_id": "a" * 32,
            "route_kind": "command_lifecycle",
            "response_kind": "command_coalesced",
            "delivery_mode": "current_input",
        }

        identity = classifier.classify(valid)

        self.assertEqual(
            (
                "a" * 32,
                "command_lifecycle",
                "command_coalesced",
                "current_input",
            ),
            identity.as_tuple(),
        )
        contextual_busy = {
            **valid,
            "event_id": "b" * 32,
            "route_kind": "command_busy_current_work",
            "response_kind": "command_status",
        }
        self.assertEqual(
            (
                "b" * 32,
                "command_busy_current_work",
                "command_status",
                "current_input",
            ),
            classifier.classify(contextual_busy).as_tuple(),
        )
        for changed in (
            {**valid, "source": "other"},
            {**valid, "delivery_mode": "non_preempting"},
            {**valid, "response_kind": "command_terminal"},
            {**valid, "event_id": None},
        ):
            with self.subTest(changed=changed):
                self.assertIsNone(classifier.classify(changed))
        for changed in (
            {**contextual_busy, "source": "other"},
            {**contextual_busy, "route_kind": "command_busy"},
            {**contextual_busy, "response_kind": "immediate"},
            {**contextual_busy, "delivery_mode": "non_preempting"},
            {**contextual_busy, "event_id": None},
        ):
            with self.subTest(contextual_busy_changed=changed):
                self.assertIsNone(classifier.classify(changed))

    def test_extracted_orchestrators_add_no_second_lock(self):
        root = (
            Path(__file__).resolve().parents[4]
            / "tts_core/delivery/lifecycle_response"
        )
        paths = (
            root / "tts_lifecycle_response_facade.py",
            root / "input/tts_lifecycle_response_input_coordinator.py",
            root
            / "receipt/tts_lifecycle_response_receipt_listener_coordinator.py",
            root / "identity/tts_lifecycle_response_identity_classifier.py",
        )

        for path in paths:
            source = path.read_text(encoding="utf-8")
            with self.subTest(path=path):
                self.assertNotIn("import threading", source)
                self.assertNotIn("Lock(", source)

    def test_input_outcome_is_explicit_about_unrelated_payload(self):
        unrelated = TtsLifecycleResponseInputOutcome.unrelated()
        handled = TtsLifecycleResponseInputOutcome.handled_receipt("receipt")

        self.assertFalse(unrelated.handled)
        self.assertIsNone(unrelated.receipt)
        self.assertTrue(handled.handled)
        self.assertEqual("receipt", handled.receipt)


if __name__ == "__main__":
    unittest.main()
