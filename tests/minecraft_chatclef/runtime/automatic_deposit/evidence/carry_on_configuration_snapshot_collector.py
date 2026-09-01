#20260831_kpopmodder: Hash one bounded in-instance Carry On configuration bundle.
from __future__ import annotations

from collections.abc import Callable, Sequence
from pathlib import Path

from .carry_on_configuration_snapshot import (
    AutomaticDepositCarryOnConfigurationSnapshot,
    _create_carry_on_configuration_snapshot,
)
from .carry_on_configuration_content import (
    verify_automatic_deposit_carry_on_configuration_content,
)
from .carry_on_configuration_paths import (
    CARRY_ON_REQUIRED_CONFIGURATION_ROLES,
    automatic_deposit_carry_on_configuration_role,
)
from .stable_file_digest import read_automatic_deposit_stable_file


def collect_automatic_deposit_carry_on_configuration_snapshot(
    instance_root: object,
    config_paths: Sequence[object],
    *,
    bytes_reader: Callable[[Path], bytes] | None = None,
    stat_reader: Callable[[Path], object] | None = None,
) -> tuple[AutomaticDepositCarryOnConfigurationSnapshot | None, str]:
    instance = _absolute_path(instance_root)
    if instance is None:
        return None, "CARRY_ON_CONFIG_INSTANCE_ROOT_INVALID"
    if not isinstance(config_paths, Sequence) or isinstance(
        config_paths,
        (str, bytes, bytearray),
    ):
        return None, "CARRY_ON_CONFIG_PATH_LIST_INVALID"
    raw_paths = tuple(config_paths)
    if len(raw_paths) < 1 or len(raw_paths) > 8:
        return None, "CARRY_ON_CONFIG_FILE_COUNT_OUT_OF_BOUND"
    paths: list[Path] = []
    seen: set[str] = set()
    roles: dict[str, str] = {}
    for raw_path in raw_paths:
        path = _absolute_path(raw_path)
        if path is None or not _is_within(path, instance):
            return None, "CARRY_ON_CONFIG_PATH_INVALID"
        role = automatic_deposit_carry_on_configuration_role(instance, path)
        if not role:
            return None, "CARRY_ON_CONFIG_PATH_NOT_RECOGNIZED"
        key = _canonical(path)
        if key in seen:
            return None, "CARRY_ON_CONFIG_PATH_DUPLICATE"
        seen.add(key)
        roles[key] = role
        paths.append(path)
    if frozenset(roles.values()) != CARRY_ON_REQUIRED_CONFIGURATION_ROLES:
        return None, "CARRY_ON_CONFIGURATION_BUNDLE_INCOMPLETE"
    digests = []
    for path in sorted(paths, key=_canonical):
        digest, payload, reason = read_automatic_deposit_stable_file(
            path,
            bytes_reader=bytes_reader,
            stat_reader=stat_reader,
        )
        if digest is None or payload is None:
            return None, f"CARRY_ON_CONFIG_{reason}"
        #20260901_kpopmodder: Do not promote names or hashes alone into exact configuration proof.
        content_error = verify_automatic_deposit_carry_on_configuration_content(
            roles[_canonical(path)],
            payload,
        )
        if content_error:
            return None, content_error
        digests.append(digest)
    return (
        _create_carry_on_configuration_snapshot(
            str(instance),
            tuple(digests),
        ),
        "CARRY_ON_CONFIGURATION_SNAPSHOT_COLLECTED",
    )


def _absolute_path(value: object) -> Path | None:
    text = str(value or "").strip()
    if not text:
        return None
    path = Path(text)
    return path.resolve(strict=False) if path.is_absolute() else None


def _canonical(path: Path) -> str:
    return str(path.resolve(strict=False)).casefold()


def _is_within(candidate: Path, parent: Path) -> bool:
    try:
        candidate.relative_to(parent)
    except ValueError:
        return False
    return candidate != parent
