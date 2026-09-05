#20260905_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO


class StopControlTerminalStateCoordinator:
    def __init__(
        self,
        *,
        connection_ownership: object,
        tracker_registry: object,
        admission_barrier: object,
    ):
        self._connection_ownership = connection_ownership
        self._tracker_registry = tracker_registry
        self._admission_barrier = admission_barrier

    def quarantine_malformed(
        self,
        tracker: object,
    ) -> tuple[object, str, str, str]:
        tracker.enter_quarantine()
        return (
            None,
            "closed",
            "malformed_quarantine",
            "malformed_matching_control_result",
        )

    def apply(
        self,
        *,
        result: CommandResultDTO,
        decision: object,
        tracker: object,
    ) -> tuple[object, str, str, str | None]:
        if decision.valid and decision.release_barrier:
            frozen_owner_match = self._retire_exact_original_owner_if_authorized(
                result,
                tracker,
            )
            retired = self._tracker_registry.retire(tracker)
            released = (
                self._admission_barrier.release(tracker.barrier_token)
                if retired
                else False
            )
            if released and tracker.mark_released():
                barrier_state = "released"
            else:
                tracker.enter_quarantine()
                barrier_state = "closed"
            retirement_evidence = (
                "verified_stopped"
                if decision.control_outcome == "stopped"
                else "proved_no_mutation"
            )
            return frozen_owner_match, barrier_state, retirement_evidence, None

        tracker.enter_quarantine()
        diagnostic_disposition = (
            None if decision.valid else "invalid_control_terminal_profile"
        )
        return (
            None,
            "closed",
            "unknown_quarantine",
            diagnostic_disposition,
        )

    def _retire_exact_original_owner_if_authorized(
        self,
        result: CommandResultDTO,
        tracker: object,
    ) -> bool | None:
        data = result.data
        target = tracker.target
        if target is None or data.get("target_resolution") != "exact":
            return None
        return self._connection_ownership.clear_command_if_identity(
            request_id=target.request_id,
            command_message_id=target.command_message_id,
            session_id=target.session_id,
            server_connection_generation=target.server_connection_generation,
            owner_token=target.owner_token,
        )


__all__ = ("StopControlTerminalStateCoordinator",)
