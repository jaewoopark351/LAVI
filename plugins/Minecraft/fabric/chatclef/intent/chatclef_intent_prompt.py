#20260803_kpopmodder: Added constrained LLM prompt text without execution authority.
from __future__ import annotations

CHATCLEF_INTENT_PROMPT = """
Return only strict JSON for a Korean Minecraft command intent.
Allowed fields are: intent_type, quantity, item_phrase, food_units, x, y, z,
player_name, original_text, source, confidence, language, slots.
Do not include command, chatclef_command, target, minecraft_id, dsl, or executable.
Allowed intent_type values are get_item, food, meat, goto, follow, idle, stop, unknown.
The JSON only describes intent. It must not execute ChatClef commands.
""".strip()
