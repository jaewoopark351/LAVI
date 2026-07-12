# #20260701_kpopmodder: Added game state snapshots for safe BWAPI-compatible adapters.
# import json
# import time
# from dataclasses import asdict, dataclass, field
# from typing import List, Optional, Tuple

# from .unit import StarCraftUnit


# @dataclass
# class StarCraftPlayer:
#     #20260701_kpopmodder: Minimal player snapshot for SAIDA strategy-code adapters.
#     player_id: Optional[int] = None
#     name: str = ""
#     race: Optional[str] = None
#     minerals: Optional[int] = None
#     gas: Optional[int] = None
#     supply_used: Optional[int] = None
#     supply_total: Optional[int] = None

#     def to_dict(self):
#         return asdict(self)


# @dataclass
# class StarCraftGameState:
#     #20260701_kpopmodder: Unknown values stay Optional because ScreenVision is not a BWAPI memory feed.
#     is_connected: bool = False
#     is_in_game: bool = False
#     is_single_player: bool = True
#     is_battlenet_screen: bool = False
#     is_multiplayer_screen: bool = False
#     player_race: Optional[str] = None
#     enemy_race: Optional[str] = None
#     minerals: Optional[int] = None
#     gas: Optional[int] = None
#     supply_used: Optional[int] = None
#     supply_total: Optional[int] = None
#     my_units: List[StarCraftUnit] = field(default_factory=list)
#     enemy_units: List[StarCraftUnit] = field(default_factory=list)
#     neutral_units: List[StarCraftUnit] = field(default_factory=list)
#     self_player: StarCraftPlayer = field(default_factory=StarCraftPlayer)
#     enemy_player: StarCraftPlayer = field(default_factory=StarCraftPlayer)
#     frame_count: int = 0
#     map_name: str = ""
#     map_width: Optional[int] = None
#     map_height: Optional[int] = None
#     game_type: str = "single_player"
#     my_start_location: Optional[Tuple[int, int]] = None
#     enemy_start_location: Optional[Tuple[int, int]] = None
#     last_screen_observation: str = ""
#     last_update_time: float = field(default_factory=time.time)
#     safety_reason: str = ""

#     def refresh_timestamp(self):
#         self.last_update_time = time.time()

#     def all_units(self):
#         return list(self.my_units) + list(self.enemy_units) + list(self.neutral_units)

#     def mark_screen_observation(self, text):
#         self.last_screen_observation = str(text or "").strip()
#         self.refresh_timestamp()

#     def to_dict(self):
#         return asdict(self)

#     def to_json(self):
#         return json.dumps(self.to_dict(), ensure_ascii=False, indent=2)
