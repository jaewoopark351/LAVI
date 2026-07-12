# #20260701_kpopmodder: Added BWAPI-style wrapper so SAIDA logic can be ported safely.
# from plugins.StarCraftRemastered.core.command import CommandType, StarCraftCommand


# class BWAPICompatProvider:
#     #20260701_kpopmodder: Exposes BWAPI-like names while delegating safety to the underlying provider.
#     def __init__(self, provider):
#         self.provider = provider

#     def connect(self):
#         return self.provider.connect()

#     def disconnect(self):
#         return self.provider.disconnect()

#     def is_available(self):
#         return self.provider.is_available()

#     def get_game_state(self):
#         return self.provider.get_game_state()

#     def safety_check(self, game_state=None):
#         return self.provider.safety_check(game_state)

#     def stop_all_control(self):
#         return self.provider.stop_all_control()

#     def send_command(self, command):
#         return self.provider.send_command(command)

#     def get_my_units(self):
#         return list(self.get_game_state().my_units)

#     def get_enemy_units(self):
#         return list(self.get_game_state().enemy_units)

#     def get_all_units(self):
#         return self.get_game_state().all_units()

#     def get_frame_count(self):
#         return int(self.get_game_state().frame_count or 0)

#     def self_player(self):
#         return self.get_game_state().self_player

#     def enemy_player(self):
#         return self.get_game_state().enemy_player

#     def get_units_in_radius(self, x, y, radius):
#         radius_squared = int(radius) ** 2
#         units = []
#         for unit in self.get_all_units():
#             distance_squared = unit.distance_squared_to(x, y)
#             if distance_squared is not None and distance_squared <= radius_squared:
#                 units.append(unit)
#         return units

#     def train(self, unit_name):
#         return self.send_command(
#             StarCraftCommand(
#                 command_type=CommandType.TRAIN,
#                 unit_name=str(unit_name or ""),
#             )
#         )

#     def build(self, building_name, x=None, y=None):
#         return self.send_command(
#             StarCraftCommand(
#                 command_type=CommandType.BUILD,
#                 building_name=str(building_name or ""),
#                 target_position=self._position(x, y),
#             )
#         )

#     def move(self, unit_ids, x, y):
#         return self.send_command(
#             StarCraftCommand(
#                 command_type=CommandType.MOVE,
#                 unit_ids=self._unit_ids(unit_ids),
#                 target_position=(int(x), int(y)),
#             )
#         )

#     def attack(self, unit_ids, target=None, x=None, y=None):
#         return self.send_command(
#             StarCraftCommand(
#                 command_type=CommandType.ATTACK,
#                 unit_ids=self._unit_ids(unit_ids),
#                 target_unit_id=self._target_unit_id(target),
#                 target_position=self._position(x, y),
#             )
#         )

#     def right_click(self, unit_ids, target=None, x=None, y=None):
#         return self.send_command(
#             StarCraftCommand(
#                 command_type=CommandType.RIGHT_CLICK,
#                 unit_ids=self._unit_ids(unit_ids),
#                 target_unit_id=self._target_unit_id(target),
#                 target_position=self._position(x, y),
#             )
#         )

#     def gather(self, unit_ids, target):
#         return self.send_command(
#             StarCraftCommand(
#                 command_type=CommandType.GATHER,
#                 unit_ids=self._unit_ids(unit_ids),
#                 target_unit_id=self._target_unit_id(target),
#             )
#         )

#     def repair(self, unit_ids, target):
#         return self.send_command(
#             StarCraftCommand(
#                 command_type=CommandType.REPAIR,
#                 unit_ids=self._unit_ids(unit_ids),
#                 target_unit_id=self._target_unit_id(target),
#             )
#         )

#     def research(self, tech_name):
#         return self.send_command(
#             StarCraftCommand(
#                 command_type=CommandType.RESEARCH,
#                 ability_name=str(tech_name or ""),
#             )
#         )

#     def upgrade(self, upgrade_name):
#         return self.send_command(
#             StarCraftCommand(
#                 command_type=CommandType.UPGRADE,
#                 ability_name=str(upgrade_name or ""),
#             )
#         )

#     def use_tech(self, unit_ids, tech_name, target=None, x=None, y=None):
#         return self.send_command(
#             StarCraftCommand(
#                 command_type=CommandType.USE_TECH,
#                 unit_ids=self._unit_ids(unit_ids),
#                 target_unit_id=self._target_unit_id(target),
#                 target_position=self._position(x, y),
#                 ability_name=str(tech_name or ""),
#             )
#         )

#     def hold_position(self, unit_ids):
#         return self.send_command(
#             StarCraftCommand(
#                 command_type=CommandType.HOLD,
#                 unit_ids=self._unit_ids(unit_ids),
#             )
#         )

#     def stop(self, unit_ids):
#         return self.send_command(
#             StarCraftCommand(
#                 command_type=CommandType.STOP,
#                 unit_ids=self._unit_ids(unit_ids),
#             )
#         )

#     def select(self, unit_ids):
#         return self.send_command(
#             StarCraftCommand(
#                 command_type=CommandType.SELECT,
#                 unit_ids=self._unit_ids(unit_ids),
#             )
#         )

#     def hotkey(self, key, action="select"):
#         return self.send_command(
#             StarCraftCommand(
#                 command_type=CommandType.HOTKEY,
#                 raw_payload={"key": str(key or ""), "action": str(action or "")},
#             )
#         )

#     def camera_move(self, x, y):
#         return self.send_command(
#             StarCraftCommand(
#                 command_type=CommandType.CAMERA_MOVE,
#                 target_position=(int(x), int(y)),
#             )
#         )

#     def chat_log_only(self, message):
#         return self.send_command(StarCraftCommand.chat_log_only(message))

#     def _unit_ids(self, unit_ids):
#         if unit_ids is None:
#             return []
#         if isinstance(unit_ids, int):
#             return [unit_ids]
#         return [int(unit_id) for unit_id in unit_ids]

#     def _target_unit_id(self, target):
#         if target is None:
#             return None
#         if isinstance(target, int):
#             return target
#         return getattr(target, "unit_id", None)

#     def _position(self, x, y):
#         if x is None or y is None:
#             return None
#         return (int(x), int(y))
