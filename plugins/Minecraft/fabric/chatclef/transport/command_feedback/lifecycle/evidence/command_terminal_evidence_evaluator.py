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
from .store_home import StoreHomeTerminalEvidenceEvaluator


class CommandTerminalEvidenceEvaluator:
    def __init__(
        self,
        profile_registry=None,
        *,
        get_evaluator=None,
        store_home_evaluator=None,
    ) -> None:
        self._profiles = profile_registry or CommandTerminalEvidenceProfileRegistry()
        self._evaluators = {
            "get_acquisition": (
                get_evaluator or GetAcquisitionTerminalEvidenceEvaluator()
            ),
            "store_home_completion": (
                store_home_evaluator or StoreHomeTerminalEvidenceEvaluator()
            ),
        }

    def evaluate(
        self,
        result: object,
        *,
        context: object = None,
    ) -> CommandTerminalEvidenceEvaluation:
        if type(result) is not CommandResultDTO:
            return CommandTerminalEvidenceEvaluation(False)
        data = result.data
        if not isinstance(data, Mapping):
            return CommandTerminalEvidenceEvaluation(False)
        descriptor = getattr(context, "descriptor", None)
        command_name = getattr(descriptor, "command_name", None) or getattr(
            context,
            "command_name",
            "get" if context is None else "",
        )
        try:
            profile = self._profiles.profile(command_name)
        except (KeyError, TypeError, ValueError):
            return CommandTerminalEvidenceEvaluation(False)
        if profile.rollout_state != CommandTerminalEvidenceProfile.VERIFIED:
            return CommandTerminalEvidenceEvaluation(False)
        if (
            descriptor is not None
            and getattr(descriptor, "rollout_state", None)
            != CommandTerminalEvidenceProfile.VERIFIED
        ):
            return CommandTerminalEvidenceEvaluation(False)
        if getattr(descriptor, "detail_level", "typed") not in {
            "typed",
            "raw_typed",
        }:
            return CommandTerminalEvidenceEvaluation(False)
        evaluator = self._evaluators.get(profile.success_evaluator_id)
        if evaluator is None:
            return CommandTerminalEvidenceEvaluation(False)
        evaluation = evaluator.evaluate(
            result,
            data=data,
            context=context,
            profile=profile,
        )
        if type(evaluation) is not CommandTerminalEvidenceEvaluation:
            return CommandTerminalEvidenceEvaluation(False)
        return evaluation

    def verified(self, result: object, *, context: object = None) -> bool:
        return self.evaluate(result, context=context).verified


__all__ = ("CommandTerminalEvidenceEvaluator",)
