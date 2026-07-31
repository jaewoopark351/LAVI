package lavi.minecraft.diagnostics;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.slots.Slot;
import baritone.api.utils.input.Input;
import lavi.minecraft.diagnostics.postplace.PostPlaceContainerDiagnosticState;
import lavi.minecraft.diagnostics.postplace.PostPlaceContainerInteractionObserver;
import lavi.minecraft.diagnostics.postplace.PostPlaceContainerOpenIntent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.StringJoiner;
import java.util.function.Supplier;

//20260730_kpopmodder: Added diagnostic logging to prove the Carry On interaction failure boundary.
//20260730_kpopmodder: Added generic ChatClef diagnostics to prove interaction/input/task boundaries without changing behavior.
public final class ChatClefDiagnostics {
    private static final DiagnosticTraceState TRACE_STATE = new DiagnosticTraceState();
    private static final DiagnosticTaskRegistry TASKS = new DiagnosticTaskRegistry();
    private static final PostPlaceContainerDiagnosticState POST_PLACE_CONTAINERS = new PostPlaceContainerDiagnosticState();
    private static volatile DiagnosticOutputMode OUTPUT_MODE = DiagnosticOutputMode.fromEnvironment();

    private ChatClefDiagnostics() {
    }

    public static void onClientTickHead() {
        if (OUTPUT_MODE == DiagnosticOutputMode.OFF) {
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
        return OUTPUT_MODE == DiagnosticOutputMode.VERBOSE;
    }

    public static boolean isBoundaryEnabled() {
        return OUTPUT_MODE != DiagnosticOutputMode.OFF;
    }

    //20260731_kpopmodder: Allow the LAVI command layer to switch diagnostics between BOUNDARY and OFF without changing engine behavior.
    public static void setBoundaryEnabled(boolean enabled) {
        OUTPUT_MODE = enabled ? DiagnosticOutputMode.BOUNDARY : DiagnosticOutputMode.OFF;
        TRACE_STATE.resetRuntimeIdentityLogged();
    }

    public static String inputHeldState(Input input) {
        if (OUTPUT_MODE == DiagnosticOutputMode.OFF) {
            return "unavailable";
        }
        return DiagnosticInputState.inputHeld(input);
    }

    public static String rawInputHeldState(Input input) {
        if (OUTPUT_MODE == DiagnosticOutputMode.OFF) {
            return "unavailable";
        }
        return DiagnosticInputState.rawKeyHeld(input);
    }

    public static void startTrace(String reason, Task task, Object... fields) {
        if (OUTPUT_MODE != DiagnosticOutputMode.VERBOSE) {
            return;
        }
        safeLog("TRACE", "START", reason, task, fields, true);
    }

    public static void enterTask(Task task) {
        if (OUTPUT_MODE != DiagnosticOutputMode.VERBOSE) {
            return;
        }
        TASKS.enterTask(task);
    }

    public static void exitTask(Task task) {
        if (OUTPUT_MODE != DiagnosticOutputMode.VERBOSE) {
            return;
        }
        TASKS.exitTask(task);
    }

    public static void beginTaskRun(Task task, TaskChain parentChain) {
        if (OUTPUT_MODE != DiagnosticOutputMode.VERBOSE) {
            return;
        }
        safeLog("TASK", "START", "first_tick", task,
                new Object[]{"parentChain", chainName(parentChain)},
                false,
                true);
    }

    public static void setParent(Task child, Task parent) {
        if (OUTPUT_MODE != DiagnosticOutputMode.VERBOSE) {
            return;
        }
        TASKS.setParent(child, parent);
    }

    public static void logEvent(String eventType, String phase, String reason, Task task, Object... fields) {
        if (OUTPUT_MODE != DiagnosticOutputMode.VERBOSE) {
            return;
        }
        safeLog(eventType, phase, reason, task, fields, false);
    }

    public static void logTaskTransition(Task parent, Task previousTask, Task nextTask, String reason, Object... fields) {
        if (OUTPUT_MODE != DiagnosticOutputMode.VERBOSE) {
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
        if (OUTPUT_MODE != DiagnosticOutputMode.VERBOSE) {
            return;
        }
        Object[] merged = mergeFields(fields,
                "input", DiagnosticInputState.inputName(input),
                "inputCallerStack", callerStack());
        safeLog("INPUT", phase, reason, TASKS.currentTask(), merged, false);
    }

    public static void logInputSnapshot(String phase, String reason, Object... fields) {
        if (OUTPUT_MODE != DiagnosticOutputMode.VERBOSE) {
            return;
        }
        Object[] merged = mergeFields(DiagnosticInputState.snapshotFields(), fields);
        safeLog("INPUT_SNAPSHOT", phase, reason, TASKS.currentTask(), merged, false);
    }

    public static void logSlotClick(String phase, String reason, Slot slot, int mouseButton, Object type, Object... fields) {
        if (OUTPUT_MODE != DiagnosticOutputMode.VERBOSE) {
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
        if (OUTPUT_MODE == DiagnosticOutputMode.OFF) {
            return;
        }
        logInteractBlock(phase, reason, MinecraftClient.getInstance().player, hand, hitResult, result);
    }

    public static void logInteractBlock(String phase, String reason, ClientPlayerEntity player, Object hand, BlockHitResult hitResult, Object result) {
        if (OUTPUT_MODE != DiagnosticOutputMode.OFF) {
            logPostPlaceContainerInteractIfMatching(phase, player, hand, hitResult, result);
        }
        if (OUTPUT_MODE != DiagnosticOutputMode.VERBOSE) {
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
        if (OUTPUT_MODE == DiagnosticOutputMode.OFF) {
            return;
        }
        emitEvent("BOUNDARY", "[LAVI ChatClefBoundary]", eventName, reason, task, fields, false);
    }

    public static void logWarningEvent(String eventName, String reason, Task task, Object... fields) {
        if (OUTPUT_MODE == DiagnosticOutputMode.OFF) {
            return;
        }
        emitEvent("WARN", "[LAVI ChatClefDiag]", eventName, reason, task, fields, true);
    }

    public static void logVerboseLine(String message) {
        if (OUTPUT_MODE != DiagnosticOutputMode.VERBOSE) {
            return;
        }
        System.out.println("ALTO CLEF: " + value(message));
    }

    public static void beginPostPlaceContainerOpenIntent(long operationId,
                                                         Object containerType,
                                                         BlockPos targetPosition,
                                                         Object targetBlockState) {
        if (OUTPUT_MODE == DiagnosticOutputMode.OFF) {
            return;
        }
        POST_PLACE_CONTAINERS.begin(
                operationId,
                value(containerType),
                targetPosition,
                value(targetBlockState),
                currentClientTickId()
        );
    }

    public static PostPlaceContainerOpenIntent activePostPlaceContainerOpenIntent(BlockPos targetPosition) {
        if (OUTPUT_MODE == DiagnosticOutputMode.OFF) {
            return null;
        }
        return POST_PLACE_CONTAINERS.active(targetPosition);
    }

    public static int postPlaceContainerAttemptCount(long operationId) {
        if (OUTPUT_MODE == DiagnosticOutputMode.OFF) {
            return 0;
        }
        return POST_PLACE_CONTAINERS.attemptCount(operationId);
    }

    public static String postPlaceContainerLastInteractResult(long operationId) {
        if (OUTPUT_MODE == DiagnosticOutputMode.OFF) {
            return "unavailable";
        }
        return POST_PLACE_CONTAINERS.lastInteractResult(operationId);
    }

    public static long postPlaceContainerElapsedTicks(long operationId) {
        if (OUTPUT_MODE == DiagnosticOutputMode.OFF) {
            return -1;
        }
        return POST_PLACE_CONTAINERS.elapsedTicks(operationId, currentClientTickId());
    }

    public static boolean markPostPlaceContainerGuiOpened(long operationId) {
        if (OUTPUT_MODE == DiagnosticOutputMode.OFF) {
            return false;
        }
        return POST_PLACE_CONTAINERS.markGuiOpened(operationId);
    }

    public static boolean markPostPlaceContainerGuiTimeout(long operationId) {
        if (OUTPUT_MODE == DiagnosticOutputMode.OFF) {
            return false;
        }
        return POST_PLACE_CONTAINERS.markGuiTimeout(operationId);
    }

    public static boolean markPostPlaceContainerWarningLogged(long operationId) {
        if (OUTPUT_MODE == DiagnosticOutputMode.OFF) {
            return false;
        }
        return POST_PLACE_CONTAINERS.markWarningLogged(operationId);
    }

    public static boolean isPostPlaceContainerGuiOpened(long operationId) {
        if (OUTPUT_MODE == DiagnosticOutputMode.OFF) {
            return false;
        }
        return POST_PLACE_CONTAINERS.isGuiOpened(operationId);
    }

    public static void clearPostPlaceContainerOpenIntent(long operationId) {
        if (OUTPUT_MODE == DiagnosticOutputMode.OFF) {
            return;
        }
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
                OUTPUT_MODE != DiagnosticOutputMode.OFF
        );
    }

    public static String taskSummary(Task task) {
        if (OUTPUT_MODE == DiagnosticOutputMode.OFF) {
            return "unavailable";
        }
        return DiagnosticGameStateFormatter.taskSummary(task, TASKS);
    }

    public static String entitySummary(Entity entity) {
        if (OUTPUT_MODE == DiagnosticOutputMode.OFF) {
            return "unavailable";
        }
        return DiagnosticGameStateFormatter.entitySummary(entity);
    }

    public static String entityDistanceSqrToPlayer(AltoClef mod, Entity entity) {
        if (OUTPUT_MODE == DiagnosticOutputMode.OFF) {
            return "unavailable";
        }
        return DiagnosticGameStateFormatter.entityDistanceSqrToPlayer(mod, entity);
    }

    public static String playerPosition(AltoClef mod) {
        if (OUTPUT_MODE == DiagnosticOutputMode.OFF) {
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
        if (OUTPUT_MODE == DiagnosticOutputMode.OFF) {
            return "unavailable";
        }
        return DiagnosticGameStateFormatter.itemStackSummary(stack);
    }

    public static String slotSummary(Slot slot) {
        if (OUTPUT_MODE == DiagnosticOutputMode.OFF) {
            return "unavailable";
        }
        return DiagnosticGameStateFormatter.slotSummary(slot);
    }

    public static String slotStackSummary(Slot slot) {
        if (OUTPUT_MODE == DiagnosticOutputMode.OFF) {
            return "unavailable";
        }
        return DiagnosticGameStateFormatter.slotStackSummary(slot);
    }

    public static String itemTargets(ItemTarget[] targets) {
        if (OUTPUT_MODE == DiagnosticOutputMode.OFF) {
            return "unavailable";
        }
        return DiagnosticGameStateFormatter.itemTargets(targets);
    }

    public static String classList(Class<?>[] classes) {
        if (OUTPUT_MODE == DiagnosticOutputMode.OFF) {
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
        try {
            StringJoiner log = new StringJoiner(" ");
            DiagnosticEventIdentity eventIdentity;
            long taskInstanceId;
            long taskRunId;
            long parentTaskRunId;
            eventIdentity = TRACE_STATE.nextEventIdentity(startNewTrace);
            taskInstanceId = task == null ? -1 : TASKS.instanceId(task);
            if (startTaskRun && task != null) {
                taskRunId = TASKS.createRunId(task);
            } else {
                taskRunId = task == null ? -1 : TASKS.existingRunId(task);
            }
            parentTaskRunId = task == null ? -1 : TASKS.parentRunId(task);

            append(log, "traceId", eventIdentity.traceId());
            append(log, "clientTickId", eventIdentity.clientTickId());
            append(log, "eventSequence", eventIdentity.eventSequence());
            append(log, "taskInstanceId", DiagnosticTaskRegistry.idLabel(taskInstanceId));
            append(log, "taskRunId", DiagnosticTaskRegistry.idLabel(taskRunId));
            append(log, "parentTaskRunId", DiagnosticTaskRegistry.idLabel(parentTaskRunId));
            append(log, "threadName", Thread.currentThread().getName());
            append(log, "eventType", eventType);
            append(log, "phase", phase);
            append(log, "reason", reason);
            append(log, "taskClass", taskName(task));
            appendPairs(log, DiagnosticScreenState.currentFields());
            appendPairs(log, DiagnosticInputState.currentStateFields());
            appendPairs(log, fields);

            System.out.println("ALTO CLEF: [LAVI ChatClefDiag] " + log);
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    private static void emitEvent(String level,
                                  String prefix,
                                  String eventName,
                                  String reason,
                                  Task task,
                                  Object[] fields,
                                  boolean warning) {
        try {
            StringJoiner log = new StringJoiner(" ");
            DiagnosticEventIdentity eventIdentity;
            long taskInstanceId;
            long taskRunId;
            long parentTaskRunId;
            eventIdentity = TRACE_STATE.nextEventIdentity(false);
            taskInstanceId = task == null ? -1 : TASKS.instanceId(task);
            taskRunId = task == null ? -1 : TASKS.existingRunId(task);
            parentTaskRunId = task == null ? -1 : TASKS.parentRunId(task);

            append(log, "traceId", eventIdentity.traceId());
            append(log, "clientTickId", eventIdentity.clientTickId());
            append(log, "eventSequence", eventIdentity.eventSequence());
            append(log, "taskInstanceId", DiagnosticTaskRegistry.idLabel(taskInstanceId));
            append(log, "taskRunId", DiagnosticTaskRegistry.idLabel(taskRunId));
            append(log, "parentTaskRunId", DiagnosticTaskRegistry.idLabel(parentTaskRunId));
            append(log, "threadName", Thread.currentThread().getName());
            append(log, "level", level);
            append(log, "event", eventName);
            append(log, "reason", reason);
            append(log, "taskClass", taskName(task));
            appendPairs(log, fields);

            if (warning) {
                Debug.logWarning(prefix + " " + log);
            } else {
                System.out.println("ALTO CLEF: " + prefix + " " + log);
            }
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    private static void logPostPlaceContainerInteractIfMatching(String phase,
                                                                ClientPlayerEntity player,
                                                                Object hand,
                                                                BlockHitResult hitResult,
                                                                Object result) {
        if (hitResult == null) {
            return;
        }
        PostPlaceContainerOpenIntent intent = activePostPlaceContainerOpenIntent(hitResult.getBlockPos());
        if (intent == null) {
            return;
        }

        int attempt = POST_PLACE_CONTAINERS.recordInteraction(intent, phase, value(result));
        if (attempt < 0) {
            return;
        }

        if ("HEAD".equals(phase)) {
            POST_PLACE_CONTAINERS.notifyBeforeInteract(intent);
            logBoundary("CONTAINER_INTERACT_ATTEMPT", "post_place_container_interact_attempt", null,
                    "operationId", intent.operationId(),
                    "attempt", attempt,
                    "targetPosition", blockPos(intent.targetPosition()),
                    "targetBlockState", intent.targetBlockState(),
                    "hand", value(hand),
                    "shiftClick", false,
                    "playerSneaking", safeValue(() -> player == null ? null : player.isSneaking()),
                    "rawSneakKeyPressed", DiagnosticInputState.rawKeyHeld(Input.SNEAK));
            return;
        }

        if ("RETURN".equals(phase)) {
            logBoundary("CONTAINER_INTERACT_RESULT", "post_place_container_interact_result", null,
                    "operationId", intent.operationId(),
                    "attempt", attempt,
                    "targetPosition", blockPos(intent.targetPosition()),
                    "targetBlockState", intent.targetBlockState(),
                    "result", value(result));
            POST_PLACE_CONTAINERS.notifyAfterInteract(intent, player, result);
        }
    }

    private static String callerStack() {
        return DiagnosticCallerStack.captureExcluding(ChatClefDiagnostics.class);
    }

    private static void appendPairs(StringJoiner log, Object... fields) {
        if (fields == null) {
            return;
        }
        for (int i = 0; i < fields.length; i += 2) {
            Object key = fields[i];
            Object fieldValue = i + 1 < fields.length ? fields[i + 1] : "missing";
            append(log, String.valueOf(key), value(fieldValue));
        }
    }

    private static void append(StringJoiner log, String key, Object fieldValue) {
        log.add(key + "=" + value(fieldValue));
    }

    private static Object[] mergeFields(Object[] fields, Object... extra) {
        if (fields == null || fields.length == 0) {
            return extra;
        }
        Object[] merged = new Object[fields.length + extra.length];
        System.arraycopy(fields, 0, merged, 0, fields.length);
        System.arraycopy(extra, 0, merged, fields.length, extra.length);
        return merged;
    }

    private static void logRuntimeIdentityOnce() {
        if (!TRACE_STATE.markRuntimeIdentityLogged()) {
            return;
        }
        logBoundary("DIAGNOSTICS_RUNTIME_IDENTITY", "diagnostics_runtime_identity", null,
                "outputMode", OUTPUT_MODE.name(),
                "diagnosticsSourceMarker", "20260731_post_place_handoff_p2",
                "diagnosticsClassCodeSource", diagnosticsCodeSourceLocation(),
                "diagnosticsCodeSourceLastModified", diagnosticsCodeSourceLastModified(),
                "diagnosticsImplementationVersion", diagnosticsImplementationVersion());
    }

    private static String diagnosticsCodeSourceLocation() {
        try {
            java.security.CodeSource source = ChatClefDiagnostics.class.getProtectionDomain().getCodeSource();
            return source == null || source.getLocation() == null ? "unavailable" : source.getLocation().toString();
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    private static String diagnosticsCodeSourceLastModified() {
        try {
            java.security.CodeSource source = ChatClefDiagnostics.class.getProtectionDomain().getCodeSource();
            if (source == null || source.getLocation() == null) {
                return "unavailable";
            }
            java.nio.file.Path path = java.nio.file.Paths.get(source.getLocation().toURI());
            if (!java.nio.file.Files.exists(path)) {
                return "unavailable";
            }
            return java.nio.file.Files.getLastModifiedTime(path).toString();
        } catch (Exception | LinkageError ignored) {
            return "unavailable";
        }
    }

    private static String diagnosticsImplementationVersion() {
        try {
            Package packageInfo = ChatClefDiagnostics.class.getPackage();
            String implementationVersion = packageInfo == null ? null : packageInfo.getImplementationVersion();
            return implementationVersion == null || implementationVersion.isBlank() ? "unavailable" : implementationVersion;
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    public static String chainName(TaskChain chain) {
        if (OUTPUT_MODE == DiagnosticOutputMode.OFF) {
            return "unavailable";
        }
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
        if (task == null) {
            return "none";
        }
        try {
            return task.getClass().getName();
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    private static String value(Object rawValue) {
        return DiagnosticValueFormatter.value(rawValue);
    }
}
