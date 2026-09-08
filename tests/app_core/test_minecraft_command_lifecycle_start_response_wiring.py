#20260907_kpopmodder: Verify direct-GUI START uses typed UI metadata and clean speech text.
from __future__ import annotations

import unittest

from app_core.composition_core.component_wiring import (
    MinecraftCommandLifecycleStartResponseWiring,
    MinecraftCommandLifecycleTerminalResponseWiring,
)
from plugins.Minecraft.fabric.chatclef.response.command_lifecycle import (
    CommandLifecycleCoalescedResponse,
    CommandLifecycleStartResponse,
    CommandLifecycleTerminalResponse,
)


class MinecraftCommandLifecycleStartResponseWiringTests(unittest.TestCase):
    def test_start_is_current_input_with_minecraft_badge_and_no_body_prefix(self):
        extension = _Extension()
        llm = _Llm()
        callback = MinecraftCommandLifecycleStartResponseWiring().wire(
            llm=llm,
            extension=extension,
        )

        result = callback(
            CommandLifecycleStartResponse(
                text="다이아 곡괭이 만들어 줄게",
                event_id="a" * 32,
                command_name="get",
            )
        )

        self.assertTrue(result.output_delivered)
        text, values = llm.calls[0]
        self.assertEqual("다이아 곡괭이 만들어 줄게", text)
        self.assertNotIn("[Minecraft]", text)
        self.assertEqual("current_input", values["delivery_mode"])
        self.assertEqual("command_lifecycle", values["route_kind"])
        self.assertEqual("command_start", values["response_kind"])
        self.assertEqual("Minecraft", values["presentation_metadata"].badge_label)
        self.assertTrue(values["send_ui"])

    def test_coalesced_terminal_is_one_current_input_minecraft_presentation(self):
        extension = _Extension()
        llm = _Llm()
        callback = MinecraftCommandLifecycleStartResponseWiring().wire(
            llm=llm,
            extension=extension,
        )

        result = callback(
            CommandLifecycleCoalescedResponse(
                text="감마를 바꾸지 못했어",
                event_id="b" * 32,
                command_name="gamma",
                presentation_detail_log=(
                    '{"command_name":"gamma","form_kind":"explicit_value",'
                    '"setting_value":"1.0"}'
                ),
            )
        )

        self.assertTrue(result.output_delivered)
        self.assertEqual(1, len(llm.calls))
        text, values = llm.calls[0]
        self.assertEqual("감마를 바꾸지 못했어", text)
        self.assertEqual("current_input", values["delivery_mode"])
        self.assertEqual("command_lifecycle", values["route_kind"])
        self.assertEqual("command_coalesced", values["response_kind"])
        self.assertEqual("Minecraft", values["presentation_metadata"].badge_label)
        self.assertIn(
            '"command_name":"gamma"',
            values["presentation_metadata"].detail_log,
        )
        self.assertNotIn("gamma", text)
        self.assertTrue(values["send_ui"])

    def test_late_terminal_keeps_start_then_non_preempting_sentence_order(self):
        extension = _Extension()
        llm = _Llm()
        MinecraftCommandLifecycleStartResponseWiring().wire(
            llm=llm,
            extension=extension,
        )
        MinecraftCommandLifecycleTerminalResponseWiring().wire(
            llm=llm,
            extension=extension,
        )

        extension.start_callback(
            CommandLifecycleStartResponse(
                text="감마를 바꿀게",
                event_id="c" * 32,
                command_name="gamma",
            )
        )
        extension.terminal_callback(
            CommandLifecycleTerminalResponse(
                text="감마를 바꿨어",
                event_id="c" * 32,
                command_name="gamma",
            )
        )

        self.assertEqual(
            ["감마를 바꿀게", "감마를 바꿨어"],
            [text for text, _values in llm.calls],
        )
        self.assertEqual(
            ["command_start", "command_terminal"],
            [values["response_kind"] for _text, values in llm.calls],
        )
        self.assertEqual(
            ["current_input", "non_preempting"],
            [values["delivery_mode"] for _text, values in llm.calls],
        )


class _Extension:
    def set_command_lifecycle_start_response_callback(self, callback):
        self.start_callback = callback
        self.callback = callback

    def set_command_lifecycle_terminal_response_callback(self, callback):
        self.terminal_callback = callback


class _Llm:
    def __init__(self):
        self.calls = []

    def emit_external_response(self, text, **values):
        self.calls.append((text, values))
        return type("Emission", (), {"output_delivered": True})()


if __name__ == "__main__":
    unittest.main()
