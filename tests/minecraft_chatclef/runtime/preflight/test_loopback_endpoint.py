#20260818_kpopmodder: Lock explicit HTTP IPv4 loopback endpoint admission.
from __future__ import annotations

import unittest

from .loopback_endpoint import inspect_loopback_gradio_url


class LoopbackEndpointTests(unittest.TestCase):
    def test_defaultless_or_ambiguous_hosts_fail_closed(self):
        for url in ("", "http://localhost:47860", "https://127.0.0.1:47860"):
            with self.subTest(url=url):
                self.assertFalse(inspect_loopback_gradio_url(url)["ok"])


if __name__ == "__main__":
    unittest.main()
