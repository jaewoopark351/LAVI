#20260818_kpopmodder: Normalize only player Inventory compounds into item totals.
from __future__ import annotations

from collections.abc import Mapping


def extract_player_inventory_counts(
    document: Mapping[str, object],
) -> dict[str, int]:
    inventory = document.get("Inventory")
    if not isinstance(inventory, list):
        raise ValueError("playerdata Inventory list is missing")
    counts: dict[str, int] = {}
    for raw_entry in inventory:
        if not isinstance(raw_entry, Mapping):
            raise ValueError("playerdata Inventory entry is not a compound")
        item_id = str(raw_entry.get("id") or "").strip()
        raw_count = raw_entry.get("Count")
        if not item_id or not isinstance(raw_count, int) or raw_count < 0:
            raise ValueError("playerdata Inventory entry is invalid")
        counts[item_id] = counts.get(item_id, 0) + raw_count
    return counts
