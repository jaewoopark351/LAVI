#20260717_kpopmodder: Isolates mutable global state storage from key definitions.
from .global_keys import GlobalKeys


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
