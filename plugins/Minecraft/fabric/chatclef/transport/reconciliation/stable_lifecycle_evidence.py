#20260821_kpopmodder: Strictly parse Java running evidence before any stale-active reconciliation decision.
from __future__ import annotations

from dataclasses import dataclass
from typing import Any, Mapping

from plugins.Minecraft.common.dto.bridge_envelope_dto import BridgeEnvelopeDTO


EXPECTED_VERSION = "2026-08-20.java-nonterminal-evidence.v1"
EXPECTED_STAGE = "stable_request_quiescence_observed"


@dataclass(frozen=True)
class StableLifecycleEvidence:
    sequence: int
    envelope_message_id: str
    fingerprint: tuple[tuple[str, object], ...]
    version: str
    stage: str
    data: dict[str, Any]

    def to_dict(self) -> dict[str, object]:
        return {
            "evidence_sequence": self.sequence,
            "envelope_message_id": self.envelope_message_id,
            "lifecycle_evidence_version": self.version,
            "lifecycle_evidence_stage": self.stage,
            "fingerprint": dict(self.fingerprint),
        }


@dataclass(frozen=True)
class StableLifecycleEvidenceParse:
    accepted: bool
    reason: str
    evidence: StableLifecycleEvidence | None = None


class StableLifecycleEvidenceParser:
    def parse(
        self,
        *,
        envelope: BridgeEnvelopeDTO,
        raw_payload: Mapping[str, Any],
    ) -> StableLifecycleEvidenceParse:
        if type(raw_payload.get("request_id")) is not str:
            return self._blocked("raw_request_id_not_str")
        if raw_payload.get("ok") is not True:
            return self._blocked("raw_ok_not_true_bool")
        if raw_payload.get("status") != "running":
            return self._blocked("raw_status_not_running")
        raw_data = raw_payload.get("data")
        if not isinstance(raw_data, Mapping):
            return self._blocked("raw_data_not_mapping")
        data = dict(raw_data)
        lifecycle = data.get("lifecycle_evidence")
        lifecycle_data = dict(lifecycle) if isinstance(lifecycle, Mapping) else {}
        sequence = _first_exact_int(
            lifecycle_data,
            data,
            keys=("evidence_sequence", "sequence"),
        )
        if sequence is None:
            return self._blocked("evidence_sequence_not_int")
        dispatch_returned = data.get("dispatch_returned")
        if dispatch_returned is not True:
            return self._blocked("dispatch_returned_not_true_bool")
        finish_callback = data.get("finish_callback_received")
        if finish_callback is not True:
            return self._blocked("finish_callback_received_not_true_bool")
        task_finished = data.get("task_finished_event_received")
        if task_finished is not False:
            return self._blocked("task_finished_event_received_not_false_bool")
        waiting_reason = data.get("waiting_reason")
        if waiting_reason != "waiting_for_task_finished_event":
            return self._blocked("waiting_reason_not_waiting_for_task_finished_event")
        version = _first_text(lifecycle_data, data, keys=("version", "lifecycle_evidence_version"))
        if version != EXPECTED_VERSION:
            return self._blocked("unknown_evidence_version")
        stage = _first_text(lifecycle_data, data, keys=("stage", "lifecycle_evidence_stage"))
        if stage != EXPECTED_STAGE:
            return self._blocked("unknown_evidence_stage")
        if data.get("classification") != "nonterminal_diagnostic":
            return self._blocked("classification_not_nonterminal_diagnostic")
        if data.get("terminal_status") != "NONE":
            return self._blocked("terminal_status_not_none")
        if _first_text(lifecycle_data, data, keys=("gameplay_effect",)) != "UNVERIFIED":
            return self._blocked("gameplay_effect_not_unverified")
        stable = data.get("stable_request_quiescence")
        if not isinstance(stable, Mapping):
            stable = data.get("stable_quiescence")
        if not isinstance(stable, Mapping):
            return self._blocked("stable_quiescence_not_mapping")
        if stable.get("qualified") is not True:
            return self._blocked("stable_quiescence_not_qualified")
        if stable.get("same_session_generation") is not True:
            return self._blocked("stable_quiescence_session_generation_not_true")
        if stable.get("request_root_reappeared") is not False:
            return self._blocked("request_root_reappeared_not_false")
        if not envelope.message_id:
            return self._blocked("envelope_message_id_blank")
        fingerprint = _fingerprint(
            data=data,
            lifecycle_data=lifecycle_data,
            stable=stable,
            version=version,
            stage=stage,
        )
        return StableLifecycleEvidenceParse(
            accepted=True,
            reason="accepted",
            evidence=StableLifecycleEvidence(
                sequence=sequence,
                envelope_message_id=envelope.message_id,
                fingerprint=fingerprint,
                version=version,
                stage=stage,
                data=data,
            ),
        )

    def _blocked(self, reason: str) -> StableLifecycleEvidenceParse:
        return StableLifecycleEvidenceParse(accepted=False, reason=reason)


def _first_exact_int(
    primary: Mapping[str, Any],
    secondary: Mapping[str, Any],
    *,
    keys: tuple[str, ...],
) -> int | None:
    for payload in (primary, secondary):
        for key in keys:
            value = payload.get(key)
            if type(value) is int:
                return value
            if value is not None:
                return None
    return None


def _first_text(
    primary: Mapping[str, Any],
    secondary: Mapping[str, Any],
    *,
    keys: tuple[str, ...],
) -> str | None:
    for payload in (primary, secondary):
        for key in keys:
            value = payload.get(key)
            if type(value) is str and value:
                return value
    return None


def _fingerprint(
    *,
    data: Mapping[str, Any],
    lifecycle_data: Mapping[str, Any],
    stable: Mapping[str, Any],
    version: str,
    stage: str,
) -> tuple[tuple[str, object], ...]:
    return tuple(
        sorted(
            {
                "version": version,
                "stage": stage,
                "dispatch_returned": data.get("dispatch_returned"),
                "finish_callback_received": data.get("finish_callback_received"),
                "task_finished_event_received": data.get(
                    "task_finished_event_received"
                ),
                "waiting_reason": data.get("waiting_reason"),
                "classification": data.get("classification"),
                "terminal_status": data.get("terminal_status"),
                "gameplay_effect": _first_text(
                    lifecycle_data,
                    data,
                    keys=("gameplay_effect",),
                ),
                "bound_root": _task_projection(data.get("bound_root_task")),
                "current_root": _task_projection(data.get("current_root_task")),
                "stable_qualified": stable.get("qualified"),
                "same_session_generation": stable.get("same_session_generation"),
                "request_root_reappeared": stable.get("request_root_reappeared"),
            }.items()
        )
    )


def _task_projection(value: Any) -> tuple[tuple[str, object], ...]:
    payload = dict(value) if isinstance(value, Mapping) else {}
    return tuple(
        sorted(
            {
                "class_name": payload.get("class_name")
                or payload.get("task_class")
                or payload.get("taskClass")
                or "unknown",
                "identity": payload.get("identity")
                or payload.get("task_identity")
                or payload.get("taskIdentity")
                or "unknown",
                "relationship": payload.get("relationship")
                or payload.get("ownership_role")
                or "unknown",
            }.items()
        )
    )

