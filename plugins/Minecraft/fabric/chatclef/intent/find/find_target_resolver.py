#20260914_kpopmodder: Resolve exact registry names and verified aliases without selecting ambiguity.
from __future__ import annotations

import re
import unicodedata

from plugins.Minecraft.fabric.chatclef.transport.find_catalog import FindCatalogSnapshot
from .find_command_compiler import PLAYER_NAME


class FindTargetResolver:
    _ALIASES = {("entity", "minecraft:villager"): ("마을 주민", "마을주민")}

    @staticmethod
    def _key(value: str) -> str:
        return re.sub(r"\s+", "", unicodedata.normalize("NFC", value)).casefold()

    def resolve(self, request: object, snapshot: object):
        if request.target_kind == "player":
            if PLAYER_NAME.fullmatch(request.target_phrase) is None:
                return "invalid_find_player", ()
            return "validated", (("player", request.target_phrase, "요청한 플레이어"),)
        if type(snapshot) is not FindCatalogSnapshot:
            return "find_catalog_unavailable", ()
        key = self._key(request.target_phrase)
        candidates = []
        for record in snapshot.records:
            if request.target_kind and request.target_kind != record.target_kind:
                continue
            if record.target_kind == "entity" and record.eligibility == "non_mob":
                continue
            labels = (record.canonical_target_id, record.korean_name, record.english_name,
                      *self._ALIASES.get((record.target_kind, record.canonical_target_id), ()))
            if any(label and self._key(label) == key for label in labels):
                label = record.korean_name or "요청한 대상"
                candidates.append((record.target_kind, record.canonical_target_id, label))
        candidates.sort(key=lambda candidate: (candidate[0], candidate[1]))
        if not candidates:
            return "find_target_unresolved", ()
        if len(candidates) != 1:
            return "find_target_ambiguous", tuple(candidates[:3])
        return "validated", tuple(candidates)
