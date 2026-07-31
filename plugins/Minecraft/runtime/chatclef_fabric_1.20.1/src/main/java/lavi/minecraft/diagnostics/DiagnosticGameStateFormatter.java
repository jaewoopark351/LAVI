package lavi.minecraft.diagnostics;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.Slot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.StringJoiner;

//20260731_kpopmodder: Keep Minecraft object formatting separate from diagnostic event ownership.
final class DiagnosticGameStateFormatter {
    private DiagnosticGameStateFormatter() {
    }

    static String taskSummary(Task task, DiagnosticTaskRegistry tasks) {
        if (task == null) {
            return "none";
        }
        return taskName(task)
                + "#instance=" + tasks.taskInstanceIdLabel(task)
                + "#run=" + tasks.taskRunIdLabel(task);
    }

    static String entitySummary(Entity entity) {
        if (entity == null) {
            return "none";
        }
        try {
            return DiagnosticValueFormatter.className(entity)
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

    static String entityDistanceSqrToPlayer(AltoClef mod, Entity entity) {
        try {
            if (mod == null || mod.getPlayer() == null || entity == null) {
                return "unavailable";
            }
            return Double.toString(entity.squaredDistanceTo(mod.getPlayer()));
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    static String playerPosition(AltoClef mod) {
        try {
            if (mod == null || mod.getPlayer() == null) {
                return "unavailable";
            }
            return vec3d(mod.getPlayer().getPos());
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    static String vec3d(Vec3d pos) {
        if (pos == null) {
            return "unavailable";
        }
        try {
            return pos.getX() + "/" + pos.getY() + "/" + pos.getZ();
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    static String blockPos(BlockPos pos) {
        if (pos == null) {
            return "unavailable";
        }
        try {
            return pos.getX() + "," + pos.getY() + "," + pos.getZ();
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    static String itemStackSummary(ItemStack stack) {
        if (stack == null) {
            return "none";
        }
        try {
            return stack.getCount() + "x" + stack.getItem().getTranslationKey() + "#empty=" + stack.isEmpty();
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    static String slotSummary(Slot slot) {
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

    static String slotStackSummary(Slot slot) {
        if (slot == null) {
            return "none";
        }
        try {
            if (Slot.isCursor(slot)) {
                return safeValue(() -> StorageHelper.getItemStackInSlot(slot));
            }
            int windowSlot = slot.getWindowSlot();
            if (windowSlot < 0) {
                return "not_read#reason=non_window_slot#windowSlot=" + windowSlot;
            }
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.player == null || client.player.currentScreenHandler == null) {
                return "not_read#reason=no_screen_handler#windowSlot=" + windowSlot;
            }
            int slotCount = client.player.currentScreenHandler.slots.size();
            if (windowSlot >= slotCount) {
                return "not_read#reason=window_slot_out_of_range#windowSlot=" + windowSlot + "#slotCount=" + slotCount;
            }
            return safeValue(() -> StorageHelper.getItemStackInSlot(slot));
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    static String itemTargets(ItemTarget[] targets) {
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

    static String classList(Class<?>[] classes) {
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

    private static String safeValue(java.util.function.Supplier<?> supplier) {
        return DiagnosticValueFormatter.safeValue(supplier, true);
    }

    private static String value(Object rawValue) {
        return DiagnosticValueFormatter.value(rawValue);
    }
}
