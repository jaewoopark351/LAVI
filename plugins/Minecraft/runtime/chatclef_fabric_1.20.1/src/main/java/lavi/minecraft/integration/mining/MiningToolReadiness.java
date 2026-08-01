package lavi.minecraft.integration.mining;

import adris.altoclef.AltoClef;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.Slot;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;

//20260801_kpopmodder: Keep mining readiness target-aware without weakening AltoClef's tool preservation policy.
public final class MiningToolReadiness {

    private MiningToolReadiness() {
    }

    public static Readiness evaluate(AltoClef mod, BlockState targetState, MiningRequirement requirement) {
        boolean broadRequirementMet = StorageHelper.miningRequirementMet(requirement);
        boolean selectableToolPresent = hasSelectableMiningTool(mod, targetState);
        boolean rejectedBySavePolicy = !selectableToolPresent
                && hasSuitableToolRejectedBySavePolicy(mod, targetState);

        return new Readiness(
                requirement,
                broadRequirementMet,
                selectableToolPresent,
                rejectedBySavePolicy,
                requirement != MiningRequirement.HAND
                        && broadRequirementMet
                        && !selectableToolPresent
                        && rejectedBySavePolicy
        );
    }

    public static boolean hasSelectableMiningTool(AltoClef mod, BlockState targetState) {
        return StorageHelper.getBestToolSlot(mod, targetState).isPresent();
    }

    public static boolean hasSuitableToolRejectedBySavePolicy(AltoClef mod, BlockState targetState) {
        for (Slot slot : Slot.getCurrentScreenSlots()) {
            if (!slot.isSlotInPlayerInventory()) {
                continue;
            }

            ItemStack stack = StorageHelper.getItemStackInSlot(slot);
            if (!(stack.getItem() instanceof ToolItem)) {
                continue;
            }

            if (!stack.getItem().getDefaultStack().isSuitableFor(targetState)) {
                continue;
            }

            if (StorageHelper.shouldSaveStack(mod, targetState.getBlock(), stack)) {
                return true;
            }
        }
        return false;
    }

    public static final class Readiness {
        private final MiningRequirement requirement;
        private final boolean broadRequirementMet;
        private final boolean selectableToolPresent;
        private final boolean rejectedBySavePolicy;
        private final boolean requiresAcquisition;

        private Readiness(
                MiningRequirement requirement,
                boolean broadRequirementMet,
                boolean selectableToolPresent,
                boolean rejectedBySavePolicy,
                boolean requiresAcquisition
        ) {
            this.requirement = requirement;
            this.broadRequirementMet = broadRequirementMet;
            this.selectableToolPresent = selectableToolPresent;
            this.rejectedBySavePolicy = rejectedBySavePolicy;
            this.requiresAcquisition = requiresAcquisition;
        }

        public MiningRequirement requirement() {
            return requirement;
        }

        public boolean broadRequirementMet() {
            return broadRequirementMet;
        }

        public boolean selectableToolPresent() {
            return selectableToolPresent;
        }

        public boolean rejectedBySavePolicy() {
            return rejectedBySavePolicy;
        }

        public boolean requiresAcquisition() {
            return requiresAcquisition;
        }
    }
}
