#20260708_kpopmodder: Added rules-based SC2 log parser for passive Changeling commentary.
from __future__ import annotations

import re
from collections import deque
from dataclasses import dataclass
from typing import Deque, Optional, Tuple

from .sc2_speech_terms import (
    sc2_strategy_speak_name,
    sc2_unit_speak_name,
    sc2_upgrade_speak_name,
)


@dataclass(frozen=True)
class SC2ParsedEvent:
    message: str
    category: str
    raw_line: str


class SC2EventParser:
    """Convert ProBots/Changeling log lines into short first-person commentary."""

    def __init__(self, bot_name: str = "Changeling", recent_cache_size: int = 80):
        self.bot_name = str(bot_name or "Changeling")
        self._recent: Deque[str] = deque(maxlen=max(1, int(recent_cache_size)))

    def parse_line(self, line: str) -> Optional[str]:
        event = self.parse_event(line)
        return event.message if event is not None else None

    def parse_event(self, line: str) -> Optional[SC2ParsedEvent]:
        raw = str(line or "").strip()
        if not raw:
            return None

        category, message = self._message_for_line(raw)
        if not message:
            return None

        # The parser lives across matches, but duplicate suppression is
        # scoped to one match. Clear the cache before deduplication so a
        # repeated opening or upgrade in the next game is observable again.
        if category == "game_started":
            self.reset()

        normalized = self._normalize_message(message)
        if normalized in self._recent:
            return None
        self._recent.append(normalized)
        return SC2ParsedEvent(message=message, category=category, raw_line=raw)

    def reset(self) -> None:
        self._recent.clear()

    def _message_for_line(self, line: str) -> Tuple[str, str]:
        clean_line = self._strip_ansi(line)
        lower = clean_line.lower()
        compact = re.sub(r"\s+", " ", clean_line).strip()

        #20260712_kpopmodder: Keep raw SC2 engine/bot exceptions in logs only.
        # Tracebacks are too noisy and should never be forwarded to spoken TTS.
        if self._is_raw_error_line(lower):
            return "", ""

        #20260710_kpopmodder: Consume structured Observation summaries from
        # SharkyLAVBot so its live game state reaches the common TTS path.
        observation = re.search(
            r"LAV_OBSERVATION frame=(?P<frame>\d+) minerals=(?P<minerals>\d+) "
            r"supply=(?P<supply>\d+) units=(?P<units>\d+)",
            compact,
            re.IGNORECASE,
        )
        if observation:
            return (
                "observation",
                f"{self.bot_name} 현재 미네랄 {observation.group('minerals')}, "
                f"보급 {observation.group('supply')}, 유닛 {observation.group('units')}기로 움직이고 있어요.",
            )
        if "LAV_OBSERVATION game_started" in compact:
            return "game_started", f"{self.bot_name} 경기를 시작했어요."
        if "LAV_OBSERVATION game_ended" in compact:
            return "result", f"{self.bot_name} 경기가 끝났어요."

        #20260710_kpopmodder: Keep SC2 log commentary in LAV's first-person voice.
        if self._has_any(lower, ("starting game", "starting ladder game", "game started", "match started")):
            return "game_started", "내가 게임을 시작했어요. 로그 해설은 기록만 남길게요."
        if self._has_any(lower, ("traceback", "exception", " fatal", "error", "failed")):
            return "error", f"내가 로그에서 오류를 감지했어요. {self._shorten(compact)}"
        if self._has_any(lower, ("victory", "winner", "won the game", "result: victory")):
            return "result", "내가 승리를 기록했어요."
        if self._has_any(lower, ("defeat", "lost the game", "result: defeat", "surrender", "resigning")):
            return "result", "내가 패배한 것으로 보여요."
        if self._has_any(lower, ("end game report", "game ended", "match ended")):
            return "result", "내가 경기를 종료했어요. 결과 로그를 확인할게요."

        if self._has_any(lower, ("starting game", "game started", "match started")):
            return "game_started", f"{self.bot_name} 게임이 시작됐어요. 로그 해설을 붙일게요."
        if self._has_any(lower, ("traceback", "exception", " fatal", "error", "failed")):
            return "error", f"{self.bot_name} 로그에서 오류가 감지됐어요. {self._shorten(compact)}"
        if self._has_any(lower, ("victory", "winner", "won the game", "result: victory")):
            return "result", f"{self.bot_name}이 승리를 기록했어요."
        if self._has_any(lower, ("defeat", "lost the game", "result: defeat", "surrender", "resigning")):
            return "result", f"{self.bot_name}이 패배하거나 항복한 것으로 보여요."
        if self._has_any(lower, ("end game report", "game ended", "match ended")):
            return "result", f"{self.bot_name} 경기가 종료됐어요. 결과 로그를 확인할게요."

        specific_category, specific_message = self._specific_changeling_message(
            lower,
            compact,
        )
        if specific_message:
            return specific_category, specific_message

        if self._has_any(lower, ("rush", "all-in", "all in")):
            return "rush", "내가 러시 징후를 포착했어요."
        if self._has_any(lower, ("under attack", "attacking", "attack", "combat", "engage")):
            return "attack", "내가 교전 상황을 감지했어요."
        if self._has_any(lower, ("enemy", "opponent", "hostile", "spotted", "detected")):
            return "enemy", "내가 적 움직임을 확인했어요."
        if self._has_any(lower, ("scout", "scouting", "recon")):
            return "scout", "내가 정찰 정보를 갱신했어요."
        if self._has_any(lower, ("train", "training", "unit created", "morph", "produced")):
            return "train", "내가 병력 생산 로그를 확인했어요."
        if self._has_any(lower, ("build", "building", "construct", "started structure")):
            return "build", "내가 건설 또는 빌드 진행을 시작했어요."
        if self._has_any(lower, ("expand", "expansion", "new base")):
            return "expand", "내가 확장 움직임을 보고 있어요."

        if self._has_any(lower, ("rush", "all-in", "all in")):
            return "rush", f"{self.bot_name}이 러시 징후를 포착했어요."
        if self._has_any(lower, ("under attack", "attacking", "attack", "combat", "engage")):
            return "attack", f"{self.bot_name}이 교전 상황을 감지했어요."
        if self._has_any(lower, ("enemy", "opponent", "hostile", "spotted", "detected")):
            return "enemy", f"{self.bot_name}이 적 움직임을 확인했어요."
        if self._has_any(lower, ("scout", "scouting", "recon")):
            return "scout", f"{self.bot_name}이 정찰 정보를 갱신했어요."
        if self._has_any(lower, ("train", "training", "unit created", "morph", "produced")):
            return "train", f"{self.bot_name}이 병력 생산 로그를 남겼어요."
        if self._has_any(lower, ("build", "building", "construct", "started structure")):
            return "build", f"{self.bot_name}이 건설 또는 빌드 진행을 시작했어요."
        if self._has_any(lower, ("expand", "expansion", "new base")):
            return "expand", f"{self.bot_name}이 확장 움직임을 보이고 있어요."

        return "", ""

    def _specific_changeling_message(self, lower: str, compact: str) -> Tuple[str, str]:
        opening = re.search(r"Chosen opening:\s*([A-Za-z0-9_.-]+)", compact)
        if opening:
            opening_name = sc2_strategy_speak_name(opening.group(1))
            return (
                "strategy",
                f"내가 빌드 오프닝을 {opening_name}로 선택했어요.",
            )

        transition = re.search(
            r"(?P<clock>\d{2}:\d{2}:?)?\s*Transitioning from "
            r"(?P<from>[A-Za-z0-9_.-]+) to (?P<to>[A-Za-z0-9_.-]+)",
            compact,
        )
        if transition:
            clock_value = str(transition.group("clock") or "").strip().rstrip(":")
            clock = f"{clock_value}에 " if clock_value else ""
            from_name = sc2_strategy_speak_name(transition.group("from"))
            to_name = sc2_strategy_speak_name(transition.group("to"))
            return (
                "strategy",
                f"내가 {clock}{from_name}에서 "
                f"{to_name} 쪽으로 전략을 전환했어요.",
            )

        army = re.search(r"Changed army composition to:\s*([A-Za-z0-9_.-]+)", compact)
        if army:
            army_name = sc2_strategy_speak_name(army.group(1))
            return (
                "strategy",
                f"내가 병력 구성을 {army_name} 쪽으로 바꿨어요.",
            )

        started = re.search(
            r"(?P<clock>\d{2}:\d{2})\s+(?P<name>[A-Z][A-Z0-9_]+)\s+started\b",
            compact,
        )
        if started:
            return (
                "build",
                f"내가 {started.group('clock')}에 "
                f"{self._unit_name(started.group('name'))} 건설을 시작했어요.",
            )

        tracked = re.search(
            r"(?P<clock>\d{2}:\d{2})\s+UnitTypeId\.(?P<name>[A-Z0-9_]+) "
            r"added to building tracker",
            compact,
        )
        if tracked:
            return (
                "build",
                f"내가 {tracked.group('clock')}에 "
                f"{self._unit_name(tracked.group('name'))} 건설 준비를 시작했어요.",
            )

        structure_queue = re.search(
            r"Structure queue:\s*\[UnitTypeId\.(?P<name>[A-Z0-9_]+)\]",
            compact,
        )
        if structure_queue:
            return (
                "build",
                f"내가 구조물 대기열에 "
                f"{self._unit_name(structure_queue.group('name'))}를 올렸어요.",
            )

        upgrade_queue = re.search(
            r"Upgrade queue:\s*\[UpgradeId\.(?P<name>[A-Z0-9_]+)\]",
            compact,
        )
        if upgrade_queue:
            return (
                "upgrade",
                f"내가 업그레이드 대기열에 "
                f"{self._upgrade_name(upgrade_queue.group('name'))}를 올렸어요.",
            )

        build_step = re.search(
            r"\b(?P<supply>\d{1,3})\s+(?P<clock>\d{2}:\d{2})\s+"
            r"(?P<name>[A-Z][A-Z0-9_]+)\b",
            compact,
        )
        if "build_runner" in lower and build_step:
            return (
                "train",
                f"내가 {build_step.group('clock')}에 "
                f"{self._unit_name(build_step.group('name'))} 생산 단계를 진행했어요.",
            )

        opening = re.search(r"Chosen opening:\s*([A-Za-z0-9_.-]+)", compact)
        if opening:
            return (
                "strategy",
                f"{self.bot_name} 빌드 오프닝은 {opening.group(1)}로 잡혔어요.",
            )

        transition = re.search(
            r"(?P<clock>\d{2}:\d{2}:?)?\s*Transitioning from "
            r"(?P<from>[A-Za-z0-9_.-]+) to (?P<to>[A-Za-z0-9_.-]+)",
            compact,
        )
        if transition:
            clock = self._clock_prefix(transition.group("clock"))
            return (
                "strategy",
                f"{self.bot_name}이 {clock}{transition.group('from')}에서 "
                f"{transition.group('to')}로 전략을 전환했어요.",
            )

        army = re.search(r"Changed army composition to:\s*([A-Za-z0-9_.-]+)", compact)
        if army:
            return (
                "strategy",
                f"{self.bot_name} 병력 구성이 {army.group(1)}으로 바뀌었어요.",
            )

        started = re.search(
            r"(?P<clock>\d{2}:\d{2})\s+(?P<name>[A-Z][A-Z0-9_]+)\s+started\b",
            compact,
        )
        if started:
            return (
                "build",
                f"{self.bot_name}이 {started.group('clock')}에 "
                f"{self._unit_name(started.group('name'))} 건설을 시작했어요.",
            )

        tracked = re.search(
            r"(?P<clock>\d{2}:\d{2})\s+UnitTypeId\.(?P<name>[A-Z0-9_]+) "
            r"added to building tracker",
            compact,
        )
        if tracked:
            return (
                "build",
                f"{self.bot_name}이 {tracked.group('clock')}에 "
                f"{self._unit_name(tracked.group('name'))} 건설 준비를 시작했어요.",
            )

        structure_queue = re.search(
            r"Structure queue:\s*\[UnitTypeId\.(?P<name>[A-Z0-9_]+)\]",
            compact,
        )
        if structure_queue:
            return (
                "build",
                f"{self.bot_name} 구조물 큐에 "
                f"{self._unit_name(structure_queue.group('name'))}가 올라왔어요.",
            )

        upgrade_queue = re.search(
            r"Upgrade queue:\s*\[UpgradeId\.(?P<name>[A-Z0-9_]+)\]",
            compact,
        )
        if upgrade_queue:
            return (
                "upgrade",
                f"{self.bot_name} 업그레이드 큐에 "
                f"{self._unit_name(upgrade_queue.group('name'))}가 올라왔어요.",
            )

        build_step = re.search(
            r"\b(?P<supply>\d{1,3})\s+(?P<clock>\d{2}:\d{2})\s+"
            r"(?P<name>[A-Z][A-Z0-9_]+)\b",
            compact,
        )
        if "build_runner" in lower and build_step:
            return (
                "train",
                f"{self.bot_name}이 {build_step.group('clock')}에 "
                f"{self._unit_name(build_step.group('name'))} 생산 단계를 진행했어요.",
            )

        return "", ""

    def _has_any(self, text: str, keywords: Tuple[str, ...]) -> bool:
        return any(keyword in text for keyword in keywords)

    def _is_raw_error_line(self, lower: str) -> bool:
        if re.search(r"\b(traceback|exception|fatal|error|failed)\b", lower):
            return True
        if re.search(r"\|\s*error\s*\|", lower):
            return True
        return any(
            marker in lower
            for marker in (
                "traceback",
                "assertionerror",
                "attributeerror",
                "caught unknown exception",
                "during handling of the above exception",
                "failed to execute script",
                "connection already closed",
                "connection was closed before the game ended",
                "cannot receive:",
                "[pyi-",
                "resigning due to previous error",
                "self._find_expansion_locations()",
            )
        )

    def _shorten(self, text: str, limit: int = 120) -> str:
        if len(text) <= limit:
            return text
        return text[: limit - 3].rstrip() + "..."

    def _normalize_message(self, message: str) -> str:
        return re.sub(r"\s+", " ", str(message or "").strip().lower())

    def _strip_ansi(self, text: str) -> str:
        return re.sub(r"\x1b\[[0-9;]*m", "", str(text or ""))

    def _unit_name(self, name: str) -> str:
        return sc2_unit_speak_name(name)

    def _upgrade_name(self, name: str) -> str:
        return sc2_upgrade_speak_name(name)

    def _clock_prefix(self, clock: Optional[str]) -> str:
        value = str(clock or "").strip().rstrip(":")
        return f"{value}에 " if value else ""
