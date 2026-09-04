package lavi.minecraft.diagnostics;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.slots.Slot;
import baritone.api.utils.input.Input;
import lavi.minecraft.diagnostics.command.DiagnosticCommandContextProvider;
import lavi.minecraft.diagnostics.command.DiagnosticCommandContextRegistry;
import lavi.minecraft.diagnostics.container.store.deposit.interaction.StoreDepositInteractionObserver;
import lavi.minecraft.diagnostics.container.gui.ContainerGuiDiagnostics;
import lavi.minecraft.diagnostics.formatting.DiagnosticFormatterFacade;
import lavi.minecraft.diagnostics.interaction.BlockInteractionObserver;
import lavi.minecraft.diagnostics.mode.DiagnosticModeController;
import lavi.minecraft.diagnostics.mode.DiagnosticOutputMode;
import lavi.minecraft.diagnostics.postplace.PostPlaceContainerInteractionObserver;
import lavi.minecraft.diagnostics.postplace.PostPlaceContainerOpenIntent;
import lavi.minecraft.diagnostics.runtime.RuntimeIdentityDiagnostics;
import lavi.minecraft.diagnostics.session.admission.DiagnosticEventFamily;
import lavi.minecraft.diagnostics.session.admission.DiagnosticSessionAdmissionAuthority;
import lavi.minecraft.diagnostics.session.admission.DiagnosticSessionSnapshot;
import lavi.minecraft.diagnostics.session.lifecycle.DiagnosticSessionLifecycleObserver;
import lavi.minecraft.diagnostics.session.lifecycle.DiagnosticSessionLifecycleRegistry;
import lavi.minecraft.diagnostics.session.lifecycle.mode.DiagnosticStateCleanupLifecycleObserver;
import lavi.minecraft.diagnostics.session.runtime.DiagnosticBoundedGroupEmission;
import lavi.minecraft.diagnostics.session.runtime.DiagnosticDispatchObserver;
import lavi.minecraft.diagnostics.session.runtime.DiagnosticDispatchResult;
import lavi.minecraft.diagnostics.session.runtime.DiagnosticSessionRuntime;
import lavi.minecraft.diagnostics.session.reset.DiagnosticSessionTestResetResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.function.Supplier;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

//20260730_kpopmodder: Added diagnostic logging to prove the Carry On interaction failure boundary.
//20260730_kpopmodder: Added generic ChatClef diagnostics to prove interaction/input/task boundaries without changing behavior.
public final class ChatClefDiagnostics {
    private static final DiagnosticTraceState TRACE_STATE = new DiagnosticTraceState();
    private static final DiagnosticTaskRegistry TASKS = new DiagnosticTaskRegistry();
    private static final DiagnosticModeController MODE = new DiagnosticModeController(DiagnosticOutputMode.fromEnvironment());
    private static final DiagnosticSessionRuntime SESSION = new DiagnosticSessionRuntime(
            MODE,
            new DiagnosticSessionAdmissionAuthority(newDiagnosticSessionId())
    );
    private static final DiagnosticSessionLifecycleRegistry SESSION_LIFECYCLE =
            new DiagnosticSessionLifecycleRegistry();
    private static final DiagnosticEventEmitter EVENTS = new DiagnosticEventEmitter(TRACE_STATE, TASKS, SESSION);
    private static final DiagnosticCommandContextRegistry COMMAND_CONTEXTS = new DiagnosticCommandContextRegistry();
    private static final DiagnosticContextBuilder CONTEXT = new DiagnosticContextBuilder(MODE, TASKS);
    private static final DiagnosticFormatterFacade FORMATTERS = new DiagnosticFormatterFacade(
            MODE::isOff,
            TASKS::taskInstanceIdLabel,
            TASKS::taskRunIdLabel
    );
    private static final PostPlaceContainerDiagnostics POST_PLACE_CONTAINERS = new PostPlaceContainerDiagnostics(
            MODE,
            EVENTS,
            ChatClefDiagnostics::currentClientTickId
    );
    private static final BlockInteractionDiagnostics BLOCK_INTERACTIONS = new BlockInteractionDiagnostics(
            MODE,
            EVENTS,
            ChatClefDiagnostics::currentClientTickId,
            ChatClefDiagnostics::nextOperationId,
            COMMAND_CONTEXTS
    );

