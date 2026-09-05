#20260905_kpopmodder: Emit one fault-contained feature-admission boundary record.
from __future__ import annotations

from core.logger import log_print

from .minecraft_korean_feature_admission_formatter import (
    MinecraftKoreanFeatureAdmissionFormatter,
)


class MinecraftKoreanFeatureAdmissionLogger:
    def __init__(self, callback=log_print, formatter=None):
        self._callback = callback
        self._formatter = formatter or MinecraftKoreanFeatureAdmissionFormatter()

    def log(self, record: object) -> bool:
        try:
            self._callback(self._formatter.format(record))
        except Exception:
            return False
        return True


__all__ = ("MinecraftKoreanFeatureAdmissionLogger",)
