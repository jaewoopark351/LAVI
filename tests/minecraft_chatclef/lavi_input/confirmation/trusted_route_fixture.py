#20260915_kpopmodder: Reuse the production trusted producer graph with persistent adapters for multi-turn confirmation.
from input_core.input_event.adapters import ProviderBoundInputEventAdapter
from input_core.input_event.provenance import InputProviderSourceResolver
from plugins.Minecraft.fabric.chatclef.extension import MinecraftFabricChatClefExtension
from tests.minecraft_chatclef.lavi_input.test_trusted_korean_chat_voice_integration import (
    _llm_harness,
    _chat_coordinator,
    _voice_provider,
)
from .recording_adapter_fixture import ConfirmationRecordingAdapter


class TrustedConfirmationRouteFixture:
    def __init__(self, catalogue=None, adapter=None):
        self.adapter = adapter or ConfirmationRecordingAdapter()
        self.adapter.catalogue = catalogue
        # Default extension composition must pass its adapter-owned catalogue provider.
        self.extension = MinecraftFabricChatClefExtension(adapter=self.adapter)
        self.service = self.extension.natural_language_service
        self.llm, self.pipeline, self.outputs = _llm_harness(self.extension)
        self.chat = _chat_coordinator(self.llm)
        voice = ProviderBoundInputEventAdapter(
            provider=_voice_provider(),
            output_callback=lambda _: None,
            source_resolver=InputProviderSourceResolver(),
        )
        self.voice = self.llm.create_trusted_voice_input_final_enqueue_coordinator(
            voice
        )

    def dispatch(self, text, *, voice=False):
        if not voice:
            return list(self.chat.dispatch(text, [], "system")), None
        assert self.voice.enqueue(text) is not None
        queued = self.llm.input_queue_worker.input_queue.get_nowait()
        return list(self.llm.accept_queued_input(queued, [], "system")), queued
