# 20260905_kpopmodder: Verifies the extracted LLM Gradio callback chain.
from __future__ import annotations

import unittest
from types import SimpleNamespace
from unittest import mock

from llm_core.ui import LlmChatUiBuilder


class LlmChatUiBuilderTests(unittest.TestCase):
    def test_build_preserves_chat_callbacks_examples_and_timer_outputs(self):
        fake_gradio = _FakeGradio()
        host = SimpleNamespace()
        calls = []
        local_factory = _LocalChatFactory(calls)
        live = _LiveTextbox("console")
        queue_live = _LiveTextbox("queue")
        def load():
            return "prompt"

        def update(value):
            return value

        def update_style(value):
            return value

        def reset():
            return None
        builder = LlmChatUiBuilder(
            host=host,
            create_plugin_selection_ui_callback=lambda: calls.append("selection"),
            create_plugin_ui_callback=lambda: calls.append("plugin_ui"),
            local_chat_interface_factory_callback=lambda: local_factory,
            load_content_callback=load,
            update_content_callback=update,
            speech_style_labels_callback=lambda: {
                "polite": "존댓말",
                "casual": "반말",
            },
            speech_style_label_callback=lambda: "존댓말",
            update_speech_style_callback=update_style,
            reset_history_callback=reset,
            live_textbox=live,
            queue_live_textbox=queue_live,
        )

        with mock.patch("llm_core.ui.llm_chat_ui_builder.gr", fake_gradio):
            builder.build()

        textbox = fake_gradio.created["Textbox"][0]
        radio = fake_gradio.created["Radio"][0]
        button = fake_gradio.created["Button"][0]
        timers = fake_gradio.created["Timer"]
        self.assertIs(load, textbox.kwargs["value"])
        self.assertEqual([(update, textbox)], textbox.change_calls)
        self.assertEqual(["존댓말", "반말"], radio.kwargs["choices"])
        self.assertEqual([(update_style, radio)], radio.change_calls)
        self.assertEqual([(reset, [], [])], button.click_calls)
        self.assertEqual([host.console_box], timers[0].tick_calls[0][1])
        self.assertEqual([host.queue_console_box], timers[1].tick_calls[0][1])
        self.assertEqual("selection", calls[0])
        self.assertEqual("local_chat", calls[1][0])
        self.assertEqual("plugin_ui", calls[-1])
        self.assertIs(textbox, calls[1][1])
        self.assertIsInstance(calls[1][2], list)
        self.assertEqual(["Hello", None, None], calls[1][2][0])


class _Context:
    def __enter__(self):
        return self

    def __exit__(self, *_args):
        return False


class _Component:
    def __init__(self, *args, **kwargs):
        self.args = args
        self.kwargs = kwargs
        self.change_calls = []
        self.click_calls = []
        self.tick_calls = []

    def change(self, *, fn, inputs):
        self.change_calls.append((fn, inputs))

    def click(self, *, fn, inputs, outputs):
        self.click_calls.append((fn, inputs, outputs))

    def tick(self, *, fn, outputs, show_progress, queue):
        self.tick_calls.append((fn, outputs, show_progress, queue))


class _FakeGradio:
    def __init__(self):
        self.created = {
            "Textbox": [],
            "Radio": [],
            "Button": [],
            "Timer": [],
        }

    def Tab(self, *_args, **_kwargs):
        return _Context()

    def Blocks(self, *_args, **_kwargs):
        return _Context()

    def Accordion(self, *_args, **_kwargs):
        return _Context()

    def Textbox(self, *args, **kwargs):
        return self._create("Textbox", *args, **kwargs)

    def Radio(self, *args, **kwargs):
        return self._create("Radio", *args, **kwargs)

    def Button(self, *args, **kwargs):
        return self._create("Button", *args, **kwargs)

    def Timer(self, *args, **kwargs):
        return self._create("Timer", *args, **kwargs)

    def _create(self, kind, *args, **kwargs):
        component = _Component(*args, **kwargs)
        self.created[kind].append(component)
        return component


class _LocalChatFactory:
    def __init__(self, calls):
        self._calls = calls

    def create(self, *, system_prompt, examples, autofocus):
        self._calls.append(
            ("local_chat", system_prompt, examples, autofocus)
        )


class _LiveTextbox:
    def __init__(self, name):
        self._name = name

    def create_ui(self, **_kwargs):
        return self._name

    def get_text(self):
        return self._name


if __name__ == "__main__":
    unittest.main()
