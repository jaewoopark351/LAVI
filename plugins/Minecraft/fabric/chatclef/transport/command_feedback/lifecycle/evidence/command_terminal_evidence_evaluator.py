#20260908_kpopmodder: Dispatch closed terminal-evidence profiles without owning command-specific validation.
from __future__ import annotations

from typing import Mapping

from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO

from .command_terminal_evidence_evaluation import (
    CommandTerminalEvidenceEvaluation,
)
from .command_terminal_evidence_profile import CommandTerminalEvidenceProfile
from .command_terminal_evidence_profile_registry import (
    CommandTerminalEvidenceProfileRegistry,
)
from .get import GetAcquisitionTerminalEvidenceEvaluator
from .goto import GotoTerminalEvidenceEvaluator
#20260914_kpopmodder: FIND projects observations, not acquisition or combat effects.
from .find import FindTerminalEvidenceEvaluator
from .store_home import StoreHomeTerminalEvidenceEvaluator
from .instant import InstantCommandEvidenceEvaluator
from .equip import EquipTerminalEvidenceEvaluator


class CommandTerminalEvidenceEvaluator:
    def __init__(
        self,
        profile_registry=None,
        *,
        get_evaluator=None,
        store_home_evaluator=None,
        goto_evaluator=None,
        diagnostic_observer=None,
        instant_diagnostic_observer=None,
        equip_diagnostic_observer=None,
    ) -> None:
        self._profiles = profile_registry or CommandTerminalEvidenceProfileRegistry()
        #20260913_kpopmodder: Observe selected verdicts without supplying evidence.
        self._diagnostic_observer = diagnostic_observer
        self._instant_diagnostic_observer = instant_diagnostic_observer
        self._equip_diagnostic_observer = equip_diagnostic_observer
        self._evaluators = {
            "get_acquisition": (
                get_evaluator or GetAcquisitionTerminalEvidenceEvaluator()
            ),
            "store_home_completion": (
                store_home_evaluator or StoreHomeTerminalEvidenceEvaluator()
            ),
            "goto_terminal": goto_evaluator or GotoTerminalEvidenceEvaluator(),
            "find_terminal": FindTerminalEvidenceEvaluator(),
            "instant_command": InstantCommandEvidenceEvaluator(),
            "equip_slots": EquipTerminalEvidenceEvaluator(),
        }

    def evaluate(
        self,
        result: object,
        *,
        context: object = None,
    ) -> CommandTerminalEvidenceEvaluation:
        if type(result) is not CommandResultDTO:
            return self._observed(CommandTerminalEvidenceEvaluation(False), result, context, None, "invalid_result_type")
        data = result.data
        if not isinstance(data, Mapping):
            return self._observed(CommandTerminalEvidenceEvaluation(False), result, context, None, "invalid_result_data")
        descriptor = getattr(context, "descriptor", None)
        command_name = getattr(descriptor, "command_name", None) or getattr(
            context,
            "command_name",
            "get" if context is None else "",
        )
        try:
            profile = self._profiles.profile(command_name)
        except (KeyError, TypeError, ValueError):
            return self._observed(CommandTerminalEvidenceEvaluation(False), result, context, None, "profile_unavailable")
        if profile.rollout_state != CommandTerminalEvidenceProfile.VERIFIED:
            return self._observed(CommandTerminalEvidenceEvaluation(False), result, context, profile, "profile_not_verified")
        if (
            descriptor is not None
            and getattr(descriptor, "rollout_state", None)
            != CommandTerminalEvidenceProfile.VERIFIED
        ):
            return self._observed(CommandTerminalEvidenceEvaluation(False), result, context, profile, "descriptor_not_verified")
        if getattr(descriptor, "detail_level", "typed") not in {
            "typed",
            "raw_typed",
        } and profile.success_evaluator_id != "instant_command":
            return self._observed(CommandTerminalEvidenceEvaluation(False), result, context, profile, "unsupported_detail")
        evaluator = self._evaluators.get(profile.success_evaluator_id)
        if evaluator is None:
            return self._observed(CommandTerminalEvidenceEvaluation(False), result, context, profile, "evaluator_unavailable")
        evaluation = evaluator.evaluate(
            result,
            data=data,
            context=context,
            profile=profile,
        )
        if type(evaluation) is not CommandTerminalEvidenceEvaluation:
            return self._observed(CommandTerminalEvidenceEvaluation(False), result, context, profile, "invalid_evaluation_type")
        return self._observed(
            evaluation, result, context, profile,
            evaluation.decision_reason or "evaluated",
        )

    #20260913_kpopmodder: A throwing observer cannot replace the owner's return value.
    def _observed(self, evaluation, result, context, profile, reason):
        for observer in (self._diagnostic_observer, self._instant_diagnostic_observer, self._equip_diagnostic_observer):
            if observer is None:
                continue
            try:
                observer.evidence_decided(
                    result=result, context=context, profile=profile,
                    evaluation=evaluation, reason=reason,
                )
            except Exception:
                pass
        return evaluation

    def verified(self, result: object, *, context: object = None) -> bool:
        return self.evaluate(result, context=context).verified


__all__ = ("CommandTerminalEvidenceEvaluator",)
