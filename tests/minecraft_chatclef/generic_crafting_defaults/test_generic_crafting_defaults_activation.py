#20260905_kpopmodder: Verify one-request Feature-B binding, spend CAS, replay, and cleanup.
from __future__ import annotations

import pickle
import unittest
from concurrent.futures import ThreadPoolExecutor

from input_core.input_event.contracts import LaviInputEvent
from plugins.Minecraft.fabric.chatclef.extension.natural_language.translated_command_request_factory import (
    TranslatedCommandRequestFactory,
)
from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults import (
    GenericCraftingDefaultsActivationRegistry,
    GenericCraftingDefaultsActivationState,
    GenericCraftingDefaultsAdmission,
    GenericCraftingDefaultsCandidateDetector,
    GenericCraftingDefaultsProfile,
)
from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults.activation import (
    GenericCraftingActivationBindingValidator,
    GenericCraftingActivationContextFactory,
    GenericCraftingActivationRecordStore,
    GenericCraftingActivationStateStore,
    GenericCraftingActivationTransitionLifecycle,
)
from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults.activation.context import (
    GenericCraftingActivationContextProjector,
    GenericCraftingActivationEventCandidateValidator,
)
from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults.activation.composition import (
    GenericCraftingActivationCompatibilityInstaller,
    GenericCraftingActivationComponentGraph,
)
from plugins.Minecraft.fabric.chatclef.input.aliases.generic_crafting_defaults.admission import (
    GenericCraftingAdmissionComponentGraph,
    GenericCraftingAdmissionEvaluator,
    GenericCraftingAdmissionRegistryBinding,
    GenericCraftingAdmissionRejectionDecisionFactory,
)
from plugins.Minecraft.fabric.chatclef.input.routing.submission.submission_request_factory import (
    MinecraftChatClefSubmissionRequestFactory,
)
from plugins.Minecraft.fabric.chatclef.intent import ChatClefNaturalLanguageService


