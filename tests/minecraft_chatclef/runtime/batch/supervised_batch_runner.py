#20260818_kpopmodder: Sequence approved one-shots and stop at the first uncertain boundary.
from __future__ import annotations

from collections.abc import Callable, Mapping

from .batch_advancement_policy import batch_advancement_error
from .batch_plan_validator import validate_batch_plan
from .batch_environment_snapshot import batch_environment_snapshot
from .batch_process_identity import (
    batch_process_identity_key,
    bind_expected_process_identity,
)
from .batch_run_result import append_batch_step, new_batch_run_result
from .gameplay_checkpoint import apply_batch_gameplay_checkpoint
from .terminal_checkpoint_gate import terminal_checkpoint_gate_error


def run_supervised_live_batch(
    command_environments: object,
    *,
    execute_command: Callable[[Mapping[str, object]], object],
    baseline_provider: Callable[[Mapping[str, object]], object],
    checkpoint_provider: Callable[
        [Mapping[str, object], Mapping[str, object], object],
        object,
    ],
    reconcile_guard: Callable[
        [Mapping[str, object], Mapping[str, object]],
        object,
    ],
) -> dict[str, object]:
    plan, plan_error = validate_batch_plan(command_environments)
    batch_result = new_batch_run_result(len(plan))
    if plan_error:
        return _stop(batch_result, f"invalid_plan: {plan_error}")
    approved_process_identity: str | None = None
    for environment in plan:
        execution_environment = batch_environment_snapshot(
            bind_expected_process_identity(
                environment,
                approved_process_identity,
            )
        )
        try:
            baseline = baseline_provider(execution_environment)
        except Exception as error:
            append_batch_step(
                batch_result,
                execution_environment,
                None,
                baseline_status="provider_failed",
                checkpoint_status="not_attempted",
                guard_reconciliation_status="not_attempted",
            )
            return _stop(
                batch_result,
                f"baseline_exception: {type(error).__name__}",
            )
        if not isinstance(baseline, Mapping) or baseline.get("ok") is not True:
            append_batch_step(
                batch_result,
                execution_environment,
                None,
                baseline_status="not_verified",
                checkpoint_status="not_attempted",
                guard_reconciliation_status="not_attempted",
            )
            return _stop(batch_result, _baseline_reason(baseline))
        batch_result["attempted_count"] = int(batch_result["attempted_count"]) + 1
        try:
            raw_result = execute_command(execution_environment)
        except Exception as error:
            append_batch_step(
                batch_result,
                execution_environment,
                None,
                baseline_status="verified",
                checkpoint_status="not_attempted",
                guard_reconciliation_status="not_attempted",
            )
            return _stop(
                batch_result,
                f"execution_exception: {type(error).__name__}",
            )
        if not isinstance(raw_result, Mapping):
            append_batch_step(
                batch_result,
                execution_environment,
                None,
                baseline_status="verified",
                checkpoint_status="not_attempted",
                guard_reconciliation_status="not_attempted",
            )
            return _stop(batch_result, "execution_result_invalid")
        command_result = dict(raw_result)
        gate_error = terminal_checkpoint_gate_error(command_result)
        if gate_error:
            append_batch_step(
                batch_result,
                execution_environment,
                command_result,
                baseline_status="verified",
                checkpoint_status="not_attempted",
                guard_reconciliation_status="not_attempted",
            )
            return _stop(batch_result, gate_error)
        current_process_identity = batch_process_identity_key(command_result)
        if approved_process_identity is None:
            approved_process_identity = current_process_identity
        elif current_process_identity != approved_process_identity:
            append_batch_step(
                batch_result,
                execution_environment,
                command_result,
                baseline_status="verified",
                checkpoint_status="not_attempted",
                guard_reconciliation_status="not_attempted",
            )
            return _stop(batch_result, "batch_process_identity_changed")
        try:
            checkpoint = checkpoint_provider(
                execution_environment,
                command_result,
                baseline,
            )
        except Exception as error:
            append_batch_step(
                batch_result,
                execution_environment,
                command_result,
                baseline_status="verified",
                checkpoint_status="provider_failed",
                guard_reconciliation_status="not_attempted",
            )
            return _stop(
                batch_result,
                f"checkpoint_exception: {type(error).__name__}",
            )
        observation = _mapping(command_result.get("observation"))
        checkpoint_result = apply_batch_gameplay_checkpoint(observation, checkpoint)
        command_result["observation"] = observation
        advancement_error = batch_advancement_error(
            command_result,
            checkpoint_result,
        )
        if advancement_error:
            append_batch_step(
                batch_result,
                execution_environment,
                command_result,
                baseline_status="verified",
                checkpoint_status=(
                    "verified"
                    if checkpoint_result.get("ok") is True
                    else "not_verified"
                ),
                guard_reconciliation_status="not_attempted",
            )
            return _stop(batch_result, advancement_error)
        try:
            reconciliation = reconcile_guard(execution_environment, command_result)
        except Exception as error:
            reconciliation = {
                "ok": False,
                "reason": f"guard reconciliation exception: {type(error).__name__}",
            }
        if not _result_ok(reconciliation):
            append_batch_step(
                batch_result,
                execution_environment,
                command_result,
                baseline_status="verified",
                checkpoint_status="verified",
                guard_reconciliation_status="failed",
            )
            return _stop(batch_result, _result_reason(reconciliation))
        observation["reconciliation_required"] = False
        batch_result["completed_count"] = int(batch_result["completed_count"]) + 1
        append_batch_step(
            batch_result,
            execution_environment,
            command_result,
            baseline_status="verified",
            checkpoint_status="verified",
            guard_reconciliation_status="cleared",
        )
    batch_result["status"] = "completed"
    batch_result["stop_reason"] = ""
    return batch_result


def _result_ok(result: object) -> bool:
    if isinstance(result, Mapping):
        return result.get("ok") is True
    return result is True


def _result_reason(result: object) -> str:
    if isinstance(result, Mapping):
        return str(result.get("reason") or "guard_reconciliation_failed")
    return "guard_reconciliation_failed"


def _mapping(value: object) -> dict[str, object]:
    return dict(value) if isinstance(value, Mapping) else {}


def _baseline_reason(baseline: object) -> str:
    if isinstance(baseline, Mapping):
        return str(baseline.get("reason") or "gameplay baseline was not verified")
    return "gameplay baseline result is invalid"


def _stop(batch_result: dict[str, object], reason: str) -> dict[str, object]:
    batch_result["status"] = "stopped"
    batch_result["stop_reason"] = reason
    return batch_result
