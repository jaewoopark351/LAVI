package adris.altoclef.tasks.construction.destroy;

import adris.altoclef.AltoClef;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.PlayerSlot;
import adris.altoclef.util.slots.Slot;
import net.minecraft.item.ItemStack;

import java.util.Objects;
import java.util.Optional;

//20260729_kpopmodder: Added this selector to log destroy tool decisions without taking over tool equip behavior.
public final class DestroyToolSelector {
    private DestroyToolSelector() {
    }

    public static ToolDecision select(AltoClef mod, DestroyTargetValidator.TargetDecision targetDecision) {
        Objects.requireNonNull(mod, "mod");
        Objects.requireNonNull(targetDecision, "targetDecision");

        Optional<Slot> currentSlot = Optional.ofNullable(PlayerSlot.getEquipSlot());
        Optional<Slot> bestToolSlot = StorageHelper.getBestToolSlot(mod, targetDecision.state());
        boolean shouldSwitch = currentSlot.isPresent()
                && bestToolSlot.isPresent()
                && !bestToolSlot.get().equals(currentSlot.get());
        String reason;
        if (bestToolSlot.isEmpty()) {
            reason = "no matching better tool found";
        } else if (currentSlot.isEmpty()) {
            reason = "current equip slot unavailable";
        } else if (shouldSwitch) {
            reason = "PlayerInteractionFixChain would be allowed to equip when its guards pass";
        } else {
            reason = "current slot already matches selected tool";
        }

        return new ToolDecision(currentSlot, bestToolSlot, shouldSwitch, reason);
    }

    public static final class ToolDecision {
        private final Optional<Slot> currentSlot;
        private final Optional<Slot> bestToolSlot;
        private final boolean shouldSwitch;
        private final String reason;

        private ToolDecision(Optional<Slot> currentSlot, Optional<Slot> bestToolSlot, boolean shouldSwitch,
                             String reason) {
            this.currentSlot = currentSlot;
            this.bestToolSlot = bestToolSlot;
            this.shouldSwitch = shouldSwitch;
            this.reason = reason;
        }

        String stateKey() {
            return "current=" + describeSlotKey(currentSlot)
                    + ",best=" + describeSlotKey(bestToolSlot)
                    + ",switch=" + shouldSwitch;
        }

        String describe() {
            return "current=" + describeSlot(currentSlot)
                    + ", best=" + describeSlot(bestToolSlot)
                    + ", shouldSwitch=" + shouldSwitch
                    + ", reason=" + reason;
        }

        private String describeSlot(Optional<Slot> slot) {
            return slot.map(value -> value + ", stack=" + describeStack(StorageHelper.getItemStackInSlot(value)))
                    .orElse("none");
        }

        private String describeSlotKey(Optional<Slot> slot) {
            return slot.map(value -> value.getInventorySlot() + "/" + value.getWindowSlot()).orElse("none");
        }

        private String describeStack(ItemStack stack) {
            if (stack == null || stack.isEmpty()) {
                return "empty";
            }
            return stack.getItem().getTranslationKey() + " x " + stack.getCount();
        }
    }
}
