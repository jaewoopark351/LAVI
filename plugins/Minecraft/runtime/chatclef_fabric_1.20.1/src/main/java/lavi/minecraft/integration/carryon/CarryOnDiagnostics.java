package lavi.minecraft.integration.carryon;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.helpers.WorldHelper;
import baritone.api.utils.input.Input;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.concurrent.atomic.AtomicLong;

public final class CarryOnDiagnostics {
    private static final AtomicLong NEXT_CORRELATION_ID = new AtomicLong(1);
    private static final ReflectiveCarryOnStateReader STATE_READER = new ReflectiveCarryOnStateReader();

    private CarryOnDiagnostics() {
    }

    public static long nextCorrelationId() {
        return NEXT_CORRELATION_ID.getAndIncrement();
    }

    public static void logInteractionStart(long correlationId, Task childTask, BlockPos targetPosition,
                                           Object direction, Object toUse, Input interactInput,
                                           boolean shiftClick, boolean walkInto) {
        log("operation=start correlationId=%d childTask=%s targetType=block targetPosition=%s direction=%s toUse=%s input=%s shiftClick=%s walkInto=%s",
                correlationId,
                taskName(childTask),
                position(targetPosition),
                value(direction),
                value(toUse),
                value(interactInput),
                shiftClick,
                walkInto);
    }

    public static String logInteractionStateChange(long correlationId, Task childTask, BlockPos targetPosition,
                                                   Object direction, Object toUse, Input interactInput,
                                                   CarryOnObservation stateBefore,
                                                   CarryOnObservation stateAfter,
                                                   Object clickResult, int attemptCount, int elapsedTicks,
                                                   String previousKey) {
        AltoClef mod = AltoClef.getInstance();
        MinecraftClient client = MinecraftClient.getInstance();
        String key = key(mod, client, stateBefore, stateAfter, targetPosition, clickResult, interactInput);

        if (key.equals(previousKey) && elapsedTicks % 20 != 0) {
            return previousKey;
        }

        log("operation=tick correlationId=%d gameTick=%s topLevelTask=%s childTask=%s previousTask=%s nextTask=%s targetType=block targetId=%s targetPosition=%s dimension=%s carryOnLoaded=%s carryOnVersion=%s carryStateBefore=%s carryStateAfter=%s rightClickState=%s sneakState=%s leftClickState=%s clickResult=%s attemptCount=%d elapsedTicks=%d screenName=%s equippedItem=%s baritonePathing=%s customGoalOwner=%s stopReason=%s exceptionType=%s direction=%s toUse=%s input=%s",
                correlationId,
                gameTick(mod),
                topLevelTask(mod),
                taskName(childTask),
                "not_observed_at_interaction_boundary",
                "not_observed_at_interaction_boundary",
                targetId(mod, targetPosition),
                position(targetPosition),
                dimension(),
                stateAfter.loaded(),
                stateAfter.version(),
                stateBefore.state(),
                stateAfter.state(),
                held(mod, interactInput),
                held(mod, Input.SNEAK),
                held(mod, Input.CLICK_LEFT),
                value(clickResult),
                attemptCount,
                elapsedTicks,
                screenName(client),
                equippedItem(mod),
                baritonePathing(mod),
                customGoalOwner(mod),
                "none",
                exceptionType(stateBefore, stateAfter),
                value(direction),
                value(toUse),
                value(interactInput));
        return key;
    }

    public static void logInteractionStop(long correlationId, Task childTask, BlockPos targetPosition,
                                          Input interactInput, Object interruptTask, int attemptCount, int elapsedTicks) {
        AltoClef mod = AltoClef.getInstance();
        MinecraftClient client = MinecraftClient.getInstance();
        CarryOnObservation observation = observe(client);
        log("operation=stop correlationId=%d gameTick=%s topLevelTask=%s childTask=%s targetType=block targetId=%s targetPosition=%s dimension=%s carryOnLoaded=%s carryOnVersion=%s carryStateBefore=%s carryStateAfter=%s rightClickState=%s sneakState=%s leftClickState=%s attemptCount=%d elapsedTicks=%d screenName=%s equippedItem=%s baritonePathing=%s customGoalOwner=%s stopReason=%s exceptionType=%s",
                correlationId,
                gameTick(mod),
                topLevelTask(mod),
                taskName(childTask),
                targetId(mod, targetPosition),
                position(targetPosition),
                dimension(),
                observation.loaded(),
                observation.version(),
                observation.state(),
                observation.state(),
                held(mod, interactInput),
                held(mod, Input.SNEAK),
                held(mod, Input.CLICK_LEFT),
                attemptCount,
                elapsedTicks,
                screenName(client),
                equippedItem(mod),
                baritonePathing(mod),
                customGoalOwner(mod),
                interruptTask == null ? "clean_stop" : "interrupted_by_" + taskName(interruptTask),
                observation.exceptionType());
    }

