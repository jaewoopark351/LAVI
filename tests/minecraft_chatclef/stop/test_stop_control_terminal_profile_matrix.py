#20260905_kpopmodder: Verify Python independently enforces every closed STOP terminal profile.
from __future__ import annotations

import unittest
from types import SimpleNamespace

from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.fabric.chatclef.transport.control.stop import (
    StopControlIdentity,
    StopControlTargetSnapshot,
)
from plugins.Minecraft.fabric.chatclef.transport.control.stop.stop_control_terminal_result_validator import (
    StopControlTerminalResultValidator,
)


class StopControlTerminalProfileMatrixTests(unittest.TestCase):
    def setUp(self):
        self.validator = StopControlTerminalResultValidator()

    def test_all_success_profiles_are_accepted_and_release(self):
        profiles = {
            "tracked_pending_stopped": ("tracked", "exact", "pending", "retired", "sent"),
            "tracked_active_stopped": ("tracked", "exact", "active", "retired", "sent"),
            "tracked_target_replaced_current_pending_stopped": (
                "tracked", "captured_current", "pending", "retired", "sent"
            ),
            "tracked_target_replaced_current_active_stopped": (
                "tracked", "captured_current", "active", "retired", "sent"
            ),
            "tracked_target_absent_global_stop_executed": (
                "tracked", "none", "none", "none", "not_applicable"
            ),
            "global_pending_stopped": (
                "global", "captured_current", "pending", "retired", "sent"
            ),
            "global_active_stopped": (
                "global", "captured_current", "active", "retired", "sent"
            ),
            "global_stop_executed_no_lavi_context": (
                "global", "none", "none", "none", "not_applicable"
            ),
        }
        for reason, (kind, resolution, before, after, delivery) in profiles.items():
            with self.subTest(reason=reason):
                tracker = _tracker(kind)
                data = _base_data(tracker)
                data.update(
                    {
                        "control_outcome": "stopped",
                        "control_reason": reason,
                        "target_resolution": resolution,
                        "target_state_before": before,
                        "target_state_after": after,
                        "original_result_delivery": delivery,
                        "stop_command_invoked": True,
                        "executed_client_tick": 10,
                        "verified_client_tick": 10 if resolution == "none" else 11,
                    }
                )
                _set_resolved(data, tracker, resolution)
                decision = self._inspect(tracker, data, "completed", True, None)
                self.assertTrue(decision.valid)
                self.assertTrue(decision.release_barrier)

    def test_all_no_mutation_profiles_are_accepted_and_release(self):
        for reason in (
            "invalid_control_profile",
            "invalid_target_scope",
            "invalid_target_fields",
            "session_mismatch",
            "server_generation_mismatch",
            "stop_control_in_flight",
            "deadline_exceeded",
        ):
            with self.subTest(reason=reason):
                tracker = _tracker("tracked")
                data = _base_data(tracker)
                data.update(
                    {
                        "control_outcome": "rejected",
                        "control_reason": reason,
                        "target_resolution": "not_evaluated",
                        "target_state_before": "not_evaluated",
                        "target_state_after": "not_evaluated",
                        "original_result_delivery": "not_applicable",
                        "stop_command_invoked": False,
                        "executed_client_tick": None,
                        "verified_client_tick": None,
                    }
                )
                _set_resolved(data, tracker, "not_evaluated")
                if reason in {
                    "invalid_control_profile",
                    "invalid_target_scope",
                    "session_mismatch",
                    "server_generation_mismatch",
                    "deadline_exceeded",
                }:
                    data["target_scope"] = None
                    _clear_requested(data)
                elif reason == "invalid_target_fields":
                    _clear_requested(data)
                status = "deadline_exceeded" if reason == "deadline_exceeded" else "rejected"
                error = "deadline_exceeded" if reason == "deadline_exceeded" else "invalid_request"
                decision = self._inspect(tracker, data, status, False, error)
                self.assertTrue(decision.valid)
                self.assertTrue(decision.release_barrier)

    def test_all_uncertainty_profiles_are_valid_but_never_release(self):
        cases = (
            ("target_observation_failed", "tracked", "unknown", "unknown", "not_applicable", False, None, 12),
            ("target_observation_failed", "tracked", "exact", "pending", "not_applicable", False, None, 12),
            ("user_stop_marker_bind_failed", "tracked", "exact", "active", "not_applicable", True, 10, 10),
            ("stop_command_exception", "global", "none", "none", "not_applicable", True, 10, 10),
            ("original_cancel_send_failed", "tracked", "exact", "active", "failed", True, 10, 15),
            ("verification_timeout", "tracked", "exact", "active", "unknown", True, 10, 30),
        )
        for case in cases:
            reason, kind, resolution, before, delivery, invoked, executed, verified = case
            with self.subTest(reason=reason, resolution=resolution):
                tracker = _tracker(kind)
                data = _base_data(tracker)
                data.update(
                    {
                        "control_outcome": "unknown",
                        "control_reason": reason,
                        "target_resolution": resolution,
                        "target_state_before": before,
                        "target_state_after": "unknown",
                        "original_result_delivery": delivery,
                        "stop_command_invoked": invoked,
                        "executed_client_tick": executed,
                        "verified_client_tick": verified,
                    }
                )
                _set_resolved(data, tracker, resolution)
                error = (
                    "internal_error"
                    if reason in {"target_observation_failed", "stop_command_exception"}
                    else None
                )
                decision = self._inspect(tracker, data, "unknown", False, error)
                self.assertTrue(decision.valid)
                self.assertFalse(decision.release_barrier)

    def test_contradictions_missing_fields_and_wrong_raw_types_quarantine(self):
        tracker = _tracker("tracked")
        data = _success_data(tracker)
        vectors = []
        wrong_reason = dict(data)
        wrong_reason["control_reason"] = "stop_verified"
        vectors.append((wrong_reason, True, "completed", None))
        missing = dict(data)
        missing.pop("original_result_delivery")
        vectors.append((missing, True, "completed", None))
        later_no_context = _success_data(_tracker("global"), resolution="none")
        later_no_context["verified_client_tick"] = 11
        vectors.append((later_no_context, True, "completed", None))
        timeout_early = dict(data)
        timeout_early.update(
            {
                "control_outcome": "unknown",
                "control_reason": "verification_timeout",
                "target_state_after": "unknown",
                "original_result_delivery": "sent",
                "verified_client_tick": 29,
            }
        )
        vectors.append((timeout_early, False, "unknown", None))

        for index, (candidate, ok, status, error) in enumerate(vectors):
            with self.subTest(index=index):
                candidate_tracker = (
                    _tracker("global") if index == 2 else tracker
                )
                decision = self._inspect(
                    candidate_tracker,
                    candidate,
                    status,
                    ok,
                    error,
                )
                self.assertFalse(decision.valid)
                self.assertFalse(decision.release_barrier)

        payload, envelope, result = _wire(tracker, data, "completed", True, None)
        payload["ok"] = 1
        decision = self.validator.inspect(envelope, result, tracker)
        self.assertFalse(decision.valid)

    def test_unknown_top_level_or_authorization_field_never_releases(self):
        tracker = _tracker("tracked")
        data = _success_data(tracker)
        payload, envelope, result = _wire(
            tracker,
            data,
            "completed",
            True,
            None,
        )
        payload["authorization_override"] = True
        decision = self.validator.inspect(envelope, result, tracker)
        self.assertFalse(decision.valid)
        self.assertFalse(decision.release_barrier)

    def test_captured_current_target_cannot_cross_control_session_or_generation(self):
        tracker = _tracker("tracked")
        for field, replacement in (
            ("resolved_target_session_id", "session-b"),
            ("resolved_target_server_connection_generation", 8),
        ):
            with self.subTest(field=field):
                data = _success_data(tracker)
                data.update(
                    {
                        "control_reason": (
                            "tracked_target_replaced_current_active_stopped"
                        ),
                        "target_resolution": "captured_current",
                    }
                )
                _set_resolved(data, tracker, "captured_current")
                data[field] = replacement
                decision = self._inspect(
                    tracker,
                    data,
                    "completed",
                    True,
                    None,
                )
                self.assertFalse(decision.valid)
                self.assertFalse(decision.release_barrier)

        data = _success_data(tracker)
        data["authorization_override"] = True
        decision = self._inspect(tracker, data, "completed", True, None)
        self.assertFalse(decision.valid)
        self.assertFalse(decision.release_barrier)

    def _inspect(self, tracker, data, status, ok, error):
        _payload, envelope, result = _wire(tracker, data, status, ok, error)
        return self.validator.inspect(envelope, result, tracker)


