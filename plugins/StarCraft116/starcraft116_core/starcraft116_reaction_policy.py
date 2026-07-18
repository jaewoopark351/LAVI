#20260703_kpopmodder: Builds concise LLM/TTS reactions from StarCraft 1.16 status snapshots.
import hashlib
import json
import re
import time

from llm_core.speech_style import build_game_reaction_system_prompt

from .starcraft116_reaction_units import (
    find_starcraft116_unit_mention as _find_starcraft116_unit_mention,
    starcraft116_base_type as _starcraft116_base_type,
    starcraft116_object as _starcraft116_object,
    starcraft116_subject as _starcraft116_subject,
)


_STARCRAFT116_REACTION_BASE_SYSTEM_PROMPT = (
    "You are LAV's Korean AI VTuber reacting to StarCraft 1.16 BWAPI status.\n"
    "Return exactly one short Korean sentence.\n"
    "Do not include JSON, process IDs, file paths, or long analysis.\n"
    "For game_event, speak as if you are the one playing through Stardust/BWAPI, "
    "not as a spectator coaching the user.\n"
    "If StarCraft is running with BWAPI, sound pleased and a little sharp.\n"
    "If there is a warning or error, state the problem directly.\n"
    "If source is game_event, react only to the in-game facts provided by "
    "the BWAPI exporter.\n"
    "For combat, enemy_spotted, or under_attack events, sound alert and sharp.\n"
    "For worker, resource, build, or production events, make a brief macro "
    "comment without pretending to know unseen strategy.\n"
    "For building_started, say the building started or is being built; "
    "do not say it is complete.\n"
    "Use unit/building names exactly as provided in allowed_unit_names; "
    "those are TTS speak names, so never translate, romanize, or invent them.\n"
    "Keep the rest of the sentence natural Korean.\n"
    "Use natural StarCraft Korean: say '자원 상태' instead of '경제', "
    "'인구수' instead of '공급', and '완성' instead of '완공'.\n"
    "Use active first-person wording: say '내가 잘하고 있어' instead of "
    "'잘하고 있어', '조심해야겠다' instead of '조심해', and "
    "'주의해야 해' instead of '주의해'.\n"
    "When our unit is attacking an enemy building, say it is breaking or "
    "destroying the building, not fighting with the building.\n"
    "Never claim the bot is playing well unless the event only says the game "
    "is running; this status does not include strategy or map control."
)


def build_starcraft116_reaction_system_prompt(speech_style_source=None):
    return build_game_reaction_system_prompt(
        _STARCRAFT116_REACTION_BASE_SYSTEM_PROMPT,
        speech_style_source,
        default_mode="casual",
    )


STARCRAFT116_REACTION_SYSTEM_PROMPT = build_starcraft116_reaction_system_prompt("casual")


LOG_ONLY_STATUS_PHASES = {
    #20260705_kpopmodder: Normal running checks should stay in logs instead of being spoken as TTS.
    "launcher_waiting_for_start",
}


LOG_ONLY_GAME_EVENT_TYPES = {
    #20260705_kpopmodder: BWAPI connection health events are useful logs, not spoken reactions.
    "bwapi_proxy_loaded",
    "bwapi_real_loaded",
}


SPEAKABLE_PHASES = {
    "config_missing",
    "config_incomplete",
    "config_mismatch",
    "game_running",
    "launcher_running_after_start",
    "launcher_waiting_for_start",
    "last_run_completed_or_exited",
    "last_launcher_log_only",
}


STARCRAFT116_UNIT_TYPE_BASE_SPEAK_NAMES = {
    "SCV": "SCV",
    "Marine": "마린",
    "Firebat": "파이어뱃",
    "Medic": "메딕",
    "Ghost": "고스트",
    "Vulture": "벌처",
    "Spider_Mine": "스파이더 마인",
    "Siege_Tank_Tank_Mode": "시즈 탱크",
    "Siege_Tank_Siege_Mode": "시즈 탱크",
    "Goliath": "골리앗",
    "Wraith": "레이스",
    "Dropship": "드랍쉽",
    "Science_Vessel": "사이언스 베슬",
    "Battlecruiser": "배틀 크루저",
    "Valkyrie": "발키리",
    "Command_Center": "커맨드 센터",
    "Comsat_Station": "컴샛 스태이션",
    "Nuclear_Silo": "뉴클리어 사일로",
    "Supply_Depot": "서플라이 디팟",
    "Refinery": "리파이너리",
    "Barracks": "배럭",
    "Academy": "아카데미",
    "Factory": "팩토리",
    "Starport": "스타포트",
    "Control_Tower": "컨트롤 타워",
    "Machine_Shop": "머신샵",
    "Covert_Ops": "Covert Ops",
    "Physics_Lab": "피직스랩",
    "Science_Facility": "사이언스 퍼실리티",
    "Armory": "아머리",
    "Engineering_Bay": "엔지니어링 배이",
    "Bunker": "벙커",
    "Missile_Turret": "미사일 터렛",
    "Probe": "프로브",
    "Zealot": "질럿",
    "Dragoon": "드라군",
    "High_Templar": "하이 템플러",
    "Dark_Templar": "다크 템플러",
    "Archon": "아칸",
    "Dark_Archon": "다크 아칸",
    "Shuttle": "셔틀",
    "Reaver": "리버",
    "Observer": "옵저버",
    "Scout": "스카웃",
    "Corsair": "커새어",
    "Carrier": "캐리어",
    "Interceptor": "인터셉터",
    "Arbiter": "아비터",
    "Scarab": "스캐럽",
    "Nexus": "넥서스",
    "Pylon": "파일런",
    "Assimilator": "어시밀레이터",
    "Gateway": "게이트웨이",
    "Forge": "포지",
    "Cybernetics_Core": "사이버네틱스 코어",
    "Photon_Cannon": "포톤 캐넌",
    "Shield_Battery": "쉴드 배터리",
    "Robotics_Facility": "로보틱스 퍼실리티",
    "Stargate": "스타게이트",
    "Citadel_of_Adun": "시타델 오브 아둔",
    "Templar_Archives": "템플러 아카이브",
    "Robotics_Support_Bay": "로보틱스 서포트 배이",
    "Observatory": "옵저버터리",
    "Fleet_Beacon": "플릿 비콘",
    "Arbiter_Tribunal": "아비터 트리뷰널",
    "Larva": "라바",
    "Egg": "에그",
    "Drone": "드론",
    "Zergling": "저글링",
    "Hydralisk": "히드라리스크",
    "Lurker": "럴커",
    "Ultralisk": "울트라리스크",
    "Broodling": "브루들링",
    "Defiler": "디파일러",
    "Scourge": "스커지",
    "Queen": "퀸",
    "Mutalisk": "뮤탈리스크",
    "Guardian": "가디언",
    "Devourer": "디바우러",
    "Overlord": "오버로드",
    "Infested_Terran": "인페스티드 테란",
    "Hatchery": "해처리",
    "Lair": "래어",
    "Hive": "하이브",
    "Extractor": "익스트렉터",
    "Spawning_Pool": "스포닝 풀",
    "Evolution_Chamber": "에볼루션 챔버",
    "Creep_Colony": "크립 콜로니",
    "Sunken_Colony": "성큰 콜로니",
    "Spore_Colony": "스포어 콜로니",
    "Hydralisk_Den": "히드라리스크 덴",
    "Spire": "스파이어",
    "Greater_Spire": "그레이트 스파이어",
    "Queen_Nest": "퀸즈 네스트",
    "Queens_Nest": "퀸즈 네스트",
    "Nydus_Canal": "나이더스 커널",
    "Ultralisk_Cavern": "울트라리스크 카번",
    "Defiler_Mound": "디파일러 마운드",
}


