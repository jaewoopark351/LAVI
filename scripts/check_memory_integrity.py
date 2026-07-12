#20260626_kpopmodder: Check raw memory log/index integrity without changing raw_events.jsonl.
import argparse
import hashlib
import json
import os
import sqlite3
import sys
from contextlib import contextmanager


SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
if PROJECT_ROOT not in sys.path:
    sys.path.insert(0, PROJECT_ROOT)

from memory_core.raw_event_sqlite_store import RawEventSQLiteStore


def line_hash(line_text):
    return hashlib.sha256(
        str(line_text or "").encode("utf-8", errors="replace")
    ).hexdigest()


def read_raw_jsonl_hashes(jsonl_path):
    result = {
        "line_count": 0,
        "nonempty_line_count": 0,
        "valid_event_count": 0,
        "malformed_line_count": 0,
        "line_hashes": set(),
    }
    if not os.path.exists(jsonl_path):
        return result

    with open(jsonl_path, "r", encoding="utf-8") as f:
        for line in f:
            result["line_count"] += 1
            stripped = line.strip()
            if not stripped:
                continue

            result["nonempty_line_count"] += 1
            try:
                event = json.loads(stripped)
            except Exception:
                result["malformed_line_count"] += 1
                continue

            if not isinstance(event, dict):
                result["malformed_line_count"] += 1
                continue

            result["valid_event_count"] += 1
            result["line_hashes"].add(line_hash(stripped))

    return result


@contextmanager
def sqlite_connection(db_path):
    connection = sqlite3.connect(db_path)
    try:
        yield connection
    finally:
        connection.close()


def sqlite_table_exists(connection, table_name):
    row = connection.execute(
        """
        SELECT name
        FROM sqlite_master
        WHERE type = 'table' AND name = ?
        """,
        (table_name,),
    ).fetchone()
    return row is not None


def read_raw_sqlite_state(db_path):
    state = {
        "exists": os.path.exists(db_path),
        "table_exists": False,
        "count": 0,
        "line_hashes": set(),
        "error": "",
    }
    if not state["exists"]:
        return state

    try:
        with sqlite_connection(db_path) as connection:
            if not sqlite_table_exists(connection, "raw_events"):
                return state

            state["table_exists"] = True
            row = connection.execute(
                "SELECT COUNT(*) FROM raw_events"
            ).fetchone()
            state["count"] = int(row[0] or 0)
            rows = connection.execute(
                """
                SELECT line_hash
                FROM raw_events
                WHERE line_hash IS NOT NULL AND line_hash != ''
                """
            ).fetchall()
            state["line_hashes"] = {str(row[0]) for row in rows}
    except Exception as exc:
        state["error"] = str(exc)

    return state


def read_derived_sqlite_state(db_path):
    state = {
        "exists": os.path.exists(db_path),
        "table_exists": False,
        "count": 0,
        "error": "",
    }
    if not state["exists"]:
        return state

    try:
        with sqlite_connection(db_path) as connection:
            if not sqlite_table_exists(connection, "derived_memories"):
                return state

            state["table_exists"] = True
            row = connection.execute(
                "SELECT COUNT(*) FROM derived_memories"
            ).fetchone()
            state["count"] = int(row[0] or 0)
    except Exception as exc:
        state["error"] = str(exc)

    return state


