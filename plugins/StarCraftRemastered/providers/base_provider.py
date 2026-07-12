# #20260701_kpopmodder: Added safe provider base class for Remastered single-player control stubs.
# from abc import ABC, abstractmethod

# from core.logger import log_print
# from plugins.StarCraftRemastered.core.game_state import StarCraftGameState


# class StarCraftProvider(ABC):
#     #20260701_kpopmodder: Providers must pass this guard before any future input automation.
#     def __init__(self, config=None, log_router=None):
#         self.config = config or {}
#         self.log_router = log_router
#         self._last_safety_reason = ""

#     @abstractmethod
#     def connect(self):
#         raise NotImplementedError

#     @abstractmethod
#     def disconnect(self):
#         raise NotImplementedError

#     @abstractmethod
#     def is_available(self):
#         raise NotImplementedError

#     @abstractmethod
#     def get_game_state(self):
#         raise NotImplementedError

#     @abstractmethod
#     def send_command(self, command):
#         raise NotImplementedError

#     @abstractmethod
#     def stop_all_control(self):
#         raise NotImplementedError

#     def safety_check(self, game_state=None):
#         state = game_state or self.get_game_state() or StarCraftGameState()
#         mode = str(self._config_value("mode", "single_player_only")).strip()
#         allow_battlenet = self._config_bool("allow_battlenet", False)
#         allow_multiplayer = self._config_bool("allow_multiplayer", False)

#         if mode != "single_player_only":
#             return self._safety_blocked("mode is not single_player_only", state)

#         if state.is_battlenet_screen and not allow_battlenet:
#             return self._safety_blocked("Battle.net screen blocked", state)

#         if state.is_multiplayer_screen and not allow_multiplayer:
#             return self._safety_blocked("multiplayer screen blocked", state)

#         if state.is_in_game and not state.is_single_player and not allow_multiplayer:
#             return self._safety_blocked("non-single-player game blocked", state)

#         self._last_safety_reason = ""
#         state.safety_reason = ""
#         return True

#     def _safety_blocked(self, reason, state):
#         self._last_safety_reason = reason
#         state.safety_reason = reason
#         self._log_event(f"safety blocked: {reason}")
#         return False

#     def _config_value(self, key, default=None):
#         try:
#             return self.config.get(key, default)
#         except Exception:
#             return default

#     def _config_bool(self, key, default=False):
#         if hasattr(self.config, "get_bool"):
#             try:
#                 return self.config.get_bool(key, default)
#             except Exception:
#                 return bool(default)

#         value = self._config_value(key, default)
#         if isinstance(value, bool):
#             return value
#         return str(value).strip().lower() in {"1", "true", "yes", "on"}

#     def _log_event(self, message):
#         log_print(f"[StarCraftRemastered] {message}")
#         if self.log_router is not None:
#             self.log_router.log_event(message)

#     def _log_command(self, command):
#         if self.log_router is not None:
#             self.log_router.log_command(command)
