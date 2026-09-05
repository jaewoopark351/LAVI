#20260905_kpopmodder: Isolate router submission request construction.
from __future__ import annotations


class MinecraftChatClefSubmissionRequestBuilder:
    def __init__(self, request_factory):
        self._request_factory = request_factory

    @property
    def request_factory(self):
        return self._request_factory

    def replace_request_factory(self, request_factory: object) -> None:
        self._request_factory = request_factory

    def build(
        self,
        event: object,
        *,
        original_text: str | None,
        translation_input_text: str | None,
    ):
        return self._request_factory.build(
            event,
            original_text=original_text,
            translation_input_text=translation_input_text,
        )

    def build_generic_crafting_defaults(self, event: object):
        return self._request_factory.build_generic_crafting_defaults(event)


__all__ = ("MinecraftChatClefSubmissionRequestBuilder",)