    static {
        BLOCK_INTERACTIONS.registerObserver(new StoreDepositInteractionObserver());
        BLOCK_INTERACTIONS.registerObserver(ContainerGuiDiagnostics.interactionObserver());
        SESSION_LIFECYCLE.register(new DiagnosticStateCleanupLifecycleObserver(
                TASKS::clearForModeTransition
        ));
        SESSION_LIFECYCLE.register(new DiagnosticStateCleanupLifecycleObserver(
                POST_PLACE_CONTAINERS::clearForSessionTransition
        ));
        SESSION_LIFECYCLE.register(new DiagnosticStateCleanupLifecycleObserver(
                BLOCK_INTERACTIONS::clearForSessionTransition
        ));
        SESSION_LIFECYCLE.register(ContainerGuiDiagnostics.lifecycleObserver());
    }

    private ChatClefDiagnostics() {
    }

    public static void onClientTickHead() {
        SESSION.runIfEligible(() -> {
            try {
                TRACE_STATE.advanceClientTick();
                ContainerGuiDiagnostics.onClientTickHead(TRACE_STATE.currentClientTickId());
                logRuntimeIdentityOnce();
            } catch (RuntimeException | LinkageError ignored) {
            }
        });
    }

    public static String currentTraceId() {
        return TRACE_STATE.currentTraceId();
    }

    public static long currentClientTickId() {
        return TRACE_STATE.currentClientTickId();
    }

    public static long nextEventSequence() {
        return TRACE_STATE.nextEventSequence();
    }

    public static long nextOperationId() {
        return TRACE_STATE.nextOperationId();
    }

    public static boolean isVerboseEnabled() {
        return MODE.isVerboseEnabled() && SESSION.isEligible();
    }

    public static boolean isBoundaryEnabled() {
        return SESSION.isEligible();
    }

    //20260731_kpopmodder: Allow the LAVI command layer to switch diagnostics between BOUNDARY and OFF without changing engine behavior.
    public static void setBoundaryEnabled(boolean enabled) {
        if (enabled) {
            SESSION.setBoundaryEnabled(true);
            SESSION.runIfEligible(TRACE_STATE::resetRuntimeIdentityLogged);
            return;
        }
        SESSION.setBoundaryEnabled(false, () -> {
            SESSION_LIFECYCLE.notifyBeforeModeOff();
            TRACE_STATE.resetRuntimeIdentityLogged();
        });
    }

    public static void registerSessionLifecycleObserver(
            DiagnosticSessionLifecycleObserver observer) {
        SESSION_LIFECYCLE.register(observer);
    }

    public static DiagnosticDispatchResult emitCleanTeardownFinalSnapshot() {
        AtomicBoolean emissionCallsReturned = new AtomicBoolean();
        try {
            return SESSION.emitCleanTeardownFinalSnapshot(
                    snapshot -> {
                        Object[] lifecycleFields = SESSION_LIFECYCLE.finalSnapshotFields();
                        EVENTS.emitCleanTeardownFinalSnapshotPhysical(
                                snapshot,
                                lifecycleFields
                        );
                        emissionCallsReturned.set(true);
                    }
            );
        } finally {
            SESSION_LIFECYCLE.notifyAfterCleanTeardownSnapshotAttempt(
                    emissionCallsReturned.get()
            );
        }
    }

    public static String inputHeldState(Input input) {
        if (!SESSION.isEligible()) {
            return "unavailable";
        }
        return DiagnosticInputState.inputHeld(input);
    }

    public static String rawInputHeldState(Input input) {
        if (!SESSION.isEligible()) {
            return "unavailable";
        }
        return DiagnosticInputState.rawKeyHeld(input);
    }

    public static void startTrace(String reason, Task task, Object... fields) {
        if (!isVerboseEnabled()) {
            return;
        }
        safeLog("TRACE", "START", reason, task, fields, true);
    }

    public static void enterTask(Task task) {
        SESSION.runIfEligible(() -> {
            ContainerGuiDiagnostics.onTaskEvaluationStarted(task);
            TASKS.enterTask(task);
        });
    }

    public static void exitTask(Task task) {
        SESSION.runIfEligible(() -> TASKS.exitTask(task));
    }

