#20260907_kpopmodder: Own command-local START/STATUS FIFO permits and deferred terminal release.
from __future__ import annotations

from ..kind import CommandFeedbackLifecycleKindProfile
from .command_feedback_publication_permit import CommandFeedbackPublicationPermit
from .command_feedback_publication_resolution import (
    CommandFeedbackPublicationResolution,
)


class CommandFeedbackPublicationCoordinator:
    def __init__(self) -> None:
        self._lifecycle_token = None
        self._next_sequence = 0
        self._pending = {}
        self._deferred_terminal = None
        self._response_lifecycle_kind = None
        self._initial_response_selection_sealed = False
        self._coalesced_terminal_selected = False

    def begin(
        self,
        lifecycle_token: object,
        response_lifecycle_kind: str = CommandFeedbackLifecycleKindProfile.FINITE_TASK,
    ) -> CommandFeedbackPublicationPermit:
        if response_lifecycle_kind not in {
            CommandFeedbackLifecycleKindProfile.FINITE_TASK,
            CommandFeedbackLifecycleKindProfile.PERSISTENT_TASK,
            CommandFeedbackLifecycleKindProfile.ASYNCHRONOUS_IMMEDIATE,
            CommandFeedbackLifecycleKindProfile.SPECIALIZED_CONTROL,
        }:
            raise ValueError("command response lifecycle kind is invalid")
        self.reset()
        self._lifecycle_token = lifecycle_token
        self._response_lifecycle_kind = response_lifecycle_kind
        return self.issue(CommandFeedbackPublicationPermit.START)

    def issue(self, kind: str) -> CommandFeedbackPublicationPermit:
        if self._lifecycle_token is None:
            raise RuntimeError("command lifecycle publication is unavailable")
        self._next_sequence += 1
        permit = CommandFeedbackPublicationPermit(
            lifecycle_token=self._lifecycle_token,
            sequence=self._next_sequence,
            kind=kind,
        )
        self._pending[permit.sequence] = permit
        if len(self._pending) == 1:
            permit._activate_turn()
        return permit

    def stage_terminal(self, lifecycle_token: object, terminal: object):
        if lifecycle_token is not self._lifecycle_token or terminal is None:
            return None, False
        if (
            self._deferred_terminal is not None
            or self._coalesced_terminal_selected
        ):
            return None, False
        if self._pending:
            self._deferred_terminal = terminal
            return None, False
        self.reset()
        return terminal, True

    def select_coalesced_terminal(
        self,
        lifecycle_token: object,
        permit: object,
    ):
        if (
            type(permit) is not CommandFeedbackPublicationPermit
            or lifecycle_token is not self._lifecycle_token
            or permit.lifecycle_token is not self._lifecycle_token
            or permit.kind != CommandFeedbackPublicationPermit.START
            or self._pending.get(permit.sequence) is not permit
            or next(iter(self._pending.values()), None) is not permit
            or not permit._is_ready()
            or self._initial_response_selection_sealed
        ):
            return None
        self._initial_response_selection_sealed = True
        if (
            self._response_lifecycle_kind
            != CommandFeedbackLifecycleKindProfile.ASYNCHRONOUS_IMMEDIATE
            or len(self._pending) != 1
            or self._deferred_terminal is None
        ):
            return None
        terminal = self._deferred_terminal
        self._deferred_terminal = None
        self._coalesced_terminal_selected = True
        return terminal

    def acknowledge(
        self,
        permit: object,
        published: bool,
    ) -> CommandFeedbackPublicationResolution:
        if (
            type(permit) is not CommandFeedbackPublicationPermit
            or type(published) is not bool
            or permit.lifecycle_token is not self._lifecycle_token
            or self._pending.get(permit.sequence) is not permit
        ):
            return CommandFeedbackPublicationResolution.rejected()
        is_head = next(iter(self._pending.values())) is permit
        if published and (not is_head or not permit._is_ready()):
            failure = self._fail(permit, is_head=is_head)
            return CommandFeedbackPublicationResolution(
                accepted=False,
                terminal_response=failure.terminal_response,
                retire_lifecycle=failure.retire_lifecycle,
            )
        if not published:
            return self._fail(permit, is_head=is_head)
        self._pending.pop(permit.sequence, None)
        permit._cancel_turn()
        self._activate_next()
        return self._release_terminal_if_ready()

    def reset(self) -> None:
        for permit in self._pending.values():
            permit._cancel_turn()
        self._lifecycle_token = None
        self._next_sequence = 0
        self._pending = {}
        self._deferred_terminal = None
        self._response_lifecycle_kind = None
        self._initial_response_selection_sealed = False
        self._coalesced_terminal_selected = False

    def _fail(
        self,
        permit: CommandFeedbackPublicationPermit,
        *,
        is_head: bool,
    ) -> CommandFeedbackPublicationResolution:
        if permit.kind == CommandFeedbackPublicationPermit.START:
            self.reset()
            return CommandFeedbackPublicationResolution.resolved(
                retire_lifecycle=True
            )
        self._pending.pop(permit.sequence, None)
        permit._cancel_turn()
        if is_head:
            self._activate_next()
        return self._release_terminal_if_ready()

    def _activate_next(self) -> None:
        if self._pending:
            next(iter(self._pending.values()))._activate_turn()

    def _release_terminal_if_ready(self) -> CommandFeedbackPublicationResolution:
        if self._pending:
            return CommandFeedbackPublicationResolution.resolved()
        if self._coalesced_terminal_selected:
            self.reset()
            return CommandFeedbackPublicationResolution.resolved(
                retire_lifecycle=True
            )
        terminal = self._deferred_terminal
        if terminal is None:
            return CommandFeedbackPublicationResolution.resolved()
        self.reset()
        return CommandFeedbackPublicationResolution.resolved(
            terminal,
            retire_lifecycle=True,
        )


__all__ = ("CommandFeedbackPublicationCoordinator",)
