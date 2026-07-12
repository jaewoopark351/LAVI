# #20260701_kpopmodder: Added bounded StarCraft log routing without coupling directly to LLM output.
# import time
# from collections import deque


# class StarCraftLogRouter:
#     #20260701_kpopmodder: Keeps recent StarCraft events inspectable while the plugin is experimental.
#     def __init__(self, max_entries=200):
#         self.max_entries = int(max_entries or 200)
#         self._logs = deque(maxlen=self.max_entries)

#     def log_event(self, message):
#         entry = {
#             "time": time.time(),
#             "kind": "event",
#             "message": str(message or ""),
#         }
#         self._logs.append(entry)
#         return entry

#     def log_command(self, command):
#         payload = command.to_dict() if hasattr(command, "to_dict") else command
#         entry = {
#             "time": time.time(),
#             "kind": "command",
#             "command": payload,
#         }
#         self._logs.append(entry)
#         return entry

#     def log_state(self, game_state):
#         payload = game_state.to_dict() if hasattr(game_state, "to_dict") else game_state
#         entry = {
#             "time": time.time(),
#             "kind": "state",
#             "state": payload,
#         }
#         self._logs.append(entry)
#         return entry

#     def get_recent_logs(self, limit=20):
#         limit = max(0, int(limit or 0))
#         if not limit:
#             return []
#         return list(self._logs)[-limit:]
