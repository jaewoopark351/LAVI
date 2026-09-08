#20260908_kpopmodder: Adapts an empty product stream into one invisible Gradio completion.
import inspect


class GradioLocalChatStreamCompletionAdapter:
    def __init__(self, *, prediction_callback):
        if not inspect.isgeneratorfunction(prediction_callback):
            raise TypeError("prediction_callback must be a generator function")

        self._prediction_callback = prediction_callback

    def predict(self, message, history, system_prompt):
        prediction_stream = self._prediction_callback(
            message,
            history,
            system_prompt,
        )
        emitted = False

        try:
            for chunk in prediction_stream:
                emitted = True
                yield chunk

            if not emitted:
                # Gradio treats a list as zero assistant messages while still
                # advancing its generator lifecycle to normal completion.
                yield []
        finally:
            prediction_stream.close()


__all__ = ("GradioLocalChatStreamCompletionAdapter",)
