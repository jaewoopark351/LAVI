#20260716_kpopmodder: Minimal CLI facade that delegates to existing startup scripts.
from __future__ import annotations

import argparse
import sys


def _parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        prog="python -m lavi",
        description="Run LAVI app and startup checks.",
    )
    subparsers = parser.add_subparsers(dest="command")

    subparsers.add_parser(
        "app",
        help="Run the Gradio app through the existing main.py entrypoint.",
    )

    smoke_parser = subparsers.add_parser(
        "smoke",
        help="Delegate to scripts.smoke_startup.",
    )
    smoke_parser.add_argument("script_args", nargs=argparse.REMAINDER)

    preflight_parser = subparsers.add_parser(
        "doctor",
        aliases=("preflight",),
        help="Delegate to scripts.preflight.",
    )
    preflight_parser.add_argument("script_args", nargs=argparse.REMAINDER)

    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    if argv is None:
        argv = sys.argv[1:]
    argv = list(argv)
    if argv[:1] == ["smoke"]:
        return _run_smoke(_script_args(argv[1:]))
    if argv[:1] in (["doctor"], ["preflight"]):
        return _run_preflight(_script_args(argv[1:]))

    args = _parse_args(argv)
    command = args.command or "app"

    if command == "app":
        return _run_app()
    if command == "smoke":
        return _run_smoke(_script_args(args.script_args))
    if command in ("doctor", "preflight"):
        return _run_preflight(_script_args(args.script_args))

    raise SystemExit(f"unknown command: {command}")


def _script_args(script_args: list[str]) -> list[str]:
    script_args = list(script_args or [])
    if script_args[:1] == ["--"]:
        return script_args[1:]
    return script_args


def _run_app() -> int:
    from main import main as run_main

    run_main()
    return 0


def _run_smoke(script_args: list[str]) -> int:
    from scripts.smoke_startup import main as run_smoke

    return int(run_smoke(script_args) or 0)


def _run_preflight(script_args: list[str]) -> int:
    from scripts.preflight import main as run_preflight

    return int(run_preflight(script_args) or 0)