    public static Task currentTaskForDiagnostics() {
        return SESSION.callIfEligible(TASKS::currentTask, null);
    }

    public static void noteBlockInteractionOwner(Task sourceTask, BlockPos targetPosition) {
        if (!isBoundaryEnabled()) {
            return;
        }
        SESSION.runIfEligible(() -> BLOCK_INTERACTIONS.noteOwner(
                sourceTask,
                targetPosition
        ));
    }

    public static void beginTaskRun(Task task, TaskChain parentChain) {
        if (!isVerboseEnabled()) {
            return;
        }
        safeLog("TASK", "START", "first_tick", task,
                new Object[]{"parentChain", chainName(parentChain)},
                false,
                true);
    }

    public static void setParent(Task child, Task parent) {
        SESSION.runIfEligible(() -> {
            if (MODE.isVerboseEnabled()) {
                TASKS.setParent(child, parent);
            }
        });
    }

    public static void logEvent(String eventType, String phase, String reason, Task task, Object... fields) {
        if (!isVerboseEnabled()) {
            return;
        }
        safeLog(eventType, phase, reason, task, fields, false);
    }

    public static void logTaskTransition(Task parent, Task previousTask, Task nextTask, String reason, Object... fields) {
        if (!isVerboseEnabled()) {
            return;
        }
        Object[] merged = CONTEXT.taskTransitionFields(fields, previousTask, nextTask);
        safeLog("TASK_CHILD", "TRANSITION", reason, parent, merged, false);
    }

    public static void logInput(String phase, String reason, Input input, Object... fields) {
        if (!isVerboseEnabled()) {
            return;
        }
        Object[] merged = CONTEXT.inputFields(fields, input);
        safeLog("INPUT", phase, reason, TASKS.currentTask(), merged, false);
    }

    public static void logInputSnapshot(String phase, String reason, Object... fields) {
        if (!isVerboseEnabled()) {
            return;
        }
        Object[] merged = CONTEXT.inputSnapshotFields(fields);
        safeLog("INPUT_SNAPSHOT", phase, reason, TASKS.currentTask(), merged, false);
    }

    public static void logSlotClick(String phase, String reason, Slot slot, int mouseButton, Object type, Object... fields) {
        if (!isVerboseEnabled()) {
            return;
        }
        Object[] merged = CONTEXT.slotClickFields(fields, slot, mouseButton, type);
        safeLog("SLOT", phase, reason, TASKS.currentTask(), merged, false);
    }

    public static void logInteractBlock(String phase, String reason, Object hand, BlockHitResult hitResult, Object result) {
        SESSION.runIfEligible(() -> logInteractBlockEligible(
                phase,
                reason,
                MinecraftClient.getInstance().player,
                hand,
                hitResult,
                result
        ));
    }

    public static void logInteractBlock(String phase, String reason, ClientPlayerEntity player, Object hand, BlockHitResult hitResult, Object result) {
        SESSION.runIfEligible(() -> logInteractBlockEligible(
                phase,
                reason,
                player,
                hand,
                hitResult,
                result
        ));
    }

