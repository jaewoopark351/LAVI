# #20260701_kpopmodder: Added ScreenVision-backed provider stub without mouse or keyboard automation.
# from plugins.StarCraftRemastered.core.game_state import StarCraftGameState
# from plugins.StarCraftRemastered.core.observation_parser import (
#     StarCraftObservationParser,
# )
# from plugins.StarCraftRemastered.providers.base_provider import StarCraftProvider


# class ScreenInputProvider(StarCraftProvider):
#     #20260701_kpopmodder: This provider logs commands only until explicit input automation is approved.
#     def __init__(self, config=None, log_router=None):
#         super().__init__(config=config, log_router=log_router)
#         self.state = StarCraftGameState(is_connected=False)
#         self.observation_parser = StarCraftObservationParser()

#     def connect(self):
#         self.state.is_connected = True
#         self.state.refresh_timestamp()
#         self._log_event("screen_input provider connected")
#         return True

#     def disconnect(self):
#         self.stop_all_control()
#         self.state.is_connected = False
#         self.state.refresh_timestamp()
#         self._log_event("screen_input provider disconnected")
#         return True

#     def is_available(self):
#         return True

#     def get_game_state(self):
#         self.state.refresh_timestamp()
#         return self.state

#     def update_screen_observation(self, text):
#         observation = str(text or "").strip()
#         self.state = self.observation_parser.parse(
#             observation,
#             previous_state=self.state,
#         )
#         return self.state

#     def send_command(self, command):
#         self._log_command(command)
#         if not self.safety_check(self.state):
#             self.stop_all_control()
#             return False

#         if not self._config_bool("auto_control", False):
#             self._log_event(
#                 "auto_control=false; command logged without input automation"
#             )
#             return False

#         #20260701_kpopmodder: Future hook for pyautogui/pynput/win32api after explicit approval.
#         self._log_event("auto_control input automation is not implemented yet")
#         return False

#     def stop_all_control(self):
#         self._log_event("screen_input stop_all_control called")
#         return True
