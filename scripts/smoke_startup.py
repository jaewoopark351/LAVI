#20260716_kpopmodder: P0-B smoke runner for Core offline and production config checks.
from __future__ import annotations

import argparse
import contextlib
import hashlib
import json
import os
from pathlib import Path
import queue
import sys
import _thread
import threading
import time
from unittest import mock

PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from core.profile_resolver import load_module_settings  # noqa: E402


EXPECTED_MODULES_JSON_HASH = "ddffc5475ef92bd40e5b0e08bce42e0c6b2b1019"

from scripts.smoke_startup_core import (  # noqa: E402
    AttemptCounters,
    SideEffectAttempt,
    SmokeError,
    SmokeSideEffectGuard,
    SmokeTimeout,
    SmokeTimer,
    _install_original_import_module_marker,
)

__all__ = [
    "AttemptCounters",
    "SideEffectAttempt",
    "SmokeError",
    "SmokeSideEffectGuard",
    "SmokeTimeout",
    "SmokeTimer",
    "_install_original_import_module_marker",
    "run_core_offline_smoke",
    "run_production_config_smoke",
]









@contextlib.contextmanager
def startup_timeout(timeout_sec, label):
    timeout = float(timeout_sec or 0)
    if timeout <= 0:
        yield
        return

    expired = {"value": False}

    def interrupt_main_thread():
        expired["value"] = True
        _thread.interrupt_main()

    timer = threading.Timer(timeout, interrupt_main_thread)
    timer.daemon = True
    timer.start()
    try:
        yield
    except KeyboardInterrupt as exc:
        if expired["value"]:
            raise SmokeTimeout(
                f"{label} exceeded timeout while running: timeout={timeout}s"
            ) from exc
        raise
    finally:
        timer.cancel()


def run_with_timeout(timeout_sec, label, function):
    timeout = float(timeout_sec or 0)
    if timeout <= 0:
        return function()

    results = queue.Queue(maxsize=1)

    def target():
        try:
            results.put(("result", function(), None))
        except BaseException as exc:
            results.put(("error", exc, exc.__traceback__))

    worker = threading.Thread(
        target=target,
        name=f"{label.replace(' ', '')}TimeoutWorker",
        daemon=True,
    )
    worker.start()
    worker.join(timeout)
    if worker.is_alive():
        raise SmokeTimeout(
            f"{label} exceeded timeout while running: timeout={timeout}s"
        )

    kind, value, tb = results.get_nowait()
    if kind == "error":
        raise value.with_traceback(tb)
    return value










@contextlib.contextmanager
def smoke_environment(profile, modules_config=None, config_file=None):
    updates = {
        "LAVI_PROFILE": profile or "",
        "GRADIO_ANALYTICS_ENABLED": "False",
        "HF_HUB_DISABLE_TELEMETRY": "1",
        "DO_NOT_TRACK": "1",
    }
    if modules_config:
        updates["LAVI_MODULES_CONFIG"] = str(modules_config)
    if config_file:
        updates["LAVI_CONFIG_FILE"] = str(config_file)

    old_values = {
        key: os.environ.get(key)
        for key in updates
    }
    try:
        for key, value in updates.items():
            if value:
                os.environ[key] = value
            else:
                os.environ.pop(key, None)
        yield
    finally:
        for key, old_value in old_values.items():
            if old_value is None:
                os.environ.pop(key, None)
            else:
                os.environ[key] = old_value


def git_blob_hash(path):
    data = Path(path).read_bytes()
    data = data.replace(b"\r\n", b"\n")
    header = f"blob {len(data)}\0".encode("utf-8")
    return hashlib.sha1(header + data).hexdigest()


def root_modules_hash(project_root):
    return git_blob_hash(Path(project_root) / "modules.json")


def _disabled_optional_modules(settings):
    from app_core.optional_module_manifest import OPTIONAL_MODULE_MANIFEST

    disabled = []
    for module_name, manifest in OPTIONAL_MODULE_MANIFEST.items():
        if settings.get(module_name) is not True:
            disabled.append(manifest["module_path"])
    return disabled


def _optional_component_state(composer):
    return {
        "SongPlayer": composer.song_player is not None,
        "Chess": composer.chess_plugin is not None,
        "StarCraftRemastered": composer.starcraft_plugin is not None,
        "StarCraft116": composer.starcraft116_plugin is not None,
        "StarCraft2": composer.starcraft2_plugin is not None,
        "ScreenVision": composer.screen_vision is not None,
    }


