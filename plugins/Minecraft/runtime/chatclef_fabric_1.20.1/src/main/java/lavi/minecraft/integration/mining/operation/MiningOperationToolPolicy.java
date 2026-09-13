package lavi.minecraft.integration.mining.operation;

import adris.altoclef.AltoClef;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.Slot;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.mining.gold.GoldMiningToolObservers;
import lavi.minecraft.diagnostics.mining.gold.MiningToolFilterTrace;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.Optional;

public final class MiningOperationToolPolicy {
    private static final int RAW_GOLD_TARGET_DURABILITY_MARGIN = 4;
    private static final int ACCESS_PICKAXE_MIN_REMAINING_DURABILITY = 32;

    private final String operationType;
    private final Item targetItem;
    private final int targetCount;
    private final BlockState targetToolProbeState;
    private final BlockState accessToolProbeState;

    private MiningOperationToolPolicy(String operationType,
                                      Item targetItem,
                                      int targetCount,
                                      BlockState targetToolProbeState,
                                      BlockState accessToolProbeState) {
        this.operationType = operationType;
        this.targetItem = targetItem;
        this.targetCount = targetCount;
        this.targetToolProbeState = targetToolProbeState;
        this.accessToolProbeState = accessToolProbeState;
    }

    public static MiningOperationToolPolicy rawGold(int targetCount) {
        return new MiningOperationToolPolicy(
                "RAW_GOLD",
                Items.RAW_GOLD,
                targetCount,
                Blocks.DEEPSLATE_GOLD_ORE.getDefaultState(),
                Blocks.DEEPSLATE.getDefaultState()
        );
    }

    public MiningOperationToolState evaluate(AltoClef mod) {
        int targetInventoryCount = mod.getItemStorage().getItemCount(targetItem);
        int missingTargetCount = Math.max(1, targetCount - targetInventoryCount);
        int targetDurabilityReserve = missingTargetCount + RAW_GOLD_TARGET_DURABILITY_MARGIN;

        //20260913_kpopmodder: Capture executed filters; never rerun the tool protection policy for diagnostics.
        MiningToolFilterTrace targetTrace = new MiningToolFilterTrace(ChatClefDiagnostics.isBoundaryEnabled());
        MiningToolFilterTrace accessTrace = new MiningToolFilterTrace(ChatClefDiagnostics.isBoundaryEnabled());
        CandidateSelection targetSelection = selectTargetTool(mod, targetDurabilityReserve, targetTrace);
        CandidateSelection accessSelection = selectAccessTool(mod, accessTrace);
        GoldMiningToolObservers.policyEvaluated(mod, targetTrace, accessTrace);

        return new MiningOperationToolState(
                operationType,
                targetCount,
                targetInventoryCount,
                targetDurabilityReserve,
                mod.getItemStorage().getItemCount(Items.IRON_PICKAXE),
                mod.getItemStorage().getItemCount(Items.STONE_PICKAXE),
                targetSelection.selectedCandidate(),
                targetSelection.hasCandidate(),
                targetSelection.hasHotbarCandidate(),
                accessSelection.selectedCandidate(),
                accessSelection.hasCandidate(),
                accessSelection.hasHotbarCandidate()
        );
    }

    public boolean isSameOperation(MiningOperationToolPolicy other) {
        return other != null
                && operationType.equals(other.operationType)
                && targetItem.equals(other.targetItem)
                && targetCount == other.targetCount;
    }

    private CandidateSelection selectTargetTool(AltoClef mod, int targetDurabilityReserve, MiningToolFilterTrace trace) {
        CandidateSelection selection = new CandidateSelection();
        for (Slot slot : Slot.getCurrentScreenSlots()) {
            if (!isPlayerInventorySlot(slot)) {
                trace.nonPlayerSlot();
                continue;
            }
            ItemStack stack = StorageHelper.getItemStackInSlot(slot);
            if (!isTargetTool(mod, stack, targetDurabilityReserve, trace)) {
                continue;
            }
            selection.accept(new MiningToolCandidate(slot, stack, remainingDurability(stack), isHotbarSlot(slot)));
        }
        return selection;
    }