def _tracker(kind):
    identity = StopControlIdentity(
        request_id="stop-request",
        message_id="stop-message",
        session_id="session-a",
        server_connection_generation=7,
    )
    target = None
    scope = "current_global_automation"
    if kind == "tracked":
        scope = "tracked_command"
        target = StopControlTargetSnapshot(
            request_id="ordinary-request",
            command_message_id="ordinary-message",
            session_id="session-a",
            server_connection_generation=7,
            owner_token=SimpleNamespace(
                request_id="ordinary-request",
                command_message_id="ordinary-message",
                session_id="session-a",
                generation=7,
            ),
        )
    return SimpleNamespace(identity=identity, target=target, target_scope=scope)


def _base_data(tracker):
    data = {
        "request_kind": "stop_control_v1",
        "operation": "stop_ai",
        "connection_generation": 7,
        "java_socket_generation": 3,
        "target_scope": tracker.target_scope,
        "requested_target_request_id": None,
        "requested_target_command_message_id": None,
        "requested_target_session_id": None,
        "requested_target_server_connection_generation": None,
        "resolved_target_request_id": None,
        "resolved_target_command_message_id": None,
        "resolved_target_session_id": None,
        "resolved_target_server_connection_generation": None,
    }
    if tracker.target is not None:
        data.update(
            {
                "requested_target_request_id": tracker.target.request_id,
                "requested_target_command_message_id": tracker.target.command_message_id,
                "requested_target_session_id": tracker.target.session_id,
                "requested_target_server_connection_generation": tracker.target.server_connection_generation,
            }
        )
    return data


