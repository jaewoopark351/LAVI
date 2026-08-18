#20260818_kpopmodder: Compose one fail-closed preflight, one submit, and bounded observation.
from __future__ import annotations

from typing import Callable, Mapping

from .observation.live_run_observation import (
    new_live_run_observation,
)
from .observation.terminal_result_observer import (
    observe_terminal_result,
)
from .preflight.preflight_runner import (
    run_live_runtime_preflight,
)
from .preflight.command_fingerprint import command_fingerprint
from .preflight.preflight_decision import preflight_decision
from .submission.one_shot_command_submission import (
    submit_command_once,
)
from .submission.one_shot_run_guard import OneShotRunGuard
from .submission.reconciliation_requirement_recorder import (
    ReconciliationRequirementRecorder,
)


def run_supervised_live_command(
    environment: Mapping[str, object],
    gateway: object,
    *,
    process_probe: Callable[..., dict[str, object]] | None = None,
    log_identity_inspector: Callable[..., dict[str, object]] | None = None,
    terminal_observer: Callable[..., dict[str, object]] = observe_terminal_result,
    run_guard: object | None = None,
    reconciliation_recorder: object | None = None,
) -> dict[str, object]:
    preflight_kwargs: dict[str, object] = {"status_reader": gateway.read_status}
    if process_probe is not None:
        preflight_kwargs["process_probe"] = process_probe
    if log_identity_inspector is not None:
        preflight_kwargs["log_identity_inspector"] = log_identity_inspector
    preflight = run_live_runtime_preflight(environment, **preflight_kwargs)
    if preflight.get("status") != "ok":
        return {
            "preflight": preflight,
            "observation": new_live_run_observation(),
        }
    guard = run_guard or OneShotRunGuard(str(environment["repository_root"]))
    recorder = reconciliation_recorder or ReconciliationRequirementRecorder(
        str(environment["repository_root"])
    )
    approved_command_fingerprint = command_fingerprint(environment.get("command"))
    try:
        guard_result = guard.claim(
            str(environment["invocation_id"]),
            approved_command_fingerprint,
        )
    except Exception as error:
        guard_result = {
            "ok": False,
            "reason": f"one-shot guard failed: {type(error).__name__}: {error}",
        }
    if not guard_result.get("ok"):
        return {
            "preflight": preflight_decision(
                status="fail",
                selected_mutating_run=True,
                stage="pre_submit_recheck",
                reason=str(guard_result.get("reason") or "one-shot claim failed"),
                observed=preflight.get("observed"),
            ),
            "observation": new_live_run_observation(),
        }
    observation = submit_command_once(gateway, str(environment["command"]))
    if observation.get("gradio_submit_call_count") != 1:
        observation["reconciliation_required"] = True
        recorder.record(
            str(environment["invocation_id"]),
            approved_command_fingerprint,
            "unexpected_gradio_submit_call_count",
        )
        return {"preflight": preflight, "observation": observation}
    if observation.get("submission_outcome") == "accepted":
        observation = terminal_observer(
            gateway,
            observation,
            timeout_sec=float(environment["timeout_sec"]),
            poll_sec=float(environment["poll_sec"]),
        )
    if (
        observation.get("submission_outcome") == "accepted"
        and observation.get("gameplay_observation_complete") is not True
    ):
        observation["reconciliation_required"] = True
    if observation.get("reconciliation_required") is True:
        recorder.record(
            str(environment["invocation_id"]),
            approved_command_fingerprint,
            _reconciliation_reason(observation),
        )
    return {"preflight": preflight, "observation": observation}


def _reconciliation_reason(observation: Mapping[str, object]) -> str:
    if observation.get("submission_outcome") == "submission_outcome_unknown":
        return "submission_outcome_unknown"
    if observation.get("observer_timeout") is True:
        return "observer_timeout"
    if observation.get("terminal_status") == "unknown":
        return "runtime_terminal_unknown"
    if observation.get("active_request_clear") is False:
        return "active_request_not_clear"
    return "gameplay_observation_incomplete_or_effect_requires_review"