    private static void logInteractBlockEligible(String phase,
                                                 String reason,
                                                 ClientPlayerEntity player,
                                                 Object hand,
                                                 BlockHitResult hitResult,
                                                 Object result) {
        logPostPlaceContainerInteractIfMatching(phase, player, hand, hitResult, result);
        BLOCK_INTERACTIONS.logInteract(phase, player, hand, hitResult, result);
        if (!MODE.isVerboseEnabled()) {
            return;
        }
        BlockPos blockPos = hitResult == null ? null : hitResult.getBlockPos();
        safeLog("MINECRAFT_INTERACTION", phase, reason, TASKS.currentTask(), new Object[]{
                "hand", value(hand),
                "hitBlockPos", blockPos == null ? "unavailable" : value(blockPos),
                "hitSide", hitResult == null ? "unavailable" : value(hitResult.getSide()),
                "hitType", hitResult == null ? "unavailable" : value(hitResult.getType()),
                "targetBlockState", blockPos == null ? "unavailable" : safeValue(() -> MinecraftClient.getInstance().world.getBlockState(blockPos)),
                "clientPlayerSneaking", safeValue(() -> MinecraftClient.getInstance().player.isSneaking()),
                "argumentPlayerSneaking", safeValue(() -> player == null ? null : player.isSneaking()),
                "clientPlayerSameAsArgument", safeValue(() -> MinecraftClient.getInstance().player == player),
                "rawUseKeyPressed", DiagnosticInputState.rawKeyHeld(Input.CLICK_RIGHT),
                "rawSneakKeyPressed", DiagnosticInputState.rawKeyHeld(Input.SNEAK),
                "rawAttackKeyPressed", DiagnosticInputState.rawKeyHeld(Input.CLICK_LEFT),
                "sneakAndUsePressedTogether", DiagnosticInputState.sneakAndUsePressedTogether(),
                "clientPlayerPosition", safeValue(() -> MinecraftClient.getInstance().player.getPos()),
                "argumentPlayerPosition", safeValue(() -> player == null ? null : player.getPos()),
                "clientMainHandItem", safeValue(() -> MinecraftClient.getInstance().player.getMainHandStack()),
                "argumentMainHandItem", safeValue(() -> player == null ? null : player.getMainHandStack()),
                "clientOffHandItem", safeValue(() -> MinecraftClient.getInstance().player.getOffHandStack()),
                "argumentOffHandItem", safeValue(() -> player == null ? null : player.getOffHandStack()),
                "result", value(result)
        }, false);
    }

    public static void logBoundary(String eventName, String reason, Task task, Object... fields) {
        if (!SESSION.isEligible()) {
            return;
        }
        emitEvent("BOUNDARY", "[LAVI ChatClefBoundary]", eventName, reason, task, fields, false);
    }

    //20260901_kpopmodder: Expose completed shared emission only to source-linked diagnostic projections.
    public static boolean logBoundaryWithPhysicalOutcome(
            String eventName,
            String reason,
            Task task,
            Object... fields) {
        return logBoundaryWithDispatchResult(
                eventName,
                reason,
                task,
                fields
        ).emissionCompleted();
    }

    //20260901_kpopmodder: Preserve shared admission independently from sink completion.
    public static DiagnosticDispatchResult logBoundaryWithDispatchResult(
            String eventName,
            String reason,
            Task task,
            Object... fields) {
        return EVENTS.emitEventWithOutcome(
                "BOUNDARY",
                "[LAVI ChatClefBoundary]",
                eventName,
                reason,
                task,
                fields,
                false
        );
    }

    public static void logBoundedBoundary(
            String eventName,
            String reason,
            Task task,
            int maxUtf8Bytes,
            Object[] requiredFields,
            Object[] optionalFields) {
        logBoundedBoundaryWithPhysicalOutcome(
                eventName,
                reason,
                task,
                maxUtf8Bytes,
                requiredFields,
                optionalFields
        );
    }

    public static boolean logBoundedBoundaryWithPhysicalOutcome(
            String eventName,
            String reason,
            Task task,
            int maxUtf8Bytes,
            Object[] requiredFields,
            Object[] optionalFields) {
        return logBoundedBoundaryWithDispatchResult(
                eventName,
                reason,
                task,
                maxUtf8Bytes,
                requiredFields,
                optionalFields
        ).emissionCompleted();
    }

    public static DiagnosticDispatchResult logBoundedBoundaryWithDispatchResult(
            String eventName,
            String reason,
            Task task,
            int maxUtf8Bytes,
            Object[] requiredFields,
            Object[] optionalFields) {
        return EVENTS.emitBoundedBoundaryEventWithOutcome(
                "[LAVI ChatClefBoundary]",
                eventName,
                reason,
                task,
                maxUtf8Bytes,
                requiredFields,
                optionalFields
        );
    }

    //20260803_kpopmodder: Keep command lifecycle diagnostics visible even when broad diagnostics are disabled.
    public static void logLifecycleBoundary(String eventName, String reason, Task task, Object... fields) {
        EVENTS.emitOperationalEvent(
                "BOUNDARY",
                "[LAVI ChatClefLifecycle]",
                eventName,
                reason,
                task,
                fields,
                false
        );
    }

    public static void logWarningEvent(String eventName, String reason, Task task, Object... fields) {
        if (!SESSION.isEligible()) {
            return;
        }
        emitEvent("WARN", "[LAVI ChatClefDiag]", eventName, reason, task, fields, true);
    }

