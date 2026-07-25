#20260725_kpopmodder: Added parser for explicit Minecraft commands from LAVI chat input.
from __future__ import annotations

import re

from .minecraft_conversation_command_route import MinecraftConversationCommandRoute


class MinecraftConversationCommandParser:
    KOREAN_MINECRAFT_PREFIX_PATTERN = (
        "(?:\uB9C8\uD06C|\uB9C8\uC778\uD06C\uB798\uD504\uD2B8)"
    )
    PREFIX_PATTERN = re.compile(
        r"^\s*(?:/?(?:minecraft|mc)|"
        + KOREAN_MINECRAFT_PREFIX_PATTERN
        + r")\s*(?::|,)?\s*(?P<command>.+?)\s*$",
        re.IGNORECASE,
    )
    SUFFIX_PATTERNS = (
        re.compile(r"\s+(?:in|on|for)\s+(?:minecraft|mc)\s*[\.\!\?]*$", re.IGNORECASE),
        re.compile(r"\s+(?:minecraft|mc)\s+action\s*[\.\!\?]*$", re.IGNORECASE),
        re.compile(r"\s+(?:minecraft|mc)\s*[\.\!\?]*$", re.IGNORECASE),
    )
    COMMAND_PREFIXES = (
        "get ",
        "get-item ",
        "get_item ",
        "getitem ",
        "get and equip ",
        "get then equip ",
        "get-and-equip ",
        "get_and_equip ",
        "getandequip ",
        "collect ",
        "collect and equip ",
        "fetch ",
        "fetch and equip ",
        "bring ",
        "bring and equip ",
        "equip ",
        "equip item ",
        "hold ",
        "hold item ",
        "select ",
        "select item ",
        "goto ",
        "go to ",
        "move to ",
        "travel to ",
        "stop",
        "cancel",
        "status",
        "health",
        "ping",
        "inventory",
        "inv",
        "current action",
        "action status",
    )
    DIRECT_ITEM_PATTERN = re.compile(r"\b[a-z0-9_:.\\-]*[_:][a-z0-9_:.\\-]*\b", re.IGNORECASE)
    NUMBER_PATTERN = re.compile(r"\b\d+\b")

    def parse(self, text: object) -> MinecraftConversationCommandRoute | None:
        raw_text = str(text or "").strip()
        if not raw_text:
            return None

        prefixed = self._parse_prefixed(raw_text)
        if prefixed is not None:
            return prefixed

        return self._parse_suffixed(raw_text)

    def _parse_prefixed(self, raw_text: str) -> MinecraftConversationCommandRoute | None:
        match = self.PREFIX_PATTERN.match(raw_text)
        if not match:
            return None

        command = self._clean_command(match.group("command"))
        if not self._looks_like_command(command):
            return None
        return self._route(command, raw_text, "prefix")

    def _parse_suffixed(self, raw_text: str) -> MinecraftConversationCommandRoute | None:
        command = self._strip_suffix(raw_text)
        if command == raw_text:
            return None
        command = self._canonicalize_command(command)
        if not self._looks_like_command(command):
            return None
        return self._route(command, raw_text, "suffix")

    def _strip_suffix(self, raw_text: str) -> str:
        command = raw_text
        for pattern in self.SUFFIX_PATTERNS:
            command = pattern.sub("", command)
        return self._clean_command(command)

    def _canonicalize_command(self, command: str) -> str:
        lowered = command.lower()
        if lowered.startswith("stop") and "action" in lowered:
            return "stop"
        return command

    def _looks_like_command(self, command: str) -> bool:
        lowered = command.lower()
        if any(
            lowered == prefix.strip() or lowered.startswith(prefix)
            for prefix in self.COMMAND_PREFIXES
        ):
            return True
        return bool(
            self.DIRECT_ITEM_PATTERN.search(lowered)
            and self.NUMBER_PATTERN.search(lowered)
        )

    def _clean_command(self, value: str) -> str:
        return " ".join(str(value or "").strip().strip("`\"'").split())

    def _route(
        self,
        command: str,
        raw_text: str,
        trigger: str,
    ) -> MinecraftConversationCommandRoute:
        return MinecraftConversationCommandRoute(
            game="minecraft",
            command=command,
            raw_text=raw_text,
            trigger=trigger,
        )
