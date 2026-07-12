#20260711_kpopmodder: Centralized SC2 telemetry IDs so every configured speech
# name can be reached from native unit and upgrade observations.
"""Canonical StarCraft II telemetry type registry.

The numeric values mirror the repository-local Sharky ``UnitTypes.cs`` and
``Upgrades.cs`` enums.  StarCraft II calls buildings "unit types" too, so this
module keeps an explicit unit/building category alongside the shared ID map.
"""
from __future__ import annotations

from typing import Any, Optional

from .sc2_speech_terms import SC2_UNIT_SPEAK_NAMES, SC2_UPGRADE_SPEAK_NAMES


SC2_UNIT_CATEGORY = "unit"
SC2_BUILDING_CATEGORY = "building"


# Units, workers, and army forms from SC2_UNIT_SPEAK_NAMES (54 entries).
SC2_NON_BUILDING_UNIT_TYPE_ID_BY_TOKEN = {
    # Terran
    "SCV": 45,
    "MARINE": 48,
    "MARAUDER": 51,
    "REAPER": 49,
    "GHOST": 50,
    "HELLION": 53,
    # HELLBAT is named TERRAN_HELLIONTANK in UnitTypes.cs.
    "HELLBAT": 484,
    "WIDOWMINE": 498,
    "SIEGETANK": 33,
    "CYCLONE": 692,
    "THOR": 52,
    "VIKINGFIGHTER": 35,
    "VIKINGASSAULT": 34,
    "MEDIVAC": 54,
    "LIBERATOR": 689,
    "RAVEN": 56,
    "BANSHEE": 55,
    "BATTLECRUISER": 57,
    # Zerg
    "DRONE": 104,
    "OVERLORD": 106,
    "OVERSEER": 129,
    "QUEEN": 126,
    "ZERGLING": 105,
    "BANELING": 9,
    "ROACH": 110,
    "RAVAGER": 688,
    "HYDRALISK": 107,
    # LURKER is named ZERG_LURKERMP in UnitTypes.cs.
    "LURKER": 502,
    "MUTALISK": 108,
    "CORRUPTOR": 112,
    "BROODLORD": 114,
    "INFESTOR": 111,
    "SWARMHOSTMP": 494,
    "VIPER": 499,
    "ULTRALISK": 109,
    # Protoss
    "PROBE": 84,
    "ZEALOT": 73,
    "STALKER": 74,
    "SENTRY": 77,
    "ADEPT": 311,
    "HIGHTEMPLAR": 75,
    "DARKTEMPLAR": 76,
    "ARCHON": 141,
    "IMMORTAL": 83,
    "COLOSSUS": 4,
    "DISRUPTOR": 694,
    "OBSERVER": 82,
    "WARPPRISM": 81,
    "PHOENIX": 78,
    "VOIDRAY": 80,
    "ORACLE": 495,
    "TEMPEST": 496,
    "CARRIER": 79,
    "MOTHERSHIP": 10,
}


# Structures from SC2_UNIT_SPEAK_NAMES (45 entries).
SC2_BUILDING_TYPE_ID_BY_TOKEN = {
    # Terran
    "COMMANDCENTER": 18,
    "ORBITALCOMMAND": 132,
    "PLANETARYFORTRESS": 130,
    "SUPPLYDEPOT": 19,
    "BARRACKS": 21,
    "ENGINEERINGBAY": 22,
    "REFINERY": 20,
    "BUNKER": 24,
    "FACTORY": 27,
    "ARMORY": 29,
    "STARPORT": 28,
    "FUSIONCORE": 30,
    "MISSILETURRET": 23,
    # Zerg
    "HATCHERY": 86,
    "LAIR": 100,
    "HIVE": 101,
    "EXTRACTOR": 88,
    "SPAWNINGPOOL": 89,
    "EVOLUTIONCHAMBER": 90,
    "ROACHWARREN": 97,
    "BANELINGNEST": 96,
    "SPINECRAWLER": 98,
    "SPORECRAWLER": 99,
    "HYDRALISKDEN": 91,
    "LURKERDENMP": 504,
    "SPIRE": 92,
    "GREATERSPIRE": 102,
    "INFESTATIONPIT": 94,
    "ULTRALISKCAVERN": 93,
    # Protoss
    "NEXUS": 59,
    "PYLON": 60,
    "ASSIMILATOR": 61,
    "GATEWAY": 62,
    "WARPGATE": 133,
    "FORGE": 63,
    "CYBERNETICSCORE": 72,
    "TWILIGHTCOUNCIL": 65,
    "ROBOTICSFACILITY": 71,
    "STARGATE": 67,
    "TEMPLARARCHIVE": 68,
    "DARKSHRINE": 69,
    "ROBOTICSBAY": 70,
    "FLEETBEACON": 64,
    "PHOTONCANNON": 66,
    "SHIELDBATTERY": 1910,
}