STARCRAFT116_UNIT_TYPE_BASE_ENGLISH_NAMES = {
    "SCV": "SCV",
    "Marine": "Marine",
    "Firebat": "Firebat",
    "Medic": "Medic",
    "Ghost": "Ghost",
    "Vulture": "Vulture",
    "Spider_Mine": "Spider Mine",
    "Siege_Tank_Tank_Mode": "Siege Tank",
    "Siege_Tank_Siege_Mode": "Siege Tank",
    "Goliath": "Goliath",
    "Wraith": "Wraith",
    "Dropship": "Dropship",
    "Science_Vessel": "Science Vessel",
    "Battlecruiser": "Battlecruiser",
    "Valkyrie": "Valkyrie",
    "Command_Center": "Command Center",
    "Comsat_Station": "Comsat Station",
    "Nuclear_Silo": "Nuclear Silo",
    "Supply_Depot": "Supply Depot",
    "Refinery": "Refinery",
    "Barracks": "Barracks",
    "Academy": "Academy",
    "Factory": "Factory",
    "Starport": "Starport",
    "Control_Tower": "Control Tower",
    "Machine_Shop": "Machine Shop",
    "Covert_Ops": "Covert Ops",
    "Physics_Lab": "Physics Lab",
    "Science_Facility": "Science Facility",
    "Armory": "Armory",
    "Engineering_Bay": "Engineering Bay",
    "Bunker": "Bunker",
    "Missile_Turret": "Missile Turret",
    "Probe": "Probe",
    "Zealot": "Zealot",
    "Dragoon": "Dragoon",
    "High_Templar": "High Templar",
    "Dark_Templar": "Dark Templar",
    "Archon": "Archon",
    "Dark_Archon": "Dark Archon",
    "Shuttle": "Shuttle",
    "Reaver": "Reaver",
    "Observer": "Observer",
    "Scout": "Scout",
    "Corsair": "Corsair",
    "Carrier": "Carrier",
    "Interceptor": "Interceptor",
    "Arbiter": "Arbiter",
    "Scarab": "Scarab",
    "Nexus": "Nexus",
    "Pylon": "Pylon",
    "Assimilator": "Assimilator",
    "Gateway": "Gateway",
    "Forge": "Forge",
    "Cybernetics_Core": "Cybernetics Core",
    "Photon_Cannon": "Photon Cannon",
    "Shield_Battery": "Shield Battery",
    "Robotics_Facility": "Robotics Facility",
    "Stargate": "Stargate",
    "Citadel_of_Adun": "Citadel of Adun",
    "Templar_Archives": "Templar Archives",
    "Robotics_Support_Bay": "Robotics Support Bay",
    "Observatory": "Observatory",
    "Fleet_Beacon": "Fleet Beacon",
    "Arbiter_Tribunal": "Arbiter Tribunal",
    "Larva": "Larva",
    "Egg": "Egg",
    "Drone": "Drone",
    "Zergling": "Zergling",
    "Hydralisk": "Hydralisk",
    "Lurker": "Lurker",
    "Ultralisk": "Ultralisk",
    "Broodling": "Broodling",
    "Defiler": "Defiler",
    "Scourge": "Scourge",
    "Queen": "Queen",
    "Mutalisk": "Mutalisk",
    "Guardian": "Guardian",
    "Devourer": "Devourer",
    "Overlord": "Overlord",
    "Infested_Terran": "Infested Terran",
    "Hatchery": "Hatchery",
    "Lair": "Lair",
    "Hive": "Hive",
    "Extractor": "Extractor",
    "Spawning_Pool": "Spawning Pool",
    "Evolution_Chamber": "Evolution Chamber",
    "Creep_Colony": "Creep Colony",
    "Sunken_Colony": "Sunken Colony",
    "Spore_Colony": "Spore Colony",
    "Hydralisk_Den": "Hydralisk Den",
    "Spire": "Spire",
    "Greater_Spire": "Greater Spire",
    "Queen_Nest": "Queen's Nest",
    "Queens_Nest": "Queen's Nest",
    "Nydus_Canal": "Nydus Canal",
    "Ultralisk_Cavern": "Ultralisk Cavern",
    "Defiler_Mound": "Defiler Mound",
}


STARCRAFT116_BUILDING_BASE_TYPES = {
    "Command_Center",
    "Comsat_Station",
    "Nuclear_Silo",
    "Supply_Depot",
    "Refinery",
    "Barracks",
    "Academy",
    "Factory",
    "Starport",
    "Control_Tower",
    "Machine_Shop",
    "Covert_Ops",
    "Physics_Lab",
    "Science_Facility",
    "Armory",
    "Engineering_Bay",
    "Bunker",
    "Missile_Turret",
    "Nexus",
    "Pylon",
    "Assimilator",
    "Gateway",
    "Forge",
    "Cybernetics_Core",
    "Photon_Cannon",
    "Shield_Battery",
    "Robotics_Facility",
    "Stargate",
    "Citadel_of_Adun",
    "Templar_Archives",
    "Robotics_Support_Bay",
    "Observatory",
    "Fleet_Beacon",
    "Arbiter_Tribunal",
    "Hatchery",
    "Lair",
    "Hive",
    "Extractor",
    "Spawning_Pool",
    "Evolution_Chamber",
    "Creep_Colony",
    "Sunken_Colony",
    "Spore_Colony",
    "Hydralisk_Den",
    "Spire",
    "Greater_Spire",
    "Queen_Nest",
    "Queens_Nest",
    "Nydus_Canal",
    "Ultralisk_Cavern",
    "Defiler_Mound",
}


#20260705_kpopmodder: These buildings can plausibly attack; other buildings should not be phrased as attackers.
STARCRAFT116_ATTACK_CAPABLE_BUILDING_BASE_TYPES = {
    "Bunker",
    "Missile_Turret",
    "Photon_Cannon",
    "Sunken_Colony",
    "Spore_Colony",
}


#20260705_kpopmodder: Terran/Protoss morph events are often building construction, not Zerg-style morphs.
STARCRAFT116_TERRAN_PROTOSS_BUILDING_MORPH_BASE_TYPES = {
    "Command_Center",
    "Comsat_Station",
    "Nuclear_Silo",
    "Supply_Depot",
    "Refinery",
    "Barracks",
    "Academy",
    "Factory",
    "Starport",
    "Control_Tower",
    "Machine_Shop",
    "Covert_Ops",
    "Physics_Lab",
    "Science_Facility",
    "Armory",
    "Engineering_Bay",
    "Bunker",
    "Missile_Turret",
    "Nexus",
    "Pylon",
    "Assimilator",
    "Gateway",
    "Forge",
    "Cybernetics_Core",
    "Photon_Cannon",
    "Shield_Battery",
    "Robotics_Facility",
    "Stargate",
    "Citadel_of_Adun",
    "Templar_Archives",
    "Robotics_Support_Bay",
    "Observatory",
    "Fleet_Beacon",
    "Arbiter_Tribunal",
}


#20260705_kpopmodder: Limit Zerg morph wording fixes to known Zerg unit/building types.
STARCRAFT116_ZERG_MORPH_BASE_TYPES = {
    "Larva",
    "Egg",
    "Drone",
    "Zergling",
    "Hydralisk",
    "Lurker",
    "Ultralisk",
    "Broodling",
    "Defiler",
    "Scourge",
    "Queen",
    "Mutalisk",
    "Guardian",
    "Devourer",
    "Overlord",
    "Hatchery",
    "Lair",
    "Hive",
    "Extractor",
    "Spawning_Pool",
    "Evolution_Chamber",
    "Creep_Colony",
    "Sunken_Colony",
    "Spore_Colony",
    "Hydralisk_Den",
    "Spire",
    "Greater_Spire",
    "Queen_Nest",
    "Queens_Nest",
    "Nydus_Canal",
    "Ultralisk_Cavern",
    "Defiler_Mound",
}


#20260705_kpopmodder: Some BWAPI proxy morph snapshots only expose "Unit"; use observed Zerg type IDs as a conservative fallback.
STARCRAFT116_ZERG_MORPH_TYPE_IDS = {
    35,
    36,
    37,
    38,
    39,
    40,
    41,
    42,
    43,
    44,
    45,
    46,
    47,
    50,
    131,
    132,
    133,
    134,
    135,
    136,
    137,
    138,
    139,
    140,
    141,
    142,
    143,
    144,
    146,
    149,
}

STARCRAFT116_ZERG_RACE_NAMES = {"zerg", "저그"}
STARCRAFT116_ZERG_PROFILE_NAMES = {"monster"}


