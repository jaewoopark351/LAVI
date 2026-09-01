#20260901_kpopmodder: Validate one sealed supervised execution result before evidence assembly.
from __future__ import annotations

from minecraft_chatclef.runtime.automatic_deposit.orchestration.live.p1_supervised.execution.p1_supervised_execution_result import (
    P1SupervisedExecutionResult,
)


def p1_supervised_execution_result_error(
    execution: P1SupervisedExecutionResult,
) -> str:
    if execution.result_error:
        return "P1_SUPERVISED_EXECUTION_ERROR"
    if execution.preflight_status != "ok":
        return "P1_SUPERVISED_PREFLIGHT_NOT_OK"
    if execution.submission_outcome != "accepted":
        return "P1_SUPERVISED_SUBMISSION_NOT_ACCEPTED"
    if execution.submit_call_count != 1:
        return "P1_SUPERVISED_SUBMIT_COUNT_NOT_ONE"
    if execution.adapter_command_request_count not in {"unknown", 1}:
        return "P1_SUPERVISED_ADAPTER_REQUEST_COUNT_VIOLATION"
    if execution.automatic_resubmit_count != 0:
        return "P1_SUPERVISED_AUTOMATIC_RESUBMIT_OBSERVED"
    if execution.automatic_rerun_count != 0:
        return "P1_SUPERVISED_AUTOMATIC_RERUN_OBSERVED"
    if not execution.submitted_request_id:
        return "P1_SUPERVISED_SUBMITTED_REQUEST_ID_MISSING"
    if execution.observer_timeout is not False:
        return "P1_SUPERVISED_OBSERVER_TIMEOUT"
    if execution.connection_state_verified is not True:
        return "P1_SUPERVISED_RUNTIME_CONNECTION_NOT_VERIFIED"
    if execution.connection_state_error != "":
        return "P1_SUPERVISED_RUNTIME_CONNECTION_ERROR"
    if execution.terminal_lifecycle_observed is not True:
        return "P1_SUPERVISED_TERMINAL_LIFECYCLE_NOT_OBSERVED"
    if execution.terminal_request_id != execution.submitted_request_id:
        return "P1_SUPERVISED_TERMINAL_REQUEST_ID_MISMATCH"
    if execution.terminal_status != "completed":
        return "P1_SUPERVISED_TERMINAL_STATUS_NOT_COMPLETED"
    if execution.active_request_clear is not True:
        return "P1_SUPERVISED_ACTIVE_REQUEST_NOT_CLEAR"
    if execution.active_clear_observation != "same_snapshot":
        return "P1_SUPERVISED_ACTIVE_CLEAR_NOT_SAME_SNAPSHOT"
    if execution.runtime_reported_completion is not True:
        return "P1_SUPERVISED_RUNTIME_COMPLETION_NOT_VERIFIED"
    return ""
