package lavi.minecraft.diagnostics;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.slots.Slot;
import baritone.api.utils.input.Input;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.StringJoiner;
import java.util.function.Supplier;

//20260730_kpopmodder: Added diagnostic logging to prove the Carry On interaction failure boundary.
//20260730_kpopmodder: Added generic ChatClef diagnostics to prove interaction/input/task boundaries without changing behavior.
public final class ChatClefDiagnostics {
    private static final Object LOCK = new Object();
    private static final IdentityHashMap<Object, Long> TASK_INSTANCE_IDS = new IdentityHashMap<>();
    private static final IdentityHashMap<Object, Long> TASK_RUN_IDS = new IdentityHashMap<>();
    private static final IdentityHashMap<Object, Long> PARENT_TASK_RUN_IDS = new IdentityHashMap<>();
    private static final ThreadLocal<Deque<Task>> TASK_STACK = ThreadLocal.withInitial(ArrayDeque::new);

    private static long nextTaskInstanceId = 1;
    private static long nextTaskRunId = 1;
    private static long nextTraceId = 1;
    private static long clientTickId = 0;
    private static long eventSequence = 0;
    private static String traceId = "unavailable";

    private ChatClefDiagnostics() {
    }

    public static void onClientTickHead() {
        try {
            synchronized (LOCK) {
                clientTickId++;
            }
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    public static String currentTraceId() {
        synchronized (LOCK) {
            return traceId;
        }
    }

    public static long currentClientTickId() {
        synchronized (LOCK) {
            return clientTickId;
        }
    }

    public static long nextEventSequence() {
        synchronized (LOCK) {
            return ++eventSequence;
        }
    }

    public static void startTrace(String reason, Task task, Object... fields) {
        safeLog("TRACE", "START", reason, task, fields, true);
    }

    public static void enterTask(Task task) {
        try {
            TASK_STACK.get().push(task);
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    public static void exitTask(Task task) {
        try {
            Deque<Task> stack = TASK_STACK.get();
            if (!stack.isEmpty() && stack.peek() == task) {
                stack.pop();
                return;
            }
            stack.remove(task);
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    public static void beginTaskRun(Task task, TaskChain parentChain) {
        safeLog("TASK", "START", "first_tick", task,
                new Object[]{"parentChain", chainName(parentChain)},
                false,
                true);
    }

    public static void setParent(Task child, Task parent) {
        try {
            synchronized (LOCK) {
                long parentRunId = existingRunId(parent);
                if (child != null) {
                    PARENT_TASK_RUN_IDS.put(child, parentRunId);
                }
            }
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    public static void logEvent(String eventType, String phase, String reason, Task task, Object... fields) {
        safeLog(eventType, phase, reason, task, fields, false);
    }

    public static void logTaskTransition(Task parent, Task previousTask, Task nextTask, String reason, Object... fields) {
        Object[] merged = mergeFields(fields,
                "previousTask", taskName(previousTask),
                "previousTaskInstanceId", taskInstanceIdLabel(previousTask),
                "previousTaskRunId", taskRunIdLabel(previousTask),
                "nextTask", taskName(nextTask),
                "nextTaskInstanceId", taskInstanceIdLabel(nextTask),
                "nextTaskRunId", taskRunIdLabel(nextTask));
        safeLog("TASK_CHILD", "TRANSITION", reason, parent, merged, false);
    }

    public static void logInput(String phase, String reason, Input input, Object... fields) {
        Object[] merged = mergeFields(fields,
                "input", inputName(input),
                "inputCallerStack", callerStack());
        safeLog("INPUT", phase, reason, currentTask(), merged, false);
    }

    public static void logInputSnapshot(String phase, String reason, Object... fields) {
        Object[] merged = mergeFields(inputSnapshotFields(), fields);
        safeLog("INPUT_SNAPSHOT", phase, reason, currentTask(), merged, false);
    }

    public static void logSlotClick(String phase, String reason, Slot slot, int mouseButton, Object type, Object... fields) {
        Object[] merged = mergeFields(fields,
                "slot", slotSummary(slot),
                "mouseButton", Integer.toString(mouseButton),
                "slotActionType", value(type),
                "cursorStack", safeValue(() -> MinecraftClient.getInstance().player.currentScreenHandler.getCursorStack()));
        safeLog("SLOT", phase, reason, currentTask(), merged, false);
    }

    public static void logInteractBlock(String phase, String reason, Object hand, BlockHitResult hitResult, Object result) {
        logInteractBlock(phase, reason, MinecraftClient.getInstance().player, hand, hitResult, result);
    }

    public static void logInteractBlock(String phase, String reason, ClientPlayerEntity player, Object hand, BlockHitResult hitResult, Object result) {
        BlockPos blockPos = hitResult == null ? null : hitResult.getBlockPos();
        safeLog("MINECRAFT_INTERACTION", phase, reason, currentTask(), new Object[]{
                "hand", value(hand),
                "hitBlockPos", blockPos == null ? "unavailable" : value(blockPos),
                "hitSide", hitResult == null ? "unavailable" : value(hitResult.getSide()),
                "hitType", hitResult == null ? "unavailable" : value(hitResult.getType()),
                "targetBlockState", blockPos == null ? "unavailable" : safeValue(() -> MinecraftClient.getInstance().world.getBlockState(blockPos)),
                "clientPlayerSneaking", safeValue(() -> MinecraftClient.getInstance().player.isSneaking()),
                "argumentPlayerSneaking", safeValue(() -> player == null ? null : player.isSneaking()),
                "clientPlayerSameAsArgument", safeValue(() -> MinecraftClient.getInstance().player == player),
                "rawUseKeyPressed", rawKeyHeld(Input.CLICK_RIGHT),
                "rawSneakKeyPressed", rawKeyHeld(Input.SNEAK),
                "rawAttackKeyPressed", rawKeyHeld(Input.CLICK_LEFT),
                "sneakAndUsePressedTogether", sneakAndUsePressedTogether(),
                "clientPlayerPosition", safeValue(() -> MinecraftClient.getInstance().player.getPos()),
                "argumentPlayerPosition", safeValue(() -> player == null ? null : player.getPos()),
                "clientMainHandItem", safeValue(() -> MinecraftClient.getInstance().player.getMainHandStack()),
                "argumentMainHandItem", safeValue(() -> player == null ? null : player.getMainHandStack()),
                "clientOffHandItem", safeValue(() -> MinecraftClient.getInstance().player.getOffHandStack()),
                "argumentOffHandItem", safeValue(() -> player == null ? null : player.getOffHandStack()),
                "result", value(result)
        }, false);
    }

    public static String className(Object value) {
        if (value == null) {
            return "none";
        }
        try {
            return value.getClass().getName();
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    public static String safeValue(Supplier<?> supplier) {
        try {
            return value(supplier == null ? null : supplier.get());
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    public static String taskSummary(Task task) {
        if (task == null) {
            return "none";
        }
        return taskName(task)
                + "#instance=" + taskInstanceIdLabel(task)
                + "#run=" + taskRunIdLabel(task);
    }

    public static String entitySummary(Entity entity) {
        if (entity == null) {
            return "none";
        }
        try {
            return className(entity)
                    + "#id=" + entity.getId()
                    + "#uuid=" + entity.getUuid()
                    + "#type=" + entity.getType().getTranslationKey()
                    + "#pos=" + vec3d(entity.getPos())
                    + "#blockPos=" + blockPos(entity.getBlockPos())
                    + "#velocity=" + vec3d(entity.getVelocity())
                    + "#alive=" + entity.isAlive()
                    + "#removed=" + entity.isRemoved();
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    public static String entityDistanceSqrToPlayer(AltoClef mod, Entity entity) {
        try {
            if (mod == null || mod.getPlayer() == null || entity == null) {
                return "unavailable";
            }
            return Double.toString(entity.squaredDistanceTo(mod.getPlayer()));
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    public static String playerPosition(AltoClef mod) {
        try {
            if (mod == null || mod.getPlayer() == null) {
                return "unavailable";
            }
            return vec3d(mod.getPlayer().getPos());
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    public static String vec3d(Vec3d pos) {
        if (pos == null) {
            return "unavailable";
        }
        try {
            return pos.getX() + "/" + pos.getY() + "/" + pos.getZ();
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    public static String blockPos(BlockPos pos) {
        if (pos == null) {
            return "unavailable";
        }
        try {
            return pos.getX() + "," + pos.getY() + "," + pos.getZ();
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    public static String itemStackSummary(ItemStack stack) {
        if (stack == null) {
            return "none";
        }
        try {
            return stack.getCount() + "x" + stack.getItem().getTranslationKey() + "#empty=" + stack.isEmpty();
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    public static String slotSummary(Slot slot) {
        if (slot == null) {
            return "none";
        }
        try {
            return value(slot)
                    + "#inventorySlot=" + slot.getInventorySlot()
                    + "#windowSlot=" + slot.getWindowSlot()
                    + "#playerInventory=" + slot.isSlotInPlayerInventory();
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    public static String itemTargets(ItemTarget[] targets) {
        if (targets == null) {
            return "null";
        }
        try {
            StringJoiner joiner = new StringJoiner(",", "[", "]");
            for (ItemTarget target : targets) {
                joiner.add(value(target));
            }
            return joiner.toString();
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    public static String classList(Class<?>[] classes) {
        if (classes == null) {
            return "all_tracked_entity_types";
        }
        try {
            StringJoiner joiner = new StringJoiner(",", "[", "]");
            for (Class<?> clazz : classes) {
                joiner.add(clazz == null ? "null" : clazz.getName());
            }
            return joiner.toString();
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    private static void safeLog(String eventType, String phase, String reason, Task task, Object[] fields, boolean startNewTrace) {
        safeLog(eventType, phase, reason, task, fields, startNewTrace, false);
    }

    private static void safeLog(String eventType, String phase, String reason, Task task, Object[] fields, boolean startNewTrace, boolean startTaskRun) {
        try {
            StringJoiner log = new StringJoiner(" ");
            long sequence;
            long tick;
            String currentTrace;
            long taskInstanceId;
            long taskRunId;
            long parentTaskRunId;
            synchronized (LOCK) {
                if (startNewTrace || "unavailable".equals(traceId)) {
                    traceId = "trace-" + nextTraceId++;
                }
                currentTrace = traceId;
                sequence = ++eventSequence;
                tick = clientTickId;
                taskInstanceId = task == null ? -1 : instanceId(task);
                if (startTaskRun && task != null) {
                    taskRunId = createRunId(task);
                    TASK_RUN_IDS.put(task, taskRunId);
                } else {
                    taskRunId = task == null ? -1 : existingRunId(task);
                }
                parentTaskRunId = task == null ? -1 : parentRunId(task);
            }

            append(log, "traceId", currentTrace);
            append(log, "clientTickId", tick);
            append(log, "eventSequence", sequence);
            append(log, "taskInstanceId", idLabel(taskInstanceId));
            append(log, "taskRunId", idLabel(taskRunId));
            append(log, "parentTaskRunId", idLabel(parentTaskRunId));
            append(log, "threadName", Thread.currentThread().getName());
            append(log, "eventType", eventType);
            append(log, "phase", phase);
            append(log, "reason", reason);
            append(log, "taskClass", taskName(task));
            appendScreen(log);
            appendInputStates(log);
            appendPairs(log, fields);

            Debug.logWarning("[LAVI ChatClefDiag] " + log);
        } catch (RuntimeException | LinkageError ignored) {
        }
    }

    private static Task currentTask() {
        try {
            Deque<Task> stack = TASK_STACK.get();
            return stack.isEmpty() ? null : stack.peek();
        } catch (RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    private static long instanceId(Object task) {
        Long existing = TASK_INSTANCE_IDS.get(task);
        if (existing != null) {
            return existing;
        }
        long created = nextTaskInstanceId++;
        TASK_INSTANCE_IDS.put(task, created);
        return created;
    }

    private static long runId(Object task) {
        return existingRunId(task);
    }

    private static long existingRunId(Object task) {
        Long existing = TASK_RUN_IDS.get(task);
        return existing == null ? -1 : existing;
    }

    private static long createRunId(Object task) {
        long created = nextTaskRunId++;
        TASK_RUN_IDS.put(task, created);
        return created;
    }

    private static long parentRunId(Object task) {
        Long existing = PARENT_TASK_RUN_IDS.get(task);
        return existing == null ? -1 : existing;
    }

    private static String taskInstanceIdLabel(Task task) {
        if (task == null) {
            return "unavailable";
        }
        synchronized (LOCK) {
            return idLabel(instanceId(task));
        }
    }

    private static String taskRunIdLabel(Task task) {
        if (task == null) {
            return "unavailable";
        }
        synchronized (LOCK) {
            return idLabel(runId(task));
        }
    }

    private static String idLabel(long id) {
        return id < 0 ? "unavailable" : Long.toString(id);
    }

    private static void appendScreen(StringJoiner log) {
        MinecraftClient client = MinecraftClient.getInstance();
        append(log, "currentScreenClass", client == null ? "unavailable" : className(client.currentScreen));
        ScreenHandler handler = null;
        if (client != null) {
            ClientPlayerEntity player = client.player;
            if (player != null) {
                handler = player.currentScreenHandler;
            }
        }
        append(log, "currentScreenHandlerClass", handler == null ? "unavailable" : className(handler));
        append(log, "screenHandlerSyncId", handler == null ? "unavailable" : Integer.toString(handler.syncId));
    }

    private static void appendInputStates(StringJoiner log) {
        append(log, "rightClickHeld", inputHeld(Input.CLICK_RIGHT));
        append(log, "leftClickHeld", inputHeld(Input.CLICK_LEFT));
        append(log, "sneakHeld", inputHeld(Input.SNEAK));
        append(log, "rawUseKeyPressed", rawKeyHeld(Input.CLICK_RIGHT));
        append(log, "rawAttackKeyPressed", rawKeyHeld(Input.CLICK_LEFT));
        append(log, "rawSneakKeyPressed", rawKeyHeld(Input.SNEAK));
        append(log, "playerSneaking", safeValue(() -> MinecraftClient.getInstance().player.isSneaking()));
        append(log, "sneakAndUsePressedTogether", sneakAndUsePressedTogether());
    }

    private static Object[] inputSnapshotFields() {
        return new Object[]{
                "snapshotRightClickHeld", inputHeld(Input.CLICK_RIGHT),
                "snapshotLeftClickHeld", inputHeld(Input.CLICK_LEFT),
                "snapshotSneakHeld", inputHeld(Input.SNEAK),
                "snapshotRawUseKeyPressed", rawKeyHeld(Input.CLICK_RIGHT),
                "snapshotRawAttackKeyPressed", rawKeyHeld(Input.CLICK_LEFT),
                "snapshotRawSneakKeyPressed", rawKeyHeld(Input.SNEAK),
                "snapshotPlayerSneaking", safeValue(() -> MinecraftClient.getInstance().player.isSneaking()),
                "snapshotSneakAndUsePressedTogether", sneakAndUsePressedTogether(),
                "snapshotMainHandItem", safeValue(() -> MinecraftClient.getInstance().player.getMainHandStack()),
                "snapshotOffHandItem", safeValue(() -> MinecraftClient.getInstance().player.getOffHandStack()),
                "snapshotPlayerPosition", safeValue(() -> MinecraftClient.getInstance().player.getPos()),
                "snapshotCrosshairTarget", safeValue(() -> MinecraftClient.getInstance().crosshairTarget)
        };
    }

    private static String sneakAndUsePressedTogether() {
        try {
            return Boolean.toString(Boolean.parseBoolean(rawKeyHeld(Input.SNEAK))
                    && Boolean.parseBoolean(rawKeyHeld(Input.CLICK_RIGHT)));
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    private static String callerStack() {
        try {
            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            StringJoiner joiner = new StringJoiner("<-");
            int added = 0;
            for (int i = 3; i < stack.length && added < 10; i++) {
                StackTraceElement element = stack[i];
                String className = element.getClassName();
                if (className.equals(ChatClefDiagnostics.class.getName())) {
                    continue;
                }
                joiner.add(className + "." + element.getMethodName() + ":" + element.getLineNumber());
                added++;
            }
            return added == 0 ? "unavailable" : joiner.toString();
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    private static String inputHeld(Input input) {
        try {
            AltoClef instance = AltoClef.getInstance();
            if (instance == null || instance.getInputControls() == null) {
                return "unavailable";
            }
            return Boolean.toString(instance.getInputControls().isHeldDown(input));
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    private static String rawKeyHeld(Input input) {
        try {
            KeyBinding key = inputToKeyBinding(input);
            return key == null ? "unavailable" : Boolean.toString(key.isPressed());
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    private static KeyBinding inputToKeyBinding(Input input) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return null;
        }
        GameOptions options = client.options;
        if (options == null || input == null) {
            return null;
        }
        return switch (input) {
            case MOVE_FORWARD -> options.forwardKey;
            case MOVE_BACK -> options.backKey;
            case MOVE_LEFT -> options.leftKey;
            case MOVE_RIGHT -> options.rightKey;
            case CLICK_LEFT -> options.attackKey;
            case CLICK_RIGHT -> options.useKey;
            case JUMP -> options.jumpKey;
            case SNEAK -> options.sneakKey;
            case SPRINT -> options.sprintKey;
            default -> null;
        };
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

    public static String chainName(TaskChain chain) {
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

    private static String inputName(Input input) {
        if (input == null) {
            return "unavailable";
        }
        try {
            return input.name();
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    private static String value(Object rawValue) {
        if (rawValue == null) {
            return "null";
        }
        String stringValue;
        try {
            stringValue = String.valueOf(rawValue);
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
        return stringValue
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ');
    }
}