STARCRAFT116_UNIT_ALIAS_BASE_TYPES = (
    ("커맨드 센터", "Command_Center"),
    ("커맨드센터", "Command_Center"),
    ("컴샛 스테이션", "Comsat_Station"),
    ("컴셋 스테이션", "Comsat_Station"),
    ("컴샛 스태이션", "Comsat_Station"),
    ("뉴클리어 사일로", "Nuclear_Silo"),
    ("서플라이 디포", "Supply_Depot"),
    ("서플라이디포", "Supply_Depot"),
    ("서플라이 디팟", "Supply_Depot"),
    ("서플라이디팟", "Supply_Depot"),
    ("서플라이", "Supply_Depot"),
    ("리파이너리", "Refinery"),
    ("바락스", "Barracks"),
    ("배럭스", "Barracks"),
    ("배럭", "Barracks"),
    ("아카데미", "Academy"),
    ("팩토리", "Factory"),
    ("스타포트", "Starport"),
    ("컨트롤 타워", "Control_Tower"),
    ("컨트롤타워", "Control_Tower"),
    ("머신샵", "Machine_Shop"),
    ("머신 샵", "Machine_Shop"),
    ("사이언스 퍼실리티", "Science_Facility"),
    ("사이언스퍼실리티", "Science_Facility"),
    ("엔지니어링 베이", "Engineering_Bay"),
    ("엔지니어링베이", "Engineering_Bay"),
    ("엔지니어링 배이", "Engineering_Bay"),
    ("엔지니어링배이", "Engineering_Bay"),
    ("아머리", "Armory"),
    ("벙커", "Bunker"),
    ("미사일 터렛", "Missile_Turret"),
    ("미사일터렛", "Missile_Turret"),
    ("마린", "Marine"),
    ("파이어뱃", "Firebat"),
    ("메딕", "Medic"),
    ("고스트", "Ghost"),
    ("벌처", "Vulture"),
    ("스파이더 마인", "Spider_Mine"),
    ("스파이더마인", "Spider_Mine"),
    ("시즈 탱크", "Siege_Tank_Tank_Mode"),
    ("시즈탱크", "Siege_Tank_Tank_Mode"),
    ("골리앗", "Goliath"),
    ("레이스", "Wraith"),
    ("드랍쉽", "Dropship"),
    ("드롭쉽", "Dropship"),
    ("사이언스 베슬", "Science_Vessel"),
    ("사이언스베슬", "Science_Vessel"),
    ("배틀크루저", "Battlecruiser"),
    ("배틀 크루저", "Battlecruiser"),
    ("발키리", "Valkyrie"),
    ("프로브", "Probe"),
    ("질럿", "Zealot"),
    ("드라군", "Dragoon"),
    ("하이 템플러", "High_Templar"),
    ("하이템플러", "High_Templar"),
    ("다크 템플러", "Dark_Templar"),
    ("다크템플러", "Dark_Templar"),
    ("다크 아콘", "Dark_Archon"),
    ("다크아콘", "Dark_Archon"),
    ("다크 아칸", "Dark_Archon"),
    ("다크아칸", "Dark_Archon"),
    ("아콘", "Archon"),
    ("아칸", "Archon"),
    ("셔틀", "Shuttle"),
    ("리버", "Reaver"),
    ("옵저버", "Observer"),
    ("스카웃", "Scout"),
    ("커세어", "Corsair"),
    ("커새어", "Corsair"),
    ("캐리어", "Carrier"),
    ("인터셉터", "Interceptor"),
    ("아비터", "Arbiter"),
    ("스캐럽", "Scarab"),
    ("넥서스", "Nexus"),
    ("파일런", "Pylon"),
    ("파일론", "Pylon"),
    ("아시밀레이터", "Assimilator"),
    ("어시밀레이터", "Assimilator"),
    ("게이트웨이", "Gateway"),
    ("게이트", "Gateway"),
    ("포지", "Forge"),
    ("포톤 캐논", "Photon_Cannon"),
    ("포톤캐논", "Photon_Cannon"),
    ("포톤 캐넌", "Photon_Cannon"),
    ("포톤캐넌", "Photon_Cannon"),
    ("캐논", "Photon_Cannon"),
    ("캐넌", "Photon_Cannon"),
    ("실드 배터리", "Shield_Battery"),
    ("실드배터리", "Shield_Battery"),
    ("쉴드 배터리", "Shield_Battery"),
    ("쉴드배터리", "Shield_Battery"),
    ("사이버네틱스 코어", "Cybernetics_Core"),
    ("사이버네틱스코어", "Cybernetics_Core"),
    ("사이버 코어", "Cybernetics_Core"),
    ("사이버코어", "Cybernetics_Core"),
    ("로보틱스 퍼실리티", "Robotics_Facility"),
    ("로보틱스퍼실리티", "Robotics_Facility"),
    ("로보틱스 서포트 베이", "Robotics_Support_Bay"),
    ("로보틱스서포트베이", "Robotics_Support_Bay"),
    ("로보틱스 서포트 배이", "Robotics_Support_Bay"),
    ("로보틱스서포트배이", "Robotics_Support_Bay"),
    ("로보틱스", "Robotics_Facility"),
    ("스타게이트", "Stargate"),
    ("시타델 오브 아둔", "Citadel_of_Adun"),
    ("시타델오브아둔", "Citadel_of_Adun"),
    ("템플러 아카이브", "Templar_Archives"),
    ("템플러아카이브", "Templar_Archives"),
    ("플릿 비콘", "Fleet_Beacon"),
    ("플릿비콘", "Fleet_Beacon"),
    ("옵저버토리", "Observatory"),
    ("옵저버터리", "Observatory"),
    ("아비터 트리뷰널", "Arbiter_Tribunal"),
    ("아비터트리뷰널", "Arbiter_Tribunal"),
    ("라바", "Larva"),
    ("에그", "Egg"),
    ("드론", "Drone"),
    ("저글링", "Zergling"),
    ("히드라리스크", "Hydralisk"),
    ("히드라", "Hydralisk"),
    ("럴커", "Lurker"),
    ("러커", "Lurker"),
    ("울트라리스크", "Ultralisk"),
    ("울트라", "Ultralisk"),
    ("브루들링", "Broodling"),
    ("디파일러", "Defiler"),
    ("스커지", "Scourge"),
    ("퀸", "Queen"),
    ("뮤탈리스크", "Mutalisk"),
    ("뮤탈", "Mutalisk"),
    ("가디언", "Guardian"),
    ("디바우러", "Devourer"),
    ("오버로드", "Overlord"),
    ("인페스티드 테란", "Infested_Terran"),
    ("인페스티드테란", "Infested_Terran"),
    ("해처리", "Hatchery"),
    ("레어", "Lair"),
    ("래어", "Lair"),
    ("하이브", "Hive"),
    ("익스트랙터", "Extractor"),
    ("익스트렉터", "Extractor"),
    ("스포닝 풀", "Spawning_Pool"),
    ("스포닝풀", "Spawning_Pool"),
    ("에볼루션 챔버", "Evolution_Chamber"),
    ("에볼루션챔버", "Evolution_Chamber"),
    ("크립 콜로니", "Creep_Colony"),
    ("크립콜로니", "Creep_Colony"),
    ("성큰 콜로니", "Sunken_Colony"),
    ("성큰콜로니", "Sunken_Colony"),
    ("성큰", "Sunken_Colony"),
    ("스포어 콜로니", "Spore_Colony"),
    ("스포어콜로니", "Spore_Colony"),
    ("스포어", "Spore_Colony"),
    ("히드라리스크 덴", "Hydralisk_Den"),
    ("히드라리스크덴", "Hydralisk_Den"),
    ("스파이어", "Spire"),
    ("그레이터 스파이어", "Greater_Spire"),
    ("그레이터스파이어", "Greater_Spire"),
    ("그레이트 스파이어", "Greater_Spire"),
    ("그레이트스파이어", "Greater_Spire"),
    ("퀸즈 네스트", "Queen_Nest"),
    ("퀸즈네스트", "Queen_Nest"),
    ("나이더스 커널", "Nydus_Canal"),
    ("나이더스커널", "Nydus_Canal"),
    ("울트라리스크 캐번", "Ultralisk_Cavern"),
    ("울트라리스크캐번", "Ultralisk_Cavern"),
    ("울트라리스크 카번", "Ultralisk_Cavern"),
    ("울트라리스크카번", "Ultralisk_Cavern"),
    ("디파일러 마운드", "Defiler_Mound"),
    ("디파일러마운드", "Defiler_Mound"),
)


STARCRAFT116_UNIT_ALIAS_REPLACEMENTS = tuple(sorted(
    [
        (alias, STARCRAFT116_UNIT_TYPE_BASE_SPEAK_NAMES[base_type])
        for alias, base_type in STARCRAFT116_UNIT_ALIAS_BASE_TYPES
        if base_type in STARCRAFT116_UNIT_TYPE_BASE_SPEAK_NAMES
    ]
    + [
        (name, STARCRAFT116_UNIT_TYPE_BASE_SPEAK_NAMES[base_type])
        for base_type, name in STARCRAFT116_UNIT_TYPE_BASE_ENGLISH_NAMES.items()
        if base_type in STARCRAFT116_UNIT_TYPE_BASE_SPEAK_NAMES
    ],
    key=lambda item: len(item[0]),
    reverse=True,
))


STARCRAFT116_KNOWN_UNIT_SPEAK_NAMES = tuple(sorted(
    set(STARCRAFT116_UNIT_TYPE_BASE_SPEAK_NAMES.values())
    | set(STARCRAFT116_UNIT_TYPE_BASE_ENGLISH_NAMES.values())
    | {old for old, _ in STARCRAFT116_UNIT_ALIAS_REPLACEMENTS}
    | {name for _, name in STARCRAFT116_UNIT_ALIAS_REPLACEMENTS},
    key=len,
    reverse=True,
))


