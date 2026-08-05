package lavi.minecraft.diagnostics.toolselect;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.integration.toolselect.snapshot.ToolSavePolicySnapshot;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

//20260805_kpopmodder: Bound tool-save snapshot diagnostics without reading live inventory from worker threads.
public final class ToolSavePolicySnapshotDiagnostics {
    private static final Object LOCK = new Object();
    private static String lastPublishedFingerprint = "";
    private static String lastConsumedFingerprint = "";

    private ToolSavePolicySnapshotDiagnostics() {
    }

    public static void logPublished(ToolSavePolicySnapshot snapshot) {
        if (!ChatClefDiagnostics.isBoundaryEnabled() || snapshot == null) {
            return;
        }
        String fingerprint = "publish|"
                + snapshot.generation()
                + "|" + snapshot.ready()
                + "|" + snapshot.hasDiamondPickaxe()
                + "|" + snapshot.status()
                + "|" + snapshot.worldKey()
                + "|" + snapshot.playerKey();
        synchronized (LOCK) {
            if (fingerprint.equals(lastPublishedFingerprint)) {
                return;
            }
            lastPublishedFingerprint = fingerprint;
        }
        ChatClefDiagnostics.logBoundary(
                "TOOL_SAVE_POLICY_SNAPSHOT_PUBLISHED",
                "tool_save_policy_snapshot_published",
                null,
                ChatClefDiagnostics.withCommandContextFields(snapshotFields(snapshot))
        );
    }

    public static void logConsumed(ToolSavePolicySnapshot snapshot,
                                   Block block,
                                   ItemStack stack,
                                   String decisionReason,
                                   boolean shouldSave) {
        if (!ChatClefDiagnostics.isBoundaryEnabled() || snapshot == null) {
            return;
        }
        String fingerprint = "consume|"
                + snapshot.generation()
                + "|" + snapshot.ready()
                + "|" + snapshot.hasDiamondPickaxe()
                + "|" + snapshot.status()
                + "|" + Thread.currentThread().getName()
                + "|" + decisionReason
                + "|" + shouldSave;
        synchronized (LOCK) {
            if (fingerprint.equals(lastConsumedFingerprint)) {
                return;
            }
            lastConsumedFingerprint = fingerprint;
        }
        ChatClefDiagnostics.logBoundary(
                "TOOL_SAVE_POLICY_SNAPSHOT_CONSUMED",
                "tool_save_policy_snapshot_consumed",
                null,
                ChatClefDiagnostics.withCommandContextFields(
                        merge(snapshotFields(snapshot), new Object[]{
                                "consumerThreadName", Thread.currentThread().getName(),
                                "consumerThreadId", Thread.currentThread().getId(),
                                "decisionReason", decisionReason,
                                "shouldSave", shouldSave,
                                "targetBlockId", blockId(block),
                                "toolItemId", itemId(stack),
                                "toolItemDamage", stack == null ? "unavailable" : stack.getDamage(),
                                "toolItemMaxDamage", stack == null ? "unavailable" : stack.getMaxDamage(),
                                "toolStackEmpty", stack == null ? "unavailable" : stack.isEmpty()
                        })
                )
        );
    }

    private static Object[] snapshotFields(ToolSavePolicySnapshot snapshot) {
        return new Object[]{
                "snapshotGeneration", snapshot.generation(),
                "snapshotPublishedClientTick", snapshot.publishedClientTick(),
                "snapshotReady", snapshot.ready(),
                "snapshotHasDiamondPickaxe", snapshot.hasDiamondPickaxe(),
                "snapshotStatus", snapshot.status(),
                "snapshotWorldKey", snapshot.worldKey(),
                "snapshotPlayerKey", snapshot.playerKey()
        };
    }

    private static Object[] merge(Object[] first, Object[] second) {
        Object[] merged = new Object[first.length + second.length];
        System.arraycopy(first, 0, merged, 0, first.length);
        System.arraycopy(second, 0, merged, first.length, second.length);
        return merged;
    }

    private static String blockId(Block block) {
        try {
            return block == null ? "unavailable" : String.valueOf(Registries.BLOCK.getId(block));
        } catch (RuntimeException | LinkageError error) {
            return "unavailable:" + error.getClass().getSimpleName();
        }
    }

    private static String itemId(ItemStack stack) {
        try {
            return stack == null ? "unavailable" : String.valueOf(Registries.ITEM.getId(stack.getItem()));
        } catch (RuntimeException | LinkageError error) {
            return "unavailable:" + error.getClass().getSimpleName();
        }
    }
}
