#20260706_kpopmodder: Added this helper to keep ScreenVision observation memory/LLM dispatch orchestration outside the facade.
from core.logger import log_print
from core.event_manager import event_manager, EventType


class ScreenObservationMemoryDispatch:
    #20260706_kpopmodder: Coordinates ScreenVision memory state updates while preserving existing dispatch behavior.
    def __init__(self, owner):
        self.owner = owner

    def record_raw_screen_event(
        self,
        event_type,
        value,
        source="ScreenVision",
        metadata=None,
    ):
        memory_store = self.owner.memory_store
        if memory_store is None:
            return

        if not hasattr(memory_store, "add_raw_event"):
            return

        try:
            memory_store.add_raw_event(
                event_type=event_type,
                value=value,
                source=source,
                metadata=metadata or {},
            )
        except Exception as e:
            log_print(f"[Memory] screen raw event save failed: {e}")

    def remember_screen_observation(self, observation, question, source):
        dispatcher = self.owner._get_observation_dispatcher()
        self.owner.last_screen_observation = observation
        self.owner.last_screen_observation_source = source
        self.owner.last_screen_observation_time = dispatcher.current_time()
        dispatcher.update_memory_store(self.owner.memory_store)
        dispatcher.save_observation(
            observation=observation,
            question=question,
            source=source,
            event_type="screen_observation_silent",
            error_log_message="[Memory] silent screen observation save failed",
            silent=True,
        )

    def send_observation_to_llm(self, observation, question, source):
        payload = self.publish_observation_event(observation, question, source)
        #20260621_kpopmodder: ScreenVision input reaches LLM/TTS but stays out of normal chat history.
        self.owner.send_output(payload)

    #20260710_kpopmodder: Publish accepted auto-watch observations to passive
    # game observers without sending them into the normal chat/LLM stream.
    def publish_observation_event(self, observation, question, source):
        payload = self._build_observation_payload(
            observation=observation,
            question=question,
            source=source,
        )
        self._publish_observation_event(payload)
        return payload

    def _build_observation_payload(self, observation, question, source):
        dispatcher = self.owner._get_observation_dispatcher()
        payload = dispatcher.build_output_payload(
            observation=observation,
            question=question,
            source=source,
        )
        #20260706_kpopmodder: Publish SCREEN_OBSERVATION contract fields for extension event consumers.
        payload["event_type"] = "screen_observation"
        payload["event_name"] = "SCREEN_OBSERVATION"
        payload_metadata = payload.get("metadata")
        if not isinstance(payload_metadata, dict):
            payload_metadata = {}
            payload["metadata"] = payload_metadata
        payload_metadata["question"] = question
        payload_metadata["remember_history"] = bool(payload.get("remember_history"))
        dispatcher.update_memory_store(self.owner.memory_store)
        dispatcher.save_observation(
            observation=observation,
            question=question,
            source=source,
            event_type="screen_observation",
            error_log_message="[Memory] screen observation save failed",
        )
        return payload

    def _publish_observation_event(self, payload):
        try:
            event_manager.trigger(
                EventType.SCREEN_OBSERVATION,
                payload=payload,
            )
        except Exception as e:
            log_print(f"[ScreenVision] event publish failed: {e}")
