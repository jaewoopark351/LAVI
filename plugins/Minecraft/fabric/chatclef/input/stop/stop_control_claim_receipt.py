#20260905_kpopmodder: Expose a process-local, non-serializable STOP claim capability.
from __future__ import annotations


_ISSUANCE_TOKEN = object()


class StopControlClaimReceipt:
    __slots__ = ("_registry", "_record_key", "_nonce", "_sealed")

    def __init__(
        self,
        *,
        registry: object,
        record_key: str,
        nonce: object,
        _issuance_token: object = None,
    ):
        if _issuance_token is not _ISSUANCE_TOKEN:
            raise TypeError("StopControlClaimReceipt is registry-issued only")
        object.__setattr__(self, "_registry", registry)
        object.__setattr__(self, "_record_key", record_key)
        object.__setattr__(self, "_nonce", nonce)
        object.__setattr__(self, "_sealed", True)

    @classmethod
    def _issue(
        cls,
        *,
        registry: object,
        record_key: str,
        nonce: object,
    ) -> "StopControlClaimReceipt":
        return cls(
            registry=registry,
            record_key=record_key,
            nonce=nonce,
            _issuance_token=_ISSUANCE_TOKEN,
        )

    def __setattr__(self, name: str, value: object) -> None:
        if getattr(self, "_sealed", False):
            raise AttributeError("StopControlClaimReceipt is immutable")
        object.__setattr__(self, name, value)

    def __repr__(self) -> str:
        return "StopControlClaimReceipt(<opaque>)"

    def __copy__(self):
        raise TypeError("StopControlClaimReceipt cannot be copied")

    def __deepcopy__(self, _memo):
        raise TypeError("StopControlClaimReceipt cannot be copied")

    def __reduce__(self):
        raise TypeError("StopControlClaimReceipt is process-local and cannot be serialized")

    def __reduce_ex__(self, _protocol):
        raise TypeError("StopControlClaimReceipt is process-local and cannot be serialized")


__all__ = ("StopControlClaimReceipt",)