STARCRAFT116_KOREAN_JOSA_PATTERN = (
    "이랑|랑|하고|에서|에게|한테|으로|로|과|와|이|가|은|는|을|를|도|에"
)


STARCRAFT116_KOREAN_TERM_REPLACEMENTS = (
    ("경제 상황", "자원 상태"),
    ("경제가", "자원 상태가"),
    ("경제는", "자원 상태는"),
    ("경제를", "자원 상태를"),
    ("경제도", "자원 상태도"),
    ("경제", "자원 상태"),
    ("공급이", "인구수가"),
    ("공급은", "인구수는"),
    ("공급을", "인구수를"),
    ("공급도", "인구수도"),
    ("공급량", "인구수"),
    ("공급", "인구수"),
    ("완공되었습니다", "완성됐어"),
    ("완공되었어", "완성됐어"),
    ("완공되었다", "완성됐다"),
    ("완공됐다", "완성됐다"),
    ("완공됐어", "완성됐어"),
    ("완공됐", "완성됐"),
    ("완공되", "완성되"),
    ("완공", "완성"),
    ("드라군이 포지와 싸우고 있어", "드라군이 포지를 박살내고 있어"),
    ("드라군이 Forge와 싸우고 있어", "드라군이 포지를 박살내고 있어"),
    ("포지와 싸우고 있어", "포지를 박살내고 있어"),
    ("Forge와 싸우고 있어", "포지를 박살내고 있어"),
    ("게이트웨이와 싸우고 있어", "게이트웨이를 박살내고 있어"),
    ("파일런과 싸우고 있어", "파일런을 박살내고 있어"),
    ("파일론과 싸우고 있어", "파일런을 박살내고 있어"),
    ("넥서스와 싸우고 있어", "넥서스를 박살내고 있어"),
    ("잘 하고 있어", "내가 잘하고 있어"),
    ("잘하고 있어", "내가 잘하고 있어"),
    ("조심해봐", "조심해야겠다"),
    ("조심해", "조심해야겠다"),
    ("주의해봐", "주의해야 해"),
    ("주의해", "주의해야 해"),
    ("빨리 처치해", "빨리 처치해야겠다"),
)


def build_starcraft116_status_event(external_status, source="status"):
    summary = (external_status or {}).get("summary", {})
    phase = str(summary.get("phase", "") or "").strip()
    severity = str(summary.get("severity", "") or "").strip()
    if phase not in SPEAKABLE_PHASES:
        return None

    bwapi_ini = (external_status or {}).get("bwapi_ini", {})
    chaoslauncher_log = (external_status or {}).get("chaoslauncher_log", {})
    readiness = (external_status or {}).get("readiness", {})
    messages = _string_list(summary.get("messages", []))
    next_actions = _string_list(summary.get("next_actions", []))
    recent_lines = _string_list(chaoslauncher_log.get("recent_relevant_lines", []))
    generated_at = (external_status or {}).get("generated_at", 0)
    profile = str((external_status or {}).get("profile", "") or "").strip()
    configured_ai = str(bwapi_ini.get("configured_ai_binary", "") or "").strip()

    return {
        "event_id": _event_id(profile, phase, severity, generated_at, source),
        "source": str(source or "status"),
        "profile": profile,
        "phase": phase,
        "severity": severity or "ok",
        "message": str(summary.get("message", "") or "").strip(),
        "messages": messages,
        "next_actions": next_actions,
        "configured_ai_binary": configured_ai,
        "expected_bot_matches_ini": bool(
            bwapi_ini.get("expected_bot_matches_ini")
        ),
        "readiness": {
            "starcraft_process_running": bool(
                readiness.get("starcraft_process_running")
            ),
            "chaoslauncher_process_running": bool(
                readiness.get("chaoslauncher_process_running")
            ),
            "bwapi_release_patch_applied": bool(
                readiness.get("bwapi_release_patch_applied")
            ),
            "wmode_ready": bool(readiness.get("wmode_ready")),
            "debug_privilege_obtained": bool(
                readiness.get("debug_privilege_obtained")
            ),
            "starcraft_start_completed": bool(
                readiness.get("starcraft_start_completed")
            ),
        },
        "recent_relevant_lines": recent_lines[-5:],
    }


def should_speak_starcraft116_event(event):
    #20260705_kpopmodder: Keep this public decision point stable while smaller helpers own each event category.
    if is_starcraft116_game_event(event):
        return not is_log_only_starcraft116_game_event(event)

    return not is_log_only_starcraft116_status_event(event)


def is_starcraft116_game_event(event):
    #20260705_kpopmodder: Treat only explicit BWAPI game_event payloads as game events.
    return event.get("source") == "game_event"


def normalized_starcraft116_event_type(event):
    return str(event.get("event_type", "") or "").strip().lower()


def normalized_starcraft116_status_phase(event):
    return str(event.get("phase", "") or "").strip()


def is_log_only_starcraft116_game_event(event):
    #20260705_kpopmodder: Connection health stays log-only; high-volume gameplay filtering is a later config step.
    return normalized_starcraft116_event_type(event) in LOG_ONLY_GAME_EVENT_TYPES


def is_log_only_starcraft116_status_event(event):
    #20260705_kpopmodder: Preserve previous status-event TTS skip behavior exactly.
    return normalized_starcraft116_status_phase(event) in LOG_ONLY_STATUS_PHASES


def build_starcraft116_status_event_key(event):
    readiness = event.get("readiness", {})
    parts = [
        event.get("profile", ""),
        event.get("phase", ""),
        event.get("severity", ""),
        event.get("configured_ai_binary", ""),
        str(readiness.get("starcraft_process_running", False)),
        str(readiness.get("chaoslauncher_process_running", False)),
        str(readiness.get("bwapi_release_patch_applied", False)),
        str(readiness.get("wmode_ready", False)),
        str(readiness.get("debug_privilege_obtained", False)),
        str(readiness.get("starcraft_start_completed", False)),
    ]
    return "|".join(str(part) for part in parts)


def build_starcraft116_game_event(raw_event, profile="", source="game_event"):
    if not isinstance(raw_event, dict):
        return None

    event_type = _first_text(
        raw_event,
        ("event_type", "type", "name", "kind"),
    )
    summary = _first_text(
        raw_event,
        ("summary", "message", "description", "text"),
    )
    if not event_type and not summary:
        return None

    severity = _first_text(raw_event, ("severity", "level")) or "info"
    frame = raw_event.get("frame", raw_event.get("game_frame", ""))
    game_time_seconds = raw_event.get(
        "game_time_seconds",
        raw_event.get("time_seconds", raw_event.get("seconds", "")),
    )
    raw_event_id = _first_text(raw_event, ("event_id", "id"))
    event_id = raw_event_id or _raw_event_hash(raw_event)
    profile = _first_text(raw_event, ("profile",)) or str(profile or "").strip()
    details = _game_event_details(raw_event)
    unit_mentions = _collect_starcraft116_unit_mentions(raw_event, details)

    return {
        "event_id": f"sc116-game-{event_id}",
        "source": str(source or "game_event"),
        "profile": profile,
        "event_type": event_type or "game_event",
        "phase": "game_event",
        "severity": severity,
        "message": summary,
        "summary": summary,
        "frame": frame,
        "game_time_seconds": game_time_seconds,
        "player": _first_text(raw_event, ("player", "self_player")),
        "race": _first_text(raw_event, ("race",)),
        "self_race": _first_text(raw_event, ("self_race", "player_race")),
        "enemy_race": _first_text(raw_event, ("enemy_race",)),
        "map": _first_text(raw_event, ("map", "map_name")),
        "details": details,
        "unit_mentions": unit_mentions,
        "raw_keys": sorted(str(key) for key in raw_event.keys())[:24],
        "received_at": time.time(),
    }


def build_starcraft116_game_event_key(event):
    parts = [
        event.get("profile", ""),
        event.get("event_type", ""),
        event.get("severity", ""),
        event.get("frame", ""),
        event.get("game_time_seconds", ""),
        event.get("summary", ""),
        _compact_value(event.get("details", {}), 400),
    ]
    return "|".join(str(part) for part in parts)


