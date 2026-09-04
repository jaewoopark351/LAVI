#20260905_kpopmodder: Verifies fail-closed router installation and source-bound direct callbacks at app wiring.
import unittest
from unittest import mock

from app_core.composition_core import (
    AppComponentWiringError,
    AppComponentWiringService,
)
from input_core.input_event import LaviInputEvent
from plugins.Minecraft.fabric.chatclef.input import MinecraftChatClefInputRouter


class AutoDepositTrustAppWiringContractTests(unittest.TestCase):
    def test_router_is_installed_when_extension_is_none(self):
        components = _components()

        AppComponentWiringService().wire_event_listeners(
            **components,
            minecraft_fabric_chatclef_extension=None,
        )

        self.assertIsInstance(
            components["llm"].input_router,
            MinecraftChatClefInputRouter,
        )
        self.assertIsNone(components["llm"].input_router.extension)

    def test_router_construction_failure_propagates_as_typed_wiring_error(self):
        components = _components()

        with mock.patch(
            "plugins.Minecraft.fabric.chatclef.input.MinecraftChatClefInputRouter",
            side_effect=RuntimeError("broken constructor"),
        ):
            with self.assertRaises(AppComponentWiringError) as caught:
                AppComponentWiringService().wire_event_listeners(
                    **components,
                    minecraft_fabric_chatclef_extension=None,
                )

        self.assertEqual(
            "minecraft_input_router_construction",
            caught.exception.stage,
        )
        self.assertEqual([], components["input_component"].listeners)

    def test_router_import_failure_propagates_as_typed_wiring_error(self):
        components = _components()
        real_import = __import__

        def import_or_fail(name, globals=None, locals=None, fromlist=(), level=0):
            if name == "plugins.Minecraft.fabric.chatclef.input":
                raise ImportError("broken router module")
            return real_import(name, globals, locals, fromlist, level)

        with mock.patch("builtins.__import__", side_effect=import_or_fail):
            with self.assertRaises(AppComponentWiringError) as caught:
                AppComponentWiringService().wire_event_listeners(
                    **components,
                    minecraft_fabric_chatclef_extension=None,
                )

        self.assertEqual("minecraft_input_router_import", caught.exception.stage)
        self.assertEqual([], components["input_component"].listeners)

    def test_screen_and_starcraft_callbacks_are_stable_and_source_bound(self):
        components = _components(include_direct_sources=True)
        screen_events = components.pop("screen_events")
        service = AppComponentWiringService()

        service.wire_event_listeners(
            **components,
            minecraft_fabric_chatclef_extension=None,
        )
        first_router = components["llm"].input_router
        service.wire_event_listeners(
            **components,
            minecraft_fabric_chatclef_extension=None,
        )

        self.assertIs(first_router, components["llm"].input_router)
        screen_source = components["screen_vision"]
        starcraft_source = components["starcraft_plugin"]
        self.assertEqual(1, len(screen_source.listeners))
        self.assertEqual(1, len(starcraft_source.listeners))

        screen_payload = {
            "kind": "screen_observation",
            "text": "describe",
            "remember_history": False,
            "metadata": {"kept": True},
        }
        screen_source.listeners[0](screen_payload)
        starcraft_source.listeners[0]("coach input")

        screen_event = screen_events[0]
        starcraft_event = components["llm"].received[0]
        self.assertIsInstance(screen_event, LaviInputEvent)
        self.assertEqual("screen_vision", screen_event.source)
        self.assertIs(screen_payload, screen_event.fallback_payload)
        self.assertEqual("starcraft_remastered", starcraft_event.source)
        self.assertEqual("StarCraftRemastered", starcraft_event.provider_id)


class _ListenerSource:
    def __init__(self):
        self.listeners = []
        self.expression_listeners = []

    def add_output_event_listener(self, listener, *args, **kwargs):
        if listener not in self.listeners:
            self.listeners.append(listener)

    def add_expression_event_listener(self, listener):
        if listener not in self.expression_listeners:
            self.expression_listeners.append(listener)

    def receive_input(self, _value):
        return None


class _FakeLlm(_ListenerSource):
    def __init__(self):
        super().__init__()
        self.input_router = None
        self.received = []

    def set_input_router(self, router):
        self.input_router = router

    def receive_input(self, value):
        self.received.append(value)


class _StarCraftSource(_ListenerSource):
    def receive_coach_response(self, _value):
        return None


class _VtuberSource(_ListenerSource):
    def receive_song_expression(self, _value):
        return None


def _components(include_direct_sources=False):
    screen_events = []
    values = {
        "input_component": _ListenerSource(),
        "llm": _FakeLlm(),
        "translate": _ListenerSource(),
        "tts": _ListenerSource(),
        "vtuber": _VtuberSource(),
    }
    if include_direct_sources:
        values.update(
            {
                "starcraft_plugin": _StarCraftSource(),
                "screen_vision": _ListenerSource(),
                "screen_vision_input_callback": screen_events.append,
                "screen_events": screen_events,
            }
        )
    return values


__all__ = ["AutoDepositTrustAppWiringContractTests"]


if __name__ == "__main__":
    unittest.main()
