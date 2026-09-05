#20260905_kpopmodder: Owns loading and saving the LLM speech-style configuration value.
from __future__ import annotations


class LlmSpeechStylePersistence:
    def __init__(
        self,
        *,
        load_section_callback,
        save_config_callback,
        section: str = "LLM",
        key: str = "speech_style",
    ) -> None:
        if not callable(load_section_callback):
            raise TypeError("load_section_callback must be callable")
        if not callable(save_config_callback):
            raise TypeError("save_config_callback must be callable")
        self._load_section_callback = load_section_callback
        self._save_config_callback = save_config_callback
        self._section = section
        self._key = key

    def load_configuration(self):
        return self._load_section_callback(self._section)

    def save_mode(self, mode: str) -> None:
        self._save_config_callback(self._section, self._key, mode)


__all__ = ("LlmSpeechStylePersistence",)
