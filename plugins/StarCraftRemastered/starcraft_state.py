# #20260630_kpopmodder: Added lightweight runtime state for StarCraft launch tracking.
# import json
# from dataclasses import asdict, dataclass


# @dataclass
# class StarCraftRuntimeState:
#     running: bool = False
#     pid: int = None
#     profile: str = "bwmetaai"
#     last_launch_command: str = ""
#     last_observation: str = ""
#     last_coach_request: str = ""
#     last_coach_message: str = ""
#     last_error: str = ""
#     last_exit_code: int = None

#     def mark_launched(self, pid, profile, command):
#         self.running = True
#         self.pid = pid
#         self.profile = str(profile or "bwmetaai")
#         self.last_launch_command = str(command or "")
#         self.last_error = ""
#         self.last_exit_code = None

#     def mark_launch_failed(self, message):
#         self.running = False
#         self.pid = None
#         self.last_error = str(message or "")

#     def update_from_process(self, process):
#         if process is None:
#             self.running = False
#             self.pid = None
#             return

#         exit_code = process.poll()
#         self.running = exit_code is None
#         self.pid = getattr(process, "pid", None)
#         self.last_exit_code = exit_code

#     def set_observation(self, observation):
#         self.last_observation = str(observation or "").strip()

#     def set_coach_request(self, request):
#         self.last_coach_request = str(request or "").strip()

#     def set_coach_message(self, message):
#         self.last_coach_message = str(message or "").strip()

#     def to_dict(self):
#         return asdict(self)

#     def to_json(self):
#         return json.dumps(self.to_dict(), ensure_ascii=False, indent=2)

