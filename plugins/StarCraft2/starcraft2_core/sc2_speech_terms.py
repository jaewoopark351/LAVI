#20260710_kpopmodder: StarCraft II speak-name mapping keeps GPT-SoVITS from spelling SC2 tokens letter by letter.
from __future__ import annotations


SC2_UNIT_SPEAK_NAMES = {
    # Terran units
    "SCV": "건설로봇",
    "MARINE": "마린",
    "MARAUDER": "불곰",
    "REAPER": "사신",
    "GHOST": "유령",
    "HELLION": "화염차",
    "HELLBAT": "화염기갑병",
    "WIDOWMINE": "땅거미 지뢰",
    "SIEGETANK": "공성 전차",
    "CYCLONE": "사이클론",
    "THOR": "토르",
    "VIKINGFIGHTER": "바이킹",
    "VIKINGASSAULT": "바이킹 돌격 모드",
    "MEDIVAC": "의료선",
    "LIBERATOR": "해방선",
    "RAVEN": "밤까마귀",
    "BANSHEE": "밴시",
    "BATTLECRUISER": "전투순양함",
    # Terran buildings
    "COMMANDCENTER": "사령부",
    "ORBITALCOMMAND": "궤도 사령부",
    "PLANETARYFORTRESS": "행성 요새",
    "SUPPLYDEPOT": "보급고",
    "BARRACKS": "병영",
    "ENGINEERINGBAY": "공학 연구소",
    "REFINERY": "정제소",
    "BUNKER": "벙커",
    "FACTORY": "군수공장",
    "ARMORY": "무기고",
    "STARPORT": "우주공항",
    "FUSIONCORE": "융합로",
    "MISSILETURRET": "미사일 포탑",
    # Zerg units
    "DRONE": "일벌레",
    "OVERLORD": "대군주",
    "OVERSEER": "감시군주",
    "QUEEN": "여왕",
    "ZERGLING": "저글링",
    "BANELING": "맹독충",
    "ROACH": "바퀴",
    "RAVAGER": "궤멸충",
    "HYDRALISK": "히드라리스크",
    "LURKER": "가시지옥",
    "MUTALISK": "뮤탈리스크",
    "CORRUPTOR": "타락귀",
    "BROODLORD": "무리 군주",
    "INFESTOR": "감염충",
    "SWARMHOSTMP": "군단 숙주",
    "VIPER": "살모사",
    "ULTRALISK": "울트라리스크",
    # Zerg buildings
    "HATCHERY": "부화장",
    "LAIR": "번식지",
    "HIVE": "군락",
    "EXTRACTOR": "추출장",
    "SPAWNINGPOOL": "산란못",
    "EVOLUTIONCHAMBER": "진화장",
    "ROACHWARREN": "바퀴 소굴",
    "BANELINGNEST": "맹독충 둥지",
    "SPINECRAWLER": "가시 촉수",
    "SPORECRAWLER": "포자 촉수",
    "HYDRALISKDEN": "히드라리스크 굴",
    "LURKERDENMP": "가시지옥 굴",
    "SPIRE": "둥지탑",
    "GREATERSPIRE": "거대 둥지탑",
    "INFESTATIONPIT": "감염 구덩이",
    "ULTRALISKCAVERN": "울트라리스크 동굴",
    # Protoss units
    "PROBE": "탐사정",
    "ZEALOT": "광전사",
    "STALKER": "추적자",
    "SENTRY": "파수기",
    "ADEPT": "사도",
    "HIGHTEMPLAR": "고위 기사",
    "DARKTEMPLAR": "암흑 기사",
    "ARCHON": "집정관",
    "IMMORTAL": "불멸자",
    "COLOSSUS": "거신",
    "DISRUPTOR": "분열기",
    "OBSERVER": "관측선",
    "WARPPRISM": "차원 분광기",
    "PHOENIX": "불사조",
    "VOIDRAY": "공허 포격기",
    "ORACLE": "예언자",
    "TEMPEST": "폭풍함",
    "CARRIER": "우주모함",
    "MOTHERSHIP": "모선",
    # Protoss buildings
    "NEXUS": "연결체",
    "PYLON": "수정탑",
    "ASSIMILATOR": "융화소",
    "GATEWAY": "관문",
    "WARPGATE": "차원 관문",
    "FORGE": "제련소",
    "CYBERNETICSCORE": "인공제어소",
    "TWILIGHTCOUNCIL": "황혼 의회",
    "ROBOTICSFACILITY": "로봇공학 시설",
    "STARGATE": "우주관문",
    "TEMPLARARCHIVE": "기사단 기록보관소",
    "DARKSHRINE": "암흑 성소",
    "ROBOTICSBAY": "로봇공학 지원소",
    "FLEETBEACON": "함대 신호소",
    "PHOTONCANNON": "광자포",
    "SHIELDBATTERY": "보호막 충전소",
}


