#20260819_kpopmodder: Parse untrusted submit payload containers without semantic decisions.
from __future__ import annotations

import copy
from typing import Any, Mapping


class SubmissionPayloadParser:
    def parse(self, payload: Any) -> dict[str, Any] | None:
        if isinstance(payload, Mapping):
            return self.mapping(payload)
        to_dict = getattr(payload, "to_dict", None)
        if not callable(to_dict):
            return None
        try:
            mapped = to_dict()
        except Exception:
            return None
        return self.mapping(mapped)

    def mapping(self, value: Any) -> dict[str, Any] | None:
        if not isinstance(value, Mapping):
            return None
        try:
            return dict(value)
        except Exception:
            return None

    def fresh_mapping(self, value: Any) -> dict[str, Any] | None:
        mapped = self.mapping(value)
        if mapped is None:
            return None
        try:
            return copy.deepcopy(mapped)
        except Exception:
            return None
