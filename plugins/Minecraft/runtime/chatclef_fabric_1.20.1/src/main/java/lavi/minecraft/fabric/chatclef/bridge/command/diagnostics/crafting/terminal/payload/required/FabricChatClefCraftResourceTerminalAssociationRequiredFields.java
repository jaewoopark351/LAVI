package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.payload.required;

import lavi.minecraft.diagnostics.crafting.acquisition.association.CraftResourceAssociationLedgerSnapshot;

import java.util.Map;
import java.util.Optional;

//20260901_kpopmodder: Own required command-root association aggregates.
public final class FabricChatClefCraftResourceTerminalAssociationRequiredFields {
    private FabricChatClefCraftResourceTerminalAssociationRequiredFields() {
    }

    public static void append(
            Map<String, Object> fields,
            Optional<CraftResourceAssociationLedgerSnapshot> associationSnapshot) {
        if (associationSnapshot.isEmpty()) {
            fields.put("chainOwnerTransitionCount", "UNAVAILABLE_SCOPE_LEDGER_NOT_ACTIVE");
            fields.put("commandDescendantObservationCount", "UNAVAILABLE_SCOPE_LEDGER_NOT_ACTIVE");
            fields.put("unownedObservationCount", "UNAVAILABLE_SCOPE_LEDGER_NOT_ACTIVE");
            fields.put("unknownAssociationCount", "UNAVAILABLE_SCOPE_LEDGER_NOT_ACTIVE");
            return;
        }
        CraftResourceAssociationLedgerSnapshot association = associationSnapshot.get();
        fields.put("chainOwnerTransitionCount", association.chainOwnerTransitionCount());
        fields.put(
                "commandDescendantObservationCount",
                association.commandDescendantObservationCount()
        );
        fields.put("unownedObservationCount", association.unownedObservationCount());
        fields.put("unknownAssociationCount", association.unknownAssociationCount());
    }
}
