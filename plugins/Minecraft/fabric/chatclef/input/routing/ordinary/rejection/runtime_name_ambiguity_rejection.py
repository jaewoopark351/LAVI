#20260915_kpopmodder: Admit bounded runtime-name clarification only for the exact live trusted request.
from collections.abc import Mapping
from plugins.Minecraft.fabric.chatclef.intent.names.korean_name_resolution_message import KoreanNameResolutionMessage
from .name.rejection_request_binding import RejectionRequestBinding


class RuntimeNameAmbiguityRejection:
    @staticmethod
    def message(translation, command_text, trusted_scope_live):
        if not isinstance(translation, Mapping) or translation.get("status") != "ambiguous":
            return None
        bound = RejectionRequestBinding.inspect(translation, command_text, trusted_scope_live)
        if bound is None:
            return None
        _, resolution, data = bound
        if not RejectionRequestBinding.has_catalogue(data) or translation.get("reason_code") != resolution.get("reason_code"):
            return None
        return KoreanNameResolutionMessage.ambiguous(resolution)
