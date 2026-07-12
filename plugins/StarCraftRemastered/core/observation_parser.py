# #20260701_kpopmodder: Parses ScreenVision text into a conservative BWAPI-RM game snapshot.
# import re

# from plugins.StarCraftRemastered.core.game_state import (
#     StarCraftGameState,
#     StarCraftPlayer,
# )
# from plugins.StarCraftRemastered.core.unit import StarCraftUnit


# RACE_ALIASES = (
#     ("Zerg", ("zerg", "저그")),
#     ("Terran", ("terran", "테란")),
#     ("Protoss", ("protoss", "프로토스")),
# )

# SELF_UNIT_ALIASES = (
#     ("Zerg Hatchery", ("hatchery", "해처리")),
#     ("Zerg Drone", ("drone", "드론", "일벌레")),
#     ("Zerg Overlord", ("overlord", "오버로드")),
#     ("Terran Command Center", ("command center", "커맨드 센터")),
#     ("Terran SCV", ("scv", "건설로봇")),
#     ("Terran Marine", ("marine", "마린")),
#     ("Protoss Nexus", ("nexus", "넥서스")),
#     ("Protoss Probe", ("probe", "프로브")),
# )

# NON_GAME_MARKERS = (
#     "visual studio code",
#     "vscode",
#     "explorer",
#     "browser",
#     "chrome",
#     "codex",
#     "gradio",
#     "main.py",
# )

# GAME_MARKERS = (
#     "brood war",
#     "starcraft",
#     "스타크래프트",
#     "hatchery",
#     "drone",
#     "overlord",
#     "해처리",
#     "드론",
#     "오버로드",
#     "mineral",
#     "minerals",
#     "미네랄",
#     "supply",
#     "서플",
# )


# class StarCraftObservationParser:
#     #20260701_kpopmodder: Keep parsing heuristic and reversible; ScreenVision is not raw BWAPI state.
#     def parse(self, text, previous_state=None):
#         observation = str(text or "").strip()
#         lowered = observation.lower()
#         state = previous_state or StarCraftGameState()
#         state.mark_screen_observation(observation)
#         state.is_connected = bool(observation)
#         state.is_battlenet_screen = self._contains_any(
#             lowered,
#             ("battle.net", "battlenet", "ladder", "로그인", "래더"),
#         )
#         state.is_multiplayer_screen = self._contains_any(
#             lowered,
#             ("multiplayer", "multi player", "멀티 플레이어", "멀티플레이어"),
#         )

#         looks_like_game = self._looks_like_game(lowered)
#         looks_like_non_game = self._looks_like_non_game(lowered)
#         state.is_in_game = looks_like_game and not (
#             looks_like_non_game and not self._has_strong_game_signal(lowered)
#         )
#         state.is_single_player = not state.is_battlenet_screen and not state.is_multiplayer_screen

#         race = self._parse_race(lowered)
#         if race:
#             state.player_race = race
#             state.self_player.race = race

#         minerals = self._parse_resource(lowered, ("minerals", "mineral", "미네랄"))
#         gas = self._parse_resource(lowered, ("gas", "vespene", "가스"))
#         supply = self._parse_supply(lowered)

#         if minerals is not None:
#             state.minerals = minerals
#             state.self_player.minerals = minerals
#         if gas is not None:
#             state.gas = gas
#             state.self_player.gas = gas
#         if supply is not None:
#             state.supply_used, state.supply_total = supply
#             state.self_player.supply_used = state.supply_used
#             state.self_player.supply_total = state.supply_total

#         state.self_player = self._ensure_self_player(state.self_player)
#         state.enemy_player = self._ensure_enemy_player(state.enemy_player)
#         state.my_units = self._parse_units(lowered)
#         state.frame_count += 1
#         state.refresh_timestamp()
#         return state

#     def _looks_like_game(self, lowered):
#         return self._contains_any(lowered, GAME_MARKERS)

#     def _looks_like_non_game(self, lowered):
#         return self._contains_any(lowered, NON_GAME_MARKERS)

#     def _has_strong_game_signal(self, lowered):
#         return bool(
#             self._parse_supply(lowered)
#             or self._parse_resource(lowered, ("minerals", "mineral", "미네랄")) is not None
#             or "hatchery" in lowered
#             or "해처리" in lowered
#         )

#     def _contains_any(self, text, markers):
#         return any(marker in text for marker in markers)

#     def _parse_race(self, lowered):
#         for race, aliases in RACE_ALIASES:
#             if self._contains_any(lowered, aliases):
#                 return race
#         if "hatchery" in lowered or "drone" in lowered or "해처리" in lowered:
#             return "Zerg"
#         if "command center" in lowered or "scv" in lowered:
#             return "Terran"
#         if "nexus" in lowered or "probe" in lowered:
#             return "Protoss"
#         return None

#     def _parse_resource(self, lowered, names):
#         for name in names:
#             patterns = (
#                 rf"{re.escape(name)}\s*[:=]?\s*(\d+)",
#                 rf"(\d+)\s*{re.escape(name)}",
#             )
#             for pattern in patterns:
#                 match = re.search(pattern, lowered)
#                 if match:
#                     return int(match.group(1))
#         return None

#     def _parse_supply(self, lowered):
#         match = re.search(r"(?:supply|서플|인구)\s*[:=]?\s*(\d+)\s*/\s*(\d+)", lowered)
#         if match:
#             return (int(match.group(1)), int(match.group(2)))

#         compact = re.search(r"\b(\d+)\s*/\s*(\d+)\b", lowered)
#         if compact:
#             used = int(compact.group(1))
#             total = int(compact.group(2))
#             if 0 <= used <= 400 and 0 < total <= 400:
#                 return (used, total)
#         return None

#     def _parse_units(self, lowered):
#         units = []
#         next_id = 1
#         for unit_type, aliases in SELF_UNIT_ALIASES:
#             if not self._contains_any(lowered, aliases):
#                 continue
#             count = self._parse_unit_count(lowered, aliases) or 1
#             for _ in range(count):
#                 units.append(
#                     StarCraftUnit(
#                         unit_id=next_id,
#                         unit_type=unit_type,
#                         owner="self",
#                         is_completed=True,
#                         is_visible=True,
#                     )
#                 )
#                 next_id += 1
#         return units

#     def _parse_unit_count(self, lowered, aliases):
#         for alias in aliases:
#             patterns = (
#                 rf"(\d+)\s*(?:x\s*)?{re.escape(alias)}",
#                 rf"{re.escape(alias)}\s*(?:x\s*)?(\d+)",
#             )
#             for pattern in patterns:
#                 match = re.search(pattern, lowered)
#                 if match:
#                     return int(match.group(1))
#         return None

#     def _ensure_self_player(self, player):
#         player = player or StarCraftPlayer()
#         player.player_id = player.player_id or 1
#         player.name = player.name or "Self"
#         return player

#     def _ensure_enemy_player(self, player):
#         player = player or StarCraftPlayer()
#         player.player_id = player.player_id or 2
#         player.name = player.name or "Enemy"
#         return player
