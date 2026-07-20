#20260626_kpopmodder: Rebuild compact derived-memory cache from append-only raw_events.jsonl.
import argparse
import json
import os
import sys


SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_ROOT = os.path.dirname(SCRIPT_DIR)
if PROJECT_ROOT not in sys.path:
    sys.path.insert(0, PROJECT_ROOT)

from memory_core.derived_memory_builder import DerivedMemoryBuilder
from memory_core.raw_event_sqlite_store import RawEventSQLiteStore
from memory_core.derived_memory_sqlite_store import DerivedMemorySQLiteStore
from memory_core.rebuild_progress import TerminalProgressBar


terminal_progress_bar = TerminalProgressBar("manual_script", stream=sys.stdout)


def read_raw_events(jsonl_path):
    events = []
    if not os.path.exists(jsonl_path):
        return events

    with open(jsonl_path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            try:
                event = json.loads(line)
            except Exception:
                continue
            if isinstance(event, dict):
                events.append(event)

    return events


def read_raw_events_from_sqlite(db_path, jsonl_path=None):
    if not os.path.exists(db_path):
        return []

    store = RawEventSQLiteStore(db_path)
    store.initialize()
    if jsonl_path and os.path.exists(jsonl_path):
        store.import_jsonl(jsonl_path)
    return store.get_all_events()


def read_raw_events_for_rebuild(args):
    if args.source in {"auto", "sqlite"}:
        events = read_raw_events_from_sqlite(
            args.raw_events_db,
            jsonl_path=None if args.dry_run else args.raw_events_jsonl,
        )
        if events or args.source == "sqlite":
            return events, "sqlite"

    return read_raw_events(args.raw_events_jsonl), "jsonl"


def print_rebuild_progress(stage, **values):
    terminal_progress_bar(stage, **values)


def parse_args():
    default_memory_dir = os.path.join(PROJECT_ROOT, "memory")
    parser = argparse.ArgumentParser(
        description="Rebuild memory/derived_memory.sqlite3 from raw events.",
    )
    parser.add_argument(
        "--raw-events-jsonl",
        default=os.path.join(default_memory_dir, "raw_events.jsonl"),
    )
    parser.add_argument(
        "--raw-events-db",
        default=os.path.join(default_memory_dir, "raw_events.sqlite3"),
        help="SQLite raw-event mirror used first when --source=auto.",
    )
    parser.add_argument(
        "--derived-db",
        default=os.path.join(default_memory_dir, "derived_memory.sqlite3"),
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Analyze raw events without writing derived_memory.sqlite3.",
    )
    parser.add_argument(
        "--source",
        choices=["auto", "sqlite", "jsonl"],
        default="auto",
        help="Read raw events from SQLite first by default, falling back to JSONL.",
    )
    parser.add_argument(
        "--no-clear",
        action="store_true",
        help="Update existing derived rows instead of recreating the cache.",
    )
    return parser.parse_args()


def main():
    args = parse_args()
    raw_events, source = read_raw_events_for_rebuild(args)
    print_rebuild_progress(
        "read_raw_events_done",
        current=len(raw_events),
        total=len(raw_events),
    )
    builder = DerivedMemoryBuilder()
    derived_store = None

    if not args.dry_run:
        derived_store = DerivedMemorySQLiteStore(args.derived_db)

    stats = builder.rebuild(
        raw_events,
        derived_store=derived_store,
        clear=not args.no_clear,
        dry_run=args.dry_run,
        progress_callback=print_rebuild_progress,
    )

    print(f"raw_events_jsonl={args.raw_events_jsonl}")
    print(f"raw_events_db={args.raw_events_db}")
    print(f"source={source}")
    print(f"derived_db={args.derived_db}")
    print(f"dry_run={args.dry_run}")
    print(f"raw_event_count={stats.get('raw_event_count', 0)}")
    print(f"episode_count={stats.get('episode_count', 0)}")
    print(f"inserted_count={stats.get('inserted_count', 0)}")
    print(
        "merged_duplicate_count="
        f"{stats.get('merged_duplicate_count', 0)}"
    )
    print(f"skipped_noise_count={stats.get('skipped_noise_count', 0)}")


if __name__ == "__main__":
    main()
