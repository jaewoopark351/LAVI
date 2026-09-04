#20260905_kpopmodder: Extracts routing text without rebuilding or mutating the fallback object.
from collections.abc import Mapping


class InputEventTextNormalizer:
    def normalize(self, payload) -> str:
        if type(payload) is str:
            return payload
        if isinstance(payload, Mapping):
            text = payload.get("text", "")
            if type(text) is str:
                return text
            return "" if text is None else str(text)
        return "" if payload is None else str(payload)


__all__ = ["InputEventTextNormalizer"]
