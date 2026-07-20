#20260626_kpopmodder: Optional derived-memory SQLite reference index; raw_events remain source of truth.
import json
import os
import re
import sqlite3
import time
from contextlib import contextmanager
from difflib import SequenceMatcher

from memory_core.sqlite_write_gate import connect_sqlite, sqlite_writer_lock


class DerivedMemorySQLiteStore:
    def __init__(self, db_path, minimum_search_score=1.15, enable_fts=True):
        self.db_path = db_path
        self.minimum_search_score = float(minimum_search_score)
        self.enable_fts = bool(enable_fts)
        self._fts_available = None

    def initialize(self):#20260626_kpopmodder: Keep derived reference index optional and cheap to create at startup.
        db_dir = os.path.dirname(self.db_path)
        if db_dir:
            os.makedirs(db_dir, exist_ok=True)

        with sqlite_writer_lock(self.db_path):#20260629_kpopmodder: Serialize optional derived-index schema writes across app/scripts.
            with self._connect() as connection:
                connection.execute("""
                    CREATE TABLE IF NOT EXISTS derived_memories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        kind TEXT NOT NULL,
                        title TEXT NOT NULL,
                        summary TEXT NOT NULL,
                        search_text TEXT NOT NULL,
                        normalized_key TEXT NOT NULL,
                        topic_key TEXT NOT NULL DEFAULT '',
                        source_event_count INTEGER NOT NULL DEFAULT 1,
                        duplicate_count INTEGER NOT NULL DEFAULT 0,
                        first_created_at TEXT NOT NULL DEFAULT '',
                        last_created_at TEXT NOT NULL DEFAULT '',
                        first_created_ts REAL NOT NULL DEFAULT 0,
                        last_created_ts REAL NOT NULL DEFAULT 0,
                        confidence REAL NOT NULL DEFAULT 0.7,
                        metadata_json TEXT NOT NULL DEFAULT '{}',
                        created_ts REAL NOT NULL,
                        updated_ts REAL NOT NULL,
                        UNIQUE(kind, normalized_key)
                    )
                """)
                connection.execute("""
                    CREATE INDEX IF NOT EXISTS idx_derived_kind
                    ON derived_memories(kind)
                """)
                connection.execute("""
                    CREATE INDEX IF NOT EXISTS idx_derived_last_created_ts
                    ON derived_memories(last_created_ts)
                """)
                connection.execute("""
                    CREATE INDEX IF NOT EXISTS idx_derived_topic_key
                    ON derived_memories(topic_key)
                """)
                if self.enable_fts:
                    self._initialize_fts(connection)

    def upsert_memory(self, memory_item):
        self.initialize()
        values = self._memory_values(memory_item)

        with sqlite_writer_lock(self.db_path):#20260629_kpopmodder: Rebuild/upsert writers share one memory SQLite gate.
            with self._connect() as connection:
                existing = connection.execute(
                    """
                    SELECT id, source_event_count, duplicate_count
                    FROM derived_memories
                    WHERE kind = ? AND normalized_key = ?
                    """,
                    (values["kind"], values["normalized_key"]),
                ).fetchone()

                if existing is None:
                    connection.execute(
                        """
                        INSERT INTO derived_memories (
                            kind,
                            title,
                            summary,
                            search_text,
                            normalized_key,
                            topic_key,
                            source_event_count,
                            duplicate_count,
                            first_created_at,
                            last_created_at,
                            first_created_ts,
                            last_created_ts,
                            confidence,
                            metadata_json,
                            created_ts,
                            updated_ts
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                        (
                            values["kind"],
                            values["title"],
                            values["summary"],
                            values["search_text"],
                            values["normalized_key"],
                            values["topic_key"],
                            values["source_event_count"],
                            values["duplicate_count"],
                            values["first_created_at"],
                            values["last_created_at"],
                            values["first_created_ts"],
                            values["last_created_ts"],
                            values["confidence"],
                            values["metadata_json"],
                            values["created_ts"],
                            values["updated_ts"],
                        ),
                    )
                    return {
                        "action": "inserted",
                        "source_event_count": values["source_event_count"],
                        "duplicate_count": values["duplicate_count"],
                    }

                #20260629_kpopmodder: On update, every incoming source event is
                # a duplicate of the existing representative row. Therefore
                # duplicate_count grows by source_event_count, not by the
                # incoming row's own duplicate_count.
                connection.execute(
                    """
                    UPDATE derived_memories
                    SET
                        title = CASE
                            WHEN ? >= last_created_ts THEN ?
                            ELSE title
                        END,
                        summary = CASE
                            WHEN ? >= last_created_ts THEN ?
                            ELSE summary
                        END,
                        search_text = CASE
                            WHEN ? >= last_created_ts THEN ?
                            ELSE search_text
                        END,
                        topic_key = CASE
                            WHEN topic_key = '' THEN ?
                            ELSE topic_key
                        END,
                        source_event_count = source_event_count + ?,
                        duplicate_count = duplicate_count + ?,
                        first_created_at = CASE
                            WHEN first_created_ts = 0 OR ? < first_created_ts
                            THEN ?
                            ELSE first_created_at
                        END,
                        first_created_ts = CASE
                            WHEN first_created_ts = 0 OR ? < first_created_ts
                            THEN ?
                            ELSE first_created_ts
                        END,
                        last_created_at = CASE
                            WHEN ? >= last_created_ts THEN ?
                            ELSE last_created_at
                        END,
                        last_created_ts = CASE
                            WHEN ? >= last_created_ts THEN ?
                            ELSE last_created_ts
                        END,
                        confidence = MAX(confidence, ?),
                        metadata_json = ?,
                        updated_ts = ?
                    WHERE kind = ? AND normalized_key = ?
                    """,
                    (
                        values["last_created_ts"],
                        values["title"],
                        values["last_created_ts"],
                        values["summary"],
                        values["last_created_ts"],
                        values["search_text"],
                        values["topic_key"],
                        values["source_event_count"],
                        values["source_event_count"],
                        values["first_created_ts"],
                        values["first_created_at"],
                        values["first_created_ts"],
                        values["first_created_ts"],
                        values["last_created_ts"],
                        values["last_created_at"],
                        values["last_created_ts"],
                        values["last_created_ts"],
                        values["confidence"],
                        values["metadata_json"],
                        values["updated_ts"],
                        values["kind"],
                        values["normalized_key"],
                    ),
                )
                return {
                    "action": "updated",
                    "source_event_count": (
                        int(existing["source_event_count"])
                        + values["source_event_count"]
                    ),
                    "duplicate_count": (
                        int(existing["duplicate_count"])
                        + values["source_event_count"]
                    ),
                }

    def search(self, query, limit=4):
        query = self._clean_text(query)
        if not query:
            return self.get_recent(limit=limit)

        self.initialize()
        limit = self._safe_limit(limit)
        candidate_limit = max(200, limit * 100)

        with self._connect() as connection:
            rows = self._search_fts_rows(
                connection,
                query=query,
                candidate_limit=candidate_limit,
            )
            rows = self._merge_rows(
                rows,
                self._search_prefilter_rows(
                    connection,
                    query=query,
                    candidate_limit=candidate_limit,
                ),
            )

        query_key = self._normalized_key(query)
        query_tokens = self._tokens(query)
        scored = []

        for row in rows:
            item = self._row_to_memory(row)
            score = self._score_item(
                query_key=query_key,
                query_tokens=query_tokens,
                item=item,
            )
            if score < self.minimum_search_score:
                continue
            item["score"] = round(score, 4)
            scored.append(item)

        scored.sort(
            key=lambda item: (
                item.get("score", 0.0),
                item.get("last_created_ts", 0.0),
            ),
            reverse=True,
        )
        return scored[:limit]

    def get_recent(self, limit=4):
        self.initialize()
        limit = self._safe_limit(limit)

        with self._connect() as connection:
            rows = connection.execute(
                """
                SELECT *
                FROM derived_memories
                ORDER BY last_created_ts DESC, id DESC
                LIMIT ?
                """,
                (limit,),
            ).fetchall()

        return [self._row_to_memory(row) for row in rows]

    def clear(self):
        self.initialize()
        with sqlite_writer_lock(self.db_path):#20260629_kpopmodder: Rebuild clear must wait for other memory SQLite writers.
            with self._connect() as connection:
                connection.execute("DELETE FROM derived_memories")
                if self._fts_available:
                    try:
                        connection.execute("DELETE FROM derived_memories_fts")
                    except sqlite3.DatabaseError:
                        self._fts_available = False

    def get_stats(self, raw_latest_created_ts=None):#20260627_kpopmodder: Startup diagnostics for optional derived fallback index.
        self.initialize()

        with self._connect() as connection:
            row = connection.execute(
                """
                SELECT
                    COUNT(*) AS row_count,
                    COALESCE(MAX(last_created_ts), 0) AS latest_source_ts,
                    COALESCE(MAX(updated_ts), 0) AS latest_updated_ts
                FROM derived_memories
                """
            ).fetchone()

        row_count = int(row["row_count"] or 0)
        latest_source_ts = self._float_or_zero(row["latest_source_ts"])
        latest_updated_ts = self._float_or_zero(row["latest_updated_ts"])
        raw_latest_ts = self._float_or_none(raw_latest_created_ts)
        stale = False
        if raw_latest_ts is not None:
            if row_count == 0:
                stale = raw_latest_ts > 0
            elif latest_source_ts > 0:
                #20260720_kpopmodder: A full rebuild can skip trailing noisy raw
                # events; if the index was updated after the latest raw event,
                # it should not stay stale just because no row used that event.
                stale = (
                    raw_latest_ts > latest_source_ts
                    and raw_latest_ts > latest_updated_ts
                )

        return {
            "row_count": row_count,
            "latest_source_ts": latest_source_ts,
            "latest_updated_ts": latest_updated_ts,
            "raw_latest_ts": raw_latest_ts,
            "stale": stale,
        }

    @contextmanager
    def _connect(self):#20260626_kpopmodder: Close every SQLite handle so Windows does not keep DB locks.
        connection = connect_sqlite(self.db_path, row_factory=sqlite3.Row)
        try:
            yield connection
            connection.commit()
        finally:
            connection.close()

    def _initialize_fts(self, connection):#20260627_kpopmodder: Optional FTS5 index; SQL prefilter remains fallback.
        if self._fts_available is not None:
            return

        try:
            connection.execute("""
                CREATE VIRTUAL TABLE IF NOT EXISTS derived_memories_fts
                USING fts5(
                    title,
                    summary,
                    search_text,
                    normalized_key,
                    topic_key,
                    content='derived_memories',
                    content_rowid='id'
                )
            """)
            connection.execute("""
                CREATE TRIGGER IF NOT EXISTS derived_memories_ai
                AFTER INSERT ON derived_memories BEGIN
                    INSERT INTO derived_memories_fts(
                        rowid,
                        title,
                        summary,
                        search_text,
                        normalized_key,
                        topic_key
                    )
                    VALUES (
                        new.id,
                        new.title,
                        new.summary,
                        new.search_text,
                        new.normalized_key,
                        new.topic_key
                    );
                END
            """)
            connection.execute("""
                CREATE TRIGGER IF NOT EXISTS derived_memories_ad
                AFTER DELETE ON derived_memories BEGIN
                    INSERT INTO derived_memories_fts(
                        derived_memories_fts,
                        rowid,
                        title,
                        summary,
                        search_text,
                        normalized_key,
                        topic_key
                    )
                    VALUES (
                        'delete',
                        old.id,
                        old.title,
                        old.summary,
                        old.search_text,
                        old.normalized_key,
                        old.topic_key
                    );
                END
            """)
            connection.execute("""
                CREATE TRIGGER IF NOT EXISTS derived_memories_au
                AFTER UPDATE ON derived_memories BEGIN
                    INSERT INTO derived_memories_fts(
                        derived_memories_fts,
                        rowid,
                        title,
                        summary,
                        search_text,
                        normalized_key,
                        topic_key
                    )
                    VALUES (
                        'delete',
                        old.id,
                        old.title,
                        old.summary,
                        old.search_text,
                        old.normalized_key,
                        old.topic_key
                    );
                    INSERT INTO derived_memories_fts(
                        rowid,
                        title,
                        summary,
                        search_text,
                        normalized_key,
                        topic_key
                    )
                    VALUES (
                        new.id,
                        new.title,
                        new.summary,
                        new.search_text,
                        new.normalized_key,
                        new.topic_key
                    );
                END
            """)
            connection.execute("""
                INSERT INTO derived_memories_fts(
                    rowid,
                    title,
                    summary,
                    search_text,
                    normalized_key,
                    topic_key
                )
                SELECT
                    id,
                    title,
                    summary,
                    search_text,
                    normalized_key,
                    topic_key
                FROM derived_memories
                WHERE id NOT IN (
                    SELECT rowid FROM derived_memories_fts
                )
            """)
            self._fts_available = True
        except sqlite3.DatabaseError:
            self._fts_available = False

    def _search_fts_rows(self, connection, query, candidate_limit):
        if self._fts_available is not True:
            return []

        match_query = self._fts_match_query(query)
        if not match_query:
            return []

        try:
            return connection.execute(
                """
                SELECT dm.*
                FROM derived_memories_fts
                JOIN derived_memories AS dm
                    ON dm.id = derived_memories_fts.rowid
                WHERE derived_memories_fts MATCH ?
                ORDER BY bm25(derived_memories_fts), dm.last_created_ts DESC
                LIMIT ?
                """,
                (match_query, candidate_limit),
            ).fetchall()
        except sqlite3.DatabaseError:
            return []

    def _search_prefilter_rows(self, connection, query, candidate_limit):
        query_key = self._normalized_key(query)
        query_tokens = list(self._tokens(query))[:8]
        topic_key = self._topic_key_for_text(query)
        conditions = []
        params = []

        if query_key:
            conditions.append(
                "(normalized_key LIKE ? OR ? LIKE '%' || normalized_key || '%')"
            )
            params.extend((f"%{query_key}%", query_key))

        if topic_key:
            conditions.append("topic_key = ?")
            params.append(topic_key)

        for token in query_tokens:
            like_token = f"%{token}%"
            normalized_token = f"%{self._normalized_key(token)}%"
            conditions.append(
                "("
                "title LIKE ? OR summary LIKE ? OR search_text LIKE ? "
                "OR normalized_key LIKE ? OR topic_key LIKE ?"
                ")"
            )
            params.extend((
                like_token,
                like_token,
                like_token,
                normalized_token,
                like_token,
            ))

        if not conditions:
            return []

        sql = (
            "SELECT * FROM derived_memories WHERE "
            + " OR ".join(conditions)
            + " ORDER BY last_created_ts DESC, id DESC LIMIT ?"
        )
        params.append(candidate_limit)
        return connection.execute(sql, params).fetchall()

    def _merge_rows(self, first_rows, second_rows):
        merged = []
        seen = set()
        for row in list(first_rows or []) + list(second_rows or []):
            row_id = row["id"]
            if row_id in seen:
                continue
            seen.add(row_id)
            merged.append(row)
        return merged

    def _memory_values(self, memory_item):
        now_ts = time.time()
        metadata = memory_item.get("metadata", {})
        if not isinstance(metadata, dict):
            metadata = {
                "metadata_value": str(metadata),
            }

        search_text = self._clean_text(memory_item.get("search_text"))
        summary = self._clean_text(memory_item.get("summary")) or search_text
        title = self._clean_text(memory_item.get("title")) or summary[:80]
        normalized_key = (
            self._clean_text(memory_item.get("normalized_key"))
            or self._normalized_key(search_text)
        )

        return {
            "kind": str(memory_item.get("kind", "") or "unknown"),
            "title": title[:160],
            "summary": summary[:1200],
            "search_text": search_text[:2000],
            "normalized_key": normalized_key,
            "topic_key": str(memory_item.get("topic_key", "") or ""),
            "source_event_count": max(
                1,
                self._int_or_default(
                    memory_item.get("source_event_count"),
                    1,
                ),
            ),
            "duplicate_count": max(
                0,
                self._int_or_default(memory_item.get("duplicate_count"), 0),
            ),
            "first_created_at": str(memory_item.get("first_created_at", "") or ""),
            "last_created_at": str(memory_item.get("last_created_at", "") or ""),
            "first_created_ts": self._float_or_zero(
                memory_item.get("first_created_ts"),
            ),
            "last_created_ts": self._float_or_zero(
                memory_item.get("last_created_ts"),
            ),
            "confidence": self._float_or_default(
                memory_item.get("confidence"),
                0.7,
            ),
            "metadata_json": json.dumps(
                metadata,
                ensure_ascii=False,
                sort_keys=True,
                default=str,
            ),
            "created_ts": self._float_or_default(
                memory_item.get("created_ts"),
                now_ts,
            ),
            "updated_ts": self._float_or_default(
                memory_item.get("updated_ts"),
                now_ts,
            ),
        }

    def _row_to_memory(self, row):
        metadata_json = row["metadata_json"]
        try:
            metadata = json.loads(metadata_json)
        except Exception:
            metadata = {}
        if not isinstance(metadata, dict):
            metadata = {}

        item = dict(row)
        item["metadata"] = metadata
        return item

    def _score_item(self, query_key, query_tokens, item):
        search_text = self._clean_text(item.get("search_text"))
        summary = self._clean_text(item.get("summary"))
        candidate_key = self._normalized_key(search_text)
        candidate_tokens = self._tokens(search_text + " " + summary)

        score = 0.0
        if query_key and candidate_key:
            if query_key in candidate_key or candidate_key in query_key:
                score += 3.0
            score += SequenceMatcher(None, query_key, candidate_key).ratio()

        if query_tokens:
            overlap = query_tokens & candidate_tokens
            if overlap:
                score += 2.5 * len(overlap) / len(query_tokens)

        try:
            score += min(0.5, float(item.get("confidence", 0.0)) * 0.25)
        except Exception:
            pass

        return score

    def _tokens(self, text):
        return {
            token
            for token in re.findall(
                r"[0-9a-zA-Z_\uac00-\ud7a3]{2,}",
                self._clean_text(text).lower(),
            )
        }

    def _fts_match_query(self, text):
        tokens = list(self._tokens(text))[:8]
        safe_tokens = []
        for token in tokens:
            safe_token = re.sub(r"[^0-9a-zA-Z_\uac00-\ud7a3]+", "", token)
            if safe_token:
                safe_tokens.append(f"{safe_token}*")
        return " OR ".join(safe_tokens)

    def _topic_key_for_text(self, text):
        normalized = self._clean_text(text).lower()
        topics = (
            ("youtube", ("youtube", "\uc720\ud29c\ube0c")),
            ("screenvision", ("screenvision", "screen vision")),
            ("gptsovits", ("gpt-sovits", "gptsovits", "tts")),
            ("voiceinput", ("voiceinput", "stt", "whisper", "microphone", "mic")),
            ("gpu", ("cuda", "gpu", "vram")),
            ("memory", ("memory", "raw_events", "derived_memory")),
            ("code", ("python", "vscode", "visual studio code", "code")),
            ("game", ("game", "steam", "ghost hunter")),
            ("browser", ("browser", "chrome", "edge")),
            ("video", ("video", "stream", "movie")),
        )
        for topic, keywords in topics:
            if any(keyword in normalized for keyword in keywords):
                return topic
        return ""

    def _normalized_key(self, text):
        return re.sub(
            r"[^0-9a-zA-Z\uac00-\ud7a3]+",
            "",
            self._clean_text(text).lower(),
        )

    def _clean_text(self, text):
        return " ".join(str(text or "").strip().split())

    def _safe_limit(self, limit):
        try:
            return max(1, int(limit))
        except Exception:
            return 4

    def _int_or_default(self, value, default):
        try:
            return int(value)
        except Exception:
            return default

    def _float_or_zero(self, value):
        return self._float_or_default(value, 0.0)

    def _float_or_none(self, value):
        if value is None:
            return None
        try:
            return float(value)
        except Exception:
            return None

    def _float_or_default(self, value, default):
        try:
            return float(value)
        except Exception:
            return default
