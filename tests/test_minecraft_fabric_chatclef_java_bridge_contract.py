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
            "adris.altoclef.tasks.",
            "baritone",
            "PlayerInteractionFixChain",
            "InteractWithBlockTask",
        )

        for path in (BRIDGE_ROOT / "transport").rglob("*.java"):
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
        capabilities_text = (
            BRIDGE_ROOT
            / "protocol"
            / "handshake"
            / "FabricChatClefHandshakeCapabilitiesPayload.java"
        ).read_text(encoding="utf-8")
        metadata_text = (
            BRIDGE_ROOT
            / "protocol"
            / "handshake"
            / "FabricChatClefHandshakeMetadataPayload.java"
        ).read_text(encoding="utf-8")

        self.assertIn('payload.put("chatclef_command_dispatch", chatClefCommandDispatch)', capabilities_text)
        self.assertIn('"phase_4_tick_dispatch"', metadata_text)

    def test_entrypoint_registers_end_client_tick_dispatcher(self):
        entrypoint_text = (
            BRIDGE_ROOT / "FabricChatClefBridgeEntrypoint.java"
        ).read_text(encoding="utf-8")

        self.assertIn("ClientTickEvents.END_CLIENT_TICK.register", entrypoint_text)
        self.assertIn("components.commandDispatcher()::onEndClientTick", entrypoint_text)

    def test_bridge_result_fidelity_combines_callback_with_task_finished_event(self):
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
        self.assertNotIn("unknownAfterFinish", dispatcher_text)
        self.assertIn("completeCommandException", dispatcher_text)
        self.assertIn("completeDispatchException", dispatcher_text)
        self.assertNotIn("FabricChatClefCommandResult.completed(", dispatcher_text)
        status_text = (
            BRIDGE_ROOT
            / "command"
            / "result"
            / "FabricChatClefCommandResultStatus.java"
        ).read_text(encoding="utf-8")

        self.assertIn('RUNNING("running", true)', status_text)
        self.assertIn('UNKNOWN("unknown", false)', status_text)

        lifecycle_root = BRIDGE_ROOT / "command" / "lifecycle"
        observer_text = (
            lifecycle_root / "FabricChatClefUserTaskFinishedObserver.java"
        ).read_text(encoding="utf-8")
        observation_text = (
            lifecycle_root / "FabricChatClefCommandTerminationObservation.java"
        ).read_text(encoding="utf-8")
        classifier_text = (
            lifecycle_root / "FabricChatClefCommandOutcomeClassifier.java"
        ).read_text(encoding="utf-8")
        execution_text = (
            execution_root / "FabricChatClefCommandExecution.java"
        ).read_text(encoding="utf-8")
        execution_state_text = (
            execution_root / "FabricChatClefCommandExecutionState.java"
        ).read_text(encoding="utf-8")

        self.assertIn("failedFromCommandException", execution_text)
        self.assertIn("failedFromDispatchException", execution_text)
        self.assertIn("EventBus.subscribe(TaskFinishedEvent.class", observer_text)
        self.assertIn("ConcurrentLinkedQueue", observer_text)
        self.assertIn("catch (Throwable error)", observer_text)
        self.assertIn("task.stopped()", observation_text)
        self.assertNotIn(".isFinished()", observation_text)
        self.assertIn("finishCallbackReceived()", classifier_text)
        self.assertIn("matchesBoundRootTask", classifier_text)
        self.assertIn("completedFromTaskFinished", classifier_text)
        self.assertIn("observation.task() == boundRootTask", execution_state_text)

    def test_entrypoint_registers_lifecycle_observer_without_engine_modification(self):
        entrypoint_text = (
            BRIDGE_ROOT / "FabricChatClefBridgeEntrypoint.java"
        ).read_text(encoding="utf-8")
        components_text = (
            BRIDGE_ROOT
            / "runtime"
            / "FabricChatClefBridgeComponents.java"
        ).read_text(encoding="utf-8")
        lifecycle_root = BRIDGE_ROOT / "command" / "lifecycle"
        coordinator_text = (
            lifecycle_root / "FabricChatClefCommandLifecycleCoordinator.java"
        ).read_text(encoding="utf-8")
        outbox_text = (
            lifecycle_root / "FabricChatClefCommandResultOutbox.java"
        ).read_text(encoding="utf-8")

        self.assertIn("components.taskFinishedObserver().register()", entrypoint_text)
        self.assertIn("FabricChatClefCommandLifecycleCoordinator", components_text)
        self.assertIn("onEndClientTick", coordinator_text)
        self.assertIn("resultOutbox.sendTerminal", coordinator_text)
        self.assertIn("beginTerminalSend(System.currentTimeMillis())", outbox_text)
        self.assertIn("drainSendCompletions", outbox_text)
        self.assertIn("completeTerminalSend(outcome)", outbox_text)
        self.assertIn("commandQueue.complete(context, \"terminal_result\")", outbox_text)

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
        self.assertIn("enqueueConnectionDetached(generation", client_text)
        self.assertNotIn("detachConnection(generation", client_text)
        self.assertIn("generation != activeConnectionGeneration", client_text)
        self.assertIn("FabricChatClefCommandContext context", client_text)

        self.assertIn("FabricChatClefCommandContext", sender_text)
        self.assertIn("Deque<FabricChatClefCommandContext>", queue_text)
        self.assertIn("Deque<FabricChatClefConnectionDetachedEvent>", queue_text)
        self.assertIn("Deque<FabricChatClefCommandResultSendCompletion>", queue_text)
        self.assertIn("clearDetachedActive", queue_text)
        self.assertIn("enqueueCommandResultSendCompletion", queue_text)
        self.assertIn("connectionGeneration", context_text)
        self.assertIn("correlationId", context_text)
        self.assertIn("sessionId", context_text)
        self.assertIn("AtomicBoolean terminalSendInFlight", context_text)
        self.assertIn("AtomicBoolean terminalSent", context_text)
        self.assertIn("terminalSendReady", context_text)

    def test_disconnect_is_processed_as_client_tick_control_event(self):
        client_text = (
            BRIDGE_ROOT
            / "transport"
            / "FabricChatClefBridgeClient.java"
        ).read_text(encoding="utf-8")
        dispatcher_text = (
            BRIDGE_ROOT
            / "command"
            / "FabricChatClefCommandDispatcher.java"
        ).read_text(encoding="utf-8")
        queue_text = (
            BRIDGE_ROOT
            / "command"
            / "FabricChatClefCommandQueue.java"
        ).read_text(encoding="utf-8")

        self.assertIn("enqueueConnectionDetached(generation, \"websocket_closed\")", client_text)
        self.assertIn("enqueueConnectionDetached(generation, \"websocket_error\")", client_text)
        self.assertNotIn("detachConnection", client_text)
        self.assertIn("processConnectionDetachedEvents()", dispatcher_text)
        self.assertIn("matchesBoundRootTask(context, currentTask)", dispatcher_text)
        self.assertIn("cancelUserTaskForDetachedCommand(rootMatchReason)", dispatcher_text)
        self.assertIn("connection_detached_task_not_owned", dispatcher_text)
        self.assertIn("mod.cancelUserTask()", dispatcher_text)
        self.assertIn("clearDetachedExecution(context, event.reason() + \":\" + rootMatchReason)", dispatcher_text)
        self.assertIn("clearDetachedActive", queue_text)

    def test_command_queue_does_not_read_live_task_state_under_monitor(self):
        queue_text = (
            BRIDGE_ROOT
            / "command"
            / "FabricChatClefCommandQueue.java"
        ).read_text(encoding="utf-8")

        self.assertNotIn("FabricChatClefCommandContextUnbindDiagnostics", queue_text)
        self.assertNotIn("captureOwnershipSnapshot", queue_text)
        self.assertNotIn("AltoClef.getInstance", queue_text)
        self.assertNotIn("getUserTaskChain", queue_text)
        self.assertIn("markConnectionDetached", queue_text)
        self.assertIn("connectionDetachedEvents.offer", queue_text)

    def test_terminal_result_clear_happens_after_explicit_send_outcome(self):
        outbox_text = (
            BRIDGE_ROOT
            / "command"
            / "lifecycle"
            / "FabricChatClefCommandResultOutbox.java"
        ).read_text(encoding="utf-8")
        sender_text = (
            BRIDGE_ROOT
            / "transport"
            / "FabricChatClefResultEnvelopeSender.java"
        ).read_text(encoding="utf-8")
        interface_text = (
            BRIDGE_ROOT
            / "command"
            / "FabricChatClefCommandResultSender.java"
        ).read_text(encoding="utf-8")

        send_index = outbox_text.index("resultSender.sendTerminalCommandResult(context, result)")
        success_check_index = outbox_text.index("if (!submission.acceptedForAsyncSend())")
        completion_index = outbox_text.index("context.completeTerminalSend(outcome)")
        clear_index = outbox_text.index("commandQueue.complete(context, \"terminal_result\")")

        self.assertLess(send_index, success_check_index)
        self.assertLess(completion_index, clear_index)
        self.assertIn("FabricChatClefCommandResultSendSubmission", interface_text)
        self.assertIn("sendTerminalCommandResult", interface_text)
        self.assertIn("socket.sendText(message, true)", sender_text)
        self.assertIn(".whenComplete((ignored, error)", sender_text)
        self.assertNotIn(".toCompletableFuture().join()", sender_text)
        self.assertIn("ASYNC_SEND_FAILED", sender_text)
        self.assertIn("enqueueCompletion(context, outcome)", sender_text)

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
        handler_text = (
            BRIDGE_ROOT
            / "transport"
            / "inbound"
            / "FabricChatClefCommandRequestHandler.java"
        ).read_text(encoding="utf-8")

        session_check_index = handler_text.index("command_request session does not match")
        offer_index = handler_text.index("commandQueue.offer(context)")

        self.assertLess(session_check_index, offer_index)


if __name__ == "__main__":
    unittest.main()
