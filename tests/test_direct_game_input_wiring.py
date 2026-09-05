#20260905_kpopmodder: Verify focused StarCraft and ScreenVision direct-input wiring.
from __future__ import annotations

import unittest

from app_core.composition_core.component_wiring.direct_game_input_wiring import (
    DirectGameInputWiring,
)


class DirectGameInputWiringTests(unittest.TestCase):
    def test_starcraft_wiring_reuses_adapter_and_registers_coach_output(self):
        llm = _Llm()
        starcraft = _StarCraft()
        wiring = DirectGameInputWiring()

        wiring.wire_starcraft(llm=llm, starcraft_plugin=starcraft)
        wiring.wire_starcraft(llm=llm, starcraft_plugin=starcraft)

        self.assertEqual(2, len(starcraft.output_listeners))
        self.assertIs(
            starcraft.output_listeners[0],
            starcraft.output_listeners[1],
        )
        self.assertEqual(
            [
                (starcraft.receive_coach_response, True),
                (starcraft.receive_coach_response, True),
            ],
            llm.output_listener_calls,
        )

        starcraft.output_listeners[0]("observation")
        self.assertEqual(1, len(llm.received))
        event = llm.received[0]
        self.assertEqual("observation", event.text)
        self.assertEqual("starcraft_remastered", event.source)
        self.assertEqual("StarCraftRemastered", event.provider_id)
        self.assertEqual("starcraft_output", event.event_kind)
        self.assertIs(True, event.final)

    def test_screen_wiring_reuses_adapter_with_screen_provenance(self):
        screen = _ListenerSource()
        received = []
        wiring = DirectGameInputWiring()

        wiring.wire_screen(
            screen_vision=screen,
            screen_vision_input_callback=received.append,
        )
        wiring.wire_screen(
            screen_vision=screen,
            screen_vision_input_callback=received.append,
        )

        self.assertEqual(2, len(screen.output_listeners))
        self.assertIs(screen.output_listeners[0], screen.output_listeners[1])
        screen.output_listeners[0]("screen")
        self.assertEqual("screen", received[0].text)
        self.assertEqual("screen_vision", received[0].source)
        self.assertEqual("ScreenVision", received[0].provider_id)
        self.assertEqual("screen_observation", received[0].event_kind)

    def test_absent_optional_plugins_do_not_register_callbacks(self):
        llm = _Llm()
        wiring = DirectGameInputWiring()

        wiring.wire_starcraft(llm=llm, starcraft_plugin=None)
        wiring.wire_screen(
            screen_vision=None,
            screen_vision_input_callback=None,
        )

        self.assertEqual([], llm.received)
        self.assertEqual([], llm.output_listener_calls)


class _ListenerSource:
    def __init__(self):
        self.output_listeners = []

    def add_output_event_listener(self, callback):
        self.output_listeners.append(callback)


class _StarCraft(_ListenerSource):
    def receive_coach_response(self, _response):
        return None


class _Llm:
    def __init__(self):
        self.received = []
        self.output_listener_calls = []

    def receive_input(self, event):
        self.received.append(event)

    def add_output_event_listener(self, callback, *, full_response=False):
        self.output_listener_calls.append((callback, full_response))


if __name__ == "__main__":
    unittest.main()
