package lavi.minecraft.fabric.chatclef.bridge.command;

import adris.altoclef.AltoClef;
import adris.altoclef.Settings;
import adris.altoclef.chains.UserTaskChain;
import adris.altoclef.commandsystem.CommandExecutor;
import adris.altoclef.tasks.movement.IdleTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskRunner;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.FabricChatClefTaskStateReader;
import lavi.minecraft.fabric.chatclef.bridge.command.execution.FabricChatClefCommandExecution;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandLifecycleCoordinator;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandOutcomeClassifier;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandResultOutbox;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefPreexistingIdleRootStabilityGate;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefRootOwnershipClassification;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefUserTaskFinishedObserver;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendCompletion;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendOutcome;
import lavi.minecraft.fabric.chatclef.bridge.command.result.send.FabricChatClefCommandResultSendSubmission;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.command.AutoDepositTrustedCommandRegistrar;
import lavi.minecraft.task.container.deposit.auto.trusted.command.form.AutoDepositTrustCommandForm;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositOpenContainerBindingTracker;
import lavi.minecraft.testsupport.HeadlessMinecraftClientSession;
import lavi.minecraft.testsupport.TestObjects;
import net.minecraft.client.MinecraftClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260905_kpopmodder: Verify the exact H5 command through dispatcher callback and successful async terminal queue retirement.
@ResourceLock("AltoClefBridgeGlobalState")
final class FabricChatClefAutoDepositTrustLifecycleIntegrationTest {
    private static final String PREFIXLESS_H5_COMMAND = "auto_deposit_trust area 16x16";

    @Test
    void noRootVisibleCompletesAndRetiresTheMatchingRequestAfterSuccessfulTerminalSend() {
        try (Harness harness = new Harness(false, "req-h5-no-root")) {
            harness.dispatchCurrentTick();

            FabricChatClefCommandExecution execution = harness.activeExecution();
            assertNotNull(execution);
            assertEquals(1, harness.bulkOperationCalls.get());
            assertEquals(AutoDepositTrustCommandForm.ENGLISH_AREA, harness.bulkOperationForm.get());
            assertEquals(harness.expectedNormalizedCommand(), execution.normalizedCommand());
            assertEquals(
                    FabricChatClefRootOwnershipClassification.NO_ROOT_VISIBLE,
                    execution.rootOwnershipClassification()
            );
            assertTrue(execution.finishCallbackReceived());
            assertTrue(execution.finishCallbackFirstObservedBeforeDispatchReturn());
            assertNull(execution.taskFinishedObservation());
            assertTerminal(
                    harness,
                    "completed",
                    "callback_completed_without_user_task",
                    "callback_without_user_task"
            );

            harness.completeSuccessfulTerminalSend();

            assertRetiredExactlyOnce(harness);
        }
    }

    @Test
    void preexistingIdleRootRemainsUnknownAndRetiresOnlyAfterStableEvidenceAndSuccessfulSend() {
        try (Harness harness = new Harness(true, "req-h5-preexisting-idle")) {
            harness.advanceDiagnosticTick();
            harness.dispatchCurrentTick();

            FabricChatClefCommandExecution execution = harness.activeExecution();
            assertNotNull(execution);
            assertEquals(
                    FabricChatClefRootOwnershipClassification.PREEXISTING_UNCHANGED_IDLE_ROOT,
                    execution.rootOwnershipClassification()
            );
            assertTrue(execution.finishCallbackReceived());
            assertTrue(execution.finishCallbackFirstObservedBeforeDispatchReturn());
            assertNull(execution.taskFinishedObservation());
            assertEquals(0, harness.sender.terminalSendCount.get());

            harness.advanceDiagnosticTick();
            harness.dispatchCurrentTick();
            harness.seedStableDurationElapsed();
            harness.advanceDiagnosticTick();
            harness.dispatchCurrentTick();
            harness.advanceDiagnosticTick();
            harness.dispatchCurrentTick();

            execution = harness.activeExecution();
            assertNotNull(execution);
            assertEquals(1, harness.bulkOperationCalls.get());
            assertEquals(AutoDepositTrustCommandForm.ENGLISH_AREA, harness.bulkOperationForm.get());
            assertEquals(harness.expectedNormalizedCommand(), execution.normalizedCommand());
            assertTrue(execution.preexistingIdleRootStabilityQualified());
            assertTerminal(
                    harness,
                    "unknown",
                    "finish_callback_without_new_command_owned_root",
                    "callback_without_matching_user_task_event"
            );

            harness.completeSuccessfulTerminalSend();

            assertRetiredExactlyOnce(harness);
        }
    }

    private static void assertTerminal(
            Harness harness,
            String expectedStatus,
            String expectedReason,
            String expectedFidelity
    ) {
        assertEquals(1, harness.sender.terminalSendCount.get());
        assertSame(harness.context, harness.sender.terminalContext);
        assertTrue(harness.queue.isActive(harness.context));
        assertTrue(harness.context.terminalSendInFlight());
        assertFalse(harness.context.terminalSent());

        Map<String, Object> payload = harness.sender.terminalPayload.toMap();
        assertEquals(harness.context.requestId(), payload.get("request_id"));
        assertEquals(expectedStatus, payload.get("status"));
        Map<?, ?> data = (Map<?, ?>) payload.get("data");
        assertEquals(expectedReason, data.get("result_reason"));
        assertEquals(expectedFidelity, data.get("result_fidelity"));
        assertEquals(true, data.get("finish_callback_received"));
        assertEquals(false, data.get("task_finished_event_received"));
    }