def _selected_provider_names(composer):
    return {
        "input": composer.input.current_plugin.__class__.__name__,
        "llm": composer.llm.current_plugin.__class__.__name__,
        "translation": composer.translate.current_plugin.__class__.__name__,
        "tts": composer.tts.current_plugin.__class__.__name__,
        "vtuber": composer.vtuber.current_plugin.__class__.__name__,
    }


def _assert_core_providers(selected_providers):
    expected = {
        "input": "NullInput",
        "llm": "NullLLM",
        "translation": "NoTranslate",
        "tts": "NullTTS",
        "vtuber": "NullVtuber",
    }
    if selected_providers != expected:
        raise SmokeError(
            "Core smoke selected unexpected providers: "
            f"expected={expected} actual={selected_providers}"
        )


def _assert_modules_hash(project_root):
    actual_hash = root_modules_hash(project_root)
    if actual_hash != EXPECTED_MODULES_JSON_HASH:
        raise SmokeError(
            "modules.json hash drift: "
            f"expected={EXPECTED_MODULES_JSON_HASH} actual={actual_hash}"
        )
    return actual_hash


def run_core_offline_smoke(
    project_root,
    profile,
    modules_config,
    accelerator,
    offline,
    timeout_sec,
):
    return run_with_timeout(
        timeout_sec,
        "Core smoke",
        lambda: _run_core_offline_smoke(
            project_root,
            profile,
            modules_config,
            accelerator,
            offline,
            timeout_sec,
        ),
    )


def _run_core_offline_smoke(
    project_root,
    profile,
    modules_config,
    accelerator,
    offline,
    timeout_sec,
):
    if not offline:
        raise SmokeError("Core smoke requires --offline")

    project_root = Path(project_root).resolve()
    started_at = time.monotonic()
    modules_hash_before = _assert_modules_hash(project_root)
    smoke_state_dir = project_root / "test" / "test_Isolation" / "smoke_startup"
    smoke_state_dir.mkdir(parents=True, exist_ok=True)
    smoke_config_file = smoke_state_dir / "config.ini"
    resolution = load_module_settings(
        project_root,
        profile=profile,
        modules_config=modules_config,
    )
    counters = AttemptCounters()
    disabled_optional_modules = _disabled_optional_modules(resolution.settings)
    shutdown_completed = False

    _install_original_import_module_marker()
    with smoke_environment(profile, modules_config, smoke_config_file):
        import app_core.app_composer as app_composer_module
        from app_core.app_composer import AppComposer
        from plugin_system.registry import plugin_registry

        composer = AppComposer(str(project_root))
        original_bootstrap_memory = app_composer_module.bootstrap_memory

        def smoke_bootstrap_memory(_current_module_directory):
            return original_bootstrap_memory(str(smoke_state_dir))

        try:
            with SmokeSideEffectGuard(counters, disabled_optional_modules):
                composer.configure_logging()
                composer.prepare_plugin_path()
                composer.load_plugins()
                composer.log_gpu_startup()
                with mock.patch(
                    "app_core.app_composer.bootstrap_memory",
                    smoke_bootstrap_memory,
                ):
                    composer.bootstrap_memory()
                    composer.build_screen_question_router()
                    composer.build_interface()
                    composer.create_runtime_lifecycle()
        finally:
            if composer.runtime_lifecycle is not None:
                composer.runtime_lifecycle.shutdown()
                shutdown_completed = composer.runtime_lifecycle.app_shutdown_done
            else:
                composer._shutdown_on_startup_failure()
                shutdown_completed = True

        selected_providers = _selected_provider_names(composer)
        optional_components = _optional_component_state(composer)
        plugin_status = plugin_registry.snapshot()

    disabled_constructed = [
        name
        for name, constructed in optional_components.items()
        if constructed and resolution.settings.get(name) is not True
    ]
    counters.disabled_optional_constructions += len(disabled_constructed)
    counters.disabled_optional_initializations += len(disabled_constructed)
    counters.assert_zero()
    _assert_core_providers(selected_providers)

    if not shutdown_completed:
        raise SmokeError("Core smoke shutdown did not complete")

    elapsed_sec = time.monotonic() - started_at
    if elapsed_sec > float(timeout_sec):
        raise SmokeError(
            f"Core smoke exceeded timeout: elapsed={elapsed_sec:.2f}s "
            f"timeout={timeout_sec}s"
        )

    modules_hash_after = _assert_modules_hash(project_root)
    return {
        "mode": "core_offline_smoke",
        "profile": profile,
        "accelerator": accelerator,
        "offline": bool(offline),
        "modules_config_path": str(resolution.path),
        "modules_config_source": resolution.source,
        "smoke_state_dir": str(smoke_state_dir),
        "modules_json_hash_before": modules_hash_before,
        "modules_json_hash_after": modules_hash_after,
        "selected_providers": selected_providers,
        "optional_components": optional_components,
        "plugin_status": plugin_status,
        **counters.as_dict(),
        "shutdown_completed": shutdown_completed,
        "elapsed_sec": round(elapsed_sec, 3),
    }


