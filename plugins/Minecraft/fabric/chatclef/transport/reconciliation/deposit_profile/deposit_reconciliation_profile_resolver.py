#20260901_kpopmodder: Isolate canonical deposit-command reconciliation eligibility.
from __future__ import annotations

import re

from .deposit_reconciliation_profile import DepositReconciliationProfile


_ITEM_ID = re.compile(r"^[a-z0-9_]+$")
_COUNT = re.compile(r"^[1-9][0-9]*$")
_JAVA_INT_MAX = 2_147_483_647


class DepositReconciliationProfileResolver:
    def resolve(self, command: object) -> DepositReconciliationProfile:
        raw = str(command or "").strip()
        if not raw:
            return self._blocked("blank_command")
        if any(_is_control(ch) for ch in raw):
            return self._blocked("control_character_in_command")
        prefixed = raw
        if prefixed[0] in {"@", "/"}:
            prefixed = prefixed[1:].strip()
            if not prefixed or prefixed[0] in {"@", "/"}:
                return self._blocked("multiple_or_mixed_command_prefix")
        normalized = " ".join(prefixed.replace("\t", " ").split(" "))
        normalized = " ".join(part for part in normalized.split(" ") if part)
        tokens = normalized.split(" ")
        if len(tokens) != 3:
            return self._blocked("deposit_command_requires_three_tokens")
        if tokens[0] != "deposit":
            return self._blocked("command_is_not_deposit")
        item_id = tokens[1]
        count_text = tokens[2]
        if not _ITEM_ID.fullmatch(item_id):
            return self._blocked("deposit_item_id_not_canonical")
        if not _COUNT.fullmatch(count_text):
            return self._blocked("deposit_count_not_positive_decimal")
        count = int(count_text)
        if count > _JAVA_INT_MAX:
            return self._blocked("deposit_count_exceeds_java_int")
        return DepositReconciliationProfile(
            allowed=True,
            normalized_command=f"deposit {item_id} {count}",
            item_id=item_id,
            count=count,
            reason="deposit_profile_exact",
        )

    def _blocked(self, reason: str) -> DepositReconciliationProfile:
        return DepositReconciliationProfile(allowed=False, reason=reason)


def _is_control(value: str) -> bool:
    codepoint = ord(value)
    return codepoint < 32 or codepoint == 127
