#20260909_kpopmodder: Verify immutable suppression and closed failure diagnostics.
from __future__ import annotations

import dataclasses
import unittest
from types import SimpleNamespace

from plugins.Minecraft.fabric.chatclef.input.routing.trusted_korean.response import (
    TrustedKoreanFeedbackRenderer,
    TrustedKoreanResponseAuthorizer,
)
from plugins.Minecraft.fabric.chatclef.input.routing.trusted_korean.response.contextual_busy import (
    CONTEXTUAL_BUSY_SUPPRESSED_DECISION,
    ContextualBusySuppressedDecision,
)
from plugins.Minecraft.fabric.chatclef.input.routing.trusted_korean.response.contextual_busy.diagnostics import (
    ContextualBusyResponseFailureFormatter,
    ContextualBusyResponseFailureLogger,
    ContextualBusyResponseFailureProjector,
)


class ContextualBusySuppressionAndDiagnosticsTests(unittest.TestCase):
    def test_suppressed_decision_is_canonical_and_deeply_immutable(self):
        decision = CONTEXTUAL_BUSY_SUPPRESSED_DECISION

        self.assertTrue(decision.handled)
        self.assertEqual("minecraft_command_busy", decision.reason)
        self.assertEqual({"ok": False, "error": "active_command"}, decision.result)
        self.assertEqual({}, decision.translation)
        self.assertEqual("", decision.response_text)
        self.assertFalse(decision.publish_external_response)
        self.assertIsNone(decision.response_emission_capability)
        self.assertTrue(decision.suppress_response)
        self.assertEqual("command_busy_current_work", decision.route_kind)
        self.assertEqual("command_status", decision.response_kind)
        self.assertIsNone(decision.response_publication_acknowledgement)

        with self.assertRaises(dataclasses.FrozenInstanceError):
            decision.response_text = "mutated"
        with self.assertRaises(TypeError):
            decision.result["ok"] = True
        with self.assertRaises(TypeError):
            decision.translation["command"] = "get pumpkin_pie 99"
        with self.assertRaises(TypeError):
            ContextualBusySuppressedDecision(handled=False)

    def test_renderer_and_authorizer_preserve_the_exact_suppressed_singleton(self):
        decision = CONTEXTUAL_BUSY_SUPPRESSED_DECISION
        renderer = TrustedKoreanFeedbackRenderer(
            SimpleNamespace(render=lambda value: value.response_text)
        )
        proof = SimpleNamespace(
            issue_response_emission_capability=lambda *_args, **_kwargs: (
                self.fail("suppressed decisions must not issue authority")
            )
        )

        rendered = renderer.render(decision)
        authorized = TrustedKoreanResponseAuthorizer(owner=object()).authorize(
            decision=rendered,
            event=object(),
            proof=proof,
        )

        self.assertIs(decision, rendered)
        self.assertIs(decision, authorized)

    def test_diagnostic_projection_is_closed_bounded_and_contains_no_identity(self):
        snapshot = SimpleNamespace(
            command_name="get",
            state="running",
            terminal_state="unclaimed",
            availability_reason="",
            session_id="SECRET_SESSION",
            request_id="SECRET_REQUEST",
            command="get private_target 999",
        )
        projector = ContextualBusyResponseFailureProjector(command_names=("get",))
        record = projector.project(
            stage="locked_status_inspection",
            snapshot=snapshot,
            availability_reason="identity_mismatch",
            exception_class="RuntimeError",
        )
        text = ContextualBusyResponseFailureFormatter().format(record)

        self.assertEqual(
            (
                "event=contextual_busy_response_failure "
                "stage=locked_status_inspection "
                "busy_reason=minecraft_command_busy "
                "active_command_name=get "
                "active_lifecycle_state=running "
                "terminal_state=unclaimed "
                "availability_reason=identity_mismatch "
                "exception_class=RuntimeError "
                "selected_fallback=generic_busy"
            ),
            text,
        )
        self.assertNotIn("SECRET", text)
        self.assertNotIn("private_target", text)
        self.assertLessEqual(len(text), 512)

    def test_invalid_diagnostic_values_are_normalized_and_sink_failure_is_contained(self):
        projector = ContextualBusyResponseFailureProjector(command_names=("get",))
        record = projector.project(
            stage="SECRET_STAGE",
            snapshot=SimpleNamespace(
                command_name="SECRET_COMMAND",
                state="SECRET_STATE",
                terminal_state="SECRET_TERMINAL",
                availability_reason="SECRET_REASON",
            ),
            exception_class="bad exception with payload",
        )

        self.assertEqual("invalid", record.stage)
        self.assertEqual("invalid", record.active_command_name)
        self.assertEqual("invalid", record.active_lifecycle_state)
        self.assertEqual("invalid", record.terminal_state)
        self.assertEqual("invalid", record.availability_reason)
        self.assertEqual("invalid", record.exception_class)
        self.assertFalse(
            ContextualBusyResponseFailureLogger(
                lambda _text: _raise(RuntimeError("sink unavailable")),
                command_names=("get",),
            ).record(
                stage="identity_freeze",
                availability_reason="malformed_observation",
            )
        )


def _raise(error):
    raise error


if __name__ == "__main__":
    unittest.main()
