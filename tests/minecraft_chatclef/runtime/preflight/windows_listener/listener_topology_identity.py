#20260819_kpopmodder: Validate exact loopback listener topology independently from process provenance.
from __future__ import annotations


LOOPBACK_ADDRESSES = {"127.0.0.1", "::1"}


def target_owner(
    entries: list[tuple[int, int, str]],
    port: int,
    expected_host: str,
    label: str,
) -> tuple[int, str]:
    expected = str(expected_host or "").strip().lower()
    if expected not in LOOPBACK_ADDRESSES:
        return -1, f"{label} expected host is not exact loopback"
    selected = [entry for entry in entries if entry[0] == port]
    owners = {entry[1] for entry in selected}
    if len(owners) != 1:
        return -1, f"{label} must have one listener owner"
    addresses = {entry[2] for entry in selected}
    if not addresses or any(address not in LOOPBACK_ADDRESSES for address in addresses):
        return -1, f"{label} listener must bind only exact loopback"
    if addresses != {expected}:
        return -1, f"{label} listener address does not match the approved URL family"
    return next(iter(owners)), ""


def owners_for_port(
    entries: list[tuple[int, int, str]],
    port: int,
) -> set[int]:
    return {process_id for entry_port, process_id, _address in entries if entry_port == port}
