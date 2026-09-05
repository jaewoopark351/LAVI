# 20260905_kpopmodder: Verifies focused owners behind the legacy Input facade.
from __future__ import annotations

import unittest
from types import SimpleNamespace
from unittest import mock

from input_core.component.lifecycle import InputComponentLifecycleCoordinator
from input_core.component.output import InputEventOutputDispatcher
from input_core.component.provider_selection import (
    InputProviderSelectionCoordinator,
)
from input_core.component.ui import InputComponentUiBuilder
from input_core.input_component import Input
from input_core.input_event.contracts import LaviInputEvent


class InputComponentResponsibilitySplitTests(unittest.TestCase):
    def test_input_ui_builder_preserves_selection_provider_and_sync_order(self):
        calls = []
        builder = InputComponentUiBuilder(
            create_provider_selection_ui_callback=lambda: calls.append(
                "selection"
            ),
            create_all_provider_ui_callback=lambda: calls.append("providers"),
            sync_provider_listeners_callback=lambda: calls.append("sync"),
        )
        context_gradio = SimpleNamespace(
            Tab=lambda *_args, **_kwargs: _Context(),
            Blocks=lambda *_args, **_kwargs: _Context(),
        )

        with mock.patch(
            "input_core.component.ui.input_component_ui_builder.gr",
            context_gradio,
        ):
            builder.build()

        self.assertEqual(["selection", "providers", "sync"], calls)

    def test_output_dispatcher_normalizes_deduplicates_and_excludes(self):
        event = LaviInputEvent(
            text="한국어 입력",
            source="voice_input_final",
            event_id="a" * 32,
            event_kind="voice_final",
            final=True,
            provider_id="VoiceInput",
            fallback_payload="한국어 입력",
        )
        logs = []
        dispatcher = InputEventOutputDispatcher(
            normalizer=SimpleNamespace(normalize=lambda _value: event),
            log_callback=logs.append,
        )
        included = []
        excluded = []
        dispatcher.add(included.append)
        dispatcher.add(included.append)
        dispatcher.add(excluded.append)

        dispatcher.send("opaque", excluded_listeners=(excluded.append,))

        self.assertEqual([event], included)
        self.assertEqual([], excluded)
        self.assertEqual([event.text], logs)
        self.assertTrue(dispatcher.remove(included.append))
        self.assertFalse(dispatcher.remove(included.append))

    def test_provider_selection_coordinator_preserves_sync_order(self):
        calls = []
        coordinator = InputProviderSelectionCoordinator(
            create_all_provider_ui_callback=lambda: calls.append("create_all"),
            select_provider_callback=lambda name: (
                calls.append(f"select:{name}") or name
            ),
            sync_provider_listeners_callback=lambda: calls.append("sync"),
        )

        coordinator.create_all_provider_ui()
        selected = coordinator.select_provider("VoiceInput")

        self.assertEqual("VoiceInput", selected)
        self.assertEqual(
            ["create_all", "sync", "select:VoiceInput", "sync"],
            calls,
        )

    def test_lifecycle_is_idempotent_and_preserves_shutdown_order(self):
        calls = []
        state = {"shutdown": False}
        coordinator = InputComponentLifecycleCoordinator(
            shutdown_state_callback=lambda: state["shutdown"],
            mark_shutdown_callback=lambda: state.__setitem__("shutdown", True),
            shutdown_provider_bindings_callback=lambda: calls.append("bindings"),
            clear_provider_binding_requests_callback=lambda: calls.append(
                "binding_requests"
            ),
            clear_output_listeners_callback=lambda: calls.append("listeners"),
            base_shutdown_callback=lambda: calls.append("base"),
        )

        coordinator.shutdown()
        coordinator.shutdown()

        self.assertEqual(
            ["bindings", "binding_requests", "listeners", "base"],
            calls,
        )
        self.assertTrue(state["shutdown"])

    def test_input_facade_methods_are_compatibility_delegates(self):
        calls = []
        component = Input.__new__(Input)
        component._input_compatibility_graph_installer = _InputInstaller(calls)

        component.create_ui()
        component.create_all_provider_ui()
        self.assertEqual(
            "VoiceInput",
            component.on_dropdown_change("VoiceInput"),
        )
        component.send_output("event", excluded_listeners=("excluded",))
        component.add_output_event_listener("listener")
        self.assertTrue(component.remove_output_event_listener("listener"))
        component.shutdown()

        self.assertEqual(
            [
                "ui",
                "create_all",
                "select:VoiceInput",
                ("send", "event", ("excluded",)),
                "add:listener",
                "remove:listener",
                "shutdown",
            ],
            calls,
        )


class _InputInstaller:
    def __init__(self, calls):
        self._calls = calls
        self._dispatcher = _InputDispatcher(calls)

    def ensure_ui_builder(self):
        return SimpleNamespace(build=lambda: self._calls.append("ui"))

    def ensure_provider_selection_coordinator(self):
        return _InputSelection(self._calls)

    def ensure_output_dispatcher(self):
        return self._dispatcher

    def ensure_lifecycle_coordinator(self):
        return SimpleNamespace(shutdown=lambda: self._calls.append("shutdown"))


class _InputSelection:
    def __init__(self, calls):
        self._calls = calls

    def create_all_provider_ui(self):
        self._calls.append("create_all")

    def select_provider(self, name):
        self._calls.append(f"select:{name}")
        return name


class _InputDispatcher:
    def __init__(self, calls):
        self._calls = calls
        self.listeners = []

    def send(self, output, *, excluded_listeners=()):
        self._calls.append(("send", output, excluded_listeners))

    def add(self, listener):
        self._calls.append(f"add:{listener}")

    def remove(self, listener):
        self._calls.append(f"remove:{listener}")
        return True


class _Context:
    def __enter__(self):
        return self

    def __exit__(self, *_args):
        return False


if __name__ == "__main__":
    unittest.main()
