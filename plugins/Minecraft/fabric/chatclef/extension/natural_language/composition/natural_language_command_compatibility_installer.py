#20260905_kpopmodder: Install natural-language compatibility seams separately from assembly.


class NaturalLanguageCommandCompatibilityInstaller:
    def install(self, owner, graph) -> None:
        owner._natural_language_service = graph.natural_language_service
        owner._translated_submission = graph.translated_submission
        owner._generic_crafting_translation = graph.generic_crafting_translation
        owner._generic_crafting_submission = graph.generic_crafting_submission
        owner._legacy_commands = graph.legacy_commands
        owner._activation_lifecycle = graph.activation_lifecycle


__all__ = ("NaturalLanguageCommandCompatibilityInstaller",)
