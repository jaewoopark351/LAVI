#20260818_kpopmodder: Orchestrate fail-closed test-only admission before one live submit.
from __future__ import annotations

from typing import Callable, Mapping

from .approval_record import (
    parse_approval_record,
    validate_approval_record,
)
from .command_fingerprint import command_fingerprint
from .loopback_endpoint import (
    inspect_loopback_gradio_url,
)
from .minecraft_log_identity import (
    inspect_minecraft_log_identity,
)
from .preflight_decision import (
    preflight_decision,
)
from .preflight_environment_validator import validate_preflight_environment
from .runtime_status_identity import (
    inspect_runtime_status,
)
from .runtime_environment import DEFAULT_KOREAN_COMMAND
from .windows_listener_preflight import (
    inspect_windows_listener_identity,
)
from .world_identity import inspect_world_identity


def run_live_runtime_preflight(
    environment: Mapping[str, object],
    *,
    status_reader: Callable[[], dict[str, object]],
    process_probe: Callable[..., dict[str, object]] = inspect_windows_listener_identity,
    log_identity_inspector: Callable[..., dict[str, object]] = inspect_minecraft_log_identity,
    approval_reader: Callable[[], object] | None = None,
) -> dict[str, object]:
    selected = bool(environment.get("live_opt_in")) and bool(
        environment.get("mutating_opt_in")
    )
    if not selected:
        return preflight_decision(
            status="skip",
            selected_mutating_run=False,
            stage="opt_in",
            reason="live and mutating opt-ins are both required",
        )
    required_error = validate_preflight_environment(environment)
    if required_error:
        return _failure("approval", required_error)

    endpoint = inspect_loopback_gradio_url(environment.get("gradio_url"))
    if not endpoint.get("ok"):
        return _failure("endpoint", str(endpoint.get("reason") or "invalid endpoint"))

    read_approval = approval_reader or (lambda: environment.get("approval_json"))
    approval, approval_error = _read_approval(read_approval)
    if approval_error:
        return _failure("approval", approval_error)
    approval_expected = _approval_expected(environment, endpoint)
    approval_error = validate_approval_record(approval, approval_expected)
    if approval_error:
        return _failure("approval", approval_error)

    observed = {
        "gradio_url": endpoint["url"],
        "approved_command_is_default": environment.get("command")
        == DEFAULT_KOREAN_COMMAND,
        "approved_command_fingerprint": command_fingerprint(
            environment.get("command")
        ),
        "approval_source": str(approval.get("approval_source") or ""),
        "approval_tuple_verified": True,
        "fabric_endpoint": f"ws://127.0.0.1:{int(environment['fabric_port'])}",
    }

    initial_process = _process_identity(environment, endpoint, process_probe)
    if not initial_process.get("ok"):
        return _failure(
            "process_identity",
            str(initial_process.get("reason") or "process identity failed"),
            observed,
        )
    observed.update(_observed(initial_process))

    initial_status = _read_status(status_reader)
    if not initial_status.get("ok"):
        return _failure("bridge", str(initial_status["reason"]), observed)
    status_identity = inspect_runtime_status(
        initial_status["payload"],
        expected_backend=str(environment["expected_backend"]),
    )
    if not status_identity.get("ok"):
        return _failure(
            "idle",
            str(status_identity.get("reason") or "runtime status failed"),
            {**observed, **_observed(status_identity)},
        )
    observed.update(_observed(status_identity))

    world_identity = inspect_world_identity(
        environment,
        _observed(status_identity),
        log_identity_inspector,
    )
    if not world_identity.get("ok"):
        return _failure(
            "world",
            str(world_identity.get("reason") or "world identity failed"),
            {**observed, **_observed(world_identity)},
        )
    observed.update(_observed(world_identity))

    final_environment_error = validate_preflight_environment(environment)
    if final_environment_error:
        return _failure("pre_submit_recheck", final_environment_error, observed)
    final_endpoint = inspect_loopback_gradio_url(environment.get("gradio_url"))
    if not final_endpoint.get("ok") or final_endpoint.get("url") != endpoint.get("url"):
        return _failure("pre_submit_recheck", "Gradio endpoint changed", observed)
    final_approval, approval_error = _read_approval(read_approval)
    if approval_error:
        return _failure("pre_submit_recheck", approval_error, observed)
    final_approval_expected = _approval_expected(environment, final_endpoint)
    approval_error = validate_approval_record(
        final_approval,
        final_approval_expected,
    )
    if approval_error or final_approval != approval:
        return _failure(
            "pre_submit_recheck",
            approval_error or "approval record changed during preflight",
            observed,
        )
    final_process = _process_identity(environment, endpoint, process_probe)
    if not final_process.get("ok"):
        return _failure(
            "pre_submit_recheck",
            str(final_process.get("reason") or "process recheck failed"),
            observed,
        )
    if _process_identity_key(initial_process) != _process_identity_key(final_process):
        return _failure(
            "pre_submit_recheck",
            "listener owner identity changed during preflight",
            observed,
        )
    final_status = _read_status(status_reader)
    if not final_status.get("ok"):
        return _failure("pre_submit_recheck", str(final_status["reason"]), observed)
    final_status_identity = inspect_runtime_status(
        final_status["payload"],
        expected_backend=str(environment["expected_backend"]),
    )
    if not final_status_identity.get("ok"):
        return _failure(
            "pre_submit_recheck",
            str(final_status_identity.get("reason") or "status recheck failed"),
            {**observed, **_observed(final_status_identity)},
        )
    observed.update(_observed(final_status_identity))
    final_world_identity = inspect_world_identity(
        environment,
        _observed(final_status_identity),
        log_identity_inspector,
    )
    if not final_world_identity.get("ok"):
        return _failure(
            "pre_submit_recheck",
            str(final_world_identity.get("reason") or "world recheck failed"),
            {**observed, **_observed(final_world_identity)},
        )
    observed.update(_observed(final_world_identity))
    observed["pre_submit_recheck_passed"] = True
    return preflight_decision(
        status="ok",
        selected_mutating_run=True,
        stage="pre_submit_recheck",
        reason="all mutating live preflight evidence matched",
        observed=observed,
    )


