#20260905_kpopmodder: Keep routed-response UI presentation in the established responsibility-split boundary.
#20260908_kpopmodder: Added strict Gradio message normalization for UI presentation confirmation.
from __future__ import annotations

from collections.abc import Mapping
from dataclasses import dataclass
import inspect

from .routed_response_ui_presentation_identity import (
    RoutedResponseUiPresentationIdentity,
)


@dataclass(frozen=True, slots=True)
class RoutedResponseUiPresentationFingerprint:
    presentation_identity: RoutedResponseUiPresentationIdentity
    role: str
    title: str
    canonical_content: str

    def __post_init__(self) -> None:
        if (
            type(self.presentation_identity)
            is not RoutedResponseUiPresentationIdentity
        ):
            raise TypeError("presentation_identity must be exact")
        if self.role != "assistant":
            raise ValueError("role must be assistant")
        if (
            type(self.title) is not str
            or not self.title
            or len(self.title) > 80
        ):
            raise ValueError("title must be a bounded non-empty exact str")
        if type(self.canonical_content) is not str:
            raise TypeError("canonical_content must be an exact str")

    @classmethod
    def from_message(
        cls,
        item: object,
    ) -> "RoutedResponseUiPresentationFingerprint | None":
        role, content, metadata = cls._message_parts(item)
        if role != "assistant" or not isinstance(metadata, Mapping):
            return None
        presentation_identity = (
            RoutedResponseUiPresentationIdentity.from_token(metadata.get("id"))
        )
        title = metadata.get("title")
        canonical_content = cls._canonicalize_content(content)
        if (
            presentation_identity is None
            or type(title) is not str
            or not title
            or len(title) > 80
            or canonical_content is None
        ):
            return None
        return cls(
            presentation_identity=presentation_identity,
            role=role,
            title=title,
            canonical_content=canonical_content,
        )

    @classmethod
    def identity_from_message(
        cls,
        item: object,
    ) -> RoutedResponseUiPresentationIdentity | None:
        role, _content, metadata = cls._message_parts(item)
        if role != "assistant" or not isinstance(metadata, Mapping):
            return None
        return RoutedResponseUiPresentationIdentity.from_token(
            metadata.get("id")
        )

    @staticmethod
    def _message_parts(item: object) -> tuple[object, object, object]:
        if isinstance(item, Mapping):
            return (
                item.get("role"),
                item.get("content"),
                item.get("metadata"),
            )
        return (
            getattr(item, "role", None),
            getattr(item, "content", None),
            getattr(item, "metadata", None),
        )

    @staticmethod
    def _canonicalize_content(content: object) -> str | None:
        if type(content) is str:
            return inspect.cleandoc(content)
        if type(content) is not list or len(content) != 1:
            return None
        text_block = content[0]
        if not isinstance(text_block, Mapping):
            return None
        if text_block.get("type") != "text":
            return None
        text = text_block.get("text")
        if type(text) is not str:
            return None
        return inspect.cleandoc(text)


__all__ = ("RoutedResponseUiPresentationFingerprint",)
