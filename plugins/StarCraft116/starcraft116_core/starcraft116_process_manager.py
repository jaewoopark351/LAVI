#20260706_kpopmodder: Isolates terminate flow from the StarCraft116 facade.


class StarCraft116ProcessManager:
    def terminate_all(self, process_entries):
        for entry in list(process_entries):
            process = entry.get("process")
            if process is None or process.poll() is not None:
                continue
            try:
                process.terminate()
            except Exception as e:
                from core.logger import log_print

                log_print(f"[StarCraft116] process terminate failed: {e}")
