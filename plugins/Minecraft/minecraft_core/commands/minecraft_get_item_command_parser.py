#20260725_kpopmodder: Added parser for get-item style Minecraft bridge commands.
from __future__ import annotations

from typing import Dict

from .minecraft_get_item_candidate_selector import MinecraftGetItemCandidateSelector
from .minecraft_get_item_candidate_normalizer import MinecraftGetItemCandidateNormalizer
from .minecraft_get_item_payload_builder import MinecraftGetItemPayloadBuilder
from .minecraft_get_item_request_extractor import MinecraftGetItemRequestExtractor


class MinecraftGetItemCommandParser:
    def __init__(
        self,
        candidate_selector: MinecraftGetItemCandidateSelector | None = None,
        candidate_normalizer: MinecraftGetItemCandidateNormalizer | None = None,
        request_extractor: MinecraftGetItemRequestExtractor | None = None,
        payload_builder: MinecraftGetItemPayloadBuilder | None = None,
    ):
        self.candidate_selector = candidate_selector or MinecraftGetItemCandidateSelector()
        self.candidate_normalizer = (
            candidate_normalizer or MinecraftGetItemCandidateNormalizer()
        )
        self.request_extractor = request_extractor or MinecraftGetItemRequestExtractor()
        self.payload_builder = payload_builder or MinecraftGetItemPayloadBuilder()

    def parse(self, text: str, lowered: str) -> Dict[str, object] | None:
        candidate = self.candidate_selector.select(text, lowered)
        if not candidate:
            return None

        candidate = self.candidate_normalizer.normalize(candidate)
        request = self.request_extractor.extract(candidate)
        if request is None:
            return None
        return self.payload_builder.build(text, request)
