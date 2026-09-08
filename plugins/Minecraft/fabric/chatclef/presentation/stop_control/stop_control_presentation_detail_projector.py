#20260907_kpopmodder: Project only the validated STOP command identity into UI detail.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.presentation.command_lifecycle import (
    CommandLifecyclePresentationDetailLogPolicy,
)


class StopControlPresentationDetailProjector:
    _DETAIL = '{"command_name":"stop","form_kind":"trusted_stop"}'

    def project(self) -> str:
        return CommandLifecyclePresentationDetailLogPolicy.validate(
            self._DETAIL
        )


__all__ = ("StopControlPresentationDetailProjector",)
