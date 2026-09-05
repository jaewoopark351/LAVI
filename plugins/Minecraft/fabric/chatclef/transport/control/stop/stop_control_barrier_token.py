#20260905_kpopmodder: Bind one Python STOP admission barrier to its exact owner.
from __future__ import annotations


class StopControlBarrierToken:
    __slots__ = ("_owner", "_identity", "_nonce")

    def __init__(self, owner: object, identity: tuple[object, ...], nonce: object):
        self._owner = owner
        self._identity = identity
        self._nonce = nonce

    def __reduce__(self):
        raise TypeError("StopControlBarrierToken cannot be serialized")

    def __reduce_ex__(self, _protocol):
        raise TypeError("StopControlBarrierToken cannot be serialized")


__all__ = ("StopControlBarrierToken",)