    private CandidateSelection selectAccessTool(AltoClef mod, MiningToolFilterTrace trace) {
        CandidateSelection selection = new CandidateSelection();
        for (Slot slot : Slot.getCurrentScreenSlots()) {
            if (!isPlayerInventorySlot(slot)) {
                trace.nonPlayerSlot();
                continue;
            }
            ItemStack stack = StorageHelper.getItemStackInSlot(slot);
            if (!isAccessTool(mod, stack, trace)) {
                continue;
            }
            selection.accept(new MiningToolCandidate(slot, stack, remainingDurability(stack), isHotbarSlot(slot)));
        }
        return selection;
    }

    private boolean isTargetTool(AltoClef mod, ItemStack stack, int targetDurabilityReserve, MiningToolFilterTrace trace) {
        Item item = stack.getItem();
        if (item != Items.IRON_PICKAXE && item != Items.DIAMOND_PICKAXE && item != Items.NETHERITE_PICKAXE) {
            trace.wrongKind();
            return false;
        }
        if (!item.getDefaultStack().isSuitableFor(targetToolProbeState)) {
            trace.unsuitable();
            return false;
        }
        if (StorageHelper.shouldSaveStack(mod, targetToolProbeState.getBlock(), stack)) {
            trace.savedByPolicy();
            return false;
        }
        boolean durableEnough = remainingDurability(stack) >= targetDurabilityReserve;
        trace.durability(durableEnough);
        return durableEnough;
    }

    private boolean isAccessTool(AltoClef mod, ItemStack stack, MiningToolFilterTrace trace) {
        if (stack.getItem() != Items.STONE_PICKAXE) {
            trace.wrongKind();
            return false;
        }
        if (!stack.getItem().getDefaultStack().isSuitableFor(accessToolProbeState)) {
            trace.unsuitable();
            return false;
        }
        if (StorageHelper.shouldSaveStack(mod, accessToolProbeState.getBlock(), stack)) {
            trace.savedByPolicy();
            return false;
        }
        boolean durableEnough = remainingDurability(stack) >= ACCESS_PICKAXE_MIN_REMAINING_DURABILITY;
        trace.durability(durableEnough);
        return durableEnough;
    }

    private boolean isPlayerInventorySlot(Slot slot) {
        return slot != null && !Slot.isCursor(slot) && slot.isSlotInPlayerInventory();
    }

    private boolean isHotbarSlot(Slot slot) {
        int inventorySlot = slot.getInventorySlot();
        return inventorySlot >= 0 && inventorySlot <= 8;
    }

    private int remainingDurability(ItemStack stack) {
        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, maxDamage - stack.getDamage());
    }

    private static final class CandidateSelection {
        private MiningToolCandidate bestCandidate;
        private MiningToolCandidate hotbarCandidate;

        void accept(MiningToolCandidate candidate) {
            if (candidate.hotbarVisible()
                    && (hotbarCandidate == null
                    || candidate.remainingDurability() > hotbarCandidate.remainingDurability())) {
                hotbarCandidate = candidate;
            }
            if (bestCandidate == null
                    || candidate.remainingDurability() > bestCandidate.remainingDurability()
                    || (candidate.hotbarVisible() && !bestCandidate.hotbarVisible())) {
                bestCandidate = candidate;
            }
        }

        boolean hasCandidate() {
            return bestCandidate != null;
        }

        boolean hasHotbarCandidate() {
            return hotbarCandidate != null;
        }

        Optional<MiningToolCandidate> selectedCandidate() {
            return Optional.ofNullable(hotbarCandidate != null ? hotbarCandidate : bestCandidate);
        }
    }
}
