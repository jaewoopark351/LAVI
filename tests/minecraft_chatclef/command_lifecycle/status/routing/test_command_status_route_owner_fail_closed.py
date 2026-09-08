#20260908_kpopmodder: Verify contextual STATUS routing and bounded failure ownership.
from __future__ import annotations

import unittest
from dataclasses import replace
from types import SimpleNamespace

from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle.classification import (
    CommandStatusClassificationFailure,
    CommandStatusQuery,
)
from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle.routing import (
    CommandStatusRouteOwner,
)
from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle.routing.decision import (
    COMMAND_STATUS_EMERGENCY_DECISION,
    CommandStatusRouteDecisionFactory,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle import (
    CommandFeedbackLifecycleSnapshot,
    ContextualCommandStatusClaimFailure,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.status.publication import (
    CommandStatusPublicationHandoffFailure,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.publication import (
    CommandFeedbackPublicationAcknowledgement,
    CommandFeedbackPublicationPermit,
)


class CommandStatusRouteOwnerFailClosedTests(unittest.TestCase):
    def setUp(self) -> None:
        self.proof = object()
        self.query = CommandStatusQuery(
            requested_family="any",
            addressed=True,
        )
        self.event = SimpleNamespace(text="마크 지금 뭐 해?")

    def test_prefixless_nonclaim_returns_explicit_conversation_fallthrough(self):
        rendered = []
        query = CommandStatusQuery(requested_family="any", addressed=False)
        snapshot = CommandFeedbackLifecycleSnapshot(
            state=CommandFeedbackLifecycleSnapshot.UNAVAILABLE,
            query_matched=False,
            owner_present=False,
            availability_reason="no_tracked_owner",
        )
        owner = self._owner(
            query=query,
            snapshot=snapshot,
            renderer=lambda *_values: rendered.append(True),
        )

        decision = owner.try_route(self.event, self.proof)

        self.assertIsNotNone(decision)
        self.assertFalse(decision.handled)
        self.assertEqual(
            "command_status_conversational_fallthrough",
            decision.reason,
        )
        self.assertEqual([], rendered)

    def test_prefixless_idle_snapshot_cannot_claim_without_active_context(self):
        query = CommandStatusQuery(requested_family="any", addressed=False)
        snapshot = CommandFeedbackLifecycleSnapshot(
            state=CommandFeedbackLifecycleSnapshot.IDLE,
            query_matched=True,
            owner_present=False,
            availability_reason="no_tracked_owner",
        )
        owner = self._owner(query=query, snapshot=snapshot)

        decision = owner.try_route(self.event, self.proof)

        self.assertFalse(decision.handled)
        self.assertEqual(
            "command_status_conversational_fallthrough",
            decision.reason,
        )

    def test_classifier_failure_with_live_proof_is_one_cautious_decision(self):
        records = []
        owner = self._owner(
            classifier=SimpleNamespace(
                classify=lambda _text: _raise(
                    CommandStatusClassificationFailure(
                        stage="family_matching",
                        exception_class="LookupError",
                    )
                )
            ),
            diagnostics=SimpleNamespace(record=lambda **values: records.append(values)),
        )

        decision = owner.try_route(self.event, self.proof)

        self.assertTrue(decision.handled)
        self.assertEqual(
            "지금 마인크래프트 작업 상태를 확인하지 못했어",
            decision.response_text,
        )
        self.assertEqual(1, len(records))
        self.assertEqual("family_matching", records[0]["stage"])
        self.assertEqual("LookupError", records[0]["exception_class"])

    def test_classifier_failure_without_live_proof_does_not_claim(self):
        records = []
        owner = self._owner(
            classifier=SimpleNamespace(
                classify=lambda _text: _raise(
                    CommandStatusClassificationFailure(
                        stage="generic_matching",
                        exception_class="RuntimeError",
                    )
                )
            ),
            proof_validator=lambda _proof, _event: False,
            diagnostics=SimpleNamespace(record=lambda **values: records.append(values)),
        )

        self.assertIsNone(owner.try_route(self.event, self.proof))
        self.assertEqual([], records)

    def test_proof_exception_does_not_claim_and_records_only_bounded_stage(self):
        records = []
        owner = self._owner(
            proof_validator=lambda _proof, _event: _raise(
                RuntimeError("raw secret must not be retained")
            ),
            diagnostics=SimpleNamespace(record=lambda **values: records.append(values)),
        )

        self.assertIsNone(owner.try_route(self.event, self.proof))
        self.assertEqual("proof_validation", records[0]["stage"])
        self.assertEqual("RuntimeError", records[0]["exception_class"])
        self.assertNotIn("raw secret", repr(records))

    def test_preclaim_failures_split_addressed_cautious_from_prefixless_bypass(self):
        for addressed, expected_handled in ((True, True), (False, False)):
            with self.subTest(addressed=addressed):
                records = []
                query = CommandStatusQuery(
                    requested_family="any",
                    addressed=addressed,
                )
                owner = self._owner(
                    query=query,
                    extension=SimpleNamespace(
                        inspect_command_feedback_status=lambda _query: _raise(
                            ContextualCommandStatusClaimFailure(
                                exception_class="ValueError"
                            )
                        )
                    ),
                    diagnostics=SimpleNamespace(
                        record=lambda **values: records.append(values)
                    ),
                )

                decision = owner.try_route(self.event, self.proof)

                self.assertIs(expected_handled, decision.handled)
                self.assertEqual("claim_evaluation", records[0]["stage"])
                self.assertEqual("ValueError", records[0]["exception_class"])
                if not addressed:
                    self.assertEqual(
                        "command_status_conversational_fallthrough",
                        decision.reason,
                    )

    def test_post_permit_handoff_failure_returns_prebuilt_emergency(self):
        owner = self._owner(
            snapshot=CommandStatusPublicationHandoffFailure(
                stage="publication_handoff_snapshot",
                exception_class="TypeError",
            )
        )

        decision = owner.try_route(self.event, self.proof)

        self.assertIs(COMMAND_STATUS_EMERGENCY_DECISION, decision)

    def test_post_permit_render_projection_and_assembly_failures_use_custody(self):
        scenarios = (
            ("status_rendering", "renderer"),
            ("presentation_detail_projection", "projector"),
            ("primary_decision_assembly", "factory"),
        )
        for expected_stage, failing_component in scenarios:
            with self.subTest(stage=expected_stage):
                custody_records = []
                local_records = []
                acknowledgement = _acknowledgement(
                    custody_records=custody_records,
                )
                snapshot = self._running_snapshot(acknowledgement)
                renderer = SimpleNamespace(render_status=lambda *_values: "작업 중이야")
                projector = SimpleNamespace(project=lambda _descriptor: "")
                decisions = CommandStatusRouteDecisionFactory()
                if failing_component == "renderer":
                    renderer.render_status = lambda *_values: _raise(LookupError())
                elif failing_component == "projector":
                    projector.project = lambda _descriptor: _raise(LookupError())
                else:
                    healthy = CommandStatusRouteDecisionFactory()
                    decisions = SimpleNamespace(
                        status=lambda **_values: _raise(LookupError()),
                        cautious=healthy.cautious,
                        conversational_fallthrough=(
                            healthy.conversational_fallthrough
                        ),
                        emergency=healthy.emergency,
                    )
                owner = self._owner(
                    snapshot=snapshot,
                    renderer=renderer.render_status,
                    projector=projector,
                    decision_factory=decisions,
                    diagnostics=SimpleNamespace(
                        record=lambda **values: local_records.append(values)
                    ),
                )

                decision = owner.try_route(self.event, self.proof)

                self.assertTrue(decision.handled)
                self.assertEqual(
                    "지금 마인크래프트 작업 상태를 확인하지 못했어",
                    decision.response_text,
                )
                self.assertIs(
                    acknowledgement,
                    decision.response_publication_acknowledgement,
                )
                self.assertEqual(expected_stage, custody_records[0]["stage"])
                self.assertEqual("LookupError", custody_records[0]["exception_class"])
                self.assertEqual([], local_records)

    def test_failed_fixed_fallback_spends_ack_false_and_keeps_first_diagnostic(self):
        custody_records = []
        acknowledgements = []
        acknowledgement = _acknowledgement(
            custody_records=custody_records,
            acknowledgements=acknowledgements,
        )
        healthy = CommandStatusRouteDecisionFactory()
        decisions = SimpleNamespace(
            status=healthy.status,
            cautious=lambda **_values: _raise(ArithmeticError()),
            conversational_fallthrough=healthy.conversational_fallthrough,
            emergency=healthy.emergency,
        )
        owner = self._owner(
            snapshot=self._running_snapshot(acknowledgement),
            renderer=lambda *_values: _raise(LookupError()),
            decision_factory=decisions,
        )

        decision = owner.try_route(self.event, self.proof)

        self.assertIs(COMMAND_STATUS_EMERGENCY_DECISION, decision)
        self.assertEqual(
            [{"stage": "status_rendering", "exception_class": "LookupError"}],
            custody_records,
        )
        self.assertEqual([False], acknowledgements)

    def test_nonclaim_snapshot_with_acknowledgement_fails_closed_without_leak(self):
        custody_records = []
        acknowledgements = []
        acknowledgement = SimpleNamespace(
            publication_failure_diagnostic_custody=SimpleNamespace(
                record_once=lambda **values: custody_records.append(values)
            ),
            acknowledge=lambda **values: acknowledgements.append(values),
        )
        query = CommandStatusQuery(requested_family="any", addressed=False)
        snapshot = CommandFeedbackLifecycleSnapshot(
            state=CommandFeedbackLifecycleSnapshot.UNAVAILABLE,
            descriptor=object(),
            query_matched=False,
            owner_present=True,
            publication_acknowledgement=acknowledgement,
            terminal_state="unclaimed",
        )
        owner = self._owner(query=query, snapshot=snapshot)

        decision = owner.try_route(self.event, self.proof)

        self.assertIs(COMMAND_STATUS_EMERGENCY_DECISION, decision)
        self.assertEqual([{"published": False}], acknowledgements)
        self.assertEqual("state_inspection", custody_records[0]["stage"])

    def test_exact_snapshot_rejects_unknown_or_malformed_terminal_state(self):
        for terminal_state in ("", "pending", [], 1, None):
            with self.subTest(terminal_state=terminal_state):
                records = []
                snapshot = CommandFeedbackLifecycleSnapshot(
                    state=CommandFeedbackLifecycleSnapshot.RUNNING,
                    descriptor=object(),
                    query_matched=True,
                    owner_present=True,
                    terminal_state=terminal_state,
                )
                owner = self._owner(
                    snapshot=snapshot,
                    diagnostics=SimpleNamespace(
                        record=lambda **values: records.append(values)
                    ),
                )

                decision = owner.try_route(self.event, self.proof)

                self.assertTrue(decision.handled)
                self.assertEqual(
                    "지금 마인크래프트 작업 상태를 확인하지 못했어",
                    decision.response_text,
                )
                self.assertEqual("state_inspection", records[0]["stage"])
                self.assertEqual("TypeError", records[0]["exception_class"])

    def test_invalid_snapshot_with_embedded_ack_spends_original_and_fails_closed(self):
        for addressed in (True, False):
            for field_name, invalid_value in (
                ("state", []),
                ("state", "finished"),
                ("terminal_state", []),
                ("terminal_state", "pending"),
            ):
                with self.subTest(
                    addressed=addressed,
                    field_name=field_name,
                    invalid_value=invalid_value,
                ):
                    records = []
                    acknowledgements = []
                    acknowledgement = _acknowledgement(
                        custody_records=records,
                        acknowledgements=acknowledgements,
                    )
                    values = {
                        "state": CommandFeedbackLifecycleSnapshot.RUNNING,
                        "descriptor": object(),
                        "query_matched": True,
                        "owner_present": True,
                        "publication_acknowledgement": acknowledgement,
                        "terminal_state": "unclaimed",
                    }
                    values[field_name] = invalid_value
                    query = CommandStatusQuery(
                        requested_family="any",
                        addressed=addressed,
                    )
                    owner = self._owner(
                        query=query,
                        snapshot=CommandFeedbackLifecycleSnapshot(**values),
                    )

                    decision = owner.try_route(self.event, self.proof)

                    self.assertIs(COMMAND_STATUS_EMERGENCY_DECISION, decision)
                    self.assertEqual([False], acknowledgements)
                    self.assertEqual("state_inspection", records[0]["stage"])
                    self.assertFalse(
                        acknowledgement.acknowledge(published=False)
                    )
                    self.assertEqual([False], acknowledgements)

    def test_invalid_snapshot_without_ack_preserves_addressed_split(self):
        for addressed, expected_handled in ((True, True), (False, False)):
            with self.subTest(addressed=addressed):
                query = CommandStatusQuery(
                    requested_family="any",
                    addressed=addressed,
                )
                owner = self._owner(
                    query=query,
                    snapshot=CommandFeedbackLifecycleSnapshot(
                        state=[],
                        query_matched=True,
                        owner_present=True,
                    ),
                )

                decision = owner.try_route(self.event, self.proof)

                self.assertIs(expected_handled, decision.handled)
                self.assertIsNot(COMMAND_STATUS_EMERGENCY_DECISION, decision)

    def test_wrong_snapshot_type_with_embedded_ack_cannot_fall_through(self):
        for addressed in (True, False):
            with self.subTest(addressed=addressed):
                records = []
                calls = []
                acknowledgement = _acknowledgement(
                    custody_records=records,
                    acknowledgements=calls,
                )
                query = CommandStatusQuery(
                    requested_family="any",
                    addressed=addressed,
                )
                owner = self._owner(
                    query=query,
                    snapshot=SimpleNamespace(
                        state="running",
                        publication_acknowledgement=acknowledgement,
                    ),
                )

                decision = owner.try_route(self.event, self.proof)

                self.assertIs(COMMAND_STATUS_EMERGENCY_DECISION, decision)
                self.assertEqual([False], calls)
                self.assertEqual("state_inspection", records[0]["stage"])

    def test_contradictory_snapshot_combinations_never_render_status(self):
        contradictions = (
            {
                "state": CommandFeedbackLifecycleSnapshot.RUNNING,
                "descriptor": None,
                "query_matched": True,
                "owner_present": True,
                "terminal_state": "unclaimed",
            },
            {
                "state": CommandFeedbackLifecycleSnapshot.RUNNING,
                "descriptor": object(),
                "query_matched": False,
                "owner_present": True,
                "terminal_state": "unclaimed",
            },
            {
                "state": CommandFeedbackLifecycleSnapshot.PENDING,
                "descriptor": object(),
                "query_matched": True,
                "owner_present": True,
                "terminal_state": "none",
            },
            {
                "state": CommandFeedbackLifecycleSnapshot.RUNNING,
                "descriptor": object(),
                "query_matched": True,
                "owner_present": False,
                "terminal_state": "none",
            },
            {
                "state": CommandFeedbackLifecycleSnapshot.IDLE,
                "descriptor": object(),
                "query_matched": True,
                "owner_present": True,
                "terminal_state": "unclaimed",
            },
        )
        for values in contradictions:
            for addressed in (True, False):
                for with_ack in (True, False):
                    with self.subTest(
                        values=values,
                        addressed=addressed,
                        with_ack=with_ack,
                    ):
                        calls = []
                        records = []
                        acknowledgement = (
                            _acknowledgement(
                                custody_records=records,
                                acknowledgements=calls,
                            )
                            if with_ack
                            else None
                        )
                        snapshot = CommandFeedbackLifecycleSnapshot(
                            **values,
                            publication_acknowledgement=acknowledgement,
                        )
                        query = CommandStatusQuery(
                            requested_family="any",
                            addressed=addressed,
                        )
                        rendered = []
                        owner = self._owner(
                            query=query,
                            snapshot=snapshot,
                            renderer=lambda *_args: rendered.append(True),
                        )

                        decision = owner.try_route(self.event, self.proof)

                        self.assertEqual([], rendered)
                        if with_ack:
                            self.assertIs(
                                COMMAND_STATUS_EMERGENCY_DECISION,
                                decision,
                            )
                            self.assertEqual([False], calls)
                            self.assertEqual(
                                "state_inspection",
                                records[0]["stage"],
                            )
                        else:
                            self.assertIs(addressed, decision.handled)
                            self.assertIsNot(
                                COMMAND_STATUS_EMERGENCY_DECISION,
                                decision,
                            )

    def test_terminal_claimed_snapshot_uses_no_status_response(self):
        for addressed in (True, False):
            for with_ack in (True, False):
                with self.subTest(addressed=addressed, with_ack=with_ack):
                    calls = []
                    records = []
                    acknowledgement = (
                        _acknowledgement(
                            custody_records=records,
                            acknowledgements=calls,
                        )
                        if with_ack
                        else None
                    )
                    query = CommandStatusQuery(
                        requested_family="any",
                        addressed=addressed,
                    )
                    snapshot = CommandFeedbackLifecycleSnapshot(
                        state=CommandFeedbackLifecycleSnapshot.IDLE,
                        query_matched=addressed,
                        owner_present=False,
                        terminal_state="claimed",
                        publication_acknowledgement=acknowledgement,
                    )
                    rendered = []
                    owner = self._owner(
                        query=query,
                        snapshot=snapshot,
                        renderer=lambda *_args: rendered.append(True),
                    )

                    decision = owner.try_route(self.event, self.proof)

                    self.assertEqual([], rendered)
                    if with_ack:
                        self.assertIs(
                            COMMAND_STATUS_EMERGENCY_DECISION,
                            decision,
                        )
                        self.assertEqual([False], calls)
                        self.assertEqual(
                            "state_inspection",
                            records[0]["stage"],
                        )
                    else:
                        self.assertIs(addressed, decision.handled)
                        self.assertIsNot(
                            COMMAND_STATUS_EMERGENCY_DECISION,
                            decision,
                        )

    def test_primary_factory_tamper_spends_original_ack_and_returns_sentinel(self):
        variants = (
            "ack_drop",
            "ack_swap",
            "wrong_type",
            "not_handled",
            "wrong_route",
            "wrong_response",
        )
        for variant in variants:
            with self.subTest(variant=variant):
                records = []
                calls = []
                acknowledgement = _acknowledgement(
                    custody_records=records,
                    acknowledgements=calls,
                )
                replacement_ack = _acknowledgement(custody_records=[])
                healthy = CommandStatusRouteDecisionFactory()

                def tampered_status(**values):
                    decision = healthy.status(**values)
                    if variant == "ack_drop":
                        return replace(
                            decision,
                            response_publication_acknowledgement=None,
                        )
                    if variant == "ack_swap":
                        return replace(
                            decision,
                            response_publication_acknowledgement=replacement_ack,
                        )
                    if variant == "wrong_type":
                        return SimpleNamespace(**vars(decision))
                    if variant == "not_handled":
                        return replace(decision, handled=False)
                    if variant == "wrong_route":
                        return replace(decision, route_kind="minecraft_command")
                    return replace(decision, response_kind="immediate")

                decisions = SimpleNamespace(
                    status=tampered_status,
                    cautious=healthy.cautious,
                    conversational_fallthrough=(
                        healthy.conversational_fallthrough
                    ),
                    emergency=healthy.emergency,
                )
                owner = self._owner(
                    snapshot=self._running_snapshot(acknowledgement),
                    decision_factory=decisions,
                )

                decision = owner.try_route(self.event, self.proof)

                self.assertIs(COMMAND_STATUS_EMERGENCY_DECISION, decision)
                self.assertEqual([False], calls)
                self.assertEqual("primary_decision_assembly", records[0]["stage"])

    def test_fallback_factory_tamper_keeps_first_failure_and_spends_ack(self):
        variants = (
            "ack_drop",
            "ack_swap",
            "wrong_type",
            "not_handled",
            "wrong_route",
            "wrong_response",
        )
        for variant in variants:
            with self.subTest(variant=variant):
                records = []
                calls = []
                acknowledgement = _acknowledgement(
                    custody_records=records,
                    acknowledgements=calls,
                )
                replacement_ack = _acknowledgement(custody_records=[])
                healthy = CommandStatusRouteDecisionFactory()

                def tampered_cautious(**values):
                    decision = healthy.cautious(**values)
                    if variant == "ack_drop":
                        return replace(
                            decision,
                            response_publication_acknowledgement=None,
                        )
                    if variant == "ack_swap":
                        return replace(
                            decision,
                            response_publication_acknowledgement=replacement_ack,
                        )
                    if variant == "wrong_type":
                        return SimpleNamespace(**vars(decision))
                    if variant == "not_handled":
                        return replace(decision, handled=False)
                    if variant == "wrong_route":
                        return replace(decision, route_kind="minecraft_command")
                    return replace(decision, response_kind="immediate")

                decisions = SimpleNamespace(
                    status=healthy.status,
                    cautious=tampered_cautious,
                    conversational_fallthrough=(
                        healthy.conversational_fallthrough
                    ),
                    emergency=healthy.emergency,
                )
                owner = self._owner(
                    snapshot=self._running_snapshot(acknowledgement),
                    renderer=lambda *_values: _raise(LookupError()),
                    decision_factory=decisions,
                )

                decision = owner.try_route(self.event, self.proof)

                self.assertIs(COMMAND_STATUS_EMERGENCY_DECISION, decision)
                self.assertEqual([False], calls)
                self.assertEqual(
                    [
                        {
                            "stage": "status_rendering",
                            "exception_class": "LookupError",
                        }
                    ],
                    records,
                )

    def test_primary_factory_may_return_normal_dataclass_replace(self):
        records = []
        calls = []
        acknowledgement = _acknowledgement(
            custody_records=records,
            acknowledgements=calls,
        )
        healthy = CommandStatusRouteDecisionFactory()
        decisions = SimpleNamespace(
            status=lambda **values: replace(
                healthy.status(**values),
                response_text="working safely",
            ),
            cautious=healthy.cautious,
            conversational_fallthrough=healthy.conversational_fallthrough,
            emergency=healthy.emergency,
        )
        owner = self._owner(
            snapshot=self._running_snapshot(acknowledgement),
            decision_factory=decisions,
        )

        decision = owner.try_route(self.event, self.proof)

        self.assertEqual("working safely", decision.response_text)
        self.assertIs(
            acknowledgement,
            decision.response_publication_acknowledgement,
        )
        self.assertEqual([], records)
        self.assertEqual([], calls)

    def test_fallthrough_assembly_failure_records_known_candidate_facts(self):
        records = []
        query = CommandStatusQuery(
            requested_family="store_home",
            addressed=False,
        )
        snapshot = CommandFeedbackLifecycleSnapshot(
            state=CommandFeedbackLifecycleSnapshot.UNAVAILABLE,
            descriptor=object(),
            command_name="store_home",
            requested_family="store_home",
            query_matched=False,
            owner_present=True,
            availability_reason="query_mismatch",
            terminal_state="unclaimed",
        )
        healthy = CommandStatusRouteDecisionFactory()
        decisions = SimpleNamespace(
            status=healthy.status,
            cautious=healthy.cautious,
            conversational_fallthrough=lambda: _raise(RuntimeError()),
            emergency=healthy.emergency,
        )
        owner = self._owner(
            query=query,
            snapshot=snapshot,
            decision_factory=decisions,
            diagnostics=SimpleNamespace(record=lambda **values: records.append(values)),
        )

        decision = owner.try_route(self.event, self.proof)

        self.assertIs(COMMAND_STATUS_EMERGENCY_DECISION, decision)
        self.assertIs(query, records[0]["query"])
        self.assertIs(snapshot, records[0]["snapshot"])
        self.assertEqual("primary_decision_assembly", records[0]["stage"])

    def _owner(
        self,
        *,
        query=None,
        snapshot=None,
        classifier=None,
        extension=None,
        proof_validator=None,
        renderer=None,
        projector=None,
        decision_factory=None,
        diagnostics=None,
    ) -> CommandStatusRouteOwner:
        selected_query = self.query if query is None else query
        selected_snapshot = (
            CommandFeedbackLifecycleSnapshot(
                state=CommandFeedbackLifecycleSnapshot.IDLE,
                query_matched=True,
                owner_present=False,
                availability_reason="no_tracked_owner",
            )
            if snapshot is None
            else snapshot
        )
        return CommandStatusRouteOwner(
            extension=(
                extension
                if extension is not None
                else SimpleNamespace(
                    inspect_command_feedback_status=lambda _query: selected_snapshot
                )
            ),
            live_proof_validator=(
                proof_validator
                if proof_validator is not None
                else lambda proof, _event: proof is self.proof
            ),
            classifier=(
                classifier
                if classifier is not None
                else SimpleNamespace(classify=lambda _text: selected_query)
            ),
            response_renderer=SimpleNamespace(
                render_status=(renderer if renderer is not None else lambda *_: "대기 중이야")
            ),
            presentation_detail_projector=(
                projector
                if projector is not None
                else SimpleNamespace(project=lambda _descriptor: "")
            ),
            decision_factory=decision_factory,
            failure_diagnostics=(
                diagnostics
                if diagnostics is not None
                else SimpleNamespace(record=lambda **_values: True)
            ),
        )

    @staticmethod
    def _running_snapshot(acknowledgement: object):
        return CommandFeedbackLifecycleSnapshot(
            state=CommandFeedbackLifecycleSnapshot.RUNNING,
            descriptor=object(),
            command_name="get",
            requested_family="item_get",
            query_matched=True,
            owner_present=True,
            publication_acknowledgement=acknowledgement,
            terminal_state="unclaimed",
        )


def _raise(error: Exception):
    raise error


def _acknowledgement(*, custody_records, acknowledgements=None):
    publication_calls = (
        acknowledgements if acknowledgements is not None else []
    )
    custody = SimpleNamespace(
        record_once=lambda *args, **values: custody_records.append(
            values
            or {"stage": args[0], "exception_class": args[1]}
        )
    )
    return CommandFeedbackPublicationAcknowledgement(
        permit=CommandFeedbackPublicationPermit(
            lifecycle_token=object(),
            sequence=1,
            kind=CommandFeedbackPublicationPermit.STATUS,
        ),
        callback=lambda _permit, published: (
            publication_calls.append(published) or True
        ),
        publication_failure_diagnostic_custody=custody,
    )


if __name__ == "__main__":
    unittest.main()
