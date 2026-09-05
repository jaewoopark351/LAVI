#20260905_kpopmodder: Assemble focused router submission boundary components.
from __future__ import annotations

from plugins.Minecraft.fabric.chatclef.input.routing.submission.minecraft_chatclef_submission_outcome_validator import MinecraftChatClefSubmissionOutcomeValidator
from plugins.Minecraft.fabric.chatclef.input.routing.submission.minecraft_chatclef_submission_request_builder import MinecraftChatClefSubmissionRequestBuilder
from plugins.Minecraft.fabric.chatclef.input.routing.submission.submission_request_factory import MinecraftChatClefSubmissionRequestFactory
from plugins.Minecraft.fabric.chatclef.input.routing.submission.submission_result_normalizer import MinecraftChatClefSubmissionResultNormalizer
from plugins.Minecraft.fabric.chatclef.input.routing.submission.minecraft_chatclef_submission_transport_invoker import MinecraftChatClefSubmissionTransportInvoker


class MinecraftChatClefSubmissionBoundaryComponentGraph:
    def __init__(self) -> None:
        self.request_builder = MinecraftChatClefSubmissionRequestBuilder(
            MinecraftChatClefSubmissionRequestFactory()
        )
        self.transport_invoker = MinecraftChatClefSubmissionTransportInvoker()
        self.outcome_validator = MinecraftChatClefSubmissionOutcomeValidator(
            MinecraftChatClefSubmissionResultNormalizer()
        )


__all__ = ("MinecraftChatClefSubmissionBoundaryComponentGraph",)
