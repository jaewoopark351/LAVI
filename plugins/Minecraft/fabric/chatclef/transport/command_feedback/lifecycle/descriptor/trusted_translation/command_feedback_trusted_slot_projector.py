#20260915_kpopmodder: Project validated canonical arguments using existing raw grammar ownership.
from __future__ import annotations

from ..raw_form import CommandFeedbackRawFormDecoder, CommandFeedbackRawSlotProjector
from ..raw_form.lexing import CommandFeedbackRawCommandLexer
from ..command_feedback_target import CommandFeedbackTarget
from ..command_feedback_quantity_semantics import command_feedback_quantity_semantics


class CommandFeedbackTrustedSlotProjector:
    def __init__(self, display_names):
        self._forms = CommandFeedbackRawFormDecoder(lexer=CommandFeedbackRawCommandLexer(max_command_length=32768))
        self._slots = CommandFeedbackRawSlotProjector()
        self._display = display_names

    def project(self, command, intent, label_resolver):
        form = self._forms.decode(command)
        if form is None:
            return None
        slots = self._slots.project(form)
        targets = ()
        items = intent.get("slots", {}).get("items", ())
        if items:
            projected = []
            for target, count in slots.target_entries:
                projected.append(CommandFeedbackTarget(target, count,
                    command_feedback_quantity_semantics(form.command_name), label_resolver(target, "")))
            targets = tuple(projected)
        return form, slots, targets
