#20260725_kpopmodder: Added parser for craft-style Minecraft bridge commands.
from __future__ import annotations

from typing import Dict

from .minecraft_craft_candidate_selector import MinecraftCraftCandidateSelector
from .minecraft_craft_payload_builder import MinecraftCraftPayloadBuilder
from .minecraft_get_item_candidate_normalizer import (
    MinecraftGetItemCandidateNormalizer,
)
from .minecraft_get_item_request_extractor import MinecraftGetItemRequestExtractor


class MinecraftCraftCommandParser:
    def __init__(
        self,
        candidate_selector: MinecraftCraftCandidateSelector | None = None,
        candidate_normalizer: MinecraftGetItemCandidateNormalizer | None = None,
        request_extractor: MinecraftGetItemRequestExtractor | None = None,
        payload_builder: MinecraftCraftPayloadBuilder | None = None,
    ):
        self.candidate_selector = candidate_selector or MinecraftCraftCandidateSelector()
        self.candidate_normalizer = (
            candidate_normalizer or MinecraftGetItemCandidateNormalizer()
        )
        self.request_extractor = request_extractor or MinecraftGetItemRequestExtractor()
        self.payload_builder = payload_builder or MinecraftCraftPayloadBuilder()

    def parse(self, text: str, lowered: str) -> Dict[str, object] | None:
        candidate = self.candidate_selector.select(text, lowered)
        if not candidate:
            return None

        candidate = self.candidate_normalizer.normalize(candidate)
        request = self.request_extractor.extract(candidate)
        if request is None:
            return None
        return self.payload_builder.build(text, request)
