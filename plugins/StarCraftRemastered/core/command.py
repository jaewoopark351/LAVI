# #20260701_kpopmodder: Added command models for the LAV-BWAPI-RM compatibility layer.
# import time
# from dataclasses import dataclass, field
# from enum import Enum
# from typing import Any, Dict, List, Optional, Tuple


# class CommandType(str, Enum):
#     #20260701_kpopmodder: Keep these names close to BWAPI/SAIDA intent without executing input yet.
#     TRAIN = "TRAIN"
#     BUILD = "BUILD"
#     MOVE = "MOVE"
#     ATTACK = "ATTACK"
#     STOP = "STOP"
#     HOLD = "HOLD"
#     SELECT = "SELECT"
#     HOTKEY = "HOTKEY"
#     CAMERA_MOVE = "CAMERA_MOVE"
#     CHAT_LOG_ONLY = "CHAT_LOG_ONLY"
#     RIGHT_CLICK = "RIGHT_CLICK"
#     GATHER = "GATHER"
#     REPAIR = "REPAIR"
#     RESEARCH = "RESEARCH"
#     UPGRADE = "UPGRADE"
#     USE_TECH = "USE_TECH"


# Position = Tuple[int, int]


# @dataclass
# class StarCraftCommand:
#     #20260701_kpopmodder: A data-only command envelope; providers decide whether it is safe to act.
#     command_type: CommandType
#     unit_ids: List[int] = field(default_factory=list)
#     target_unit_id: Optional[int] = None
#     target_position: Optional[Position] = None
#     ability_name: Optional[str] = None
#     building_name: Optional[str] = None
#     unit_name: Optional[str] = None
#     raw_payload: Dict[str, Any] = field(default_factory=dict)
#     created_at: float = field(default_factory=time.time)

#     def to_dict(self):
#         return {
#             "command_type": self.command_type.value,
#             "unit_ids": list(self.unit_ids),
#             "target_unit_id": self.target_unit_id,
#             "target_position": self.target_position,
#             "ability_name": self.ability_name,
#             "building_name": self.building_name,
#             "unit_name": self.unit_name,
#             "raw_payload": dict(self.raw_payload),
#             "created_at": self.created_at,
#         }

#     @classmethod
#     def chat_log_only(cls, message):
#         return cls(
#             command_type=CommandType.CHAT_LOG_ONLY,
#             raw_payload={"message": str(message or "")},
#         )
