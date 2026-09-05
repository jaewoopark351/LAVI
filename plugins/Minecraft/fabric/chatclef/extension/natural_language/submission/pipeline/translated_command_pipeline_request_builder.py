#20260905_kpopmodder: Isolate translated-command pipeline request construction.
from __future__ import annotations


class TranslatedCommandPipelineRequestBuilder:
    def __init__(self, request_stage):
        self._request_stage = request_stage

    def build(self, command: object, translation: object, original_text: str):
        return self._request_stage.build(command, translation, original_text)


__all__ = ("TranslatedCommandPipelineRequestBuilder",)
