package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.association;

import lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.lifecycle.FabricChatClefCraftResourceDiagnosticLifecycle;
import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceAssociationDiagnosticsRegistry;
import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceAssociationLedgerSnapshot;
import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceAssociationStatus;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionActivation;
import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeKey;

import java.util.Optional;

//20260901_kpopmodder: Own bounded Fabric association accounting outside the pure classifier.
public final class FabricChatClefCraftResourceAssociationScopeDiagnostics {
    private static final CraftResourceAssociationDiagnosticsRegistry REGISTRY =
            new CraftResourceAssociationDiagnosticsRegistry();

    private FabricChatClefCraftResourceAssociationScopeDiagnostics() {
    }

    public static void observeActivation(IronPickaxeAcquisitionActivation activation) {
        //20260913_kpopmodder: Serialize passive capture with owner invalidation.
        FabricChatClefCraftResourceDiagnosticLifecycle.runIfAvailable(() -> observeActivationWhileAvailable(activation));
    }

    private static void observeActivationWhileAvailable(IronPickaxeAcquisitionActivation activation) {
        if (activation != null && activation.binding().isPresent()) {
            REGISTRY.activate(activation.binding().get().key());
        }
    }

    public static void observeClassification(
            IronPickaxeAcquisitionScopeKey key,
            CraftResourceAssociationStatus status
    ) {
        //20260913_kpopmodder: Serialize passive capture with owner invalidation.
        FabricChatClefCraftResourceDiagnosticLifecycle.runIfAvailable(() -> observeClassificationWhileAvailable(key, status));
    }

    private static void observeClassificationWhileAvailable(
            IronPickaxeAcquisitionScopeKey key,
            CraftResourceAssociationStatus status
    ) {
        REGISTRY.observe(key, status);
    }

    public static Optional<CraftResourceAssociationLedgerSnapshot> snapshot(
            IronPickaxeAcquisitionScopeKey key
    ) {
        return REGISTRY.snapshot(key);
    }

    public static void observeObservationGap(
            IronPickaxeAcquisitionScopeKey key,
            String boundary,
            String reason) {
        //20260913_kpopmodder: Serialize passive capture with owner invalidation.
        FabricChatClefCraftResourceDiagnosticLifecycle.runIfAvailable(() -> observeObservationGapWhileAvailable(key, boundary, reason));
    }

    private static void observeObservationGapWhileAvailable(
            IronPickaxeAcquisitionScopeKey key,
            String boundary,
            String reason) {
        REGISTRY.observeObservationGap(key, boundary, reason);
    }

    public static void retire(IronPickaxeAcquisitionScopeKey key) {
        REGISTRY.retire(key);
    }

    public static void clearForModeOff() {
        REGISTRY.clearForModeOff();
    }
}
