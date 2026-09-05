#20260905_kpopmodder: Assemble focused translated-command pipeline boundaries.
from plugins.Minecraft.fabric.chatclef.extension.natural_language.submission.pipeline.translated_command_submission_failure_handler import TranslatedCommandSubmissionFailureHandler
from plugins.Minecraft.fabric.chatclef.extension.natural_language.submission.pipeline.translated_command_pre_submit_guard import TranslatedCommandPreSubmitGuard
from plugins.Minecraft.fabric.chatclef.extension.natural_language.submission.pipeline.translated_command_pipeline_request_builder import TranslatedCommandPipelineRequestBuilder
from plugins.Minecraft.fabric.chatclef.extension.natural_language.submission.pipeline.translated_command_submission_result_handler import TranslatedCommandSubmissionResultHandler
from plugins.Minecraft.fabric.chatclef.extension.natural_language.submission.pipeline.translated_command_transport_invoker import TranslatedCommandTransportInvoker


class TranslatedCommandPipelineComponentGraph:
    def __init__(
        self,
        *,
        command_submitter,
        result_recorder,
        result_factory,
        request_stage,
    ) -> None:
        self.request_builder = TranslatedCommandPipelineRequestBuilder(
            request_stage
        )
        self.guard = TranslatedCommandPreSubmitGuard()
        self.transport = TranslatedCommandTransportInvoker(command_submitter)
        self.failure_handler = TranslatedCommandSubmissionFailureHandler(
            result_factory=result_factory,
            result_recorder=result_recorder,
        )
        self.result_handler = TranslatedCommandSubmissionResultHandler(
            result_factory=result_factory,
            result_recorder=result_recorder,
        )


__all__ = ("TranslatedCommandPipelineComponentGraph",)
