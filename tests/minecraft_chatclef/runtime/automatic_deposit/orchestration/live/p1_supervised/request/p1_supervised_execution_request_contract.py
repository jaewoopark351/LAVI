#20260901_kpopmodder: Validate exact P1 request, approval, external manifest, and runner environment bindings.
from __future__ import annotations

from collections.abc import Mapping

from minecraft_chatclef.runtime.preflight.approval_record import (
    parse_approval_record,
    validate_approval_record,
)
from minecraft_chatclef.runtime.preflight.command_transport import (
    validate_approved_command_transport,
)
from minecraft_chatclef.runtime.submission.command_submission_transport import (
    CommandSubmissionTransport,
)

from .p1_supervised_execution_request import P1SupervisedExecutionRequest


def verify_p1_supervised_execution_request(
    request: object,
    runner_environment: object,
) -> str:
    if not isinstance(request, P1SupervisedExecutionRequest):
        return "P1_SUPERVISED_EXECUTION_REQUEST_NOT_TYPED"
    if request.command != "@store_home":
        return "P1_SUPERVISED_COMMAND_NOT_EXACT"
    if request.transport is not CommandSubmissionTransport.RAW:
        return "P1_SUPERVISED_TRANSPORT_NOT_RAW"
    if not _identity(request.expected_run_manifest_id):
        return "P1_SUPERVISED_RUN_MANIFEST_ID_MISSING"
    if not _identity(request.expected_candidate_position):
        return "P1_SUPERVISED_CANDIDATE_POSITION_INVALID"
    if not isinstance(runner_environment, Mapping):
        return "P1_SUPERVISED_RUNNER_ENVIRONMENT_INVALID"
    expected = {
        "command": request.command,
        "gradio_url": request.gradio_url,
        "backend": request.expected_backend,
        "instance": request.expected_instance,
        "world": request.expected_world,
        "invocation_id": request.invocation_id,
        "transport": request.transport.value,
    }
    environment_fields = {
        "command": runner_environment.get("command"),
        "gradio_url": runner_environment.get("gradio_url"),
        "backend": runner_environment.get("expected_backend"),
        "instance": runner_environment.get("expected_instance"),
        "world": runner_environment.get("expected_world"),
        "invocation_id": runner_environment.get("invocation_id"),
        "transport": runner_environment.get("transport"),
    }
    if environment_fields != expected:
        return "P1_SUPERVISED_RUNNER_ENVIRONMENT_MISMATCH"
    if runner_environment.get("approval_json") != request.approval_json:
        return "P1_SUPERVISED_APPROVAL_JSON_MISMATCH"
    if runner_environment.get("repository_root") != request.repository_root:
        return "P1_SUPERVISED_REPOSITORY_ROOT_MISMATCH"

    approval, parse_error = parse_approval_record(request.approval_json)
    if parse_error:
        return "P1_SUPERVISED_APPROVAL_INVALID"
    approved_transport = approval.get("transport")
    transport_error = validate_approved_command_transport(
        approved_transport,
        request.transport,
    )
    if transport_error:
        return "P1_SUPERVISED_APPROVAL_TRANSPORT_MISMATCH"
    approval_error = validate_approval_record(approval, expected)
    if approval_error:
        if approval_error == "approval field mismatch: transport":
            return "P1_SUPERVISED_APPROVAL_TRANSPORT_MISMATCH"
        return "P1_SUPERVISED_APPROVAL_INVALID"
    approved_manifest_id = approval.get("run_manifest_id")
    if not _identity(approved_manifest_id):
        return "P1_SUPERVISED_APPROVAL_RUN_MANIFEST_ID_MISSING"
    if approved_manifest_id != request.expected_run_manifest_id:
        return "P1_SUPERVISED_APPROVAL_RUN_MANIFEST_ID_MISMATCH"
    return ""


def _identity(value: object) -> bool:
    return (
        type(value) is str
        and value == value.strip()
        and 0 < len(value) <= 512
        and value.casefold() not in {"none", "null", "unavailable", "unverified"}
        and not any(
            ord(character) < 0x20 or ord(character) == 0x7F
            for character in value
        )
    )
