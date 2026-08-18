#20260819_kpopmodder: Revalidate the immutable approved ticket immediately before one live submit.
from __future__ import annotations

from collections.abc import Callable, Mapping

from .approved_live_run_ticket import ApprovedLiveRunTicket
from .command_fingerprint import command_fingerprint
from .preflight_decision import preflight_decision
from .preflight_runner import run_live_runtime_preflight


def revalidate_pre_submit(
    ticket: ApprovedLiveRunTicket,
    environment: Mapping[str, object],
    *,
    status_reader: Callable[[], dict[str, object]],
    process_probe: Callable[..., dict[str, object]] | None = None,
    log_identity_inspector: Callable[..., dict[str, object]] | None = None,
) -> dict[str, object]:
    preflight_kwargs: dict[str, object] = {"status_reader": status_reader}
    if process_probe is not None:
        preflight_kwargs["process_probe"] = process_probe
    if log_identity_inspector is not None:
        preflight_kwargs["log_identity_inspector"] = log_identity_inspector
    decision = run_live_runtime_preflight(environment, **preflight_kwargs)
    observed = _mapping(decision.get("observed"))
    if decision.get("status") != "ok":
        return _failure(
            str(decision.get("reason") or "ticket-bound pre-submit recheck failed"),
            observed,
        )

    mismatch = _ticket_mismatch(ticket, environment, observed)
    if mismatch:
        return _failure(mismatch, observed)
    return decision


def _ticket_mismatch(
    ticket: ApprovedLiveRunTicket,
    environment: Mapping[str, object],
    observed: Mapping[str, object],
) -> str:
    current_values = {
        "command": _text(environment.get("command")),
        "command_fingerprint": command_fingerprint(environment.get("command")),
        "gradio_url": _text(observed.get("gradio_url")),
        "expected_backend": _text(environment.get("expected_backend")),
        "expected_instance": _text(environment.get("expected_instance")),
        "expected_world": _text(environment.get("expected_world")),
        "invocation_id": _text(environment.get("invocation_id")),
        "approval_json": _exact_text(environment.get("approval_json")),
        "repository_root": _text(environment.get("repository_root")),
        "process_identity_fingerprint": _text(
            observed.get("process_identity_fingerprint")
        ),
    }
    approved_values = {
        "command": ticket.command,
        "command_fingerprint": ticket.command_fingerprint,
        "gradio_url": ticket.gradio_url,
        "expected_backend": ticket.expected_backend,
        "expected_instance": ticket.expected_instance,
        "expected_world": ticket.expected_world,
        "invocation_id": ticket.invocation_id,
        "approval_json": ticket.approval_json,
        "repository_root": ticket.repository_root,
        "process_identity_fingerprint": ticket.process_identity_fingerprint,
    }
    for field, approved in approved_values.items():
        if current_values[field] != approved:
            return f"approved live-run ticket changed: {field}"
    if observed.get("approval_tuple_verified") is not True:
        return "approved live-run ticket lost approval verification"
    if observed.get("pre_submit_recheck_passed") is not True:
        return "approved live-run ticket lost pre-submit verification"
    if _text(observed.get("backend")) != ticket.expected_backend:
        return "approved live-run ticket changed: observed backend"
    if _text(observed.get("instance")) != ticket.expected_instance:
        return "approved live-run ticket changed: observed instance"
    if _text(observed.get("world")) != ticket.expected_world:
        return "approved live-run ticket changed: observed world"
    if _text(observed.get("approved_command_fingerprint")) != (
        ticket.command_fingerprint
    ):
        return "approved live-run ticket changed: observed command fingerprint"
    return ""


def _failure(
    reason: str,
    observed: Mapping[str, object],
) -> dict[str, object]:
    return preflight_decision(
        status="fail",
        selected_mutating_run=True,
        stage="pre_submit_recheck",
        reason=reason,
        observed=observed,
    )


def _mapping(value: object) -> dict[str, object]:
    return dict(value) if isinstance(value, Mapping) else {}


def _text(value: object) -> str:
    return value.strip() if type(value) is str else ""


def _exact_text(value: object) -> str:
    return value if type(value) is str else ""
