#20260907_kpopmodder: Keep immutable descriptor phrase semantics in one class-free module.
from __future__ import annotations


def is_typed(descriptor: object) -> bool:
    return getattr(descriptor, "detail_level", "typed") in {
        "typed",
        "raw_typed",
    }


def get_verb(descriptor: object) -> str:
    verb = str(getattr(descriptor, "acquisition_verb_class", "") or "")
    return verb if verb in {"craft", "mining"} else "acquire"


def inventory_default(descriptor: object) -> bool:
    return bool(
        getattr(descriptor, "form_kind", "") == "inventory_default"
        or (
            getattr(descriptor, "detail_level", "typed") == "typed"
            and not getattr(descriptor, "target_item", None)
        )
    )


__all__ = ("get_verb", "inventory_default", "is_typed")
