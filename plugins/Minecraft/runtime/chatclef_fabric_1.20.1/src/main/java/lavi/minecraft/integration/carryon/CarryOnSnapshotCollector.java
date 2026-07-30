package lavi.minecraft.integration.carryon;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.util.helpers.WorldHelper;
import baritone.api.utils.input.Input;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.StringJoiner;

//20260730_kpopmodder: Collect Carry On diagnostic snapshots without owning engine behavior.
public final class CarryOnSnapshotCollector {
    private CarryOnSnapshotCollector() {
    }

    public static CarryOnSnapshot collect(long taskInstanceId,
                                          long operationId,
                                          String eventName,
                                          CarryOnOperationType operationType,
                                          CarryOnTransition expectedTransition,
                                          String childTask,
                                          String targetType,
                                          String targetId,
                                          String targetPosition,
                                          CarryOnObservation stateBefore,
                                          CarryOnObservation stateAfter,
                                          String clickResult,
                                          int attemptCount,
                                          int elapsedTicks,
                                          CarryOnTerminalReason terminalReason) {
        AltoClef mod = AltoClef.getInstance();
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client == null ? null : client.player;

        return new CarryOnSnapshot(
                taskInstanceId,
                operationId,
                value(eventName),
                operationType == null ? CarryOnOperationType.OBSERVATION_ONLY : operationType,
                expectedTransition == null ? CarryOnTransition.NONE : expectedTransition,
                gameTick(mod),
                Thread.currentThread().getName(),
                topLevelTask(mod),
                value(childTask),
                value(targetType),
                value(targetId),
                value(targetPosition),
                dimension(),
                currentChain(mod),
                taskChain(mod),
                taskRunnerActive(mod),
                userTaskChainActive(mod),
                paused(mod),
                chatClefEnabled(mod),
                playerMode(mod),
                playerPosition(player),
                playerVelocity(player),
                lookRotation(player),
                playerPoseState(player),
                inputState(mod, Input.CLICK_RIGHT),
                inputState(mod, Input.SNEAK),
                inputState(mod, Input.CLICK_LEFT),
                movementInputState(mod),
                baritonePathing(mod),
                customGoalOwner(mod),
                breakingBlockState(mod),
                crosshairType(client),
                crosshairTarget(client),
                crosshairBlockId(client),
                stateBefore,
                stateAfter,
                value(clickResult),
                attemptCount,
                elapsedTicks,
                screenName(client),
                screenHandlerName(player),
                screenHandlerSyncId(player),
                cursorStack(player),
                selectedHotbarSlot(player),
                mainHandItem(player),
                offHandItem(player),
                terminalReason == null ? CarryOnTerminalReason.UNAVAILABLE : terminalReason
        );
    }

    private static long gameTick(AltoClef mod) {
        if (mod == null || mod.getWorld() == null) {
            return -1;
        }
        return mod.getWorld().getTime();
    }

    private static String topLevelTask(AltoClef mod) {
        if (mod == null || mod.getUserTaskChain() == null || mod.getUserTaskChain().getCurrentTask() == null) {
            return "unavailable";
        }
        return taskName(mod.getUserTaskChain().getCurrentTask());
    }

    private static String currentChain(AltoClef mod) {
        try {
            if (mod == null || mod.getTaskRunner() == null || mod.getTaskRunner().getCurrentTaskChain() == null) {
                return "unavailable";
            }
            return value(mod.getTaskRunner().getCurrentTaskChain().getName());
        } catch (RuntimeException | LinkageError e) {
            return unavailable(e);
        }
    }

    private static String taskChain(AltoClef mod) {
        try {
            if (mod == null || mod.getTaskRunner() == null || mod.getTaskRunner().getCurrentTaskChain() == null) {
                return "unavailable";
            }
            TaskChain chain = mod.getTaskRunner().getCurrentTaskChain();
            List<Task> tasks = chain.getTasks();
            if (tasks == null || tasks.isEmpty()) {
                return "empty";
            }
            StringJoiner joiner = new StringJoiner(" > ");
            for (Task task : tasks) {
                joiner.add(value(task).replace('\n', ' '));
            }
            return joiner.toString();
        } catch (RuntimeException | LinkageError e) {
            return unavailable(e);
        }
    }

