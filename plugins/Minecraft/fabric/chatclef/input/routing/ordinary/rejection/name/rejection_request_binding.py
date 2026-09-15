#20260915_kpopmodder: Bind a non-executable name rejection to one live trusted deterministic request.
from collections.abc import Mapping
import re

from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_dto import ChatClefIntentDTO
from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_schema_validator import ChatClefIntentSchemaValidator
from plugins.Minecraft.fabric.chatclef.intent.korean_chatclef_rule_parser import KoreanChatClefRuleParser


class RejectionRequestBinding:
    @staticmethod
    def inspect(translation, command_text, trusted_scope_live):
        if trusted_scope_live is not True or not isinstance(translation, Mapping):
            return None
        if (type(translation.get("status")) is not str
                or translation.get("status") not in {"unknown", "unsupported", "ambiguous"}
                or translation.get("executable") is not False or translation.get("command") is not None):
            return None
        data = translation.get("data")
        resolution = data.get("resolution") if isinstance(data, Mapping) else None
        if not isinstance(resolution, Mapping):
            return None
        try:
            if not ChatClefIntentSchemaValidator().validate(translation.get("intent"))[0]:
                return None
            intent = ChatClefIntentDTO.from_mapping(translation.get("intent"))
            parsed = KoreanChatClefRuleParser().parse(command_text)
            if intent.to_dict() != parsed.to_dict() or intent.source != "rule" or intent.language != "ko":
                return None
        except (ValueError, TypeError, KeyError):
            return None
        return intent, resolution, data

    @staticmethod
    def has_catalogue(data):
        catalogue = data.get("runtime_catalogue") if isinstance(data, Mapping) else None
        return (isinstance(catalogue, Mapping) and type(catalogue.get("session_id")) is str
                and bool(catalogue["session_id"]) and type(catalogue.get("catalogue_sha256")) is str
                and re.fullmatch(r"[0-9a-f]{64}", catalogue["catalogue_sha256"]) is not None)