# Exactly the 99 entries configured in SC2_UNIT_SPEAK_NAMES.
SC2_UNIT_TYPE_ID_BY_TOKEN = {
    **SC2_NON_BUILDING_UNIT_TYPE_ID_BY_TOKEN,
    **SC2_BUILDING_TYPE_ID_BY_TOKEN,
}
SC2_UNIT_TYPE_TOKEN_BY_ID = {
    unit_type_id: token
    for token, unit_type_id in SC2_UNIT_TYPE_ID_BY_TOKEN.items()
}


# Legacy transient Zerg types remain resolvable for logging/silencing, but are
# deliberately separate from the 99-entry speech dictionary registry.
SC2_LEGACY_UNIT_TYPE_ID_BY_TOKEN = {
    "EGG": 103,
    "LARVA": 151,
}
SC2_LEGACY_UNIT_TYPE_TOKEN_BY_ID = {
    unit_type_id: token
    for token, unit_type_id in SC2_LEGACY_UNIT_TYPE_ID_BY_TOKEN.items()
}
SC2_LEGACY_UNIT_SPEAK_NAMES = {
    "EGG": "알",
    "LARVA": "애벌레",
}


# Merged telemetry maps include the two legacy transient types for runtime use.
SC2_TELEMETRY_UNIT_TYPE_ID_BY_TOKEN = {
    **SC2_UNIT_TYPE_ID_BY_TOKEN,
    **SC2_LEGACY_UNIT_TYPE_ID_BY_TOKEN,
}
SC2_TELEMETRY_UNIT_TYPE_TOKEN_BY_ID = {
    unit_type_id: token
    for token, unit_type_id in SC2_TELEMETRY_UNIT_TYPE_ID_BY_TOKEN.items()
}

SC2_UNIT_TOKENS = frozenset(SC2_NON_BUILDING_UNIT_TYPE_ID_BY_TOKEN)
SC2_BUILDING_TOKENS = frozenset(SC2_BUILDING_TYPE_ID_BY_TOKEN)
SC2_UNIT_TYPE_IDS = frozenset(SC2_NON_BUILDING_UNIT_TYPE_ID_BY_TOKEN.values())
SC2_BUILDING_UNIT_TYPE_IDS = frozenset(SC2_BUILDING_TYPE_ID_BY_TOKEN.values())
SC2_LEGACY_UNIT_TYPE_IDS = frozenset(SC2_LEGACY_UNIT_TYPE_ID_BY_TOKEN.values())
SC2_SPEAKABLE_UNIT_TYPE_IDS = frozenset(SC2_UNIT_TYPE_ID_BY_TOKEN.values())
SC2_TELEMETRY_UNIT_TYPE_IDS = frozenset(
    SC2_TELEMETRY_UNIT_TYPE_ID_BY_TOKEN.values()
)


