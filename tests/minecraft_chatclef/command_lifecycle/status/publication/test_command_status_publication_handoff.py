#20260908_kpopmodder: Verify post-lock STATUS handoff cleanup and opaque custody.
from __future__ import annotations

import threading
import unittest
from types import SimpleNamespace

from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.command_feedback_server_api import (
    CommandFeedbackServerApi,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.publication import (
    CommandFeedbackPublicationAcknowledgement,
    CommandFeedbackPublicationPermit,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.state import (
    CommandFeedbackLifecycleSnapshot,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.status.publication import (
    CommandStatusPublicationHandoff,
    CommandStatusPublicationHandoffFailure,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.status.diagnostics import (
    CommandStatusFailureDiagnosticCustodyFactory,
)
from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle.diagnostics import (
    CommandStatusRouteFailureProjector,
)


class CommandStatusPublicationHandoffTests(unittest.TestCase):
    def test_server_api_attaches_same_opaque_custody_to_exact_status_ack(self):
        custody = SimpleNamespace(record_once=lambda *_args, **_kwargs: True)
        ownership = _Ownership()
        api = _api(ownership, custody_factory=lambda _query, _snapshot: custody)

        snapshot = api.inspect_status(SimpleNamespace(requested_family="any"))

        acknowledgement = snapshot.publication_acknowledgement
        self.assertIs(type(acknowledgement), CommandFeedbackPublicationAcknowledgement)
        self.assertIs(
            custody,
            acknowledgement.publication_failure_diagnostic_custody,
        )
        self.assertEqual([], ownership.acknowledgements)

    def test_default_factory_preserves_status_acknowledgement_behavior(self):
        ownership = _Ownership()
        snapshot = _api(ownership).inspect_status()

        self.assertIs(
            type(snapshot.publication_acknowledgement),
            CommandFeedbackPublicationAcknowledgement,
        )
        self.assertIsNone(
            snapshot.publication_acknowledgement
            .publication_failure_diagnostic_custody
        )

    def test_factory_failure_observes_once_spends_false_and_releases_terminal(self):
        ownership = _Ownership(terminal_response="terminal")
        observations = []
        delivery = _Delivery()

        def fail_factory(_query, _snapshot):
            raise RuntimeError("SECRET")

        api = _api(
            ownership,
            custody_factory=fail_factory,
            fallback_observer=lambda query, snapshot, **values: observations.append(
                (query, snapshot, values)
            ),
            delivery=delivery,
        )
        query = SimpleNamespace(requested_family="any")

        result = api.inspect_status(query)

        self.assertIs(type(result), CommandStatusPublicationHandoffFailure)
        self.assertEqual("publication_handoff_diagnostic_custody", result.stage)
        self.assertEqual("RuntimeError", result.exception_class)
        self.assertEqual([(ownership.permit, False)], ownership.acknowledgements)
        self.assertEqual(["terminal"], delivery.published)
        self.assertEqual(1, len(observations))
        self.assertIs(query, observations[0][0])
        self.assertIs(ownership.snapshot, observations[0][1])

    def test_snapshot_failure_records_first_failure_and_never_retries_false(self):
        calls = []
        records = []
        acknowledgements = []
        permit = _permit()
        custody = SimpleNamespace(
            record_once=lambda stage, exception_class: records.append(
                (stage, exception_class)
            )
        )
        handoff = CommandStatusPublicationHandoff(
            acknowledgement_factory=lambda raw, attached: (
                acknowledgements.append(
                    CommandFeedbackPublicationAcknowledgement(
                        permit=raw,
                        callback=lambda callback_permit, published: (
                            calls.append((callback_permit, published)) or True
                        ),
                        publication_failure_diagnostic_custody=attached,
                    )
                )
                or acknowledgements[-1]
            ),
            publication_failure_callback=lambda raw, published: calls.append(
                (raw, published)
            ),
            diagnostic_custody_factory=lambda _query, _snapshot: custody,
        )

        result = handoff.attach(query=object(), snapshot=object(), permit=permit)

        self.assertIs(type(result), CommandStatusPublicationHandoffFailure)
        self.assertEqual([("publication_handoff_snapshot", "TypeError")], records)
        self.assertEqual([(permit, False)], calls)
        self.assertFalse(acknowledgements[0].acknowledge(published=False))
        self.assertEqual([(permit, False)], calls)

    def test_acknowledgement_factory_failure_records_and_spends_once(self):
        calls = []
        records = []
        permit = _permit()
        handoff = CommandStatusPublicationHandoff(
            acknowledgement_factory=lambda *_args: _raise(
                RuntimeError("SECRET")
            ),
            publication_failure_callback=lambda raw, published: calls.append(
                (raw, published)
            ),
            diagnostic_custody_factory=lambda _query, _snapshot: (
                SimpleNamespace(
                    record_once=lambda stage, exception_class: records.append(
                        (stage, exception_class)
                    )
                )
            ),
        )

        result = handoff.attach(
            query=object(),
            snapshot=CommandFeedbackLifecycleSnapshot(state="running"),
            permit=permit,
        )

        self.assertIs(type(result), CommandStatusPublicationHandoffFailure)
        self.assertEqual(
            [("publication_handoff_acknowledgement", "RuntimeError")],
            records,
        )
        self.assertEqual([(permit, False)], calls)

    def test_snapshot_failure_consumes_created_ack_and_releases_terminal_once(self):
        ownership = _Ownership(terminal_response="terminal")
        ownership.snapshot = object()
        delivery = _Delivery()
        api = _api(
            ownership,
            custody_factory=lambda _query, _snapshot: SimpleNamespace(
                record_once=lambda *_args, **_kwargs: True
            ),
            delivery=delivery,
        )
        created = []
        original_factory = (
            api._status_publication_handoff._acknowledgement_factory
        )

        def capture_acknowledgement(permit, custody):
            acknowledgement = original_factory(permit, custody)
            created.append(acknowledgement)
            return acknowledgement

        api._status_publication_handoff._acknowledgement_factory = (
            capture_acknowledgement
        )

        result = api.inspect_status(
            SimpleNamespace(requested_family="any")
        )

        self.assertIs(type(result), CommandStatusPublicationHandoffFailure)
        self.assertEqual([(ownership.permit, False)], ownership.acknowledgements)
        self.assertEqual(["terminal"], delivery.published)
        self.assertFalse(created[0].acknowledge(published=False))
        self.assertEqual([(ownership.permit, False)], ownership.acknowledgements)
        self.assertEqual(["terminal"], delivery.published)

    def test_factory_and_fallback_sink_failure_still_attempt_false_once(self):
        calls = []
        permit = _permit()

        def fail_publication(raw, published):
            calls.append((raw, published))
            raise RuntimeError("callback")

        handoff = CommandStatusPublicationHandoff(
            acknowledgement_factory=lambda *_args: None,
            publication_failure_callback=fail_publication,
            diagnostic_custody_factory=lambda *_args: _raise(
                RuntimeError("factory")
            ),
            fallback_failure_observer=lambda *_args, **_kwargs: _raise(
                LookupError("sink")
            ),
        )

        result = handoff.attach(
            query=object(),
            snapshot=CommandFeedbackLifecycleSnapshot(state="running"),
            permit=permit,
        )

        self.assertIs(type(result), CommandStatusPublicationHandoffFailure)
        self.assertEqual([(permit, False)], calls)

    def test_diagnostic_custody_claims_once_before_a_throwing_sink(self):
        attempts = []
        factory = CommandStatusFailureDiagnosticCustodyFactory(
            projection_callback=CommandStatusRouteFailureProjector(
                command_names=("get",)
            ).project,
            record_callback=lambda record: (
                attempts.append(record)
                or _raise(LookupError("sink"))
            ),
        )
        custody = factory.create(
            SimpleNamespace(requested_family="item_get", addressed=True),
            CommandFeedbackLifecycleSnapshot(
                state="running",
                command_name="get",
                requested_family="item_get",
                owner_present=True,
            ),
        )

        first = custody.record_once("status_rendering", "RuntimeError")
        second = custody.record_once(
            "response_capability_authorization",
            "ValueError",
        )

        self.assertFalse(first)
        self.assertFalse(second)
        self.assertEqual(1, len(attempts))
        self.assertEqual("status_rendering", attempts[0].stage)
        self.assertEqual("RuntimeError", attempts[0].exception_class)

    def test_diagnostic_custody_accepts_new_dispatch_publication_stages(self):
        for stage in (
            "dispatcher_external_response_publication",
            "dispatcher_publication_commit_inspection",
        ):
            with self.subTest(stage=stage):
                records = []
                factory = CommandStatusFailureDiagnosticCustodyFactory(
                    projection_callback=CommandStatusRouteFailureProjector(
                        command_names=("get",)
                    ).project,
                    record_callback=records.append,
                )
                custody = factory.create(
                    SimpleNamespace(
                        requested_family="item_get",
                        addressed=True,
                    ),
                    CommandFeedbackLifecycleSnapshot(
                        state="running",
                        command_name="get",
                        requested_family="item_get",
                        owner_present=True,
                    ),
                )

                self.assertTrue(custody.record_once(stage, "RuntimeError"))
                self.assertEqual(stage, records[0].stage)


class _Ownership:
    def __init__(self, *, terminal_response=None):
        self.permit = _permit()
        self.snapshot = CommandFeedbackLifecycleSnapshot(state="running")
        self.acknowledgements = []
        self._terminal_response = terminal_response

    def inspect_command_feedback_status_for_publication(self, **_kwargs):
        return self.snapshot, self.permit

    def acknowledge_command_feedback_publication(self, permit, published):
        self.acknowledgements.append((permit, published))
        return SimpleNamespace(
            accepted=True,
            terminal_response=self._terminal_response,
        )

    def select_command_feedback_coalesced_terminal(self, _permit):
        return None


class _Delivery:
    def __init__(self):
        self.published = []

    def publish(self, value):
        self.published.append(value)


def _permit():
    return CommandFeedbackPublicationPermit(
        lifecycle_token=object(),
        sequence=1,
        kind=CommandFeedbackPublicationPermit.STATUS,
    )


def _api(
    ownership,
    *,
    custody_factory=None,
    fallback_observer=None,
    delivery=None,
):
    return CommandFeedbackServerApi(
        connection_ownership=ownership,
        command_lock=threading.RLock(),
        terminal_listener=SimpleNamespace(set_callback=lambda _callback: None),
        terminal_delivery=delivery or _Delivery(),
        status_publication_failure_diagnostic_custody_factory=custody_factory,
        status_publication_handoff_failure_observer=fallback_observer,
    )


def _raise(error):
    raise error


if __name__ == "__main__":
    unittest.main()
