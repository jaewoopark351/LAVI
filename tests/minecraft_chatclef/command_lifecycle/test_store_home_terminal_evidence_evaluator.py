#20260908_kpopmodder: Lock closed STORE_HOME terminal evidence and immutable success projection.
from __future__ import annotations

import unittest
from dataclasses import FrozenInstanceError, replace
from types import SimpleNamespace

from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.bridge_error_code import BridgeErrorCode
from plugins.Minecraft.common.protocol.command_result_status import CommandResultStatus
from plugins.Minecraft.fabric.chatclef.result.store_home import (
    StoreHomeTerminalPayload,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackDescriptor,
    CommandFeedbackDescriptorFactory,
    CommandTerminalEvidenceEvaluation,
    CommandTerminalEvidenceEvaluator,
    CommandTerminalEvidenceProfileRegistry,
)


STOP_ONLY_KEYS = (
    "control_outcome",
    "control_reason",
    "target_scope",
    "target_resolution",
    "requested_target_request_id",
    "requested_target_command_message_id",
    "requested_target_session_id",
    "requested_target_server_connection_generation",
    "resolved_target_request_id",
    "resolved_target_command_message_id",
    "resolved_target_session_id",
    "resolved_target_server_connection_generation",
    "target_state_before",
    "target_state_after",
    "original_result_delivery",
    "stop_command_invoked",
    "connection_generation",
    "java_socket_generation",
    "executed_client_tick",
    "verified_client_tick",
)

GET_RESERVED_KEYS = (
    "effect_profile_id",
    "effect_profile_version",
    "effect_payload",
    "effect_kind",
    "target_item",
    "target_match_ids",
    "quantity_semantics",
    "requested_count",
    "requested_delta",
    "before_target_count",
    "after_target_count",
    "target_count_delta",
    "effect_observation_status",
    "effect_observation_reason",
)

NON_COMPLETED_RESULTS = (
    "PARTIAL_TRUSTED_CAPACITY_EXHAUSTED",
    "PARTIAL_TRUSTED_DESTINATIONS_UNAVAILABLE",
    "NO_USABLE_TRUSTED_DESTINATION",
    "NO_TRUSTED_CAPACITY",
    "CURSOR_NOT_EMPTY",
    "MANIFEST_STALE",
    "CONTEXT_CHANGED",
    "TRANSFER_UNCONFIRMED",
    "INTERRUPTED",
)


