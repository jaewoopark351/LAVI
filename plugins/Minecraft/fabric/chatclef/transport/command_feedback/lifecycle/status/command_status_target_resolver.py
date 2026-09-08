#20260907_kpopmodder: Match one classified status query to immutable active slots.
from __future__ import annotations

import re

from .command_status_resolution import CommandStatusResolution


class CommandStatusTargetResolver:
    def resolve(self, query: object, descriptor: object) -> CommandStatusResolution:
        family = str(getattr(query, "requested_family", "") or "")
        target_text = str(getattr(query, "target_text", "") or "").strip()
        addressed = getattr(query, "addressed", False) is True
        family_matched = family in {
            "any",
            getattr(descriptor, "requested_family", ""),
        }
        target_matched = not target_text or self._target_matches(
            target_text,
            descriptor,
        )
        if addressed:
            claimed = True
        else:
            claimed = family_matched and target_matched and bool(target_text)
        return CommandStatusResolution(
            claimed=claimed,
            family_matched=family_matched,
            target_matched=target_matched,
        )

    def _target_matches(self, target_text: str, descriptor: object) -> bool:
        requested = self._normalize(target_text)
        base_candidates = {
            self._normalize(getattr(descriptor, "target_item", "")),
            self._normalize(getattr(descriptor, "spoken_target_label", "")),
            self._normalize(getattr(descriptor, "player_name", "")),
        }
        base_candidates.discard("")
        candidates = set(base_candidates)
        for candidate in base_candidates:
            candidates.add(f"{candidate}을")
            candidates.add(f"{candidate}를")
        quantity = getattr(descriptor, "requested_count", None)
        label = self._normalize(
            getattr(descriptor, "spoken_target_label", "")
        )
        if type(quantity) is int and label:
            quantified = f"{label} {quantity}개"
            candidates.update(
                {quantified, f"{quantified}을", f"{quantified}를"}
            )
        return bool(requested and requested in candidates)

    @staticmethod
    def _normalize(value: object) -> str:
        return re.sub(
            r"\s+",
            " ",
            str(value or "").replace("_", " ").strip().lower(),
        )


__all__ = ("CommandStatusTargetResolver",)
