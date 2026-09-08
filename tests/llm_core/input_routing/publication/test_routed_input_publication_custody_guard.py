#20260908_kpopmodder: Verify both injected dispatcher resolver custody boundaries.
from __future__ import annotations

from dataclasses import replace
import threading
import unittest
from types import SimpleNamespace

from llm_core.input_routing import RoutedInputDispatchCoordinator
from llm_core.input_routing.decision import RoutedInputDecisionResolver
from plugins.Minecraft.fabric.chatclef.input.minecraft_chatclef_input_route_decision import (
    MinecraftChatClefInputRouteDecision,
)
from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle.publication import (
    CommandStatusPublicationCustodyPolicy,
)
from plugins.Minecraft.fabric.chatclef.input.status.command_lifecycle.diagnostics import (
    CommandStatusPublicationFailureAdapter,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.publication import (
    CommandFeedbackPublicationAcknowledgement,
    CommandFeedbackPublicationPermit,
)


class RoutedInputPublicationCustodyGuardTests(unittest.TestCase):
    def test_initial_resolver_exception_acks_false_then_observes_and_suppresses(self):
        for route_kind, response_kind in _STATUS_ROUTE_IDENTITIES:
            with self.subTest(route_kind=route_kind):
                coordinator, calls, observations = _coordinator(
                    route_kind=route_kind,
                    response_kind=response_kind,
                )
                coordinator._components.decision_resolver = _FailingResolver(
                    fail_on=1
                )

                outcome = coordinator.dispatch("event")

                self.assertTrue(outcome.suppress_response)
                self.assertEqual([False], calls)
                self.assertEqual(
                    [("dispatcher_initial_decision_resolution", "RuntimeError")],
                    observations,
                )

    def test_ready_resolver_exception_is_independently_guarded(self):
        for route_kind, response_kind in _STATUS_ROUTE_IDENTITIES:
            with self.subTest(route_kind=route_kind):
                coordinator, calls, observations = _coordinator(
                    activate_turn=True,
                    route_kind=route_kind,
                    response_kind=response_kind,
                )
                coordinator._components.decision_resolver = _FailingResolver(
                    fail_on=2
                )

                outcome = coordinator.dispatch("event")

                self.assertTrue(outcome.suppress_response)
                self.assertEqual([False], calls)
                self.assertEqual(
                    [("dispatcher_ready_decision_resolution", "RuntimeError")],
                    observations,
                )

    def test_router_replacement_and_none_clear_stale_custody_ports(self):
        coordinator, calls, observations = _coordinator()
        unprotected = _Router(
            _decision(_acknowledgement([], activate_turn=False)),
            claims=False,
        )
        coordinator.set_router(unprotected)
        coordinator._components.decision_resolver = _FailingResolver(fail_on=1)

        with self.assertRaises(RuntimeError):
            coordinator.dispatch("event")
        coordinator.set_router(None)
        outcome = coordinator.dispatch("event")

        self.assertFalse(outcome.handled)
        self.assertEqual([], calls)
        self.assertEqual([], observations)

    def test_unclaimed_publisher_boundary_exception_is_not_reclassified(self):
        decision = MinecraftChatClefInputRouteDecision.handled_result(
            reason="ordinary",
            response_text="response",
            publish_external_response=True,
        )
        coordinator = RoutedInputDispatchCoordinator(
            response_publisher_callback=lambda: None,
            router=_Router(decision, claims=False),
            log_callback=lambda _message: None,
        )
        coordinator._components.external_response_publisher = SimpleNamespace(
            publish=lambda **_values: _raise(RuntimeError("ordinary"))
        )

        with self.assertRaises(RuntimeError):
            coordinator.dispatch("event")

    def test_observer_failure_cannot_disable_acknowledgement_cleanup(self):
        coordinator, calls, _observations = _coordinator(observer_raises=True)
        coordinator._components.decision_resolver = _FailingResolver(fail_on=1)

        outcome = coordinator.dispatch("event")

        self.assertTrue(outcome.suppress_response)
        self.assertEqual([False], calls)

    def test_ready_decision_route_metadata_tamper_spends_original_custody(self):
        variants = ("type", "handled", "route", "response")
        for variant in variants:
            with self.subTest(variant=variant):
                publisher = _Publisher(SimpleNamespace(output_delivered=True))

                def tamper(decision):
                    if variant == "type":
                        return _StatusDecisionSubclass(**vars(decision))
                    if variant == "handled":
                        return replace(decision, handled=False)
                    if variant == "route":
                        return replace(decision, route_kind="minecraft_command")
                    return replace(decision, response_kind="immediate")

                coordinator, calls, observations = _coordinator(
                    activate_turn=True,
                    publisher=publisher,
                    ready_transform=tamper,
                )

                outcome = coordinator.dispatch("event")

                self.assertTrue(outcome.suppress_response)
                self.assertEqual([False], calls)
                self.assertEqual(
                    [("publication_route_identity", "none")],
                    observations,
                )
                self.assertEqual([], publisher.calls)

    def test_ready_decision_ack_swap_spends_only_original_custody(self):
        replacement_calls = []
        logs = []
        replacement = _acknowledgement(
            replacement_calls,
            activate_turn=True,
        )
        publisher = _Publisher(SimpleNamespace(output_delivered=True))
        coordinator, calls, observations = _coordinator(
            activate_turn=True,
            publisher=publisher,
            logs=logs,
            ready_transform=lambda decision: replace(
                decision,
                response_publication_acknowledgement=replacement,
            ),
        )

        outcome = coordinator.dispatch("event")

        self.assertTrue(outcome.suppress_response)
        self.assertEqual([False], calls)
        self.assertEqual([], replacement_calls)
        self.assertEqual(
            [("publication_acknowledgement_identity", "none")],
            observations,
        )
        self.assertEqual([], publisher.calls)
        self.assertEqual([], logs)

    def test_protected_ready_resolver_failure_has_one_status_diagnostic(self):
        scenarios = (
            ("exception", "RuntimeError"),
            ("none", "none"),
        )
        for scenario, expected_exception_class in scenarios:
            with self.subTest(scenario=scenario):
                logs = []
                if scenario == "exception":
                    def transform(_decision):
                        return _raise(RuntimeError("SECRET"))
                else:
                    def transform(_decision):
                        return None
                coordinator, calls, observations = _coordinator(
                    activate_turn=True,
                    publisher=_Publisher(
                        SimpleNamespace(output_delivered=True)
                    ),
                    ready_transform=transform,
                    logs=logs,
                )

                outcome = coordinator.dispatch("event")

                self.assertTrue(outcome.suppress_response)
                self.assertEqual([False], calls)
                self.assertEqual(
                    [
                        (
                            "dispatcher_ready_decision_resolution",
                            expected_exception_class,
                        )
                    ],
                    observations,
                )
                self.assertEqual([], logs)

    def test_status_publisher_exception_uses_custody_without_generic_log(self):
        logs = []
        publisher = _Publisher(error=LookupError("SECRET"))
        coordinator, calls, observations = _coordinator(
            activate_turn=True,
            publisher=publisher,
            logs=logs,
        )

        outcome = coordinator.dispatch("event")

        self.assertTrue(outcome.suppress_response)
        self.assertEqual([False], calls)
        self.assertEqual(
            [("dispatcher_external_response_publication", "LookupError")],
            observations,
        )
        self.assertEqual([], logs)

    def test_prior_status_failure_prevents_duplicate_publisher_diagnostic(self):
        records = []
        custody = _OneShotCustody(records)
        self.assertTrue(custody.record_once("status_rendering", "LookupError"))
        calls = []
        acknowledgement = _acknowledgement(
            calls,
            activate_turn=True,
            diagnostic_custody=custody,
        )
        decision = _decision(acknowledgement)
        policy = CommandStatusPublicationCustodyPolicy()
        adapter = CommandStatusPublicationFailureAdapter(
            custody_policy=policy,
        )
        router = _Router(
            decision,
            observer=lambda observed, **values: adapter.observe(
                observed,
                **values,
            ),
        )
        router.claims_routed_input_publication_custody = policy.matches
        logs = []
        coordinator = RoutedInputDispatchCoordinator(
            response_publisher_callback=lambda: _Publisher(
                error=RuntimeError("SECRET")
            ),
            router=router,
            log_callback=logs.append,
        )

        outcome = coordinator.dispatch("event")

        self.assertTrue(outcome.suppress_response)
        self.assertEqual([False], calls)
        self.assertEqual(
            [("status_rendering", "LookupError")],
            records,
        )
        self.assertEqual([], logs)

    def test_status_output_delivery_property_exception_fails_commit_closed(self):
        publisher = _Publisher(_RaisingEmission())
        coordinator, calls, observations = _coordinator(
            activate_turn=True,
            publisher=publisher,
        )

        outcome = coordinator.dispatch("event")

        self.assertTrue(outcome.suppress_response)
        self.assertEqual([False], calls)
        self.assertEqual(
            [("dispatcher_publication_commit_inspection", "RuntimeError")],
            observations,
        )

    def test_healthy_status_publication_preserves_original_ack_success(self):
        emission = SimpleNamespace(output_delivered=True)
        publisher = _Publisher(emission)
        coordinator, calls, observations = _coordinator(
            activate_turn=True,
            publisher=publisher,
        )

        outcome = coordinator.dispatch("event")

        self.assertTrue(outcome.handled)
        self.assertIs(emission, outcome.response)
        self.assertEqual([True], calls)
        self.assertEqual([], observations)

    def test_router_rebind_cannot_split_decision_from_custody_ports(self):
        calls = []
        observations = []
        entered = threading.Event()
        release = threading.Event()
        setter_started = threading.Event()
        setter_done = threading.Event()
        acknowledgement = _acknowledgement(calls, activate_turn=False)
        decision = _decision(acknowledgement)
        policy = CommandStatusPublicationCustodyPolicy()
        router = _BlockingRouter(
            decision,
            entered=entered,
            release=release,
            observer=lambda _decision, **values: observations.append(
                (values["stage"], values["exception_class"])
            ),
        )
        router.claims_routed_input_publication_custody = policy.matches
        coordinator = RoutedInputDispatchCoordinator(
            response_publisher_callback=lambda: None,
            router=router,
            log_callback=lambda _message: None,
        )
        coordinator._components.decision_resolver = _FailingResolver(fail_on=1)
        outcomes = []

        dispatch_thread = threading.Thread(
            target=lambda: outcomes.append(coordinator.dispatch("event"))
        )

        def replace_router():
            setter_started.set()
            coordinator.set_router(
                _Router(
                    MinecraftChatClefInputRouteDecision.not_handled("new"),
                    claims=False,
                )
            )
            setter_done.set()

        setter_thread = threading.Thread(target=replace_router)
        dispatch_thread.start()
        self.assertTrue(entered.wait(1.0))
        setter_thread.start()
        self.assertTrue(setter_started.wait(1.0))
        self.assertTrue(setter_done.wait(1.0))
        release.set()
        dispatch_thread.join(1.0)
        setter_thread.join(1.0)

        self.assertFalse(dispatch_thread.is_alive())
        self.assertFalse(setter_thread.is_alive())
        self.assertTrue(outcomes[0].suppress_response)
        self.assertEqual([False], calls)
        self.assertEqual(
            [("dispatcher_initial_decision_resolution", "RuntimeError")],
            observations,
        )


class _FailingResolver:
    def __init__(self, *, fail_on):
        self._fail_on = fail_on
        self._calls = 0
        self._delegate = RoutedInputDecisionResolver(_OutcomeFactoryProxy())

    def resolve(self, decision):
        self._calls += 1
        if self._calls == self._fail_on:
            raise RuntimeError("SECRET")
        return self._delegate.resolve(decision)


class _OutcomeFactoryProxy:
    def unhandled(self):
        raise AssertionError("not used")

    def suppressed(self):
        raise AssertionError("not used")

    def handled(self, _response):
        raise AssertionError("not used")


class _Router:
    def __init__(self, decision, *, claims=True, observer=None):
        self._decision = decision
        self._claims = claims
        self._observer = observer or (lambda *_args, **_kwargs: None)

    def route(self, _event):
        return self._decision

    def claims_routed_input_publication_custody(self, _decision):
        return self._claims

    def observe_routed_input_publication_failure(self, decision, **values):
        return self._observer(decision, **values)


class _BlockingRouter(_Router):
    def __init__(self, decision, *, entered, release, observer):
        super().__init__(decision, observer=observer)
        self._entered = entered
        self._release = release

    def route(self, _event):
        self._entered.set()
        if not self._release.wait(1.0):
            raise TimeoutError("test release was not signalled")
        return self._decision


class _StatusDecisionSubclass(MinecraftChatClefInputRouteDecision):
    pass


class _Publisher:
    def __init__(self, emission=None, *, error=None):
        self._emission = emission
        self._error = error
        self.calls = []

    def emit_capability_response(self, text, **metadata):
        self.calls.append((text, metadata))
        if self._error is not None:
            raise self._error
        return self._emission


class _RaisingEmission:
    @property
    def output_delivered(self):
        raise RuntimeError("SECRET")


def _raise(error):
    raise error


class _OneShotCustody:
    def __init__(self, records):
        self._records = records
        self._recorded = False

    def record_once(self, stage, exception_class):
        if self._recorded:
            return False
        self._recorded = True
        self._records.append((stage, exception_class))
        return True


def _coordinator(
    *,
    activate_turn=False,
    observer_raises=False,
    publisher=None,
    ready_transform=None,
    logs=None,
    route_kind="command_status_query",
    response_kind="command_status",
):
    calls = []
    observations = []
    acknowledgement = _acknowledgement(calls, activate_turn=activate_turn)
    decision = _decision(
        acknowledgement,
        route_kind=route_kind,
        response_kind=response_kind,
    )
    if ready_transform is not None:
        acknowledgement.resolve_ready_decision = ready_transform

    def observe(_decision, *, stage, exception_class):
        if observer_raises:
            raise LookupError("sink")
        observations.append((stage, exception_class))

    router = _Router(decision, observer=observe)
    policy = CommandStatusPublicationCustodyPolicy()
    router.claims_routed_input_publication_custody = policy.matches
    coordinator = RoutedInputDispatchCoordinator(
        response_publisher_callback=lambda: publisher,
        router=router,
        log_callback=(logs.append if logs is not None else lambda _message: None),
    )
    return coordinator, calls, observations


def _acknowledgement(
    calls,
    *,
    activate_turn,
    diagnostic_custody=None,
):
    permit = CommandFeedbackPublicationPermit(
        lifecycle_token=object(),
        sequence=1,
        kind=CommandFeedbackPublicationPermit.STATUS,
    )
    if activate_turn:
        permit._activate_turn()
    return CommandFeedbackPublicationAcknowledgement(
        permit=permit,
        callback=lambda _permit, published: calls.append(published) or True,
        publication_failure_diagnostic_custody=diagnostic_custody,
    )


_STATUS_ROUTE_IDENTITIES = (
    ("command_status_query", "command_status"),
    ("crafting_status_query", "immediate"),
)


def _decision(
    acknowledgement,
    *,
    route_kind="command_status_query",
    response_kind="command_status",
):
    return MinecraftChatClefInputRouteDecision.handled_result(
        reason="status",
        response_text="working",
        publish_external_response=True,
        response_emission_capability=object(),
        route_kind=route_kind,
        response_kind=response_kind,
        response_publication_acknowledgement=acknowledgement,
    )


if __name__ == "__main__":
    unittest.main()
