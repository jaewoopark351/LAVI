#20260621_kpopmodder: Minimal memory store for LAV.
# Working/session memory is runtime-only. Long-term memory is saved to JSON.
#20260621_kpopmodder: Raw events are saved to JSONL for future summarization, not injected into prompts directly.
import json
import os
import threading
import time
from collections import deque
from datetime import datetime

from memory_core.raw_event_sqlite_store import RawEventSQLiteStore


class MemoryStore:
    def __init__(
        self,
        memory_dir="memory",
        max_working_items=8,
        long_term_file_name="long_term_memory.json",
        raw_events_file_name="raw_events.jsonl",
        raw_events_db_file_name="raw_events.sqlite3",#20260622_kpopmodder: SQLite read index file name.
        #max_raw_event_value_chars=4000,#20260621_kpopmodder
        max_raw_event_value_chars=None,#20260621_kpopmodder
    ):
        self.memory_dir = memory_dir
        self.max_working_items = max_working_items
        self.long_term_path = os.path.join(
            self.memory_dir,
            long_term_file_name,
        )
        self.raw_events_path = os.path.join(
            self.memory_dir,
            raw_events_file_name,
        )
        self.raw_events_db_path = os.path.join(
            self.memory_dir,
            raw_events_db_file_name,
        )
        self.raw_event_sqlite_store = RawEventSQLiteStore(#20260622_kpopmodder: Keep JSONL as recovery log and use SQLite as helper storage.
            self.raw_events_db_path,
        )
        self._raw_event_sqlite_jsonl_imported = False#20260626_kpopmodder: Avoid full JSONL import on every recall lookup.
        #self.max_raw_event_value_chars = int(max_raw_event_value_chars)#20260621_kpopmodder
        self.max_raw_event_value_chars = (#20260621_kpopmodder
            None
            if max_raw_event_value_chars is None
            else int(max_raw_event_value_chars)
        )

        self.lock = threading.RLock()

        self.working_memory = deque(maxlen=max_working_items)
        self.session_memory = {}
        self.long_term_memory = {}

        os.makedirs(self.memory_dir, exist_ok=True)

        self.long_term_memory = self._load_json(self.long_term_path, {})

    def initialize_raw_event_sqlite(self):#20260622_kpopmodder: Startup hook for confirming the SQLite raw-event store is usable.
        with self.lock:
            self.raw_event_sqlite_store.initialize()
            self._import_raw_events_jsonl_once()
        return self.raw_events_db_path

    def _import_raw_events_jsonl_once(self):#20260626_kpopmodder: Sync recovery JSONL once; add_raw_event mirrors new rows directly.
        if self._raw_event_sqlite_jsonl_imported:
            return

        self.raw_event_sqlite_store.import_jsonl(
            self.raw_events_path,
        )
        self._raw_event_sqlite_jsonl_imported = True

    def _now_text(self):
        return datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    def _now_ts(self):
        return time.time()

    def _load_json(self, path, default):
        if not os.path.exists(path):
            return default

        try:
            with open(path, "r", encoding="utf-8") as f:
                data = json.load(f)

            if isinstance(data, dict):
                return data

            return default
        except Exception:
            return default

    def _save_json(self, path, data):
        os.makedirs(os.path.dirname(path), exist_ok=True)

        tmp_path = path + ".tmp"

        with open(tmp_path, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)

        os.replace(tmp_path, path)

    def _make_item(
        self,
        key,
        value,
        memory_type,
        source,
        confidence,
        ttl_seconds=None,
    ):
        now_ts = self._now_ts()
        expires_at = None

        if ttl_seconds is not None:
            expires_at = now_ts + float(ttl_seconds)

        return {
            "key": str(key),
            "value": str(value),
            "memory_type": str(memory_type),
            "source": str(source),
            "confidence": float(confidence),
            "created_at": self._now_text(),
            "updated_at": self._now_text(),
            "expires_at": expires_at,
        }

    def _is_expired(self, item):
        expires_at = item.get("expires_at")

        if expires_at is None:
            return False

        try:
            return self._now_ts() >= float(expires_at)
        except Exception:
            return False

    def cleanup_expired(self):
        with self.lock:
            self.working_memory = deque(
                [
                    item
                    for item in self.working_memory
                    if not self._is_expired(item)
                ],
                maxlen=self.max_working_items,
            )

            expired_session_keys = [
                key
                for key, item in self.session_memory.items()
                if self._is_expired(item)
            ]

            for key in expired_session_keys:
                self.session_memory.pop(key, None)

    def add_raw_event(
        self,
        event_type,
        value,
        source="unknown",
        metadata=None,
    ):
        """모든 대화/화면 관찰 원본을 JSONL로 저장한다.

        long_term_memory.json에 넣는 장기기억이 아니다.
        나중에 요약/자동 승격할 때 사용할 원본 이벤트 로그다.
        """

        value = str(value or "").strip()

        if not value:
            return None

        # if len(value) > self.max_raw_event_value_chars:#20260621_kpopmodder
        #     value = value[: self.max_raw_event_value_chars] + "...[truncated]"#20260621_kpopmodder

        # if (#20260621_kpopmodder
        #     self.max_raw_event_value_chars is not None
        #     and len(value) > int(self.max_raw_event_value_chars)
        # ):#20260621_kpopmodder
        #     value = value[: int(self.max_raw_event_value_chars)] + "...[truncated]"

        if (#20260621_kpopmodder
            self.max_raw_event_value_chars is not None
            and len(value) > self.max_raw_event_value_chars
        ):#20260621_kpopmodder
            value = value[: self.max_raw_event_value_chars] + "...[truncated]"

        if metadata is None:
            metadata = {}

        if not isinstance(metadata, dict):
            metadata = {
                "metadata_value": str(metadata),
            }

        event = {
            "event_type": str(event_type),
            "value": value,
            "source": str(source),
            "metadata": metadata,
            "created_at": self._now_text(),
            "created_ts": self._now_ts(),
        }

        with self.lock:
            os.makedirs(self.memory_dir, exist_ok=True)

            line = json.dumps(event, ensure_ascii=False, default=str)#20260622_kpopmodder: Reuse the JSONL line for stable SQLite duplicate hashes.
            with open(self.raw_events_path, "a", encoding="utf-8") as f:
                f.write(line)
                f.write("\n")

            try:#20260622_kpopmodder: SQLite failure must not block the app or JSONL logging.
                self.raw_event_sqlite_store.add_event(
                    event,
                    line_text=line,
                )
            except Exception:
                pass

        return event

    def get_raw_events(self, limit=2000):#20260622_kpopmodder: 과거 회상을 위해 최근 원본 이벤트를 읽는다.
        """최근 원본 이벤트를 오래된 순서로 반환한다."""
        limit = self._normalize_raw_event_limit(limit)

        with self.lock:
            try:
                self.raw_event_sqlite_store.initialize()
                self._import_raw_events_jsonl_once()
                if limit is None:
                    return self.raw_event_sqlite_store.get_all_events()
                return self.raw_event_sqlite_store.get_recent_events(limit)
            except Exception:
                pass

        return self._get_raw_events_from_jsonl(limit)

    def _get_raw_events_from_jsonl(self, limit):#20260622_kpopmodder: Fallback to the old JSONL read path if SQLite fails.
        if not os.path.exists(self.raw_events_path):
            return []

        #20260626_kpopmodder: limit=None means full raw JSONL scan for long-memory recall.
        lines = [] if limit is None else deque(maxlen=limit)

        with self.lock:
            try:
                with open(self.raw_events_path, "r", encoding="utf-8") as f:
                    for line in f:
                        line = line.strip()
                        if line:
                            lines.append(line)
            except Exception:
                return []

        events = []
        for line in lines:
            try:
                event = json.loads(line)
            except Exception:
                continue
            if isinstance(event, dict):
                events.append(event)

        return events

    def iter_raw_events(
        self,
        limit=2000,
        event_types=None,
        batch_size=500,
        max_events=None,
        time_budget_sec=None,
    ):#20260703_kpopmodder: Recall can scan raw events with pagination and a runtime budget.
        limit = self._normalize_raw_event_limit(limit)
        max_events = self._normalize_raw_event_limit(max_events)
        effective_limit = limit
        if effective_limit is None:
            effective_limit = max_events
        elif max_events is not None:
            effective_limit = min(effective_limit, max_events)

        deadline = None
        if time_budget_sec is not None:
            deadline = time.monotonic() + max(0.0, float(time_budget_sec))

        with self.lock:
            try:
                self.raw_event_sqlite_store.initialize()
                self._import_raw_events_jsonl_once()
            except Exception:
                pass

        try:
            if hasattr(self.raw_event_sqlite_store, "iter_events"):
                for event in self.raw_event_sqlite_store.iter_events(
                    limit=effective_limit,
                    event_types=event_types,
                    batch_size=batch_size,
                ):
                    if deadline is not None and time.monotonic() >= deadline:
                        break
                    yield event
                return

            if effective_limit is None:
                events = self.raw_event_sqlite_store.get_all_events()
            else:
                events = self.raw_event_sqlite_store.get_recent_events(
                    effective_limit
                )
            for event in events:
                if deadline is not None and time.monotonic() >= deadline:
                    break
                if self._raw_event_matches_types(event, event_types):
                    yield event
            return
        except Exception:
            pass

        for event in self._iter_raw_events_from_jsonl(
            effective_limit,
            event_types=event_types,
            time_budget_sec=time_budget_sec,
        ):
            yield event

    def _iter_raw_events_from_jsonl(
        self,
        limit,
        event_types=None,
        time_budget_sec=None,
    ):#20260703_kpopmodder: JSONL fallback also respects recall limits and filters.
        if not os.path.exists(self.raw_events_path):
            return

        lines = [] if limit is None else deque(maxlen=limit)
        deadline = None
        if time_budget_sec is not None:
            deadline = time.monotonic() + max(0.0, float(time_budget_sec))

        with self.lock:
            try:
                with open(self.raw_events_path, "r", encoding="utf-8") as f:
                    for line in f:
                        if deadline is not None and time.monotonic() >= deadline:
                            break
                        line = line.strip()
                        if line:
                            lines.append(line)
            except Exception:
                return

        for line in lines:
            if deadline is not None and time.monotonic() >= deadline:
                break
            try:
                event = json.loads(line)
            except Exception:
                continue
            if (
                isinstance(event, dict)
                and self._raw_event_matches_types(event, event_types)
            ):
                yield event

    def _raw_event_matches_types(self, event, event_types):
        if not event_types:
            return True
        return str(event.get("event_type", "")) in {
            str(event_type) for event_type in event_types
        }

    def _normalize_raw_event_limit(self, limit):
        if limit is None:
            return None

        try:
            return max(1, int(limit))
        except Exception:
            return 2000

    def add_working_memory(
        self,
        key,
        value,
        source="system",
        confidence=1.0,
        ttl_seconds=300,
    ):
        value = str(value or "").strip()

        if not value:
            return

        item = self._make_item(
            key=key,
            value=value,
            memory_type="working",
            source=source,
            confidence=confidence,
            ttl_seconds=ttl_seconds,
        )

        with self.lock:
            self.cleanup_expired()
            self.working_memory.append(item)

    def add_screen_observation(
        self,
        observation,
        source="ScreenVision",
        ttl_seconds=600,#20260622_kpopmodder
        confidence=0.95,#20260720_kpopmodder: ScreenVision UI-noise observations can opt into lower confidence.
    ):
        observation = str(observation or "").strip()

        if not observation:
            return

        self.add_working_memory(
            key="screen_observation",
            value=observation,
            source=source,
            confidence=confidence,
            ttl_seconds=ttl_seconds,
        )

    def set_session_memory(
        self,
        key,
        value,
        source="system",
        confidence=0.9,
        ttl_seconds=None,
    ):
        value = str(value or "").strip()

        if not value:
            return

        item = self._make_item(
            key=key,
            value=value,
            memory_type="session",
            source=source,
            confidence=confidence,
            ttl_seconds=ttl_seconds,
        )

        with self.lock:
            self.cleanup_expired()
            self.session_memory[str(key)] = item

    def set_long_term_memory(
        self,
        key,
        value,
        source="manual",
        confidence=0.85,
    ):
        value = str(value or "").strip()

        if not value:
            return

        item = self._make_item(
            key=key,
            value=value,
            memory_type="long_term",
            source=source,
            confidence=confidence,
            ttl_seconds=None,
        )

        with self.lock:
            self.long_term_memory[str(key)] = item
            self._save_json(self.long_term_path, self.long_term_memory)

    def delete_long_term_memory(self, key):
        with self.lock:
            removed = self.long_term_memory.pop(str(key), None)

            if removed is not None:
                self._save_json(self.long_term_path, self.long_term_memory)

            return removed

    def delete_long_term_memory_by_query(self, query):
        query = str(query or "").strip()

        if not query:
            return []

        removed = []

        with self.lock:
            for key, item in list(self.long_term_memory.items()):
                value = str(item.get("value", ""))

                if query == key or query in value:
                    removed.append(item)
                    self.long_term_memory.pop(key, None)

            if removed:
                self._save_json(self.long_term_path, self.long_term_memory)

        return removed

    def get_working_memory(self):
        with self.lock:
            self.cleanup_expired()
            return list(self.working_memory)

    def get_session_memory(self):
        with self.lock:
            self.cleanup_expired()
            return dict(self.session_memory)

    def get_long_term_memory(self):
        with self.lock:
            return dict(self.long_term_memory)