    private static void assertRetiredExactlyOnce(Harness harness) {
        assertTrue(harness.context.terminalSent());
        assertFalse(harness.context.terminalSendInFlight());
        assertFalse(harness.queue.hasActive());
        assertTrue(harness.queue.peekPending().isEmpty());
        assertNull(harness.activeExecution());
        assertEquals(1, harness.sender.terminalSendCount.get());
        assertEquals(1, harness.bulkOperationCalls.get());
    }

    private static final class Harness implements AutoCloseable {
        private final Field altoClefInstanceField = staticField(AltoClef.class, "instance");
        private final Field commandExecutorField = staticField(AltoClef.class, "commandExecutor");
        private final Object previousAltoClefInstance = getStatic(altoClefInstanceField);
        private final Object previousCommandExecutor = getStatic(commandExecutorField);
        private final boolean previousBoundaryEnabled = ChatClefDiagnostics.isBoundaryEnabled();
        private final HeadlessMinecraftClientSession clientSession = HeadlessMinecraftClientSession.inGame();
        private final TestAltoClef mod;
        private final CommandExecutor executor;
        private final AutoDepositOpenContainerBindingTracker bindingTracker;
        private final FabricChatClefCommandQueue queue = new FabricChatClefCommandQueue();
        private final FabricChatClefCommandContext context;
        private final CapturingResultSender sender = new CapturingResultSender();
        private final FabricChatClefCommandLifecycleCoordinator coordinator;
        private final FabricChatClefCommandDispatcher dispatcher;
        private final AtomicInteger bulkOperationCalls = new AtomicInteger();
        private final AtomicReference<AutoDepositTrustCommandForm> bulkOperationForm = new AtomicReference<>();

        private Harness(boolean preexistingIdleRoot, String requestId) {
            mod = new TestAltoClef(preexistingIdleRoot);
            executor = new CommandExecutor(mod);
            setStatic(altoClefInstanceField, mod);
            setStatic(commandExecutorField, executor);
            if (preexistingIdleRoot && !previousBoundaryEnabled) {
                ChatClefDiagnostics.setBoundaryEnabled(true);
            }

            bindingTracker = new AutoDepositOpenContainerBindingTracker(mod);
            AutoDepositTrustedCommandRegistrar registrar = new AutoDepositTrustedCommandRegistrar(
                    AutoDepositTrustedDestinationRepository.inMemoryEmpty(),
                    bindingTracker,
                    (ignored, form) -> {
                        bulkOperationCalls.incrementAndGet();
                        bulkOperationForm.set(form);
                    }
            );
            registrar.register(mod);
            if (!registrar.registered() || !registrar.koreanBulkAliasRegistered()) {
                throw new AssertionError("H5 registrar was not ready for the lifecycle fixture");
            }

            FabricChatClefCommandRequest request = new FabricChatClefCommandRequest();
            request.requestId = requestId;
            request.command = PREFIXLESS_H5_COMMAND;
            request.source = "lavi_chat_ui";
            context = new FabricChatClefCommandContext(
                    request,
                    "corr-" + requestId,
                    "session-" + requestId,
                    7L
            );
            if (!queue.offer(context)) {
                throw new AssertionError("Failed to enqueue H5 lifecycle fixture request");
            }

            FabricChatClefBridgeDiagnostics diagnostics = new FabricChatClefBridgeDiagnostics();
            FabricChatClefTaskStateReader taskStateReader = new FabricChatClefTaskStateReader();
            FabricChatClefUserTaskFinishedObserver observer =
                    new FabricChatClefUserTaskFinishedObserver(diagnostics, taskStateReader);
            FabricChatClefCommandResultOutbox outbox =
                    new FabricChatClefCommandResultOutbox(queue, sender, diagnostics);
            coordinator = new FabricChatClefCommandLifecycleCoordinator(
                    observer,
                    new FabricChatClefCommandOutcomeClassifier(),
                    outbox,
                    sender,
                    diagnostics,
                    taskStateReader
            );
            dispatcher = new FabricChatClefCommandDispatcher(
                    queue,
                    sender,
                    coordinator,
                    diagnostics,
                    taskStateReader
            );
        }

        private String expectedNormalizedCommand() {
            return mod.getModSettings().getCommandPrefix() + PREFIXLESS_H5_COMMAND;
        }

        private void dispatchCurrentTick() {
            dispatcher.onEndClientTick(MinecraftClient.getInstance());
        }

        private void advanceDiagnosticTick() {
            long before = ChatClefDiagnostics.currentClientTickId();
            ChatClefDiagnostics.onClientTickHead();
            if (ChatClefDiagnostics.currentClientTickId() <= before) {
                throw new AssertionError("Diagnostic client tick did not advance");
            }
        }

