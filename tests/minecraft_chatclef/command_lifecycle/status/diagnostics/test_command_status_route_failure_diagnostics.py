#20260908_kpopmodder: Verify status-route diagnostics expose only bounded facts.
from __future__ import annotations

import unittest
from dataclasses import FrozenInstanceError
from types import SimpleNamespace

from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle.diagnostics import (
    CommandStatusRouteFailureDiagnostics,
    CommandStatusRouteFailureProjector,
)


class CommandStatusRouteFailureDiagnosticsTests(unittest.TestCase):
    def test_exact_bounded_fields_are_formatted_without_input_or_error_text(self):
        messages = []
        diagnostics = CommandStatusRouteFailureDiagnostics(messages.append)

        recorded = diagnostics.record(
            stage="status_rendering",
            exception_class="PrivateRendererFailure",
            query=SimpleNamespace(
                requested_family="item_get",
                addressed=False,
                target_text="",
            ),
            snapshot=SimpleNamespace(
                command_name="get",
                state="running",
                availability_reason="",
            ),
        )

        self.assertTrue(recorded)
        self.assertEqual(1, len(messages))
        self.assertEqual(
            (
                "event=command_status_route_failure "
                "stage=status_rendering query_kind=family addressed=false "
                "requested_family=item_get active_command_name=get "
                "lifecycle_state=running terminal_state=none "
                "availability_reason=none "
                "exception_class=PrivateRendererFailure"
            ),
            messages[0],
        )
        self.assertNotIn("user utterance", messages[0])
        self.assertNotIn("private exception message", messages[0])

    def test_missing_and_uncontrolled_values_use_exact_bounded_atoms(self):
        record = CommandStatusRouteFailureProjector().project(
            stage="SECRET_STAGE",
            exception_class="bad-name",
            query=SimpleNamespace(
                requested_family="SECRET_FAMILY",
                addressed="true",
                target_text=object(),
            ),
            snapshot=SimpleNamespace(
                command_name="SECRET_COMMAND",
                state="SECRET_STATE",
                terminal_state="SECRET_TERMINAL",
                availability_reason="SECRET_REASON",
            ),
        )

        self.assertEqual("invalid", record.stage)
        self.assertEqual("invalid", record.query_kind)
        self.assertEqual("invalid", record.addressed)
        self.assertEqual("invalid", record.requested_family)
        self.assertEqual("invalid", record.active_command_name)
        self.assertEqual("invalid", record.lifecycle_state)
        self.assertEqual("invalid", record.terminal_state)
        self.assertEqual("invalid", record.availability_reason)
        self.assertEqual("invalid", record.exception_class)

        empty = CommandStatusRouteFailureProjector().project(
            stage="proof_validation",
            exception_class="none",
        )
        self.assertEqual("none", empty.query_kind)
        self.assertEqual("none", empty.addressed)
        self.assertEqual("none", empty.requested_family)
        self.assertEqual("none", empty.active_command_name)
        self.assertEqual("none", empty.lifecycle_state)
        self.assertEqual("none", empty.terminal_state)
        self.assertEqual("none", empty.availability_reason)
        self.assertEqual("none", empty.exception_class)

    def test_exception_class_ascii_identifier_boundaries_are_closed(self):
        projector = CommandStatusRouteFailureProjector()
        valid_one = projector.project(
            stage="input_validation",
            exception_class="A",
        )
        valid_ninety_six = projector.project(
            stage="input_validation",
            exception_class="A" * 96,
        )

        self.assertEqual("A", valid_one.exception_class)
        self.assertEqual("A" * 96, valid_ninety_six.exception_class)
        for invalid in ("A" * 97, "오류", "A-B", "", None):
            with self.subTest(invalid=invalid):
                record = projector.project(
                    stage="input_validation",
                    exception_class=invalid,
                )
                self.assertEqual("invalid", record.exception_class)

    def test_record_is_frozen_and_sink_failure_never_escapes(self):
        record = CommandStatusRouteFailureProjector().project(
            stage="state_inspection",
            exception_class="RuntimeError",
        )
        with self.assertRaises(FrozenInstanceError):
            record.stage = "changed"

        diagnostics = CommandStatusRouteFailureDiagnostics(
            lambda _message: (_ for _ in ()).throw(RuntimeError("sink secret"))
        )
        self.assertFalse(
            diagnostics.record(
                stage="state_inspection",
                exception_class="RuntimeError",
            )
        )

    def test_dispatch_publication_stages_are_closed_bounded_atoms(self):
        projector = CommandStatusRouteFailureProjector()

        for stage in (
            "dispatcher_external_response_publication",
            "dispatcher_publication_commit_inspection",
        ):
            with self.subTest(stage=stage):
                record = projector.project(
                    stage=stage,
                    exception_class="RuntimeError",
                )
                self.assertEqual(stage, record.stage)


if __name__ == "__main__":
    unittest.main()
