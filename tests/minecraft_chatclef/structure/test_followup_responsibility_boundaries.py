#20260905_kpopmodder: Lock the follow-up responsibility boundary component graphs.
from __future__ import annotations

import threading
import unittest
from unittest.mock import Mock

from plugins.Minecraft.fabric.chatclef.extension import (
    MinecraftFabricChatClefExtension,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language.submission.pipeline import (
    TranslatedCommandPipelineRequestBuilder,
    TranslatedCommandPreSubmitGuard,
    TranslatedCommandSubmissionFailureHandler,
    TranslatedCommandSubmissionResultHandler,
    TranslatedCommandTransportInvoker,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language.submission.stages.admission import (
    TranslatedCommandAdmissionCommitter,
    TranslatedCommandAdmissionInspector,
)
from plugins.Minecraft.fabric.chatclef.input import MinecraftChatClefInputRouter
from plugins.Minecraft.fabric.chatclef.input.routing.decisions.submission import (
    MinecraftPrecheckRouteDecisionFactory,
    MinecraftReconciledRouteDecisionFactory,
    MinecraftSubmittedRouteDecisionFactory,
)
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.precheck.ordinary_submission_precheck_diagnostics import (
    OrdinarySubmissionPrecheckDiagnostics,
)
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.precheck.ordinary_submission_precheck_evaluator import (
    OrdinarySubmissionPrecheckEvaluator,
)
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.rejection.ordinary_item_translation_rejection_owner import (
    OrdinaryItemTranslationRejectionOwner,
)
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.rejection.ordinary_generic_translation_rejection import (
    OrdinaryGenericTranslationRejection,
)
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.submission.ordinary_submission_decision_builder import (
    OrdinarySubmissionDecisionBuilder,
)
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.submission.ordinary_submission_result_diagnostics import (
    OrdinarySubmissionResultDiagnostics,
)
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.submission.ordinary_submission_result_reconciler import (
    OrdinarySubmissionResultReconciler,
)
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.submission.ordinary_submission_transport import (
    OrdinarySubmissionTransport,
)
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.translation.ordinary_translation_invocation import (
    OrdinaryTranslationInvocation,
)
from plugins.Minecraft.fabric.chatclef.input.routing.ordinary.translation.ordinary_translation_dto_validation import (
    OrdinaryTranslationDtoValidation,
)
from plugins.Minecraft.fabric.chatclef.input.routing.orchestration.minecraft_generic_crafting_dispatch_cleanup import (
    MinecraftGenericCraftingDispatchCleanup,
)
from plugins.Minecraft.fabric.chatclef.input.routing.orchestration.failure import (
    MinecraftRouteFailureDecisionBuilder,
    MinecraftRouteFailureDiagnostics,
)
from plugins.Minecraft.fabric.chatclef.input.routing.orchestration.minecraft_input_route_sequence import (
    MinecraftInputRouteSequence,
)
from plugins.Minecraft.fabric.chatclef.input.routing.submission.minecraft_chatclef_submission_outcome_validator import (
    MinecraftChatClefSubmissionOutcomeValidator,
)
from plugins.Minecraft.fabric.chatclef.input.routing.submission.minecraft_chatclef_submission_request_builder import (
    MinecraftChatClefSubmissionRequestBuilder,
)
from plugins.Minecraft.fabric.chatclef.input.routing.submission.minecraft_chatclef_submission_transport_invoker import (
    MinecraftChatClefSubmissionTransportInvoker,
)
from plugins.Minecraft.fabric.chatclef.input.routing.translation import (
    MinecraftChatClefTranslationCapabilityInspector,
    MinecraftChatClefTranslationInvoker,
    MinecraftChatClefTranslationResultValidator,
)
from plugins.Minecraft.fabric.chatclef.transport.server.fabric_chatclef_client_handler import (
    FabricChatClefClientHandler,
)
from plugins.Minecraft.fabric.chatclef.transport.server.client_session.handshake import (
    FabricChatClefHandshakeAcknowledger,
    FabricChatClefHandshakeAdmission,
    FabricChatClefHandshakeDiagnostics,
    FabricChatClefHandshakeSessionPersistence,
    FabricChatClefSessionIdFactory,
)


class FollowupResponsibilityBoundaryTests(unittest.TestCase):
    def test_router_boundaries_install_focused_components(self):
        router = MinecraftChatClefInputRouter(
            extension=None,
            log_callback=lambda _message: None,
        )

        submission = router._submission_boundary
        self.assertIsInstance(
            submission._request_builder,
            MinecraftChatClefSubmissionRequestBuilder,
        )
        self.assertIsInstance(
            submission._transport_invoker,
            MinecraftChatClefSubmissionTransportInvoker,
        )
        self.assertIsInstance(
            submission._outcome_validator,
            MinecraftChatClefSubmissionOutcomeValidator,
        )
        translation = router._translation_boundary
        self.assertIsInstance(
            translation._capability_inspector,
            MinecraftChatClefTranslationCapabilityInspector,
        )
        self.assertIsInstance(
            translation._invoker,
            MinecraftChatClefTranslationInvoker,
        )
        self.assertIsInstance(
            translation._result_validator,
            MinecraftChatClefTranslationResultValidator,
        )

    def test_router_orchestration_installs_focused_route_stages(self):
        router = MinecraftChatClefInputRouter(
            extension=None,
            log_callback=lambda _message: None,
        )
        failure = router._route_failure_handler
        ordering = router._route_ordering_coordinator
        ordinary = router._ordinary_command_route_coordinator

        self.assertIsInstance(
            failure._diagnostics,
            MinecraftRouteFailureDiagnostics,
        )
        self.assertIsInstance(
            failure._decision_builder,
            MinecraftRouteFailureDecisionBuilder,
        )
        self.assertIsInstance(
            ordering._crafting_dispatch_cleanup,
            MinecraftGenericCraftingDispatchCleanup,
        )
        self.assertIsInstance(ordering._route_sequence, MinecraftInputRouteSequence)
        self.assertIsInstance(
            ordinary._translation_stage._invocation,
            OrdinaryTranslationInvocation,
        )
        self.assertIsInstance(
            ordinary._translation_stage._dto_validation,
            OrdinaryTranslationDtoValidation,
        )
        self.assertIsInstance(
            ordinary._rejection_stage._item_rejection_owner,
            OrdinaryItemTranslationRejectionOwner,
        )
        self.assertIsInstance(
            ordinary._rejection_stage._generic_rejection,
            OrdinaryGenericTranslationRejection,
        )
        self.assertIsInstance(
            ordinary._precheck_stage._evaluator,
            OrdinarySubmissionPrecheckEvaluator,
        )
        self.assertIsInstance(
            ordinary._precheck_stage._diagnostics,
            OrdinarySubmissionPrecheckDiagnostics,
        )
        submission = ordinary._submission_result_stage
        self.assertIsInstance(submission._transport, OrdinarySubmissionTransport)
        self.assertIsInstance(
            submission._reconciler,
            OrdinarySubmissionResultReconciler,
        )
        self.assertIsInstance(
            submission._diagnostics,
            OrdinarySubmissionResultDiagnostics,
        )
        self.assertIsInstance(
            submission._decision_builder,
            OrdinarySubmissionDecisionBuilder,
        )

    def test_submission_decision_and_pipeline_facades_install_focused_owners(self):
        adapter = Mock()
        adapter.backend_id = "fabric_chatclef"
        extension = MinecraftFabricChatClefExtension(adapter=adapter)
        pipeline = extension._natural_language_commands._translated_submission._pipeline
        admission = pipeline._admission_stage

        self.assertIsInstance(
            admission._inspector,
            TranslatedCommandAdmissionInspector,
        )
        self.assertIsInstance(
            admission._committer,
            TranslatedCommandAdmissionCommitter,
        )
        self.assertIsInstance(
            pipeline._request_builder,
            TranslatedCommandPipelineRequestBuilder,
        )
        self.assertIsInstance(
            pipeline._pre_submit_guard,
            TranslatedCommandPreSubmitGuard,
        )
        self.assertIsInstance(pipeline._transport, TranslatedCommandTransportInvoker)
        self.assertIsInstance(
            pipeline._failure_handler,
            TranslatedCommandSubmissionFailureHandler,
        )
        self.assertIsInstance(
            pipeline._result_handler,
            TranslatedCommandSubmissionResultHandler,
        )
        submission_factory = MinecraftChatClefInputRouter(
            extension=None,
            log_callback=lambda _message: None,
        )._decision_factory._submission_factory
        self.assertIsInstance(
            submission_factory._reconciliation_factory,
            MinecraftReconciledRouteDecisionFactory,
        )
        self.assertIsInstance(
            submission_factory._precheck_factory,
            MinecraftPrecheckRouteDecisionFactory,
        )
        self.assertIsInstance(
            submission_factory._submitted_factory,
            MinecraftSubmittedRouteDecisionFactory,
        )

    def test_client_handshake_handler_installs_focused_owners(self):
        handler = FabricChatClefClientHandler(
            connection_ownership=Mock(),
            command_lock=threading.RLock(),
            session_registry=Mock(),
            diagnostics=Mock(),
            envelope_transport=Mock(),
            command_result_handler=Mock(),
            status_provider=dict,
            stopping_provider=lambda: False,
            last_error_reporter=lambda _message: None,
            now_ms=lambda: 1,
        )._handshake_handler

        self.assertIsInstance(
            handler._session_id_factory,
            FabricChatClefSessionIdFactory,
        )
        self.assertIsInstance(handler._admission, FabricChatClefHandshakeAdmission)
        self.assertIsInstance(
            handler._persistence,
            FabricChatClefHandshakeSessionPersistence,
        )
        self.assertIsInstance(
            handler._acknowledger,
            FabricChatClefHandshakeAcknowledger,
        )
        self.assertIsInstance(
            handler._handshake_diagnostics,
            FabricChatClefHandshakeDiagnostics,
        )


if __name__ == "__main__":
    unittest.main()
