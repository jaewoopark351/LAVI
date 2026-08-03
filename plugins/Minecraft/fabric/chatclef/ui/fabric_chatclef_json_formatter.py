#20260803_kpopmodder: Added focused JSON formatting for Fabric ChatClef UI payloads.
from __future__ import annotations

import json
from typing import Any, Mapping


class FabricChatClefJsonFormatter:
    def mapping_payload(self, value: Any) -> dict[str, Any]:
        return dict(value) if isinstance(value, Mapping) else {}

    def to_json(self, value: Any) -> str:
        return json.dumps(
            value,
            ensure_ascii=False,
            indent=2,
            default=str,
        )
