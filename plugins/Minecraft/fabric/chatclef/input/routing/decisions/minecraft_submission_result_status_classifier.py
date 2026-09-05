#20260905_kpopmodder: Isolate submission-status normalization from decision assembly.
from __future__ import annotations

from typing import Any, Mapping


class MinecraftSubmissionResultStatusClassifier:
    def classify(self, result: Mapping[str, Any]) -> str:
        status = result.get("status")
        if isinstance(status, Mapping):
            return str(status.get("status") or "").strip().lower()
        return str(status or "").strip().lower()
