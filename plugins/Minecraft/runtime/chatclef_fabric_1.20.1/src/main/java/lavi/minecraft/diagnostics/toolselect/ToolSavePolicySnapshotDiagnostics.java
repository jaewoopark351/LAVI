package lavi.minecraft.diagnostics.toolselect;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.toolselect.lifecycle.ToolSelectionDiagnosticStateObserver;
import lavi.minecraft.diagnostics.toolselect.support.ToolMiningDiagnosticFieldValues;
import lavi.minecraft.integration.toolselect.snapshot.ToolSavePolicySnapshot;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

import java.util.Locale;

//20260805_kpopmodder: Bound tool-save snapshot diagnostics without reading live inventory from worker threads.
public final class ToolSavePolicySnapshotDiagnostics {
    static final String LOCAL_CAP_EVENT =
            "TOOL_SAVE_POLICY_SNAPSHOT_DIAGNOSTIC_CAP_REACHED";

    private static final Object LOCK = new Object();
    private static final ToolSavePolicySnapshotEmissionLimiter CONSUMED_LIMITER =
            new ToolSavePolicySnapshotEmissionLimiter();
    private static String lastPublishedFingerprint = "";
    private static final ToolSelectionDiagnosticStateObserver OFF_STATE_OBSERVER =
            new ToolSelectionDiagnosticStateObserver(
                    ToolSavePolicySnapshotDiagnostics::clearDiagnosticStateForModeOff
            );

    static {
        ChatClefDiagnostics.registerSessionLifecycleObserver(OFF_STATE_OBSERVER);
    }

    private ToolSavePolicySnapshotDiagnostics() {
    }

    public static void logPublished(ToolSavePolicySnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        ChatClefDiagnostics.runIfDiagnosticsEligible(() -> logPublishedEligible(snapshot));
    }

    private static void logPublishedEligible(ToolSavePolicySnapshot snapshot) {
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
        if (snapshot == null) {
            return;
        }
        ChatClefDiagnostics.runIfDiagnosticsEligible(
                () -> logConsumedEligible(snapshot, block, stack, decisionReason, shouldSave)
        );
    }