class StoreHomeTerminalEvidenceEvaluatorTests(unittest.TestCase):
    def setUp(self) -> None:
        self.evaluator = CommandTerminalEvidenceEvaluator()
        self.descriptor = _trusted_descriptor("lavi_chat_ui")
        self.context = _context(self.descriptor)

    def test_exact_completed_evidence_returns_frozen_count_projection(self):
        result = _result(_payload(stored_items=909))

        evaluation = self.evaluator.evaluate(result, context=self.context)

        self.assertIs(type(evaluation), CommandTerminalEvidenceEvaluation)
        self.assertIs(evaluation.verified, True)
        self.assertIs(type(evaluation.projection), StoreHomeTerminalPayload)
        self.assertEqual(909, evaluation.projection.stored_items)
        self.assertEqual(0, evaluation.projection.remaining_stacks)
        self.assertEqual("all_items_stored", evaluation.projection.reason)
        self.assertIs(
            self.evaluator.verified(result, context=self.context),
            evaluation.verified,
        )
        with self.assertRaises(FrozenInstanceError):
            evaluation.projection.stored_items = 0

    def test_completed_zero_work_is_verified_without_inventing_a_count(self):
        evaluation = self.evaluator.evaluate(
            _result(_payload(stored_items=0)),
            context=self.context,
        )

        self.assertIs(evaluation.verified, True)
        self.assertEqual(0, evaluation.projection.stored_items)
        self.assertEqual("COMPLETED", evaluation.projection.result)

    def test_descriptor_command_is_compared_after_whitespace_normalization(self):
        descriptor = replace(self.descriptor, command="  store_home  ")

        evaluation = self.evaluator.evaluate(
            _result(_payload()),
            context=_context(descriptor),
        )

        self.assertIs(evaluation.verified, True)

    def test_only_chat_and_final_microphone_trusted_translations_are_candidates(self):
        for source in ("lavi_chat_ui", "voice_input_final"):
            with self.subTest(source=source):
                descriptor = _trusted_descriptor(source)
                self.assertIs(
                    self.evaluator.evaluate(
                        _result(_payload()),
                        context=_context(descriptor),
                    ).verified,
                    True,
                )

        direct_typed = _trusted_descriptor("direct_typed")
        self.assertIs(
            self.evaluator.evaluate(
                _result(_payload()),
                context=_context(direct_typed),
            ).verified,
            False,
        )

        factory = CommandFeedbackDescriptorFactory()
        for command in ("store_home", "@store_home"):
            with self.subTest(command=command):
                raw = factory.decode_registered_command_name_only(
                    command,
                    command_source="lavi_gui",
                    event_id="b" * 32,
                    provider_id="minecraft_fabric_chatclef_ui",
                    event_kind="minecraft_raw_gui_submit",
                )
                self.assertIsNotNone(raw)
                self.assertEqual("cautious", raw.rollout_state)
                self.assertIs(
                    self.evaluator.evaluate(
                        _result(_payload()),
                        context=_context(raw),
                    ).verified,
                    False,
                )
                command_name_only = replace(
                    raw,
                    detail_level=CommandFeedbackDescriptor.COMMAND_NAME_ONLY,
                )
                self.assertIs(
                    self.evaluator.evaluate(
                        _result(_payload()),
                        context=_context(command_name_only),
                    ).verified,
                    False,
                )

    def test_each_descriptor_disagreement_fails_closed(self):
        cases = (
            replace(self.descriptor, command_name="get"),
            replace(self.descriptor, command="store_home now"),
            replace(self.descriptor, form_kind="no_arguments"),
            replace(
                self.descriptor,
                detail_level=CommandFeedbackDescriptor.RAW_TYPED,
            ),
            replace(self.descriptor, command_source="voice_input_final"),
            replace(self.descriptor, input_source="voice_input_final"),
            replace(
                self.descriptor,
                command_source="direct_typed",
                input_source="direct_typed",
            ),
            replace(
                self.descriptor,
                evidence_profile_id="get_terminal_evidence_v1",
            ),
            replace(self.descriptor, rollout_state="cautious"),
        )

        for descriptor in cases:
            with self.subTest(descriptor=descriptor):
                evaluation = self.evaluator.evaluate(
                    _result(_payload()),
                    context=_context(descriptor),
                )
                self.assertIs(evaluation.verified, False)
                self.assertIsNone(evaluation.projection)

    def test_each_profile_disagreement_fails_closed(self):
        profile = CommandTerminalEvidenceProfileRegistry().profile("store_home")
        cases = (
            replace(profile, command_name="get"),
            replace(profile, profile_id="other_terminal_evidence_v1"),
            replace(profile, rollout_state="cautious"),
            replace(profile, success_evaluator_id="get_acquisition"),
            replace(profile, success_evaluator_id="unknown"),
        )

        for candidate in cases:
            with self.subTest(profile=candidate):
                evaluator = CommandTerminalEvidenceEvaluator(
                    profile_registry=_StaticProfileRegistry(candidate)
                )
                evaluation = evaluator.evaluate(
                    _result(_payload()),
                    context=self.context,
                )
                self.assertIs(evaluation.verified, False)
                self.assertIsNone(evaluation.projection)

    def test_store_home_profile_dispatches_to_store_home_only(self):
        projection = StoreHomeTerminalPayload.from_data(_payload())
        expected = CommandTerminalEvidenceEvaluation(True, projection)
        get_evaluator = _RecordingEvaluator(CommandTerminalEvidenceEvaluation(False))
        store_home_evaluator = _RecordingEvaluator(expected)
        evaluator = CommandTerminalEvidenceEvaluator(
            get_evaluator=get_evaluator,
            store_home_evaluator=store_home_evaluator,
        )
        result = _result(_payload())

        actual = evaluator.evaluate(result, context=self.context)

        self.assertIs(expected, actual)
        self.assertEqual([], get_evaluator.calls)
        self.assertEqual(1, len(store_home_evaluator.calls))
        self.assertIs(result, store_home_evaluator.calls[0][0])

    def test_outer_terminal_identity_and_status_fail_closed_independently(self):
        mutations = (
            {"result_reason": "completed"},
            {"result_reason": None},
            {"result_fidelity": "callback_only"},
            {"result_fidelity": None},
        )
        for overrides in mutations:
            with self.subTest(overrides=overrides):
                evaluation = self.evaluator.evaluate(
                    _result(_payload(**overrides)),
                    context=self.context,
                )
                self.assertIs(evaluation.verified, False)
                self.assertIsNone(evaluation.projection)

        for status, error_code in (
            (CommandResultStatus.RUNNING, None),
            (CommandResultStatus.REJECTED, None),
            (CommandResultStatus.FAILED, BridgeErrorCode.INTERNAL_ERROR),
            (CommandResultStatus.UNKNOWN, None),
        ):
            with self.subTest(status=status):
                evaluation = self.evaluator.evaluate(
                    _result(
                        _payload(),
                        status=status,
                        error_code=error_code,
                    ),
                    context=self.context,
                )
                self.assertIs(evaluation.verified, False)
                self.assertIsNone(evaluation.projection)

    def test_every_required_payload_field_is_required(self):
        for key in (
            "result_reason",
            "result_fidelity",
            "operation",
            "store_home_result",
            "stored_items",
            "remaining_stacks",
            "reason",
            "goal_satisfied",
        ):
            data = _payload()
            data.pop(key)
            with self.subTest(key=key):
                evaluation = self.evaluator.evaluate(
                    _result(data),
                    context=self.context,
                )
                self.assertIs(evaluation.verified, False)
                self.assertIsNone(evaluation.projection)

    def test_each_malformed_typed_payload_value_fails_without_raising(self):
        cases = (
            {"operation": "deposit"},
            {"operation": 1},
            {"store_home_result": "UNKNOWN_RESULT"},
            {"store_home_result": 1},
            {"stored_items": True},
            {"stored_items": -1},
            {"stored_items": "909"},
            {"remaining_stacks": True},
            {"remaining_stacks": -1},
            {"remaining_stacks": 1},
            {"reason": None},
            {"reason": 1},
            {"reason": ""},
            {"reason": "   "},
            {"goal_satisfied": 1},
            {"goal_satisfied": None},
            {"goal_satisfied": False},
        )

        for overrides in cases:
            with self.subTest(overrides=overrides):
                evaluation = self.evaluator.evaluate(
                    _result(_payload(**overrides)),
                    context=self.context,
                )
                self.assertIs(evaluation.verified, False)
                self.assertIsNone(evaluation.projection)

    def test_partial_and_failure_payloads_never_become_completed_success(self):
        for result_name in NON_COMPLETED_RESULTS:
            partial = result_name.startswith("PARTIAL_")
            data = _payload(
                store_home_result=result_name,
                stored_items=1 if partial else 0,
                remaining_stacks=1,
                goal_satisfied=False,
            )
            with self.subTest(result_name=result_name):
                evaluation = self.evaluator.evaluate(
                    _result(data),
                    context=self.context,
                )
                self.assertIs(evaluation.verified, False)
                self.assertIsNone(evaluation.projection)

    def test_reason_bound_uses_trimmed_length(self):
        accepted = self.evaluator.evaluate(
            _result(_payload(reason="  " + "r" * 256 + "  ")),
            context=self.context,
        )
        rejected = self.evaluator.evaluate(
            _result(_payload(reason="  " + "r" * 257 + "  ")),
            context=self.context,
        )

        self.assertIs(accepted.verified, True)
        self.assertEqual("r" * 256, accepted.projection.reason)
        self.assertIs(rejected.verified, False)
        self.assertIsNone(rejected.projection)

    def test_presence_of_every_closed_cross_family_key_fails_closed(self):
        for key in ("request_kind", *STOP_ONLY_KEYS, *GET_RESERVED_KEYS):
            data = _payload()
            data[key] = "foreign_evidence"
            with self.subTest(key=key):
                evaluation = self.evaluator.evaluate(
                    _result(data),
                    context=self.context,
                )
                self.assertIs(evaluation.verified, False)
                self.assertIsNone(evaluation.projection)

        stop_operation = _payload(operation="stop_ai")
        evaluation = self.evaluator.evaluate(
            _result(stop_operation),
            context=self.context,
        )
        self.assertIs(evaluation.verified, False)
        self.assertIsNone(evaluation.projection)

    def test_wrong_result_objects_and_wrong_shaped_data_fail_without_raising(self):
        class CommandResultSubclass(CommandResultDTO):
            pass

        wrong_objects = (
            None,
            {},
            SimpleNamespace(data=_payload()),
            CommandResultSubclass(
                request_id="request-subclass",
                ok=True,
                status=CommandResultStatus.COMPLETED,
                data=_payload(),
            ),
            CommandResultDTO(
                request_id="request-empty-data",
                ok=True,
                status=CommandResultStatus.COMPLETED,
                data=None,
            ),
            CommandResultDTO(
                request_id="request-list-data",
                ok=True,
                status=CommandResultStatus.COMPLETED,
                data=[],
            ),
        )

        for result in wrong_objects:
            with self.subTest(result=result):
                evaluation = self.evaluator.evaluate(
                    result,
                    context=self.context,
                )
                self.assertIs(type(evaluation), CommandTerminalEvidenceEvaluation)
                self.assertIs(evaluation.verified, False)
                self.assertIsNone(evaluation.projection)


