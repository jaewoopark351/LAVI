package lavi.minecraft.diagnostics;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.slots.Slot;
import baritone.api.utils.input.Input;
import lavi.minecraft.diagnostics.postplace.PostPlaceContainerInteractionObserver;
import lavi.minecraft.diagnostics.postplace.PostPlaceContainerOpenIntent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.function.Supplier;

//20260730_kpopmodder: Added diagnostic logging to prove the Carry On interaction failure boundary.
//20260730_kpopmodder: Added generic ChatClef diagnostics to prove interaction/input/task boundaries without changing behavior.
public final class ChatClefDiagnostics {
    private static final DiagnosticTraceState TRACE_STATE = new DiagnosticTraceState();
    private static final DiagnosticTaskRegistry TASKS = new DiagnosticTaskRegistry();
    private static final DiagnosticModeController MODE = new DiagnosticModeController(DiagnosticOutputMode.fromEnvironment());
    private static final DiagnosticEventEmitter EVENTS = new DiagnosticEventEmitter(TRACE_STATE, TASKS);
    private static final PostPlaceContainerDiagnostics POST_PLACE_CONTAINERS = new PostPlaceContainerDiagnostics(
            MODE,
            EVENTS,
            ChatClefDiagnostics::currentClientTickId
    );

    private ChatClefDiagnostics() {
    }

    public static void onClientTickHead() {
        if (MODE.isOff()) {
            return;
        }
        try {
            TRACE_STATE.advanceClientTick();
            logRuntimeIdentityOnce();
        } catch (RuntimeException | LinkageError ignored) {
        }
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
        return MODE.isVerboseEnabled();
    }

    public static boolean isBoundaryEnabled() {
        return MODE.isBoundaryEnabled();
    }

    //20260731_kpopmodder: Allow the LAVI command layer to switch diagnostics between BOUNDARY and OFF without changing engine behavior.
    public static void setBoundaryEnabled(boolean enabled) {
        MODE.setBoundaryEnabled(enabled);
        TRACE_STATE.resetRuntimeIdentityLogged();
    }

    public static String inputHeldState(Input input) {
        if (MODE.isOff()) {
            return "unavailable";
        }
        return DiagnosticInputState.inputHeld(input);
    }

    public static String rawInputHeldState(Input input) {
        if (MODE.isOff()) {
            return "unavailable";
        }
        return DiagnosticInputState.rawKeyHeld(input);
    }

    public static void startTrace(String reason, Task task, Object... fields) {
        if (!MODE.isVerboseEnabled()) {
            return;
        }
        safeLog("TRACE", "START", reason, task, fields, true);
    }

    public static void enterTask(Task task) {
        if (!MODE.isVerboseEnabled()) {
            return;
        }
        TASKS.enterTask(task);
    }

    public static void exitTask(Task task) {
        if (!MODE.isVerboseEnabled()) {
            return;
        }
        TASKS.exitTask(task);
    }

    public static void beginTaskRun(Task task, TaskChain parentChain) {
        if (!MODE.isVerboseEnabled()) {
            return;
        }
        safeLog("TASK", "START", "first_tick", task,
                new Object[]{"parentChain", chainName(parentChain)},
                false,
                true);
    }

    public static void setParent(Task child, Task parent) {
        if (!MODE.isVerboseEnabled()) {
            return;
        }
        TASKS.setParent(child, parent);
    }

    public static void logEvent(String eventType, String phase, String reason, Task task, Object... fields) {
        if (!MODE.isVerboseEnabled()) {
            return;
        }
        safeLog(eventType, phase, reason, task, fields, false);
    }

    public static void logTaskTransition(Task parent, Task previousTask, Task nextTask, String reason, Object... fields) {
        if (!MODE.isVerboseEnabled()) {
            return;
        }
        Object[] merged = mergeFields(fields,
                "previousTask", taskName(previousTask),
                "previousTaskInstanceId", TASKS.taskInstanceIdLabel(previousTask),
                "previousTaskRunId", TASKS.taskRunIdLabel(previousTask),
                "nextTask", taskName(nextTask),
                "nextTaskInstanceId", TASKS.taskInstanceIdLabel(nextTask),
                "nextTaskRunId", TASKS.taskRunIdLabel(nextTask));
        safeLog("TASK_CHILD", "TRANSITION", reason, parent, merged, false);
    }

