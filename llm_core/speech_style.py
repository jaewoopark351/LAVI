#20260705_kpopmodder: Added this helper to keep LLM speech-style prompt assembly outside the facade.


class LLMSpeechStyleHelper:
    #20260705_kpopmodder: Keeps speech-style normalization and prompt composition small and testable.
    def __init__(
        self,
        default_mode,
        labels,
        prompts,
        runtime_ability_prompt,
    ):
        self.default_mode = default_mode
        self.labels = labels
        self.prompts = prompts
        self.runtime_ability_prompt = runtime_ability_prompt

    def normalize(self, value):
        #20260705_kpopmodder: Accept old English aliases and Korean UI labels without changing config values.
        text = str(value or "").strip()
        text_lower = text.lower()
        aliases = {
            "formal": "polite",
            "polite": "polite",
            "honorific": "polite",
            "casual": "casual",
            "banmal": "casual",
        }
        if text in self.labels:
            return text
        for mode, label in self.labels.items():
            if text == label:
                return mode
        return aliases.get(text_lower, self.default_mode)

    def label_for(self, value):
        #20260705_kpopmodder: Convert stored ASCII config mode back to the existing UI label.
        mode = self.normalize(value)
        return self.labels.get(mode, self.labels[self.default_mode])

    def build_prompt(self, system_prompt, speech_style_mode):
        #20260705_kpopmodder: Preserve original prompt section order: base, runtime abilities, speech style.
        if system_prompt is None:
            base_prompt = ""
        else:
            base_prompt = str(system_prompt or "")

        mode = self.normalize(speech_style_mode)
        style_prompt = self.prompts.get(mode, "")
        runtime_prompt = str(self.runtime_ability_prompt or "").strip()
        base_prompt = base_prompt.rstrip()

        prompt_sections = []
        if base_prompt:
            prompt_sections.append(base_prompt)
        if runtime_prompt:
            prompt_sections.append("[Runtime Abilities]\n" + runtime_prompt)
        if style_prompt:
            prompt_sections.append("[Speech Style]\n" + style_prompt)

        return "\n\n".join(prompt_sections)