def _context(descriptor: object):
    return SimpleNamespace(descriptor=descriptor)


def _trusted_descriptor(source: str):
    provider_id = "VoiceInput" if source == "voice_input_final" else source
    event_kind = "final_transcript" if source == "voice_input_final" else "chat_submit"
    descriptor = CommandFeedbackDescriptorFactory().from_trusted_translation(
        event=SimpleNamespace(
            text="인벤토리 전부 집에 보관해",
            source=source,
            provider_id=provider_id,
            event_kind=event_kind,
            final=True,
            event_id="a" * 32,
        ),
        translation={
            "status": "validated",
            "executable": True,
            "command": "store_home",
            "resolved_target": None,
            "intent": {
                "intent_type": "store_home",
                "quantity": None,
                "item_phrase": "",
                "language": "ko",
                "original_text": "인벤토리 전부 집에 보관해",
            },
        },
    )
    if descriptor is None:
        raise AssertionError(f"trusted STORE_HOME descriptor failed for {source}")
    return descriptor


def _payload(**overrides) -> dict[str, object]:
    data: dict[str, object] = {
        "result_reason": "matching_task_finished",
        "result_fidelity": "callback_plus_matching_user_task_event",
        "operation": "store_home",
        "store_home_result": "COMPLETED",
        "stored_items": 909,
        "remaining_stacks": 0,
        "reason": "all_items_stored",
        "goal_satisfied": True,
    }
    data.update(overrides)
    return data


def _result(
    data: object,
    *,
    status: CommandResultStatus = CommandResultStatus.COMPLETED,
    error_code: BridgeErrorCode | None = None,
) -> CommandResultDTO:
    return CommandResultDTO(
        request_id="request-store-home",
        ok=status.ok,
        status=status,
        error_code=error_code,
        data=data,
    )


class _StaticProfileRegistry:
    def __init__(self, profile: object) -> None:
        self._profile = profile

    def profile(self, _command_name: object):
        return self._profile


class _RecordingEvaluator:
    def __init__(self, evaluation: CommandTerminalEvidenceEvaluation) -> None:
        self._evaluation = evaluation
        self.calls: list[tuple[object, object, object, object]] = []

    def evaluate(self, result: object, *, data: object, context: object, profile: object):
        self.calls.append((result, data, context, profile))
        return self._evaluation


if __name__ == "__main__":
    unittest.main()
