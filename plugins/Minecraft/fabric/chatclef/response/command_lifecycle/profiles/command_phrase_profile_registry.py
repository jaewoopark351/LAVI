#20260907_kpopmodder: Keep exact registered-command phrase coverage independent from evidence.
from __future__ import annotations

from types import MappingProxyType

from plugins.Minecraft.fabric.chatclef.command_registry import (
    KoreanChatClefCommandRegistry,
)
from plugins.Minecraft.fabric.chatclef.transport.command_feedback.lifecycle.kind import (
    CommandFeedbackLifecycleKindProfileRegistry,
)

from .command_phrase_profile import CommandPhraseProfile


_FAMILIES = {
    "get": ("item_get", "아이템 구하기"),
    "deposit": ("item_deposit", "아이템 보관"),
    "deposit_all": ("item_deposit", "아이템 전체 보관"),
    "equip": ("item_equip", "아이템 장착"),
    "give": ("item_give", "아이템 전달"),
    "goto": ("movement_goto", "위치 이동"),
    "follow": ("movement_follow", "플레이어 따라가기"),
    "food": ("food_acquisition", "음식 모으기"),
    "meat": ("meat_acquisition", "고기 모으기"),
    "auto_deposit_trust": ("registry", "자동 보관 대상 등록"),
    "auto_deposit_trusted_list": ("query", "자동 보관 대상 조회"),
    "auto_deposit_untrust": ("registry", "자동 보관 대상 해제"),
    "자동보관등록": ("registry", "자동 보관 대상 등록"),
    "store_home": ("store_home", "아이템 집 정리"),
    "locate_structure": ("query", "구조물 찾기"),
    "scan": ("query", "주변 탐색"),
    "gamma": ("settings", "감마 설정"),
    "overlay": ("settings", "오버레이 설정"),
    "chatclef": ("settings", "ChatClef 설정"),
    "reload_settings": ("settings", "설정 다시 불러오기"),
    "resetmemory": ("settings", "기억 초기화"),
    "idle": ("persistent", "대기"),
    "hero": ("persistent", "영웅 모드"),
    "gamer": ("generic", "게임 수행"),
    "attack": ("generic", "공격"),
    "stop": ("control", "작업 중지"),
}


class CommandPhraseProfileRegistry:
    def __init__(self, command_registry=None) -> None:
        registry = command_registry or KoreanChatClefCommandRegistry()
        lifecycle_kinds = CommandFeedbackLifecycleKindProfileRegistry(registry)
        profiles = {}
        for command_name in registry.command_names():
            family, label = _FAMILIES[command_name]
            profiles[command_name] = CommandPhraseProfile(
                command_name=command_name,
                lifecycle_kind=registry.spec(command_name).lifecycle_kind,
                profile_id=f"{command_name}_phrase_v1",
                family=family,
                command_label=label,
                response_lifecycle_kind=(
                    lifecycle_kinds.profile(command_name).response_lifecycle_kind
                ),
            )
        expected = tuple(registry.command_names())
        if tuple(profiles) != expected or set(_FAMILIES) != set(expected):
            raise RuntimeError("command phrase profiles must cover the registry exactly")
        self._profiles = MappingProxyType(profiles)

    def command_names(self) -> tuple[str, ...]:
        return tuple(self._profiles)

    def profile(self, command_name: object) -> CommandPhraseProfile:
        name = str(command_name or "").strip().lower()
        try:
            return self._profiles[name]
        except KeyError as error:
            raise KeyError(f"unknown command phrase profile: {name}") from error

    def mapping(self):
        return self._profiles


__all__ = ("CommandPhraseProfileRegistry",)
