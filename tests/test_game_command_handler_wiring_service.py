#20260725_kpopmodder: Covers focused wiring for LLM game command handlers.
import unittest

from app_core.composition_core.game_command_handler_wiring_service import (
    GameCommandHandlerWiringService,
)
from app_core.extensions.minecraft_core import MinecraftConversationCommandHandler


class FakeLLM:
    def __init__(self):
        self.handler = "unset"

    def set_game_command_handler(self, handler):
        self.handler = handler

    def emit_background_response(self, text):
        return text


class FakeRegistry:
    pass


class ObjectWithoutSetter:
    pass


class GameCommandHandlerWiringServiceTests(unittest.TestCase):
    def test_wires_minecraft_conversation_handler_to_llm(self):
        llm = FakeLLM()
        registry = FakeRegistry()

        GameCommandHandlerWiringService().wire(
            llm=llm,
            game_extension_registry=registry,
        )

        self.assertIsInstance(llm.handler, MinecraftConversationCommandHandler)
        self.assertIs(llm.handler.extension_registry, registry)
        self.assertTrue(llm.handler.async_command_runner.can_notify)

    def test_clears_handler_when_registry_is_missing(self):
        llm = FakeLLM()

        GameCommandHandlerWiringService().wire(llm=llm)

        self.assertIsNone(llm.handler)

    def test_ignores_llm_without_setter(self):
        GameCommandHandlerWiringService().wire(
            llm=ObjectWithoutSetter(),
            game_extension_registry=FakeRegistry(),
        )


if __name__ == "__main__":
    unittest.main()
