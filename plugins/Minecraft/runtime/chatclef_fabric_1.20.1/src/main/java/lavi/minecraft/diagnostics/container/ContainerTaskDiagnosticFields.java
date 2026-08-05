package lavi.minecraft.diagnostics.container;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import baritone.api.utils.input.Input;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.integration.carryon.CarryOnObservation;
import lavi.minecraft.integration.carryon.snapshot.CarryOnBaritoneSnapshot;
import lavi.minecraft.integration.carryon.snapshot.CarryOnBaritoneSnapshotCollector;
import lavi.minecraft.integration.carryon.snapshot.CarryOnPlayerSnapshot;
import lavi.minecraft.integration.carryon.snapshot.CarryOnPlayerSnapshotCollector;
import lavi.minecraft.integration.carryon.snapshot.CarryOnScreenSnapshot;
import lavi.minecraft.integration.carryon.snapshot.CarryOnScreenSnapshotCollector;
import lavi.minecraft.integration.carryon.snapshot.CarryOnTaskSnapshot;
import lavi.minecraft.integration.carryon.snapshot.CarryOnTaskSnapshotCollector;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.Arrays;

//20260805_kpopmodder: Keep container-task diagnostic field assembly out of upstream container tasks.
final class ContainerTaskDiagnosticFields {
    private static final int MAX_REPEAT_KEY_LENGTH = 280;

    private ContainerTaskDiagnosticFields() {
    }

    static Object[] fields(AltoClef mod,
                           Task task,
                           ItemTarget containerTarget,
                           Block[] containerBlocks,
                           CarryOnObservation carryOn,
                           int suppressedRepeatCount,
                           Object[] branchFields) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client == null ? null : client.player;
        CarryOnTaskSnapshot taskSnapshot = CarryOnTaskSnapshotCollector.collect(mod, ChatClefDiagnostics.taskSummary(task));
        CarryOnBaritoneSnapshot baritoneSnapshot = CarryOnBaritoneSnapshotCollector.collect(mod);
        CarryOnScreenSnapshot screenSnapshot = CarryOnScreenSnapshotCollector.collect(client, player);
        CarryOnPlayerSnapshot playerSnapshot = CarryOnPlayerSnapshotCollector.collect(player);

