#20260913_kpopmodder: Decode bounded GOTO terminal wire fields without deciding success.
from __future__ import annotations

import re
from typing import Mapping

from .goto_command_binding_decoder import GotoCommandBindingDecoder
from .goto_terminal_payload import GotoTerminalPayload


class GotoTerminalPayloadDecoder:
    _BINDING_FIELDS = frozenset({
        "request_id", "command_message_id", "session_id",
        "server_connection_generation", "java_socket_generation", "task_owner",
        "task_identity", "operation_id", "request_shape", "target_x", "target_y",
        "target_z", "requested_dimension", "world_dimension",
    })
    _TERMINAL_FIELDS = frozenset({
        "outcome", "failure_reason", "goal_satisfied", "binding_valid",
        "children_quiescent", "evidence_kind", "terminal_dimension",
    })
    _REASON = re.compile(r"[A-Z][A-Z0-9_]{0,63}\Z", re.ASCII)
    _DIMENSIONS = frozenset({
        "minecraft:overworld", "minecraft:the_nether", "minecraft:the_end",
    })

    def __init__(self, *, binding_decoder=None) -> None:
        self._bindings = binding_decoder or GotoCommandBindingDecoder()

    def decode(self, data: object) -> GotoTerminalPayload | None:
        if not isinstance(data, Mapping):
            return None
        if (
            data.get("goto_profile_id") != "fabric_chatclef_goto_terminal"
            or type(data.get("goto_profile_version")) is not int
            or data.get("goto_profile_version") != 1
        ):
            return None
        payload = data.get("goto_terminal")
        if (
            not isinstance(payload, Mapping)
            or frozenset(payload) != self._BINDING_FIELDS | self._TERMINAL_FIELDS
        ):
            return None
        binding = self._bindings.decode({key: payload[key] for key in self._BINDING_FIELDS})
        reason = payload.get("failure_reason")
        dimension = payload.get("terminal_dimension")
        if (
            binding is None
            or payload.get("outcome") not in ("ARRIVED", "FAILED")
            or type(reason) is not str
            or self._REASON.fullmatch(reason) is None
            or any(type(payload.get(key)) is not bool for key in (
                "goal_satisfied", "binding_valid", "children_quiescent",
            ))
            or payload.get("evidence_kind") not in (
                "prepared_goto_terminal", "legacy_get_to_block_terminal",
            )
            or (dimension is not None and (
                type(dimension) is not str or dimension not in self._DIMENSIONS
            ))
        ):
            return None
        return GotoTerminalPayload(
            binding=binding,
            **{key: payload[key] for key in self._TERMINAL_FIELDS},
        )


__all__ = ("GotoTerminalPayloadDecoder",)