        private void seedStableDurationElapsed() {
            Object gate = getInstanceField(coordinator, "preexistingIdleRootStabilityGate");
            Field firstObservedAtNanos = instanceField(gate.getClass(), "firstObservedAtNanos");
            long elapsedNanos = (FabricChatClefPreexistingIdleRootStabilityGate.MIN_STABLE_DURATION_MS + 1L)
                    * 1_000_000L;
            setLong(firstObservedAtNanos, gate, System.nanoTime() - elapsedNanos);
        }

        private FabricChatClefCommandExecution activeExecution() {
            Object value = getInstanceField(coordinator, "activeExecution");
            if (!(value instanceof AtomicReference<?> reference)) {
                throw new AssertionError("Lifecycle activeExecution field is not an AtomicReference");
            }
            return (FabricChatClefCommandExecution) reference.get();
        }

        private void completeSuccessfulTerminalSend() {
            queue.enqueueCommandResultSendCompletion(
                    FabricChatClefCommandResultSendCompletion.of(
                            context,
                            FabricChatClefCommandResultSendOutcome.sent()
                    )
            );
            dispatchCurrentTick();
        }

        @Override
        public void close() {
            try {
                bindingTracker.stop();
            } finally {
                try {
                    if (!previousBoundaryEnabled && ChatClefDiagnostics.isBoundaryEnabled()) {
                        ChatClefDiagnostics.setBoundaryEnabled(false);
                    }
                } finally {
                    setStatic(commandExecutorField, previousCommandExecutor);
                    setStatic(altoClefInstanceField, previousAltoClefInstance);
                    clientSession.close();
                }
            }
        }
    }

    private static final class TestAltoClef extends AltoClef {
        private final Settings settings = testSettings();
        private final UserTaskChain userTaskChain;

        private TestAltoClef(boolean preexistingIdleRoot) {
            userTaskChain = preexistingIdleRoot ? new StableIdleUserTaskChain(this) : null;
        }

        @Override
        public Settings getModSettings() {
            return settings;
        }

        @Override
        public UserTaskChain getUserTaskChain() {
            return userTaskChain;
        }

        private static Settings testSettings() {
            Settings settings = TestObjects.allocate(Settings.class);
            TestObjects.setField(settings, Settings.class, "commandPrefix", "@");
            return settings;
        }
    }

    private static final class StableIdleUserTaskChain extends UserTaskChain {
        private final IdleTask idleRoot = new IdleTask();

        private StableIdleUserTaskChain(AltoClef mod) {
            super(new TaskRunner(mod));
        }

        @Override
        public Task getCurrentTask() {
            return idleRoot;
        }

        @Override
        public String diagnosticRootAssignmentId() {
            return "user-root-h5-test";
        }

        @Override
        public long diagnosticRootGeneration() {
            return 1L;
        }

        @Override
        public boolean diagnosticRunningIdleTaskFlag() {
            return true;
        }

        @Override
        public boolean diagnosticNextTaskIdleFlag() {
            return false;
        }
    }

    private static final class CapturingResultSender implements FabricChatClefCommandResultSender {
        private final AtomicInteger terminalSendCount = new AtomicInteger();
        private FabricChatClefCommandContext terminalContext;
        private FabricChatClefCommandResultPayload terminalPayload;

        @Override
        public FabricChatClefCommandResultSendSubmission sendCommandResult(
                FabricChatClefCommandContext context,
                FabricChatClefCommandResultPayload payload
        ) {
            return FabricChatClefCommandResultSendSubmission.accepted();
        }

        @Override
        public FabricChatClefCommandResultSendSubmission sendTerminalCommandResult(
                FabricChatClefCommandContext context,
                FabricChatClefCommandResultPayload payload
        ) {
            terminalContext = context;
            terminalPayload = payload;
            terminalSendCount.incrementAndGet();
            return FabricChatClefCommandResultSendSubmission.accepted();
        }
    }

    private static Field staticField(Class<?> owner, String name) {
        return instanceField(owner, name);
    }

    private static Field instanceField(Class<?> owner, String name) {
        try {
            Field field = owner.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException exception) {
            throw new AssertionError("Missing field " + owner.getName() + "." + name, exception);
        }
    }

    private static Object getStatic(Field field) {
        try {
            return field.get(null);
        } catch (IllegalAccessException exception) {
            throw new AssertionError("Failed to read static field " + field.getName(), exception);
        }
    }

    private static void setStatic(Field field, Object value) {
        try {
            field.set(null, value);
        } catch (IllegalAccessException exception) {
            throw new AssertionError("Failed to write static field " + field.getName(), exception);
        }
    }

    private static Object getInstanceField(Object owner, String name) {
        Field field = instanceField(owner.getClass(), name);
        try {
            return field.get(owner);
        } catch (IllegalAccessException exception) {
            throw new AssertionError("Failed to read instance field " + name, exception);
        }
    }

    private static void setLong(Field field, Object owner, long value) {
        try {
            field.setLong(owner, value);
        } catch (IllegalAccessException exception) {
            throw new AssertionError("Failed to write long field " + field.getName(), exception);
        }
    }
}
