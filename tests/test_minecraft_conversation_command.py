#20260725_kpopmodder: Covers explicit Minecraft commands routed from LAVI chat input.
import unittest

from app_core.extensions.minecraft_core import (
    MinecraftConversationCommandHandler,
    MinecraftConversationCommandParser,
)


class FakeMinecraftExtension:
    def __init__(self, response=None):
        self.commands = []
        self.response = response or {
            "ok": True,
            "accepted": True,
            "action": "get_item",
        }

    def handle_command(self, command):
        self.commands.append(command)
        return self.response


class FakeExtensionRegistry:
    def __init__(self, extension=None):
        self.extension = extension
        self.names = []

    def get(self, name):
        self.names.append(name)
        return self.extension


class MinecraftConversationCommandParserTests(unittest.TestCase):
    def test_parses_explicit_prefix_command(self):
        route = MinecraftConversationCommandParser().parse(
            "minecraft get oak_log 1",
        )

        self.assertIsNotNone(route)
        self.assertEqual("minecraft", route.game)
        self.assertEqual("get oak_log 1", route.command)
        self.assertEqual("prefix", route.trigger)

    def test_parses_suffixed_goto_command(self):
        route = MinecraftConversationCommandParser().parse(
            "go to 0 64 0 in Minecraft",
        )

        self.assertIsNotNone(route)
        self.assertEqual("go to 0 64 0", route.command)
        self.assertEqual("suffix", route.trigger)

    def test_parses_stop_action_suffix(self):
        route = MinecraftConversationCommandParser().parse(
            "stop Minecraft action",
        )

        self.assertIsNotNone(route)
        self.assertEqual("stop", route.command)

    def test_parses_suffixed_equip_command(self):
        route = MinecraftConversationCommandParser().parse(
            "equip iron_pickaxe in Minecraft",
        )

        self.assertIsNotNone(route)
        self.assertEqual("equip iron_pickaxe", route.command)
        self.assertEqual("suffix", route.trigger)

    def test_parses_suffixed_get_and_equip_command(self):
        route = MinecraftConversationCommandParser().parse(
            "get and equip diamond_pickaxe in Minecraft",
        )

        self.assertIsNotNone(route)
        self.assertEqual("get and equip diamond_pickaxe", route.command)
        self.assertEqual("suffix", route.trigger)

    def test_parses_suffixed_craft_command(self):
        route = MinecraftConversationCommandParser().parse(
            "craft stick 4 in Minecraft",
        )

        self.assertIsNotNone(route)
        self.assertEqual("craft stick 4", route.command)
        self.assertEqual("suffix", route.trigger)

    def test_ignores_general_minecraft_chat(self):
        route = MinecraftConversationCommandParser().parse(
            "tell me about Minecraft",
        )

        self.assertIsNone(route)


class MinecraftConversationCommandHandlerTests(unittest.TestCase):
    def test_dispatches_to_minecraft_extension(self):
        extension = FakeMinecraftExtension()
        registry = FakeExtensionRegistry(extension)
        handler = MinecraftConversationCommandHandler(registry)

        response = handler.try_handle("minecraft get oak_log 1")

        self.assertEqual("Minecraft command accepted: get_item", response)
        self.assertEqual(["minecraft"], registry.names)
        self.assertEqual(["get oak_log 1"], extension.commands)

    def test_returns_none_for_non_minecraft_command(self):
        extension = FakeMinecraftExtension()
        handler = MinecraftConversationCommandHandler(
            FakeExtensionRegistry(extension),
        )

        self.assertIsNone(handler.try_handle("hello LAVI"))
        self.assertEqual([], extension.commands)


if __name__ == "__main__":
    unittest.main()