    private static void logConsumedEligible(ToolSavePolicySnapshot snapshot,
                                            Block block,
                                            ItemStack stack,
                                            String decisionReason,
                                            boolean shouldSave) {
        String repeatKey = consumedRepeatKey(snapshot, block, stack, decisionReason, shouldSave);
        ToolSavePolicySnapshotEmissionLimiter.Decision decision = CONSUMED_LIMITER.evaluate(repeatKey);
        if (decision.emitCap) {
            ChatClefDiagnostics.logBoundary(
                    LOCAL_CAP_EVENT,
                    "tool_save_policy_snapshot_diagnostic_cap_reached",
                    null,
                    ChatClefDiagnostics.withCommandContextFields(new Object[]{
                            "capScope", "tool_save_policy_snapshot_consumed",
                            "cap", ToolSavePolicySnapshotEmissionLimiter.SESSION_HARD_CAP,
                            "maxEmission", maxEmissionDescription(),
                            "behavior_effect", "none"
                    })
            );
            return;
        }
        if (decision.emitSummary) {
            ChatClefDiagnostics.logBoundary(
                    "TOOL_SAVE_POLICY_SNAPSHOT_CONSUMED_REPEAT_SUMMARY",
                    "tool_save_policy_snapshot_consumed_repeat_summary",
                    null,
                    ChatClefDiagnostics.withCommandContextFields(new Object[]{
                            "capScope", "tool_save_policy_snapshot_consumed",
                            "repeatKey", repeatKey,
                            "suppressedRepeatCount", decision.suppressedRepeatCount,
                            "firstObservedTick", decision.firstObservedTick,
                            "lastObservedTick", decision.lastObservedTick,
                            "maxEmission", maxEmissionDescription(),
                            "behavior_effect", "none"
                    })
            );
            return;
        }
        if (!decision.emitEvent) {
            return;
        }
        ChatClefDiagnostics.logBoundary(
                "TOOL_SAVE_POLICY_SNAPSHOT_CONSUMED",
                "tool_save_policy_snapshot_consumed",
                null,
                ChatClefDiagnostics.withCommandContextFields(
                        merge(snapshotFields(snapshot), new Object[]{
                                "dedupeKey", repeatKey,
                                "maxEmission", maxEmissionDescription(),
                                "summary", false,
                                "suppressedRepeatCount", decision.suppressedRepeatCount,
                                "firstObservedTick", decision.firstObservedTick,
                                "lastObservedTick", decision.lastObservedTick,
                                "behavior_effect", "none",
                                "consumerThreadName", Thread.currentThread().getName(),
                                "consumerThreadId", Thread.currentThread().getId(),
                                "decisionReason", decisionReason,
                                "shouldSave", shouldSave,
                                "targetBlockId", blockId(block),
                                "targetMinimumMiningRequirement", ToolMiningDiagnosticFieldValues.minimumMiningRequirement(block),
                                "toolItemId", itemId(stack),
                                "toolItemDamage", stack == null ? "unavailable" : stack.getDamage(),
                                "toolItemMaxDamage", stack == null ? "unavailable" : stack.getMaxDamage(),
                                "toolItemRemainingDurability", ToolMiningDiagnosticFieldValues.remainingDurability(stack),
                                "toolItemDamagePlus8", ToolMiningDiagnosticFieldValues.damagePlus(stack, 8),
                                "toolItemDamagePlus30", ToolMiningDiagnosticFieldValues.damagePlus(stack, 30),
                                "toolCriticalDurabilityThresholdReached", ToolMiningDiagnosticFieldValues.durabilityThresholdReached(stack, 8),
                                "toolLowDurabilityThresholdReached", ToolMiningDiagnosticFieldValues.durabilityThresholdReached(stack, 30),
                                "toolIsIronPickaxe", ToolMiningDiagnosticFieldValues.isIronPickaxe(stack),
                                "lowDurabilityNonIronBlockProtected", shouldSave && "LOW_DURABILITY_BLOCK_NOT_IRON_REQUIRED".equals(decisionReason),
                                "toolStackEmpty", stack == null ? "unavailable" : stack.isEmpty()
                        })
                )
        );
    }

    private static void clearDiagnosticStateForModeOff() {
        synchronized (LOCK) {
            lastPublishedFingerprint = "";
        }
        CONSUMED_LIMITER.clearForModeOff();
    }

    private static String consumedRepeatKey(ToolSavePolicySnapshot snapshot,
                                            Block block,
                                            ItemStack stack,
                                            String decisionReason,
                                            boolean shouldSave) {
        return "consume|"
                + snapshot.generation()
                + "|" + snapshot.ready()
                + "|" + snapshot.hasDiamondPickaxe()
                + "|" + snapshot.status()
                + "|thread=" + threadCategory()
                + "|decision=" + decisionReason
                + "|shouldSave=" + shouldSave
                + "|block=" + blockId(block)
                + "|requirement=" + ToolMiningDiagnosticFieldValues.minimumMiningRequirement(block)
                + "|tool=" + itemId(stack)
                + "|critical=" + ToolMiningDiagnosticFieldValues.durabilityThresholdReached(stack, 8)
                + "|low=" + ToolMiningDiagnosticFieldValues.durabilityThresholdReached(stack, 30)
                + "|iron=" + ToolMiningDiagnosticFieldValues.isIronPickaxe(stack)
                + "|empty=" + (stack == null ? "unavailable" : stack.isEmpty());
    }

    private static String threadCategory() {
        String threadName = Thread.currentThread().getName();
        if ("Render thread".equals(threadName) || "Client thread".equals(threadName)) {
            return "minecraft_client";
        }
        String normalized = threadName == null ? "" : threadName.toLowerCase(Locale.ROOT);
        if (normalized.contains("baritone")) {
            return "baritone_worker";
        }
        if (normalized.contains("worker")) {
            return "worker";
        }
        return normalized.isEmpty() ? "unknown" : "other";
    }

    private static String maxEmissionDescription() {
        return "detail_per_bucket=256,repeat_states=16,session=5000,summary_ticks=200";
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
