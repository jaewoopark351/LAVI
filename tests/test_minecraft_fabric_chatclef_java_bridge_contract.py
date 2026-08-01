#20260801_kpopmodder: Guard Phase 4 Java bridge tick-dispatch scope.
import json
import unittest
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[1]
RUNTIME_ROOT = (
    PROJECT_ROOT
    / "plugins"
    / "Minecraft"
    / "runtime"
    / "chatclef_fabric_1.20.1"
)
BRIDGE_ROOT = (
    RUNTIME_ROOT
    / "src"
    / "main"
    / "java"
    / "lavi"
    / "minecraft"
    / "fabric"
    / "chatclef"
    / "bridge"
)


class MinecraftFabricChatClefJavaBridgeContractTests(unittest.TestCase):
    def test_fabric_mod_json_registers_single_bridge_entrypoint(self):
        manifest = json.loads(
            (
                RUNTIME_ROOT / "src" / "main" / "resources" / "fabric.mod.json"
            ).read_text(encoding="utf-8")
        )

        main_entrypoints = manifest["entrypoints"]["main"]

        self.assertEqual(
            1,
            main_entrypoints.count(
                "lavi.minecraft.fabric.chatclef.bridge.FabricChatClefBridgeEntrypoint"
            ),
        )

    def test_java_bridge_is_fabric_owned_and_does_not_reference_future_backends(self):
        self.assertTrue(BRIDGE_ROOT.exists())
        banned_fragments = ("forge", "minemind", "MineMind")

        for path in BRIDGE_ROOT.rglob("*.java"):
            text = path.read_text(encoding="utf-8")
            for fragment in banned_fragments:
                self.assertNotIn(fragment, text, f"{path} references {fragment}")

    def test_phase_4_bridge_keeps_command_execution_in_tick_dispatcher(self):
        banned_fragments = (
            "TaskRunner",
            "adris.altoclef.tasks",
            "baritone",
            "PlayerInteractionFixChain",
            "InteractWithBlockTask",
        )

        for path in BRIDGE_ROOT.rglob("*.java"):
            text = path.read_text(encoding="utf-8")
            for fragment in banned_fragments:
                self.assertNotIn(fragment, text, f"{path} references {fragment}")

        dispatcher_text = (
            BRIDGE_ROOT
            / "command"
            / "FabricChatClefCommandDispatcher.java"
        ).read_text(encoding="utf-8")
        client_text = (
            BRIDGE_ROOT
            / "transport"
            / "FabricChatClefBridgeClient.java"
        ).read_text(encoding="utf-8")

        self.assertIn("CommandExecutor", dispatcher_text)
        self.assertIn("executor.execute(", dispatcher_text)
        self.assertNotIn("CommandExecutor", client_text)
        self.assertNotIn("executor.execute(", client_text)

    def test_bridge_uses_new_endpoint_not_legacy_player2_endpoint(self):
        config_text = (
            BRIDGE_ROOT
            / "config"
            / "FabricChatClefBridgeConfig.java"
        ).read_text(encoding="utf-8")

        self.assertIn("ws://127.0.0.1:4316", config_text)
        self.assertNotIn("4315", config_text)

    def test_handshake_declares_tick_dispatched_command_capability(self):
        factory_text = (
            BRIDGE_ROOT
            / "protocol"
            / "FabricChatClefBridgeMessageFactory.java"
        ).read_text(encoding="utf-8")

        self.assertIn('capabilities.put("chatclef_command_dispatch", true)', factory_text)
        self.assertIn('metadata.put("phase", "phase_4_tick_dispatch")', factory_text)

    def test_entrypoint_registers_end_client_tick_dispatcher(self):
        entrypoint_text = (
            BRIDGE_ROOT / "FabricChatClefBridgeEntrypoint.java"
        ).read_text(encoding="utf-8")

        self.assertIn("ClientTickEvents.END_CLIENT_TICK.register", entrypoint_text)
        self.assertIn("commandDispatcher::onEndClientTick", entrypoint_text)

    def test_bridge_result_fidelity_does_not_treat_callback_as_completed(self):
        dispatcher_text = (
            BRIDGE_ROOT
            / "command"
            / "FabricChatClefCommandDispatcher.java"
        ).read_text(encoding="utf-8")
        result_text = (
            BRIDGE_ROOT
            / "command"
            / "FabricChatClefCommandResult.java"
        ).read_text(encoding="utf-8")
        execution_root = BRIDGE_ROOT / "command" / "execution"

        self.assertTrue(execution_root.exists())
        self.assertIn("FabricChatClefCommandExecution", dispatcher_text)
        self.assertIn("execution.runningResult()", dispatcher_text)
        self.assertIn("unknownAfterFinish", dispatcher_text)
        self.assertIn("failedFromCommandException", dispatcher_text)
        self.assertIn("failedFromDispatchException", dispatcher_text)
        self.assertNotIn("FabricChatClefCommandResult.completed(", dispatcher_text)
        self.assertIn('"running"', result_text)
        self.assertIn('"unknown"', result_text)


if __name__ == "__main__":
    unittest.main()
