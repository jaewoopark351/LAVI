# #20260701_kpopmodder: Added Samase provider stub for launch/state coordination only.
# import json
# import os

# from plugins.StarCraftRemastered.core.game_state import (
#     StarCraftGameState,
#     StarCraftPlayer,
# )
# from plugins.StarCraftRemastered.core.unit import StarCraftUnit
# from plugins.StarCraftRemastered.providers.base_provider import StarCraftProvider
# from plugins.StarCraftRemastered.starcraft_launcher import StarCraftLauncher


# class SamaseProvider(StarCraftProvider):
#     #20260701_kpopmodder: Reads Samase-side snapshots only; it never writes game input.
#     def __init__(self, config=None, launcher=None, log_router=None):
#         super().__init__(config=config, log_router=log_router)
#         self.launcher = launcher or StarCraftLauncher(config)
#         self.process = None
#         self.state = StarCraftGameState(is_connected=False)
#         self.state_path = self._resolve_state_path()
#         self.last_load_error = ""

#     def connect(self):
#         if not self._config_bool("enabled", False):
#             self._log_event("Samase provider skipped because enabled=false")
#             return False

#         if not self._config_bool("auto_launch", False):
#             self.state.is_connected = True
#             self.state.refresh_timestamp()
#             self._log_event(
#                 "Samase provider ready; auto_launch=false; read-only state file="
#                 f"{self.state_path}"
#             )
#             return True

#         result = self.launcher.launch()
#         if result.ok:
#             self.process = result.process
#             self.state.is_connected = True
#             self._log_event(result.message)
#             return True

#         self._log_event(f"Samase provider launch skipped: {result.message}")
#         return False

#     def disconnect(self):
#         self.stop_all_control()
#         self.state.is_connected = False
#         self.state.refresh_timestamp()
#         self._log_event("Samase provider disconnected")
#         return True

#     def is_available(self):
#         return True

#     def get_game_state(self):
#         if self.process is not None:
#             exit_code = self.process.poll()
#             self.state.is_connected = exit_code is None

#         loaded_state = self._load_readonly_state()
#         if loaded_state is not None:
#             connected = self.state.is_connected or loaded_state.is_connected
#             self.state = loaded_state
#             self.state.is_connected = connected

#         self.state.refresh_timestamp()
#         return self.state

#     def send_command(self, command):
#         self._log_command(command)
#         if not self.safety_check(self.state):
#             self.stop_all_control()
#             return False
#         self._log_event("Samase command logged; read-only bridge will not execute it")
#         return False

#     def stop_all_control(self):
#         self._log_event("Samase stop_all_control called")
#         return True

#     def _load_readonly_state(self):
#         if not self.state_path or not os.path.isfile(self.state_path):
#             return None

#         try:
#             with open(self.state_path, "r", encoding="utf-8") as file:
#                 payload = json.load(file)
#         except Exception as e:
#             self.last_load_error = str(e)
#             self._log_event(f"Samase read-only state load failed: {e}")
#             return None

#         try:
#             state = self._state_from_payload(payload)
#             self.last_load_error = ""
#             return state
#         except Exception as e:
#             self.last_load_error = str(e)
#             self._log_event(f"Samase read-only state parse failed: {e}")
#             return None

#     def _state_from_payload(self, payload):
#         if not isinstance(payload, dict):
#             raise ValueError("Samase state root must be a JSON object")

#         game = payload.get("game")
#         if not isinstance(game, dict):
#             game = payload

#         units = payload.get("units")
#         if not isinstance(units, dict):
#             units = {}

#         self_player = self._player_from_payload(
#             game.get("self") or payload.get("self_player") or {},
#             fallback_name="Self",
#             fallback_id=1,
#         )
#         enemy_player = self._player_from_payload(
#             game.get("enemy") or payload.get("enemy_player") or {},
#             fallback_name="Enemy",
#             fallback_id=2,
#         )
#         self_payload = game.get("self") if isinstance(game.get("self"), dict) else {}
#         enemy_payload = game.get("enemy") if isinstance(game.get("enemy"), dict) else {}

#         state = StarCraftGameState(
#             is_connected=self._bool_value(game, "connected", "is_connected", True),
#             is_in_game=self._bool_value(game, "in_game", "is_in_game", False),
#             is_single_player=self._bool_value(
#                 game,
#                 "single_player",
#                 "is_single_player",
#                 True,
#             ),
#             is_battlenet_screen=self._bool_value(
#                 game,
#                 "battle_net_screen",
#                 "is_battlenet_screen",
#                 False,
#             ),
#             is_multiplayer_screen=self._bool_value(
#                 game,
#                 "multiplayer_screen",
#                 "is_multiplayer_screen",
#                 False,
#             ),
#             player_race=self_player.race or game.get("player_race"),
#             enemy_race=enemy_player.race or game.get("enemy_race"),
#             minerals=self._int_value(game, "minerals", self_player.minerals),
#             gas=self._int_value(game, "gas", self_player.gas),
#             supply_used=self._int_value(
#                 game,
#                 "supply_used",
#                 self_player.supply_used,
#             ),
#             supply_total=self._int_value(
#                 game,
#                 "supply_total",
#                 self_player.supply_total,
#             ),
#             self_player=self_player,
#             enemy_player=enemy_player,
#             frame_count=self._int_value(game, "frame_count", 0),
#             map_name=str(game.get("map_name") or payload.get("map_name") or ""),
#             map_width=self._optional_int(game.get("map_width") or game.get("mapWidth")),
#             map_height=self._optional_int(
#                 game.get("map_height") or game.get("mapHeight")
#             ),
#             game_type=str(game.get("game_type") or "single_player"),
#             my_start_location=self._position_value(
#                 game.get("my_start_location") or self_payload.get("start_location")
#             ),
#             enemy_start_location=self._position_value(
#                 game.get("enemy_start_location") or enemy_payload.get("start_location")
#             ),
#             last_screen_observation=str(payload.get("observation") or ""),
#             safety_reason=str(game.get("safety_reason") or ""),
#         )
#         state.my_units = self._units_from_payload(
#             units.get("my") or payload.get("my_units") or [],
#             fallback_owner="self",
#         )
#         state.enemy_units = self._units_from_payload(
#             units.get("enemy") or payload.get("enemy_units") or [],
#             fallback_owner="enemy",
#         )
#         state.neutral_units = self._units_from_payload(
#             units.get("neutral") or payload.get("neutral_units") or [],
#             fallback_owner="neutral",
#         )
#         return state

