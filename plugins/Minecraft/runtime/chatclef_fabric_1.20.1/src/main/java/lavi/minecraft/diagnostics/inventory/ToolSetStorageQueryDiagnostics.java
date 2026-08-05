package lavi.minecraft.diagnostics.inventory;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

//20260805_kpopmodder: Observe Baritone ToolSet reads that enter AltoClef live storage from worker threads.
public final class ToolSetStorageQueryDiagnostics {
    private static final InventoryScanEventLimiter LIMITER = new InventoryScanEventLimiter();

    private ToolSetStorageQueryDiagnostics() {
    }

    public static void observe(Block block, ItemStack stack, String redirectPoint) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        try {
            if (isClientThread()) {
                return;
            }
            InventoryCallerSnapshot caller = InventoryCallerSnapshot.capture(true);
            String fingerprint = "BARITONE_TOOLSET_STORAGE_QUERY|"
                    + redirectPoint
                    + "|"
                    + Thread.currentThread().getName()
                    + "|"
                    + caller.callerBoundary();
            InventoryScanEmissionDecision decision = LIMITER.evaluate(
                    fingerprint,
                    ChatClefDiagnostics.currentClientTickId(),
                    false
            );
            if (decision.emitCap()) {
                ChatClefDiagnostics.logBoundary("BARITONE_TOOLSET_STORAGE_QUERY_DIAGNOSTIC_CAP_REACHED",
                        "baritone_toolset_storage_query_diagnostic_cap_reached",
                        null,
                        ChatClefDiagnostics.withCommandContextFields(new Object[]{
                                "capScope", "baritone_toolset_storage_query",
                                "cap", InventoryScanEventLimiter.SESSION_HARD_CAP
                        }));
                return;
            }
            if (decision.emitSummary()) {
                ChatClefDiagnostics.logBoundary("BARITONE_TOOLSET_STORAGE_QUERY_REPEAT_SUMMARY",
                        "baritone_toolset_storage_query_repeat_summary",
                        null,
                        ChatClefDiagnostics.withCommandContextFields(new Object[]{
                                "capScope", "baritone_toolset_storage_query",
                                "repeatKey", fingerprint,
                                "suppressedRepeatCount", decision.suppressedRepeatCount()
                        }));
                return;
            }
            if (!decision.emitEvent()) {
                return;
            }
            ChatClefDiagnostics.logBoundary("BARITONE_TOOLSET_STORAGE_QUERY_OBSERVED",
                    "baritone_toolset_storage_query_observed",
                    null,
                    ChatClefDiagnostics.withCommandContextFields(fields(block, stack, redirectPoint, caller)));
        } catch (RuntimeException | LinkageError ignoredError) {
        }
    }

    private static Object[] fields(Block block,
                                   ItemStack stack,
                                   String redirectPoint,
                                   InventoryCallerSnapshot caller) {
        return new Object[]{
                "redirectPoint", redirectPoint,
                "inventoryQueryRequested", "StorageHelper.shouldSaveStack",
                "queryThreadName", Thread.currentThread().getName(),
                "queryThreadId", Thread.currentThread().getId(),
                "isClientThread", false,
                "callerBoundary", caller.callerBoundary(),
                "callerTopFrames", caller.callerTopFrames(),
                "targetBlockId", blockId(block),
                "toolItemId", itemId(stack),
                "toolItemCount", stack == null ? "unavailable" : stack.getCount(),
                "toolItemDamage", stack == null ? "unavailable" : stack.getDamage(),
                "toolItemMaxDamage", stack == null ? "unavailable" : stack.getMaxDamage(),
                "toolStackEmpty", stack == null ? "unavailable" : stack.isEmpty(),
                "activeInventoryScanCount", InventorySubTrackerScanProbe.activeScanCount()
        };
    }

    private static boolean isClientThread() {
        String threadName = Thread.currentThread().getName();
        return "Render thread".equals(threadName) || "Client thread".equals(threadName);
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
