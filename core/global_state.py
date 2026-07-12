#20260622_kpopmodder: Canonical global state module for shared runtime flags.
from enum import Enum, auto


class GlobalKeys(Enum):
    IS_IDLE = auto()
    IS_AI_SPEAKING = auto()#20260611_kpopmodder
    IS_SONG_PLAYING = auto()#20260628_kpopmodder
    LAST_AI_SPEAK_END_TIME = auto()#20260611_kpopmodder


class GlobalState:
    def __init__(self):
        self._state = {}

    def set_value(self, key: GlobalKeys, value):
        """Set a value for a given key."""
        if not isinstance(key, GlobalKeys):
            raise ValueError("key must be an instance of GlobalKeys enum")
        self._state[key] = value

    def get_value(self, key: GlobalKeys, default=None):
        """Get a value by key, return default if key does not exist."""
        if not isinstance(key, GlobalKeys):
            raise ValueError("key must be an instance of GlobalKeys enum")
        return self._state.get(key, default)

    def has_key(self, key: GlobalKeys):
        """Check if the given key exists in the state."""
        if not isinstance(key, GlobalKeys):
            raise ValueError("key must be an instance of GlobalKeys enum")
        return key in self._state


# Global instance of the GlobalState
global_state = GlobalState()
global_state.set_value(GlobalKeys.IS_IDLE, True)#20260611_kpopmodder
global_state.set_value(GlobalKeys.IS_AI_SPEAKING, False)#20260611_kpopmodder
global_state.set_value(GlobalKeys.IS_SONG_PLAYING, False)#20260628_kpopmodder
global_state.set_value(GlobalKeys.LAST_AI_SPEAK_END_TIME, 0)#20260611_kpopmodder