SC2_UPGRADE_SPEAK_NAMES = {
    "ZERGLINGMOVEMENTSPEED": "저글링 발업",
    "ZERGLINGATTACKSPEED": "저글링 공속업",
    "GLIALRECONSTITUTION": "바퀴 발업",
    "TUNNELINGCLAWS": "잠복 발톱",
    "CENTRIFICALHOOKS": "맹독충 원심 고리",
    "EVOLVEGROOVEDSPINES": "히드라 사거리업",
    "EVOLVEMUSCULARAUGMENTS": "히드라 속업",
    "CHITINOUSPLATING": "울트라 방업",
    "ANABOLICSYNTHESIS": "울트라 속업",
    "STIMPACK": "전투 자극제",
    "SHIELDWALL": "전투 방패",
    "PUNISHERGRENADES": "충격탄",
    "TERRANINFANTRYWEAPONSLEVEL1": "보병 공업 1단계",
    "TERRANINFANTRYARMORSLEVEL1": "보병 방업 1단계",
    "WARPGATERESEARCH": "차원 관문 연구",
    "BLINKTECH": "점멸",
    "CHARGE": "돌진",
    "PSISTORMTECH": "사이오닉 폭풍",
}


SC2_STRATEGY_SPEAK_NAMES = {
    "BOTMODE.HATCHPOOLHATCHGAS": "해처리 풀 해처리 가스 빌드",
    "HATCHPOOLHATCHGAS": "해처리 풀 해처리 가스 빌드",
    "BOTMODE.HATCHGASPOOLEXTRACTORTRICK": "부화장-가스-산란못 추출장 트릭 빌드",
    "HATCHGASPOOLEXTRACTORTRICK": "부화장-가스-산란못 추출장 트릭 빌드",
    "ADAPTIVE": "적응형 운영",
    "LING_BANE": "저글링 맹독충 조합",
    "ROACH_RAVAGER": "바퀴 궤멸충 조합",
    "HYDRA_LURKER": "히드라 가시지옥 조합",
    "MUTALISK": "뮤탈리스크 운영",
}


def sc2_unit_speak_name(raw_name: str) -> str:
    return _lookup_speak_name(raw_name, SC2_UNIT_SPEAK_NAMES)


def sc2_upgrade_speak_name(raw_name: str) -> str:
    return _lookup_speak_name(raw_name, SC2_UPGRADE_SPEAK_NAMES)


def sc2_strategy_speak_name(raw_name: str) -> str:
    return _lookup_speak_name(raw_name, SC2_STRATEGY_SPEAK_NAMES)


def sc2_speak_name(raw_name: str) -> str:
    for mapping in (
        SC2_UNIT_SPEAK_NAMES,
        SC2_UPGRADE_SPEAK_NAMES,
        SC2_STRATEGY_SPEAK_NAMES,
    ):
        value = _lookup_speak_name(raw_name, mapping, default="")
        if value:
            return value
    return str(raw_name or "").replace("_", " ")


def _lookup_speak_name(raw_name: str, mapping: dict[str, str], default: str | None = None) -> str:
    text = str(raw_name or "").strip()
    #20260711_kpopmodder: Accept unit/upgrade/strategy identifiers regardless
    # of input or dictionary key casing.
    normalized_mapping = {
        str(key).replace(" ", "").replace("_", "").upper(): value
        for key, value in mapping.items()
    }
    for candidate in _lookup_candidates(text):
        normalized_candidate = candidate.replace(" ", "").replace("_", "").upper()
        if normalized_candidate in normalized_mapping:
            return normalized_mapping[normalized_candidate]
    if default is not None:
        return default
    return text.replace("_", " ")


def _lookup_candidates(text: str) -> tuple[str, ...]:
    compact = text.replace(" ", "").strip()
    no_prefix = compact.rsplit(".", 1)[-1]
    return (
        compact.upper(),
        no_prefix.upper(),
        no_prefix.replace("_", "").upper(),
        compact.replace("_", "").upper(),
    )
