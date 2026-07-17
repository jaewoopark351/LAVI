#20260717_kpopmodder: Compatibility facade for global state key/store classes and singleton.
from core.global_state_core.global_keys import GlobalKeys
from core.global_state_core.global_state import GlobalState


# Global instance of the GlobalState
global_state = GlobalState()
global_state.set_value(GlobalKeys.IS_IDLE, True)#20260611_kpopmodder
global_state.set_value(GlobalKeys.IS_AI_SPEAKING, False)#20260611_kpopmodder
global_state.set_value(GlobalKeys.IS_SONG_PLAYING, False)#20260628_kpopmodder
global_state.set_value(GlobalKeys.LAST_AI_SPEAK_END_TIME, 0)#20260611_kpopmodder

__all__ = [
    "GlobalKeys",
    "GlobalState",
    "global_state",
]
