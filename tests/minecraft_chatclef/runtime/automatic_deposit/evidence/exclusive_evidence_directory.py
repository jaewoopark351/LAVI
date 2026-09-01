#20260831_kpopmodder: Exclusively create one canonical run evidence directory.
from __future__ import annotations

import re
from collections.abc import Callable
from pathlib import Path

from .evidence_directory_result import AutomaticDepositEvidenceDirectoryResult


class AutomaticDepositExclusiveEvidenceDirectory:
    def __init__(
        self,
        repository_root: object,
        *,
        exists: Callable[[Path], bool] | None = None,
        ensure_parent: Callable[[Path], None] | None = None,
        create_exclusive: Callable[[Path], None] | None = None,
    ) -> None:
        supplied_repository_root = Path(str(repository_root or "").strip())
        self._repository_root_was_absolute = supplied_repository_root.is_absolute()
        self._repository_root = supplied_repository_root.resolve(
            strict=False
        )
        self._active_repository_root = Path(__file__).resolve().parents[5]
        self._base_directory = self._repository_root.joinpath(
            "test",
            "test_Isolation",
            "automatic_deposit_runtime",
        )
        self._exists = exists or _path_exists
        self._ensure_parent = ensure_parent or _ensure_parent
        self._create_exclusive = create_exclusive or _create_exclusive

    def create(self, run_id: object) -> AutomaticDepositEvidenceDirectoryResult:
        if not self._repository_root_was_absolute:
            return AutomaticDepositEvidenceDirectoryResult(
                False,
                "EVIDENCE_REPOSITORY_ROOT_NOT_ABSOLUTE",
            )
        if self._repository_root != self._active_repository_root:
            return AutomaticDepositEvidenceDirectoryResult(
                False,
                "EVIDENCE_REPOSITORY_ROOT_NOT_ACTIVE",
            )
        normalized_run_id = str(run_id or "").strip()
        if not re.fullmatch(
            r"[A-Za-z0-9][A-Za-z0-9._-]{0,127}",
            normalized_run_id,
        ) or normalized_run_id in (".", ".."):
            return AutomaticDepositEvidenceDirectoryResult(
                False,
                "EVIDENCE_RUN_ID_INVALID",
            )
        expected_base = self._repository_root.joinpath(
            "test",
            "test_Isolation",
            "automatic_deposit_runtime",
        ).resolve(strict=False)
        try:
            expected_base.relative_to(self._repository_root)
        except ValueError:
            return AutomaticDepositEvidenceDirectoryResult(
                False,
                "EVIDENCE_BASE_DIRECTORY_ESCAPES_REPOSITORY",
            )
        if self._base_directory.resolve(strict=False) != expected_base:
            return AutomaticDepositEvidenceDirectoryResult(
                False,
                "EVIDENCE_BASE_DIRECTORY_NOT_CANONICAL",
            )
        target = self._base_directory.joinpath(normalized_run_id).resolve(
            strict=False
        )
        if target.parent != expected_base:
            return AutomaticDepositEvidenceDirectoryResult(
                False,
                "EVIDENCE_TARGET_ESCAPES_BASE_DIRECTORY",
            )
        if self._exists(target):
            return AutomaticDepositEvidenceDirectoryResult(
                False,
                "EVIDENCE_DIRECTORY_ALREADY_EXISTS",
                str(target),
            )
        try:
            self._ensure_parent(expected_base)
            self._create_exclusive(target)
        except FileExistsError:
            return AutomaticDepositEvidenceDirectoryResult(
                False,
                "EVIDENCE_DIRECTORY_ALREADY_EXISTS",
                str(target),
            )
        except Exception as error:
            return AutomaticDepositEvidenceDirectoryResult(
                False,
                f"EVIDENCE_DIRECTORY_CREATE_FAILED:{type(error).__name__}",
                str(target),
            )
        return AutomaticDepositEvidenceDirectoryResult(
            True,
            "EVIDENCE_DIRECTORY_CREATED_EXCLUSIVELY",
            str(target),
        )


def _path_exists(path: Path) -> bool:
    return path.exists()


def _ensure_parent(path: Path) -> None:
    path.mkdir(parents=True, exist_ok=True)


def _create_exclusive(path: Path) -> None:
    path.mkdir(parents=False, exist_ok=False)