def build_starcraft116_reaction_user_message(event):
    if event.get("source") == "game_event":
        return _build_starcraft116_game_event_user_message(event)

    readiness = event.get("readiness", {})
    lines = [
        "StarCraft 1.16 BWAPI status event:",
        f"source: {event.get('source', '')}",
        f"profile: {event.get('profile', '')}",
        f"phase: {event.get('phase', '')}",
        f"severity: {event.get('severity', '')}",
        f"configured_ai_binary: {event.get('configured_ai_binary', '')}",
        "readiness:",
    ]
    lines.extend(
        f"- {key}: {value}"
        for key, value in sorted(readiness.items())
    )
    lines.append("summary_messages:")
    lines.extend(
        f"- {message}"
        for message in event.get("messages", [])
    )
    next_actions = event.get("next_actions", [])
    if next_actions:
        lines.append("next_actions:")
        lines.extend(f"- {action}" for action in next_actions)
    recent_lines = event.get("recent_relevant_lines", [])
    if recent_lines:
        lines.append("recent_chaoslauncher_lines:")
        lines.extend(f"- {line}" for line in recent_lines)
    return "\n".join(lines)


def build_starcraft116_fallback_reaction(event):
    if event.get("source") == "game_event":
        return _build_starcraft116_game_event_fallback_reaction(event)

    phase = event.get("phase", "")
    bot = str(event.get("configured_ai_binary", "") or "BWAPI").strip()
    if phase == "game_running":
        return f"좋아, 스타는 떴고 {bot}도 물린 걸로 보여."
    if phase == "launcher_waiting_for_start":
        return "카오스런처는 열렸어, BWAPI랑 W-MODE 체크하고 Start 누르면 돼."
    if phase == "launcher_running_after_start":
        return "Start는 지나간 것 같은데, 카오스런처가 아직 남아 있어."
    if phase == "last_run_completed_or_exited":
        return "방금 실행 기록은 있는데, 지금 스타 프로세스는 안 보여."
    if phase == "last_launcher_log_only":
        return "BWAPI 로드 기록은 있는데, 지금 실행 중인 스타는 안 잡혀."
    if phase == "config_missing":
        return "BWAPI 설정 파일이 안 보여, 경로부터 다시 확인해야 해."
    if phase == "config_incomplete":
        return "bwapi.ini는 있는데 AI DLL 설정이 비어 있어."
    if phase == "config_mismatch":
        return "선택한 봇이랑 bwapi.ini의 AI DLL이 서로 안 맞아."
    message = str(event.get("message", "") or "").strip()
    return message or "스타크래프트 상태가 바뀌었어."


def clean_starcraft116_reaction_text(text):
    text = str(text or "").strip()
    if not text:
        return ""

    for wrapper in ("\"", "'", "`"):
        text = text.strip(wrapper)

    lines = [line.strip() for line in text.splitlines() if line.strip()]
    if lines:
        text = lines[0]

    text = _normalize_starcraft116_korean_terms(text)

    if len(text) > 140:
        text = text[:137].rstrip() + "..."
    return text


def build_starcraft116_reaction_tts_text(event, reaction):
    cleaned = clean_starcraft116_reaction_text(reaction)
    if cleaned:
        normalized = _normalize_starcraft116_event_terms(event, cleaned)
        if _contains_disallowed_starcraft116_unit_name(event, normalized):
            return _build_starcraft116_validated_game_event_fallback(event)
        return normalized
    fallback = clean_starcraft116_reaction_text(
        build_starcraft116_fallback_reaction(event)
    )
    normalized_fallback = _normalize_starcraft116_event_terms(event, fallback)
    if _contains_disallowed_starcraft116_unit_name(event, normalized_fallback):
        return _build_starcraft116_validated_game_event_fallback(event)
    return normalized_fallback


def _event_id(profile, phase, severity, generated_at, source):
    try:
        timestamp = int(float(generated_at or 0) * 1000)
    except (TypeError, ValueError):
        timestamp = 0
    return f"sc116-{source}-{profile}-{phase}-{severity}-{timestamp}"


def _build_starcraft116_game_event_user_message(event):
    unit_mentions = event.get("unit_mentions", [])
    lines = [
        "StarCraft 1.16 BWAPI game event:",
        "Korean style rules:",
        "- Speak as if I am playing, not as if I am coaching the user.",
        "- Use 자원 상태, 인구수, and 완성; do not use 경제, 공급, or 완공.",
        "- Prefer active decisions like 조심해야겠다 or 주의해야 해.",
        "- If our unit attacks an enemy building, say it is breaking it.",
        "- Use only allowed_unit_names for unit/building names.",
        "- Keep unit/building names exactly as shown in allowed_unit_names.",
        "- Keep the rest of the sentence natural Korean.",
        "- If allowed_unit_names is empty, do not mention any unit name.",
        "allowed_unit_names:",
    ]
    if unit_mentions:
        lines.extend(
            f"- {mention.get('role', 'unit')}: "
            f"{mention.get('speak_name', '')} "
            f"(raw={mention.get('raw_type', '')}, owner={mention.get('owner', '')})"
            for mention in unit_mentions
        )
    else:
        lines.append("- none")
    lines.extend([
        f"source: {event.get('source', '')}",
        f"profile: {event.get('profile', '')}",
        f"event_type: {event.get('event_type', '')}",
        f"severity: {event.get('severity', '')}",
        f"summary: {event.get('summary', '')}",
        f"frame: {event.get('frame', '')}",
        f"game_time_seconds: {event.get('game_time_seconds', '')}",
        f"player: {event.get('player', '')}",
        f"race: {event.get('race', '')}",
        f"map: {event.get('map', '')}",
        "details:",
        _compact_value(event.get("details", {}), 1200),
    ])
    return "\n".join(str(line) for line in lines if str(line).strip())


def _build_starcraft116_game_event_fallback_reaction(event):
    validated = _build_starcraft116_validated_game_event_fallback(event)
    if validated:
        return validated

    event_type = str(event.get("event_type", "") or "").lower()
    summary = str(event.get("summary", "") or "").strip()
    if event_type in {"enemy_spotted", "scouting_enemy_spotted"}:
        return summary or "적 확인됐어, 이제 눈 떼면 안 돼."
    if event_type in {"under_attack", "combat_started", "unit_under_attack"}:
        return summary or "교전 들어왔어, 지금은 손 빨라야 해."
    if event_type in {"build_started", "building_started", "production_started"}:
        return summary or "빌드 올라간다, 흐름은 유지해야지."
    if event_type in {"unit_completed", "unit_created", "worker_created"}:
        return summary or "유닛 하나 더 붙었어, 괜찮은 흐름이야."
    if event_type in {"unit_moved", "unit_movement_detected"}:
        return "유닛 움직임 잡혔어. Monster가 실제로 명령을 굴리고 있어."
    if event_type in {"supply_blocked", "resource_low"}:
        return summary or "막히기 전에 정리해야 해."
    return summary or "게임 이벤트 들어왔어."


def _collect_starcraft116_unit_mentions(raw_event, details):
    mentions = []
    seen = set()

    def add_mention(role, raw_type, owner=""):
        raw_type = str(raw_type or "").strip()
        speak_name = _starcraft116_unit_speak_name(raw_type)
        if not speak_name:
            return
        key = (str(role or "unit"), raw_type, speak_name, str(owner or ""))
        if key in seen:
            return
        seen.add(key)
        mentions.append({
            "role": str(role or "unit"),
            "raw_type": raw_type,
            "speak_name": speak_name,
            "owner": str(owner or ""),
        })

    def add_unit_payload(role, payload):
        if not isinstance(payload, dict):
            return
        add_mention(role, payload.get("type", ""), payload.get("owner", ""))

    for source in (raw_event, details):
        if not isinstance(source, dict):
            continue
        for key in ("unit", "friendly_unit", "enemy_unit"):
            add_unit_payload(key, source.get(key))
        units = source.get("units")
        if isinstance(units, dict):
            for raw_type in units.keys():
                add_mention("units", raw_type)
        combat = source.get("combat")
        if isinstance(combat, dict):
            for raw_type in _string_list(combat.get("friendly_units", [])):
                add_mention("friendly_unit", raw_type, "self")
            for raw_type in _string_list(combat.get("enemy_units", [])):
                add_mention("enemy_unit", raw_type, "enemy")

    for text in (
        raw_event.get("summary", ""),
        raw_event.get("message", ""),
        raw_event.get("description", ""),
        raw_event.get("text", ""),
    ):
        _collect_starcraft116_unit_mentions_from_text(text, add_mention)

    return mentions


def _collect_starcraft116_unit_mentions_from_text(text, add_mention):
    text = str(text or "")
    for match in re.finditer(r"\b(?:Terran|Protoss|Zerg)_[A-Za-z0-9_]+\b", text):
        add_mention("summary", match.group(0))
    for name in STARCRAFT116_KNOWN_UNIT_SPEAK_NAMES:
        if _contains_starcraft116_english_name(text, name):
            add_mention("summary", name)