#     def _player_from_payload(self, payload, fallback_name, fallback_id):
#         if not isinstance(payload, dict):
#             payload = {}
#         return StarCraftPlayer(
#             player_id=self._optional_int(
#                 payload.get("player_id") or payload.get("id") or fallback_id
#             ),
#             name=str(payload.get("name") or fallback_name),
#             race=payload.get("race"),
#             minerals=self._optional_int(payload.get("minerals")),
#             gas=self._optional_int(payload.get("gas")),
#             supply_used=self._optional_int(payload.get("supply_used")),
#             supply_total=self._optional_int(payload.get("supply_total")),
#         )

#     def _units_from_payload(self, values, fallback_owner):
#         if not isinstance(values, list):
#             return []

#         units = []
#         for index, payload in enumerate(values, start=1):
#             if not isinstance(payload, dict):
#                 continue

#             position = self._position_value(payload.get("position"))
#             x_value = payload.get("x")
#             y_value = payload.get("y")
#             if position is not None:
#                 x_value, y_value = position

#             units.append(
#                 StarCraftUnit(
#                     unit_id=self._int_value(payload, "unit_id", payload.get("id", index)),
#                     unit_type=str(payload.get("unit_type") or payload.get("type") or ""),
#                     owner=str(payload.get("owner") or fallback_owner),
#                     owner_id=self._optional_int(payload.get("owner_id")),
#                     x=self._optional_int(x_value),
#                     y=self._optional_int(y_value),
#                     hp=self._optional_int(
#                         payload.get("hp")
#                         if "hp" in payload
#                         else payload.get("hit_points")
#                     ),
#                     shields=self._optional_int(payload.get("shields")),
#                     energy=self._optional_int(payload.get("energy")),
#                     resources=self._optional_int(payload.get("resources")),
#                     is_completed=self._bool_value(
#                         payload,
#                         "is_completed",
#                         "completed",
#                         False,
#                     ),
#                     is_selected=self._bool_value(
#                         payload,
#                         "is_selected",
#                         "selected",
#                         False,
#                     ),
#                     current_order=str(
#                         payload.get("current_order") or payload.get("order") or ""
#                     ),
#                     is_visible=self._bool_value(
#                         payload,
#                         "is_visible",
#                         "visible",
#                         True,
#                     ),
#                     is_flying=self._bool_value(
#                         payload,
#                         "is_flying",
#                         "flying",
#                         False,
#                     ),
#                     is_idle=self._bool_value(payload, "is_idle", "idle", False),
#                 )
#             )
#         return units

#     def _resolve_state_path(self):
#         if hasattr(self.config, "resolve_samase_state_path"):
#             try:
#                 return self.config.resolve_samase_state_path()
#             except Exception:
#                 pass

#         value = self._config_value("samase_state_path", "")
#         if not value:
#             return ""
#         value = os.path.expandvars(os.path.expanduser(str(value)))
#         if os.path.isabs(value):
#             return os.path.normpath(value)
#         return os.path.normpath(os.path.join(os.getcwd(), value))

#     def _bool_value(self, payload, key, alias=None, default=False):
#         if not isinstance(payload, dict):
#             return bool(default)
#         value = payload.get(key)
#         if value is None and alias:
#             value = payload.get(alias)
#         if value is None:
#             return bool(default)
#         if isinstance(value, bool):
#             return value
#         return str(value).strip().lower() in {"1", "true", "yes", "on"}

#     def _int_value(self, payload, key, default=0):
#         if isinstance(payload, dict):
#             value = payload.get(key, default)
#         else:
#             value = default
#         try:
#             return int(value)
#         except Exception:
#             try:
#                 return int(default)
#             except Exception:
#                 return 0

#     def _optional_int(self, value):
#         if value is None:
#             return None
#         try:
#             return int(value)
#         except Exception:
#             return None

#     def _position_value(self, value):
#         if isinstance(value, (list, tuple)) and len(value) >= 2:
#             x_value = self._optional_int(value[0])
#             y_value = self._optional_int(value[1])
#         elif isinstance(value, dict):
#             x_value = self._optional_int(value.get("x"))
#             y_value = self._optional_int(value.get("y"))
#         else:
#             return None

#         if x_value is None or y_value is None:
#             return None
#         return (x_value, y_value)
