#20260725_kpopmodder: Covers explicit Minecraft commands routed from LAVI chat input.
import unittest

from app_core.extensions.minecraft_core import (
    MinecraftConversationCommandHandler,
    MinecraftConversationCommandParser,
    MinecraftImplicitConversationCommandParser,
)

class FakeMinecraftExtension:
    def __init__(self, response=None, status=None):
        self.commands = []
        self.response = response or {
            "ok": True,
            "accepted": True,
            "action": "get_item",
        }
        self.status = status or {
            "ok": True,
            "enabled": True,
            "allow_actions": True,
            "bridge": {
                "ok": True,
                "in_game": True,
            },
            "extension": {
                "started": True,
            },
        }

    def handle_command(self, command):
        self.commands.append(command)
        return self.response

    def get_status(self):
        return self.status


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

    def test_parses_korean_minecraft_prefix_command(self):
        route = MinecraftConversationCommandParser().parse(
            "\ub9c8\ud06c\uc5d0\uc11c \ucca0 \uace1\uad2d\uc774 "
            "\uc7a5\ucc29\ud574",
        )

        self.assertIsNotNone(route)
        self.assertEqual("minecraft", route.game)
        self.assertEqual(
            "\ucca0 \uace1\uad2d\uc774 \uc7a5\ucc29\ud574",
            route.command,
        )
        self.assertEqual("korean_prefix", route.trigger)

    def test_parses_korean_minecraft_suffix_command(self):
        route = MinecraftConversationCommandParser().parse(
            "\ub9c9\ub300\uae30 4\uac1c \ub9cc\ub4e4\uc5b4 "
            "\ub9c8\ud06c\uc5d0\uc11c",
        )

        self.assertIsNotNone(route)
        self.assertEqual(
            "\ub9c9\ub300\uae30 4\uac1c \ub9cc\ub4e4\uc5b4",
            route.command,
        )
        self.assertEqual("suffix", route.trigger)

    def test_ignores_general_minecraft_chat(self):
        route = MinecraftConversationCommandParser().parse(
            "tell me about Minecraft",
        )

        self.assertIsNone(route)

    def test_ignores_general_korean_minecraft_chat(self):
        route = MinecraftConversationCommandParser().parse(
            "\ub9c8\ud06c\uc5d0\uc11c \ubb50 \ud560 \uc218 \uc788\uc5b4?",
        )

        self.assertIsNone(route)


class MinecraftImplicitConversationCommandParserTests(unittest.TestCase):
    def test_parses_bare_korean_item_command(self):
        route = MinecraftImplicitConversationCommandParser().parse(
            "\ucca0 \uace1\uad2d\uc774 \ub9cc\ub4e4\uace0 \uc7a5\ucc29\ud574",
        )

        self.assertIsNotNone(route)
        self.assertEqual("minecraft", route.game)
        self.assertEqual(
            "\ucca0 \uace1\uad2d\uc774 \ub9cc\ub4e4\uace0 \uc7a5\ucc29\ud574",
            route.command,
        )
        self.assertEqual("implicit_active", route.trigger)

    def test_parses_bare_english_command(self):
        route = MinecraftImplicitConversationCommandParser().parse("craft stick 4")

        self.assertIsNotNone(route)
        self.assertEqual("craft stick 4", route.command)
        self.assertEqual("implicit_active", route.trigger)

    def test_ignores_obviously_unrelated_command(self):
        route = MinecraftImplicitConversationCommandParser().parse(
            "\ud30c\uc774\uc36c \ucf54\ub4dc \ub9cc\ub4e4\uc5b4\uc918",
        )

        self.assertIsNone(route)


