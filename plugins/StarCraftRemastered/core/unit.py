# #20260701_kpopmodder: Added BWAPI-style unit snapshot model for SAIDA compatibility work.
# from dataclasses import asdict, dataclass
# from typing import Optional


# @dataclass
# class StarCraftUnit:
#     #20260701_kpopmodder: This is a snapshot model, not a live memory-backed BWAPI Unit handle.
#     unit_id: int
#     unit_type: str = ""
#     owner: str = ""
#     owner_id: Optional[int] = None
#     x: Optional[int] = None
#     y: Optional[int] = None
#     hp: Optional[int] = None
#     shields: Optional[int] = None
#     energy: Optional[int] = None
#     resources: Optional[int] = None
#     is_completed: bool = False
#     is_selected: bool = False
#     current_order: str = ""
#     is_visible: bool = True
#     is_flying: bool = False
#     is_idle: bool = False

#     def position(self):
#         if self.x is None or self.y is None:
#             return None
#         return (self.x, self.y)

#     def distance_squared_to(self, x, y):
#         if self.x is None or self.y is None:
#             return None
#         return (self.x - int(x)) ** 2 + (self.y - int(y)) ** 2

#     def to_dict(self):
#         return asdict(self)
