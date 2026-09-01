#20260901_kpopmodder: Verify the raw one-shot gateway uses only the raw Gradio endpoint.
from __future__ import annotations

import json
import unittest

from .gradio_runtime_gateway import LaviGradioRuntimeGateway


class LaviGradioRuntimeGatewayTests(unittest.TestCase):
    def test_raw_command_uses_exact_raw_endpoint_once(self):
        client = _PredictClient((_accepted_json(), "endpoint", "connected"))
        gateway = _gateway(client)

        payload = gateway.submit_raw_command("@store_home")

        self.assertTrue(payload["ok"])
        self.assertEqual(
            [
                {
                    "command": "@store_home",
                    "api_name": "/on_submit_command_click",
                }
            ],
            client.calls,
        )
        self.assertEqual(1, gateway.submit_call_count)

    def test_raw_command_rejects_invalid_response_shape_without_retry(self):
        client = _PredictClient(())
        gateway = _gateway(client)

        with self.assertRaisesRegex(ValueError, "response shape is invalid"):
            gateway.submit_raw_command("@store_home")

        self.assertEqual(1, len(client.calls))
        self.assertEqual(1, gateway.submit_call_count)


class _PredictClient:
    def __init__(self, result):
        self._result = result
        self.calls = []

    def predict(self, **kwargs):
        self.calls.append(dict(kwargs))
        return self._result


def _gateway(client):
    gateway = LaviGradioRuntimeGateway.__new__(LaviGradioRuntimeGateway)
    gateway.gradio_url = "http://127.0.0.1:47860"
    gateway._client = client
    gateway.submit_call_count = 0
    gateway.status_call_count = 0
    return gateway


def _accepted_json() -> str:
    return json.dumps(
        {
            "ok": True,
            "status": {
                "request_id": "request-1",
                "ok": True,
                "status": "accepted",
                "error_code": None,
                "message": "accepted",
                "data": {},
            },
            "error": None,
            "message": "accepted",
            "details": {},
        }
    )


if __name__ == "__main__":
    unittest.main()
