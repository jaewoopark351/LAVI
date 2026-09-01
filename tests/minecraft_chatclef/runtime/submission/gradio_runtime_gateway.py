#20260818_kpopmodder: Isolate the Gradio API boundary used by supervised live tests.
from __future__ import annotations

import json
from typing import Any, Mapping


class LaviGradioRuntimeGateway:
    def __init__(self, gradio_url: str):
        from gradio_client import Client

        self.gradio_url = gradio_url
        self._client = Client(gradio_url, verbose=False)
        self.submit_call_count = 0
        self.status_call_count = 0

    def read_status(self) -> dict[str, object]:
        self.status_call_count += 1
        result = self._client.predict(api_name="/on_refresh_click_2")
        if not isinstance(result, (list, tuple)) or len(result) <= 3:
            raise ValueError("Gradio status response shape is invalid")
        payload = json.loads(result[3])
        if not isinstance(payload, Mapping):
            raise ValueError("Gradio status payload must be an object")
        return dict(payload)

    def submit_korean_command(self, command: str) -> dict[str, object]:
        return self._submit_command(
            command,
            api_name="/on_submit_korean_command_click",
        )

    def submit_raw_command(self, command: str) -> dict[str, object]:
        return self._submit_command(
            command,
            api_name="/on_submit_command_click",
        )

    def _submit_command(
        self,
        command: str,
        *,
        api_name: str,
    ) -> dict[str, object]:
        self.submit_call_count += 1
        result = self._client.predict(
            command=command,
            api_name=api_name,
        )
        if not isinstance(result, (list, tuple)) or not result:
            raise ValueError("Gradio submit response shape is invalid")
        payload: Any = json.loads(result[0])
        if not isinstance(payload, Mapping):
            raise ValueError("Gradio submit payload must be an object")
        return dict(payload)
