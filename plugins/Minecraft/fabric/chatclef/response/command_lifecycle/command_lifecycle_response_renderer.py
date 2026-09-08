#20260907_kpopmodder: Select deterministic lifecycle phrases without reading mutable command state.
from __future__ import annotations

from types import SimpleNamespace

from .grammar.korean_command_phrase_renderer import KoreanCommandPhraseRenderer
from .profiles.command_phrase_profile_registry import CommandPhraseProfileRegistry


class CommandLifecycleResponseRenderer:
    def __init__(self, *, profile_registry=None, phrase_renderer=None) -> None:
        self._profiles = profile_registry or CommandPhraseProfileRegistry()
        self._phrases = phrase_renderer or KoreanCommandPhraseRenderer()

    def render_start(self, descriptor: object) -> str:
        profile = self._profiles.profile(getattr(descriptor, "command_name", ""))
        return self._phrases.start(descriptor, profile)

    def render_status(self, snapshot: object, query: object = None) -> str:
        descriptor = getattr(snapshot, "descriptor", None)
        state = str(getattr(snapshot, "state", "unavailable") or "unavailable")
        if (
            state == "idle"
            and getattr(snapshot, "owner_present", False) is False
            and getattr(snapshot, "availability_reason", "")
            == "no_tracked_owner"
        ):
            return "지금 내가 처리 중인 마인크래프트 명령은 없어"
        if descriptor is None and getattr(snapshot, "target_item", None):
            descriptor = SimpleNamespace(
                command_name="get",
                command="get diamond_pickaxe 1",
                requested_family="item_get",
                target_item=getattr(snapshot, "target_item", None),
                requested_count=getattr(snapshot, "requested_count", None),
                spoken_target_label="다이아 곡괭이",
                acquisition_verb_class="craft",
                detail_level="typed",
            )
        if descriptor is None:
            family = str(getattr(query, "requested_family", "") or "")
            if family == "item_get":
                return "지금 제작 상태를 확인하지 못했어"
            return "지금 마인크래프트 작업 상태를 확인하지 못했어"
        profile = self._profiles.profile(getattr(descriptor, "command_name", ""))
        return self._phrases.status(descriptor, profile, state)

    def render_terminal(self, fact: object) -> str:
        descriptor = getattr(fact, "descriptor", None)
        profile = self._profiles.profile(getattr(descriptor, "command_name", ""))
        return self._phrases.terminal(
            descriptor,
            profile,
            status=str(getattr(fact, "status", "unknown") or "unknown"),
            verified=getattr(fact, "verified", False) is True,
            dispatch_started=getattr(fact, "dispatch_started", False) is True,
            evidence_projection=getattr(fact, "evidence_projection", None),
        )


__all__ = ("CommandLifecycleResponseRenderer",)