    public static CarryOnObservation observe() {
        return observe(MinecraftClient.getInstance());
    }

    private static CarryOnObservation observe(MinecraftClient client) {
        return STATE_READER.observe(client == null ? null : client.player);
    }

    private static String key(AltoClef mod, MinecraftClient client, CarryOnObservation stateBefore,
                              CarryOnObservation stateAfter,
                              BlockPos targetPosition, Object clickResult, Input interactInput) {
        return stateBefore.state()
                + "|" + stateAfter.state()
                + "|" + exceptionType(stateBefore, stateAfter)
                + "|" + value(clickResult)
                + "|" + held(mod, interactInput)
                + "|" + held(mod, Input.SNEAK)
                + "|" + held(mod, Input.CLICK_LEFT)
                + "|" + baritonePathing(mod)
                + "|" + customGoalOwner(mod)
                + "|" + screenName(client)
                + "|" + targetId(mod, targetPosition);
    }

    private static String exceptionType(CarryOnObservation stateBefore, CarryOnObservation stateAfter) {
        if (!"none".equals(stateAfter.exceptionType())) {
            return stateAfter.exceptionType();
        }
        return stateBefore.exceptionType();
    }

    private static String gameTick(AltoClef mod) {
        if (mod == null || mod.getWorld() == null) {
            return "unknown";
        }
        return Long.toString(mod.getWorld().getTime());
    }

    private static String topLevelTask(AltoClef mod) {
        if (mod == null || mod.getUserTaskChain() == null || mod.getUserTaskChain().getCurrentTask() == null) {
            return "none";
        }
        return taskName(mod.getUserTaskChain().getCurrentTask());
    }

    private static String targetId(AltoClef mod, BlockPos targetPosition) {
        if (mod == null || mod.getWorld() == null || targetPosition == null) {
            return "unknown";
        }
        return String.valueOf(mod.getWorld().getBlockState(targetPosition).getBlock());
    }

    private static String dimension() {
        try {
            return String.valueOf(WorldHelper.getCurrentDimension());
        } catch (RuntimeException e) {
            return "unknown";
        }
    }

    private static String held(AltoClef mod, Input input) {
        if (mod == null || input == null) {
            return "unknown";
        }
        return Boolean.toString(mod.getInputControls().isHeldDown(input));
    }

    private static String screenName(MinecraftClient client) {
        if (client == null || client.currentScreen == null) {
            return "none";
        }
        return client.currentScreen.getClass().getSimpleName();
    }

    private static String equippedItem(AltoClef mod) {
        if (mod == null || mod.getPlayer() == null || mod.getPlayer().getMainHandStack() == null) {
            return "unknown";
        }
        return String.valueOf(mod.getPlayer().getMainHandStack().getItem());
    }

    private static String baritonePathing(AltoClef mod) {
        if (mod == null || mod.getClientBaritone() == null) {
            return "unknown";
        }
        return Boolean.toString(mod.getClientBaritone().getPathingBehavior().isPathing());
    }

    private static String customGoalOwner(AltoClef mod) {
        if (mod == null || mod.getClientBaritone() == null) {
            return "unknown";
        }
        return mod.getClientBaritone().getCustomGoalProcess().isActive() ? "active_owner_unknown" : "none";
    }

    private static String taskName(Object task) {
        if (task == null) {
            return "none";
        }
        return task.getClass().getSimpleName();
    }

    private static String position(BlockPos position) {
        return position == null ? "unknown" : position.toShortString();
    }

    private static String value(Object value) {
        return value == null ? "null" : String.valueOf(value);
    }

    private static void log(String format, Object... args) {
        Debug.logWarning("[LAVI CarryOnDiag] " + String.format(format, args));
    }
}
