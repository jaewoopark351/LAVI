#20260827_kpopmodder: Validate typed STORE_HOME terminal data before it crosses Python boundaries.
from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Mapping


@dataclass(frozen=True)
class StoreHomeTerminalPayload:
    result: str
    stored_items: int
    remaining_stacks: int
    reason: str
    goal_satisfied: bool

    _KNOWN_RESULTS = frozenset(
        {
            "COMPLETED",
            "PARTIAL_TRUSTED_CAPACITY_EXHAUSTED",
            "PARTIAL_TRUSTED_DESTINATIONS_UNAVAILABLE",
            "NO_USABLE_TRUSTED_DESTINATION",
            "NO_TRUSTED_CAPACITY",
            "CURSOR_NOT_EMPTY",
            "MANIFEST_STALE",
            "CONTEXT_CHANGED",
            "TRANSFER_UNCONFIRMED",
            "INTERRUPTED",
        }
    )
    _FIELDS = (
        "operation",
        "store_home_result",
        "stored_items",
        "remaining_stacks",
        "reason",
        "goal_satisfied",
    )

    @classmethod
    def from_command_result(
        cls,
        command_result: Mapping[str, Any],
    ) -> "StoreHomeTerminalPayload | None":
        candidates = cls._candidate_maps(command_result)
        typed = [
            candidate
            for candidate in candidates
            if cls.claims_store_home(candidate)
        ]
        if not typed:
            return None
        first_values = tuple(typed[0].get(field) for field in cls._FIELDS)
        if any(
            tuple(candidate.get(field) for field in cls._FIELDS) != first_values
            for candidate in typed[1:]
        ):
            return None
        return cls.from_data(typed[0])

    @classmethod
    def from_data(
        cls,
        value: Mapping[str, Any],
    ) -> "StoreHomeTerminalPayload | None":
        result = value.get("store_home_result")
        stored_items = value.get("stored_items")
        remaining_stacks = value.get("remaining_stacks")
        reason = value.get("reason")
        goal_satisfied = value.get("goal_satisfied")
        if value.get("operation") != "store_home":
            return None
        if not isinstance(result, str) or result not in cls._KNOWN_RESULTS:
            return None
        if type(stored_items) is not int or stored_items < 0:
            return None
        if type(remaining_stacks) is not int or remaining_stacks < 0:
            return None
        if not isinstance(reason, str) or not reason.strip():
            return None
        if type(goal_satisfied) is not bool:
            return None
        if goal_satisfied != (result == "COMPLETED"):
            return None
        if result == "COMPLETED" and remaining_stacks != 0:
            return None
        if result.startswith("PARTIAL_") and stored_items == 0:
            return None
        return cls(
            result=result,
            stored_items=stored_items,
            remaining_stacks=remaining_stacks,
            reason=reason.strip(),
            goal_satisfied=goal_satisfied,
        )

    @classmethod
    def claims_store_home(cls, value: Mapping[str, Any]) -> bool:
        return value.get("operation") == "store_home" or "store_home_result" in value

    @classmethod
    def _candidate_maps(
        cls,
        command_result: Mapping[str, Any],
    ) -> list[Mapping[str, Any]]:
        candidates: list[Mapping[str, Any]] = []
        details = command_result.get("details")
        if isinstance(details, Mapping):
            candidates.append(details)
        status = command_result.get("status")
        if isinstance(status, Mapping):
            data = status.get("data")
            if isinstance(data, Mapping):
                candidates.append(data)
        return candidates