    public static void logVerboseLine(String message) {
        if (!isVerboseEnabled()) {
            return;
        }
        EVENTS.emitRawLine("RAW_VERBOSE_DIAGNOSTIC_LINE", message, false);
    }

    public static void logWarningLine(String message) {
        if (!SESSION.isEligible()) {
            return;
        }
        EVENTS.emitRawLine("RAW_DIAGNOSTIC_WARNING", value(message), true);
    }

    public static DiagnosticDispatchResult emitCriticalBoundedGroup(
            DiagnosticEventFamily family,
            String groupEventName,
            DiagnosticBoundedGroupEmission groupEmission,
            DiagnosticDispatchObserver observer) {
        return EVENTS.emitCriticalBoundedGroup(
                family,
                groupEventName,
                groupEmission,
                observer
        );
    }

    public static boolean runIfDiagnosticsEligible(Runnable action) {
        return SESSION.runIfEligible(action);
    }

    public static <T> T callIfDiagnosticsEligible(Supplier<T> action, T ineligibleValue) {
        return SESSION.callIfEligible(action, ineligibleValue);
    }

    public static DiagnosticSessionSnapshot diagnosticSessionSnapshot() {
        return SESSION.snapshot();
    }

    public static DiagnosticSessionSnapshot resetDiagnosticSessionForTests() {
        if (!MODE.isOff()) {
            throw new IllegalStateException("Disable diagnostics before using the OFF-only test reset seam.");
        }
        return SESSION.replaceOffSessionForTests(newDiagnosticSessionId());
    }

    public static DiagnosticSessionTestResetResult resetDiagnosticSessionForTestsWithResult() {
        if (!MODE.isOff()) {
            throw new IllegalStateException("Disable diagnostics before using the OFF-only test reset seam.");
        }
        return SESSION.replaceOffSessionForTestsWithResult(newDiagnosticSessionId());
    }

    public static void beginPostPlaceContainerOpenIntent(long operationId,
                                                         Object containerType,
                                                         BlockPos targetPosition,
                                                         Object targetBlockState) {
        SESSION.runIfEligible(() -> POST_PLACE_CONTAINERS.begin(
                operationId,
                containerType,
                targetPosition,
                targetBlockState
        ));
    }

    public static PostPlaceContainerOpenIntent activePostPlaceContainerOpenIntent(BlockPos targetPosition) {
        return SESSION.callIfEligible(() -> POST_PLACE_CONTAINERS.active(targetPosition), null);
    }

    public static int postPlaceContainerAttemptCount(long operationId) {
        return SESSION.callIfEligible(() -> POST_PLACE_CONTAINERS.attemptCount(operationId), 0);
    }

    public static String postPlaceContainerLastInteractResult(long operationId) {
        return SESSION.callIfEligible(
                () -> POST_PLACE_CONTAINERS.lastInteractResult(operationId),
                "unavailable"
        );
    }

    public static long postPlaceContainerElapsedTicks(long operationId) {
        return SESSION.callIfEligible(() -> POST_PLACE_CONTAINERS.elapsedTicks(operationId), -1L);
    }

    public static boolean markPostPlaceContainerGuiOpened(long operationId) {
        return SESSION.callIfEligible(() -> POST_PLACE_CONTAINERS.markGuiOpened(operationId), false);
    }

    public static boolean markPostPlaceContainerGuiTimeout(long operationId) {
        return SESSION.callIfEligible(() -> POST_PLACE_CONTAINERS.markGuiTimeout(operationId), false);
    }

    public static boolean markPostPlaceContainerWarningLogged(long operationId) {
        return SESSION.callIfEligible(() -> POST_PLACE_CONTAINERS.markWarningLogged(operationId), false);
    }

    public static boolean isPostPlaceContainerGuiOpened(long operationId) {
        return SESSION.callIfEligible(() -> POST_PLACE_CONTAINERS.isGuiOpened(operationId), false);
    }

    public static void clearPostPlaceContainerOpenIntent(long operationId) {
        POST_PLACE_CONTAINERS.clear(operationId);
    }

    public static void registerPostPlaceContainerInteractionObserver(PostPlaceContainerInteractionObserver observer) {
        POST_PLACE_CONTAINERS.registerObserver(observer);
    }

