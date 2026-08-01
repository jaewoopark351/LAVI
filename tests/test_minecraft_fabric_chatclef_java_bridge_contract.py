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

    def test_java_bridge_binds_results_to_connection_generation_and_context(self):
        client_text = (
            BRIDGE_ROOT
            / "transport"
            / "FabricChatClefBridgeClient.java"
        ).read_text(encoding="utf-8")
        queue_text = (
            BRIDGE_ROOT
            / "command"
            / "FabricChatClefCommandQueue.java"
        ).read_text(encoding="utf-8")
        sender_text = (
            BRIDGE_ROOT
            / "command"
            / "FabricChatClefCommandResultSender.java"
        ).read_text(encoding="utf-8")
        context_text = (
            BRIDGE_ROOT
            / "command"
            / "FabricChatClefCommandContext.java"
        ).read_text(encoding="utf-8")

        self.assertIn("AtomicLong connectionGenerations", client_text)
        self.assertIn("activeConnectionGeneration", client_text)
        self.assertIn("isCurrentSocket", client_text)
        self.assertIn("detachConnection(generation", client_text)
        self.assertIn("generation != activeConnectionGeneration", client_text)
        self.assertIn("FabricChatClefCommandContext context", client_text)

        self.assertIn("FabricChatClefCommandContext", sender_text)
        self.assertIn("AtomicReference<FabricChatClefCommandContext>", queue_text)
        self.assertIn("compareAndSet(context, null)", queue_text)
        self.assertIn("connectionGeneration", context_text)
        self.assertIn("correlationId", context_text)
        self.assertIn("sessionId", context_text)
        self.assertIn("AtomicBoolean terminalSent", context_text)

    def test_java_dispatcher_checks_active_deadline_before_busy_return(self):
        dispatcher_text = (
            BRIDGE_ROOT
            / "command"
            / "FabricChatClefCommandDispatcher.java"
        ).read_text(encoding="utf-8")
        active_context_index = dispatcher_text.index("commandQueue.activeContext()")
        deadline_index = dispatcher_text.index("completeActiveDeadline(context)")
        engine_ready_index = dispatcher_text.index("if (!isEngineReady())")

        self.assertLess(active_context_index, engine_ready_index)
        self.assertLess(deadline_index, engine_ready_index)
        self.assertNotIn("if (commandQueue.hasActive()) {\n            return;", dispatcher_text)

    def test_java_bridge_rejects_session_mismatch_before_queueing_command(self):
        client_text = (
            BRIDGE_ROOT
            / "transport"
            / "FabricChatClefBridgeClient.java"
        ).read_text(encoding="utf-8")

        session_check_index = client_text.index("command_request session does not match")
        offer_index = client_text.index("commandQueue.offer(context)")

        self.assertLess(session_check_index, offer_index)


if __name__ == "__main__":
    unittest.main()
