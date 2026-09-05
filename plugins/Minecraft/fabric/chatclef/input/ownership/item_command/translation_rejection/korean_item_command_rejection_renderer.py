#20260905_kpopmodder: Render trusted Korean item-command rejections only.
from __future__ import annotations

from types import MappingProxyType


class KoreanItemCommandRejectionRenderer:
    _MESSAGES = MappingProxyType(
        {
            "ambiguous": "어떤 아이템을 준비할지 하나로 정해 주세요.",
            "unsupported": "요청한 재료나 아이템은 현재 지원하지 않아요.",
        }
    )
    _UNKNOWN_MESSAGE = "어떤 아이템을 준비할지 이해하지 못했어요."

    def render(self, status: str) -> str:
        return self._MESSAGES.get(status, self._UNKNOWN_MESSAGE)


__all__ = ("KoreanItemCommandRejectionRenderer",)
