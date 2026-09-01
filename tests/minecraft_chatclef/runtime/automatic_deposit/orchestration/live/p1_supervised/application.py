#20260901_kpopmodder: Compose guarded pre-submit, one RAW execution, post-submit evidence, and exact reconciliation.
from __future__ import annotations

from minecraft_chatclef.runtime.automatic_deposit.evidence.assembly.store_home.supervised.p1_supervised_verified_evidence_assembly import (
    build_p1_supervised_verified_evidence,
)
from minecraft_chatclef.runtime.automatic_deposit.evidence.assembly.store_home.supervised.p1_supervised_verified_evidence_result import (
    P1SupervisedVerifiedEvidenceResult,
    inconclusive_p1_supervised_evidence,
    with_p1_supervised_reconciliation,
)
from minecraft_chatclef.runtime.preflight.command_fingerprint import (
    command_fingerprint,
)

from ..p1_guard_gate import evaluate_p1_guard_gate
from .execution.p1_supervised_command_executor import (
    execute_p1_supervised_command,
)
from .execution.p1_supervised_execution_dependencies import (
    P1SupervisedExecutionDependencies,
)
from .post_submit.p1_supervised_post_submit_dependencies import (
    P1SupervisedPostSubmitDependencies,
)
from .post_submit.p1_supervised_gameplay_observer import (
    observe_p1_supervised_gameplay,
)
from .post_submit.p1_supervised_post_submit_observer import (
    observe_p1_supervised_post_submit_log,
)
from .post_submit.p1_supervised_reconciliation import (
    reconcile_verified_p1_supervised_run,
)
from .pre_submit.p1_supervised_pre_submit_dependencies import (
    P1SupervisedPreSubmitDependencies,
)
from .pre_submit.p1_supervised_pre_submit_gate import (
    verify_p1_supervised_pre_submit_evidence,
)
from .pre_submit.p1_supervised_pre_submit_observer import (
    observe_p1_supervised_pre_submit_evidence,
)
from .request.p1_supervised_execution_request import (
    P1SupervisedExecutionRequest,
)
from .request.p1_supervised_execution_request_contract import (
    verify_p1_supervised_execution_request,
)


