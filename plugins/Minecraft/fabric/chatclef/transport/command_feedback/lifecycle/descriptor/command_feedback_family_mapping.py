#20260907_kpopmodder: Keep exact command-to-feedback-family coverage immutable.
from types import MappingProxyType


COMMAND_FEEDBACK_FAMILIES = MappingProxyType(
    {
        "attack": "generic",
        "auto_deposit_trust": "registry",
        "auto_deposit_trusted_list": "query",
        "auto_deposit_untrust": "registry",
        "chatclef": "settings",
        "deposit": "item_deposit",
        "deposit_all": "item_deposit",
        "equip": "item_equip",
        "follow": "movement_follow",
        "food": "food_acquisition",
        "gamer": "generic",
        "gamma": "settings",
        "get": "item_get",
        "give": "item_give",
        "goto": "movement_goto",
        "hero": "persistent",
        "idle": "persistent",
        "locate_structure": "query",
        "meat": "meat_acquisition",
        "overlay": "settings",
        "reload_settings": "settings",
        "resetmemory": "settings",
        "scan": "query",
        "stop": "control",
        "store_home": "store_home",
        "자동보관등록": "registry",
    }
)


__all__ = ("COMMAND_FEEDBACK_FAMILIES",)
