from .routing import RouteDecision


class RouterFirstHybridEngine:
    def __init__(
        self,
        command_router,
        route_provider,
        memory_router_provider,
        openai_provider,
        local_provider,
        log_print,
    ):
        self.command_router = command_router
        self.route_provider = route_provider
        self.memory_router_provider = memory_router_provider
        self.openai_provider = openai_provider
        self.local_provider = local_provider
        self.log_print = log_print

    def stream(self, message, history, system_prompt):
        decision = self._decide_route(message, history, system_prompt)
        self.log_print(
            "[Hybrid_OpenAI_LLM] route decision: "
            f"route={decision.route} reason={decision.reason} "
            f"forced={decision.forced} fallback_used={decision.fallback_used}"
        )

        if decision.route == "openai_chat":
            try:
                yield from self.openai_provider.stream(
                    message,
                    history,
                    system_prompt,
                )
                return
            except Exception as e:
                if not self._local_enabled():
                    self.log_print(
                        "[Hybrid_OpenAI_LLM] OpenAI failed; "
                        f"local_light disabled: {type(e).__name__}"
                    )
                    raise
                self.log_print(
                    "[Hybrid_OpenAI_LLM] OpenAI failed; "
                    f"falling back to local_light: {type(e).__name__}"
                )

        if not self._local_enabled():
            raise RuntimeError("local_light provider is disabled")

        yield from self.local_provider.stream(
            message,
            history,
            system_prompt,
        )

    def _decide_route(self, message, history, system_prompt):
        forced = self.command_router.route(message)
        if forced is not None:
            return forced

        if not self._local_enabled():
            #20260626_kpopmodder: OpenAI-only mode skips answer and memory routing here.
            # Memory recall is handled by memory_core before the LLM provider is called.
            return RouteDecision(
                route="openai_chat",
                reason="openai_only_local_disabled",
                fallback_used=True,
            )

        memory_decision = self.memory_router_provider.route(message)
        if getattr(memory_decision, "need_memory", False):
            return RouteDecision(
                route="openai_chat",
                reason="memory_router_requires_openai",
            )

        route_decision = self.route_provider.route(
            message,
            history=history,
            system_prompt=system_prompt,
        )
        if route_decision.route not in {"openai_chat", "local_light"}:
            return RouteDecision(
                route="openai_chat",
                reason="invalid_route_conservative",
                fallback_used=True,
            )
        return route_decision

    def _local_enabled(self):
        return bool(getattr(self.local_provider, "enabled", True))
