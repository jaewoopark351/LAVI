package lavi.minecraft.task.container.deposit.auto.progress;

import adris.altoclef.AltoClef;
import adris.altoclef.tasks.ResourceTask;
import adris.altoclef.tasks.container.DepositAllTask;
import adris.altoclef.tasks.resources.MineAndCollectTask;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.pathing.goals.GoalTwoBlocks;
import baritone.api.utils.interfaces.IGoalRenderPos;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

//20260915_kpopmodder: Read only selected general-storage prerequisites and their native, matching movement goal.
public final class AutoDepositProgressReader {
    private static final int MAX_TASKS = 64;
    private static final int MAX_ITEM_MATCHES = 64;
    private AutoDepositProgressReader() { }

    /** Called on the client thread, only from a selected maintenance root with a matching plan context. */
    public static AutoDepositProgressSample capture(AltoClef mod, DepositAllTask child) {
        if (mod == null || mod.getWorld() == null || mod.getPlayer() == null
                || child == null || !child.isActive() || child.stopped()) {
            return AutoDepositProgressSample.UNAVAILABLE;
        }
        Optional<BlockPos> container = child.automaticSelectedContainerTarget();
        Optional<AutoDepositProgressSample.Navigation> navigation = Optional.empty();
        List<AutoDepositProgressSample.Resource> resources = new ArrayList<>();
        var process = mod.getClientBaritone().getCustomGoalProcess();
        Goal goal = process.isActive() ? process.getGoal() : null;
        BlockPos nativeTarget = fixedGoalPosition(goal);
        if (container.isPresent()) {
            navigation = navigation(mod, container.orElseThrow(), nativeTarget);
        } else {
            // Task's existing traversal visits the retained active child chain; it creates/runs no child.
            List<BlockPos> miningTargets = new ArrayList<>(1);
            int[] visited = {0};
            child.thisOrChildSatisfies(task -> {
                if (++visited[0] > MAX_TASKS) return true;
                if (!task.isActive() || task.stopped()) return true;
                if (task instanceof MineAndCollectTask.MineOrCollectTask mine
                        && mine.miningPos() != null && matches(mine.miningPos(), nativeTarget)) {
                    miningTargets.clear();
                    miningTargets.add(mine.miningPos().toImmutable());
                }
                if (task instanceof ResourceTask resource) appendResources(mod, resource, resources);
                return false;
            });
            if (!miningTargets.isEmpty()) navigation = navigation(mod, miningTargets.get(0), nativeTarget);
        }
        return new AutoDepositProgressSample(container.isPresent()
                ? AutoDepositProgressSample.Phase.APPROACH_CONTAINER
                : AutoDepositProgressSample.Phase.PREPARE_CONTAINER, navigation, resources);
    }

    private static BlockPos fixedGoalPosition(Goal goal) {
        // Exclude composites, moving/escape/exploration/custom goals whose center is not a fixed objective.
        if (goal == null || (goal.getClass() != GoalBlock.class && goal.getClass() != GoalNear.class
                && goal.getClass() != GoalTwoBlocks.class)) return null;
        return ((IGoalRenderPos) goal).getGoalPos();
    }

    private static boolean matches(BlockPos owner, BlockPos nativeTarget) {
        // Native block interaction uses target.up(); normalize to the owner's position for stable keys.
        return owner != null && nativeTarget != null && owner.getX() == nativeTarget.getX()
                && owner.getZ() == nativeTarget.getZ()
                && (nativeTarget.getY() == owner.getY() || (long) nativeTarget.getY() == (long) owner.getY() + 1);
    }

    private static Optional<AutoDepositProgressSample.Navigation> navigation(
            AltoClef mod, BlockPos owner, BlockPos nativeTarget) {
        if (!matches(owner, nativeTarget)) return Optional.empty();
        var player = mod.getPlayer().getPos();
        double distance = Math.hypot(Math.hypot(player.x - (owner.getX() + 0.5),
                player.y - (owner.getY() + 0.5)), player.z - (owner.getZ() + 0.5));
        if (!Double.isFinite(distance)) return Optional.empty();
        return Optional.of(new AutoDepositProgressSample.Navigation(
                new AutoDepositProgressSample.Target(owner.getX(), owner.getY(), owner.getZ()), distance));
    }

    private static void appendResources(AltoClef mod, ResourceTask task,
                                        List<AutoDepositProgressSample.Resource> result) {
        ItemTarget[] outputs = task.getItemTargets();
        if (outputs == null) return;
        int inspected = 0;
        for (ItemTarget output : outputs) {
            if (++inspected > AutoDepositProgressSample.MAX_RESOURCE_SAMPLES
                    || result.size() >= AutoDepositProgressSample.MAX_RESOURCE_SAMPLES) return;
            if (output == null || output.getTargetCount() <= 0) continue;
            Item[] matches = output.getMatches();
            if (matches == null || matches.length == 0 || matches.length > MAX_ITEM_MATCHES) continue;
            TreeSet<String> ids = new TreeSet<>();
            boolean valid = true;
            for (Item item : matches) {
                if (item == null) { valid = false; break; }
                ids.add(Registries.ITEM.getId(item).toString());
            }
            if (!valid) continue;
            String key = String.join(",", ids);
            if (key.length() > AutoDepositProgressSample.MAX_RESOURCE_KEY_LENGTH) continue;
            long held = 0;
            for (ItemStack stack : mod.getPlayer().getInventory().main) {
                if (!stack.isEmpty() && output.matches(stack.getItem())) held += stack.getCount();
            }
            result.add(new AutoDepositProgressSample.Resource(key,
                    (int) Math.min(Integer.MAX_VALUE, held), output.getTargetCount()));
        }
    }
}
