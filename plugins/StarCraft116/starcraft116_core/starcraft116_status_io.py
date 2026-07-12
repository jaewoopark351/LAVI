#20260706_kpopmodder: Read-only status file/process parsing helpers split from StarCraft116StatusReader.
import csv
import io
import json
import locale
import os
import subprocess


def read_starcraft116_ini_values(path):
    values = {}
    with open(path, "r", encoding="utf-8", errors="replace") as file:
        for line in file:
            stripped = line.strip()
            if not stripped or stripped.startswith(("#", ";", "[")):
                continue
            if "=" not in stripped:
                continue
            key, value = stripped.split("=", 1)
            values[key.strip().lower()] = value.strip()
    return values


def read_starcraft116_tail_lines(path, line_count=80, max_bytes=65536):
    with open(path, "rb") as file:
        file.seek(0, os.SEEK_END)
        size = file.tell()
        file.seek(max(0, size - max_bytes), os.SEEK_SET)
        data = file.read()
    text = decode_starcraft116_text(data)
    return text.splitlines()[-line_count:]


def read_latest_starcraft116_jsonl_event(path, line_count=80):
    for line in reversed(read_starcraft116_tail_lines(path, line_count=line_count)):
        stripped = line.strip()
        if not stripped:
            continue
        try:
            event = json.loads(stripped)
        except json.JSONDecodeError:
            continue
        if isinstance(event, dict):
            return event
    return {}


def starcraft116_tasklist_rows(process_name):
    output = subprocess.check_output(
        [
            "tasklist",
            "/FI",
            f"IMAGENAME eq {process_name}",
            "/FO",
            "CSV",
            "/NH",
        ],
        stderr=subprocess.STDOUT,
        timeout=2,
        text=True,
        encoding="mbcs",
        errors="replace",
    )
    return parse_starcraft116_tasklist_output(output)


def parse_starcraft116_tasklist_output(output):
    rows = []
    for raw_line in str(output or "").splitlines():
        stripped = raw_line.strip()
        if not stripped or stripped.startswith(("INFO:", "ERROR:")):
            continue
        for columns in csv.reader(io.StringIO(stripped)):
            if len(columns) < 2:
                continue
            try:
                pid = int(str(columns[1]).strip())
            except ValueError:
                pid = 0
            rows.append({
                "image": str(columns[0]).strip(),
                "pid": pid,
                "session_name": str(columns[2]).strip()
                if len(columns) > 2
                else "",
                "memory": str(columns[4]).strip() if len(columns) > 4 else "",
            })
    return rows


def basename_starcraft116_path(value):
    value = str(value or "").strip().replace("\\", "/")
    if not value:
        return ""
    return value.rsplit("/", 1)[-1]


def decode_starcraft116_text(data):
    encodings = [
        locale.getpreferredencoding(False),
        "utf-8",
        "cp949",
        "mbcs",
    ]
    tried = set()
    for encoding in encodings:
        if not encoding or encoding in tried:
            continue
        tried.add(encoding)
        try:
            return data.decode(encoding)
        except (LookupError, UnicodeDecodeError):
            continue
    return data.decode("utf-8", errors="ignore")