class MinecraftConversationCommandHandlerTests(unittest.TestCase):
    def test_dispatches_to_minecraft_extension(self):
        extension = FakeMinecraftExtension(
            response={
                "ok": True,
                "accepted": True,
                "action": {
                    "type": "get-item",
                    "request": {"item": "oak_log", "count": 1},
                },
            },
        )
        registry = FakeExtensionRegistry(extension)
        handler = MinecraftConversationCommandHandler(registry)

        response = handler.try_handle("minecraft get oak_log 1")

        self.assertEqual(
            "\uc751. \ucc38\ub098\ubb34 \uc6d0\ubaa9 \uad6c\ud574\ubcfc\uac8c.",
            response,
        )
        self.assertEqual(["minecraft"], registry.names)
        self.assertEqual(["get oak_log 1"], extension.commands)

    def test_dispatches_korean_minecraft_command_to_extension(self):
        extension = FakeMinecraftExtension(
            response={
                "ok": True,
                "accepted": True,
                "action": {
                    "type": "equip",
                    "request": {"item": "iron_pickaxe"},
                },
            },
        )
        registry = FakeExtensionRegistry(extension)
        handler = MinecraftConversationCommandHandler(registry)

        response = handler.try_handle(
            "\ub9c8\ud06c\uc5d0\uc11c \ucca0 \uace1\uad2d\uc774 "
            "\uc7a5\ucc29\ud574"
        )

        self.assertEqual(
            "\uc751. \ucca0 \uace1\uad2d\uc774 \ub4e4\uc5b4\ubcfc\uac8c.",
            response,
        )
        self.assertEqual(["minecraft"], registry.names)
        self.assertEqual(
            ["\ucca0 \uace1\uad2d\uc774 \uc7a5\ucc29\ud574"],
            extension.commands,
        )

    def test_dispatches_implicit_korean_command_when_minecraft_is_active(self):
        extension = FakeMinecraftExtension(
            response={
                "ok": True,
                "accepted": True,
                "action": {
                    "type": "get-and-equip",
                    "request": {"item": "iron_pickaxe", "count": 1},
                },
            },
        )
        registry = FakeExtensionRegistry(extension)
        handler = MinecraftConversationCommandHandler(registry)

        response = handler.try_handle(
            "\ucca0 \uace1\uad2d\uc774 \ub9cc\ub4e4\uace0 \uc7a5\ucc29\ud574"
        )

        self.assertEqual(
            "\uc751. \ucca0 \uace1\uad2d\uc774 "
            "\ucc59\uaca8\uc11c \uc7a5\ucc29\ud574\ubcfc\uac8c.",
            response,
        )
        self.assertEqual(
            ["\ucca0 \uace1\uad2d\uc774 \ub9cc\ub4e4\uace0 \uc7a5\ucc29\ud574"],
            extension.commands,
        )

    def test_dispatches_implicit_korean_craft_command_with_human_reply(self):
        extension = FakeMinecraftExtension(
            response={
                "ok": True,
                "accepted": True,
                "action": {
                    "type": "craft",
                    "request": {"item": "stick", "count": 4},
                },
            },
        )
        registry = FakeExtensionRegistry(extension)
        handler = MinecraftConversationCommandHandler(registry)

        response = handler.try_handle("\ub9c9\ub300\uae30 4\uac1c \ub9cc\ub4e4\uc5b4\uc918.")

        self.assertEqual(
            "\uc751. \ub9c9\ub300\uae30 4\uac1c \ub9cc\ub4e4\uc5b4\ubcfc\uac8c.",
            response,
        )
        self.assertEqual(
            ["\ub9c9\ub300\uae30 4\uac1c \ub9cc\ub4e4\uc5b4\uc918."],
            extension.commands,
        )

    def test_formats_completed_korean_get_item_reply_from_completion_action(self):
        extension = FakeMinecraftExtension(
            response={
                "ok": True,
                "accepted": True,
                "action": "get_item",
                "completion": {
                    "action": {
                        "type": "get-item",
                        "request": {"item": "oak_log", "count": 10},
                    }
                },
            },
        )
        registry = FakeExtensionRegistry(extension)
        handler = MinecraftConversationCommandHandler(registry)

        response = handler.try_handle("\ub098\ubb34 \uc5f4 \uac1c \uce90\uc918.")

        self.assertEqual(
            "\uc751. \ucc38\ub098\ubb34 \uc6d0\ubaa9 10\uac1c \uad6c\ud574\ubcfc\uac8c.",
            response,
        )

    def test_ignores_implicit_command_when_minecraft_is_not_in_game(self):
        extension = FakeMinecraftExtension(
            status={
                "ok": True,
                "enabled": True,
                "allow_actions": True,
                "bridge": {
                    "ok": True,
                    "in_game": False,
                },
                "extension": {
                    "started": True,
                },
            }
        )
        registry = FakeExtensionRegistry(extension)
        handler = MinecraftConversationCommandHandler(registry)

        self.assertIsNone(
            handler.try_handle(
                "\ucca0 \uace1\uad2d\uc774 \ub9cc\ub4e4\uace0 \uc7a5\ucc29\ud574"
            )
        )
        self.assertEqual([], extension.commands)

    def test_returns_none_for_non_minecraft_command(self):
        extension = FakeMinecraftExtension()
        handler = MinecraftConversationCommandHandler(
            FakeExtensionRegistry(extension),
        )

        self.assertIsNone(handler.try_handle("hello LAVI"))
        self.assertEqual([], extension.commands)

    def test_returns_none_for_obviously_unrelated_implicit_command(self):
        extension = FakeMinecraftExtension()
        handler = MinecraftConversationCommandHandler(
            FakeExtensionRegistry(extension),
        )

        self.assertIsNone(
            handler.try_handle("\ud30c\uc774\uc36c \ucf54\ub4dc \ub9cc\ub4e4\uc5b4\uc918")
        )
        self.assertEqual([], extension.commands)

    def test_hides_internal_completion_failure_codes(self):
        extension = FakeMinecraftExtension(
            response={"ok": False, "error": "action_completion_failed"},
        )
        handler = MinecraftConversationCommandHandler(
            FakeExtensionRegistry(extension),
        )

        response = handler.try_handle("minecraft get oak_log 1")

        self.assertNotIn("action_completion_failed", response)
        self.assertIn("\ud655\uc778", response)


if __name__ == "__main__":
    unittest.main()
