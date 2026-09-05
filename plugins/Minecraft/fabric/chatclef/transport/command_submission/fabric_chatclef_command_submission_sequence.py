#20260905_kpopmodder: Sequence focused stages for exactly one ordinary-command submission.
from __future__ import annotations

from plugins.Minecraft.common.dto.command_request_dto import CommandRequestDTO
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.bridge_error_code import BridgeErrorCode


class FabricChatClefCommandSubmissionSequence:
    def __init__(
        self,
        *,
        loop_provider,
        envelope_factory,
        admission,
        transport_delivery,
        outcome_policy,
        event_reporter,
        result_factory,
    ) -> None:
        self._loop_provider = loop_provider
        self._envelope_factory = envelope_factory
        self._admission = admission
        self._transport_delivery = transport_delivery
        self._outcome_policy = outcome_policy
        self._event_reporter = event_reporter
        self._result_factory = result_factory

    def submit(self, request: CommandRequestDTO) -> CommandResultDTO:
        loop = self._loop_provider()
        message_id = self._envelope_factory.new_message_id()
        decision = self._admission.inspect(
            request,
            message_id=message_id,
            loop=loop,
        )
        self._event_reporter.admission(decision)
        if not decision.accepted:
            return self._result_factory.rejected(
                decision.request,
                decision.error_code or BridgeErrorCode.INTERNAL_ERROR,
                decision.message,
            )

        context = decision.command_context
        envelope = self._envelope_factory.build(
            request=decision.request,
            command_context=context,
            message_id=message_id,
        )
        delivery = self._transport_delivery.deliver(
            command_context=context,
            envelope=envelope,
            loop=loop,
        )
        if self._outcome_policy.was_not_scheduled(delivery):
            commands = self._admission.release_if_not_scheduled(context)
            self._event_reporter.schedule_failed(
                decision.request,
                error=delivery.error,
                commands=commands,
            )
            return self._result_factory.rejected(
                decision.request,
                BridgeErrorCode.INTERNAL_ERROR,
                self._outcome_policy.unscheduled_message(delivery.error),
            )
        if self._outcome_policy.outcome_is_unknown(delivery):
            commands = self._admission.snapshot()
            self._event_reporter.outcome_unknown(
                decision.request,
                error=delivery.error,
                commands=commands,
            )
            return self._result_factory.unknown(
                decision.request,
                context,
                self._outcome_policy.unknown_error(delivery.error),
            )

        self._event_reporter.succeeded(
            decision.request,
            command_context=context,
            commands_provider=self._admission.snapshot,
        )
        return self._result_factory.accepted(decision.request, context)
