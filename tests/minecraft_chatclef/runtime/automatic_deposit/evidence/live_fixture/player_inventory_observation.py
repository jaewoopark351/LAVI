#20260901_kpopmodder: Seal one stable saved-player inventory observation.
from __future__ import annotations

from dataclasses import dataclass, field


_PLAYER_INVENTORY_OBSERVATION_SEAL = object()


@dataclass(frozen=True, slots=True)
class SavedPlayerInventoryObservation:
    player_data_path: str
    player_data_sha256: str
    player_data_size: int
    player_data_mtime_ns: int
    inventory_counts: tuple[tuple[str, int], ...]
    inventory_fingerprint: str
    _seal: object = field(default=None, repr=False, compare=False)

    def __post_init__(self) -> None:
        if self._seal is not _PLAYER_INVENTORY_OBSERVATION_SEAL:
            raise ValueError("player inventory observation must be observer-created")


def _create_saved_player_inventory_observation(
    *,
    player_data_path: str,
    player_data_sha256: str,
    player_data_size: int,
    player_data_mtime_ns: int,
    inventory_counts: tuple[tuple[str, int], ...],
    inventory_fingerprint: str,
) -> SavedPlayerInventoryObservation:
    return SavedPlayerInventoryObservation(
        player_data_path=player_data_path,
        player_data_sha256=player_data_sha256,
        player_data_size=player_data_size,
        player_data_mtime_ns=player_data_mtime_ns,
        inventory_counts=inventory_counts,
        inventory_fingerprint=inventory_fingerprint,
        _seal=_PLAYER_INVENTORY_OBSERVATION_SEAL,
    )
