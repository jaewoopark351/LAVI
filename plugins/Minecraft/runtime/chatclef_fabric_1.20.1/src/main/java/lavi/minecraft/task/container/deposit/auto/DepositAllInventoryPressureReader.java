package lavi.minecraft.task.container.deposit.auto;

import adris.altoclef.AltoClef;
import lavi.minecraft.diagnostics.container.store.deposit.pressure.AutoDepositBoundaryDiagnostics;
import lavi.minecraft.task.container.deposit.auto.pressure.AutoDepositInventoryPressureSource;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;

import java.util.Objects;
import java.util.Optional;

//20260826_kpopmodder: Added client-thread main-inventory occupancy observation for automatic deposit_all.
//20260829_kpopmodder: Expose that unchanged observation through the automatic-only read port.
public final class DepositAllInventoryPressureReader implements AutoDepositInventoryPressureSource {
    @Override
    public Optional<DepositAllInventoryPressureSnapshot> read(AltoClef mod) {
        Objects.requireNonNull(mod, "mod");
        if (mod.getPlayer() == null) {
            //20260913_kpopmodder: Report only the existing empty-read branch and preserve Optional semantics.
            AutoDepositBoundaryDiagnostics.log("AUTO_DEPOSIT_PRESSURE_READ", "player_absent", null,
                    "pressureRead", "UNAVAILABLE", "inventoryRead", "NOT_EVALUATED");
            return Optional.empty();
        }

        PlayerInventory inventory = mod.getPlayer().getInventory();
        int totalSlots = inventory.main.size();
        if (totalSlots == 0) {
            AutoDepositBoundaryDiagnostics.log("AUTO_DEPOSIT_PRESSURE_READ", "main_inventory_empty", null,
                    "pressureRead", "UNAVAILABLE", "totalSlots", totalSlots);
            return Optional.empty();
        }

        int occupiedSlots = 0;
        for (ItemStack stack : inventory.main) {
            if (!stack.isEmpty()) {
                occupiedSlots++;
            }
        }
        return Optional.of(new DepositAllInventoryPressureSnapshot(
                occupiedSlots,
                totalSlots
        ));
    }
}
