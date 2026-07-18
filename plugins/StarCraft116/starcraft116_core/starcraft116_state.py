#20260702_kpopmodder: Tracks detached StarCraft 1.16 launch status for the UI.
import json
import time


class StarCraft116RuntimeState:
    #20260702_kpopmodder: Stores process ids without owning external game shutdown.
    def __init__(self, profile="monster"):
        self.profile = profile
        self.running = False
        self.processes = []
        self.last_message = "StarCraft 1.16 profile not launched."
        self.last_launch_commands = []
        self.launched_at = None

    def mark_launched(self, profile, launch_result, command_displays):
        self.profile = profile
        self.running = True
        self.processes = [
            {
                "label": item.label,
                "pid": item.process.pid,
            }
            for item in launch_result.processes
        ]
        self.last_message = launch_result.message
        self.last_launch_commands = list(command_displays)
        self.launched_at = time.time()

    def mark_launch_failed(self, profile, message, command_displays=None):
        self.profile = profile
        self.running = False
        self.processes = []
        self.last_message = message
        self.last_launch_commands = list(command_displays or [])

    def update_from_processes(self, process_entries):
        active = []
        for entry in process_entries:
            process = entry.get("process")
            if process is None:
                continue
            if process.poll() is None:
                active.append({
                    "label": entry.get("label", ""),
                    "pid": process.pid,
                })
        self.processes = active
        self.running = bool(active)
        if not active and process_entries:
            self.last_message = "StarCraft 1.16 tracked processes have exited."

    def update_from_external_status(self, external_status):
        matches = external_status.get("processes", {}).get("matches", {})
        active = list(self.processes)
        seen_pids = {
            item.get("pid")
            for item in active
            if item.get("pid")
        }
        process_labels = (
            ("StarCraft.exe", "external_starcraft"),
            ("Chaoslauncher.exe", "external_chaoslauncher"),
        )
        for image_name, label in process_labels:
            for row in matches.get(image_name, []):
                pid = row.get("pid")
                if pid and pid in seen_pids:
                    continue
                if pid:
                    seen_pids.add(pid)
                active.append({
                    "label": label,
                    "pid": pid,
                    "image": row.get("image", image_name),
                })

        self.processes = active
        self.running = bool(active)

        summary = external_status.get("summary", {})
        message = summary.get("message")
        if message:
            self.last_message = message

    def to_dict(self):
        return {
            "profile": self.profile,
            "running": self.running,
            "processes": list(self.processes),
            "last_message": self.last_message,
            "last_launch_commands": list(self.last_launch_commands),
            "launched_at": self.launched_at,
        }

    def to_json(self):
        return json.dumps(self.to_dict(), indent=2, ensure_ascii=False)
