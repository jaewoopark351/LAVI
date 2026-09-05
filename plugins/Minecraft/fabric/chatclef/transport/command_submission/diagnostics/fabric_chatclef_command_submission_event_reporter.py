#20260905_kpopmodder: Report lifecycle events for one ordinary-command submission.
from __future__ import annotations


class FabricChatClefCommandSubmissionEventReporter:
    def __init__(self, *, diagnostics, details_factory) -> None:
        self._diagnostics = diagnostics
        self._details_factory = details_factory

    def admission(self, decision) -> None:
        self._diagnostics.log(
            decision.event,
            decision.request,
            decision.details or {},
        )

    def schedule_failed(self, request, *, error, commands) -> None:
        self._diagnostics.log(
            "command_schedule_failed",
            request,
            self._details_factory.delivery_error(error=error, commands=commands),
        )

    def outcome_unknown(self, request, *, error, commands) -> None:
        self._diagnostics.log(
            "command_send_outcome_unknown",
            request,
            self._details_factory.outcome_unknown(
                error=error,
                commands=commands,
            ),
        )

    def succeeded(self, request, *, command_context, commands_provider) -> None:
        self._diagnostics.log(
            "command_send_succeeded",
            request,
            self._details_factory.succeeded(
                command_context=command_context,
                commands_provider=commands_provider,
            ),
        )