def _success_data(tracker, resolution="exact"):
    reason = "tracked_active_stopped"
    if tracker.target is None:
        reason = "global_stop_executed_no_lavi_context"
    data = _base_data(tracker)
    data.update(
        {
            "control_outcome": "stopped",
            "control_reason": reason,
            "target_resolution": resolution,
            "target_state_before": "none" if resolution == "none" else "active",
            "target_state_after": "none" if resolution == "none" else "retired",
            "original_result_delivery": "not_applicable" if resolution == "none" else "sent",
            "stop_command_invoked": True,
            "executed_client_tick": 10,
            "verified_client_tick": 10 if resolution == "none" else 11,
        }
    )
    _set_resolved(data, tracker, resolution)
    return data


def _set_resolved(data, tracker, resolution):
    _clear_resolved(data)
    if resolution == "exact":
        target = tracker.target
        data.update(
            {
                "resolved_target_request_id": target.request_id,
                "resolved_target_command_message_id": target.command_message_id,
                "resolved_target_session_id": target.session_id,
                "resolved_target_server_connection_generation": target.server_connection_generation,
            }
        )
    elif resolution == "captured_current":
        data.update(
            {
                "resolved_target_request_id": "captured-request",
                "resolved_target_command_message_id": "captured-message",
                "resolved_target_session_id": tracker.identity.session_id,
                "resolved_target_server_connection_generation": (
                    tracker.identity.server_connection_generation
                ),
            }
        )


def _clear_requested(data):
    for key in tuple(data):
        if key.startswith("requested_target_"):
            data[key] = None


def _clear_resolved(data):
    for key in tuple(data):
        if key.startswith("resolved_target_"):
            data[key] = None


def _wire(tracker, data, status, ok, error):
    payload = {
        "request_id": tracker.identity.request_id,
        "ok": ok,
        "status": status,
        "error_code": error,
        "message": "terminal",
        "data": data,
    }
    result = CommandResultDTO.from_mapping(payload)
    envelope = SimpleNamespace(
        payload=payload,
        correlation_id=tracker.identity.message_id,
        session_id=tracker.identity.session_id,
    )
    return payload, envelope, result


if __name__ == "__main__":
    unittest.main()
