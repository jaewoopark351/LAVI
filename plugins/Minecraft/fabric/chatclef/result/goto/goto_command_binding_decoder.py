#20260913_kpopmodder: Validate the closed wire binding before constructing its DTO.
from __future__ import annotations

import re
from dataclasses import fields
from types import MappingProxyType
from typing import Mapping

from .goto_command_binding import GotoCommandBinding


class GotoCommandBindingDecoder:
    _TOKEN = re.compile(r"[A-Za-z0-9_.:-]{1,160}\Z", re.ASCII)
    _OWNERS = frozenset({
        "lavi.minecraft.task.movement.gotopreflight.PreparedGotoTask",
        "lavi.minecraft.task.movement.gotoresult.tracking.ReportedGotoBlockTask",
    })
    _DIMENSIONS = MappingProxyType({
        "overworld": "minecraft:overworld",
        "nether": "minecraft:the_nether",
        "end": "minecraft:the_end",
    })

    def decode(self, payload: object) -> GotoCommandBinding | None:
        if not isinstance(payload, Mapping):
            return None
        names = tuple(field.name for field in fields(GotoCommandBinding))
        if set(payload) != set(names):
            return None
        for name in ("request_id", "command_message_id", "session_id",
                     "task_identity", "operation_id"):
            value = payload.get(name)
            if type(value) is not str or self._TOKEN.fullmatch(value) is None:
                return None
        if type(payload.get("task_owner")) is not str or payload.get("task_owner") not in self._OWNERS:
            return None
        for name in ("server_connection_generation", "java_socket_generation"):
            value = payload.get(name)
            if type(value) is not int or not 1 <= value < 2**63:
                return None
        for name in ("target_x", "target_y", "target_z"):
            value = payload.get(name)
            if type(value) is not int or not -(2**31) <= value < 2**31:
                return None
        if payload.get("request_shape") != "XYZ":
            return None
        requested = payload.get("requested_dimension")
        world = payload.get("world_dimension")
        if type(world) is not str or world not in self._DIMENSIONS.values():
            return None
        if requested is not None and (
            type(requested) is not str or self._DIMENSIONS.get(requested) != world
        ):
            return None
        return GotoCommandBinding(**{name: payload[name] for name in names})