def _starcraft116_unit_speak_name(raw_type):
    raw_type = str(raw_type or "").strip()
    if not raw_type:
        return ""
    normalized = raw_type.replace(" ", "_")
    for prefix in ("Terran_", "Protoss_", "Zerg_"):
        if normalized.startswith(prefix):
            normalized = normalized[len(prefix):]
            break
    alias_name = normalized.replace("_", " ")
    for alias, speak_name in STARCRAFT116_UNIT_ALIAS_REPLACEMENTS:
        if alias_name.casefold() == str(alias).casefold():
            return speak_name
    if normalized in STARCRAFT116_UNIT_TYPE_BASE_SPEAK_NAMES:
        return STARCRAFT116_UNIT_TYPE_BASE_SPEAK_NAMES[normalized]
    parts = [part for part in normalized.split("_") if part]
    if not parts:
        return raw_type
    return " ".join(_starcraft116_title_part(part) for part in parts)


def _starcraft116_title_part(part):
    upper = str(part).upper()
    if upper in {"SCV", "VHP", "HP"}:
        return upper
    return str(part).capitalize()


def _allowed_starcraft116_unit_speak_names(event):
    names = []
    seen = set()
    for mention in event.get("unit_mentions", []):
        name = str(mention.get("speak_name", "") or "").strip()
        if name and name.casefold() not in seen:
            names.append(name)
            seen.add(name.casefold())
    return names


def _contains_disallowed_starcraft116_unit_name(event, text):
    if event.get("source") != "game_event":
        return False
    allowed = {
        name.casefold()
        for name in _allowed_starcraft116_unit_speak_names(event)
    }
    for name in STARCRAFT116_KNOWN_UNIT_SPEAK_NAMES:
        if not _contains_starcraft116_english_name(text, name):
            continue
        if name.casefold() not in allowed:
            return True
    return False


def _contains_starcraft116_english_name(text, name):
    text = str(text or "")
    name = str(name or "")
    if not name:
        return False
    if _has_starcraft116_non_ascii(name):
        pattern = (
            r"(?<![A-Za-z0-9_가-힣])"
            + re.escape(name)
            + rf"(?:{STARCRAFT116_KOREAN_JOSA_PATTERN})?"
            + r"(?![A-Za-z0-9_가-힣])"
        )
        return re.search(pattern, text) is not None
    pattern = (
        r"(?<![A-Za-z0-9_])"
        + re.escape(name)
        + r"(?![A-Za-z0-9_])"
    )
    return re.search(pattern, text, flags=re.IGNORECASE) is not None


def _replace_starcraft116_english_name(text, old, new):
    text = str(text)
    old = str(old)
    if not old:
        return text
    if _has_starcraft116_non_ascii(old):
        pattern = (
            r"(?<![A-Za-z0-9_가-힣])"
            + re.escape(old)
            + rf"(?P<josa>{STARCRAFT116_KOREAN_JOSA_PATTERN})?"
            + r"(?![A-Za-z0-9_가-힣])"
        )

        def replace_match(match):
            return str(new) + (match.group("josa") or "")

        return re.sub(pattern, replace_match, text)
    pattern = (
        r"(?<![A-Za-z0-9_])"
        + re.escape(old)
        + r"(?![A-Za-z0-9_])"
    )
    return re.sub(pattern, str(new), text, flags=re.IGNORECASE)


def _has_starcraft116_non_ascii(value):
    return any(ord(char) > 127 for char in str(value or ""))


def _build_starcraft116_validated_game_event_fallback(event):
    if event.get("source") != "game_event":
        return ""

    event_type = str(event.get("event_type", "") or "").lower()
    unit = _find_starcraft116_unit_mention(event, ("unit",))
    friendly = _find_starcraft116_unit_mention(event, ("friendly_unit",))
    enemy = _find_starcraft116_unit_mention(event, ("enemy_unit", "unit"))

    if event_type in {"enemy_spotted", "scouting_enemy_spotted"} and enemy:
        return f"적 {enemy['speak_name']} 발견했어, 조심해야겠다."
    if event_type in {"under_attack", "combat_started", "unit_under_attack"}:
        if friendly and enemy:
            if _is_starcraft116_building(enemy.get("raw_type", "")):
                return (
                    f"내 {_starcraft116_subject(friendly['speak_name'])} "
                    f"{_starcraft116_object(enemy['speak_name'])} 박살내고 있어."
                )
            return (
                f"내 {_starcraft116_subject(friendly['speak_name'])} "
                f"{enemy['speak_name']}이랑 붙었어."
            )
        return "교전 들어왔어, 조심해야겠다."
    if event_type in {"build_started", "building_started", "production_started"}:
        target = unit or friendly
        if target:
            return f"내가 {_starcraft116_object(target['speak_name'])} 짓고 있어."
    if event_type in {"building_completed"}:
        target = unit or friendly
        if target:
            return f"내 {target['speak_name']} 완성됐어."
    if event_type in {"unit_completed", "unit_created", "worker_created"}:
        target = unit or friendly
        if target:
            return f"내 {target['speak_name']} 준비됐어."
    if event_type == "unit_destroyed" and unit:
        if unit.get("owner") == "enemy":
            return f"적 {unit['speak_name']} 하나 잡았어."
        if unit.get("owner") == "self":
            return f"내 {_starcraft116_subject(unit['speak_name'])} 터졌어."
        return f"{unit['speak_name']} 하나 잡았어."
    return ""


def _is_starcraft116_building(raw_type):
    base = _starcraft116_base_type(raw_type)
    return base in STARCRAFT116_BUILDING_BASE_TYPES


def _normalize_starcraft116_korean_terms(text):
    for old, new in STARCRAFT116_KOREAN_TERM_REPLACEMENTS:
        text = text.replace(old, new)
    for old, new in STARCRAFT116_UNIT_ALIAS_REPLACEMENTS:
        text = _replace_starcraft116_english_name(text, old, new)
    text = _normalize_starcraft116_raw_unit_tokens(text)

    while "내가 내가" in text:
        text = text.replace("내가 내가", "내가")
    while "조심해야겠다야겠다" in text:
        text = text.replace("조심해야겠다야겠다", "조심해야겠다")
    while "주의해야 해야 해" in text:
        text = text.replace("주의해야 해야 해", "주의해야 해")
    while "빨리 처치해야겠다야겠다" in text:
        text = text.replace("빨리 처치해야겠다야겠다", "빨리 처치해야겠다")
    return text


def _normalize_starcraft116_raw_unit_tokens(text):
    for base_type, speak_name in STARCRAFT116_UNIT_TYPE_BASE_SPEAK_NAMES.items():
        for raw_type in (
            base_type,
            f"Terran_{base_type}",
            f"Protoss_{base_type}",
            f"Zerg_{base_type}",
        ):
            text = _replace_starcraft116_english_name(text, raw_type, speak_name)
    return text


def _normalize_starcraft116_event_terms(event, text):
    event_type = str(event.get("event_type", "") or "").lower()
    text = _normalize_starcraft116_completed_subject_agreement_terms(event, text)
    if event_type in {"build_started", "building_started", "production_started"}:
        text = text.replace("완성하고 있어", "짓고 있어")
        text = text.replace("완성 중", "건설 중")
        text = text.replace("완성 시작", "건설 시작")
        text = text.replace("완성하기 시작", "짓기 시작")
    if _is_starcraft116_morph_event(event):
        text = _normalize_starcraft116_siege_tank_morph_terms(text)
        if _mentions_starcraft116_terran_protoss_building_morph_target(event, text):
            text = _normalize_starcraft116_terran_protoss_building_morph_terms(text)
    if (
        _is_starcraft116_morph_event(event)
        and (
            _mentions_starcraft116_zerg_morph_target(event, text)
            or _is_starcraft116_zerg_morph_context(event)
        )
    ):
        text = _normalize_starcraft116_zerg_morph_terms(text)
        text = _normalize_starcraft116_generic_monster_morph_terms(text)
    elif _is_starcraft116_morph_event(event):
        text = _normalize_starcraft116_non_zerg_morph_terms(text)
        text = _normalize_starcraft116_generic_morph_terms(text)
    if event_type in {"under_attack", "combat_started", "unit_under_attack"}:
        text = _normalize_starcraft116_non_attacking_building_fight_terms(event, text)
        if _mentions_starcraft116_building_fight(event, text):
            fallback = _build_starcraft116_validated_game_event_fallback(event)
            if fallback:
                return fallback
    return text


