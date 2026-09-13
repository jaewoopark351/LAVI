package lavi.minecraft.diagnostics.mining.gold;

import adris.altoclef.AltoClef;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.integration.mining.operation.MiningToolCandidate;
import lavi.minecraft.integration.mining.operation.MiningToolRole;
import net.minecraft.item.ItemStack;

//20260913_kpopmodder: Capture one void forceEquipSlot invocation separately from actual slot reflection.
public final class GoldHotbarMoveObservation {
    private final GoldToolLoopObserver observer;
    private final long attempt;
    private final int source;
    private final int destination;
    private final int selectedBefore;
    private final ItemStack expected;
    private final String destinationBefore;

    public GoldHotbarMoveObservation(GoldToolLoopObserver observer, AltoClef mod,
                                     MiningToolCandidate candidate, MiningToolRole role, int destination) {
        this.observer = observer;
        this.attempt = ChatClefDiagnostics.nextOperationId();
        this.source = candidate.slot().getInventorySlot();
        this.destination = destination;
        this.selectedBefore = mod.getPlayer().getInventory().selectedSlot;
        this.expected = candidate.stack().copy();
        this.destinationBefore = GoldToolSnapshot.stack(mod.getPlayer().getInventory().getStack(destination));
        observer.hotbar("VOID_CALL_REQUEST", "moveAttemptId", attempt, "role", role,
                "sourceInventoryIndex", source, "sourceWindowIndex", candidate.slot().getWindowSlot(),
                "destinationHotbarIndex", destination, "destinationBefore", destinationBefore,
                "sourceExpected", GoldToolSnapshot.stack(expected), "selectedBefore", selectedBefore,
                "slotCoordinates", "PLAYER_INVENTORY_INDEX_HOTBAR_0_TO_8");
    }

    public void returned(AltoClef mod, boolean normalReturn) {
        ItemStack destinationAfter = mod.getPlayer().getInventory().getStack(destination);
        observer.hotbar(normalReturn ? "VOID_CALL_RETURNED" : "VOID_CALL_DID_NOT_RETURN_NORMALLY",
                "moveAttemptId", attempt, "methodReturnType", "VOID", "reportedSuccess", "NOT_APPLICABLE",
                "normalReturn", normalReturn, "sourceInventoryIndex", source,
                "destinationHotbarIndex", destination, "selectedBefore", selectedBefore,
                "selectedAfter", mod.getPlayer().getInventory().selectedSlot,
                "destinationBefore", destinationBefore, "destinationAfter", GoldToolSnapshot.stack(destinationAfter),
                "sourceAfter", GoldToolSnapshot.stack(mod.getPlayer().getInventory().getStack(source)),
                "sameItemType", destinationAfter.getItem() == expected.getItem(),
                "sameStackValue", ItemStack.areEqual(destinationAfter, expected),
                "exactStackIdentity", "UNPROVEN_EQUAL_VALUES_DO_NOT_IDENTIFY_STACK",
                "slotReflectionTiming", "IMMEDIATE_LOCAL_SNAPSHOT_NOT_SERVER_ACK");
    }
}
