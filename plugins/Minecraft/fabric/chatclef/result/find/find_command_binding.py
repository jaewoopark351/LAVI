#20260914_kpopmodder: Bind FIND feedback to one exact admitted request meaning.
from __future__ import annotations

from dataclasses import dataclass
import hashlib

from plugins.Minecraft.fabric.chatclef.intent.find.find_command_compiler import FindCommandCompiler
from plugins.Minecraft.fabric.chatclef.transport.find_catalog.find_catalog_snapshot import DIGEST, safe_text


@dataclass(frozen=True, slots=True)
class FindCommandBinding:
    target_kind: str
    target: str
    mode: str
    catalog_digest: str = ""
    resource_generation: int = -1
    session_id: str = ""
    connection_generation: int = -1

    def __post_init__(self):
        FindCommandCompiler.compile_slots(self.target_kind, self.target, self.mode)
        if self.catalog_digest and (type(self.catalog_digest) is not str or DIGEST.fullmatch(self.catalog_digest) is None
                or type(self.resource_generation) is not int or not 0 <= self.resource_generation <= 2**63 - 1
                or not safe_text(self.session_id, 128)
                or type(self.connection_generation) is not int or self.connection_generation < 0):
            raise ValueError("invalid_find_catalog_binding")

    @property
    def command(self):
        return FindCommandCompiler.compile_slots(self.target_kind, self.target, self.mode)

    @property
    def player_identity_digest(self):
        return hashlib.sha256(self.target.encode("utf-8")).hexdigest() if self.target_kind == "player" else ""

    @classmethod
    def from_translation(cls, translation: object):
        if type(translation) is not dict:
            return None
        intent, data = translation.get("intent"), translation.get("data")
        if type(intent) is not dict or type(data) is not dict or intent.get("intent_type") != "find" or intent.get("source") != "rule":
            return None
        from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_schema_validator import ChatClefIntentSchemaValidator
        if ChatClefIntentSchemaValidator().validate(intent)[0] is not True:
            return None
        slots = intent.get("slots")
        if type(slots) is not dict or set(slots) != {"target_kind", "target_phrase", "mode"}:
            return None
        try:
            binding = cls(slots["target_kind"], translation["resolved_target"], slots["mode"],
                data.get("find_catalog_digest", ""), data.get("find_resource_generation", -1),
                data.get("find_session_id", ""), data.get("find_connection_generation", -1))
        except (KeyError, TypeError, ValueError):
            return None
        if binding.target_kind != "player" and not binding.catalog_digest:
            return None
        return binding if translation.get("command") == binding.command else None

    @classmethod
    def from_command(cls, command: object):
        if type(command) is not str:
            return None
        units = command.removeprefix("@").split()
        if len(units) not in {3, 4} or units[0] != "find":
            return None
        try:
            return cls(units[1], units[2], units[3] if len(units) == 4 else "report")
        except (TypeError, ValueError):
            return None
