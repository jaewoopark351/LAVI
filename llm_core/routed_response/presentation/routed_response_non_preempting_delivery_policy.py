#20260905_kpopmodder: Keep routed-response presentation in the established responsibility-split boundary.
#20260907_kpopmodder: Keep asynchronous lifecycle delivery outside global LLM generation ownership.
from __future__ import annotations


class RoutedResponseNonPreemptingDeliveryPolicy:
    CURRENT_INPUT = "current_input"
    NON_PREEMPTING = "non_preempting"
    KINDS = frozenset({CURRENT_INPUT, NON_PREEMPTING})

    @classmethod
    def normalize(cls, value: object) -> str:
        if type(value) is not str or value not in cls.KINDS:
            raise ValueError("delivery_mode must be a registered exact str")
        return value

    @classmethod
    def begins_global_generation(cls, value: object) -> bool:
        return cls.normalize(value) == cls.CURRENT_INPUT


__all__ = ("RoutedResponseNonPreemptingDeliveryPolicy",)
