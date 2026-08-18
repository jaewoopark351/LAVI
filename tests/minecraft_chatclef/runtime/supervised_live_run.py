#20260818_kpopmodder: Compose one fail-closed preflight, one submit, and bounded observation.
from __future__ import annotations

from typing import Callable, Mapping

from .observation.live_run_observation import (
    new_live_run_observation,
)
from .observation.terminal_result_observer import (
    observe_terminal_result,
)
from .preflight.approved_live_run_ticket import ApprovedLiveRunTicket
from .preflight.command_fingerprint import command_fingerprint
from .preflight.gateway_endpoint_identity import inspect_gateway_endpoint
from .preflight.preflight_decision import preflight_decision
from .preflight.preflight_runner import (
    run_live_runtime_preflight,
)
from .preflight.pre_submit_revalidator import revalidate_pre_submit
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
    candidate_command = _text(environment.get("command"))
    candidate_invocation_id = _text(environment.get("invocation_id"))
    candidate_approval_json = _exact_text(environment.get("approval_json"))
    candidate_repository_root = _text(environment.get("repository_root"))
    candidate_expected_backend = _text(environment.get("expected_backend"))
    candidate_expected_instance = _text(environment.get("expected_instance"))
    candidate_expected_world = _text(environment.get("expected_world"))
    preflight_kwargs: dict[str, object] = {
        "status_reader": lambda: gateway.read_status()
    }
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
    gateway_endpoint = inspect_gateway_endpoint(gateway)
    if not gateway_endpoint.get("ok"):
        return _preflight_failure(
            "endpoint",
            str(gateway_endpoint.get("reason") or "gateway endpoint failed"),
            _mapping(preflight.get("observed")),
        )
    observed = _mapping(preflight.get("observed"))
    if gateway_endpoint.get("url") != observed.get("gradio_url"):
        return _preflight_failure(
            "endpoint",
            "live gateway endpoint does not match the approved endpoint",
            observed,
        )
    ticket = ApprovedLiveRunTicket(
        command=candidate_command,
        command_fingerprint=command_fingerprint(candidate_command),
        gradio_url=_text(observed.get("gradio_url")),
        expected_backend=candidate_expected_backend,
        expected_instance=candidate_expected_instance,
        expected_world=candidate_expected_world,
        invocation_id=candidate_invocation_id,
        approval_json=candidate_approval_json,
        repository_root=candidate_repository_root,
        process_identity_fingerprint=_text(
            observed.get("process_identity_fingerprint")
        ),
    )
    guard = run_guard or OneShotRunGuard(ticket.repository_root)
    recorder = reconciliation_recorder or ReconciliationRequirementRecorder(
        ticket.repository_root
    )
    try:
        guard_result = guard.claim(
            ticket.invocation_id,
            ticket.command_fingerprint,
        )
    except Exception as error:
        guard_result = {
            "ok": False,
            "reason": f"one-shot guard failed: {type(error).__name__}: {error}",
        }
    if not isinstance(guard_result, Mapping) or guard_result.get("ok") is not True:
        guard_reason = (
            str(guard_result.get("reason") or "one-shot claim failed")
            if isinstance(guard_result, Mapping)
            else "one-shot claim result is invalid"
        )
        return {
            "preflight": preflight_decision(
                status="fail",
                selected_mutating_run=True,
                stage="pre_submit_recheck",
                reason=guard_reason,
                observed=preflight.get("observed"),
            ),
            "observation": new_live_run_observation(),
        }
    revalidation = revalidate_pre_submit(
        ticket,
        environment,
        status_reader=gateway.read_status,
        process_probe=process_probe,
        log_identity_inspector=log_identity_inspector,
    )
    if revalidation.get("status") != "ok":
        return {
            "preflight": revalidation,
            "observation": new_live_run_observation(),
        }
    final_gateway_endpoint = inspect_gateway_endpoint(gateway)
    if not final_gateway_endpoint.get("ok"):
        return _preflight_failure(
            "pre_submit_recheck",
            str(
                final_gateway_endpoint.get("reason")
                or "live gateway endpoint is unavailable before submit"
            ),
            _mapping(revalidation.get("observed")),
        )
    if final_gateway_endpoint.get("url") != ticket.gradio_url:
        return _preflight_failure(
            "pre_submit_recheck",
            "live gateway endpoint changed before submit",
            _mapping(revalidation.get("observed")),
        )
    preflight = revalidation
    observation = submit_command_once(gateway, ticket.command)
    if observation.get("gradio_submit_call_count") != 1:
        observation["reconciliation_required"] = True
        recorder.record(
            ticket.invocation_id,
            ticket.command_fingerprint,
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
            ticket.invocation_id,
            ticket.command_fingerprint,
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


def _preflight_failure(
    stage: str,
    reason: str,
    observed: Mapping[str, object] | None = None,
) -> dict[str, object]:
    return {
        "preflight": preflight_decision(
            status="fail",
            selected_mutating_run=True,
            stage=stage,
            reason=reason,
            observed=observed,
        ),
        "observation": new_live_run_observation(),
    }


def _mapping(value: object) -> dict[str, object]:
    return dict(value) if isinstance(value, Mapping) else {}


def _text(value: object) -> str:
    return value.strip() if type(value) is str else ""


def _exact_text(value: object) -> str:
    return value if type(value) is str else ""
