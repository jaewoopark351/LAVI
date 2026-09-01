package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.target;

import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionActivation;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeKey;
import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceAssociationStatus;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetDiagnosticsRegistry;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetObservation;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetObservationKind;

//20260901_kpopmodder: Own target-ledger activation separately from target event projection.
public final class FabricChatClefCraftResourceTargetScopeDiagnostics {
    private static final CraftResourceTargetDiagnosticsRegistry REGISTRY =
            new CraftResourceTargetDiagnosticsRegistry();

    private FabricChatClefCraftResourceTargetScopeDiagnostics() {
    }

    public static void observeActivation(IronPickaxeAcquisitionActivation activation) {
        if (activation != null && activation.binding().isPresent()) {
            REGISTRY.activateScope(activation.binding().get().key());
        }
    }

    public static CraftResourceTargetDiagnosticsRegistry registry() {
        return REGISTRY;
    }

    public static void closeForCommandTerminal(IronPickaxeAcquisitionScopeKey key) {
        if (key == null) {
            return;
        }
        REGISTRY.observeTarget(
                key,
                new CraftResourceTargetObservation(
                        CraftResourceAssociationStatus.COMMAND_ROOT_DESCENDANT,
                        CraftResourceTargetObservationKind.COMMAND_TERMINAL,
                        REGISTRY.currentTuple(key)
                )
        );
    }

    public static void retire(IronPickaxeAcquisitionScopeKey key) {
        REGISTRY.retireScope(key);
    }

    public static void clearForModeOff() {
        REGISTRY.clearForModeOff();
    }
}