    public static void logInput(String phase, String reason, Input input, Object... fields) {
        if (!MODE.isVerboseEnabled()) {
            return;
        }
        Object[] merged = mergeFields(fields,
                "input", DiagnosticInputState.inputName(input),
                "inputCallerStack", callerStack());
        safeLog("INPUT", phase, reason, TASKS.currentTask(), merged, false);
    }

    public static void logInputSnapshot(String phase, String reason, Object... fields) {
        if (!MODE.isVerboseEnabled()) {
            return;
        }
        Object[] merged = mergeFields(DiagnosticInputState.snapshotFields(), fields);
        safeLog("INPUT_SNAPSHOT", phase, reason, TASKS.currentTask(), merged, false);
    }

    public static void logSlotClick(String phase, String reason, Slot slot, int mouseButton, Object type, Object... fields) {
        if (!MODE.isVerboseEnabled()) {
            return;
        }
        Object[] merged = mergeFields(fields,
                "slot", slotSummary(slot),
                "mouseButton", Integer.toString(mouseButton),
                "slotActionType", value(type),
                "cursorStack", safeValue(() -> MinecraftClient.getInstance().player.currentScreenHandler.getCursorStack()));
        safeLog("SLOT", phase, reason, TASKS.currentTask(), merged, false);
    }

    public static void logInteractBlock(String phase, String reason, Object hand, BlockHitResult hitResult, Object result) {
        if (MODE.isOff()) {
            return;
        }
        logInteractBlock(phase, reason, MinecraftClient.getInstance().player, hand, hitResult, result);
    }

    public static void logInteractBlock(String phase, String reason, ClientPlayerEntity player, Object hand, BlockHitResult hitResult, Object result) {
        if (!MODE.isOff()) {
            logPostPlaceContainerInteractIfMatching(phase, player, hand, hitResult, result);
        }
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
        if (MODE.isOff()) {
            return;
        }
        emitEvent("BOUNDARY", "[LAVI ChatClefBoundary]", eventName, reason, task, fields, false);
    }

    //20260803_kpopmodder: Keep command lifecycle diagnostics visible even when broad diagnostics are disabled.
    public static void logLifecycleBoundary(String eventName, String reason, Task task, Object... fields) {
        emitEvent("BOUNDARY", "[LAVI ChatClefLifecycle]", eventName, reason, task, fields, false);
    }

    public static void logWarningEvent(String eventName, String reason, Task task, Object... fields) {
        if (MODE.isOff()) {
            return;
        }
        emitEvent("WARN", "[LAVI ChatClefDiag]", eventName, reason, task, fields, true);
    }

    public static void logVerboseLine(String message) {
        if (!MODE.isVerboseEnabled()) {
            return;
        }
        System.out.println("ALTO CLEF: " + value(message));
    }

    public static void beginPostPlaceContainerOpenIntent(long operationId,
                                                         Object containerType,
                                                         BlockPos targetPosition,
                                                         Object targetBlockState) {
        POST_PLACE_CONTAINERS.begin(operationId, containerType, targetPosition, targetBlockState);
    }

    public static PostPlaceContainerOpenIntent activePostPlaceContainerOpenIntent(BlockPos targetPosition) {
        return POST_PLACE_CONTAINERS.active(targetPosition);
    }

    public static int postPlaceContainerAttemptCount(long operationId) {
        return POST_PLACE_CONTAINERS.attemptCount(operationId);
    }

    public static String postPlaceContainerLastInteractResult(long operationId) {
        return POST_PLACE_CONTAINERS.lastInteractResult(operationId);
    }

    public static long postPlaceContainerElapsedTicks(long operationId) {
        return POST_PLACE_CONTAINERS.elapsedTicks(operationId);
    }

    public static boolean markPostPlaceContainerGuiOpened(long operationId) {
        return POST_PLACE_CONTAINERS.markGuiOpened(operationId);
    }

    public static boolean markPostPlaceContainerGuiTimeout(long operationId) {
        return POST_PLACE_CONTAINERS.markGuiTimeout(operationId);
    }

    public static boolean markPostPlaceContainerWarningLogged(long operationId) {
        return POST_PLACE_CONTAINERS.markWarningLogged(operationId);
    }

