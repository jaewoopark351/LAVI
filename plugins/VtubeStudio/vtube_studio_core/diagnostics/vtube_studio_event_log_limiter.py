#20260904_kpopmodder: Apply sampling and a session hard cap to repeated VTube Studio events.
import threading


class VTubeStudioEventLogLimiter:
    def __init__(self, max_emissions_per_event=8):
        self._lock = threading.Lock()
        self._counts = {}
        self._emissions = {}
        self.max_emissions_per_event = max(1, int(max_emissions_per_event))

    def should_log(self, event):
        with self._lock:
            count = self._counts.get(event, 0) + 1
            self._counts[event] = count
            emissions = self._emissions.get(event, 0)
            sampled = count <= 3 or count & (count - 1) == 0
            if not sampled or emissions >= self.max_emissions_per_event:
                return False
            self._emissions[event] = emissions + 1
            return True
