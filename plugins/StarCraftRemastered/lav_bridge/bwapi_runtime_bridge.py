# #20260701_kpopmodder: File-based bridge for SAIDA-style shim experiments without injection.
# import json
# import os
# import time

# from plugins.StarCraftRemastered.core.command import CommandType, StarCraftCommand


# class BWAPIRuntimeBridge:
#     #20260701_kpopmodder: Exchanges snapshots/commands through files so native code stays isolated.
#     SNAPSHOT_SCHEMA = "lav_bwapi_rm_snapshot_v1"
#     COMMAND_SCHEMA = "lav_bwapi_rm_command_v1"

#     def __init__(self, snapshot_path, command_queue_path):
#         self.snapshot_path = snapshot_path
#         self.command_queue_path = command_queue_path

#     def write_snapshot(self, game_state):
#         payload = self.snapshot_payload(game_state)
#         self._ensure_parent(self.snapshot_path)
#         temp_path = self.snapshot_path + ".tmp"
#         with open(temp_path, "w", encoding="utf-8") as file:
#             json.dump(payload, file, ensure_ascii=False, indent=2)
#         os.replace(temp_path, self.snapshot_path)
#         return payload

#     def snapshot_payload(self, game_state):
#         state = game_state.to_dict()
#         return {
#             "schema": self.SNAPSHOT_SCHEMA,
#             "written_at": time.time(),
#             "safety": {
#                 "single_player_only": True,
#                 "battle_net_blocked": not bool(state.get("is_battlenet_screen")),
#                 "multiplayer_blocked": not bool(state.get("is_multiplayer_screen")),
#                 "reason": state.get("safety_reason", ""),
#             },
#             "game": {
#                 "connected": bool(state.get("is_connected")),
#                 "in_game": bool(state.get("is_in_game")),
#                 "single_player": bool(state.get("is_single_player")),
#                 "battle_net_screen": bool(state.get("is_battlenet_screen")),
#                 "multiplayer_screen": bool(state.get("is_multiplayer_screen")),
#                 "frame_count": int(state.get("frame_count") or 0),
#                 "map_name": state.get("map_name", "") or "",
#                 "map_width": self._int_or_zero(state.get("map_width")),
#                 "map_height": self._int_or_zero(state.get("map_height")),
#                 "self": self._player_payload(
#                     state.get("self_player", {}),
#                     state,
#                     state.get("my_start_location"),
#                 ),
#                 "enemy": self._player_payload(
#                     state.get("enemy_player", {}),
#                     state,
#                     state.get("enemy_start_location"),
#                 ),
#             },
#             "units": {
#                 "my": list(state.get("my_units", [])),
#                 "enemy": list(state.get("enemy_units", [])),
#                 "neutral": list(state.get("neutral_units", [])),
#             },
#             "observation": state.get("last_screen_observation", "") or "",
#         }

#     def read_pending_commands(self, clear=True):
#         if not os.path.isfile(self.command_queue_path):
#             return []

#         commands = []
#         with open(self.command_queue_path, "r", encoding="utf-8") as file:
#             for line in file:
#                 line = line.strip()
#                 if not line:
#                     continue
#                 try:
#                     payload = json.loads(line)
#                     commands.append(self.command_from_payload(payload))
#                 except Exception:
#                     continue

#         if clear:
#             self._ensure_parent(self.command_queue_path)
#             open(self.command_queue_path, "w", encoding="utf-8").close()
#         return commands

#     def command_from_payload(self, payload):
#         command_type = self._command_type(payload.get("type"))
#         target_position = payload.get("target_position")
#         if isinstance(target_position, list):
#             target_position = tuple(target_position[:2])

#         return StarCraftCommand(
#             command_type=command_type,
#             unit_ids=[int(unit_id) for unit_id in payload.get("unit_ids", [])],
#             target_unit_id=payload.get("target_unit_id"),
#             target_position=target_position,
#             ability_name=payload.get("ability_name"),
#             building_name=payload.get("building_name"),
#             unit_name=payload.get("unit_name"),
#             raw_payload=payload,
#         )

#     def _command_type(self, value):
#         value = str(value or "CHAT_LOG_ONLY").upper()
#         aliases = {
#             "LOGONLY": "CHAT_LOG_ONLY",
#             "HOLDPOSITION": "HOLD",
#             "RIGHTCLICK": "RIGHT_CLICK",
#             "USETECH": "USE_TECH",
#         }
#         value = aliases.get(value, value)
#         try:
#             return CommandType(value)
#         except Exception:
#             return CommandType.CHAT_LOG_ONLY

#     def _player_payload(self, player, state, start_location=None):
#         return {
#             "id": player.get("player_id") or 0,
#             "name": player.get("name", "") or "",
#             "race": player.get("race") or state.get("player_race") or "",
#             "minerals": self._int_or_zero(player.get("minerals", state.get("minerals"))),
#             "gas": self._int_or_zero(player.get("gas", state.get("gas"))),
#             "supply_used": self._int_or_zero(
#                 player.get("supply_used", state.get("supply_used"))
#             ),
#             "supply_total": self._int_or_zero(
#                 player.get("supply_total", state.get("supply_total"))
#             ),
#             "start_location": self._position_payload(start_location),
#         }

#     def _position_payload(self, value):
#         if isinstance(value, (list, tuple)) and len(value) >= 2:
#             return [self._int_or_zero(value[0]), self._int_or_zero(value[1])]
#         return None

#     def _int_or_zero(self, value):
#         try:
#             return int(value)
#         except Exception:
#             return 0

#     def _ensure_parent(self, path):
#         parent = os.path.dirname(path)
#         if parent:
#             os.makedirs(parent, exist_ok=True)
