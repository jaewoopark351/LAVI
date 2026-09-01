#20260901_kpopmodder: Added this module to keep one project class per Python file.
from __future__ import annotations

from collections.abc import Callable
from dataclasses import dataclass


PreSubmitEvidenceReader = Callable[[], tuple[object | None, str]]


@dataclass(frozen=True, slots=True)
class P1SupervisedPreSubmitDependencies:
    runtime_artifact_reader: PreSubmitEvidenceReader
    trusted_destination_reader: PreSubmitEvidenceReader
