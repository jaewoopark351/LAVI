#20260908_kpopmodder: Verify STOP terminal delivery is complete before Minecraft input exposure.
from __future__ import annotations

import unittest

from app_core.composition_core.component_wiring import (
    AppComponentWiringError,
    ComponentEventListenerWiring,
)


class ComponentEventListenerWiringTests(unittest.TestCase):
    def test_terminal_and_delivery_topology_precedes_router_and_input_exposure(self):
        trace = []
        wiring = _wiring(trace, stop_callback=lambda _response: None)
        components = _components(trace)

        wiring.wire(
            **components,
            minecraft_fabric_chatclef_extension=object(),
        )

        self.assertEqual(
            [
                "stop_terminal_callback",
                "crafting_terminal_callback",
                "command_start_callback",
                "command_terminal_callback",
                "llm_output_listener",
                "translate_tts_listener",
                "tts_receipts",
                "tts_output_listener",
                "minecraft_input_router",
                "chat_input_listener",
                "final_voice_listener",
                "starcraft_input",
                "screen_input",
            ],
            trace,
        )

    def test_missing_stop_callback_fails_before_sinks_router_or_input_exposure(self):
        trace = []
        wiring = _wiring(trace, stop_callback=None)

        with self.assertRaises(AppComponentWiringError) as caught:
            wiring.wire(
                **_components(trace),
                minecraft_fabric_chatclef_extension=object(),
            )

        self.assertEqual(
            "minecraft_stop_terminal_response_callback",
            caught.exception.stage,
        )
        self.assertEqual(["stop_terminal_callback"], trace)

    def test_absent_extension_keeps_optional_stop_and_exposes_input_after_sinks(self):
        trace = []
        wiring = _wiring(trace, stop_callback=None)

        wiring.wire(
            **_components(trace),
            minecraft_fabric_chatclef_extension=None,
        )

        self.assertLess(
            trace.index("tts_receipts"),
            trace.index("minecraft_input_router"),
        )
        self.assertLess(
            trace.index("minecraft_input_router"),
            trace.index("chat_input_listener"),
        )
        self.assertLess(
            trace.index("chat_input_listener"),
            trace.index("final_voice_listener"),
        )


class _ListenerSource:
    def __init__(self, trace, event_name):
        self._trace = trace
        self._event_name = event_name

    def add_output_event_listener(self, _listener, *args, **kwargs):
        self._trace.append(self._event_name)

    def receive_input(self, _value):
        return None


class _Vtuber(_ListenerSource):
    def receive_song_expression(self, _value):
        return None


class _NamedWiring:
    def __init__(self, trace, event_name, result=None):
        self._trace = trace
        self._event_name = event_name
        self._result = result

    def wire(self, **_values):
        self._trace.append(self._event_name)
        return self._result


class _DirectGameInputWiring:
    def __init__(self, trace):
        self._trace = trace

    def wire_starcraft(self, **_values):
        self._trace.append("starcraft_input")

    def wire_screen(self, **_values):
        self._trace.append("screen_input")


def _wiring(trace, *, stop_callback):
    return ComponentEventListenerWiring(
        _NamedWiring(trace, "minecraft_input_router"),
        direct_game_input_wiring=_DirectGameInputWiring(trace),
        trusted_voice_input_wiring=_NamedWiring(trace, "final_voice_listener"),
        minecraft_stop_terminal_response_wiring=_NamedWiring(
            trace,
            "stop_terminal_callback",
            stop_callback,
        ),
        minecraft_crafting_terminal_response_wiring=_NamedWiring(
            trace,
            "crafting_terminal_callback",
        ),
        minecraft_command_lifecycle_start_response_wiring=_NamedWiring(
            trace,
            "command_start_callback",
        ),
        minecraft_command_lifecycle_terminal_response_wiring=_NamedWiring(
            trace,
            "command_terminal_callback",
        ),
        minecraft_lifecycle_tts_receipt_wiring=_NamedWiring(
            trace,
            "tts_receipts",
        ),
    )


def _components(trace):
    return {
        "input_component": _ListenerSource(trace, "chat_input_listener"),
        "llm": _ListenerSource(trace, "llm_output_listener"),
        "translate": _ListenerSource(trace, "translate_tts_listener"),
        "tts": _ListenerSource(trace, "tts_output_listener"),
        "vtuber": _Vtuber(trace, "unused_vtuber_listener"),
    }


if __name__ == "__main__":
    unittest.main()
