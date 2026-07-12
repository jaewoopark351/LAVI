#20260629_kpopmodder: Serialize memory SQLite writers across app/scripts without changing memory layering.
import os
import sqlite3
import time
from contextlib import contextmanager

if os.name == "nt":
    import msvcrt
else:
    import fcntl


DEFAULT_SQLITE_TIMEOUT_SEC = 15.0
DEFAULT_SQLITE_BUSY_TIMEOUT_MS = 15000
DEFAULT_LOCK_POLL_INTERVAL_SEC = 0.05
LOCK_FILE_NAME = ".sqlite_writer.lock"


class SQLiteWriteLockTimeout(TimeoutError):
    pass


def sqlite_writer_lock_path(db_path):
    db_path = os.path.abspath(str(db_path or "memory.sqlite3"))
    db_dir = os.path.dirname(db_path) or os.getcwd()
    return os.path.join(db_dir, LOCK_FILE_NAME)


def connect_sqlite(
    db_path,
    row_factory=None,
    timeout_sec=DEFAULT_SQLITE_TIMEOUT_SEC,
    busy_timeout_ms=DEFAULT_SQLITE_BUSY_TIMEOUT_MS,
    enable_wal=True,
):
    connection = sqlite3.connect(
        db_path,
        timeout=float(timeout_sec),
    )
    try:
        if row_factory is not None:
            connection.row_factory = row_factory
        connection.execute(
            f"PRAGMA busy_timeout = {max(0, int(busy_timeout_ms))}"
        )
        if enable_wal:
            try:
                connection.execute("PRAGMA journal_mode = WAL")
            except Exception:
                pass
        return connection
    except Exception:
        connection.close()
        raise


@contextmanager
def sqlite_writer_lock(
    db_path,
    timeout_sec=DEFAULT_SQLITE_TIMEOUT_SEC,
    poll_interval_sec=DEFAULT_LOCK_POLL_INTERVAL_SEC,
):
    lock_path = sqlite_writer_lock_path(db_path)
    lock_dir = os.path.dirname(lock_path)
    if lock_dir:
        os.makedirs(lock_dir, exist_ok=True)

    with open(lock_path, "a+b") as lock_file:
        _ensure_lock_byte(lock_file)
        _acquire_file_lock(
            lock_file,
            lock_path=lock_path,
            timeout_sec=timeout_sec,
            poll_interval_sec=poll_interval_sec,
        )
        try:
            yield lock_path
        finally:
            _unlock_file(lock_file)


def _ensure_lock_byte(lock_file):
    lock_file.seek(0, os.SEEK_END)
    if lock_file.tell() < 1:
        lock_file.write(b"\0")
        lock_file.flush()
        os.fsync(lock_file.fileno())
    lock_file.seek(0)


def _acquire_file_lock(lock_file, lock_path, timeout_sec, poll_interval_sec):
    deadline = None
    if timeout_sec is not None:
        deadline = time.monotonic() + max(0.0, float(timeout_sec))

    while True:
        try:
            _lock_file(lock_file)
            return
        except OSError as exc:
            if deadline is not None and time.monotonic() >= deadline:
                raise SQLiteWriteLockTimeout(
                    "Memory SQLite writer lock timed out: "
                    f"{lock_path}"
                ) from exc
            time.sleep(max(0.001, float(poll_interval_sec)))


if os.name == "nt":

    def _lock_file(lock_file):
        lock_file.seek(0)
        msvcrt.locking(lock_file.fileno(), msvcrt.LK_NBLCK, 1)


    def _unlock_file(lock_file):
        lock_file.seek(0)
        msvcrt.locking(lock_file.fileno(), msvcrt.LK_UNLCK, 1)

else:

    def _lock_file(lock_file):
        fcntl.flock(lock_file.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)


    def _unlock_file(lock_file):
        fcntl.flock(lock_file.fileno(), fcntl.LOCK_UN)
