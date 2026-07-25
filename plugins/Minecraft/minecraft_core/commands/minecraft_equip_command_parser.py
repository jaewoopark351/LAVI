#20260725_kpopmodder: Added parser for equip-style Minecraft bridge commands.
from __future__ import annotations

from typing import Dict

from .minecraft_equip_candidate_selector import MinecraftEquipCandidateSelector
from .minecraft_equip_payload_builder import MinecraftEquipPayloadBuilder
from .minecraft_equip_request_extractor import MinecraftEquipRequestExtractor
from .minecraft_item_phrase_normalizer import MinecraftItemPhraseNormalizer


class MinecraftEquipCommandParser:
    def __init__(
        self,
        candidate_selector: MinecraftEquipCandidateSelector | None = None,
        item_phrase_normalizer: MinecraftItemPhraseNormalizer | None = None,
        request_extractor: MinecraftEquipRequestExtractor | None = None,
        payload_builder: MinecraftEquipPayloadBuilder | None = None,
    ):
        self.candidate_selector = candidate_selector or MinecraftEquipCandidateSelector()
        self.item_phrase_normalizer = (
            item_phrase_normalizer or MinecraftItemPhraseNormalizer()
        )
        self.request_extractor = request_extractor or MinecraftEquipRequestExtractor()
        self.payload_builder = payload_builder or MinecraftEquipPayloadBuilder()

    def parse(self, text: str, lowered: str) -> Dict[str, object] | None:
        candidate = self.candidate_selector.select(text, lowered)
        if not candidate:
            return None

        candidate = self.item_phrase_normalizer.normalize(candidate)
        request = self.request_extractor.extract(candidate)
        if request is None:
            return None
        return self.payload_builder.build(text, request)
