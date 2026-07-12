#20260705_kpopmodder: Keep optional direct plugin metadata out of app composition code.
OPTIONAL_MODULE_MANIFEST = {
    "SongPlayer": {
        "module_path": "plugins.SongPlayer.SongPlayer",
        "class_name": "SongPlayer",
        "default_enabled": True,
    },
    "Chess": {
        "module_path": "plugins.Chess.Chess",
        "class_name": "Chess",
        "default_enabled": False,
    },
    "StarCraftRemastered": {
        "module_path": "plugins.StarCraftRemastered.starcraft_remastered",
        "class_name": "StarCraftRemastered",
        "default_enabled": False,
    },
    "StarCraft116": {
        "module_path": "plugins.StarCraft116.starcraft116",
        "class_name": "StarCraft116",
        "default_enabled": False,
    },
    "StarCraft2": {
        "module_path": "plugins.StarCraft2.starcraft2",
        "class_name": "StarCraft2",
        "default_enabled": False,
    },
    "ScreenVision": {
        "module_path": "plugins.ScreenVision.screenVision",
        "class_name": "ScreenVision",
        "default_enabled": True,
    },
}


def get_optional_module_manifest(module_name):
    return OPTIONAL_MODULE_MANIFEST[module_name]