def _approval_expected(
    environment: Mapping[str, object],
    endpoint: Mapping[str, object],
) -> dict[str, object]:
    return {
        "command": environment.get("command"),
        "gradio_url": endpoint.get("url"),
        "backend": environment.get("expected_backend"),
        "instance": environment.get("expected_instance"),
        "world": environment.get("expected_world"),
        "invocation_id": environment.get("invocation_id"),
    }


def _process_identity(
    environment: Mapping[str, object],
    endpoint: Mapping[str, object],
    process_probe: Callable[..., dict[str, object]],
) -> dict[str, object]:
    return process_probe(
        gradio_port=int(endpoint["port"]),
        fabric_port=int(environment["fabric_port"]),
        gradio_range_start=int(environment["gradio_range_start"]),
        gradio_range_end=int(environment["gradio_range_end"]),
        repository_root=str(environment["repository_root"]),
    )


def _read_status(
    status_reader: Callable[[], dict[str, object]],
) -> dict[str, object]:
    try:
        payload = status_reader()
    except Exception as error:
        return {
            "ok": False,
            "reason": f"runtime status read failed: {type(error).__name__}: {error}",
            "payload": {},
        }
    if not isinstance(payload, Mapping):
        return {"ok": False, "reason": "runtime status is not an object", "payload": {}}
    return {"ok": True, "reason": "runtime_status_read", "payload": dict(payload)}


def _read_approval(
    approval_reader: Callable[[], object],
) -> tuple[dict[str, object], str]:
    try:
        raw_approval = approval_reader()
    except Exception as error:
        return {}, f"approval read failed: {type(error).__name__}: {error}"
    return parse_approval_record(raw_approval)


def _process_identity_key(result: Mapping[str, object]) -> tuple[int, str]:
    evidence = _observed(result)
    try:
        process_id = int(evidence.get("intended_lavi_pid") or -1)
    except (TypeError, ValueError):
        process_id = -1
    return (
        process_id,
        str(evidence.get("intended_lavi_creation_date") or "").strip(),
    )


def _observed(result: Mapping[str, object]) -> dict[str, object]:
    observed = result.get("observed")
    return dict(observed) if isinstance(observed, Mapping) else {}


def _failure(
    stage: str,
    reason: str,
    observed: Mapping[str, object] | None = None,
) -> dict[str, object]:
    return preflight_decision(
        status="fail",
        selected_mutating_run=True,
        stage=stage,
        reason=reason,
        observed=observed,
    )
