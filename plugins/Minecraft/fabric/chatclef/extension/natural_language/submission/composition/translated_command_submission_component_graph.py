#20260905_kpopmodder: Assemble translated-command stages outside its facade.
from __future__ import annotations

from ..stages import (
    TranslatedCommandAdmissionStage,
    TranslatedCommandInputStage,
    TranslatedCommandRequestStage,
    TranslatedCommandRouteClaimLifecycle,
)
from ..translated_command_submission_pipeline import (
    TranslatedCommandSubmissionPipeline,
)
from .translated_command_submission_compatibility_installer import (
    TranslatedCommandSubmissionCompatibilityInstaller,
)


class TranslatedCommandSubmissionComponentGraph:
    def __init__(
        self,
        *,
        registry_provider,
        command_submitter,
        result_recorder,
        admission,
        request_factory,
        result_factory,
    ):
        self._compatibility_installer = (
            TranslatedCommandSubmissionCompatibilityInstaller()
        )
        self.registry_provider = registry_provider
        self.command_submitter = command_submitter
        self.result_recorder = result_recorder
        self.admission = admission
        self.result_factory = result_factory
        self.input_stage = TranslatedCommandInputStage()
        self.admission_stage = TranslatedCommandAdmissionStage(
            admission=admission,
            registry_provider=registry_provider,
        )
        self.request_stage = TranslatedCommandRequestStage(request_factory)
        self.route_claim_lifecycle = TranslatedCommandRouteClaimLifecycle(
            admission
        )
        self.pipeline = TranslatedCommandSubmissionPipeline(
            command_submitter=command_submitter,
            result_recorder=result_recorder,
            result_factory=result_factory,
            input_stage=self.input_stage,
            admission_stage=self.admission_stage,
            request_stage=self.request_stage,
            route_claim_lifecycle=self.route_claim_lifecycle,
        )

    def install_compatibility_seams(self, owner) -> None:
        self._compatibility_installer.install(owner, self)


__all__ = ("TranslatedCommandSubmissionComponentGraph",)
