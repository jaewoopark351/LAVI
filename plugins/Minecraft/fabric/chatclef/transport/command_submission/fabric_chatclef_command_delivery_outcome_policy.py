#20260905_kpopmodder: Define fail-closed policy for unscheduled and unknown command delivery.
from __future__ import annotations


class FabricChatClefCommandDeliveryOutcomePolicy:
    @staticmethod
    def was_not_scheduled(delivery) -> bool:
        return delivery.status == "not_scheduled"

    @staticmethod
    def outcome_is_unknown(delivery) -> bool:
        return delivery.status == "outcome_unknown"

    @staticmethod
    def unscheduled_message(error: Exception | None) -> str:
        return (
            "Fabric ChatClef command could not be scheduled: "
            f"{_error_text(error)}"
        )

    @staticmethod
    def unknown_error(error: Exception | None) -> Exception:
        return error or RuntimeError("unknown delivery error")


def _error_text(error: Exception | None) -> str:
    if error is None:
        return "UnknownError: unknown error"
    return f"{type(error).__name__}: {error}"
