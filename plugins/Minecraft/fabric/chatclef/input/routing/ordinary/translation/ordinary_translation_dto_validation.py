#20260905_kpopmodder: Isolate ordinary translation DTO validation diagnostics.
from __future__ import annotations


class OrdinaryTranslationDtoValidation:
    def __init__(self, *, translation_boundary, decision_factory, router_logger):
        self._translation_boundary = translation_boundary
        self._decision_factory = decision_factory
        self._router_logger = router_logger

    def validate(self, raw_translation: object):
        try:
            return self._translation_boundary.validate(raw_translation), None
        except Exception as error:
            self._router_logger.log(
                "route rejected malformed translation: "
                f"error={type(error).__name__}: {error}"
            )
            return None, self._decision_factory.malformed_translation(error)


__all__ = ("OrdinaryTranslationDtoValidation",)