def _manifest_file_path(project_root, module_path):
    return Path(project_root).joinpath(*module_path.split(".")).with_suffix(".py")


def run_production_config_smoke(project_root, timeout_sec):
    return run_with_timeout(
        timeout_sec,
        "production config smoke",
        lambda: _run_production_config_smoke(project_root, timeout_sec),
    )


def _run_production_config_smoke(project_root, timeout_sec):
    project_root = Path(project_root).resolve()
    started_at = time.monotonic()
    modules_hash_before = _assert_modules_hash(project_root)
    resolution = load_module_settings(project_root, argv=[], environ={})
    if resolution.source != "production":
        raise SmokeError(
            "production config smoke requires root modules.json; "
            f"resolved source={resolution.source} path={resolution.path}"
        )
    if not resolution.settings:
        raise SmokeError("production modules.json is empty")

    invalid_entries = {
        key: value
        for key, value in resolution.settings.items()
        if not isinstance(key, str) or not key or not isinstance(value, bool)
    }
    if invalid_entries:
        raise SmokeError(f"invalid modules.json entries: {invalid_entries}")

    from app_core.optional_module_manifest import OPTIONAL_MODULE_MANIFEST

    optional_status = {}
    for module_name, manifest in OPTIONAL_MODULE_MANIFEST.items():
        enabled = resolution.settings.get(module_name) is True
        module_file = _manifest_file_path(project_root, manifest["module_path"])
        status = "DISABLED"
        if enabled:
            status = "CONFIGURED_NOT_STARTED"
            if not module_file.exists():
                status = "UNAVAILABLE"
        optional_status[module_name] = {
            "enabled": enabled,
            "status": status,
            "module_path": manifest["module_path"],
            "module_file_exists": module_file.exists(),
        }

    elapsed_sec = time.monotonic() - started_at
    if elapsed_sec > float(timeout_sec):
        raise SmokeError(
            f"production config smoke exceeded timeout: "
            f"elapsed={elapsed_sec:.2f}s timeout={timeout_sec}s"
        )

    modules_hash_after = _assert_modules_hash(project_root)
    return {
        "mode": "production_config_smoke",
        "validation_scope": "configuration_only",
        "modules_config_path": str(resolution.path),
        "modules_config_source": resolution.source,
        "modules_json_hash_before": modules_hash_before,
        "modules_json_hash_after": modules_hash_after,
        "module_count": len(resolution.settings),
        "optional_status": optional_status,
        "plugin_import_attempted": False,
        "resource_start_attempted": False,
        "shutdown_completed": True,
        "elapsed_sec": round(elapsed_sec, 3),
    }


def build_parser():
    parser = argparse.ArgumentParser(description="Run LAVI startup smoke checks.")
    parser.add_argument(
        "--profile",
        default="Core",
        choices=("Core", "Full", "Voice", "Vision", "Games"),
    )
    parser.add_argument(
        "--accelerator",
        default="CPU",
        choices=("CPU", "cu130"),
    )
    parser.add_argument("--modules-config", default="")
    parser.add_argument("--offline", action="store_true")
    parser.add_argument("--timeout-sec", type=float, default=30.0)
    parser.add_argument("--production-config-smoke", action="store_true")
    return parser


def main(argv=None):
    args = build_parser().parse_args(argv)
    try:
        if args.production_config_smoke:
            result = run_production_config_smoke(
                PROJECT_ROOT,
                timeout_sec=args.timeout_sec,
            )
        else:
            result = run_core_offline_smoke(
                PROJECT_ROOT,
                profile=args.profile,
                modules_config=args.modules_config,
                accelerator=args.accelerator,
                offline=args.offline,
                timeout_sec=args.timeout_sec,
            )
    except Exception as error:
        print(f"[LAVI smoke][FAIL] {type(error).__name__}: {error}", file=sys.stderr)
        return 1

    print(json.dumps(result, indent=2, sort_keys=True))
    print("[LAVI smoke][OK]")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
