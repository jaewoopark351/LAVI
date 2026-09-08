#20260908_kpopmodder: Project status-route failures into a closed bounded vocabulary.
from __future__ import annotations

import re

from plugins.Minecraft.fabric.chatclef.command_registry import (
    KoreanChatClefCommandRegistry,
)

from .command_status_route_failure_record import CommandStatusRouteFailureRecord


_STAGES = frozenset(
    {
        "proof_validation",
        "input_validation",
        "addressee_parsing",
        "generic_matching",
        "family_matching",
        "target_matching",
        "state_inspection",
        "claim_evaluation",
        "publication_handoff_diagnostic_custody",
        "publication_handoff_acknowledgement",
        "publication_handoff_snapshot",
        "status_rendering",
        "presentation_detail_projection",
        "primary_decision_assembly",
        "fallback_decision_assembly",
        "optional_route_result_validation",
        "optional_route_result_acknowledgement_identity",
        "trusted_feedback_rendering",
        "response_capability_authorization",
        "feature_admission_finalization",
        "proof_lifecycle_close",
        "publication_acknowledgement_identity",
        "publication_route_identity",
        "dispatcher_initial_decision_resolution",
        "dispatcher_ready_decision_resolution",
        "dispatcher_external_response_publication",
        "dispatcher_publication_commit_inspection",
    }
)
_QUERY_KINDS = frozenset({"generic", "family", "target", "none"})
_REQUESTED_FAMILIES = frozenset(
    {
        "any",
        "item_get",
        "item_deposit",
        "item_equip",
        "item_give",
        "movement_follow",
        "movement_goto",
        "store_home",
        "none",
    }
)
_LIFECYCLE_STATES = frozenset(
    {"running", "pending", "idle", "unavailable", "none"}
)
_TERMINAL_STATES = frozenset({"claimed", "unclaimed", "none"})
_AVAILABILITY_REASONS = frozenset(
    {
        "no_tracked_owner",
        "inspection_failed",
        "query_mismatch",
        "disconnected",
        "quarantined",
        "stale_owner",
        "evidence_unavailable",
        "none",
    }
)
_EXCEPTION_CLASS = re.compile(r"[A-Za-z_][A-Za-z0-9_]{0,95}\Z", re.ASCII)


class CommandStatusRouteFailureProjector:
    def __init__(self, *, command_names: object = None) -> None:
        names = (
            KoreanChatClefCommandRegistry().command_names()
            if command_names is None
            else command_names
        )
        self._command_names = frozenset(
            name
            for name in names
            if type(name) is str and name
        )

    def project(
        self,
        *,
        stage: object,
        exception_class: object,
        query: object = None,
        snapshot: object = None,
    ) -> CommandStatusRouteFailureRecord:
        return CommandStatusRouteFailureRecord(
            stage=self._allowed(stage, _STAGES),
            query_kind=self._query_kind(query),
            addressed=self._addressed(query),
            requested_family=self._allowed(
                getattr(query, "requested_family", None),
                _REQUESTED_FAMILIES,
            ),
            active_command_name=self._allowed(
                getattr(snapshot, "command_name", None),
                self._command_names | {"none"},
            ),
            lifecycle_state=self._allowed(
                getattr(snapshot, "state", None),
                _LIFECYCLE_STATES,
            ),
            terminal_state=self._allowed(
                getattr(snapshot, "terminal_state", None),
                _TERMINAL_STATES,
            ),
            availability_reason=self._allowed(
                getattr(snapshot, "availability_reason", None),
                _AVAILABILITY_REASONS,
            ),
            exception_class=self._exception_class(exception_class),
        )

    @staticmethod
    def _allowed(value: object, allowed: frozenset[str] | set[str]) -> str:
        if value is None or value == "":
            return "none"
        return value if type(value) is str and value in allowed else "invalid"

    @staticmethod
    def _addressed(query: object) -> bool | str:
        if query is None:
            return "none"
        value = getattr(query, "addressed", None)
        return value if type(value) is bool else "invalid"

    @staticmethod
    def _query_kind(query: object) -> str:
        if query is None:
            return "none"
        target = getattr(query, "target_text", None)
        family = getattr(query, "requested_family", None)
        if type(target) is not str:
            return "invalid"
        if target:
            return "target"
        if family == "any":
            return "generic"
        if family in _REQUESTED_FAMILIES - {"any", "none"}:
            return "family"
        return "invalid"

    @staticmethod
    def _exception_class(value: object) -> str:
        if value == "none":
            return "none"
        if type(value) is not str or _EXCEPTION_CLASS.fullmatch(value) is None:
            return "invalid"
        return value


__all__ = ("CommandStatusRouteFailureProjector",)
