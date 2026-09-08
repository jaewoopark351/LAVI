#20260905_kpopmodder: Keep routed-response UI presentation in the established responsibility-split boundary.
#20260908_kpopmodder: Added immutable lifecycle identity for routed-response UI presentations.
from __future__ import annotations

from dataclasses import dataclass
from hashlib import sha256
import json
from typing import ClassVar


@dataclass(frozen=True, slots=True)
class RoutedResponseUiPresentationIdentity:
    token: str

    TOKEN_PREFIX: ClassVar[str] = "lavi-routed-response-ui-v1:"
    _TOKEN_DIGEST_LENGTH: ClassVar[int] = 64

    def __post_init__(self) -> None:
        if not self._is_valid_token(self.token):
            raise ValueError("token must be a valid routed-response UI token")

    @classmethod
    def from_response(
        cls,
        *,
        event_id: object,
        route_kind: object,
        response_kind: object,
        response_source: object,
        source_kind: object,
        badge_label: object,
    ) -> "RoutedResponseUiPresentationIdentity":
        normalized_event_id = (
            event_id if type(event_id) is str and event_id else "none"
        )
        for name, value in (
            ("route_kind", route_kind),
            ("response_kind", response_kind),
            ("response_source", response_source),
            ("source_kind", source_kind),
            ("badge_label", badge_label),
        ):
            if type(value) is not str or not value:
                raise ValueError(f"{name} must be a non-empty exact str")
        token_payload = json.dumps(
            (
                normalized_event_id,
                route_kind,
                response_kind,
                response_source,
                source_kind,
                badge_label,
            ),
            ensure_ascii=False,
            separators=(",", ":"),
        ).encode("utf-8")
        token = cls.TOKEN_PREFIX + sha256(token_payload).hexdigest()
        return cls(token=token)

    @classmethod
    def from_token(
        cls,
        token: object,
    ) -> "RoutedResponseUiPresentationIdentity | None":
        return cls(token=token) if cls._is_valid_token(token) else None

    @classmethod
    def _is_valid_token(cls, token: object) -> bool:
        if type(token) is not str or not token.startswith(cls.TOKEN_PREFIX):
            return False
        digest = token[len(cls.TOKEN_PREFIX) :]
        return (
            len(digest) == cls._TOKEN_DIGEST_LENGTH
            and all(character in "0123456789abcdef" for character in digest)
        )


__all__ = ("RoutedResponseUiPresentationIdentity",)
