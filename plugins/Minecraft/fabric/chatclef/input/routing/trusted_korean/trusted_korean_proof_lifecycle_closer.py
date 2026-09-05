#20260905_kpopmodder: Own ordered feature-dispatch and proof cleanup.
from __future__ import annotations


class TrustedKoreanProofLifecycleCloser:
    def __init__(self, close_feature_dispatch_callback):
        self._close_feature_dispatch_callback = close_feature_dispatch_callback

    def close(self, proof: object) -> None:
        try:
            self._close_feature_dispatch_callback(proof)
        finally:
            proof.close()


__all__ = ("TrustedKoreanProofLifecycleCloser",)
