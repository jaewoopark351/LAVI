#20260905_kpopmodder: Inspect Feature-B seam availability only.


class GenericCraftingScopedCapabilityAvailabilityStage:
    def __init__(
        self,
        *,
        extension,
        translation_boundary,
        submission_boundary,
        decision_factory,
    ):
        self._extension = extension
        self._translation_boundary = translation_boundary
        self._submission_boundary = submission_boundary
        self._decision_factory = decision_factory

    def is_available(self) -> bool:
        return bool(
            self._extension is not None
            and self._translation_boundary.is_generic_crafting_defaults_available(
                self._extension
            )
            and self._submission_boundary.is_generic_crafting_defaults_available(
                self._extension
            )
        )

    def rejection_if_unavailable(self):
        if self.is_available():
            return None
        return self._decision_factory.generic_crafting_defaults_rejection(
            "generic_crafting_scoped_seam_unavailable",
            "요청별 제작 명령 경계를 사용할 수 없어요.",
        )


__all__ = ("GenericCraftingScopedCapabilityAvailabilityStage",)
