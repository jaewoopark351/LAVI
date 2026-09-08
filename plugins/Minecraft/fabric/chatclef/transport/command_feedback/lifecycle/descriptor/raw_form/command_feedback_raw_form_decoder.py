#20260907_kpopmodder: Preserve the raw-form decoder API as a focused composition facade.
from __future__ import annotations

from .command_feedback_raw_form import CommandFeedbackRawForm
from .command_feedback_raw_form_profile_registry import (
    CommandFeedbackRawFormProfileRegistry,
)
from .grammar import CommandFeedbackRawGrammarRouter
from .lexing import CommandFeedbackRawCommandLexer


class CommandFeedbackRawFormDecoder:
    def __init__(self, *, profile_registry=None, lexer=None, grammar_router=None) -> None:
        self._profiles = profile_registry or CommandFeedbackRawFormProfileRegistry()
        self._lexer = lexer or CommandFeedbackRawCommandLexer()
        self._grammars = grammar_router or CommandFeedbackRawGrammarRouter()

    def decode(self, command: object) -> CommandFeedbackRawForm | None:
        parsed = self._lexer.split(command)
        if parsed is None:
            return None
        command_name, arguments = parsed
        try:
            profile = self._profiles.profile(command_name)
        except KeyError:
            return None
        form_kind = self._grammars.decode(
            profile.grammar_id,
            command_name,
            arguments,
        )
        if form_kind is None:
            return None
        return CommandFeedbackRawForm(
            command_name=command_name,
            form_kind=form_kind,
            argument_units=arguments,
        )

__all__ = ("CommandFeedbackRawFormDecoder",)
