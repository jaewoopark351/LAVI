#20260821_kpopmodder: Bound reconciliation tombstones by count and monotonic retention.
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Iterable, Mapping

from plugins.Minecraft.common.dto.command_result_dto import CommandResultDTO

from .reconciled_active_command_tombstone import ReconciledActiveCommandTombstone


@dataclass(frozen=True)
class ActiveCommandTombstoneStore:
    max_entries: int = 128
    retention_ms: int = 600_000
    entries: tuple[ReconciledActiveCommandTombstone, ...] = field(
        default_factory=tuple
    )

    def record(
        self,
        tombstone: ReconciledActiveCommandTombstone,
        *,
        now_ms: int,
    ) -> "ActiveCommandTombstoneStore":
        retained = [entry for entry in self.entries if not entry.is_expired(now_ms)]
        retained.append(tombstone)
        retained.sort(key=lambda entry: entry.reconciled_at_ms)
        if len(retained) > self.max_entries:
            retained = retained[-self.max_entries :]
        return ActiveCommandTombstoneStore(
            max_entries=self.max_entries,
            retention_ms=self.retention_ms,
            entries=tuple(retained),
        )

    def audit_late_result(
        self,
        *,
        session_id: str | None,
        correlation_id: str | None,
        result: CommandResultDTO,
        data: Mapping[str, Any],
        event_kind: str,
        now_ms: int,
    ) -> tuple["ActiveCommandTombstoneStore", dict[str, Any] | None]:
        retained = [entry for entry in self.entries if not entry.is_expired(now_ms)]
        updated: list[ReconciledActiveCommandTombstone] = []
        audit: dict[str, Any] | None = None
        matched = False
        for entry in retained:
            if (
                not matched
                and entry.identity.matches_result(
                    session_id=session_id,
                    correlation_id=correlation_id,
                    request_id=result.request_id,
                    data=data,
                )
            ):
                matched = True
                next_entry = entry.with_late_event(event_kind)
                updated.append(next_entry)
                audit = {
                    "event": event_kind,
                    "matched_tombstone": True,
                    "identity": entry.identity.to_dict(),
                    "late_event_count": next_entry.late_event_count,
                    "state_mutation": "audit_only",
                }
            else:
                updated.append(entry)
        return (
            ActiveCommandTombstoneStore(
                max_entries=self.max_entries,
                retention_ms=self.retention_ms,
                entries=tuple(updated),
            ),
            audit,
        )

    def to_list(self, now_ms: int | None = None) -> list[dict[str, Any]]:
        entries = (
            self.entries
            if now_ms is None
            else tuple(entry for entry in self.entries if not entry.is_expired(now_ms))
        )
        return [entry.to_dict() for entry in entries]

    def __iter__(self) -> Iterable[ReconciledActiveCommandTombstone]:
        return iter(self.entries)
