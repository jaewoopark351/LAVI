#20260905_kpopmodder: Lock the public router as a compatibility-preserving facade.
from __future__ import annotations

import unittest
from types import SimpleNamespace

from plugins.Minecraft.fabric.chatclef.input import (
    MinecraftChatClefInputRouteDecision,
    MinecraftChatClefInputRouter,
)
from plugins.Minecraft.fabric.chatclef.input.auto_deposit_trust.routing import (
    AutoDepositTrustRouteCoordinator,
)
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary import (
    OrdinaryMinecraftCommandRouteCoordinator,
)
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.ordinary_route_availability_stage import (
    OrdinaryRouteAvailabilityStage,
)
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.composition import (
    OrdinaryMinecraftCommandRouteCompatibilityInstaller,
    OrdinaryMinecraftCommandRouteComponentGraph,
)
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.ordinary_minecraft_command_route_pipeline import (
    OrdinaryMinecraftCommandRoutePipeline,
)
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.precheck import (
    OrdinarySubmissionPrecheckStage,
)
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.ordinary_submission_reconciliation_stage import (
    OrdinarySubmissionReconciliationStage,
)
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.rejection import (
    OrdinaryTranslationRejectionStage,
)
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.submission import (
    OrdinarySubmissionResultStage,
)
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.translation import (
    OrdinaryTranslationStage,
)
from plugins.Minecraft.fabric.chatclef.input.routing.composition import (
    MinecraftChatClefRouterCompatibilityInstaller,
    MinecraftChatClefRouterDependencyResolver,
)
from plugins.Minecraft.fabric.chatclef.input.routing.orchestration import (
    MinecraftInputRouteOrderingCoordinator,
)
from plugins.Minecraft.fabric.chatclef.input.routing.orchestration.minecraft_input_gate_inspector import (
    MinecraftInputGateInspector,
)
from plugins.Minecraft.fabric.chatclef.input.routing.orchestration.invocation import (
    MinecraftOptionalRouteOwnerInvoker,
)
from plugins.Minecraft.fabric.chatclef.input.routing.trusted_korean import (
    TrustedKoreanInputRouteCoordinator,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.publication import (
    CommandFeedbackPublicationAcknowledgement,
    CommandFeedbackPublicationPermit,
)


class MinecraftChatClefInputRouterDelegationTests(unittest.TestCase):
    def test_component_graph_installs_focused_coordinators_and_shared_seams(self):
        router = MinecraftChatClefInputRouter(
            extension=None,
            log_callback=lambda _message: None,
        )

        self.assertIsInstance(
            router._trusted_input_route_coordinator,
            TrustedKoreanInputRouteCoordinator,
        )
        self.assertIsInstance(
            router._route_ordering_coordinator,
            MinecraftInputRouteOrderingCoordinator,
        )
        self.assertIsInstance(
            router._route_ordering_coordinator._input_gate_inspector,
            MinecraftInputGateInspector,
        )
        self.assertIsInstance(
            router._route_ordering_coordinator._optional_route_owner_invoker,
            MinecraftOptionalRouteOwnerInvoker,
        )
        self.assertIsInstance(
            router._auto_deposit_trust_route_coordinator,
            AutoDepositTrustRouteCoordinator,
        )
        self.assertIsInstance(
            router._ordinary_command_route_coordinator,
            OrdinaryMinecraftCommandRouteCoordinator,
        )
        self.assertIsInstance(
            router._component_graph._dependency_resolver,
            MinecraftChatClefRouterDependencyResolver,
        )
        self.assertIsInstance(
            router._component_graph._compatibility_installer,
            MinecraftChatClefRouterCompatibilityInstaller,
        )
        ordinary = router._ordinary_command_route_coordinator
        self.assertIsInstance(
            ordinary._component_graph,
            OrdinaryMinecraftCommandRouteComponentGraph,
        )
        self.assertIsInstance(
            ordinary._component_graph._compatibility_installer,
            OrdinaryMinecraftCommandRouteCompatibilityInstaller,
        )
        self.assertIsInstance(
            ordinary._availability_stage,
            OrdinaryRouteAvailabilityStage,
        )
        self.assertIsInstance(
            ordinary._reconciliation_stage,
            OrdinarySubmissionReconciliationStage,
        )
        self.assertIsInstance(ordinary._translation_stage, OrdinaryTranslationStage)
        self.assertIsInstance(
            ordinary._rejection_stage,
            OrdinaryTranslationRejectionStage,
        )
        self.assertIsInstance(
            ordinary._precheck_stage,
            OrdinarySubmissionPrecheckStage,
        )
        self.assertIsInstance(
            ordinary._submission_result_stage,
            OrdinarySubmissionResultStage,
        )
        self.assertIsInstance(
            ordinary._pipeline,
            OrdinaryMinecraftCommandRoutePipeline,
        )
        self.assertIs(
            router._submission_boundary,
            router._ordinary_command_route_coordinator._submission_boundary,
        )
        self.assertIs(
            router._submission_boundary,
            router._auto_deposit_trust_route_coordinator._submission_boundary,
        )
        self.assertIs(
            router.submission_reconciliation,
            router._ordinary_command_route_coordinator._submission_reconciliation,
        )

    def test_one_status_custody_policy_is_shared_across_every_router_boundary(self):
        router = MinecraftChatClefInputRouter(
            extension=None,
            log_callback=lambda _message: None,
        )
        graph = router._component_graph
        policy = graph.command_status_publication_custody_policy

        self.assertIs(
            policy,
            graph.command_status_publication_failure_adapter._custody_policy,
        )
        self.assertIs(
            policy,
            graph.command_status_route_owner._publication_custody_policy,
        )
        self.assertIs(
            policy,
            graph.trusted_input_route_coordinator
            ._components.status_publication_custody_guard._custody_policy,
        )
        self.assertIs(
            policy,
            graph.route_ordering_coordinator
            ._optional_route_owner_invoker._result_guard._custody_policy,
        )
        self.assertIs(
            graph.command_status_emergency_decision,
            graph.trusted_input_route_coordinator
            ._components.status_publication_custody_guard._emergency_decision,
        )
        self.assertIs(
            graph.command_status_emergency_decision,
            graph.route_ordering_coordinator
            ._optional_route_owner_invoker._result_guard._emergency_decision,
        )

    def test_public_route_delegates_without_changing_arguments_or_result(self):
        router = MinecraftChatClefInputRouter(
            extension=None,
            log_callback=lambda _message: None,
        )
        coordinator = _RecordingRouteCoordinator()
        router._route_ordering_coordinator = coordinator
        proof = object()

        decision = router.route("다이아몬드 캐줘", korean_eligibility_proof=proof)

        self.assertIs(coordinator.decision, decision)
        self.assertEqual([("다이아몬드 캐줘", proof)], coordinator.calls)

    def test_trusted_entrypoint_delegates_exact_ingress_evidence(self):
        router = MinecraftChatClefInputRouter(
            extension=None,
            log_callback=lambda _message: None,
        )
        coordinator = _RecordingTrustedCoordinator()
        router._trusted_input_route_coordinator = coordinator
        evidence = object()

        decision = router.route_trusted_user_input("멈춰", evidence)

        self.assertIs(coordinator.decision, decision)
        self.assertEqual([("멈춰", evidence)], coordinator.calls)

    def test_public_status_custody_ports_delegate_to_the_shared_policy_and_adapter(self):
        router = MinecraftChatClefInputRouter(
            extension=None,
            log_callback=lambda _message: None,
        )
        records = []
        acknowledgement = CommandFeedbackPublicationAcknowledgement(
            permit=CommandFeedbackPublicationPermit(
                lifecycle_token=object(),
                sequence=1,
                kind=CommandFeedbackPublicationPermit.STATUS,
            ),
            callback=lambda _permit, _published: True,
            publication_failure_diagnostic_custody=SimpleNamespace(
                record_once=lambda stage, exception_class: records.append(
                    (stage, exception_class)
                )
            ),
        )
        decision = MinecraftChatClefInputRouteDecision.handled_result(
            reason="minecraft_command_status_query",
            response_text="작업 중이야",
            route_kind="command_status_query",
            response_kind="command_status",
            response_publication_acknowledgement=acknowledgement,
        )

        self.assertTrue(
            router.claims_routed_input_publication_custody(decision)
        )
        router.observe_routed_input_publication_failure(
            decision,
            stage="dispatcher_initial_decision_resolution",
            exception_class="RuntimeError",
        )

        self.assertEqual(
            [("dispatcher_initial_decision_resolution", "RuntimeError")],
            records,
        )

    def test_ordinary_route_locked_delegates_exact_inputs_to_pipeline(self):
        router = MinecraftChatClefInputRouter(
            extension=None,
            log_callback=lambda _message: None,
        )
        pipeline = _RecordingOrdinaryPipeline()
        ordinary = router._ordinary_command_route_coordinator
        ordinary._pipeline = pipeline
        event = object()
        proof = object()

        decision = ordinary.route_locked(
            event,
            "레드스톤 5개 구해줘",
            korean_eligibility_proof=proof,
        )

        self.assertIs(pipeline.decision, decision)
        self.assertEqual(
            [(event, "레드스톤 5개 구해줘", proof)],
            pipeline.calls,
        )


class _RecordingRouteCoordinator:
    def __init__(self):
        self.calls = []
        self.decision = MinecraftChatClefInputRouteDecision.not_handled(
            "recorded_route"
        )

    def route(self, value, *, korean_eligibility_proof=None):
        self.calls.append((value, korean_eligibility_proof))
        return self.decision


class _RecordingTrustedCoordinator:
    def __init__(self):
        self.calls = []
        self.decision = MinecraftChatClefInputRouteDecision.not_handled(
            "recorded_trusted_route"
        )

    def route(self, value, consumed_ingress_evidence):
        self.calls.append((value, consumed_ingress_evidence))
        return self.decision


class _RecordingOrdinaryPipeline:
    def __init__(self):
        self.calls = []
        self.decision = MinecraftChatClefInputRouteDecision.not_handled(
            "recorded_ordinary_route"
        )

    def route_locked(
        self,
        event,
        command_text,
        *,
        korean_eligibility_proof=None,
    ):
        self.calls.append((event, command_text, korean_eligibility_proof))
        return self.decision


if __name__ == "__main__":
    unittest.main()