    private static String taskRunnerActive(AltoClef mod) {
        try {
            return mod == null || mod.getTaskRunner() == null
                    ? "unavailable"
                    : Boolean.toString(mod.getTaskRunner().isActive());
        } catch (RuntimeException | LinkageError e) {
            return unavailable(e);
        }
    }

    private static String userTaskChainActive(AltoClef mod) {
        try {
            return mod == null || mod.getUserTaskChain() == null
                    ? "unavailable"
                    : Boolean.toString(mod.getUserTaskChain().isActive());
        } catch (RuntimeException | LinkageError e) {
            return unavailable(e);
        }
    }

    private static String paused(AltoClef mod) {
        try {
            return mod == null ? "unavailable" : Boolean.toString(mod.isPaused());
        } catch (RuntimeException | LinkageError e) {
            return unavailable(e);
        }
    }

    private static String chatClefEnabled(AltoClef mod) {
        try {
            return mod == null || mod.getAiBridge() == null
                    ? "unavailable"
                    : Boolean.toString(mod.getAiBridge().getEnabled());
        } catch (RuntimeException | LinkageError e) {
            return unavailable(e);
        }
    }

    private static String playerMode(AltoClef mod) {
        try {
            return mod == null || mod.getAiBridge() == null
                    ? "unavailable"
                    : Boolean.toString(mod.getAiBridge().getPlayerMode());
        } catch (RuntimeException | LinkageError e) {
            return unavailable(e);
        }
    }

    private static String dimension() {
        try {
            return String.valueOf(WorldHelper.getCurrentDimension());
        } catch (RuntimeException e) {
            return "unavailable";
        }
    }

    private static String screenName(MinecraftClient client) {
        if (client == null || client.currentScreen == null) {
            return "none";
        }
        return client.currentScreen.getClass().getSimpleName();
    }

    private static String playerPosition(ClientPlayerEntity player) {
        if (player == null) {
            return "unavailable";
        }
        return player.getBlockPos().toShortString();
    }

    private static String playerVelocity(ClientPlayerEntity player) {
        if (player == null) {
            return "unavailable";
        }
        Vec3d velocity = player.getVelocity();
        return String.format("%.3f/%.3f/%.3f", velocity.x, velocity.y, velocity.z);
    }

    private static String lookRotation(ClientPlayerEntity player) {
        if (player == null) {
            return "unavailable";
        }
        return String.format("%.2f/%.2f", player.getYaw(), player.getPitch());
    }

    private static String playerPoseState(ClientPlayerEntity player) {
        if (player == null) {
            return "unavailable";
        }
        return "onGround=" + player.isOnGround()
                + ",sneaking=" + player.isSneaking()
                + ",sprinting=" + player.isSprinting()
                + ",usingItem=" + player.isUsingItem()
                + ",blocking=" + player.isBlocking();
    }

    private static String mainHandItem(ClientPlayerEntity player) {
        if (player == null || player.getMainHandStack() == null) {
            return "unavailable";
        }
        return String.valueOf(player.getMainHandStack().getItem());
    }

    private static String offHandItem(ClientPlayerEntity player) {
        if (player == null || player.getOffHandStack() == null) {
            return "unavailable";
        }
        return String.valueOf(player.getOffHandStack().getItem());
    }

    private static String screenHandlerName(ClientPlayerEntity player) {
        if (player == null || player.currentScreenHandler == null) {
            return "unavailable";
        }
        return player.currentScreenHandler.getClass().getSimpleName();
    }

    private static String screenHandlerSyncId(ClientPlayerEntity player) {
        if (player == null || player.currentScreenHandler == null) {
            return "unavailable";
        }
        return Integer.toString(player.currentScreenHandler.syncId);
    }

    private static String cursorStack(ClientPlayerEntity player) {
        if (player == null || player.currentScreenHandler == null) {
            return "unavailable";
        }
        ItemStack cursorStack = player.currentScreenHandler.getCursorStack();
        return cursorStack == null ? "unavailable" : String.valueOf(cursorStack);
    }

