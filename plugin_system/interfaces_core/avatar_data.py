#20260718_kpopmodder: Added shared avatar DTO without sharing mutable VtuberPluginInterface state.
from dataclasses import dataclass


@dataclass
class AvatarData:
    #20260718_kpopmodder: Per-instance avatar state for vtuber providers.
    mouth_open: float = 0
    # TODO current emotion, current pheonome etc