def run_p1_supervised_live_application(
    request: object,
    runner_environment: object,
    *,
    guard_state_reader: object,
    pre_submit_dependencies: object,
    execution_dependencies: object,
    post_submit_dependencies: object,
) -> P1SupervisedVerifiedEvidenceResult:
    repository_root = (
        request.repository_root
        if isinstance(request, P1SupervisedExecutionRequest)
        else ""
    )
    if not callable(guard_state_reader):
        return inconclusive_p1_supervised_evidence(
            "LIVE_ONE_SHOT_GUARD_STATE_UNREADABLE"
        )
    guard = evaluate_p1_guard_gate(repository_root, guard_state_reader)
    if not guard.clear:
        return inconclusive_p1_supervised_evidence(guard.reason)

    request_error = verify_p1_supervised_execution_request(
        request,
        runner_environment,
    )
    if request_error:
        return inconclusive_p1_supervised_evidence(request_error)
    if not isinstance(post_submit_dependencies, P1SupervisedPostSubmitDependencies):
        return inconclusive_p1_supervised_evidence(
            "P1_SUPERVISED_POST_SUBMIT_DEPENDENCIES_NOT_TYPED"
        )
    if not callable(post_submit_dependencies.log_delta_reader):
        return inconclusive_p1_supervised_evidence(
            "P1_SUPERVISED_POST_SUBMIT_READER_NOT_CALLABLE"
        )
    if not callable(post_submit_dependencies.gameplay_observation_reader):
        return inconclusive_p1_supervised_evidence(
            "P1_SUPERVISED_GAMEPLAY_READER_NOT_CALLABLE"
        )
    if not callable(post_submit_dependencies.reconciler):
        return inconclusive_p1_supervised_evidence(
            "P1_SUPERVISED_RECONCILER_NOT_CALLABLE"
        )
    if not isinstance(
        pre_submit_dependencies,
        P1SupervisedPreSubmitDependencies,
    ):
        return inconclusive_p1_supervised_evidence(
            "P1_SUPERVISED_PRE_SUBMIT_DEPENDENCIES_NOT_TYPED"
        )
    runtime_artifact, runtime_reason = observe_p1_supervised_pre_submit_evidence(
        pre_submit_dependencies.runtime_artifact_reader
    )
    if runtime_artifact is None:
        return inconclusive_p1_supervised_evidence(
            "P1_SUPERVISED_RUNTIME_ARTIFACT_NOT_VERIFIED"
        )
    trusted_destination, trusted_reason = observe_p1_supervised_pre_submit_evidence(
        pre_submit_dependencies.trusted_destination_reader
    )
    pre_submit_error = verify_p1_supervised_pre_submit_evidence(
        request,
        runtime_artifact,
        runtime_reason,
        trusted_destination,
        trusted_reason,
    )
    if pre_submit_error:
        return inconclusive_p1_supervised_evidence(pre_submit_error)
    if not isinstance(execution_dependencies, P1SupervisedExecutionDependencies):
        return inconclusive_p1_supervised_evidence(
            "P1_SUPERVISED_EXECUTION_DEPENDENCIES_NOT_TYPED"
        )
    execution = execute_p1_supervised_command(
        request,
        runner_environment,
        execution_dependencies,
    )
    if execution.submission_outcome != "accepted":
        return build_p1_supervised_verified_evidence(
            execution,
            None,
            runtime_artifact=runtime_artifact,
            live_fixture=trusted_destination,
            gameplay_observation=None,
            expected_run_manifest_id=request.expected_run_manifest_id,
        )
    log_delta, log_reason = observe_p1_supervised_post_submit_log(
        post_submit_dependencies.log_delta_reader,
        execution,
    )
    if log_delta is None:
        return inconclusive_p1_supervised_evidence(
            log_reason or "P1_SUPERVISED_POST_SUBMIT_LOG_NOT_VERIFIED",
            submit_call_count=max(execution.submit_call_count, 0),
            automatic_resubmit_count=max(execution.automatic_resubmit_count, 0),
            submitted_request_id=execution.submitted_request_id,
        )
    gameplay_observation, gameplay_reason = observe_p1_supervised_gameplay(
        post_submit_dependencies.gameplay_observation_reader,
        execution,
        log_delta,
    )
    if gameplay_observation is None:
        return inconclusive_p1_supervised_evidence(
            gameplay_reason or "P1_SUPERVISED_GAMEPLAY_OBSERVATION_NOT_VERIFIED",
            submit_call_count=max(execution.submit_call_count, 0),
            automatic_resubmit_count=max(execution.automatic_resubmit_count, 0),
            submitted_request_id=execution.submitted_request_id,
        )
    result = build_p1_supervised_verified_evidence(
        execution,
        log_delta,
        runtime_artifact=runtime_artifact,
        live_fixture=trusted_destination,
        gameplay_observation=gameplay_observation,
        expected_run_manifest_id=request.expected_run_manifest_id,
    )
    if not result.ok:
        return result
    reconciliation = reconcile_verified_p1_supervised_run(
        post_submit_dependencies.reconciler,
        request.invocation_id,
        command_fingerprint(request.command),
    )
    if reconciliation.get("ok") is not True:
        return inconclusive_p1_supervised_evidence(
            "P1_SUPERVISED_RECONCILIATION_FAILED",
            submit_call_count=result.submit_call_count,
            automatic_resubmit_count=result.automatic_resubmit_count,
            submitted_request_id=result.submitted_request_id,
            operation_id=result.operation_id,
        )
    return with_p1_supervised_reconciliation(result)
