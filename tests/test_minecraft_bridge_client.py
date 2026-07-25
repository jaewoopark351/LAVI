#20260725_kpopmodder: Covers ChatClef bridge HTTP client endpoint behavior.
import json
import unittest

from plugins.Minecraft.minecraft_core import ChatClefBridgeClient


class FakeHttpResponse:
    def __init__(self, payload):
        self.payload = payload

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, traceback):
        return False

    def read(self):
        return json.dumps(self.payload).encode("utf-8")


class MinecraftBridgeClientTests(unittest.TestCase):
    def test_client_posts_get_item_to_v1_endpoint(self):
        calls = []

        def opener(request, timeout):
            calls.append((request, timeout))
            return FakeHttpResponse({"ok": True, "accepted": True})

        client = ChatClefBridgeClient(
            base_url="http://127.0.0.1:4316",
            timeout_sec=2,
            opener=opener,
        )

        result = client.get_item("oak_log", 2)

        self.assertTrue(result["ok"])
        request, timeout = calls[0]
        self.assertEqual(2, timeout)
        self.assertEqual("POST", request.get_method())
        self.assertEqual(
            "http://127.0.0.1:4316/v1/actions/get-item",
            request.full_url,
        )
        self.assertEqual(
            {"item": "oak_log", "count": 2},
            json.loads(request.data.decode("utf-8")),
        )

    def test_client_posts_get_and_equip_to_v1_endpoint(self):
        calls = []

        def opener(request, timeout):
            calls.append((request, timeout))
            return FakeHttpResponse({"ok": True, "accepted": True})

        client = ChatClefBridgeClient(
            base_url="http://127.0.0.1:4316",
            timeout_sec=2,
            opener=opener,
        )

        result = client.get_and_equip("iron_pickaxe", 1)

        self.assertTrue(result["ok"])
        request, _timeout = calls[0]
        self.assertEqual("POST", request.get_method())
        self.assertEqual(
            "http://127.0.0.1:4316/v1/actions/get-and-equip",
            request.full_url,
        )
        self.assertEqual(
            {"item": "iron_pickaxe", "count": 1},
            json.loads(request.data.decode("utf-8")),
        )

    def test_client_posts_equip_goto_and_stop_to_v1_endpoints(self):
        calls = []

        def opener(request, timeout):
            calls.append((request, timeout))
            return FakeHttpResponse({"ok": True, "accepted": True})

        client = ChatClefBridgeClient(
            base_url="http://127.0.0.1:4316",
            timeout_sec=2,
            opener=opener,
        )

        equip_result = client.equip("iron_pickaxe")
        goto_result = client.goto("0 64 0 overworld")
        stop_result = client.stop()

        self.assertTrue(equip_result["ok"])
        self.assertTrue(goto_result["ok"])
        self.assertTrue(stop_result["ok"])
        self.assertEqual("POST", calls[0][0].get_method())
        self.assertEqual(
            "http://127.0.0.1:4316/v1/actions/equip",
            calls[0][0].full_url,
        )
        self.assertEqual(
            {"item": "iron_pickaxe"},
            json.loads(calls[0][0].data.decode("utf-8")),
        )
        self.assertEqual("POST", calls[1][0].get_method())
        self.assertEqual(
            "http://127.0.0.1:4316/v1/actions/goto",
            calls[1][0].full_url,
        )
        self.assertEqual(
            {"target": "0 64 0 overworld"},
            json.loads(calls[1][0].data.decode("utf-8")),
        )
        self.assertEqual(
            "http://127.0.0.1:4316/v1/actions/stop",
            calls[2][0].full_url,
        )


if __name__ == "__main__":
    unittest.main()
