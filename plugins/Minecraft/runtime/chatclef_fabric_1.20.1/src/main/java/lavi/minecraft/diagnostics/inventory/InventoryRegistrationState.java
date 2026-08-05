package lavi.minecraft.diagnostics.inventory;

//20260805_kpopmodder: Track only diagnostic pre-add registration ownership for overlap evidence.
public record InventoryRegistrationState(String targetMap,
                                         String itemId,
                                         String listIdentity,
                                         long scanId,
                                         long threadId) {
}
