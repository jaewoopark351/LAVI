#20260717_kpopmodder: Isolates global state keys from the mutable store.
from enum import Enum, auto


class GlobalKeys(Enum):
    IS_IDLE = auto()
    IS_AI_SPEAKING = auto()#20260611_kpopmodder
    IS_SONG_PLAYING = auto()#20260628_kpopmodder
    LAST_AI_SPEAK_END_TIME = auto()#20260611_kpopmodder
