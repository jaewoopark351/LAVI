#20260905_kpopmodder: Verify the natural-language facade delegates Feature-B one-shot work.
from __future__ import annotations

import unittest
from unittest.mock import Mock, sentinel

from input_core.input_event.contracts import LaviInputEvent
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.protocol.command_result_status import CommandResultStatus
from plugins.Minecraft.fabric.chatclef.extension import (
    MinecraftFabricChatClefExtension,
    MinecraftFabricChatClefExtensionLifecycle,
    MinecraftFabricChatClefStatusProvider,
    MinecraftFabricChatClefStopFacade,
)
from plugins.Minecraft.fabric.chatclef.extension.composition import (
    MinecraftFabricChatClefAdapterResolver,
    MinecraftFabricChatClefExtensionCompatibilityInstaller,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language.generic_crafting_defaults import (
    GenericCraftingDefaultsSubmissionCoordinator,
    GenericCraftingDefaultsTranslationCoordinator,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language import (
    LegacyNaturalLanguageCommandCoordinator,
    NaturalLanguageCommandCoordinator,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language.composition import (
    NaturalLanguageCommandCompatibilityInstaller,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language.generic_crafting_defaults.submission import (
    GenericCraftingExtensionSubmissionPipeline,
    GenericCraftingSubmissionActivationSpendGuard,
    GenericCraftingSubmissionCleanup,
    GenericCraftingSubmissionExecutionLifecycle,
    GenericCraftingSubmissionInputDecoder,
    GenericCraftingSubmissionResultRecorder,
    GenericCraftingSubmissionTranslationParser,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language.generic_crafting_defaults.translation import (
    GenericCraftingScopedTranslationInvoker,
    GenericCraftingTranslationActivationGuard,
    GenericCraftingTranslationBindingCommit,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language.submission import (
    TranslatedCommandSubmissionPipeline,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language.submission.composition import (
    TranslatedCommandSubmissionCompatibilityInstaller,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language.submission.stages import (
    TranslatedCommandAdmissionStage,
    TranslatedCommandInputStage,
    TranslatedCommandRequestStage,
    TranslatedCommandRouteClaimLifecycle,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language.translated_command_submission_coordinator import (
    TranslatedCommandSubmissionCoordinator,
)
from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults import (
    GenericCraftingDefaultsActivationState,
    GenericCraftingDefaultsAdmission,
    GenericCraftingDefaultsCandidateDetector,
    GenericCraftingDefaultsProfile,
)
from plugins.Minecraft.fabric.chatclef.input.routing.submission.submission_request_factory import (
    MinecraftChatClefSubmissionRequestFactory,
)


class GenericCraftingDefaultsNaturalLanguageDelegationTests(unittest.TestCase):
    def test_extension_and_natural_language_facades_install_focused_graphs(self):
        adapter = Mock()
        adapter.backend_id = "fabric_chatclef"

        extension = MinecraftFabricChatClefExtension(adapter=adapter)
        coordinator = extension._natural_language_commands
        submission = coordinator._translated_submission

        self.assertIsInstance(
            extension._component_graph._adapter_resolver,
            MinecraftFabricChatClefAdapterResolver,
        )
        self.assertIsInstance(
            extension._component_graph._compatibility_installer,
            MinecraftFabricChatClefExtensionCompatibilityInstaller,
        )
        self.assertIsInstance(
            extension._lifecycle,
            MinecraftFabricChatClefExtensionLifecycle,
        )
        self.assertIsInstance(
            extension._stop_facade,
            MinecraftFabricChatClefStopFacade,
        )
        self.assertIsInstance(
            extension._status_provider,
            MinecraftFabricChatClefStatusProvider,
        )
        self.assertIsInstance(
            coordinator._component_graph._compatibility_installer,
            NaturalLanguageCommandCompatibilityInstaller,
        )
        self.assertIsInstance(
            coordinator._legacy_commands,
            LegacyNaturalLanguageCommandCoordinator,
        )
        generic_translation = coordinator._generic_crafting_translation
        generic_submission = coordinator._generic_crafting_submission
        self.assertIsInstance(
            generic_translation,
            GenericCraftingDefaultsTranslationCoordinator,
        )
        self.assertIsInstance(
            generic_translation._activation_guard,
            GenericCraftingTranslationActivationGuard,
        )
        self.assertIsInstance(
            generic_translation._translation_invoker,
            GenericCraftingScopedTranslationInvoker,
        )
        self.assertIsInstance(
            generic_translation._binding_commit,
            GenericCraftingTranslationBindingCommit,
        )
        self.assertIsInstance(
            generic_submission,
            GenericCraftingDefaultsSubmissionCoordinator,
        )
        self.assertIsInstance(
            generic_submission._input_decoder,
            GenericCraftingSubmissionInputDecoder,
        )
        self.assertIsInstance(
            generic_submission._translation_parser,
            GenericCraftingSubmissionTranslationParser,
        )
        self.assertIsInstance(
            generic_submission._spend_guard,
            GenericCraftingSubmissionActivationSpendGuard,
        )
        self.assertIsInstance(
            generic_submission._early_result_recorder,
            GenericCraftingSubmissionResultRecorder,
        )
        self.assertIsInstance(
            generic_submission._cleanup,
            GenericCraftingSubmissionCleanup,
        )
        self.assertIsInstance(
            generic_submission._execution_lifecycle,
            GenericCraftingSubmissionExecutionLifecycle,
        )
        self.assertIsInstance(
            generic_submission._pipeline,
            GenericCraftingExtensionSubmissionPipeline,
        )
        self.assertIsInstance(submission, TranslatedCommandSubmissionCoordinator)
        self.assertIsInstance(
            submission._component_graph._compatibility_installer,
            TranslatedCommandSubmissionCompatibilityInstaller,
        )
        self.assertIsInstance(submission._input_stage, TranslatedCommandInputStage)
        self.assertIsInstance(
            submission._admission_stage,
            TranslatedCommandAdmissionStage,
        )
        self.assertIsInstance(
            submission._request_stage,
            TranslatedCommandRequestStage,
        )
        self.assertIsInstance(
            submission._route_claim_lifecycle,
            TranslatedCommandRouteClaimLifecycle,
        )
        self.assertIsInstance(
            submission._pipeline,
            TranslatedCommandSubmissionPipeline,
        )

    def test_facade_delegates_feature_translation_and_submission_unchanged(self):
        translated_submission = Mock()
        generic_translation = Mock()
        generic_submission = Mock()
        generic_translation.translate.return_value = sentinel.translation_result
        generic_submission.submit.return_value = sentinel.submission_result
        coordinator = NaturalLanguageCommandCoordinator(
            natural_language_service=Mock(),
            registry_provider=Mock(),
            command_submitter=Mock(),
            result_recorder=Mock(),
            translated_submission=translated_submission,
            generic_crafting_translation=generic_translation,
            generic_crafting_submission=generic_submission,
        )

        translated = coordinator.translate_generic_crafting_defaults(
            sentinel.command,
            input_event=sentinel.event,
            item_resolution_profile=sentinel.profile,
            activation_receipt=sentinel.receipt,
            korean_eligibility_proof=sentinel.proof,
        )
        submitted = coordinator.submit_translated_generic_crafting_defaults(
            sentinel.command,
            sentinel.translation,
            activation_receipt=sentinel.receipt,
            korean_eligibility_proof=sentinel.proof,
        )

        self.assertIs(sentinel.translation_result, translated)
        self.assertIs(sentinel.submission_result, submitted)
        generic_translation.translate.assert_called_once_with(
            sentinel.command,
            input_event=sentinel.event,
            item_resolution_profile=sentinel.profile,
            activation_receipt=sentinel.receipt,
            korean_eligibility_proof=sentinel.proof,
        )
        generic_submission.submit.assert_called_once_with(
            sentinel.command,
            sentinel.translation,
            activation_receipt=sentinel.receipt,
            korean_eligibility_proof=sentinel.proof,
        )
        translated_submission.submit_translation.assert_not_called()
        translated_submission.submit_translated.assert_not_called()

    def test_bound_activation_is_spent_once_before_exactly_one_submission(self):
        adapter = Mock()
        adapter.backend_id = "fabric_chatclef"
        adapter.submit_command.side_effect = lambda request: CommandResultDTO(
            request_id=request.request_id,
            ok=True,
            status=CommandResultStatus.ACCEPTED,
            message="accepted",
            data={"command": request.command},
        )
        extension = MinecraftFabricChatClefExtension(adapter=adapter)
        registry = extension.get_generic_crafting_defaults_activation_registry()
        event = LaviInputEvent(
            text="지도 만들어줘",
            source="lavi_chat_ui",
            provider_id="lavi_chat_ui",
            event_kind="chat_submit",
            final=True,
            event_id="a" * 32,
            fallback_payload="지도 만들어줘",
        )
        proof = object()
        admission = GenericCraftingDefaultsAdmission(
            registry,
            lambda candidate_proof, candidate_event: (
                candidate_proof is proof and candidate_event is event
            ),
        )
        profile = GenericCraftingDefaultsProfile()
        candidate = GenericCraftingDefaultsCandidateDetector(profile).inspect(
            event.text
        )
        receipt = admission.admit(event, proof, candidate).receipt
        self.assertIsNotNone(receipt)
        route_request = (
            MinecraftChatClefSubmissionRequestFactory().build_generic_crafting_defaults(
                event
            )
        )

        translation = extension.translate_generic_crafting_defaults_command(
            route_request,
            input_event=event,
            item_resolution_profile=profile,
            activation_receipt=receipt,
            korean_eligibility_proof=proof,
        )
        first = extension.submit_translated_generic_crafting_defaults_command(
            route_request,
            translation,
            activation_receipt=receipt,
            korean_eligibility_proof=proof,
        )
        replay = extension.submit_translated_generic_crafting_defaults_command(
            route_request,
            translation,
            activation_receipt=receipt,
            korean_eligibility_proof=proof,
        )

        self.assertTrue(first["ok"])
        self.assertFalse(replay["ok"])
        self.assertEqual("generic_crafting_activation_invalid", replay["details"]["reason_code"])
        self.assertEqual(1, adapter.submit_command.call_count)
        self.assertEqual(
            GenericCraftingDefaultsActivationState.SPENT.value,
            registry.state(receipt),
        )

    def test_ordinary_natural_language_behavior_bypasses_feature_lifecycle(self):
        adapter = Mock()
        adapter.backend_id = "fabric_chatclef"
        adapter.submit_command.side_effect = lambda request: CommandResultDTO(
            request_id=request.request_id,
            ok=True,
            status=CommandResultStatus.ACCEPTED,
            message="accepted",
            data={"command": request.command},
        )
        extension = MinecraftFabricChatClefExtension(adapter=adapter)

        result = extension.handle_natural_language_command(
            {
                "request_id": "ordinary-natural-language",
                "text": "다이아몬드 도끼 하나 가져와",
                "source": "direct_typed",
            }
        )

        self.assertTrue(result["ok"])
        request = adapter.submit_command.call_args.args[0]
        self.assertEqual("get diamond_axe 1", request.command)
        self.assertEqual(
            0,
            extension.get_generic_crafting_defaults_activation_registry().record_count,
        )


if __name__ == "__main__":
    unittest.main()
