#20260907_kpopmodder: Verify generalized terminals use typed non-preempting delivery.
from __future__ import annotations

import unittest

from app_core.composition_core.component_wiring import (
    MinecraftCommandLifecycleTerminalResponseWiring,
)
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle import (
    CommandLifecycleTerminalResponse,
)
from plugins.Minecraft.fabric.chatclef.response.crafting_lifecycle import (
    CraftingLifecycleTerminalResponse,
)


class MinecraftCommandLifecycleTerminalResponseWiringTests(unittest.TestCase):
    def test_generic_terminal_uses_ui_badge_and_non_preempting_delivery(self):
        llm = _LLM()
        extension = _Extension()
        callback = MinecraftCommandLifecycleTerminalResponseWiring().wire(
            llm=llm,
            extension=extension,
        )
        response = CommandLifecycleTerminalResponse(
            text="철 10개 다 보관했어",
            event_id="4" * 32,
            command_name="deposit",
            presentation_detail_log=(
                '{"command_name":"deposit","form_kind":"item_count",'
                '"requested_count":10,"target":"iron_ingot"}'
            ),
        )

        callback(response)

        self.assertIs(callback, extension.callback)
        text, values = llm.emissions[0]
        self.assertEqual("철 10개 다 보관했어", text)
        self.assertNotIn("[Minecraft]", text)
        self.assertEqual("command_lifecycle", values["route_kind"])
        self.assertEqual("command_terminal", values["response_kind"])
        self.assertEqual("non_preempting", values["delivery_mode"])
        self.assertEqual(
            "minecraft",
            values["presentation_metadata"].source_kind,
        )
        self.assertIn(
            '"target":"iron_ingot"',
            values["presentation_metadata"].detail_log,
        )
        self.assertNotIn("iron_ingot", text)
        self.assertTrue(values["send_ui"])

    def test_generic_callback_preserves_exact_crafting_compatibility_response(self):
        llm = _LLM()
        extension = _Extension()
        callback = MinecraftCommandLifecycleTerminalResponseWiring().wire(
            llm=llm,
            extension=extension,
        )

        callback(
            CraftingLifecycleTerminalResponse(
                text="다이아 곡괭이 다 만들었어",
                event_id="5" * 32,
            )
        )

        text, values = llm.emissions[0]
        self.assertEqual("다이아 곡괭이 다 만들었어", text)
        self.assertEqual("crafting_lifecycle", values["route_kind"])
        self.assertEqual("crafting_terminal", values["response_kind"])


class _LLM:
    def __init__(self):
        self.emissions = []

    def emit_external_response(self, text, **values):
        self.emissions.append((text, values))


class _Extension:
    def __init__(self):
        self.callback = None

    def set_command_lifecycle_terminal_response_callback(self, callback):
        self.callback = callback


if __name__ == "__main__":
    unittest.main()
