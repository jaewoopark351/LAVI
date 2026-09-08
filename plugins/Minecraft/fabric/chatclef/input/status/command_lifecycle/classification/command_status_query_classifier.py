#20260907_kpopmodder: Classify closed Korean status-question shapes without resolving targets.
#20260908_kpopmodder: Delegate STATUS validation, addressing, and grammar matching to focused collaborators.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.intent.korean_text_normalizer import (
    KoreanTextNormalizer,
)

from .addressing import CommandStatusAddresseeParser
from .command_status_classification_failure import (
    CommandStatusClassificationFailure,
)
from .command_status_query import CommandStatusQuery
from .family import FamilyCommandStatusQuestionMatcher
from .generic import GenericCommandStatusQuestionMatcher
from .target import TargetCommandStatusQuestionMatcher
from .validation import CommandStatusQuestionInputValidator


class CommandStatusQueryClassifier:
    _EXPANDED_ADDRESSEE = "마크 ai "
    _ADDRESSEES = frozenset({"", "마크 ai ", "마인크래프트 ", "마크 "})
    _FAMILY_VALUES = frozenset({"item_get", "movement_goto", "store_home"})
    _TARGET_FAMILY_VALUES = frozenset(
        {
            "item_get",
            "item_deposit",
            "item_equip",
            "item_give",
            "movement_follow",
        }
    )

    def __init__(
        self,
        normalizer=None,
        *,
        input_validator=None,
        addressee_parser=None,
        generic_matcher=None,
        family_matcher=None,
        target_matcher=None,
    ) -> None:
        self._normalizer = normalizer or KoreanTextNormalizer()
        self._input_validator = (
            input_validator or CommandStatusQuestionInputValidator()
        )
        self._addressee_parser = addressee_parser or CommandStatusAddresseeParser()
        self._generic_matcher = generic_matcher or GenericCommandStatusQuestionMatcher()
        self._family_matcher = family_matcher or FamilyCommandStatusQuestionMatcher()
        self._target_matcher = target_matcher or TargetCommandStatusQuestionMatcher()

    def classify(self, text: object) -> CommandStatusQuery | None:
        normalized = self._call_stage(
            "input_validation",
            self._normalizer.normalize,
            text,
            result_validator=lambda result: type(result) is str,
        )
        addressing = self._call_stage(
            "addressee_parsing",
            self._addressee_parser.parse,
            normalized,
            result_validator=self._addressing_is_valid,
        )
        addressee, body = addressing
        addressed = bool(addressee)
        expanded_addressee = addressee == self._EXPANDED_ADDRESSEE
        if expanded_addressee and not self._raw_input_is_valid(text):
            return None
        generic_matched = self._call_stage(
            "generic_matching",
            self._generic_matcher.matches,
            body,
            result_validator=lambda result: type(result) is bool,
        )
        if generic_matched:
            if not expanded_addressee and not self._raw_input_is_valid(text):
                return None
            return CommandStatusQuery(requested_family="any", addressed=addressed)
        family = self._call_stage(
            "family_matching",
            self._family_matcher.match,
            body,
            result_validator=lambda result: result is None
            or (type(result) is str and result in self._FAMILY_VALUES),
        )
        if family is not None:
            return CommandStatusQuery(requested_family=family, addressed=addressed)
        target_match = self._call_stage(
            "target_matching",
            self._target_matcher.match,
            body,
            result_validator=self._target_match_is_valid,
        )
        if target_match is not None:
            family, target = target_match
            return CommandStatusQuery(
                requested_family=family,
                addressed=addressed,
                target_text=target,
            )
        return None

    def _raw_input_is_valid(self, text: object) -> bool:
        return self._call_stage(
            "input_validation",
            self._input_validator.accepts,
            text,
            result_validator=lambda result: type(result) is bool,
        ) is True

    @staticmethod
    def _call_stage(stage: str, callback, *values, result_validator=None):
        try:
            result = callback(*values)
            if result_validator is not None and not result_validator(result):
                raise TypeError("command status stage result is invalid")
            return result
        except CommandStatusClassificationFailure:
            raise
        except Exception as error:
            raise CommandStatusClassificationFailure(
                stage=stage,
                exception_class=type(error).__name__,
            ) from None

    def _addressing_is_valid(self, result: object) -> bool:
        return (
            type(result) is tuple
            and len(result) == 2
            and type(result[0]) is str
            and result[0] in self._ADDRESSEES
            and type(result[1]) is str
        )

    def _target_match_is_valid(self, result: object) -> bool:
        return result is None or (
            type(result) is tuple
            and len(result) == 2
            and type(result[0]) is str
            and result[0] in self._TARGET_FAMILY_VALUES
            and type(result[1]) is str
            and bool(result[1])
        )


__all__ = ("CommandStatusQueryClassifier",)
