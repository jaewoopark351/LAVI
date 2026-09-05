#20260905_kpopmodder: Assemble translated-command admission boundaries.
from plugins.Minecraft.fabric.chatclef.extension.natural_language.submission.stages.admission.translated_command_admission_committer import (
    TranslatedCommandAdmissionCommitter,
)
from plugins.Minecraft.fabric.chatclef.extension.natural_language.submission.stages.admission.translated_command_admission_inspector import (
    TranslatedCommandAdmissionInspector,
)


class TranslatedCommandAdmissionComponentGraph:
    def __init__(self, *, admission, registry_provider) -> None:
        self.inspector = TranslatedCommandAdmissionInspector(
            admission=admission,
            registry_provider=registry_provider,
        )
        self.committer = TranslatedCommandAdmissionCommitter(admission)


__all__ = ("TranslatedCommandAdmissionComponentGraph",)
