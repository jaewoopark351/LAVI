#20260907_kpopmodder: Own one trusted zero-submission generalized status response.
#20260908_kpopmodder: Keep STATUS routing orchestration fail-closed across each bounded stage.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle.classification import (
    CommandStatusClassificationFailure,
    CommandStatusQuery,
)
from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle.diagnostics import (
    CommandStatusRouteFailureDiagnostics,
)
from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle.publication import (
    CommandStatusPublicationCustodyPolicy,
)
from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)
from plugins.Minecraft.fabric.chatclef.presentation.command_lifecycle import (
    CommandLifecyclePresentationDetailProjector,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackLifecycleSnapshot,
    ContextualCommandStatusClaimFailure,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.status.publication import (
    CommandStatusPublicationHandoffFailure,
)

from .decision import (
    COMMAND_STATUS_EMERGENCY_DECISION,
    CommandStatusRouteDecisionFactory,
)


class CommandStatusRouteOwner:
    _LIFECYCLE_STATES = frozenset(
        {
            CommandFeedbackLifecycleSnapshot.RUNNING,
            CommandFeedbackLifecycleSnapshot.PENDING,
            CommandFeedbackLifecycleSnapshot.IDLE,
            CommandFeedbackLifecycleSnapshot.UNAVAILABLE,
        }
    )
    _TERMINAL_STATES = frozenset({"none", "unclaimed", "claimed"})
    _ACTIVE_STATES = frozenset(
        {
            CommandFeedbackLifecycleSnapshot.RUNNING,
            CommandFeedbackLifecycleSnapshot.PENDING,
        }
    )
    _NO_OWNER_STATES = frozenset(
        {
            CommandFeedbackLifecycleSnapshot.IDLE,
            CommandFeedbackLifecycleSnapshot.UNAVAILABLE,
        }
    )
    _NO_OWNER_TERMINAL_STATES = frozenset({"none", "claimed"})

    def __init__(
        self,
        *,
        extension,
        live_proof_validator,
        classifier,
        response_renderer,
        presentation_detail_projector=None,
        decision_factory=None,
        failure_diagnostics=None,
        publication_custody_policy=None,
    ) -> None:
        self._extension = extension
        self._live_proof_validator = live_proof_validator
        self._classifier = classifier
        self._response_renderer = response_renderer
        self._presentation_details = (
            presentation_detail_projector
            if presentation_detail_projector is not None
            else CommandLifecyclePresentationDetailProjector()
        )
        self._decisions = (
            decision_factory
            if decision_factory is not None
            else CommandStatusRouteDecisionFactory()
        )
        self._failure_diagnostics = (
            failure_diagnostics
            if failure_diagnostics is not None
            else CommandStatusRouteFailureDiagnostics()
        )
        self._publication_custody_policy = (
            publication_custody_policy
            if publication_custody_policy is not None
            else CommandStatusPublicationCustodyPolicy()
        )

    def try_route(self, event: object, korean_eligibility_proof: object):
        query = None
        try:
            query = self._classifier.classify(getattr(event, "text", None))
        except CommandStatusClassificationFailure as failure:
            if not self._proof_is_live(
                event,
                korean_eligibility_proof,
                query=None,
            ):
                return None
            return self._cautious_or_emergency(
                query=None,
                snapshot=None,
                acknowledgement=None,
                failure=(failure.stage, failure.exception_class),
            )
        except Exception as error:
            if not self._proof_is_live(
                event,
                korean_eligibility_proof,
                query=None,
            ):
                return None
            return self._cautious_or_emergency(
                query=None,
                snapshot=None,
                acknowledgement=None,
                failure=("input_validation", type(error).__name__),
            )

        if query is None:
            return None
        if not self._proof_is_live(event, korean_eligibility_proof, query=query):
            return None
        if type(query) is not CommandStatusQuery:
            return self._cautious_or_emergency(
                query=query,
                snapshot=None,
                acknowledgement=None,
                failure=("input_validation", "TypeError"),
            )

        try:
            snapshot = self._inspect(query)
        except ContextualCommandStatusClaimFailure as failure:
            return self._route_preclaim_failure(
                query=query,
                stage=failure.stage,
                exception_class=failure.exception_class,
            )
        except Exception as error:
            return self._route_preclaim_failure(
                query=query,
                stage="state_inspection",
                exception_class=type(error).__name__,
            )

        if type(snapshot) is CommandStatusPublicationHandoffFailure:
            return self._emergency_decision()
        if snapshot is None:
            return self._route_without_claim(query=query)
        acknowledgement = self._snapshot_acknowledgement(snapshot)
        if (
            not self._snapshot_is_valid(snapshot)
            or not self._snapshot_is_consistent(snapshot)
        ):
            if acknowledgement is not None:
                return self._fail_closed_with_acknowledgement(
                    query=query,
                    snapshot=snapshot,
                    acknowledgement=acknowledgement,
                    stage="state_inspection",
                    exception_class="TypeError",
                )
            return self._route_preclaim_failure(
                query=query,
                stage="state_inspection",
                exception_class="TypeError",
                snapshot=snapshot,
            )
        if snapshot.terminal_state == "claimed":
            if acknowledgement is not None:
                return self._fail_closed_with_acknowledgement(
                    query=query,
                    snapshot=snapshot,
                    acknowledgement=acknowledgement,
                    stage="state_inspection",
                    exception_class="TypeError",
                )
            return self._route_preclaim_failure(
                query=query,
                stage="state_inspection",
                exception_class="TypeError",
                snapshot=snapshot,
            )

        if snapshot.query_matched is not True:
            return self._route_without_claim(
                query=query,
                snapshot=snapshot,
                acknowledgement=acknowledgement,
            )
        if (
            query.addressed is not True
            and (
                snapshot.owner_present is not True
                or snapshot.descriptor is None
            )
        ):
            return self._route_without_claim(
                query=query,
                snapshot=snapshot,
                acknowledgement=acknowledgement,
            )

        try:
            response_text = self._response_renderer.render_status(snapshot, query)
            if type(response_text) is not str or not response_text:
                raise TypeError("STATUS renderer must return nonempty text")
        except Exception as error:
            return self._cautious_or_emergency(
                query=query,
                snapshot=snapshot,
                acknowledgement=acknowledgement,
                failure=("status_rendering", type(error).__name__),
            )

        try:
            presentation_detail_log = self._presentation_details.project(
                snapshot.descriptor
            )
            if type(presentation_detail_log) is not str:
                raise TypeError("STATUS presentation detail must be text")
        except Exception as error:
            return self._cautious_or_emergency(
                query=query,
                snapshot=snapshot,
                acknowledgement=acknowledgement,
                failure=("presentation_detail_projection", type(error).__name__),
            )

        try:
            decision = self._decisions.status(
                response_text=response_text,
                state=snapshot.state,
                acknowledgement=acknowledgement,
                presentation_detail_log=presentation_detail_log,
            )
        except Exception as error:
            return self._cautious_or_emergency(
                query=query,
                snapshot=snapshot,
                acknowledgement=acknowledgement,
                failure=("primary_decision_assembly", type(error).__name__),
            )
        if not self._status_decision_is_valid(decision, acknowledgement):
            return self._fail_closed_with_acknowledgement(
                query=query,
                snapshot=snapshot,
                acknowledgement=acknowledgement,
                stage="primary_decision_assembly",
                exception_class="TypeError",
            )
        return decision

    def _proof_is_live(
        self,
        event: object,
        korean_eligibility_proof: object,
        *,
        query: object,
    ) -> bool:
        try:
            return (
                self._live_proof_validator(korean_eligibility_proof, event)
                is True
            )
        except Exception as error:
            self._record_failure(
                stage="proof_validation",
                exception_class=type(error).__name__,
                query=query,
                snapshot=None,
                acknowledgement=None,
            )
            return False

    def _route_preclaim_failure(
        self,
        *,
        query: CommandStatusQuery,
        stage: str,
        exception_class: str,
        snapshot: object = None,
    ):
        if query.addressed is not True:
            self._record_failure(
                stage=stage,
                exception_class=exception_class,
                query=query,
                snapshot=snapshot,
                acknowledgement=None,
            )
            return self._fallthrough_or_emergency(
                query=query,
                snapshot=snapshot,
                acknowledgement=None,
                record_failure=False,
            )
        return self._cautious_or_emergency(
            query=query,
            snapshot=snapshot,
            acknowledgement=None,
            failure=(stage, exception_class),
        )

    def _route_without_claim(
        self,
        *,
        query: CommandStatusQuery,
        snapshot: object = None,
        acknowledgement: object = None,
    ):
        if acknowledgement is not None:
            self._record_failure(
                stage="state_inspection",
                exception_class="TypeError",
                query=query,
                snapshot=snapshot,
                acknowledgement=acknowledgement,
            )
            self._acknowledge_false(acknowledgement)
            return self._emergency_decision()
        if query.addressed is not True:
            return self._fallthrough_or_emergency(
                query=query,
                snapshot=snapshot,
                acknowledgement=None,
            )
        return self._cautious_or_emergency(
            query=query,
            snapshot=snapshot,
            acknowledgement=acknowledgement,
        )

    def _fallthrough_or_emergency(
        self,
        *,
        query: object,
        snapshot: object,
        acknowledgement: object,
        record_failure: bool = True,
    ):
        try:
            return self._decisions.conversational_fallthrough()
        except Exception as error:
            if record_failure:
                self._record_failure(
                    stage="primary_decision_assembly",
                    exception_class=type(error).__name__,
                    query=query,
                    snapshot=snapshot,
                    acknowledgement=acknowledgement,
                )
            self._acknowledge_false(acknowledgement)
            return self._emergency_decision()

    def _cautious_or_emergency(
        self,
        *,
        query: object,
        snapshot: object,
        acknowledgement: object,
        failure: tuple[str, str] | None = None,
    ):
        if failure is not None:
            self._record_failure(
                stage=failure[0],
                exception_class=failure[1],
                query=query,
                snapshot=snapshot,
                acknowledgement=acknowledgement,
            )
        try:
            decision = self._decisions.cautious(
                acknowledgement=acknowledgement,
            )
        except Exception as error:
            if failure is None:
                self._record_failure(
                    stage="fallback_decision_assembly",
                    exception_class=type(error).__name__,
                    query=query,
                    snapshot=snapshot,
                    acknowledgement=acknowledgement,
                )
            self._acknowledge_false(acknowledgement)
            return self._emergency_decision()
        if not self._status_decision_is_valid(decision, acknowledgement):
            if failure is None:
                self._record_failure(
                    stage="fallback_decision_assembly",
                    exception_class="TypeError",
                    query=query,
                    snapshot=snapshot,
                    acknowledgement=acknowledgement,
                )
            self._acknowledge_false(acknowledgement)
            return self._emergency_decision()
        return decision

    def _fail_closed_with_acknowledgement(
        self,
        *,
        query: object,
        snapshot: object,
        acknowledgement: object,
        stage: str,
        exception_class: str,
    ):
        self._record_failure(
            stage=stage,
            exception_class=exception_class,
            query=query,
            snapshot=snapshot,
            acknowledgement=acknowledgement,
        )
        self._acknowledge_false(acknowledgement)
        return self._emergency_decision()

    def _record_failure(
        self,
        *,
        stage: str,
        exception_class: str,
        query: object,
        snapshot: object,
        acknowledgement: object,
    ) -> None:
        custody = None
        try:
            custody = getattr(
                acknowledgement,
                "publication_failure_diagnostic_custody",
                None,
            )
        except Exception:
            pass
        try:
            record_once = getattr(custody, "record_once", None)
        except Exception:
            record_once = None
        if callable(record_once):
            try:
                record_once(stage=stage, exception_class=exception_class)
            except Exception:
                pass
            return
        try:
            self._failure_diagnostics.record(
                stage=stage,
                exception_class=exception_class,
                query=query,
                snapshot=snapshot,
            )
        except Exception:
            pass

    @staticmethod
    def _acknowledge_false(acknowledgement: object) -> None:
        try:
            callback = getattr(acknowledgement, "acknowledge", None)
        except Exception:
            return
        if not callable(callback):
            return
        try:
            callback(published=False)
        except Exception:
            pass

    def _emergency_decision(self):
        try:
            decision = self._decisions.emergency()
        except Exception:
            return COMMAND_STATUS_EMERGENCY_DECISION
        return (
            decision
            if decision is COMMAND_STATUS_EMERGENCY_DECISION
            else COMMAND_STATUS_EMERGENCY_DECISION
        )

    @classmethod
    def _snapshot_is_valid(cls, snapshot: object) -> bool:
        return (
            type(snapshot) is CommandFeedbackLifecycleSnapshot
            and type(snapshot.state) is str
            and snapshot.state in cls._LIFECYCLE_STATES
            and type(snapshot.query_matched) is bool
            and type(snapshot.owner_present) is bool
            and type(snapshot.terminal_state) is str
            and snapshot.terminal_state in cls._TERMINAL_STATES
        )

    @classmethod
    def _snapshot_is_consistent(cls, snapshot: object) -> bool:
        descriptor_present = snapshot.descriptor is not None
        if snapshot.owner_present:
            return (
                descriptor_present
                and snapshot.terminal_state == "unclaimed"
                and snapshot.state != CommandFeedbackLifecycleSnapshot.IDLE
                and (
                    snapshot.state not in cls._ACTIVE_STATES
                    or snapshot.query_matched is True
                )
            )
        return (
            not descriptor_present
            and snapshot.terminal_state in cls._NO_OWNER_TERMINAL_STATES
            and snapshot.state in cls._NO_OWNER_STATES
        )

    @staticmethod
    def _snapshot_acknowledgement(snapshot: object):
        try:
            return getattr(snapshot, "publication_acknowledgement", None)
        except Exception:
            return None

    def _status_decision_is_valid(
        self,
        decision: object,
        acknowledgement: object,
    ) -> bool:
        try:
            if type(decision) is not MinecraftChatClefInputRouteDecision:
                return False
            if decision.handled is not True:
                return False
            if decision.route_kind != "command_status_query":
                return False
            if decision.response_kind != "command_status":
                return False
            if (
                decision.response_publication_acknowledgement
                is not acknowledgement
            ):
                return False
            if acknowledgement is None:
                return True
            return self._publication_custody_policy.matches(decision) is True
        except Exception:
            return False

    def _inspect(self, query: object):
        inspect = getattr(self._extension, "inspect_command_feedback_status", None)
        if callable(inspect):
            return inspect(query)
        legacy = getattr(self._extension, "inspect_crafting_feedback_status", None)
        if (
            callable(legacy)
            and query.requested_family == "item_get"
            and query.target_text == ""
        ):
            return legacy(None)
        return None


__all__ = ("CommandStatusRouteOwner",)
