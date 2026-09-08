#20260907_kpopmodder: Bound playback facts deferred until enqueue commit.
from __future__ import annotations

from collections import deque

from ..tts_lifecycle_response_playback_receipt import (
    TtsLifecycleResponsePlaybackReceipt,
)


class TtsLifecycleResponseDeferredReceiptBuffer:
    """Caller-synchronized buffer; intentionally owns no lock."""

    def __init__(self, *, capacity: int) -> None:
        if type(capacity) is not int or capacity <= 0:
            raise ValueError("capacity must be a positive exact int")
        self._receipts = deque(maxlen=capacity)

    def append(self, receipt: TtsLifecycleResponsePlaybackReceipt) -> None:
        if type(receipt) is not TtsLifecycleResponsePlaybackReceipt:
            raise TypeError("receipt must be exact")
        self._receipts.append(receipt)

    def drain(self) -> tuple[TtsLifecycleResponsePlaybackReceipt, ...]:
        receipts = tuple(self._receipts)
        self._receipts.clear()
        return receipts

    def clear(self) -> None:
        self._receipts.clear()


__all__ = ("TtsLifecycleResponseDeferredReceiptBuffer",)