        Object[] commonFields = new Object[]{
                "diagnosticScope", "container_task",
                "suppressedRepeatCount", suppressedRepeatCount,
                "gameTick", ChatClefDiagnostics.currentClientTickId(),
                "dimension", ChatClefDiagnostics.safeValue(() -> mod == null || mod.getWorld() == null ? null : mod.getWorld().getRegistryKey().getValue()),
                "containerTarget", containerTarget,
                "containerBlocks", Arrays.toString(containerBlocks),
                "topLevelTask", taskSnapshot.topLevelTask(),
                "childTask", taskSnapshot.childTask(),
                "currentChain", taskSnapshot.currentChain(),
                "taskChain", taskSnapshot.taskChain(),
                "taskRunnerActive", taskSnapshot.taskRunnerActive(),
                "userTaskChainActive", taskSnapshot.userTaskChainActive(),
                "paused", taskSnapshot.paused(),
                "chatClefEnabled", taskSnapshot.chatClefEnabled(),
                "playerMode", taskSnapshot.playerMode(),
                "playerPosition", playerSnapshot.playerPosition(),
                "playerVelocity", playerSnapshot.playerVelocity(),
                "lookRotation", playerSnapshot.lookRotation(),
                "playerPoseState", playerSnapshot.playerPoseState(),
                "rightClickState", ChatClefDiagnostics.inputHeldState(Input.CLICK_RIGHT),
                "sneakState", ChatClefDiagnostics.inputHeldState(Input.SNEAK),
                "leftClickState", ChatClefDiagnostics.inputHeldState(Input.CLICK_LEFT),
                "baritonePathing", baritoneSnapshot.baritonePathing(),
                "customGoalOwner", baritoneSnapshot.customGoalOwner(),
                "breakingBlockState", baritoneSnapshot.breakingBlockState(),
                "screenName", screenSnapshot.screenName(),
                "screenHandlerName", screenSnapshot.screenHandlerName(),
                "screenHandlerSyncId", screenSnapshot.screenHandlerSyncId(),
                "cursorStack", screenSnapshot.cursorStack(),
                "selectedHotbarSlot", playerSnapshot.selectedHotbarSlot(),
                "mainHandItem", playerSnapshot.mainHandItem(),
                "offHandItem", playerSnapshot.offHandItem(),
                "carryOnLoaded", loaded(carryOn),
                "carryOnVersion", version(carryOn),
                "carryState", state(carryOn),
                "carriedBlockId", carriedBlockId(carryOn),
                "carriedBlockDescription", carriedBlockDescription(carryOn),
                "carriedBlockState", carriedBlockState(carryOn),
                "carryObservationExceptionType", exceptionType(carryOn),
                "carriedBlockExceptionType", carriedBlockExceptionType(carryOn)
        };
        return mergeFields(commonFields, branchFields);
    }

    static Object[] cap(String scope, int cap) {
        return new Object[]{
                "capScope", scope,
                "cap", cap
        };
    }

    static Object[] repeatSummary(String repeatKey, int suppressedRepeatCount) {
        return new Object[]{
                "repeatKey", repeatKey,
                "suppressedRepeatCount", suppressedRepeatCount
        };
    }

    static String repeatKey(String eventName,
                            String reason,
                            String stateKey,
                            Task task,
                            ItemTarget containerTarget,
                            Block[] containerBlocks,
                            CarryOnObservation carryOn) {
        String key = eventName
                + "|" + reason
                + "|" + ChatClefDiagnostics.taskSummary(task)
                + "|" + containerTarget
                + "|" + Arrays.toString(containerBlocks)
                + "|" + stateKey
                + "|carry=" + state(carryOn)
                + "|carried=" + carriedBlockId(carryOn);
        return normalizeRepeatKey(key);
    }

    private static Object[] mergeFields(Object[] first, Object[] second) {
        if (first == null || first.length == 0) {
            return second == null ? new Object[0] : second;
        }
        if (second == null || second.length == 0) {
            return first;
        }
        Object[] merged = new Object[first.length + second.length];
        System.arraycopy(first, 0, merged, 0, first.length);
        System.arraycopy(second, 0, merged, first.length, second.length);
        return merged;
    }

    private static String normalizeRepeatKey(String repeatKey) {
        if (repeatKey == null) {
            return "none";
        }
        if (repeatKey.length() <= MAX_REPEAT_KEY_LENGTH) {
            return repeatKey;
        }
        return repeatKey.substring(0, MAX_REPEAT_KEY_LENGTH) + "...";
    }

    private static String loaded(CarryOnObservation observation) {
        return observation == null ? "unavailable" : Boolean.toString(observation.loaded());
    }

    private static String version(CarryOnObservation observation) {
        return observation == null ? "unavailable" : observation.version();
    }

    private static String state(CarryOnObservation observation) {
        return observation == null ? "unavailable" : String.valueOf(observation.state());
    }

    private static String exceptionType(CarryOnObservation observation) {
        return observation == null ? "unavailable" : observation.exceptionType();
    }

    private static String carriedBlockId(CarryOnObservation observation) {
        return observation == null ? "unavailable" : observation.carriedBlockId();
    }

    private static String carriedBlockDescription(CarryOnObservation observation) {
        return observation == null ? "unavailable" : observation.carriedBlockDescription();
    }

    private static String carriedBlockState(CarryOnObservation observation) {
        return observation == null ? "unavailable" : observation.carriedBlockState();
    }

    private static String carriedBlockExceptionType(CarryOnObservation observation) {
        return observation == null ? "unavailable" : observation.carriedBlockExceptionType();
    }
}
