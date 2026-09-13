#20260913_kpopmodder: Bind the first correlated running Task and reject replacement evidence.
from __future__ import annotations

from dataclasses import replace
from typing import Mapping

from plugins.Minecraft.fabric.chatclef.result.goto.goto_command_binding_decoder import (
    GotoCommandBindingDecoder,
)
from plugins.Minecraft.fabric.chatclef.result.goto.goto_command_binding_matcher import (
    GotoCommandBindingMatcher,
)


class GotoTaskBindingCoordinator:
    def __init__(self, *, decoder=None, matcher=None) -> None:
        self._decoder = decoder or GotoCommandBindingDecoder()
        self._matcher = matcher or GotoCommandBindingMatcher()

    def bind(self, context, data: object):
        if (
            getattr(context.descriptor, "command_name", None) != "goto"
            or context.goto_binding_rejected
        ):
            return context
        binding = None
        if (
            isinstance(data, Mapping)
            and data.get("goto_profile_id") == "fabric_chatclef_goto_terminal"
            and type(data.get("goto_profile_version")) is int
            and data.get("goto_profile_version") == 1
        ):
            binding = self._decoder.decode(data.get("goto_binding"))
        if binding is None or not self._matcher.matches_context(binding, context):
            return replace(context, goto_binding_rejected=True)
        if context.goto_binding is None:
            return replace(context, goto_binding=binding)
        if context.goto_binding != binding:
            return replace(context, goto_binding_rejected=True)
        return context
