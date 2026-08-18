#20260818_kpopmodder: Compose the approved four-step supervised live batch.
from __future__ import annotations

from collections.abc import Callable, Mapping

from ..preflight.runtime_opt_in import live_mutating_run_selected
from ..submission.gradio_runtime_gateway import LaviGradioRuntimeGateway
from ..supervised_live_run import run_supervised_live_command
from .batch_approval_parser import parse_batch_approval_record
from .batch_approval_record import validate_batch_approval_record
from .batch_environment_builder import build_batch_command_environments
from .batch_run_result import new_batch_run_result
from .get_item_expectation_attacher import attach_get_item_expectations
from .gameplay_oracle.get_item_inventory_baseline import (
    capture_get_item_inventory_baseline,
)
from .gameplay_oracle.post_terminal_get_item_checkpoint import (
    collect_post_terminal_get_item_checkpoint,
)
from .gameplay_test_objective import validate_gameplay_test_objective
from .live_get_batch_plan import COMMANDS
from .one_shot_guard_reconciliation import reconcile_batch_one_shot_guard
from .supervised_batch_runner import run_supervised_live_batch


def run_live_get_batch_application(
    base_environment: Mapping[str, object],
    raw_batch_approval: object,
    *,
    gateway_factory: Callable[[str], object] = LaviGradioRuntimeGateway,
    command_runner: Callable[[Mapping[str, object], object], object] = (
        run_supervised_live_command
    ),
    baseline_provider: Callable[[Mapping[str, object]], object] = (
        capture_get_item_inventory_baseline
    ),
    checkpoint_provider: Callable[
        [Mapping[str, object], Mapping[str, object], object],
        object,
    ] = collect_post_terminal_get_item_checkpoint,
    reconcile_guard: Callable[
        [Mapping[str, object], Mapping[str, object]],
        object,
    ] = reconcile_batch_one_shot_guard,
) -> dict[str, object]:
    if not live_mutating_run_selected(base_environment):
        return _setup_result("skipped", "live_and_mutating_opt_ins_required")
    objective_error = validate_gameplay_test_objective(
        base_environment.get("gameplay_test_objective")
    )
    if objective_error:
        return _setup_result("stopped", objective_error)
    approval, approval_error = parse_batch_approval_record(raw_batch_approval)
    if approval_error:
        return _setup_result("stopped", approval_error)
    approved_steps, approval_error = validate_batch_approval_record(
        approval,
        COMMANDS,
        base_environment,
    )
    if approval_error:
        return _setup_result("stopped", approval_error)
    approved_steps_with_expectations, plan_error = attach_get_item_expectations(
        approved_steps
    )
    if plan_error:
        return _setup_result("stopped", plan_error)
    environments = build_batch_command_environments(
        base_environment,
        approval,
        approved_steps_with_expectations,
    )

    def execute_command(environment: Mapping[str, object]) -> object:
        gateway = gateway_factory(str(environment.get("gradio_url") or ""))
        return command_runner(environment, gateway)

    return run_supervised_live_batch(
        environments,
        execute_command=execute_command,
        baseline_provider=baseline_provider,
        checkpoint_provider=checkpoint_provider,
        reconcile_guard=reconcile_guard,
    )


def _setup_result(status: str, reason: str) -> dict[str, object]:
    result = new_batch_run_result(len(COMMANDS))
    result["status"] = status
    result["stop_reason"] = reason
    return result
