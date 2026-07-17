#20260717_kpopmodder: Isolates Hybrid OpenAI memory router provider adapter.
from memory_core.memory_router import MemoryRouter
from memory_core.openai_memory_router_provider import OpenAIMemoryRouterProvider


class MemoryRouterProvider_OpenAI:
    def __init__(self, settings, log_print, router=None):
        self.settings = settings
        self.log_print = log_print
        if router is not None:
            self.router = router
            return

        provider = OpenAIMemoryRouterProvider(
            api_key=settings.openai_api_key,
            model_name=settings.memory_router_model_name,
            temperature=0.0,
        )
        self.router = MemoryRouter(
            enabled=settings.memory_router_enabled,
            provider="openai",
            timeout_sec=settings.memory_router_timeout_sec,
            max_items=5,
            fallback_to_keyword=True,
            ai_response_callback=provider,
        )

    def route(self, message):
        #20260626_kpopmodder: This provider sends only the current utterance to OpenAI, never raw_events or long_term_memory dumps.
        try:
            return self.router.route(message)
        except Exception as e:
            self.log_print(
                f"[Hybrid_OpenAI_LLM] memory router failed: {type(e).__name__}"
            )
            return None