    public static void registerBlockInteractionObserver(BlockInteractionObserver observer) {
        BLOCK_INTERACTIONS.registerObserver(observer);
    }

    public static void registerCommandContextProvider(DiagnosticCommandContextProvider provider) {
        COMMAND_CONTEXTS.register(provider);
    }

    public static Object[] withCommandContextFields(Object... fields) {
        return COMMAND_CONTEXTS.appendFields(fields);
    }

    public static Object[] currentCommandContextFields() {
        return COMMAND_CONTEXTS.fields();
    }

    public static String className(Object value) {
        return FORMATTERS.className(value);
    }

    public static String safeValue(Supplier<?> supplier) {
        return FORMATTERS.safeValue(supplier);
    }

    public static String safeValueForDiagnosticLog(Supplier<?> supplier) {
        return FORMATTERS.safeValueForDiagnosticLog(supplier);
    }

    public static String taskSummary(Task task) {
        return FORMATTERS.taskSummary(task);
    }

    public static String taskSummaryForDiagnosticLog(Task task) {
        return FORMATTERS.taskSummaryForDiagnosticLog(task);
    }

    public static String entitySummary(Entity entity) {
        return FORMATTERS.entitySummary(entity);
    }

    public static String entityDistanceSqrToPlayer(AltoClef mod, Entity entity) {
        return FORMATTERS.entityDistanceSqrToPlayer(mod, entity);
    }

    public static String playerPosition(AltoClef mod) {
        return FORMATTERS.playerPosition(mod);
    }

    public static String vec3d(Vec3d pos) {
        return FORMATTERS.vec3d(pos);
    }

    public static String blockPos(BlockPos pos) {
        return FORMATTERS.blockPos(pos);
    }

    public static String itemStackSummary(ItemStack stack) {
        return FORMATTERS.itemStackSummary(stack);
    }

    public static String slotSummary(Slot slot) {
        return FORMATTERS.slotSummary(slot);
    }

    public static String slotStackSummary(Slot slot) {
        return FORMATTERS.slotStackSummary(slot);
    }

    public static String itemTargets(ItemTarget[] targets) {
        return FORMATTERS.itemTargets(targets);
    }

    public static String classList(Class<?>[] classes) {
        return FORMATTERS.classList(classes);
    }

    private static void safeLog(String eventType, String phase, String reason, Task task, Object[] fields, boolean startNewTrace) {
        safeLog(eventType, phase, reason, task, fields, startNewTrace, false);
    }

    private static void safeLog(String eventType, String phase, String reason, Task task, Object[] fields, boolean startNewTrace, boolean startTaskRun) {
        if (!isVerboseEnabled()) {
            return;
        }
        EVENTS.logVerboseEvent(eventType, phase, reason, task, fields, startNewTrace, startTaskRun);
    }

    private static void emitEvent(String level,
                                  String prefix,
                                  String eventName,
                                  String reason,
                                  Task task,
                                  Object[] fields,
                                  boolean warning) {
        EVENTS.emitEvent(level, prefix, eventName, reason, task, fields, warning);
    }

    private static void logPostPlaceContainerInteractIfMatching(String phase,
                                                                ClientPlayerEntity player,
                                                                Object hand,
                                                                BlockHitResult hitResult,
                                                                Object result) {
        POST_PLACE_CONTAINERS.logInteractIfMatching(phase, player, hand, hitResult, result);
    }

    private static void logRuntimeIdentityOnce() {
        if (!TRACE_STATE.markRuntimeIdentityLogged()) {
            return;
        }
        logBoundary(
                "DIAGNOSTICS_RUNTIME_IDENTITY",
                "diagnostics_runtime_identity",
                null,
                RuntimeIdentityDiagnostics.fields(MODE.current().name(), ChatClefDiagnostics.class)
        );
    }

    public static String chainName(TaskChain chain) {
        return FORMATTERS.chainName(chain);
    }

    public static String chainNameForDiagnosticLog(TaskChain chain) {
        return FORMATTERS.chainNameForDiagnosticLog(chain);
    }

    private static String value(Object rawValue) {
        return FORMATTERS.value(rawValue);
    }

    private static String newDiagnosticSessionId() {
        return "chatclef-diagnostic-" + UUID.randomUUID();
    }
}
