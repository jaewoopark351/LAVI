package adris.altoclef.lavibridge;

//20260725_kpopmodder: Added this class to expose read-only Minecraft state for LAVI.

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskChain;
import adris.altoclef.util.helpers.ItemHelper;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LaviStateReader {

    private final AltoClef mod;
    private final LaviActionRegistry actionRegistry;

    public LaviStateReader(AltoClef mod, LaviActionRegistry actionRegistry) {
        this.mod = mod;
        this.actionRegistry = actionRegistry;
    }

    public Map<String, Object> health() {
        Map<String, Object> result = baseResponse();
        result.put("service", "lavi-chatclef-bridge");
        result.put("version", "0.1");
        result.put("control_mode", controlMode());
        return result;
    }

    public Map<String, Object> status() {
        Map<String, Object> result = baseResponse();
        result.put("control_mode", controlMode());
        result.put("player", playerStatus());
        result.put("task", taskStatus());
        result.put("baritone", baritoneStatus());
        result.put("current_action", actionRegistry.currentActionSnapshotOnly());
        return result;
    }

    public Map<String, Object> inventory() {
        Map<String, Object> result = baseResponse();
        result.put("counts", inventoryCounts());
        result.put("slots", inventorySlots());
        return result;
    }

    private Map<String, Object> baseResponse() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", true);
        result.put("initialized", mod != null && mod.getTaskRunner() != null);
        result.put("in_game", AltoClef.inGame());
        return result;
    }

    private Map<String, Object> playerStatus() {
        Map<String, Object> player = new LinkedHashMap<>();
        if (!AltoClef.inGame() || mod.getPlayer() == null || mod.getWorld() == null) {
            player.put("available", false);
            return player;
        }

        ClientPlayerEntity entity = mod.getPlayer();
        BlockPos blockPos = entity.getBlockPos();

        player.put("available", true);
        player.put("name", entity.getName().getString());
        player.put("health", entity.getHealth());
        player.put("food", entity.getHungerManager().getFoodLevel());
        player.put("saturation", entity.getHungerManager().getSaturationLevel());
        player.put("air", entity.getAir());
        player.put("dimension", mod.getWorld().getRegistryKey().getValue().toString());
        player.put("position", Map.of(
                "x", entity.getX(),
                "y", entity.getY(),
                "z", entity.getZ(),
                "block_x", blockPos.getX(),
                "block_y", blockPos.getY(),
                "block_z", blockPos.getZ()
        ));
        player.put("creative", entity.isCreative());
        return player;
    }

    private Map<String, Object> taskStatus() {
        Map<String, Object> task = new LinkedHashMap<>();
        if (mod == null || mod.getTaskRunner() == null || mod.getUserTaskChain() == null) {
            task.put("available", false);
            return task;
        }

        Task currentTask = mod.getUserTaskChain().getCurrentTask();
        TaskChain currentChain = mod.getTaskRunner().getCurrentTaskChain();

        task.put("available", true);
        task.put("task_runner_active", mod.getTaskRunner().isActive());
        task.put("user_task_active", mod.getUserTaskChain().isActive());
        task.put("running_idle_task", mod.getUserTaskChain().isRunningIdleTask());
        task.put("paused", mod.isPaused());
        task.put("status_report", mod.getTaskRunner().statusReport);
        task.put("current_chain", currentChain == null ? null : currentChain.getName());
        task.put("current_task", currentTask == null ? null : currentTask.toString());

        List<String> taskChain = new ArrayList<>();
        for (Task chainTask : mod.getUserTaskChain().getTasks()) {
            taskChain.add(chainTask.toString());
        }
        task.put("user_task_chain", taskChain);
        return task;
    }

    private Map<String, Object> baritoneStatus() {
        Map<String, Object> baritone = new LinkedHashMap<>();
        if (!AltoClef.inGame()) {
            baritone.put("available", false);
            return baritone;
        }

        try {
            baritone.put("available", true);
            baritone.put("pathing", mod.getClientBaritone().getPathingBehavior().isPathing());
            baritone.put("custom_goal_active", mod.getClientBaritone().getCustomGoalProcess().isActive());
            baritone.put("builder_active", mod.getClientBaritone().getBuilderProcess().isActive());
            baritone.put("explore_active", mod.getClientBaritone().getExploreProcess().isActive());
        } catch (RuntimeException exception) {
            baritone.put("available", false);
            baritone.put("error", exception.getMessage());
        }
        return baritone;
    }

    private Map<String, Integer> inventoryCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (!AltoClef.inGame() || mod.getPlayer() == null) {
            return counts;
        }

        for (int slot = 0; slot < mod.getPlayer().getInventory().size(); slot++) {
            ItemStack stack = mod.getPlayer().getInventory().getStack(slot);
            if (!stack.isEmpty()) {
                String itemName = ItemHelper.stripItemName(stack.getItem());
                counts.put(itemName, counts.getOrDefault(itemName, 0) + stack.getCount());
            }
        }
        return counts;
    }

    private List<Map<String, Object>> inventorySlots() {
        List<Map<String, Object>> slots = new ArrayList<>();
        if (!AltoClef.inGame() || mod.getPlayer() == null) {
            return slots;
        }

        for (int slot = 0; slot < mod.getPlayer().getInventory().size(); slot++) {
            ItemStack stack = mod.getPlayer().getInventory().getStack(slot);
            if (!stack.isEmpty()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("slot", slot);
                item.put("item", ItemHelper.stripItemName(stack.getItem()));
                item.put("count", stack.getCount());
                slots.add(item);
            }
        }
        return slots;
    }

    private String controlMode() {
        if (mod == null || mod.isPaused()) {
            return "PAUSED";
        }
        if (actionRegistry.hasRunningAction()) {
            return "AI";
        }
        if (mod.getUserTaskChain() != null
                && mod.getUserTaskChain().isActive()
                && !mod.getUserTaskChain().isRunningIdleTask()) {
            return "AI";
        }
        return "MANUAL";
    }
}
