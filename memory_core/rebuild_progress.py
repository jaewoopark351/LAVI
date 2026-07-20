#20260720_kpopmodder: Render derived-memory rebuild progress like a terminal download bar.
import sys
import threading


FINISH_STAGES = {
    "read_raw_events_done",
    "collect_raw_events_done",
    "consolidate_done",
    "build_items_done",
    "clear_done",
    "dry_run_done",
    "rebuild_done",
    "stats_refreshed",
}


def format_rebuild_progress_bar(reason, stage, values=None, width=28):
    values = values or {}
    current = _int_or_none(values.get("current"))
    total = _int_or_none(values.get("total"))
    width = max(10, int(width or 28))

    if total and total > 0:
        current_value = min(max(current or 0, 0), total)
        ratio = current_value / total
        filled = min(width, max(0, int(round(width * ratio))))
        percent = f"{ratio * 100:6.2f}%"
        count_text = f"{current_value}/{total}"
    else:
        filled = 0
        percent = " --.--%"
        count_text = f"{current or 0}/?"

    bar = "#" * filled + "-" * (width - filled)
    parts = [
        "[Memory][RebuildProgress]",
        f"[{bar}]",
        percent,
        count_text,
        f"stage={stage}",
        f"reason={reason}",
    ]
    for key in ("inserted", "skipped", "stale"):
        if key in values:
            parts.append(f"{key}={values[key]}")
    return " ".join(parts)


class TerminalProgressBar:
    def __init__(self, reason, stream=None, width=28):
        self.reason = reason
        self.stream = stream
        self.width = width
        self._lock = threading.Lock()
        self._last_length = 0

    def __call__(self, stage, **values):
        stream = self.stream or sys.stderr
        line = format_rebuild_progress_bar(
            self.reason,
            stage,
            values,
            width=self.width,
        )
        finish_line = stage in FINISH_STAGES

        with self._lock:
            if self._supports_carriage_return(stream):
                padding = " " * max(0, self._last_length - len(line))
                stream.write("\r" + line + padding)
                if finish_line:
                    stream.write("\n")
                    self._last_length = 0
                else:
                    self._last_length = len(line)
            else:
                stream.write(line + "\n")
                self._last_length = 0
            try:
                stream.flush()
            except Exception:
                pass

    def _supports_carriage_return(self, stream):
        try:
            return bool(stream.isatty())
        except Exception:
            return False


def _int_or_none(value):
    try:
        return int(value)
    except Exception:
        return None
