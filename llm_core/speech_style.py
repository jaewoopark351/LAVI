#20260705_kpopmodder: Added this helper to keep LLM speech-style prompt assembly outside the facade.
import re


SPEECH_STYLE_DEFAULT_MODE = "polite"
SPEECH_STYLE_LABELS = {
    "polite": "존댓말",
    "casual": "반말",
}

GAME_REACTION_SPEECH_STYLE_PROMPTS = {
    "polite": (
        "Speech style: use natural polite Korean 존댓말. "
        "Do not use casual banmal endings such as '-했어', '-네', '-야', or '-해'. "
        "Keep game terms, move notation, unit names, and player names unchanged. "
        "If the base prompt gives casual examples, preserve the same game intent "
        "but phrase the final answer politely."
    ),
    "casual": (
        "Speech style: use natural casual Korean banmal 반말. "
        "Do not use polite endings such as '-요' or '-습니다'. "
        "Keep game terms, move notation, unit names, and player names unchanged."
    ),
}

_POLITE_REACTION_REPLACEMENTS = (
    (r"내가\s+", "제가 "),
    (r"내\s+", "제 "),
    (r"개못하네", "많이 못하시네요"),
    (r"수 읽는 척은 그만해(?!요)", "수 읽는 척은 그만하세요"),
    (r"그만해(?!요)", "그만하세요"),
    (r"못 막네", "못 막으시네요"),
    (r"허접(?!하시네요)", "허접하시네요"),
    (r"조심해야겠다", "조심해야겠어요"),
    (r"해야 해(?!요)", "해야 해요"),
    (r"물렸네", "물렸네요"),
    (r"됐네", "됐네요"),
    (r"떴어(?!요)", "떴어요"),
    (r"왔어(?!요)", "왔어요"),
    (r"갔어(?!요)", "갔어요"),
    (r"졌어(?!요)", "졌어요"),
    (r"이겼어(?!요)", "이겼어요"),
    (r"끝났어(?!요)", "끝났어요"),
    (r"늘었어(?!요)", "늘었어요"),
    (r"막혔어(?!요)", "막혔어요"),
    (r"잃었어(?!요)", "잃었어요"),
    (r"있어(?!요)", "있어요"),
    (r"없어(?!요)", "없어요"),
    (r"보여(?!요)", "보여요"),
    (r"중이야", "중이에요"),
    (r"([가-힣])했어(?!요)", r"\1했어요"),
    (r"([가-힣])됐어(?!요)", r"\1됐어요"),
    (r"([가-힣])었어(?!요)", r"\1었어요"),
    (r"([가-힣])았어(?!요)", r"\1았어요"),
)

_CASUAL_REACTION_REPLACEMENTS = (
    (r"제가\s+", "내가 "),
    (r"제\s+", "내 "),
    (r"많이 못하시네요", "개못하네"),
    (r"수 읽는 척은 그만하세요", "수 읽는 척은 그만해"),
    (r"그만하세요", "그만해"),
    (r"못 막으시네요", "못 막네"),
    (r"허접하시네요", "허접"),
    (r"조심해야겠어요", "조심해야겠다"),
    (r"해야 해요", "해야 해"),
    (r"물렸네요", "물렸네"),
    (r"됐네요", "됐네"),
    (r"했어요", "했어"),
    (r"됐어요", "됐어"),
    (r"되었어요", "되었어"),
    (r"왔어요", "왔어"),
    (r"갔어요", "갔어"),
    (r"졌어요", "졌어"),
    (r"이겼어요", "이겼어"),
    (r"끝났어요", "끝났어"),
    (r"늘었어요", "늘었어"),
    (r"막혔어요", "막혔어"),
    (r"잃었어요", "잃었어"),
    (r"있어요", "있어"),
    (r"없어요", "없어"),
    (r"보여요", "보여"),
    (r"중이에요", "중이야"),
    (r"할게요", "할게"),
    (r"볼게요", "볼게"),
    (r"이에요", "이야"),
    (r"예요", "야"),
    (r"입니다", "야"),
    (r"합니다", "해"),
    (r"해요", "해"),
)


def normalize_speech_style_mode(
    value,
    default_mode=SPEECH_STYLE_DEFAULT_MODE,
    labels=None,
):
    #20260718_kpopmodder: Share the same UI label normalization with game reactions.
    labels = labels or SPEECH_STYLE_LABELS
    text = str(value or "").strip()
    text_lower = text.lower()
    aliases = {
        "formal": "polite",
        "polite": "polite",
        "honorific": "polite",
        "casual": "casual",
        "banmal": "casual",
    }
    if text in labels:
        return text
    for mode, label in labels.items():
        if text == label:
            return mode
    if text_lower in aliases:
        return aliases[text_lower]
    if default_mode is None:
        return ""
    return normalize_speech_style_mode(
        default_mode,
        default_mode=SPEECH_STYLE_DEFAULT_MODE,
        labels=labels,
    )


def resolve_game_reaction_speech_style_mode(source=None, default_mode=None):
    #20260718_kpopmodder: Game plugins pass the live LLM object so UI changes apply immediately.
    if isinstance(source, str):
        return normalize_speech_style_mode(source, default_mode=default_mode)
    if isinstance(source, dict):
        value = source.get("speech_style_mode") or source.get("speech_style")
        if isinstance(value, str) and value.strip():
            return normalize_speech_style_mode(value, default_mode=default_mode)
    if source is not None:
        for attr_name in ("speech_style_mode", "speech_style"):
            value = getattr(source, attr_name, None)
            if isinstance(value, str) and value.strip():
                return normalize_speech_style_mode(value, default_mode=default_mode)
        getter = getattr(source, "get_speech_style_mode", None)
        if callable(getter):
            try:
                value = getter()
            except Exception:
                value = None
            if isinstance(value, str) and value.strip():
                return normalize_speech_style_mode(value, default_mode=default_mode)
    return normalize_speech_style_mode(None, default_mode=default_mode)


def build_game_reaction_speech_style_prompt(source=None, default_mode="casual"):
    mode = resolve_game_reaction_speech_style_mode(
        source,
        default_mode=default_mode,
    )
    return GAME_REACTION_SPEECH_STYLE_PROMPTS.get(mode, "")


def build_game_reaction_system_prompt(
    base_prompt,
    speech_style_source=None,
    default_mode="casual",
):
    #20260718_kpopmodder: Dedicated game prompts keep their policy text and share only speech style.
    prompt = str(base_prompt or "").rstrip()
    style_prompt = build_game_reaction_speech_style_prompt(
        speech_style_source,
        default_mode=default_mode,
    )
    sections = []
    if prompt:
        sections.append(prompt)
    if style_prompt:
        sections.append("[Speech Style]\n" + style_prompt)
    return "\n\n".join(sections)


def apply_game_reaction_speech_style(text, speech_style_source=None, default_mode=None):
    message = str(text or "").strip()
    if not message:
        return ""
    mode = resolve_game_reaction_speech_style_mode(
        speech_style_source,
        default_mode=default_mode,
    )
    if mode == "polite":
        return _apply_reaction_replacements(message, _POLITE_REACTION_REPLACEMENTS)
    if mode == "casual":
        return _apply_reaction_replacements(message, _CASUAL_REACTION_REPLACEMENTS)
    return message


def _apply_reaction_replacements(text, replacements):
    result = str(text or "")
    for pattern, replacement in replacements:
        result = re.sub(pattern, replacement, result)
    return result


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
        return normalize_speech_style_mode(
            value,
            default_mode=self.default_mode,
            labels=self.labels,
        )

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
