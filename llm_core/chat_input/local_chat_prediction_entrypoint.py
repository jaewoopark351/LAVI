#20260905_kpopmodder: Keeps Gradio streaming dispatch separate from local Chat event adaptation.


class LocalChatPredictionEntrypoint:
    def __init__(self, *, input_event_adapter, predict_callback):
        adapt = getattr(input_event_adapter, "adapt", None)
        if not callable(adapt):
            raise TypeError("input_event_adapter.adapt must be callable")
        if not callable(predict_callback):
            raise TypeError("predict_callback must be callable")

        self._input_event_adapter = input_event_adapter
        self._predict_callback = predict_callback

    def predict(self, message, history, system_prompt):
        input_event = self._input_event_adapter.adapt(message)
        yield from self._predict_callback(input_event, history, system_prompt)


__all__ = ["LocalChatPredictionEntrypoint"]
