#20260907_kpopmodder: Route one registered grammar identity to its focused decoder.
from __future__ import annotations

from .command_feedback_raw_auto_deposit_grammar import (
    CommandFeedbackRawAutoDepositGrammar,
)
from .command_feedback_raw_item_grammar import CommandFeedbackRawItemGrammar
from .command_feedback_raw_location_grammar import CommandFeedbackRawLocationGrammar
from .command_feedback_raw_scalar_grammar import CommandFeedbackRawScalarGrammar


class CommandFeedbackRawGrammarRouter:
    def __init__(
        self,
        *,
        item_grammar=None,
        location_grammar=None,
        scalar_grammar=None,
        auto_deposit_grammar=None,
    ) -> None:
        grammars = (
            item_grammar or CommandFeedbackRawItemGrammar(),
            location_grammar or CommandFeedbackRawLocationGrammar(),
            scalar_grammar or CommandFeedbackRawScalarGrammar(),
            auto_deposit_grammar or CommandFeedbackRawAutoDepositGrammar(),
        )
        self._by_id = {
            grammar_id: grammar
            for grammar in grammars
            for grammar_id in grammar.GRAMMAR_IDS
        }

    def decode(
        self,
        grammar_id: str,
        command_name: str,
        arguments: tuple[str, ...],
    ) -> str | None:
        grammar = self._by_id.get(grammar_id)
        if grammar is None:
            return None
        return grammar.decode(grammar_id, command_name, arguments)


__all__ = ("CommandFeedbackRawGrammarRouter",)