def _normalize_starcraft116_completed_subject_agreement_terms(event, text):
    names = tuple(
        sorted(_allowed_starcraft116_unit_speak_names(event), key=len, reverse=True)
    )
    if not names:
        return text

    completed_predicates = (
        "완성됐어",
        "완성되었어",
        "완성됐다",
        "완성됐네",
        "완성되었네",
    )
    for name in names:
        name_pattern = re.escape(name)
        predicate_pattern = "|".join(re.escape(predicate) for predicate in completed_predicates)
        pattern = rf"내가\s+{name_pattern}(이|가)\s+({predicate_pattern})"

        def replace_subject_agreement(match):
            particle = match.group(1)
            predicate = match.group(2)
            return f"내 {name}{particle} {predicate}"

        text = re.sub(pattern, replace_subject_agreement, text)
    return text


def _normalize_starcraft116_siege_tank_morph_terms(text):
    if not re.search(r"시즈\s*탱크", text):
        return text

    tank_name = "시즈탱크" if "시즈탱크" in text else "시즈 탱크"
    replacement = f"{tank_name}가 시즈모드를 했어"
    tank_pattern = r"시즈\s*탱크"
    patterns = (
        rf"내가\s*{tank_pattern}(?:를|로)\s*(?:변형|변신|변환)(?:되었어|됐어|됐다|했어|했다|되고 있어|중이야|중이네| 중이야| 중이네)",
        rf"{tank_pattern}가\s*(?:변형|변신|변환)(?:되었어|됐어|됐다|했어|했다|되고 있어|중이야|중이네| 중이야| 중이네)",
        rf"{tank_pattern}\s*(?:변형|변신|변환)(?:되었어|됐어|됐다|했어|했다|되고 있어|중이야|중이네| 중이야| 중이네)",
        rf"{tank_pattern}가\s*형태를\s*바꿨(?:어|네)",
        rf"{tank_pattern}\s*형태를\s*바꿨(?:어|네)",
    )
    for pattern in patterns:
        text = re.sub(pattern, replacement, text)
    return text


def _normalize_starcraft116_terran_protoss_building_morph_terms(text):
    replacements = (
        ("변형되고 있어", "건설되고 있어"),
        ("변형 중이야", "건설 중이야"),
        ("변형중이야", "건설중이야"),
        ("변형 중이네", "건설 중이네"),
        ("변형중이네", "건설중이네"),
        ("변형되었어", "건설되었어"),
        ("변형됐어", "건설됐어"),
        ("변형했어", "건설했어"),
        ("변신되고 있어", "건설되고 있어"),
        ("변신 중이야", "건설 중이야"),
        ("변신중이야", "건설중이야"),
        ("변신되었어", "건설되었어"),
        ("변신됐어", "건설됐어"),
        ("변신했어", "건설됐어"),
        ("변환되고 있어", "건설되고 있어"),
        ("변환 중이야", "건설 중이야"),
        ("변환중이야", "건설중이야"),
        ("변환되었어", "건설되었어"),
        ("변환됐어", "건설됐어"),
        ("변환했어", "건설했어"),
        ("형태를 바꾸고 있어", "건설되고 있어"),
        ("형태를 바꿨어", "건설됐어"),
        ("형태를 바꿨네", "건설됐어"),
    )
    for old, new in replacements:
        text = text.replace(old, new)
    return text


def _normalize_starcraft116_non_attacking_building_fight_terms(event, text):
    enemy = _find_starcraft116_unit_mention(event, ("enemy_unit", "unit"))
    if not enemy:
        return text

    enemy_name = str(enemy.get("speak_name", "") or "").strip()
    if not enemy_name:
        return text

    for base_type in STARCRAFT116_BUILDING_BASE_TYPES:
        if base_type in STARCRAFT116_ATTACK_CAPABLE_BUILDING_BASE_TYPES:
            continue
        building_name = STARCRAFT116_UNIT_TYPE_BASE_SPEAK_NAMES.get(base_type, "")
        if not building_name or building_name not in text:
            continue
        building_pattern = re.escape(building_name)
        enemy_pattern = re.escape(enemy_name)
        pattern = (
            rf"내\s+{building_pattern}(?:이|가)\s+"
            rf"{enemy_pattern}(?:을|를)\s*"
            rf"(?:부수고|깨고|박살내고|공격하고)\s+있어"
        )
        replacement = f"내 {_starcraft116_subject(building_name)} {enemy_name}에게 공격당하고 있어"
        text = re.sub(pattern, replacement, text)
    return text


def _normalize_starcraft116_zerg_morph_terms(text):
    replacements = (
        ("모핑되고 있어", "변신되고 있어"),
        ("모핑하고 있어", "변신하고 있어"),
        ("모핑 중이야", "변신 중이야"),
        ("모핑됐어", "변신했어"),
        ("모핑되었어", "변신했어"),
        ("모핑했어", "변신했어"),
        ("변형 중이야", "변신 중이야"),
        ("변환 중이야", "변신 중이야"),
        ("변신 중이야", "변신 중이야"),
        ("변형시켰어", "변신시켰어"),
        ("변환시켰어", "변신시켰어"),
        ("변신시켰어", "변신시켰어"),
        ("변형시키고 있어", "변신시키고 있어"),
        ("변환시키고 있어", "변신시키고 있어"),
        ("변신시키고 있어", "변신시키고 있어"),
        ("유닛이 변형되었어", "유닛이 변신되었어"),
        ("유닛이 변환되었어", "유닛이 변신되었어"),
        ("유닛이 변신되었어", "유닛이 변신되었어"),
        ("유닛이 변형됐어", "유닛이 변신했어"),
        ("유닛이 변환됐어", "유닛이 변신했어"),
        ("유닛이 변신됐어", "유닛이 변신했어"),
        ("내가 새로운 유닛으로 변태했어", "새로운 유닛을 변신했어"),
        ("내가 새로운 유닛으로 변신했어", "새로운 유닛을 변신했어"),
        ("새로운 유닛으로 변태했어", "새로운 유닛을 변신했어"),
        ("새로운 유닛으로 변신했어", "새로운 유닛을 변신했어"),
        ("내가 유닛을 변형했어", "내가 유닛을 변신했어"),
        ("내가 유닛을 변환했어", "내가 유닛을 변신했어"),
        ("내가 유닛을 형태 변경했어", "내가 유닛을 변신했어"),
        ("단위가 변형되고 있어", "유닛이 변신되고 있어"),
        ("변이를 했어", "변신했어"),
        ("변이 중이야", "변신 중이야"),
        ("변이하고 있어", "변신하고 있어"),
        ("변이했어", "변신했어"),
        ("탈바꿈되고 있어", "변신되고 있어"),
        ("탈바꿈하고 있어", "변신하고 있어"),
        ("탈바꿈됐어", "변신했어"),
        ("탈바꿈되었어", "변신했어"),
        ("탈바꿈했어", "변신했어"),
        ("탈바꿈했다", "변신했다"),
        ("변형되고 있어", "변신되고 있어"),
        ("변형하고 있어", "변신하고 있어"),
        ("변형시키고 있어", "변신시키고 있어"),
        ("변형됐어", "변신했어"),
        ("변형되었어", "변신했어"),
        ("변형했어", "변신했어"),
        ("변형했다", "변신했다"),
        ("변환되고 있어", "변신되고 있어"),
        ("변환하고 있어", "변신하고 있어"),
        ("변환시키고 있어", "변신시키고 있어"),
        ("변환됐어", "변신했어"),
        ("변환되었어", "변신했어"),
        ("변환했어", "변신했어"),
        ("변환했다", "변신했다"),
        ("변신되고 있어", "변신되고 있어"),
        ("변신하고 있어", "변신하고 있어"),
        ("변신됐어", "변신했어"),
        ("변신되었어", "변신했어"),
        ("변신했어", "변신했어"),
        ("변신했다", "변신했다"),
    )
    for old, new in replacements:
        text = text.replace(old, new)
    return text


def _normalize_starcraft116_non_zerg_morph_terms(text):
    replacements = (
        ("변태 중이야", "변신 중이야"),
        ("변태시켰어", "변신시켰어"),
        ("변태되고 있어", "변신되고 있어"),
        ("변태하고 있어", "변신하고 있어"),
        ("변태시키고 있어", "변신시키고 있어"),
        ("변태되었어", "변신되었어"),
        ("변태됐어", "변신됐어"),
        ("변태했어", "변신했어"),
        ("변태했다", "변신했다"),
    )
    for old, new in replacements:
        text = text.replace(old, new)
    return text


