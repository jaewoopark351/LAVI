#20260907_kpopmodder: Verify terminal crafting feedback output/TTS wiring metadata.
from __future__ import annotations

import unittest

from app_core.composition_core.component_wiring import (
    MinecraftCraftingTerminalResponseWiring,
)
from plugins.Minecraft.fabric.chatclef.response.crafting_lifecycle import (
    CraftingLifecycleTerminalResponse,
)


class CraftingTerminalResponseWiringTests(unittest.TestCase):
    def test_terminal_response_emits_without_history_or_minecraft_prefix(self):
        llm = _LLM()
        extension = _Extension()
        callback = MinecraftCraftingTerminalResponseWiring().wire(
            llm=llm,
            extension=extension,
        )
        response = CraftingLifecycleTerminalResponse(
            text="다이아 곡괭이 다 만들었어",
            event_id="9" * 32,
            presentation_detail_log=(
                '{"command_name":"get","form_kind":"target_count",'
                '"requested_count":1,"target":"diamond_pickaxe"}'
            ),
        )

        callback(response)

        self.assertIs(callback, extension.callback)
        self.assertEqual(1, len(llm.emissions))
        text, values = llm.emissions[0]
        self.assertEqual("다이아 곡괭이 다 만들었어", text)
        self.assertNotIn("[Minecraft]", text)
        self.assertTrue(values["send_output"])
        self.assertFalse(values["send_full_output"])
        self.assertFalse(values["remember_history"])
        self.assertEqual("crafting_lifecycle", values["route_kind"])
        self.assertEqual("crafting_terminal", values["response_kind"])
        self.assertEqual("non_preempting", values["delivery_mode"])
        self.assertEqual("minecraft", values["presentation_metadata"].source_kind)
        self.assertIn(
            '"target":"diamond_pickaxe"',
            values["presentation_metadata"].detail_log,
        )
        self.assertNotIn("diamond_pickaxe", text)
        self.assertTrue(values["send_ui"])


class _LLM:
    def __init__(self):
        self.emissions = []

    def emit_external_response(self, text, **values):
        self.emissions.append((text, values))


class _Extension:
    def __init__(self):
        self.callback = None

    def set_crafting_terminal_response_callback(self, callback):
        self.callback = callback


if __name__ == "__main__":
    unittest.main()
