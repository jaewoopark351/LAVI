#20260720_kpopmodder: Keep derived-memory rebuilds manual or background so app startup is not blocked.
import threading

from core.logger import log_print
from memory_core.derived_memory_builder import DerivedMemoryBuilder
from memory_core.rebuild_progress import TerminalProgressBar


DERIVED_MEMORY_INDEX_EVENT_TYPES = (
    "user_message",
    "assistant_message",
    "screen_observation",
    "screen_observation_silent",
)


class DerivedMemoryRebuildService:
    DEFAULT_PROGRESS_INTERVAL = 1000
    _lock = threading.Lock()
    _running_keys = set()

    @classmethod
    def rebuild_now(
        cls,
        derived_store,
        memory_store,
        clear=True,
        reason="rebuild",
        progress=False,
        progress_interval=None,
    ):
        progress_interval = cls._normalize_progress_interval(progress_interval)
        progress_callback = cls._progress_logger(reason) if progress else None
        raw_events = cls._collect_rebuild_events(
            memory_store,
            progress_callback=progress_callback,
            progress_interval=progress_interval,
        )
        rebuild_stats = DerivedMemoryBuilder().rebuild(
            raw_events,
            derived_store=derived_store,
            clear=clear,
            progress_callback=progress_callback,
            progress_interval=progress_interval,
        )
        refreshed_stats = cls.get_refreshed_stats(derived_store, memory_store)
        cls._emit_progress(
            progress_callback,
            "stats_refreshed",
            current=int(refreshed_stats.get("row_count", 0) or 0),
            total=int(refreshed_stats.get("row_count", 0) or 0),
            stale=bool(refreshed_stats.get("stale", False)),
        )
        return rebuild_stats, refreshed_stats

    @classmethod
    def rebuild_now_exclusive(
        cls,
        derived_store,
        memory_store,
        reason="manual",
        clear=True,
        progress=True,
        progress_interval=None,
    ):
        key = cls._running_key(derived_store)
        with cls._lock:
            if key in cls._running_keys:
                log_print(
                    "[Memory] derived_memory.sqlite3 rebuild already running "
                    f"(reason={reason})"
                )
                return None
            cls._running_keys.add(key)

        try:
            log_print(
                "[Memory] derived_memory.sqlite3 manual rebuild started "
                f"(reason={reason})"
            )
            rebuild_stats, refreshed_stats = cls.rebuild_now(
                derived_store,
                memory_store,
                clear=clear,
                reason=reason,
                progress=progress,
                progress_interval=progress_interval,
            )
            log_print(
                "[Memory] derived_memory.sqlite3 manual rebuild finished "
                f"(reason={reason}, "
                f"raw_events={rebuild_stats.get('raw_event_count', 0)}, "
                f"rows={refreshed_stats.get('row_count', 0)}, "
                f"stale={refreshed_stats.get('stale', False)})"
            )
            return rebuild_stats, refreshed_stats
        finally:
            with cls._lock:
                cls._running_keys.discard(key)

    @classmethod
    def get_refreshed_stats(cls, derived_store, memory_store):
        return derived_store.get_stats(
            raw_latest_created_ts=cls._latest_raw_event_ts(memory_store),
        )

    @classmethod
    def schedule_background_rebuild(
        cls,
        derived_store,
        memory_store,
        reason="stale",
        clear=True,
        progress=True,
        progress_interval=None,
    ):
        key = cls._running_key(derived_store)
        with cls._lock:
            if key in cls._running_keys:
                log_print(
                    "[Memory] derived_memory.sqlite3 background rebuild already running "
                    f"(reason={reason})"
                )
                return None
            cls._running_keys.add(key)

        thread = threading.Thread(
            target=cls._background_rebuild,
            args=(
                key,
                derived_store,
                memory_store,
                reason,
                clear,
                progress,
                progress_interval,
            ),
            name="DerivedMemoryRebuild",
            daemon=True,
        )
        thread.start()
        log_print(
            "[Memory] derived_memory.sqlite3 background rebuild scheduled "
            f"(reason={reason})"
        )
        return thread

    @classmethod
    def _background_rebuild(
        cls,
        key,
        derived_store,
        memory_store,
        reason,
        clear,
        progress,
        progress_interval,
    ):
        try:
            rebuild_stats, refreshed_stats = cls.rebuild_now(
                derived_store,
                memory_store,
                clear=clear,
                reason=reason,
                progress=progress,
                progress_interval=progress_interval,
            )
            log_print(
                "[Memory] derived_memory.sqlite3 background rebuild finished "
                f"(reason={reason}, "
                f"raw_events={rebuild_stats.get('raw_event_count', 0)}, "
                f"rows={refreshed_stats.get('row_count', 0)}, "
                f"stale={refreshed_stats.get('stale', False)})"
            )
        except Exception as e:
            log_print(
                "[Memory][Warning] derived_memory.sqlite3 background rebuild failed "
                f"(reason={reason}): {e}"
            )
        finally:
            with cls._lock:
                cls._running_keys.discard(key)

    @classmethod
    def _collect_rebuild_events(
        cls,
        memory_store,
        progress_callback=None,
        progress_interval=None,
    ):
        progress_interval = cls._normalize_progress_interval(progress_interval)
        cls._emit_progress(
            progress_callback,
            "collect_raw_events_start",
            current=0,
        )
        raw_events = []
        for index, event in enumerate(cls._iter_rebuild_events(memory_store), start=1):
            raw_events.append(event)
            if index % progress_interval == 0:
                cls._emit_progress(
                    progress_callback,
                    "collect_raw_events",
                    current=index,
                )
        cls._emit_progress(
            progress_callback,
            "collect_raw_events_done",
            current=len(raw_events),
            total=len(raw_events),
        )
        return raw_events

    @classmethod
    def _iter_rebuild_events(cls, memory_store):
        if hasattr(memory_store, "iter_raw_events"):
            yield from memory_store.iter_raw_events(limit=None)
            return

        for event in memory_store.get_raw_events(limit=None):
            yield event

    @classmethod
    def _latest_raw_event_ts(cls, memory_store):
        latest_ts = None
        try:
            if hasattr(memory_store, "iter_raw_events"):
                for event in memory_store.iter_raw_events(
                    limit=None,
                    event_types=DERIVED_MEMORY_INDEX_EVENT_TYPES,
                ):
                    try:
                        created_ts = float(event.get("created_ts", 0) or 0)
                    except Exception:
                        continue
                    if latest_ts is None or created_ts > latest_ts:
                        latest_ts = created_ts
                return latest_ts
        except Exception:
            pass

        try:
            events = memory_store.get_raw_events(limit=2000)
        except Exception:
            return None

        for event in reversed(events or []):
            if str(event.get("event_type", "")) not in DERIVED_MEMORY_INDEX_EVENT_TYPES:
                continue
            try:
                return float(event.get("created_ts", 0) or 0)
            except Exception:
                return None

        return None

    @classmethod
    def _running_key(cls, derived_store):
        return str(getattr(derived_store, "db_path", id(derived_store)))

    @classmethod
    def _normalize_progress_interval(cls, progress_interval):
        try:
            return max(1, int(progress_interval))
        except Exception:
            return cls.DEFAULT_PROGRESS_INTERVAL

    @classmethod
    def _progress_logger(cls, reason):
        return TerminalProgressBar(reason)

    @classmethod
    def _emit_progress(cls, progress_callback, stage, **values):
        if not callable(progress_callback):
            return
        try:
            progress_callback(stage, **values)
        except Exception:
            pass
