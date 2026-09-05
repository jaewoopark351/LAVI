#20260905_kpopmodder: Owns the Gradio registration contract for local Chat streaming.
import inspect

import gradio as gr


class LocalChatInterfaceFactory:
    def __init__(self, *, prediction_entrypoint):
        prediction_callback = getattr(prediction_entrypoint, "predict", None)
        if not inspect.isgeneratorfunction(prediction_callback):
            raise TypeError(
                "prediction_entrypoint.predict must be a generator function"
            )

        self._prediction_callback = prediction_callback

    def create(
        self,
        *,
        system_prompt,
        examples,
        autofocus,
    ):
        return gr.ChatInterface(
            self._prediction_callback,
            additional_inputs=[system_prompt],
            examples=examples,
            autofocus=autofocus,
        )


__all__ = ("LocalChatInterfaceFactory",)