    public static boolean isPostPlaceContainerGuiOpened(long operationId) {
        return POST_PLACE_CONTAINERS.isGuiOpened(operationId);
    }

    public static void clearPostPlaceContainerOpenIntent(long operationId) {
        POST_PLACE_CONTAINERS.clear(operationId);
    }

    public static void registerPostPlaceContainerInteractionObserver(PostPlaceContainerInteractionObserver observer) {
        POST_PLACE_CONTAINERS.registerObserver(observer);
    }

    public static String className(Object value) {
        return DiagnosticValueFormatter.className(value);
    }

    public static String safeValue(Supplier<?> supplier) {
        return DiagnosticValueFormatter.safeValue(
                () -> supplier == null ? null : supplier.get(),
                !MODE.isOff()
        );
    }

    public static String safeValueForDiagnosticLog(Supplier<?> supplier) {
        return DiagnosticValueFormatter.safeValue(
                () -> supplier == null ? null : supplier.get(),
                true
        );
    }

    public static String taskSummary(Task task) {
        if (MODE.isOff()) {
            return "unavailable";
        }
        return DiagnosticGameStateFormatter.taskSummary(task, TASKS);
    }

    public static String taskSummaryForDiagnosticLog(Task task) {
        return DiagnosticGameStateFormatter.taskSummary(task, TASKS);
    }

    public static String entitySummary(Entity entity) {
        if (MODE.isOff()) {
            return "unavailable";
        }
        return DiagnosticGameStateFormatter.entitySummary(entity);
    }

    public static String entityDistanceSqrToPlayer(AltoClef mod, Entity entity) {
        if (MODE.isOff()) {
            return "unavailable";
        }
        return DiagnosticGameStateFormatter.entityDistanceSqrToPlayer(mod, entity);
    }

    public static String playerPosition(AltoClef mod) {
        if (MODE.isOff()) {
            return "unavailable";
        }
        return DiagnosticGameStateFormatter.playerPosition(mod);
    }

    public static String vec3d(Vec3d pos) {
        return DiagnosticGameStateFormatter.vec3d(pos);
    }

    public static String blockPos(BlockPos pos) {
        return DiagnosticGameStateFormatter.blockPos(pos);
    }

    public static String itemStackSummary(ItemStack stack) {
        if (MODE.isOff()) {
            return "unavailable";
        }
        return DiagnosticGameStateFormatter.itemStackSummary(stack);
    }

    public static String slotSummary(Slot slot) {
        if (MODE.isOff()) {
            return "unavailable";
        }
        return DiagnosticGameStateFormatter.slotSummary(slot);
    }

    public static String slotStackSummary(Slot slot) {
        if (MODE.isOff()) {
            return "unavailable";
        }
        return DiagnosticGameStateFormatter.slotStackSummary(slot);
    }

    public static String itemTargets(ItemTarget[] targets) {
        if (MODE.isOff()) {
            return "unavailable";
        }
        return DiagnosticGameStateFormatter.itemTargets(targets);
    }

    public static String classList(Class<?>[] classes) {
        if (MODE.isOff()) {
            return "unavailable";
        }
        return DiagnosticGameStateFormatter.classList(classes);
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

    private static String callerStack() {
        return DiagnosticCallerStack.captureExcluding(ChatClefDiagnostics.class);
    }

    private static Object[] mergeFields(Object[] fields, Object... extra) {
        return DiagnosticEventEmitter.mergeFields(fields, extra);
    }

    private static void logRuntimeIdentityOnce() {
        if (!TRACE_STATE.markRuntimeIdentityLogged()) {
            return;
        }
        logBoundary(
                "DIAGNOSTICS_RUNTIME_IDENTITY",
                "diagnostics_runtime_identity",
                null,
                RuntimeIdentityDiagnostics.fields(MODE.current(), ChatClefDiagnostics.class)
        );
    }

    public static String chainName(TaskChain chain) {
        if (MODE.isOff()) {
            return "unavailable";
        }
        return chainNameForDiagnosticLog(chain);
    }

    public static String chainNameForDiagnosticLog(TaskChain chain) {
        if (chain == null) {
            return "unavailable";
        }
        try {
            return chain.getName();
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    private static String taskName(Task task) {
        return DiagnosticEventEmitter.taskName(task);
    }

    private static String value(Object rawValue) {
        return DiagnosticValueFormatter.value(rawValue);
    }
}
