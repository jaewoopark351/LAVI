# #20260701_kpopmodder: Writes the read-only Samase state contract used by the BWAPI-RM bridge.
# import json
# import os
# import time

# from plugins.StarCraftRemastered.core.game_state import StarCraftGameState
# from plugins.StarCraftRemastered.lav_bridge.bwapi_runtime_bridge import (
#     BWAPIRuntimeBridge,
# )


# class SamaseReadonlyStateWriter:
#     #20260701_kpopmodder: This writer serializes state only; it does not read process memory.
#     STATE_SCHEMA = "lav_samase_readonly_state_v1"

#     def __init__(
#         self,
#         state_path,
#         bwapi_snapshot_path=None,
#         command_queue_path=None,
#     ):
#         self.state_path = state_path
#         self.bwapi_snapshot_path = bwapi_snapshot_path
#         self.command_queue_path = command_queue_path or ""

#     def write_state(self, game_state, source="samase"):
#         payload = self.state_payload(game_state, source=source)
#         self.write_payload(payload)
#         if self.bwapi_snapshot_path:
#             BWAPIRuntimeBridge(
#                 self.bwapi_snapshot_path,
#                 self.command_queue_path,
#             ).write_snapshot(game_state)
#         return payload

#     def write_payload(self, payload):
#         self._write_json_atomic(self.state_path, payload)
#         return payload

#     def state_payload(self, game_state, source="samase"):
#         if not isinstance(game_state, StarCraftGameState):
#             raise TypeError("game_state must be StarCraftGameState")

#         state = game_state.to_dict()
#         return {
#             "schema": self.STATE_SCHEMA,
#             "written_at": time.time(),
#             "source": str(source or "samase"),
#             "safety": {
#                 "single_player_only": True,
#                 "battle_net_blocked": not bool(state.get("is_battlenet_screen")),
#                 "multiplayer_blocked": not bool(state.get("is_multiplayer_screen")),
#                 "reason": state.get("safety_reason", "") or "",
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
#                 "my_start_location": self._position_payload(
#                     state.get("my_start_location")
#                 ),
#                 "enemy_start_location": self._position_payload(
#                     state.get("enemy_start_location")
#                 ),
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
#                 "my": [self._unit_payload(unit) for unit in state.get("my_units", [])],
#                 "enemy": [
#                     self._unit_payload(unit)
#                     for unit in state.get("enemy_units", [])
#                 ],
#                 "neutral": [
#                     self._unit_payload(unit)
#                     for unit in state.get("neutral_units", [])
#                 ],
#             },
#             "observation": state.get("last_screen_observation", "") or "",
#         }

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

#     def _unit_payload(self, unit):
#         return {
#             "unit_id": self._int_or_zero(unit.get("unit_id")),
#             "unit_type": unit.get("unit_type", "") or "",
#             "owner": unit.get("owner", "") or "",
#             "owner_id": self._optional_int(unit.get("owner_id")),
#             "x": self._optional_int(unit.get("x")),
#             "y": self._optional_int(unit.get("y")),
#             "hp": self._optional_int(unit.get("hp")),
#             "shields": self._optional_int(unit.get("shields")),
#             "energy": self._optional_int(unit.get("energy")),
#             "resources": self._optional_int(unit.get("resources")),
#             "is_completed": bool(unit.get("is_completed")),
#             "is_selected": bool(unit.get("is_selected")),
#             "current_order": unit.get("current_order", "") or "",
#             "is_visible": bool(unit.get("is_visible", True)),
#             "is_flying": bool(unit.get("is_flying")),
#             "is_idle": bool(unit.get("is_idle")),
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

#     def _optional_int(self, value):
#         if value is None:
#             return None
#         try:
#             return int(value)
#         except Exception:
#             return None

#     def _write_json_atomic(self, path, payload):
#         parent = os.path.dirname(path)
#         if parent:
#             os.makedirs(parent, exist_ok=True)
#         temp_path = path + ".tmp"
#         with open(temp_path, "w", encoding="utf-8") as file:
#             json.dump(payload, file, ensure_ascii=False, indent=2)
#         os.replace(temp_path, path)
