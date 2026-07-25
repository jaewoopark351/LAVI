#20260725_kpopmodder: Added parser for get-and-equip Minecraft bridge commands.
from __future__ import annotations

from typing import Dict

from .minecraft_get_and_equip_candidate_selector import (
    MinecraftGetAndEquipCandidateSelector,
)
from .minecraft_get_and_equip_payload_builder import (
    MinecraftGetAndEquipPayloadBuilder,
)
from .minecraft_get_item_candidate_normalizer import (
    MinecraftGetItemCandidateNormalizer,
)
from .minecraft_get_item_request_extractor import MinecraftGetItemRequestExtractor


class MinecraftGetAndEquipCommandParser:
    def __init__(
        self,
        candidate_selector: MinecraftGetAndEquipCandidateSelector | None = None,
        candidate_normalizer: MinecraftGetItemCandidateNormalizer | None = None,
        request_extractor: MinecraftGetItemRequestExtractor | None = None,
        payload_builder: MinecraftGetAndEquipPayloadBuilder | None = None,
    ):
        self.candidate_selector = (
            candidate_selector or MinecraftGetAndEquipCandidateSelector()
        )
        self.candidate_normalizer = (
            candidate_normalizer or MinecraftGetItemCandidateNormalizer()
        )
        self.request_extractor = request_extractor or MinecraftGetItemRequestExtractor()
        self.payload_builder = payload_builder or MinecraftGetAndEquipPayloadBuilder()

    def parse(self, text: str, lowered: str) -> Dict[str, object] | None:
        candidate = self.candidate_selector.select(text, lowered)
        if not candidate:
            return None

        candidate = self.candidate_normalizer.normalize(candidate)
        request = self.request_extractor.extract(candidate)
        if request is None:
            return None
        return self.payload_builder.build(text, request)
