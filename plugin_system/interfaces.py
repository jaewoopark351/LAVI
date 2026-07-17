#20260622_kpopmodder: Compatibility exports for canonical plugin interfaces.
from plugin_system.interfaces_core.input_plugin_interface import InputPluginInterface
from plugin_system.interfaces_core.llm_plugin_interface import LLMPluginInterface
from plugin_system.interfaces_core.translation_plugin_interface import (
    TranslationPluginInterface,
)
from plugin_system.interfaces_core.tts_plugin_interface import TTSPluginInterface
from plugin_system.interfaces_core.vtuber_plugin_interface import VtuberPluginInterface


__all__ = [
    "InputPluginInterface",
    "LLMPluginInterface",
    "TranslationPluginInterface",
    "TTSPluginInterface",
    "VtuberPluginInterface",
]