def _normalize_starcraft116_generic_monster_morph_terms(text):
    replacements = (
        ("내가 유닛을 변신했어", "내가 유닛을 변태했어"),
        ("유닛이 변신했어", "유닛이 변태했어"),
        ("유닛을 변신했어", "유닛을 변태했어"),
        ("변신했어", "변태했어"),
        ("변신되었어", "변태되었어"),
        ("변신됐어", "변태됐어"),
        ("변신됐다", "변태됐다"),
        ("변신했어", "변태했어"),
        ("변신하고 있어", "변태하고 있어"),
        ("변신되고 있어", "변태되고 있어"),
        ("변신 중이야", "변태 중이야"),
        ("변신중이야", "변태중이야"),
        ("변신 중이네", "변태 중이네"),
        ("변신시키고 있어", "변태시키고 있어"),
        ("변형했어", "변태했어"),
        ("변형되었어", "변태되었어"),
        ("변형됐어", "변태됐어"),
        ("변형됐다", "변태됐다"),
        ("변형하고 있어", "변태하고 있어"),
        ("변형되고 있어", "변태되고 있어"),
        ("변형시키고 있어", "변태시키고 있어"),
        ("변환했어", "변태했어"),
        ("변환되었어", "변태되었어"),
        ("변환됐어", "변태됐어"),
        ("변환됐다", "변태됐다"),
        ("변환하고 있어", "변태하고 있어"),
        ("변환되고 있어", "변태되고 있어"),
        ("변환시키고 있어", "변태시키고 있어"),
        ("변태되었다", "변태되었다"),
    )
    for old, new in replacements:
        text = text.replace(old, new)
    return text


def _normalize_starcraft116_generic_morph_terms(text):
    replacements = (
        ("유닛을 변태했어", "유닛을 변신했어"),
        ("유닛이 변태했어", "유닛이 변신했어"),
        ("변태했어", "변신했어"),
        ("변태되고 있어", "변신되고 있어"),
        ("변태하고 있어", "변신하고 있어"),
        ("변태시키고 있어", "변신시키고 있어"),
        ("변태되었어", "변신되었어"),
        ("변태됐어", "변신됐어"),
        ("변태됐다", "변신됐다"),
        ("변태했다", "변신했다"),
    )
    for old, new in replacements:
        text = text.replace(old, new)
    return text


def _is_starcraft116_morph_event(event):
    event_type = str(event.get("event_type", "") or "").lower()
    return event_type in {"unit_morphed", "morph", "morphed"}


def _mentions_starcraft116_zerg_morph_target(event, text):
    if event.get("source") != "game_event":
        return False

    for source in (event, event.get("details", {})):
        if not isinstance(source, dict):
            continue
        for key in ("unit", "friendly_unit", "enemy_unit"):
            if _starcraft116_unit_payload_is_zerg_morph(source.get(key)):
                return True

    for mention in event.get("unit_mentions", []):
        raw_type = _starcraft116_base_type(mention.get("raw_type", ""))
        if raw_type in STARCRAFT116_ZERG_MORPH_BASE_TYPES:
            return True

    for base_type in STARCRAFT116_ZERG_MORPH_BASE_TYPES:
        speak_name = STARCRAFT116_UNIT_TYPE_BASE_SPEAK_NAMES.get(base_type, "")
        if speak_name and _contains_starcraft116_english_name(text, speak_name):
            return True
    return False


def _mentions_starcraft116_terran_protoss_building_morph_target(event, text):
    if event.get("source") != "game_event":
        return False

    for source in (event, event.get("details", {})):
        if not isinstance(source, dict):
            continue
        for key in ("unit", "friendly_unit", "enemy_unit"):
            payload = source.get(key)
            if not isinstance(payload, dict):
                continue
            raw_type = _starcraft116_base_type(payload.get("type", ""))
            if raw_type in STARCRAFT116_TERRAN_PROTOSS_BUILDING_MORPH_BASE_TYPES:
                return True

    for mention in event.get("unit_mentions", []):
        raw_type = _starcraft116_base_type(mention.get("raw_type", ""))
        if raw_type in STARCRAFT116_TERRAN_PROTOSS_BUILDING_MORPH_BASE_TYPES:
            return True

    for base_type in STARCRAFT116_TERRAN_PROTOSS_BUILDING_MORPH_BASE_TYPES:
        speak_name = STARCRAFT116_UNIT_TYPE_BASE_SPEAK_NAMES.get(base_type, "")
        if speak_name and _contains_starcraft116_english_name(text, speak_name):
            return True
    return False


def _is_starcraft116_zerg_morph_context(event):
    if event.get("source") != "game_event":
        return False

    if _is_starcraft116_zerg_profile(event.get("profile", "")):
        return True

    race_values = (
        event.get("self_race", ""),
        event.get("race", ""),
        event.get("player_race", ""),
    )
    if any(_is_starcraft116_zerg_race(value) for value in race_values):
        return True

    for source in (event, event.get("details", {})):
        if not isinstance(source, dict):
            continue
        for key in ("self_race", "player_race", "race"):
            if _is_starcraft116_zerg_race(source.get(key, "")):
                return True
        for key in ("unit", "friendly_unit"):
            payload = source.get(key)
            if _starcraft116_unit_payload_has_self_zerg_race(payload):
                return True

    return False


def _starcraft116_unit_payload_has_self_zerg_race(payload):
    if not isinstance(payload, dict):
        return False
    owner = str(payload.get("owner", "") or "").strip().lower()
    if owner and owner != "self":
        return False
    return _is_starcraft116_zerg_race(payload.get("race", ""))


def _is_starcraft116_zerg_race(value):
    return str(value or "").strip().lower() in STARCRAFT116_ZERG_RACE_NAMES


def _is_starcraft116_zerg_profile(value):
    profile = str(value or "").strip().lower()
    return profile in STARCRAFT116_ZERG_PROFILE_NAMES


def _starcraft116_unit_payload_is_zerg_morph(payload):
    if not isinstance(payload, dict):
        return False

    raw_type = _starcraft116_base_type(payload.get("type", ""))
    if raw_type in STARCRAFT116_ZERG_MORPH_BASE_TYPES:
        return True
    if str(payload.get("type", "") or "").strip().lower().startswith("zerg "):
        return True
    if _starcraft116_unit_payload_has_self_zerg_race(payload):
        return True

    try:
        type_id = int(payload.get("type_id"))
    except (TypeError, ValueError):
        return False
    return type_id in STARCRAFT116_ZERG_MORPH_TYPE_IDS


def _mentions_starcraft116_building_fight(event, text):
    enemy = _find_starcraft116_unit_mention(event, ("enemy_unit", "unit"))
    if not enemy or not _is_starcraft116_building(enemy.get("raw_type", "")):
        return False

    enemy_name = str(enemy.get("speak_name", "") or "").strip()
    if not enemy_name:
        return False
    fight_fragments = (
        f"{enemy_name}와 싸우",
        f"{enemy_name}과 싸우",
        f"{enemy_name}랑 싸우",
        f"{enemy_name}하고 싸우",
    )
    return any(fragment in text for fragment in fight_fragments)


def _game_event_details(raw_event):
    detail_keys = (
        "details",
        "resources",
        "units",
        "combat",
        "build_order",
        "production",
        "supply",
        "workers",
        "army",
        "enemy",
        "scouting",
        "map_state",
        "score",
        "unit",
        "friendly_unit",
        "enemy_unit",
    )
    details = {}
    for key in detail_keys:
        if key in raw_event:
            details[key] = raw_event.get(key)
    if details:
        return details

    ignored = {
        "event_id",
        "id",
        "event_type",
        "type",
        "name",
        "kind",
        "summary",
        "message",
        "description",
        "text",
        "severity",
        "level",
        "frame",
        "game_frame",
        "game_time_seconds",
        "time_seconds",
        "seconds",
        "profile",
        "player",
        "self_player",
        "race",
        "map",
        "map_name",
    }
    for key, value in raw_event.items():
        if key not in ignored:
            details[str(key)] = value
    return details


def _first_text(payload, keys):
    for key in keys:
        value = payload.get(key)
        if value is None:
            continue
        text = str(value).strip()
        if text:
            return text
    return ""


def _compact_value(value, max_chars):
    try:
        text = json.dumps(value, ensure_ascii=False, sort_keys=True, default=str)
    except TypeError:
        text = str(value)
    if len(text) > max_chars:
        return text[: max_chars - 3].rstrip() + "..."
    return text


def _raw_event_hash(raw_event):
    text = _compact_value(raw_event, 4000)
    return hashlib.sha1(text.encode("utf-8", errors="replace")).hexdigest()[:16]


def _string_list(value):
    if not isinstance(value, list):
        return []
    return [
        str(item).strip()
        for item in value
        if str(item).strip()
    ]