# Upgrade IDs from Upgrades.cs (all 18 SC2_UPGRADE_SPEAK_NAMES entries).
SC2_UPGRADE_ID_BY_TOKEN = {
    "ZERGLINGMOVEMENTSPEED": 66,
    "ZERGLINGATTACKSPEED": 65,
    "GLIALRECONSTITUTION": 2,
    "TUNNELINGCLAWS": 3,
    "CENTRIFICALHOOKS": 75,
    "EVOLVEGROOVEDSPINES": 134,
    "EVOLVEMUSCULARAUGMENTS": 135,
    "CHITINOUSPLATING": 4,
    "ANABOLICSYNTHESIS": 88,
    "STIMPACK": 15,
    "SHIELDWALL": 16,
    "PUNISHERGRENADES": 17,
    "TERRANINFANTRYWEAPONSLEVEL1": 7,
    "TERRANINFANTRYARMORSLEVEL1": 11,
    "WARPGATERESEARCH": 84,
    "BLINKTECH": 87,
    "CHARGE": 86,
    "PSISTORMTECH": 52,
}
SC2_UPGRADE_TOKEN_BY_ID = {
    upgrade_id: token
    for token, upgrade_id in SC2_UPGRADE_ID_BY_TOKEN.items()
}
SC2_UPGRADE_IDS = frozenset(SC2_UPGRADE_TOKEN_BY_ID)


_UNIT_TOKEN_ALIASES = {
    "HELLIONTANK": "HELLBAT",
    "LURKERMP": "LURKER",
}


def canonical_unit_token(value: Any) -> Optional[str]:
    """Return the speech dictionary token for a telemetry ID or token."""
    type_id = _coerce_numeric_id(value)
    if type_id is not None:
        return SC2_TELEMETRY_UNIT_TYPE_TOKEN_BY_ID.get(type_id)

    for candidate in _identifier_candidates(value):
        candidate = _UNIT_TOKEN_ALIASES.get(candidate, candidate)
        if candidate in SC2_TELEMETRY_UNIT_TYPE_ID_BY_TOKEN:
            return candidate
    return None


def unit_type_id(value: Any) -> Optional[int]:
    """Return the numeric telemetry unit type ID for an ID or known token."""
    token = canonical_unit_token(value)
    if token is None:
        return None
    return SC2_TELEMETRY_UNIT_TYPE_ID_BY_TOKEN[token]


def unit_category(value: Any) -> Optional[str]:
    """Return ``unit`` or ``building`` for a known telemetry type."""
    token = canonical_unit_token(value)
    if token in SC2_BUILDING_TOKENS:
        return SC2_BUILDING_CATEGORY
    if token in SC2_UNIT_TOKENS or token in SC2_LEGACY_UNIT_TYPE_ID_BY_TOKEN:
        return SC2_UNIT_CATEGORY
    return None


def unit_speak_name(value: Any, default: Optional[str] = None) -> str:
    """Return a Korean TTS name for a telemetry ID or canonical token."""
    token = canonical_unit_token(value)
    if token in SC2_UNIT_SPEAK_NAMES:
        return SC2_UNIT_SPEAK_NAMES[token]
    if token in SC2_LEGACY_UNIT_SPEAK_NAMES:
        return SC2_LEGACY_UNIT_SPEAK_NAMES[token]
    if default is not None:
        return default
    return str(value or "").replace("_", " ")


def is_unit_type(value: Any) -> bool:
    return unit_category(value) == SC2_UNIT_CATEGORY


def is_building_type(value: Any) -> bool:
    return unit_category(value) == SC2_BUILDING_CATEGORY


def canonical_upgrade_token(value: Any) -> Optional[str]:
    """Return the speech dictionary token for an upgrade ID or token."""
    upgrade_id = _coerce_numeric_id(value)
    if upgrade_id is not None:
        return SC2_UPGRADE_TOKEN_BY_ID.get(upgrade_id)

    for candidate in _identifier_candidates(value):
        if candidate in SC2_UPGRADE_ID_BY_TOKEN:
            return candidate
    return None


def upgrade_id(value: Any) -> Optional[int]:
    """Return the numeric upgrade ID for an ID or known token."""
    token = canonical_upgrade_token(value)
    if token is None:
        return None
    return SC2_UPGRADE_ID_BY_TOKEN[token]


def upgrade_speak_name(value: Any, default: Optional[str] = None) -> str:
    """Return a Korean TTS name for an upgrade ID or canonical token."""
    token = canonical_upgrade_token(value)
    if token in SC2_UPGRADE_SPEAK_NAMES:
        return SC2_UPGRADE_SPEAK_NAMES[token]
    if default is not None:
        return default
    return str(value or "").replace("_", " ")


