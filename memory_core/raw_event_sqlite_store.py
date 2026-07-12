#20260622_kpopmodder: SQLite index for raw_events.jsonl while keeping JSONL as the recovery log.
import hashlib
import json
import os
from contextlib import contextmanager

from memory_core.sqlite_write_gate import connect_sqlite, sqlite_writer_lock


class RawEventSQLiteStore:
    def __init__(self, db_path):
        self.db_path = db_path

    def initialize(self):#20260622_kpopmodder: Prepare the SQLite table used to query raw_events.jsonl.
        db_dir = os.path.dirname(self.db_path)
        if db_dir:
            os.makedirs(db_dir, exist_ok=True)#20260627_kpopmodder: Filename-only SQLite paths have no directory to create.

        with sqlite_writer_lock(self.db_path):#20260629_kpopmodder: Serialize SQLite schema writers across app/scripts.
            with self._connect() as connection:
                connection.execute("""
                    CREATE TABLE IF NOT EXISTS raw_events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        event_type TEXT NOT NULL,
                        value TEXT NOT NULL,
                        source TEXT NOT NULL,
                        metadata_json TEXT NOT NULL,
                        created_at TEXT NOT NULL,
                        created_ts REAL NOT NULL,
                        line_hash TEXT UNIQUE
                    )
                """)
                connection.execute("""
                    CREATE INDEX IF NOT EXISTS idx_raw_events_created_ts
                    ON raw_events(created_ts)
                """)
                connection.execute("""
                    CREATE INDEX IF NOT EXISTS idx_raw_events_event_type
                    ON raw_events(event_type)
                """)

    def add_event(self, event, line_text=None):#20260622_kpopmodder: Mirror each JSONL raw event into SQLite.
        self.initialize()
        line_hash = self._line_hash(line_text or self._canonical_line(event))

        with sqlite_writer_lock(self.db_path):#20260629_kpopmodder: Keep runtime app writes and maintenance imports one-at-a-time.
            with self._connect() as connection:
                connection.execute(
                    """
                    INSERT OR IGNORE INTO raw_events (
                        event_type,
                        value,
                        source,
                        metadata_json,
                        created_at,
                        created_ts,
                        line_hash
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    self._event_values(event, line_hash),
                )

    def import_jsonl(self, jsonl_path):#20260622_kpopmodder: Import existing JSONL rows into SQLite without duplicates.
        if not os.path.exists(jsonl_path):
            return

        self.initialize()

        with sqlite_writer_lock(self.db_path):#20260629_kpopmodder: JSONL sync may be long, so it owns the shared writer gate.
            with self._connect() as connection:
                with open(jsonl_path, "r", encoding="utf-8") as f:
                    for line in f:
                        line = line.strip()
                        if not line:
                            continue

                        try:
                            event = json.loads(line)
                        except Exception:
                            continue

                        if not isinstance(event, dict):
                            continue

                        connection.execute(
                            """
                            INSERT OR IGNORE INTO raw_events (
                                event_type,
                                value,
                                source,
                                metadata_json,
                                created_at,
                                created_ts,
                                line_hash
                            )
                            VALUES (?, ?, ?, ?, ?, ?, ?)
                            """,
                            self._event_values(event, self._line_hash(line)),
                        )

    def get_recent_events(self, limit):#20260622_kpopmodder: Return recent raw events in chronological order for recall.
        self.initialize()

        with self._connect() as connection:
            rows = connection.execute(
                """
                SELECT
                    id,
                    event_type,
                    value,
                    source,
                    metadata_json,
                    created_at,
                    created_ts,
                    line_hash
                FROM raw_events
                ORDER BY created_ts DESC, id DESC
                LIMIT ?
                """,
                (int(limit),),
            ).fetchall()

        events = [self._row_to_event(row) for row in reversed(rows)]
        return [event for event in events if event is not None]

    def get_all_events(self):#20260626_kpopmodder: Long recall may scan every raw event for accuracy.
        return list(self.iter_events())

    def iter_events(
        self,
        limit=None,
        event_types=None,
        batch_size=500,
    ):#20260703_kpopmodder: Stream raw recall rows instead of fetchall() loading the DB.
        self.initialize()

        batch_size = max(1, int(batch_size or 500))
        params = []
        where_sql = ""
        normalized_event_types = [
            str(event_type)
            for event_type in (event_types or [])
            if str(event_type or "").strip()
        ]
        if normalized_event_types:
            placeholders = ", ".join("?" for _ in normalized_event_types)
            where_sql = f"WHERE event_type IN ({placeholders})"
            params.extend(normalized_event_types)

        limit_sql = ""
        if limit is not None:
            limit_sql = "LIMIT ?"
            params.append(max(1, int(limit)))

        query = f"""
            SELECT
                id,
                event_type,
                value,
                source,
                metadata_json,
                created_at,
                created_ts,
                line_hash
            FROM raw_events
            {where_sql}
            ORDER BY created_ts ASC, id ASC
            {limit_sql}
        """

        with self._connect() as connection:
            cursor = connection.execute(query, tuple(params))
            while True:
                rows = cursor.fetchmany(batch_size)
                if not rows:
                    break
                for row in rows:
                    event = self._row_to_event(row)
                    if event is not None:
                        yield event

    @contextmanager
    def _connect(self):#20260622_kpopmodder: Always close SQLite handles so Windows can release the db file.
        connection = connect_sqlite(self.db_path)
        try:
            yield connection
            connection.commit()
        finally:
            connection.close()

    def _event_values(self, event, line_hash):
        metadata = event.get("metadata", {})
        if not isinstance(metadata, dict):
            metadata = {
                "metadata_value": str(metadata),
            }

        return (
            str(event.get("event_type", "")),
            str(event.get("value", "")),
            str(event.get("source", "unknown")),
            json.dumps(metadata, ensure_ascii=False, default=str),
            str(event.get("created_at", "")),
            self._float_or_zero(event.get("created_ts")),
            line_hash,
        )

    def _row_to_event(self, row):#20260626_kpopmodder: Include SQLite row evidence so recall logs can point to raw storage.
        try:
            metadata = json.loads(row[4])
        except Exception:
            metadata = {}

        if not isinstance(metadata, dict):
            metadata = {}

        return {
            "event_type": row[1],
            "value": row[2],
            "source": row[3],
            "metadata": metadata,
            "created_at": row[5],
            "created_ts": row[6],
            "raw_event_id": row[0],#20260626_kpopmodder: Keep recall evidence traceable to raw_events.sqlite3.
            "raw_line_hash": row[7],
        }

    def _canonical_line(self, event):
        return json.dumps(
            event,
            ensure_ascii=False,
            sort_keys=True,
            default=str,
        )

    def _line_hash(self, line_text):
        return hashlib.sha256(
            str(line_text or "").encode("utf-8", errors="replace")
        ).hexdigest()

    def _float_or_zero(self, value):
        try:
            return float(value)
        except Exception:
            return 0.0
