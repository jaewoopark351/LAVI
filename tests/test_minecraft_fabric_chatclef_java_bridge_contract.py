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
        ordinary_dispatcher_text = (
            BRIDGE_ROOT
            / "command"
            / "FabricChatClefOrdinaryCommandDispatchCoordinator.java"
        ).read_text(encoding="utf-8")
        executor_invocation_text = (
            BRIDGE_ROOT
            / "command"
            / "FabricChatClefOrdinaryCommandExecutorInvocation.java"
        ).read_text(encoding="utf-8")
        client_text = (
            BRIDGE_ROOT
            / "transport"
            / "FabricChatClefBridgeClient.java"
        ).read_text(encoding="utf-8")

        self.assertIn("ordinaryCommandDispatcher::dispatch", dispatcher_text)
        self.assertIn("CommandExecutor", ordinary_dispatcher_text)
        self.assertIn("executorInvocation.invoke", ordinary_dispatcher_text)
        self.assertIn("executor.execute(", executor_invocation_text)
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
        ordinary_dispatcher_text = (
            BRIDGE_ROOT
            / "command"
            / "FabricChatClefOrdinaryCommandDispatchCoordinator.java"
        ).read_text(encoding="utf-8")
        running_result_publisher_text = (
            BRIDGE_ROOT
            / "command"
            / "FabricChatClefOrdinaryCommandRunningResultPublisher.java"
        ).read_text(encoding="utf-8")
        executor_invocation_text = (
            BRIDGE_ROOT
            / "command"
            / "FabricChatClefOrdinaryCommandExecutorInvocation.java"
        ).read_text(encoding="utf-8")
        execution_root = BRIDGE_ROOT / "command" / "execution"

        self.assertTrue(execution_root.exists())
        self.assertIn("ordinaryCommandDispatcher::dispatch", dispatcher_text)
        self.assertIn("FabricChatClefCommandExecution", ordinary_dispatcher_text)
        self.assertIn("runningResultPublisher.publish", ordinary_dispatcher_text)
        self.assertIn("executorInvocation.invoke", ordinary_dispatcher_text)
        self.assertIn("execution.runningResult()", running_result_publisher_text)
        self.assertNotIn("unknownAfterFinish", executor_invocation_text)
        self.assertIn("completeCommandException", executor_invocation_text)
        self.assertIn("completeDispatchException", executor_invocation_text)
        self.assertNotIn(
            "FabricChatClefCommandResult.completed(",
            running_result_publisher_text + executor_invocation_text,
        )
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
        terminal_dispatcher_text = (
            lifecycle_root
            / "terminal"
            / "FabricChatClefCommandTerminalResultDispatcher.java"
        ).read_text(encoding="utf-8")
        outbox_text = (
            lifecycle_root / "FabricChatClefCommandResultOutbox.java"
        ).read_text(encoding="utf-8")
        outbox_root = lifecycle_root / "outbox"
        terminal_submission_text = (
            outbox_root
            / "FabricChatClefTerminalResultSubmission.java"
        ).read_text(encoding="utf-8")
        terminal_committer_text = (
            outbox_root / "FabricChatClefTerminalResultCommitter.java"
        ).read_text(encoding="utf-8")
        completion_drainer_text = (
            outbox_root
            / "FabricChatClefCommandResultCompletionDrainer.java"
        ).read_text(encoding="utf-8")
        retirement_text = (
            outbox_root
            / "FabricChatClefCommandResultRetirementCommit.java"
        ).read_text(encoding="utf-8")

        self.assertIn("components.taskFinishedObserver().register()", entrypoint_text)
        self.assertIn("FabricChatClefCommandLifecycleCoordinator", components_text)
        self.assertIn("onEndClientTick", coordinator_text)
        self.assertIn(
            "components.tickCoordinator().onEndClientTick(activeContext)",
            coordinator_text,
        )
        self.assertIn("resultOutbox.sendTerminal", terminal_dispatcher_text)
        self.assertIn("drainSendCompletions", outbox_text)
        self.assertIn("terminalSubmission.submitActive", outbox_text)
        self.assertIn(
            "beginTerminalSend(System.currentTimeMillis())", terminal_committer_text
        )
        self.assertIn("committer.commit", terminal_submission_text)
        self.assertIn("ownershipValidator.validateActive", terminal_submission_text)
        self.assertIn("transport.submit", terminal_submission_text)
        self.assertIn("completeTerminalSend(outcome)", completion_drainer_text)
        self.assertIn(
            'commandQueue.complete(context, "terminal_result")', retirement_text
        )

    def test_java_bridge_binds_results_to_connection_generation_and_context(self):
        client_text = (
            BRIDGE_ROOT
            / "transport"
            / "FabricChatClefBridgeClient.java"
        ).read_text(encoding="utf-8")
        connection_state_text = (
            BRIDGE_ROOT
            / "transport"
            / "connection"
            / "FabricChatClefWebSocketConnectionState.java"
        ).read_text(encoding="utf-8")
        connection_lifecycle_state_text = (
            BRIDGE_ROOT
            / "transport"
            / "connection"
            / "FabricChatClefConnectionLifecycleState.java"
        ).read_text(encoding="utf-8")
        connection_lifecycle_text = (
            BRIDGE_ROOT
            / "transport"
            / "connection"
            / "FabricChatClefWebSocketConnectionLifecycle.java"
        ).read_text(encoding="utf-8")
        connection_text_receiver_text = (
            BRIDGE_ROOT
            / "transport"
            / "connection"
            / "FabricChatClefConnectionTextReceiver.java"
        ).read_text(encoding="utf-8")
        connection_detach_handler_text = (
            BRIDGE_ROOT
            / "transport"
            / "connection"
            / "FabricChatClefConnectionDetachHandler.java"
        ).read_text(encoding="utf-8")
        connection_detach_notification_text = (
            BRIDGE_ROOT
            / "transport"
            / "connection"
            / "FabricChatClefConnectionDetachNotification.java"
        ).read_text(encoding="utf-8")
        queue_text = (
            BRIDGE_ROOT
            / "command"
            / "FabricChatClefCommandQueue.java"
        ).read_text(encoding="utf-8")
        ownership_state_text = (
            BRIDGE_ROOT
            / "command"
            / "queue"
            / "ownership"
            / "FabricChatClefCommandOwnershipState.java"
        ).read_text(encoding="utf-8")
        detach_queue_text = (
            BRIDGE_ROOT
            / "command"
            / "connection"
            / "detach"
            / "FabricChatClefConnectionDetachEventQueue.java"
        ).read_text(encoding="utf-8")
        result_completion_queue_text = (
            BRIDGE_ROOT
            / "command"
            / "result"
            / "send"
            / "FabricChatClefCommandResultSendCompletionQueue.java"
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
        terminal_state_text = (
            BRIDGE_ROOT
            / "command"
            / "result"
            / "send"
            / "FabricChatClefCommandTerminalResultState.java"
        ).read_text(encoding="utf-8")
        terminal_send_state_text = (
            BRIDGE_ROOT
            / "command"
            / "result"
            / "send"
            / "FabricChatClefCommandResultSendAttemptState.java"
        ).read_text(encoding="utf-8")

        self.assertIn("FabricChatClefConnectionLifecycleState", connection_state_text)
        self.assertIn("AtomicLong connectionGenerations", connection_lifecycle_state_text)
        self.assertIn("activeConnectionGeneration", connection_lifecycle_state_text)
        self.assertIn("isCurrentSocket", connection_state_text)
        self.assertIn(
            "enqueueConnectionDetached(generation", connection_detach_notification_text
        )
        self.assertNotIn("detachConnection(generation", connection_detach_handler_text)
        self.assertIn(
            "generation != connectionState.activeConnectionGeneration()",
            connection_text_receiver_text,
        )
        self.assertIn("textReceiver.onText", connection_lifecycle_text)
        self.assertIn("detachHandler.onClose", connection_lifecycle_text)
        self.assertIn("FabricChatClefCommandContext context", client_text)

        self.assertIn("FabricChatClefCommandContext", sender_text)
        self.assertIn(
            "Deque<FabricChatClefCommandContext>", ownership_state_text
        )
        self.assertIn(
            "Deque<FabricChatClefConnectionDetachedEvent>", detach_queue_text
        )
        self.assertIn(
            "Deque<FabricChatClefCommandResultSendCompletion>",
            result_completion_queue_text,
        )
        self.assertIn("clearDetachedActive", queue_text)
        self.assertIn("enqueueCommandResultSendCompletion", queue_text)
        self.assertIn("connectionGeneration", context_text)
        self.assertIn("correlationId", context_text)
        self.assertIn("sessionId", context_text)
        self.assertIn("FabricChatClefCommandTerminalResultState", context_text)
        self.assertIn("FabricChatClefCommandResultSendAttemptState", terminal_state_text)
        self.assertIn("AtomicBoolean sendInFlight", terminal_send_state_text)
        self.assertIn("AtomicBoolean sent", terminal_send_state_text)
        self.assertIn("terminalSendReady", context_text)

    def test_disconnect_is_processed_as_client_tick_control_event(self):
        connection_lifecycle_text = (
            BRIDGE_ROOT
            / "transport"
            / "connection"
            / "FabricChatClefWebSocketConnectionLifecycle.java"
        ).read_text(encoding="utf-8")
        connection_detach_handler_text = (
            BRIDGE_ROOT
            / "transport"
            / "connection"
            / "FabricChatClefConnectionDetachHandler.java"
        ).read_text(encoding="utf-8")
        connection_detach_notification_text = (
            BRIDGE_ROOT
            / "transport"
            / "connection"
            / "FabricChatClefConnectionDetachNotification.java"
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
        detach_coordinator_text = (
            BRIDGE_ROOT
            / "command"
            / "connection"
            / "FabricChatClefConnectionDetachCoordinator.java"
        ).read_text(encoding="utf-8")
        detach_resolver_text = (
            BRIDGE_ROOT
            / "command"
            / "connection"
            / "detach"
            / "FabricChatClefConnectionDetachDecisionResolver.java"
        ).read_text(encoding="utf-8")
        detach_retirement_text = (
            BRIDGE_ROOT
            / "command"
            / "connection"
            / "detach"
            / "FabricChatClefConnectionDetachRetirement.java"
        ).read_text(encoding="utf-8")
        cancellation_text = (
            BRIDGE_ROOT
            / "command"
            / "connection"
            / "detach"
            / "FabricChatClefAltoClefDetachedCommandCancellation.java"
        ).read_text(encoding="utf-8")

        self.assertIn(
            'notifyOwners(generation, "websocket_closed")',
            connection_detach_notification_text,
        )
        self.assertIn(
            'notifyOwners(generation, "websocket_error")',
            connection_detach_notification_text,
        )
        self.assertIn(
            "commandQueue.enqueueConnectionDetached(generation, reason)",
            connection_detach_notification_text,
        )
        self.assertNotIn("detachConnection", connection_detach_handler_text)
        self.assertIn("detachHandler.onClose", connection_lifecycle_text)
        self.assertIn(
            "connectionDetachCoordinator.processQueuedEvents()", dispatcher_text
        )
        self.assertIn(
            "matchesBoundRootTask(context, currentTask)", detach_resolver_text
        )
        self.assertIn(
            "Consumer<String> detachedCommandCancellationAction",
            detach_coordinator_text,
        )
        self.assertIn(
            "detachedCommandCancellationAction::accept", detach_coordinator_text
        )
        owned_branch_index = detach_retirement_text.index(
            "if (decision.ownsCurrentTask())"
        )
        delegate_index = detach_retirement_text.index(
            "cancellation.cancel(decision.rootMatchReason())", owned_branch_index
        )
        clear_index = detach_retirement_text.index(
            "lifecycleCoordinator.clearDetachedExecution", delegate_index
        )
        self.assertLess(owned_branch_index, delegate_index)
        self.assertLess(delegate_index, clear_index)
        self.assertIn(
            "connection_detached_task_not_owned", detach_retirement_text
        )
        self.assertIn("mod.cancelUserTask()", cancellation_text)
        self.assertIn(
            'event.reason() + ":" + decision.rootMatchReason()',
            detach_retirement_text,
        )
        self.assertIn("clearDetachedActive", queue_text)

    def test_command_queue_does_not_read_live_task_state_under_monitor(self):
        queue_text = (
            BRIDGE_ROOT
            / "command"
            / "FabricChatClefCommandQueue.java"
        ).read_text(encoding="utf-8")
        detach_queue_text = (
            BRIDGE_ROOT
            / "command"
            / "connection"
            / "detach"
            / "FabricChatClefConnectionDetachEventQueue.java"
        ).read_text(encoding="utf-8")

        self.assertNotIn("FabricChatClefCommandContextUnbindDiagnostics", queue_text)
        self.assertNotIn("captureOwnershipSnapshot", queue_text)
        self.assertNotIn("AltoClef.getInstance", queue_text)
        self.assertNotIn("getUserTaskChain", queue_text)
        self.assertIn("markConnectionDetached", queue_text)
        self.assertIn("detachEvents.enqueue", queue_text)
        self.assertIn("events.offer", detach_queue_text)

    def test_terminal_result_clear_happens_after_explicit_send_outcome(self):
        outbox_root = (
            BRIDGE_ROOT
            / "command"
            / "lifecycle"
            / "outbox"
        )
        terminal_submission_text = (
            outbox_root / "FabricChatClefTerminalResultSubmission.java"
        ).read_text(encoding="utf-8")
        transport_text = (
            outbox_root / "FabricChatClefTerminalResultTransport.java"
        ).read_text(encoding="utf-8")
        completion_text = (
            outbox_root
            / "FabricChatClefCommandResultCompletionDrainer.java"
        ).read_text(encoding="utf-8")
        retirement_text = (
            outbox_root
            / "FabricChatClefCommandResultRetirementCommit.java"
        ).read_text(encoding="utf-8")
        sender_text = (
            BRIDGE_ROOT
            / "transport"
            / "FabricChatClefResultEnvelopeSender.java"
        ).read_text(encoding="utf-8")
        transport_submission_text = (
            BRIDGE_ROOT
            / "transport"
            / "result"
            / "FabricChatClefResultTransportSubmission.java"
        ).read_text(encoding="utf-8")
        async_completion_text = (
            BRIDGE_ROOT
            / "transport"
            / "result"
            / "FabricChatClefResultAsyncCompletion.java"
        ).read_text(encoding="utf-8")
        completion_router_text = (
            BRIDGE_ROOT
            / "transport"
            / "result"
            / "FabricChatClefResultSendCompletionRouter.java"
        ).read_text(encoding="utf-8")
        interface_text = (
            BRIDGE_ROOT
            / "command"
            / "FabricChatClefCommandResultSender.java"
        ).read_text(encoding="utf-8")

        send_index = transport_text.index(
            "resultSender.sendTerminalCommandResult(context, result)"
        )
        success_check_index = transport_text.index(
            "if (!submission.acceptedForAsyncSend())"
        )
        completion_index = completion_text.index("context.completeTerminalSend(outcome)")
        retire_index = completion_text.index("retirementCommit.retire(")

        self.assertLess(send_index, success_check_index)
        self.assertIn("committer.commit", terminal_submission_text)
        self.assertIn("ownershipValidator.validate", terminal_submission_text)
        self.assertIn("transport.submit", terminal_submission_text)
        self.assertLess(completion_index, retire_index)
        self.assertIn(
            'commandQueue.complete(context, "terminal_result")', retirement_text
        )
        self.assertIn("FabricChatClefCommandResultSendSubmission", interface_text)
        self.assertIn("sendTerminalCommandResult", interface_text)
        self.assertIn("transportSubmission.submit", sender_text)
        self.assertIn("socket.sendText(message, true)", transport_submission_text)
        self.assertIn(".whenComplete(", transport_submission_text)
        self.assertNotIn(
            ".toCompletableFuture().join()",
            sender_text + transport_submission_text,
        )
        self.assertIn("ASYNC_SEND_FAILED", async_completion_text)
        self.assertIn("ordinaryCompletionSink.accept", completion_router_text)

    def test_java_dispatcher_checks_active_deadline_before_busy_return(self):
        dispatcher_text = (
            BRIDGE_ROOT
            / "command"
            / "FabricChatClefCommandDispatcher.java"
        ).read_text(encoding="utf-8")
        active_context_index = dispatcher_text.index("commandQueue.activeContext()")
        deadline_index = dispatcher_text.index("completeActiveDeadline(context)")
        engine_ready_index = dispatcher_text.index(
            "if (!ordinaryCommandDispatcher.isEngineReady())"
        )

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
        command_root = BRIDGE_ROOT / "transport" / "inbound" / "command"
        admission_text = (
            command_root
            / "admission"
            / "FabricChatClefCommandRequestAdmission.java"
        ).read_text(encoding="utf-8")
        session_admission_text = (
            command_root
            / "admission"
            / "FabricChatClefCommandSessionAdmission.java"
        ).read_text(encoding="utf-8")
        enqueuer_text = (
            command_root
            / "FabricChatClefCommandRequestEnqueuer.java"
        ).read_text(encoding="utf-8")
        queue_admission_text = (
            command_root
            / "FabricChatClefCommandRequestQueueAdmission.java"
        ).read_text(encoding="utf-8")

        admission_index = handler_text.index("admission.admitSession")
        offer_index = handler_text.index("enqueuer.offer")

        self.assertLess(admission_index, offer_index)
        self.assertIn("sessionAdmission.admit", admission_text)
        self.assertIn("command_request session does not match", session_admission_text)
        self.assertIn("queueAdmission.offer(context)", enqueuer_text)
        self.assertIn("commandQueue.offer(context)", queue_admission_text)

    def test_inbound_and_session_flow_are_split_by_responsibility(self):
        inbound_root = BRIDGE_ROOT / "transport" / "inbound"
        inbound_facade_text = (
            inbound_root / "FabricChatClefInboundMessageHandler.java"
        ).read_text(encoding="utf-8")
        inbound_router_text = (
            inbound_root
            / "FabricChatClefInboundMessageRouter.java"
        ).read_text(encoding="utf-8")
        inbound_decoder_text = (
            inbound_root
            / "FabricChatClefInboundEnvelopeDecoder.java"
        ).read_text(encoding="utf-8")
        session_root = BRIDGE_ROOT / "transport" / "session"
        session_facade_text = (
            session_root / "FabricChatClefSessionGuard.java"
        ).read_text(encoding="utf-8")
        session_state_text = (
            session_root
            / "FabricChatClefMutableSessionState.java"
        ).read_text(encoding="utf-8")
        ack_validator_text = (
            session_root
            / "validation"
            / "FabricChatClefHandshakeAckValidator.java"
        ).read_text(encoding="utf-8")
        ack_coordinator_text = (
            session_root
            / "FabricChatClefHandshakeAckCoordinator.java"
        ).read_text(encoding="utf-8")

        self.assertIn("router.route(decoder.decodeRaw(message), generation)", inbound_facade_text)
        stop_index = inbound_router_text.index("stopControlInboundHandler.isCandidate")
        typed_index = inbound_router_text.index("decoder.decodeTyped(rawEnvelope)")
        self.assertLess(stop_index, typed_index)
        self.assertIn("json.decodeTree(message)", inbound_decoder_text)
        self.assertIn("handshakeCoordinator.accept", session_facade_text)
        self.assertIn("FabricChatClefAcceptedSessionIdentity acceptedIdentity", session_state_text)
        self.assertIn("FabricChatClefHandshakeAckValidation validate", ack_validator_text)
        self.assertNotIn("FabricChatClefBridgeDiagnostics", ack_validator_text)
        self.assertIn("sessionState.accept(validation.identity())", ack_coordinator_text)


if __name__ == "__main__":
    unittest.main()