def _coerce_numeric_id(value: Any) -> Optional[int]:
    if isinstance(value, bool):
        return None
    if isinstance(value, int):
        return value
    text = str(value or "").strip()
    if text.startswith("+"):
        text = text[1:]
    if text.isdigit():
        return int(text)
    return None


def _identifier_candidates(value: Any) -> tuple[str, ...]:
    text = str(value or "").strip().upper()
    enum_name = text.rsplit(".", 1)[-1]
    candidates = [_normalize_identifier(enum_name)]
    for prefix in ("TERRAN_", "ZERG_", "PROTOSS_"):
        if enum_name.startswith(prefix):
            candidates.append(_normalize_identifier(enum_name[len(prefix):]))
    return tuple(dict.fromkeys(candidate for candidate in candidates if candidate))


def _normalize_identifier(value: str) -> str:
    return "".join(character for character in value if character.isalnum())


def _validate_registry() -> None:
    configured_unit_tokens = set(SC2_UNIT_SPEAK_NAMES)
    registered_unit_tokens = set(SC2_UNIT_TYPE_ID_BY_TOKEN)
    if registered_unit_tokens != configured_unit_tokens:
        missing = sorted(configured_unit_tokens - registered_unit_tokens)
        extra = sorted(registered_unit_tokens - configured_unit_tokens)
        raise RuntimeError(
            f"SC2 unit telemetry registry mismatch: missing={missing}, extra={extra}"
        )
    if len(SC2_UNIT_TYPE_TOKEN_BY_ID) != len(SC2_UNIT_TYPE_ID_BY_TOKEN):
        raise RuntimeError("SC2 unit telemetry registry contains duplicate IDs")
    if SC2_UNIT_TOKENS & SC2_BUILDING_TOKENS:
        raise RuntimeError("SC2 unit/building telemetry categories overlap")
    if SC2_UNIT_TOKENS | SC2_BUILDING_TOKENS != configured_unit_tokens:
        raise RuntimeError("SC2 unit/building telemetry categories are incomplete")
    if set(SC2_UPGRADE_ID_BY_TOKEN) != set(SC2_UPGRADE_SPEAK_NAMES):
        raise RuntimeError("SC2 upgrade telemetry registry is incomplete")
    if len(SC2_UPGRADE_TOKEN_BY_ID) != len(SC2_UPGRADE_ID_BY_TOKEN):
        raise RuntimeError("SC2 upgrade telemetry registry contains duplicate IDs")


_validate_registry()


__all__ = [
    "SC2_UNIT_CATEGORY",
    "SC2_BUILDING_CATEGORY",
    "SC2_NON_BUILDING_UNIT_TYPE_ID_BY_TOKEN",
    "SC2_BUILDING_TYPE_ID_BY_TOKEN",
    "SC2_UNIT_TYPE_ID_BY_TOKEN",
    "SC2_UNIT_TYPE_TOKEN_BY_ID",
    "SC2_LEGACY_UNIT_TYPE_ID_BY_TOKEN",
    "SC2_LEGACY_UNIT_TYPE_TOKEN_BY_ID",
    "SC2_LEGACY_UNIT_SPEAK_NAMES",
    "SC2_TELEMETRY_UNIT_TYPE_ID_BY_TOKEN",
    "SC2_TELEMETRY_UNIT_TYPE_TOKEN_BY_ID",
    "SC2_UNIT_TOKENS",
    "SC2_BUILDING_TOKENS",
    "SC2_UNIT_TYPE_IDS",
    "SC2_BUILDING_UNIT_TYPE_IDS",
    "SC2_LEGACY_UNIT_TYPE_IDS",
    "SC2_SPEAKABLE_UNIT_TYPE_IDS",
    "SC2_TELEMETRY_UNIT_TYPE_IDS",
    "SC2_UPGRADE_ID_BY_TOKEN",
    "SC2_UPGRADE_TOKEN_BY_ID",
    "SC2_UPGRADE_IDS",
    "canonical_unit_token",
    "unit_type_id",
    "unit_category",
    "unit_speak_name",
    "is_unit_type",
    "is_building_type",
    "canonical_upgrade_token",
    "upgrade_id",
    "upgrade_speak_name",
]