class GenericCraftingDefaultsActivationTests(unittest.TestCase):
    def test_registry_facade_keeps_one_focused_activation_object_graph(self):
        registry = GenericCraftingDefaultsActivationRegistry()

        self.assertIsInstance(
            registry._component_graph,
            GenericCraftingActivationComponentGraph,
        )
        self.assertIsInstance(
            registry._component_graph._compatibility_installer,
            GenericCraftingActivationCompatibilityInstaller,
        )
        self.assertIsInstance(
            registry._context_factory,
            GenericCraftingActivationContextFactory,
        )
        self.assertIsInstance(
            registry._binding_validator,
            GenericCraftingActivationBindingValidator,
        )
        self.assertIsInstance(
            registry._state_store,
            GenericCraftingActivationStateStore,
        )
        self.assertIsInstance(
            registry._context_input_validator,
            GenericCraftingActivationEventCandidateValidator,
        )
        self.assertIsInstance(
            registry._context_projector,
            GenericCraftingActivationContextProjector,
        )
        self.assertIsInstance(
            registry._record_store,
            GenericCraftingActivationRecordStore,
        )
        self.assertIsInstance(
            registry._transition_lifecycle,
            GenericCraftingActivationTransitionLifecycle,
        )
        self.assertIs(
            registry._record_store,
            registry._state_store._record_store,
        )
        self.assertIs(
            registry._transition_lifecycle,
            registry._state_store._transition_lifecycle,
        )
        self.assertIs(registry._records, registry._state_store.records)
        self.assertIs(registry._lock, registry._state_store.lock)
        self.assertIs(
            registry._registry_token,
            registry._state_store.registry_token,
        )

    def test_admission_facade_delegates_to_binding_evaluator_and_factory(self):
        registry = GenericCraftingDefaultsActivationRegistry()
        admission = GenericCraftingDefaultsAdmission(
            registry,
            lambda _proof, _event: True,
        )

        self.assertIsInstance(
            admission._component_graph,
            GenericCraftingAdmissionComponentGraph,
        )
        self.assertIsInstance(
            admission._registry_binding,
            GenericCraftingAdmissionRegistryBinding,
        )
        self.assertIsInstance(
            admission._rejection_decision_factory,
            GenericCraftingAdmissionRejectionDecisionFactory,
        )
        self.assertIsInstance(
            admission._evaluator,
            GenericCraftingAdmissionEvaluator,
        )
        self.assertIs(admission, registry._admission_owner)

    def test_projection_binds_then_only_one_concurrent_spend_wins(self):
        fixture = _ActivationFixture("다락문 만들어줘", "1" * 32)
        receipt, translation, request = fixture.issue_bind_and_build_request()

        with ThreadPoolExecutor(max_workers=8) as pool:
            results = list(
                pool.map(
                    lambda _index: fixture.registry.spend(
                        receipt,
                        fixture.proof,
                        request,
                        translation,
                    ),
                    range(32),
                )
            )

        self.assertEqual(1, results.count(True))
        self.assertEqual(31, results.count(False))
        self.assertEqual(
            GenericCraftingDefaultsActivationState.SPENT.value,
            fixture.registry.state(receipt),
        )
        self.assertFalse(
            fixture.registry.spend(
                receipt,
                fixture.proof,
                request,
                translation,
            )
        )

        fixture.registry.close_dispatch(fixture.proof)
        self.assertEqual("INVALID", fixture.registry.state(receipt))
        self.assertEqual(0, fixture.registry.record_count)

    def test_receipt_is_process_local_and_non_serializable(self):
        fixture = _ActivationFixture("지도 만들어줘", "2" * 32)
        receipt = fixture.issue()

        with self.assertRaises(TypeError):
            pickle.dumps(receipt)
        with self.assertRaises(AttributeError):
            receipt._quantity = 99
        self.assertEqual(
            "GenericCraftingDefaultsActivationReceipt(<opaque>)",
            repr(receipt),
        )

    def test_cross_event_projection_and_request_swaps_do_not_spend(self):
        first = _ActivationFixture("압력판 2개 만들어줘", "3" * 32)
        second = _ActivationFixture(
            "버튼 3개 만들어줘",
            "4" * 32,
            registry=first.registry,
            admission=first.admission,
            accepted_proofs=first.accepted_proofs,
        )
        first.accepted_proofs[first.proof] = first.event
        first.accepted_proofs[second.proof] = second.event

        first_receipt, first_translation, first_request = (
            first.issue_bind_and_build_request()
        )
        second_receipt, second_translation, second_request = (
            second.issue_bind_and_build_request()
        )

        self.assertFalse(
            first.registry.spend(
                first_receipt,
                first.proof,
                second_request,
                second_translation,
            )
        )
        self.assertFalse(
            first.registry.spend(
                second_receipt,
                second.proof,
                first_request,
                first_translation,
            )
        )
        self.assertEqual(
            GenericCraftingDefaultsActivationState.TRANSLATION_BOUND.value,
            first.registry.state(first_receipt),
        )
        self.assertEqual(
            GenericCraftingDefaultsActivationState.TRANSLATION_BOUND.value,
            first.registry.state(second_receipt),
        )

    def test_capacity_is_fail_closed_and_dispatch_close_reclaims_it(self):
        accepted: dict[object, object] = {}
        registry = GenericCraftingDefaultsActivationRegistry(capacity=1)
        admission = GenericCraftingDefaultsAdmission(
            registry,
            lambda proof, event: accepted.get(proof) is event,
        )
        first = _ActivationFixture(
            "발판 만들어줘",
            "5" * 32,
            registry=registry,
            admission=admission,
            accepted_proofs=accepted,
        )
        second = _ActivationFixture(
            "버튼 만들어줘",
            "6" * 32,
            registry=registry,
            admission=admission,
            accepted_proofs=accepted,
        )
        accepted[first.proof] = first.event
        accepted[second.proof] = second.event

        first_receipt = first.issue()
        rejected = second.admission.admit(
            second.event,
            second.proof,
            second.candidate,
        )

        self.assertFalse(rejected.admitted)
        self.assertTrue(rejected.feature_owned)
        self.assertEqual(
            "generic_crafting_activation_capacity_exhausted",
            rejected.reason_code,
        )
        registry.abandon_if_live(first_receipt)
        registry.close_dispatch(first.proof)
        self.assertTrue(
            second.admission.admit(
                second.event,
                second.proof,
                second.candidate,
            ).admitted
        )

    def test_forged_proof_does_not_create_an_activation_record(self):
        fixture = _ActivationFixture("다락문 만들어줘", "7" * 32)
        decision = fixture.admission.admit(
            fixture.event,
            object(),
            fixture.candidate,
        )

        self.assertFalse(decision.admitted)
        self.assertFalse(decision.feature_owned)
        self.assertEqual(0, fixture.registry.record_count)


class _ActivationFixture:
    def __init__(
        self,
        text: str,
        event_id: str,
        *,
        registry=None,
        admission=None,
        accepted_proofs=None,
    ):
        self.profile = GenericCraftingDefaultsProfile()
        self.detector = GenericCraftingDefaultsCandidateDetector(self.profile)
        self.event = LaviInputEvent(
            text=text,
            source="lavi_chat_ui",
            provider_id="lavi_chat_ui",
            event_kind="chat_submit",
            final=True,
            event_id=event_id,
            fallback_payload=text,
        )
        self.proof = object()
        self.accepted_proofs = (
            accepted_proofs if accepted_proofs is not None else {}
        )
        self.accepted_proofs[self.proof] = self.event
        self.registry = registry or GenericCraftingDefaultsActivationRegistry()
        self.admission = admission or GenericCraftingDefaultsAdmission(
            self.registry,
            lambda proof, event: self.accepted_proofs.get(proof) is event,
        )
        self.candidate = self.detector.inspect(text)

    def issue(self):
        decision = self.admission.admit(
            self.event,
            self.proof,
            self.candidate,
        )
        if not decision.admitted or decision.receipt is None:
            raise AssertionError(decision)
        return decision.receipt

    def issue_bind_and_build_request(self):
        receipt = self.issue()
        translation = ChatClefNaturalLanguageService().translate_with_item_resolution_profile(
            receipt.translation_input_text,
            self.profile,
        )
        if not self.registry.bind_translation(
            receipt,
            self.proof,
            self.event,
            translation,
        ):
            raise AssertionError("translation did not bind")
        route_request = (
            MinecraftChatClefSubmissionRequestFactory().build_generic_crafting_defaults(
                self.event
            )
        )
        request = TranslatedCommandRequestFactory().build(
            route_request,
            translation,
            receipt.translation_input_text,
        )
        return receipt, translation, request


if __name__ == "__main__":
    unittest.main()
