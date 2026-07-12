#20260707_kpopmodder: Added StarCraft2 optional plugin package for Windows-first SC2 engine adapters.

__all__ = ["StarCraft2Extension"]


def __getattr__(name):
    #20260710_kpopmodder: Keep package import lazy so the plugin loader can
    # load sibling modules without triggering a circular import.
    if name == "StarCraft2Extension":
        from .starcraft2_core.sc2_extension import StarCraft2Extension
        return StarCraft2Extension
    raise AttributeError(name)
