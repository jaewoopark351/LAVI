from core.logger import log_print


class LLMMemoryBridge:
    """LLM 응답 파이프라인과 메모리 모듈 사이의 연동을 담당한다."""

    def __init__(
        self,
        memory_context_builder=None,
        memory_command_handler=None,
    ):
        self.memory_context_builder = memory_context_builder
        self.memory_command_handler = memory_command_handler

    def build_augmented_system_prompt(self, system_prompt, query=None):#20260622_kpopmodder: 현재 질문을 기억 검색 질의로 전달한다.
        base_prompt = str(system_prompt or "")

        if self.memory_context_builder is None:
            return base_prompt

        try:
            try:
                memory_context = self.memory_context_builder.build_context_text(
                    query=query,
                )
            except TypeError:
                #20260622_kpopmodder: Keep compatibility with older/custom context builders.
                memory_context = self.memory_context_builder.build_context_text()
        except Exception as e:
            log_print(f"[Memory] context build failed: {e}")
            return base_prompt

        if not memory_context:
            return base_prompt

        return base_prompt + memory_context

    def set_memory_router_ai_callback(self, callback):#20260626_kpopmodder: Let MemoryRouter reuse the active LLM plugin when enabled.
        if self.memory_context_builder is None:
            return
        if not hasattr(self.memory_context_builder, "set_memory_router_ai_callback"):
            return

        try:
            self.memory_context_builder.set_memory_router_ai_callback(callback)
        except Exception as e:
            log_print(f"[Memory] router AI callback setup failed: {e}")

    def try_handle_command(self, message):
        if self.memory_command_handler is None:
            return None

        try:
            return self.memory_command_handler.try_handle(message)
        except Exception as e:
            log_print(f"[Memory] command handling failed: {e}")
            return None

    def get_memory_store(self):
        if self.memory_context_builder is None:
            return None

        return getattr(self.memory_context_builder, "memory_store", None)

    def get_latest_screen_observation(self):#20260622_kpopmodder
        """MemoryStore working memory에서 가장 최근 ScreenVision 관찰 기록을 가져온다."""
        memory_store = self.get_memory_store()
        if memory_store is None:
            return ""

        if not hasattr(memory_store, "get_working_memory"):
            return ""

        try:
            items = memory_store.get_working_memory()
        except Exception as e:
            log_print(f"[Memory] latest screen observation read failed: {e}")
            return ""

        for item in reversed(items or []):
            try:
                key = str(item.get("key", "")).strip()
                value = str(item.get("value", "")).strip()
            except Exception:
                continue

            if key == "screen_observation" and value:
                return value

        return ""

    def record_raw_event(
        self,
        event_type,
        value,
        source="unknown",
        metadata=None,
    ):
        memory_store = self.get_memory_store()

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
            log_print(f"[Memory] raw event save failed: {e}")
