#20260915_kpopmodder: Own only source-bound Minecraft name failures; preserve ordinary conversation fallthrough.
from collections.abc import Mapping
import re

from plugins.Minecraft.fabric.chatclef.intent.names.korean_name_resolution_message import KoreanNameResolutionMessage
from .rejection_request_binding import RejectionRequestBinding
from .minecraft_request_evidence import MinecraftRequestEvidence


class MinecraftNameRejectionPolicy:
    _NAMED_INTENTS = frozenset({"get_item", "equip_item", "deposit_item", "deposit_all", "give_item", "attack", "scan"})
    _REASONS = {
        "unknown": frozenset({"unknown_item_phrase", "empty_item_phrase", "unknown_registered_item", "unknown_registered_target"}),
        "unsupported": frozenset({"unsupported_material", "unsupported_equipment_material", "native_command_target_unsupported", "runtime_catalogue_required", "target_not_equippable", "target_not_in_chatclef_catalog"}),
        "ambiguous": frozenset({"ambiguous_item_phrase", "ambiguous_registered_name", "ambiguous_native_command_token"}),
    }

    def __init__(self):
        self._evidence = MinecraftRequestEvidence()

    def message(self, translation, command_text, trusted_scope_live):
        bound = RejectionRequestBinding.inspect(translation, command_text, trusted_scope_live)
        if bound is None:
            return None
        intent, resolution, data = bound
        status, reason = translation["status"], translation.get("reason_code")
        if intent.intent_type.value not in self._NAMED_INTENTS or type(reason) is not str or reason not in self._REASONS[status]:
            return None
        target_failure = reason in {"target_not_equippable", "target_not_in_chatclef_catalog"}
        if target_failure:
            if resolution.get("status") != "validated" or type(resolution.get("target")) is not str:
                return None
        elif resolution.get("status") != status or resolution.get("reason_code") != reason or resolution.get("target") is not None:
            return None
        if reason in {"native_command_target_unsupported", "unknown_registered_item", "unknown_registered_target"} and not RejectionRequestBinding.has_catalogue(data):
            return None
        if reason in {"ambiguous_registered_name", "ambiguous_native_command_token"}:
            return KoreanNameResolutionMessage.ambiguous(resolution) if RejectionRequestBinding.has_catalogue(data) else None
        detail = resolution.get("data")
        identifier = detail.get("registry_id") if isinstance(detail, Mapping) else None
        registered = (reason == "native_command_target_unsupported" and RejectionRequestBinding.has_catalogue(data)
                      and type(identifier) is str and len(identifier) <= 256
                      and re.fullmatch(r"[a-z0-9_.-]+:[a-z0-9_./-]+", identifier) is not None)
        if not registered and not self._evidence.matches(intent, command_text):
            return None
        return KoreanNameResolutionMessage.failure(reason)
