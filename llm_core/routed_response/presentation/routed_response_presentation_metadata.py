#20260905_kpopmodder: Keep routed-response presentation in the established responsibility-split boundary.
#20260907_kpopmodder: Carry UI-only response source decoration without changing spoken text.
from __future__ import annotations

from dataclasses import dataclass
from types import MappingProxyType


@dataclass(frozen=True, slots=True)
class RoutedResponsePresentationMetadata:
    source_kind: str
    badge_label: str
    detail_log: str = ""

    MINECRAFT_SOURCE_KIND = "minecraft"
    MINECRAFT_BADGE_LABEL = "Minecraft"

    def __post_init__(self) -> None:
        for name, value in (
            ("source_kind", self.source_kind),
            ("badge_label", self.badge_label),
        ):
            if type(value) is not str or not value or len(value) > 80:
                raise ValueError(f"{name} must be a bounded non-empty exact str")
        if (
            type(self.detail_log) is not str
            or len(self.detail_log) > 1024
            or any(
                ord(character) < 32 or ord(character) > 126
                for character in self.detail_log
            )
        ):
            raise ValueError("detail_log must be bounded printable ASCII")

    @classmethod
    def minecraft(
        cls,
        *,
        detail_log: str = "",
    ) -> "RoutedResponsePresentationMetadata":
        return cls(
            source_kind=cls.MINECRAFT_SOURCE_KIND,
            badge_label=cls.MINECRAFT_BADGE_LABEL,
            detail_log=detail_log,
        )

    def as_mapping(self):
        return MappingProxyType(
            {
                "source_kind": self.source_kind,
                "badge_label": self.badge_label,
            }
        )


__all__ = ("RoutedResponsePresentationMetadata",)
