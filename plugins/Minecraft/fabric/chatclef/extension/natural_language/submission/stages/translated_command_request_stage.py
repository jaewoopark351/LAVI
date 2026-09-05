#20260905_kpopmodder: Isolate translated-command request construction and replacement seam.
from __future__ import annotations


class TranslatedCommandRequestStage:
    def __init__(self, request_factory):
        self._request_factory = request_factory

    @property
    def request_factory(self):
        return self._request_factory

    def replace_request_factory(self, request_factory: object) -> None:
        self._request_factory = request_factory

    def build(self, command: object, translation: object, original_text: str):
        return self._request_factory.build(
            command,
            translation,
            original_text,
        )


__all__ = ("TranslatedCommandRequestStage",)
