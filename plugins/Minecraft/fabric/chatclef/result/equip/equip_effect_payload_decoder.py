#20260915_kpopmodder: Validate bounded EQUIP wire observations and native all-target/any-match semantics.
from collections.abc import Mapping
import re

from .equip_effect_payload import EquipEffectPayload


class EquipEffectPayloadDecoder:
    SLOTS = ("head", "chest", "legs", "feet", "offhand")
    MAX_TARGETS = 32
    MAX_MATCHES = 32
    _ID = re.compile(r"[a-z0-9_.-]+:[a-z0-9_./-]+\Z", re.ASCII)
    _REASON = re.compile(r"[A-Za-z0-9_]{1,128}\Z", re.ASCII)
    _TASK_ID = re.compile(r"[0-9a-f]{1,16}\Z", re.ASCII)
    _KEYS = frozenset({
        "command", "request_id", "session_id", "server_connection_generation",
        "java_socket_generation", "task_identity", "observation_source", "quantity_semantics",
        "targets", "before", "after", "binding_valid", "effect_observation_status",
        "effect_observation_reason", "before_satisfied", "after_satisfied",
    })

    @classmethod
    def decode(cls, data: object) -> EquipEffectPayload | None:
        if (not isinstance(data, Mapping) or data.get("effect_kind") != "equip_slots"
                or data.get("effect_profile_id") != "equip_armor_slots_v1"
                or type(data.get("effect_profile_version")) is not int
                or data["effect_profile_version"] != 1):
            return None
        value = data.get("effect_payload")
        if not isinstance(value, Mapping) or set(value) != cls._KEYS:
            return None
        if (value["observation_source"] != "minecraft_client_equipment_slots"
                or value["quantity_semantics"] != "all_targets_any_match_slot_presence"
                or value["binding_valid"] is not True
                or not cls._text(value["command"], 4096)
                or not value["command"].startswith("equip ")
                or any(not cls._text(value[key], 256) for key in ("request_id", "session_id", "task_identity"))
                or cls._TASK_ID.fullmatch(value["task_identity"]) is None
                or any(not cls._positive_int(value[key]) for key in (
                    "server_connection_generation", "java_socket_generation"))
                or not cls._reason(value["effect_observation_reason"])):
            return None
        targets = cls._targets(value["targets"])
        before = cls._snapshot(value["before"])
        after = cls._snapshot(value["after"])
        if targets is None or before is None or after is None or after[0] < before[0]:
            return None
        before_satisfied = cls._satisfied(targets, before[1])
        after_satisfied = cls._satisfied(targets, after[1])
        for key, expected in (("before_satisfied", before_satisfied), ("after_satisfied", after_satisfied)):
            actual = value[key]
            if (type(actual) is not list or any(type(item) is not bool for item in actual)
                    or tuple(actual) != expected):
                return None
        if all(after_satisfied):
            outcome = "already_satisfied" if all(before_satisfied) and before[1] == after[1] else "satisfied"
        else:
            outcome = "partial" if any(after_satisfied) else "not_satisfied"
        if value["effect_observation_status"] != outcome:
            return None
        reasons = {"satisfied": "all_targets_equipped", "already_satisfied": "already_equipped",
                   "partial": "some_targets_not_equipped", "not_satisfied": "no_targets_equipped"}
        if value["effect_observation_reason"] != reasons[outcome]:
            return None
        return EquipEffectPayload(
            value["command"], value["request_id"], value["session_id"],
            value["server_connection_generation"], value["java_socket_generation"], value["task_identity"],
            outcome, value["effect_observation_reason"], before[0], after[0],
            targets, before[1], after[1], before_satisfied, after_satisfied,
        )

    @classmethod
    def _targets(cls, value):
        if type(value) is not list or not 1 <= len(value) <= cls.MAX_TARGETS:
            return None
        targets = []
        for index, target in enumerate(value):
            if (not isinstance(target, Mapping) or set(target) != {"index", "native_target", "requested_count", "matches"}
                    or type(target["index"]) is not int or target["index"] != index
                    or not cls._text(target["native_target"], 256)
                    or not cls._positive_int(target["requested_count"])):
                return None
            matches = target["matches"]
            if type(matches) is not list or not 1 <= len(matches) <= cls.MAX_MATCHES:
                return None
            frozen = []
            for match in matches:
                if (not isinstance(match, Mapping) or set(match) != {"item_id", "slot"}
                        or not cls._item_id(match["item_id"]) or match["item_id"] == "minecraft:air"
                        or type(match["slot"]) is not str or match["slot"] not in cls.SLOTS):
                    return None
                pair = (match["item_id"], match["slot"])
                if pair in frozen:
                    return None
                frozen.append(pair)
            targets.append((index, target["requested_count"], tuple(frozen), target["native_target"]))
        return tuple(targets)

    @classmethod
    def _snapshot(cls, value):
        if (not isinstance(value, Mapping) or set(value) != {"available", "reason", "observed_at_ms", "slots"}
                or value["available"] is not True or value["reason"] != "available"
                or type(value["observed_at_ms"]) is not int or not 0 <= value["observed_at_ms"] <= 9223372036854775807
                or type(value["slots"]) is not list or len(value["slots"]) != len(cls.SLOTS)):
            return None
        slots = {}
        for entry in value["slots"]:
            if (not isinstance(entry, Mapping) or set(entry) != {"slot", "item_id", "count"}
                    or type(entry["slot"]) is not str or entry["slot"] not in cls.SLOTS
                    or entry["slot"] in slots or not cls._item_id(entry["item_id"])
                    or type(entry["count"]) is not int or not 0 <= entry["count"] <= 2147483647
                    or (entry["item_id"] == "minecraft:air") != (entry["count"] == 0)):
                return None
            slots[entry["slot"]] = (entry["slot"], entry["item_id"], entry["count"])
        return value["observed_at_ms"], tuple(slots[slot] for slot in cls.SLOTS)

    @staticmethod
    def _satisfied(targets, slots):
        occupied = {(item_id, slot) for slot, item_id, count in slots if count > 0}
        return tuple(any(match in occupied for match in target[2]) for target in targets)

    @staticmethod
    def _positive_int(value):
        return type(value) is int and 1 <= value <= 2147483647

    @staticmethod
    def _text(value, limit):
        return (type(value) is str and 0 < len(value) <= limit and value == value.strip()
                and all(32 <= ord(char) < 127 for char in value))

    @classmethod
    def _item_id(cls, value):
        return type(value) is str and len(value) <= 256 and cls._ID.fullmatch(value) is not None

    @classmethod
    def _reason(cls, value):
        return type(value) is str and cls._REASON.fullmatch(value) is not None
