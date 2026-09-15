#20260915_kpopmodder: Resolve one command's slot against actual native tokens and capabilities.
from __future__ import annotations

import re

from ..chatclef_intent_status import ChatClefIntentStatus as S


class CommandTargetResolver:
    def __init__(self, base_resolver, command, catalog=None):
        self._base = base_resolver
        self.command = command
        self.catalog = catalog

    def resolve(self, phrase):
        kind = {"attack": "entity", "scan": "block"}.get(self.command, "item")
        normalized = re.sub(r"[을를]$", "", str(phrase).strip()).strip()
        rows = self.catalog.matching(kind, normalized) if self.catalog is not None else ()
        if len(rows) > 1:
            return self._result(S.AMBIGUOUS, "ambiguous_registered_name", suggestions=[r["id"] for r in rows[:5]])
        if rows:
            return self._registered(rows[0], kind)
        if kind == "item":
            legacy = self._base.resolve(phrase)
            if legacy.get("status") == S.VALIDATED.value:
                #20260915_kpopmodder: Verified aliases still obey the actual ID's native capability.
                target = legacy.get("target")
                row = self.catalog.entries.get((kind, "minecraft:" + target)) if self.catalog is not None and type(target) is str else None
                if row is not None:
                    return self._registered(row, kind, equipment_alias=legacy.get("data", {}).get("equipment_alias"))
                matches = self.catalog.token_matches(kind, self.command, legacy.get("target")) if self.catalog is not None else ()
                if len(matches) > 1:
                    return self._result(S.AMBIGUOUS, "ambiguous_native_command_token",
                                        suggestions=[r["id"] for r in matches[:5]])
                return legacy
            return legacy if self.catalog is None else self._result(S.UNKNOWN, "unknown_registered_item")
        if self.catalog is None:
            return self._result(S.UNSUPPORTED, "runtime_catalogue_required")
        return self._result(S.UNKNOWN, "unknown_registered_target")

    def _registered(self, row, kind, **evidence):
        token = row["tokens"].get(self.command)
        if self.command not in row["capabilities"] or not token:
            return self._result(S.UNSUPPORTED, "native_command_target_unsupported", registry_id=row["id"], **evidence)
        matches = self.catalog.token_matches(kind, self.command, token)
        if len(matches) > 1:
            return self._result(S.AMBIGUOUS, "ambiguous_native_command_token",
                                suggestions=[r["id"] for r in matches[:5]], **evidence)
        return self._result(S.VALIDATED, "resolved_registered_command_target", target=token,
                            registry_id=row["id"], label=row["label"], native_capability=True, **evidence)

    def supports_equipment_target(self, target):
        # EquipArmorTask supports armor or its explicit shield path, not tools or arbitrary Equipment.
        if self.catalog is not None and any(row["tokens"].get("equip") == target and "equip" in row["capabilities"] for row in self.catalog.entries.values()):
            return True
        return bool(re.fullmatch(r"(?:leather|chainmail|iron|golden|diamond|netherite)_(?:helmet|chestplate|leggings|boots)|turtle_helmet|shield", str(target)))

    def native_capability(self, resolution):
        return resolution.get("data", {}).get("native_capability") is True

    @staticmethod
    def _result(status, reason, target=None, **data):
        return {"status": status.value, "reason_code": reason, "target": target, "data": data}
