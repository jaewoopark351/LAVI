#20260905_kpopmodder: Encode one ordinary-command diagnostic payload without raising.
from __future__ import annotations

import json
from typing import Any


class FabricChatClefCommandSubmissionDiagnosticEncoder:
    @staticmethod
    def encode(payload: Any) -> str:
        try:
            return json.dumps(
                payload,
                ensure_ascii=False,
                sort_keys=True,
                separators=(",", ":"),
            )
        except Exception as error:
            return f"<json failed {type(error).__name__}: {error}>"
