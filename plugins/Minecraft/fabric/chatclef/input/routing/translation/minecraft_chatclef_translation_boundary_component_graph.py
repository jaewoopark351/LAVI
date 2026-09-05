#20260905_kpopmodder: Assemble focused translation boundary components.
from plugins.Minecraft.fabric.chatclef.input.routing.translation.minecraft_chatclef_translation_capability_inspector import MinecraftChatClefTranslationCapabilityInspector
from plugins.Minecraft.fabric.chatclef.input.routing.translation.minecraft_chatclef_translation_invoker import MinecraftChatClefTranslationInvoker
from plugins.Minecraft.fabric.chatclef.input.routing.translation.minecraft_chatclef_translation_result_validator import MinecraftChatClefTranslationResultValidator


class MinecraftChatClefTranslationBoundaryComponentGraph:
    def __init__(self) -> None:
        self.capability_inspector = (
            MinecraftChatClefTranslationCapabilityInspector()
        )
        self.invoker = MinecraftChatClefTranslationInvoker()
        self.result_validator = MinecraftChatClefTranslationResultValidator()


__all__ = ("MinecraftChatClefTranslationBoundaryComponentGraph",)