#20260626_kpopmodder: Compare append-only JSONL with SQLite indexes without touching raw log content.
def build_report(
    raw_events_jsonl,
    raw_events_db,
    derived_db,
    sync=False,
):
    if sync:
        try:
            RawEventSQLiteStore(raw_events_db).import_jsonl(raw_events_jsonl)
        except Exception:
            pass

    jsonl_state = read_raw_jsonl_hashes(raw_events_jsonl)
    raw_sqlite_state = read_raw_sqlite_state(raw_events_db)
    derived_state = read_derived_sqlite_state(derived_db)

    jsonl_hashes = jsonl_state["line_hashes"]
    sqlite_hashes = raw_sqlite_state["line_hashes"]
    missing_hashes = jsonl_hashes - sqlite_hashes
    extra_hashes = sqlite_hashes - jsonl_hashes

    warnings = []
    if jsonl_state["malformed_line_count"]:
        warnings.append("raw_events_jsonl_has_malformed_lines")
    if not raw_sqlite_state["exists"]:
        warnings.append("raw_events_sqlite_missing")
    elif raw_sqlite_state["error"]:
        warnings.append("raw_events_sqlite_error")
    elif not raw_sqlite_state["table_exists"]:
        warnings.append("raw_events_sqlite_table_missing")
    if missing_hashes:
        warnings.append("raw_sqlite_missing_jsonl_events")
    if extra_hashes:
        warnings.append("raw_sqlite_has_extra_events")
    if derived_state["error"]:
        warnings.append("derived_memory_sqlite_error")

    return {
        "raw_events_jsonl": raw_events_jsonl,
        "raw_events_db": raw_events_db,
        "derived_db": derived_db,
        "sync": bool(sync),
        "raw_events_jsonl_line_count": jsonl_state["line_count"],
        "raw_events_jsonl_nonempty_line_count": jsonl_state["nonempty_line_count"],
        "raw_events_jsonl_valid_event_count": jsonl_state["valid_event_count"],
        "raw_events_jsonl_malformed_line_count": jsonl_state["malformed_line_count"],
        "raw_events_sqlite_exists": raw_sqlite_state["exists"],
        "raw_events_sqlite_table_exists": raw_sqlite_state["table_exists"],
        "raw_events_sqlite_count": raw_sqlite_state["count"],
        "raw_sqlite_missing_count": len(missing_hashes),
        "raw_sqlite_extra_count": len(extra_hashes),
        "derived_memory_exists": derived_state["exists"],
        "derived_memory_table_exists": derived_state["table_exists"],
        "derived_memory_count": derived_state["count"],
        "warnings": warnings,
        "status": "OK" if not warnings else "WARN",
    }


def print_report(report):
    for key in (
        "raw_events_jsonl",
        "raw_events_db",
        "derived_db",
        "sync",
        "raw_events_jsonl_line_count",
        "raw_events_jsonl_nonempty_line_count",
        "raw_events_jsonl_valid_event_count",
        "raw_events_jsonl_malformed_line_count",
        "raw_events_sqlite_exists",
        "raw_events_sqlite_table_exists",
        "raw_events_sqlite_count",
        "raw_sqlite_missing_count",
        "raw_sqlite_extra_count",
        "derived_memory_exists",
        "derived_memory_table_exists",
        "derived_memory_count",
        "status",
    ):
        print(f"{key}={report.get(key)}")

    warnings = report.get("warnings") or []
    print("warnings=" + ",".join(warnings))


def parse_args():
    default_memory_dir = os.path.join(PROJECT_ROOT, "memory")
    parser = argparse.ArgumentParser(
        description="Check raw memory JSONL/SQLite integrity.",
    )
    parser.add_argument(
        "--raw-events-jsonl",
        default=os.path.join(default_memory_dir, "raw_events.jsonl"),
    )
    parser.add_argument(
        "--raw-events-db",
        default=os.path.join(default_memory_dir, "raw_events.sqlite3"),
    )
    parser.add_argument(
        "--derived-db",
        default=os.path.join(default_memory_dir, "derived_memory.sqlite3"),
    )
    parser.add_argument(
        "--sync",
        action="store_true",
        help="Import JSONL into raw_events.sqlite3 before checking.",
    )
    return parser.parse_args()


def main():#20260626_kpopmodder: CLI entry point for quick memory health checks before commit/runtime debugging.
    args = parse_args()
    report = build_report(
        raw_events_jsonl=args.raw_events_jsonl,
        raw_events_db=args.raw_events_db,
        derived_db=args.derived_db,
        sync=args.sync,
    )
    print_report(report)
    return 0 if report["status"] == "OK" else 1


if __name__ == "__main__":
    raise SystemExit(main())
