#20260907_kpopmodder: Isolate automatic-deposit registered raw forms.
from __future__ import annotations

import re


class CommandFeedbackRawAutoDepositGrammar:
    GRAMMAR_IDS = frozenset(
        {"automatic_trust", "automatic_untrust", "korean_automatic_trust"}
    )
    _DESTINATION_ID = re.compile(r"td_[0-9a-f]{24}\Z", re.ASCII)

    def decode(
        self,
        grammar_id: str,
        _command_name: str,
        arguments: tuple[str, ...],
    ) -> str | None:
        if grammar_id == "automatic_trust":
            if not arguments:
                return "crosshair_target"
            if arguments == ("area", "16x16"):
                return "area_16x16"
            if arguments == ("반경", "16x16"):
                return "radius_16x16"
            return None
        if grammar_id == "korean_automatic_trust":
            if arguments == ("영역", "16x16"):
                return "area_16x16"
            if arguments == ("반경", "16x16"):
                return "radius_16x16"
            return None
        if grammar_id == "automatic_untrust":
            if not arguments:
                return "crosshair_target"
            if len(arguments) == 1 and self._DESTINATION_ID.fullmatch(arguments[0]):
                return "explicit_destination"
        return None


__all__ = ("CommandFeedbackRawAutoDepositGrammar",)
