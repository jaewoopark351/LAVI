#20260705_kpopmodder: Added this helper to keep text-only LLM generation outside the LLM facade.


class LLMTextOnlyGenerationHelper:
    #20260705_kpopmodder: Keeps Chess/StarCraft reaction generation free of TTS/listener dispatch.
    def __init__(
        self,
        current_plugin_callback,
        provider_list_callback,
        find_provider_callback,
        load_provider_callback,
        is_generator_plugin_callback,
        log_callback,
    ):
        self.current_plugin_callback = current_plugin_callback
        self.provider_list_callback = provider_list_callback
        self.find_provider_callback = find_provider_callback
        self.load_provider_callback = load_provider_callback
        self.is_generator_plugin_callback = is_generator_plugin_callback
        self.log_callback = log_callback

    def generate(self, message, system_prompt, preferred_provider_name=None):
        #20260705_kpopmodder: Preserve preferred-provider fallback behavior from LLM.generate_text_only.
        current_plugin = self.current_plugin_callback()
        if preferred_provider_name:
            provider = self.find_provider_callback(
                self.provider_list_callback(),
                preferred_provider_name,
            )
            if provider is not None and provider.plugin is not None:
                #20260630_kpopmodder: Lazy-init text-only preferred provider when startup skipped it.
                if (
                    hasattr(provider, "initialized")
                    and not provider.initialized
                    and not self.load_provider_callback(provider.name)
                ):
                    self.log_callback(
                        "[LLM] text-only preferred provider disabled: "
                        f"{preferred_provider_name}"
                    )
                else:
                    current_plugin = provider.plugin
            else:
                self.log_callback(
                    "[LLM] text-only preferred provider unavailable: "
                    f"{preferred_provider_name}"
                )

        if current_plugin is None:
            #20260630_kpopmodder: Provider failure should skip text-only helper instead of crashing.
            self.log_callback("[LLM] text-only generation skipped: no active provider")
            return ""

        result = current_plugin.predict(
            str(message or ""),
            [],
            str(system_prompt or ""),
        )

        if self.is_generator_plugin_callback(current_plugin):
            return self.collect_generator_output(result)

        return str(result or "").strip()

    def collect_generator_output(self, result):
        #20260705_kpopmodder: Support both snapshot-style and delta-style generator chunks.
        output = ""
        for chunk in result:
            if chunk is None:
                continue

            text = str(chunk)
            if not text:
                continue

            if not output:
                output = text
            elif text.startswith(output):
                output = text
            else:
                output += text

        return output.strip()
