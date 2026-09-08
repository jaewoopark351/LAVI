#20260907_kpopmodder: Verify trusted Chat start/progress feedback without LLM recall.
from __future__ import annotations

import unittest
from types import SimpleNamespace

from app_core.composition_core.component_wiring import MinecraftInputRouterWiring
from input_core.input_event.adapters import (
    LocalChatInputEventAdapter,
    ProviderBoundInputEventAdapter,
)
from input_core.input_event.normalization import LaviInputEventNormalizer
from input_core.input_event.provenance import InputProviderSourceResolver
from input_core.input_event.provenance.trusted_user_ingress import (
    TrustedIngressProducerRegistrarFactory,
    TrustedUserInputIngressClaimRegistry,
)
from llm_core.chat_input import TrustedLocalChatInputDispatchCoordinator
from llm_core.event_dispatcher import LLMEventDispatcher
from llm_core.input_queue_worker import LLMInputQueueWorker
from llm_core.llm_component import LLM
from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO
from plugins.Minecraft.common.dto.status_snapshot_dto import StatusSnapshotDTO
from plugins.Minecraft.common.protocol.bridge_lifecycle_state import (
    BridgeLifecycleState,
)
from plugins.Minecraft.common.protocol.command_result_status import (
    CommandResultStatus,
)
from plugins.Minecraft.fabric.chatclef.extension import (
    MinecraftFabricChatClefExtension,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.crafting import (
    CraftingFeedbackStatusSnapshot,
)


class NaturalCraftingFeedbackIntegrationTests(unittest.TestCase):
    def test_exact_craft_starts_naturally_then_status_submits_zero_commands(self):
        adapter = _CraftingFeedbackAdapter()
        extension = MinecraftFabricChatClefExtension(adapter=adapter)
        llm, pipeline, outputs = _llm_harness(extension)
        chat = _chat_coordinator(llm)

        start = list(chat.dispatch("다이아 곡괭이 만들어줘", [], "system"))
        request_count = len(adapter.requests)
        status = list(chat.dispatch("마크 지금 뭐 만들고 있어?", [], "system"))

        self.assertEqual(
            ["다이아 곡괭이 만들어 줄게"],
            [message.content for message in start],
        )
        self.assertEqual(
            ["다이아 곡괭이 만드는 중이야"],
            [message.content for message in status],
        )
        self.assertEqual("Minecraft", start[0].metadata["title"])
        self.assertEqual("Minecraft", status[0].metadata["title"])
        self.assertEqual(1, request_count)
        self.assertEqual(request_count, len(adapter.requests))
        self.assertEqual("get diamond_pickaxe 1", adapter.requests[0].command)
        self.assertEqual(
            ["다이아 곡괭이 만들어 줄게", "다이아 곡괭이 만드는 중이야"],
            [payload["text"] for payload in outputs],
        )
        self.assertEqual(0, pipeline.provider_calls)

    def test_exact_final_voice_craft_uses_same_start_admission_and_renderer(self):
        adapter = _CraftingFeedbackAdapter()
        extension = MinecraftFabricChatClefExtension(adapter=adapter)
        llm, pipeline, outputs = _llm_harness(extension)
        voice_adapter = ProviderBoundInputEventAdapter(
            provider=_voice_provider(),
            output_callback=lambda _event: None,
            source_resolver=InputProviderSourceResolver(),
        )
        voice = llm.create_trusted_voice_input_final_enqueue_coordinator(
            voice_adapter
        )

        receipt = voice.enqueue("다이아 곡괭이 만들어줘")
        queued = llm.input_queue_worker.input_queue.get_nowait()
        yielded = list(llm.accept_queued_input(queued, [], "system"))

        self.assertIsNotNone(receipt)
        self.assertEqual(["다이아 곡괭이 만들어 줄게"], yielded)
        self.assertEqual(["다이아 곡괭이 만들어 줄게"], [item["text"] for item in outputs])
        self.assertEqual(1, adapter.crafting_reservations)
        self.assertEqual(1, len(adapter.requests))
        request = adapter.requests[0]
        self.assertEqual("get diamond_pickaxe 1", request.command)
        self.assertEqual("voice_input_final", request.source)
        self.assertEqual("VoiceInput", request.metadata["input_event"]["provider_id"])
        self.assertEqual(
            "final_transcript",
            request.metadata["input_event"]["event_kind"],
        )
        self.assertTrue(request.metadata["input_event"]["final"])
        self.assertEqual(0, pipeline.provider_calls)

    def test_same_target_acquisition_verb_keeps_existing_non_lifecycle_response(self):
        adapter = _CraftingFeedbackAdapter()
        extension = MinecraftFabricChatClefExtension(adapter=adapter)
        llm, pipeline, _outputs = _llm_harness(extension)

        response = list(
            _chat_coordinator(llm).dispatch(
                "다이아 곡괭이 가져와줘",
                [],
                "system",
            )
        )

        self.assertEqual(1, len(adapter.requests))
        self.assertEqual(0, adapter.crafting_reservations)
        self.assertNotEqual(["다이아 곡괭이 만들어 줄게"], response)
        self.assertEqual(0, pipeline.provider_calls)


class _CraftingFeedbackAdapter:
    backend_id = "fabric_chatclef"

    def __init__(self):
        self.requests = []
        self.crafting_reservations = 0
        self._grant = None
        self._terminal_callback = None

    def submit_command(self, request):
        self.requests.append(request)
        return CommandResultDTO(
            request_id=request.request_id,
            ok=True,
            status=CommandResultStatus.ACCEPTED,
            message="accepted",
            data={"command": request.command},
        )

    def get_status(self):
        return StatusSnapshotDTO(
            backend_id=self.backend_id,
            enabled=True,
            connected=True,
            lifecycle_state=BridgeLifecycleState.CONNECTED,
            detail="connected",
            details={
                "commands": {
                    "active_request_id": None,
                    "active_command": None,
                    "last_result": None,
                }
            },
        )

    def reserve_crafting_feedback(self, grant):
        if not grant.reserve():
            return False
        self.crafting_reservations += 1
        self._grant = grant
        return True

    def abandon_crafting_feedback(self, grant):
        if self._grant is grant:
            grant.abandon_if_reserved()
        return True

    def claim_crafting_feedback_start(self, grant, result):
        if self._grant is not grant or result.get("ok") is not True:
            return False
        return grant.bind()

    def inspect_command_feedback_status(self, _query):
        descriptor = self._grant.descriptor if self._grant is not None else None
        return CraftingFeedbackStatusSnapshot(
            state=CraftingFeedbackStatusSnapshot.RUNNING,
            descriptor=descriptor,
            command_name=(
                descriptor.command_name if descriptor is not None else None
            ),
            requested_family=(
                descriptor.requested_family if descriptor is not None else None
            ),
            target_item="diamond_pickaxe",
            requested_count=1,
            result_reason="dispatch_started",
            owner_present=descriptor is not None,
            terminal_state="unclaimed" if descriptor is not None else "none",
        )

    def inspect_crafting_feedback_status(self, _target_item):
        return CraftingFeedbackStatusSnapshot(
            state=CraftingFeedbackStatusSnapshot.RUNNING,
            target_item="diamond_pickaxe",
            requested_count=1,
            result_reason="dispatch_started",
        )

    def set_crafting_terminal_response_callback(self, callback):
        self._terminal_callback = callback


def _llm_harness(extension):
    llm = LLM.__new__(LLM)
    pipeline = _Pipeline()
    outputs = []
    llm.response_pipeline = pipeline
    llm.event_dispatcher = LLMEventDispatcher()
    llm.event_dispatcher.add_output_event_listener(outputs.append)
    llm.input_event_normalizer = LaviInputEventNormalizer()
    llm.speech_style_helper = SimpleNamespace(
        build_prompt=lambda prompt, _mode: prompt,
    )
    llm.speech_style_mode = "polite"
    llm.history = []
    registry = TrustedUserInputIngressClaimRegistry()
    llm.trusted_user_input_ingress_claim_registry = registry
    llm.trusted_ingress_producer_registrar_factory = (
        TrustedIngressProducerRegistrarFactory(registry)
    )
    llm.set_input_router = lambda router: setattr(llm, "input_router", router)
    MinecraftInputRouterWiring().wire(llm=llm, extension=extension)
    llm.input_queue_worker = LLMInputQueueWorker(
        response_callback=llm.predict_wrapper,
        history_callback=lambda: llm.history,
        system_prompt_callback=lambda: "system",
        queue_updated_callback=lambda: None,
        queued_response_callback=llm.accept_queued_input,
    )
    llm.input_queue_worker.process_input_queue = lambda: None
    return llm, pipeline, outputs


def _chat_coordinator(llm):
    return TrustedLocalChatInputDispatchCoordinator(
        input_event_adapter=LocalChatInputEventAdapter(),
        producer_registrar_factory=llm.trusted_ingress_producer_registrar_factory,
        registered_dispatch_callback=llm.accept_registered_input,
        fallback_predict_callback=llm.predict_wrapper,
    )


def _voice_provider():
    descriptor = SimpleNamespace(id="VoiceInput")
    return SimpleNamespace(handle=SimpleNamespace(descriptor=descriptor))


class _Pipeline:
    def __init__(self):
        self.response_generation = 0
        self.provider_calls = 0

    def begin_response_generation(self):
        self.response_generation += 1
        return self.response_generation

    def build_stream_payload(self, text, generation):
        return {"text": text, "response_generation": generation}

    def predict(self, payload, _history, _system_prompt):
        self.provider_calls += 1
        yield f"LLM:{payload}"


if __name__ == "__main__":
    unittest.main()
