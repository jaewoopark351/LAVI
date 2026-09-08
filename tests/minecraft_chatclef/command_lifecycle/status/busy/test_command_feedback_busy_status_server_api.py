#20260909_kpopmodder: Verify busy STATUS selection is locked and handoff occurs after release.
from __future__ import annotations

import unittest
from types import SimpleNamespace

from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandBusyObservedIdentity,
    CommandFeedbackLifecycleSnapshot,
    CommandFeedbackPublicationAcknowledgement,
    CommandFeedbackPublicationPermit,
    CommandFeedbackServerApi,
    CommandStatusPublicationHandoffFailure,
)


class CommandFeedbackBusyStatusServerApiTests(unittest.TestCase):
    def test_lock_contains_selection_but_not_queryless_handoff(self):
        lock = _ObservedLock()
        ownership = _Ownership(lock)
        custody = object()
        custody_calls = []

        def custody_factory(query, snapshot):
            self.assertEqual(0, lock.depth)
            custody_calls.append((query, snapshot))
            return SimpleNamespace(record_once=lambda *_args, **_kwargs: True)

        api = _api(ownership, lock=lock, custody_factory=custody_factory)
        identity = _identity()

        snapshot = api.inspect_busy_status(identity)

        self.assertIs(identity, ownership.observed_identity)
        self.assertIs(type(snapshot), CommandFeedbackLifecycleSnapshot)
        self.assertIs(
            type(snapshot.publication_acknowledgement),
            CommandFeedbackPublicationAcknowledgement,
        )
        self.assertEqual([(None, ownership.snapshot)], custody_calls)
        self.assertEqual(0, lock.depth)
        self.assertIsNotNone(custody)

    def test_no_raw_permit_returns_the_original_prepermit_result(self):
        lock = _ObservedLock()
        ownership = _Ownership(lock, permit=None)
        api = _api(
            ownership,
            lock=lock,
            custody_factory=lambda *_args: self.fail("handoff must not run"),
        )

        result = api.inspect_busy_status(_identity())

        self.assertIs(ownership.snapshot, result)
        self.assertEqual(0, lock.depth)

    def test_queryless_handoff_failure_spends_the_raw_permit_false_once(self):
        lock = _ObservedLock()
        ownership = _Ownership(lock)
        queries = []

        def fail_factory(query, _snapshot):
            if lock.depth:
                raise AssertionError("handoff must run outside the command lock")
            queries.append(query)
            raise RuntimeError("SECRET")

        result = _api(
            ownership,
            lock=lock,
            custody_factory=fail_factory,
        ).inspect_busy_status(_identity())

        self.assertIs(type(result), CommandStatusPublicationHandoffFailure)
        self.assertEqual([None], queries)
        self.assertEqual([(ownership.permit, False)], ownership.acknowledgements)


class _ObservedLock:
    def __init__(self) -> None:
        self.depth = 0

    def __enter__(self):
        self.depth += 1
        return self

    def __exit__(self, _type, _value, _traceback):
        self.depth -= 1


class _Ownership:
    _DEFAULT = object()

    def __init__(self, lock: _ObservedLock, *, permit=_DEFAULT) -> None:
        self._lock = lock
        self.snapshot = CommandFeedbackLifecycleSnapshot(
            state=CommandFeedbackLifecycleSnapshot.RUNNING,
            owner_present=True,
        )
        self.permit = _permit() if permit is self._DEFAULT else permit
        self.observed_identity = None
        self.acknowledgements = []

    def inspect_command_feedback_busy_status_for_publication(self, identity):
        if self._lock.depth != 1:
            raise AssertionError("busy inspection must hold the command lock")
        self.observed_identity = identity
        return self.snapshot, self.permit

    def acknowledge_command_feedback_publication(self, permit, published):
        self.acknowledgements.append((permit, published))
        return SimpleNamespace(accepted=True, terminal_response=None)

    def select_command_feedback_coalesced_terminal(self, _permit):
        return None


def _api(ownership, *, lock, custody_factory):
    return CommandFeedbackServerApi(
        connection_ownership=ownership,
        command_lock=lock,
        terminal_listener=SimpleNamespace(set_callback=lambda _callback: None),
        terminal_delivery=SimpleNamespace(publish=lambda _response: None),
        status_publication_failure_diagnostic_custody_factory=custody_factory,
    )


def _identity():
    return CommandBusyObservedIdentity(
        active_session_id="session-a",
        active_generation=1,
        active_request_id="request-a",
        active_command_message_id="message-a",
    )


def _permit():
    return CommandFeedbackPublicationPermit(
        lifecycle_token=object(),
        sequence=1,
        kind=CommandFeedbackPublicationPermit.STATUS,
    )


if __name__ == "__main__":
    unittest.main()
