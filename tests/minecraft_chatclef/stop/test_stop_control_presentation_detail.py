#20260907_kpopmodder: Prove trusted STOP keeps typed UI detail out of body and TTS text.
from __future__ import annotations

import json
import unittest
from types import SimpleNamespace

from app_core.composition_core.component_wiring.minecraft_stop_terminal_response_wiring import (
    MinecraftStopTerminalResponseWiring,
)
from plugins.Minecraft.fabric.chatclef.input.stop.routing.stop_control_route_outcome_builder import (
    StopControlRouteOutcomeBuilder,
)
from plugins.Minecraft.fabric.chatclef.transport.control.stop.result_handling.terminal_response.stop_control_terminal_response_factory import (
    StopControlTerminalResponseFactory,
)


class StopControlPresentationDetailTests(unittest.TestCase):
    def test_accepted_submission_suppresses_start_and_keeps_typed_stop_detail(self):
        builder = StopControlRouteOutcomeBuilder(
            SimpleNamespace(
                render_start=lambda: self.fail(
                    "accepted trusted STOP must not render a START response"
                ),
                render_local=lambda _reason: "실행하지 못했어",
            )
        )

        decision = builder.submitted(
            SimpleNamespace(
                reason="accepted",
                accepted=True,
                result={
                    "ok": True,
                    "status": "accepted",
                    "data": {
                        "request_id": "stop-request",
                        "command_message_id": "stop-message",
                    },
                },
            )
        )

        self.assertTrue(decision.handled)
        self.assertEqual("stop_control_accepted", decision.reason)
        self.assertEqual("", decision.response_text)
        self.assertEqual("stop_control", decision.route_kind)
        self.assertEqual("command_start", decision.response_kind)
        self.assertEqual("accepted", decision.result["status"])
        self.assertEqual(
            "stop-request",
            decision.result["data"]["request_id"],
        )
        self.assertEqual(
            {"command_name": "stop", "form_kind": "trusted_stop"},
            json.loads(decision.presentation_detail_log),
        )

    def test_unaccepted_submission_keeps_one_immediate_error_response(self):
        builder = StopControlRouteOutcomeBuilder(
            SimpleNamespace(
                render_start=lambda: self.fail(
                    "an unaccepted STOP must not render a START response"
                ),
                render_local=lambda reason: f"error:{reason}",
            )
        )

        for reason in (
            "bridge_disconnected",
            "control_send_rejected",
            "control_send_unknown",
        ):
            with self.subTest(reason=reason):
                decision = builder.submitted(
                    SimpleNamespace(
                        reason=reason,
                        accepted=False,
                        result={"ok": False, "status": "rejected"},
                    )
                )

                self.assertTrue(decision.handled)
                self.assertEqual(f"error:{reason}", decision.response_text)
                self.assertEqual("immediate", decision.response_kind)
                self.assertEqual("stop_control", decision.route_kind)
                self.assertFalse(decision.suppress_response)

    def test_terminal_wiring_forwards_detail_only_as_minecraft_metadata(self):
        response = StopControlTerminalResponseFactory(
            SimpleNamespace(
                render_terminal=lambda **_values: "멈췄어",
            )
        ).create(
            tracker=SimpleNamespace(event_id="a" * 32),
            decision=SimpleNamespace(
                status="completed",
                control_outcome="stopped",
                reason="stopped",
            ),
        )
        extension = _Extension()
        llm = _Llm()
        MinecraftStopTerminalResponseWiring().wire(
            llm=llm,
            extension=extension,
        )

        extension.callback(response)

        self.assertEqual("멈췄어", llm.text)
        self.assertNotIn("[Minecraft]", llm.text)
        metadata = llm.values["presentation_metadata"]
        self.assertEqual("Minecraft", metadata.badge_label)
        self.assertEqual(
            {"command_name": "stop", "form_kind": "trusted_stop"},
            json.loads(metadata.detail_log),
        )


class _Extension:
    def set_stop_terminal_response_callback(self, callback):
        self.callback = callback


class _Llm:
    def emit_external_response(self, text, **values):
        self.text = text
        self.values = values
        return object()


if __name__ == "__main__":
    unittest.main()
