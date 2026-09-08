#20260907_kpopmodder: Spend one immutable descriptor capability without owning lifecycle truth.
from __future__ import annotations

import threading

from ..descriptor.command_feedback_descriptor import CommandFeedbackDescriptor


_ISSUANCE_TOKEN = object()


class CommandFeedbackAdmissionGrant:
    __slots__ = ("_descriptor", "_lock", "_state")

    def __init__(
        self,
        *,
        descriptor: CommandFeedbackDescriptor | None = None,
        _issuance_token: object = None,
        **legacy_values: object,
    ) -> None:
        if _issuance_token is not _ISSUANCE_TOKEN:
            raise TypeError("CommandFeedbackAdmissionGrant is admission-issued only")
        if descriptor is None:
            descriptor = self._legacy_descriptor(legacy_values)
        if type(descriptor) is not CommandFeedbackDescriptor:
            raise TypeError("command feedback grant requires an exact descriptor")
        self._descriptor = descriptor
        self._lock = threading.Lock()
        self._state = "issued"

    @classmethod
    def _issue(
        cls,
        *,
        descriptor: CommandFeedbackDescriptor | None = None,
        **legacy_values: object,
    ) -> "CommandFeedbackAdmissionGrant":
        return cls(
            descriptor=descriptor,
            _issuance_token=_ISSUANCE_TOKEN,
            **legacy_values,
        )

    @property
    def descriptor(self) -> CommandFeedbackDescriptor:
        return self._descriptor

    def __getattr__(self, name: str):
        compatibility_names = {
            "command",
            "command_source",
            "acquisition_verb_class",
            "event_id",
            "event_kind",
            "input_source",
            "provider_id",
            "requested_count",
            "spoken_item_label",
            "target_item",
            "intent_kind",
        }
        if name in compatibility_names:
            return getattr(self._descriptor, name)
        raise AttributeError(name)

    def reserve(self) -> bool:
        return self._transition("issued", "reserved")

    def bind(self) -> bool:
        return self._transition("reserved", "bound")

    def abandon_if_reserved(self) -> bool:
        return self._transition("reserved", "retired")

    def retire(self) -> None:
        with self._lock:
            self._state = "retired"

    def _transition(self, expected: str, replacement: str) -> bool:
        with self._lock:
            if self._state != expected:
                return False
            self._state = replacement
            return True

    @staticmethod
    def _legacy_descriptor(values: dict[str, object]) -> CommandFeedbackDescriptor:
        return CommandFeedbackDescriptor(
            command_name="get",
            command=str(values.get("command") or "get diamond_pickaxe 1"),
            command_source=str(values.get("command_source") or "lavi_chat_ui"),
            lifecycle_kind="task",
            phrase_profile_id="get_item_phrase_v1",
            evidence_profile_id="get_terminal_evidence_v1",
            rollout_state="verified",
            event_id=str(values.get("event_id") or ""),
            input_source=str(values.get("input_source") or "lavi_chat_ui"),
            provider_id=str(values.get("provider_id") or "lavi_chat_ui"),
            event_kind=str(values.get("event_kind") or "chat_submit"),
            requested_family="item_get",
            intent_kind=str(values.get("intent_kind") or "get_item"),
            target_item=str(values.get("target_item") or "diamond_pickaxe"),
            requested_count=values.get("requested_count", 1),
            quantity_semantics=CommandFeedbackDescriptor.ACQUIRE_DELTA,
            acquisition_verb_class=str(
                values.get("acquisition_verb_class") or "craft"
            ),
            spoken_target_label=str(
                values.get("spoken_item_label") or "다이아 곡괭이"
            ),
        )

    def __copy__(self):
        raise TypeError("CommandFeedbackAdmissionGrant cannot be copied")

    def __deepcopy__(self, _memo):
        raise TypeError("CommandFeedbackAdmissionGrant cannot be copied")

    def __reduce__(self):
        raise TypeError("CommandFeedbackAdmissionGrant cannot be serialized")

    def __reduce_ex__(self, _protocol):
        raise TypeError("CommandFeedbackAdmissionGrant cannot be serialized")


__all__ = ("CommandFeedbackAdmissionGrant",)
