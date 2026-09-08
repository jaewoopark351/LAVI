#20260905_kpopmodder: Owns the Gradio registration contract for local Chat streaming.
import inspect

import gradio as gr

from llm_core.chat_input.gradio import GradioLocalChatStreamCompletionAdapter


class LocalChatInterfaceFactory:
    def __init__(self, *, prediction_entrypoint):
        prediction_callback = getattr(prediction_entrypoint, "predict", None)
        if not inspect.isgeneratorfunction(prediction_callback):
            raise TypeError(
                "prediction_entrypoint.predict must be a generator function"
            )

        self._prediction_callback = prediction_callback
        self._stream_completion_adapter = GradioLocalChatStreamCompletionAdapter(
            prediction_callback=prediction_callback,
        )

    def create(
        self,
        *,
        system_prompt,
        examples,
        autofocus,
    ):
        return gr.ChatInterface(
            self._stream_completion_adapter.predict,
            additional_inputs=[system_prompt],
            examples=examples,
            autofocus=autofocus,
        )


__all__ = ("LocalChatInterfaceFactory",)