    private static String selectedHotbarSlot(ClientPlayerEntity player) {
        if (player == null || player.getInventory() == null) {
            return "unavailable";
        }
        return Integer.toString(player.getInventory().selectedSlot);
    }

    private static String taskName(Object task) {
        return task == null ? "unavailable" : task.getClass().getSimpleName();
    }

    private static String inputState(AltoClef mod, Input input) {
        try {
            if (mod == null || mod.getInputControls() == null) {
                return "unavailable";
            }
            return Boolean.toString(mod.getInputControls().isHeldDown(input));
        } catch (RuntimeException | LinkageError e) {
            return unavailable(e);
        }
    }

    private static String movementInputState(AltoClef mod) {
        return "forward=" + inputState(mod, Input.MOVE_FORWARD)
                + ",back=" + inputState(mod, Input.MOVE_BACK)
                + ",left=" + inputState(mod, Input.MOVE_LEFT)
                + ",right=" + inputState(mod, Input.MOVE_RIGHT)
                + ",jump=" + inputState(mod, Input.JUMP)
                + ",sprint=" + inputState(mod, Input.SPRINT);
    }

    private static String baritonePathing(AltoClef mod) {
        try {
            if (mod == null || mod.getClientBaritone() == null) {
                return "unavailable";
            }
            return Boolean.toString(mod.getClientBaritone().getPathingBehavior().isPathing());
        } catch (RuntimeException | LinkageError e) {
            return unavailable(e);
        }
    }

    private static String customGoalOwner(AltoClef mod) {
        try {
            if (mod == null || mod.getClientBaritone() == null || mod.getClientBaritone().getCustomGoalProcess() == null) {
                return "unavailable";
            }
            return mod.getClientBaritone().getCustomGoalProcess().isActive() ? "active_owner_unavailable" : "inactive";
        } catch (RuntimeException | LinkageError e) {
            return unavailable(e);
        }
    }

    private static String breakingBlockState(AltoClef mod) {
        try {
            if (mod == null || mod.getControllerExtras() == null || mod.getWorld() == null) {
                return "unavailable";
            }
            if (!mod.getControllerExtras().isBreakingBlock()) {
                return "false";
            }
            BlockPos pos = mod.getControllerExtras().getBreakingBlockPos();
            if (pos == null) {
                return "true@unavailable";
            }
            BlockState state = mod.getWorld().getBlockState(pos);
            return "true@" + pos.toShortString() + ":" + state.getBlock();
        } catch (RuntimeException | LinkageError e) {
            return unavailable(e);
        }
    }

    private static String crosshairType(MinecraftClient client) {
        HitResult result = client == null ? null : client.crosshairTarget;
        return result == null ? "unavailable" : String.valueOf(result.getType());
    }

    private static String crosshairTarget(MinecraftClient client) {
        try {
            HitResult result = client == null ? null : client.crosshairTarget;
            if (result == null) {
                return "unavailable";
            }
            if (result instanceof BlockHitResult blockHitResult) {
                return blockHitResult.getBlockPos().toShortString() + ":" + blockHitResult.getSide();
            }
            if (result instanceof EntityHitResult entityHitResult) {
                Entity entity = entityHitResult.getEntity();
                return entity == null ? "entity:unavailable" : entity.getType() + ":" + entity.getUuidAsString();
            }
            return value(result.getPos());
        } catch (RuntimeException | LinkageError e) {
            return unavailable(e);
        }
    }

    private static String crosshairBlockId(MinecraftClient client) {
        try {
            if (client == null || client.world == null || !(client.crosshairTarget instanceof BlockHitResult blockHitResult)) {
                return "unavailable";
            }
            return String.valueOf(client.world.getBlockState(blockHitResult.getBlockPos()).getBlock());
        } catch (RuntimeException | LinkageError e) {
            return unavailable(e);
        }
    }

    private static String value(String value) {
        return value == null || value.isBlank() ? "unavailable" : value;
    }

    private static String value(Object value) {
        return value == null ? "unavailable" : String.valueOf(value);
    }

    private static String unavailable(Throwable throwable) {
        return throwable == null ? "unavailable" : "unavailable:" + throwable.getClass().getSimpleName();
    }
}
