#20260818_kpopmodder: Sequence approved one-shots and stop at the first uncertain boundary.
from __future__ import annotations

from collections.abc import Callable, Mapping

from .batch_plan_validator import validate_batch_plan
from .batch_run_result import append_batch_step, new_batch_run_result
from .batch_step_gate import batch_step_gate_error
from .gameplay_checkpoint import apply_batch_gameplay_checkpoint


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
    for environment in plan:
        try:
            baseline = baseline_provider(environment)
        except Exception as error:
            append_batch_step(
                batch_result,
                environment,
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
                environment,
                None,
                baseline_status="not_verified",
                checkpoint_status="not_attempted",
                guard_reconciliation_status="not_attempted",
            )
            return _stop(batch_result, _baseline_reason(baseline))
        batch_result["attempted_count"] = int(batch_result["attempted_count"]) + 1
        try:
            raw_result = execute_command(environment)
        except Exception as error:
            append_batch_step(
                batch_result,
                environment,
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
                environment,
                None,
                baseline_status="verified",
                checkpoint_status="not_attempted",
                guard_reconciliation_status="not_attempted",
            )
            return _stop(batch_result, "execution_result_invalid")
        command_result = dict(raw_result)
        gate_error = batch_step_gate_error(command_result)
        if gate_error:
            append_batch_step(
                batch_result,
                environment,
                command_result,
                baseline_status="verified",
                checkpoint_status="not_attempted",
                guard_reconciliation_status="not_attempted",
            )
            return _stop(batch_result, gate_error)
        try:
            checkpoint = checkpoint_provider(environment, command_result, baseline)
        except Exception as error:
            append_batch_step(
                batch_result,
                environment,
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
        if not checkpoint_result.get("ok"):
            append_batch_step(
                batch_result,
                environment,
                command_result,
                baseline_status="verified",
                checkpoint_status="not_verified",
                guard_reconciliation_status="not_attempted",
            )
            return _stop(
                batch_result,
                str(checkpoint_result.get("reason") or "gameplay checkpoint failed"),
            )
        try:
            reconciliation = reconcile_guard(environment, command_result)
        except Exception as error:
            reconciliation = {
                "ok": False,
                "reason": f"guard reconciliation exception: {type(error).__name__}",
            }
        if not _result_ok(reconciliation):
            append_batch_step(
                batch_result,
                environment,
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
            environment,
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
