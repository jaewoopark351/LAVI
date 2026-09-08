#20260905_kpopmodder: Keep trusted Korean route responsibilities split by module.
#20260909_kpopmodder: Prepare one verified current-work response without command authority.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)
from plugins.Minecraft.fabric.chatclef.presentation.command_lifecycle import (
    CommandLifecyclePresentationDetailProjector,
)
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle import (
    CommandLifecycleResponseRenderer,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandBusyObservedIdentityFactory,
    CommandFeedbackLifecycleSnapshot,
    CommandFeedbackPublicationAcknowledgement,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.status.publication import (
    CommandStatusPublicationHandoffFailure,
)

from .contextual_busy_response_evaluation import (
    ContextualBusyResponseEvaluation,
)
from .contextual_busy_response_evaluator import ContextualBusyResponseEvaluator
from .contextual_busy_response_preparation import ContextualBusyResponsePreparation
from .contextual_busy_route_decision_decorator import (
    ContextualBusyRouteDecisionDecorator,
)
from .contextual_busy_suppressed_decision import (
    CONTEXTUAL_BUSY_SUPPRESSED_DECISION,
)
from .diagnostics import ContextualBusyResponseFailureLogger


class ContextualBusyResponseCoordinator:
    _ACTIVE_STATES = frozenset(
        {
            CommandFeedbackLifecycleSnapshot.RUNNING,
            CommandFeedbackLifecycleSnapshot.PENDING,
        }
    )

    def __init__(
        self,
        *,
        inspect_busy_status_callback=None,
        evaluator=None,
        identity_factory=None,
        response_renderer=None,
        presentation_detail_projector=None,
        decision_decorator=None,
        failure_logger=None,
    ) -> None:
        self._inspect_busy_status = inspect_busy_status_callback
        self._evaluator = evaluator or ContextualBusyResponseEvaluator()
        self._identities = identity_factory or CommandBusyObservedIdentityFactory()
        self._renderer = response_renderer or CommandLifecycleResponseRenderer()
        self._presentation_details = (
            presentation_detail_projector
            or CommandLifecyclePresentationDetailProjector()
        )
        self._decisions = decision_decorator or ContextualBusyRouteDecisionDecorator()
        self._failures = failure_logger or ContextualBusyResponseFailureLogger()

    def prepare(
        self,
        *,
        decision: object,
        event: object,
        proof: object,
        live_proof_validator,
    ) -> ContextualBusyResponsePreparation:
        if not self._candidate(decision):
            return ContextualBusyResponsePreparation(decision)

        try:
            proof_is_live = live_proof_validator(proof, event)
            evaluation = self._evaluator.evaluate(
                decision,
                event=event,
                proof_is_live=proof_is_live,
            )
        except Exception as error:
            self._record_prepermit(
                stage="decision_validation",
                availability_reason="malformed_observation",
                exception_class=type(error).__name__,
            )
            return ContextualBusyResponsePreparation(decision)
        if (
            type(evaluation) is not ContextualBusyResponseEvaluation
            or evaluation.candidate is not True
            or evaluation.verified is not True
        ):
            self._record_prepermit(
                stage="decision_validation",
                availability_reason="malformed_observation",
            )
            return ContextualBusyResponsePreparation(decision)

        try:
            identity = self._identities.create(decision.result)
        except Exception as error:
            self._record_prepermit(
                stage="identity_freeze",
                availability_reason="malformed_observation",
                exception_class=type(error).__name__,
            )
            return ContextualBusyResponsePreparation(decision)
        if identity is None:
            self._record_prepermit(
                stage="identity_freeze",
                availability_reason="malformed_observation",
            )
            return ContextualBusyResponsePreparation(decision)

        try:
            if not callable(self._inspect_busy_status):
                raise TypeError("contextual busy status inspector is unavailable")
            snapshot = self._inspect_busy_status(identity)
        except Exception as error:
            self._record_prepermit(
                stage="locked_status_inspection",
                availability_reason="evidence_unavailable",
                exception_class=type(error).__name__,
            )
            return ContextualBusyResponsePreparation(decision)

        if type(snapshot) is CommandStatusPublicationHandoffFailure:
            return ContextualBusyResponsePreparation(
                CONTEXTUAL_BUSY_SUPPRESSED_DECISION
            )

        acknowledgement = self._acknowledgement(snapshot)
        if type(acknowledgement) is CommandFeedbackPublicationAcknowledgement:
            if not self._snapshot_is_publishable(snapshot):
                return self._cautious_or_suppressed(
                    decision=decision,
                    acknowledgement=acknowledgement,
                    stage="primary_decision_assembly",
                    exception_class="TypeError",
                )
            return self._render(
                decision=decision,
                snapshot=snapshot,
                acknowledgement=acknowledgement,
            )

        if acknowledgement is not None:
            self._acknowledge_false(acknowledgement)
            return ContextualBusyResponsePreparation(
                CONTEXTUAL_BUSY_SUPPRESSED_DECISION
            )

        self._record_prepermit(
            stage="locked_status_inspection",
            snapshot=snapshot,
            availability_reason=self._availability_reason(snapshot),
        )
        return ContextualBusyResponsePreparation(decision)

    def _candidate(self, decision: object) -> bool:
        try:
            return self._evaluator.is_candidate(decision) is True
        except Exception as error:
            if self._looks_like_busy(decision):
                self._record_prepermit(
                    stage="decision_validation",
                    availability_reason="malformed_observation",
                    exception_class=type(error).__name__,
                )
            return False

    def _render(self, *, decision, snapshot, acknowledgement):
        try:
            response_text = self._renderer.render_status(snapshot, query=None)
        except Exception as error:
            return self._cautious_or_suppressed(
                decision=decision,
                acknowledgement=acknowledgement,
                stage="status_rendering",
                exception_class=type(error).__name__,
            )
        try:
            presentation_detail_log = self._presentation_details.project(
                snapshot.descriptor
            )
        except Exception as error:
            return self._cautious_or_suppressed(
                decision=decision,
                acknowledgement=acknowledgement,
                stage="presentation_detail_projection",
                exception_class=type(error).__name__,
            )
        try:
            prepared = self._decisions.current_work(
                decision,
                response_text=response_text,
                acknowledgement=acknowledgement,
                presentation_detail_log=presentation_detail_log,
            )
        except Exception as error:
            return self._cautious_or_suppressed(
                decision=decision,
                acknowledgement=acknowledgement,
                stage="primary_decision_assembly",
                exception_class=type(error).__name__,
            )
        if not self._decision_is_valid(
            prepared,
            original=decision,
            acknowledgement=acknowledgement,
        ):
            return self._cautious_or_suppressed(
                decision=decision,
                acknowledgement=acknowledgement,
                stage="primary_decision_assembly",
                exception_class="TypeError",
            )
        return ContextualBusyResponsePreparation(
            prepared,
            local_handoff_token=acknowledgement,
        )

    def _cautious_or_suppressed(
        self,
        *,
        decision,
        acknowledgement,
        stage: str,
        exception_class: str,
    ) -> ContextualBusyResponsePreparation:
        self._record_acknowledged(
            acknowledgement,
            stage=stage,
            exception_class=exception_class,
        )
        try:
            cautious = self._decisions.cautious(
                decision,
                acknowledgement=acknowledgement,
            )
        except Exception as error:
            self._record_acknowledged(
                acknowledgement,
                stage="fallback_decision_assembly",
                exception_class=type(error).__name__,
            )
            self._acknowledge_false(acknowledgement)
            return ContextualBusyResponsePreparation(
                CONTEXTUAL_BUSY_SUPPRESSED_DECISION
            )
        if not self._decision_is_valid(
            cautious,
            original=decision,
            acknowledgement=acknowledgement,
        ):
            self._record_acknowledged(
                acknowledgement,
                stage="fallback_decision_assembly",
                exception_class="TypeError",
            )
            self._acknowledge_false(acknowledgement)
            return ContextualBusyResponsePreparation(
                CONTEXTUAL_BUSY_SUPPRESSED_DECISION
            )
        return ContextualBusyResponsePreparation(
            cautious,
            local_handoff_token=acknowledgement,
        )

    @classmethod
    def _snapshot_is_publishable(cls, snapshot: object) -> bool:
        try:
            return bool(
                type(snapshot) is CommandFeedbackLifecycleSnapshot
                and snapshot.state in cls._ACTIVE_STATES
                and snapshot.descriptor is not None
                and snapshot.query_matched is True
                and snapshot.query_family_matched is True
                and snapshot.query_target_matched is True
                and snapshot.owner_present is True
                and snapshot.availability_reason == ""
                and snapshot.terminal_state == "unclaimed"
            )
        except Exception:
            return False

    @staticmethod
    def _decision_is_valid(decision, *, original, acknowledgement) -> bool:
        try:
            return bool(
                type(decision) is MinecraftChatClefInputRouteDecision
                and decision.handled is True
                and decision.reason == "minecraft_command_busy"
                and decision.result is original.result
                and decision.translation is original.translation
                and decision.route_kind == "command_busy_current_work"
                and decision.response_kind == "command_status"
                and type(decision.response_text) is str
                and bool(decision.response_text)
                and decision.response_publication_acknowledgement
                is acknowledgement
                and decision.publish_external_response is False
                and decision.response_source == "minecraft_chatclef"
                and decision.response_emission_capability is None
                and decision.suppress_response is False
            )
        except Exception:
            return False

    def _record_prepermit(self, **values) -> None:
        try:
            self._failures.record(**values)
        except Exception:
            pass

    @staticmethod
    def _record_acknowledged(
        acknowledgement: object,
        *,
        stage: str,
        exception_class: str,
    ) -> None:
        try:
            custody = acknowledgement.publication_failure_diagnostic_custody
            record_once = getattr(custody, "record_once", None)
            if callable(record_once):
                record_once(stage, exception_class)
        except Exception:
            pass

    @staticmethod
    def _acknowledgement(snapshot: object):
        try:
            return snapshot.publication_acknowledgement
        except Exception:
            return None

    @staticmethod
    def _acknowledge_false(acknowledgement: object) -> None:
        try:
            callback = acknowledgement.acknowledge
            if callable(callback):
                callback(published=False)
        except Exception:
            pass

    @staticmethod
    def _availability_reason(snapshot: object) -> str:
        try:
            reason = snapshot.availability_reason
            if type(reason) is str and reason:
                return reason
        except Exception:
            pass
        return "identity_mismatch" if snapshot is None else "evidence_unavailable"

    @staticmethod
    def _looks_like_busy(decision: object) -> bool:
        try:
            return decision.reason == "minecraft_command_busy"
        except Exception:
            return False


__all__ = ("ContextualBusyResponseCoordinator",)
