#20260908_kpopmodder: Claim validated STATUS queries only for one exact nonterminal ordinary command owner.
from __future__ import annotations

from ..command_status_target_resolver import CommandStatusTargetResolver
from .contextual_command_status_claim_evaluation import (
    ContextualCommandStatusClaimEvaluation,
)
from .contextual_command_status_claim_failure import (
    ContextualCommandStatusClaimFailure,
)
from .descriptor_validation import CommandStatusDescriptorEligibilityValidator


class ContextualCommandStatusClaimEvaluator:
    _QUERY_FAMILIES = frozenset(
        {
            "any",
            "item_get",
            "item_deposit",
            "item_equip",
            "item_give",
            "movement_goto",
            "movement_follow",
            "store_home",
        }
    )
    _FAMILY_ONLY_QUERY_FAMILIES = frozenset(
        {"movement_goto", "store_home"}
    )
    _TARGET_REQUIRED_QUERY_FAMILIES = frozenset(
        {"item_deposit", "item_equip", "item_give", "movement_follow"}
    )
    _ORDINARY_LIFECYCLE_KINDS = frozenset(
        {"finite_task", "persistent_task", "asynchronous_immediate"}
    )

    def __init__(
        self,
        *,
        target_resolver=None,
        descriptor_validator=None,
    ) -> None:
        self._target_resolver = target_resolver or CommandStatusTargetResolver()
        self._descriptor_validator = (
            descriptor_validator
            if descriptor_validator is not None
            else CommandStatusDescriptorEligibilityValidator()
        )

    def evaluate(
        self,
        *,
        query: object,
        descriptor: object,
        admission_grant: object = None,
        active_command: object,
        owner_token: object,
        terminal_claimed: bool,
    ) -> ContextualCommandStatusClaimEvaluation:
        try:
            return self._evaluate(
                query=query,
                descriptor=descriptor,
                admission_grant=admission_grant,
                active_command=active_command,
                owner_token=owner_token,
                terminal_claimed=terminal_claimed,
            )
        except ContextualCommandStatusClaimFailure:
            raise
        except Exception as error:
            raise ContextualCommandStatusClaimFailure(
                exception_class=type(error).__name__,
            ) from None

    def _evaluate(
        self,
        *,
        query: object,
        descriptor: object,
        admission_grant: object,
        active_command: object,
        owner_token: object,
        terminal_claimed: bool,
    ) -> ContextualCommandStatusClaimEvaluation:
        if (
            type(terminal_claimed) is not bool
            or terminal_claimed
            or not self._query_is_valid(query)
            or not self._descriptor_is_valid(descriptor, admission_grant)
        ):
            return self._rejected()
        ordinary_context = self._is_ordinary_context(descriptor)
        owner_matched = owner_token is not None and active_command is owner_token
        if not ordinary_context or not owner_matched:
            return self._rejected(
                ordinary_context=ordinary_context,
                owner_matched=owner_matched,
            )
        resolution = self._target_resolver.resolve(query, descriptor)
        if not self._resolution_is_valid(resolution):
            return self._rejected(
                ordinary_context=True,
                owner_matched=True,
            )
        family_matched = resolution.family_matched
        target_matched = resolution.target_matched
        if query.addressed:
            claimed = resolution.claimed
        else:
            claimed = family_matched and target_matched
        return ContextualCommandStatusClaimEvaluation(
            claimed=claimed,
            family_matched=family_matched,
            target_matched=target_matched,
            ordinary_context=True,
            owner_matched=True,
        )

    def _query_is_valid(self, query: object) -> bool:
        family = getattr(query, "requested_family", None)
        addressed = getattr(query, "addressed", None)
        target_text = getattr(query, "target_text", None)
        if not (
            type(family) is str
            and family in self._QUERY_FAMILIES
            and type(addressed) is bool
            and type(target_text) is str
            and target_text == target_text.strip()
        ):
            return False
        if family == "any" or family in self._FAMILY_ONLY_QUERY_FAMILIES:
            return not target_text
        if family in self._TARGET_REQUIRED_QUERY_FAMILIES:
            return bool(target_text)
        return family == "item_get"

    def _descriptor_is_valid(
        self,
        descriptor: object,
        admission_grant: object,
    ) -> bool:
        return self._descriptor_validator.accepts(
            descriptor,
            admission_grant=admission_grant,
        )

    def _is_ordinary_context(self, descriptor: object) -> bool:
        command_name = descriptor.command_name
        lifecycle_kind = getattr(
            descriptor,
            "response_lifecycle_kind",
            None,
        )
        return (
            command_name != "stop"
            and lifecycle_kind in self._ORDINARY_LIFECYCLE_KINDS
        )

    @staticmethod
    def _resolution_is_valid(resolution: object) -> bool:
        return all(
            type(getattr(resolution, name, None)) is bool
            for name in ("claimed", "family_matched", "target_matched")
        )

    @staticmethod
    def _rejected(
        *,
        ordinary_context: bool = False,
        owner_matched: bool = False,
    ) -> ContextualCommandStatusClaimEvaluation:
        return ContextualCommandStatusClaimEvaluation(
            claimed=False,
            family_matched=False,
            target_matched=False,
            ordinary_context=ordinary_context,
            owner_matched=owner_matched,
        )


__all__ = ("ContextualCommandStatusClaimEvaluator",)
