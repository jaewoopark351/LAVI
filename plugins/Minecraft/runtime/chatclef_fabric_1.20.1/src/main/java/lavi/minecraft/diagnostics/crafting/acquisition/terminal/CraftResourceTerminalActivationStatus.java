package lavi.minecraft.diagnostics.crafting.acquisition.terminal;

/**
 * Bounded-registry result for one authoritative terminal-ledger activation.
 */
public enum CraftResourceTerminalActivationStatus {
    ACTIVATED,
    ALREADY_ACTIVE,
    FINALIZED_TOMBSTONE_PRESENT,
    ACTIVE_CAPACITY_REACHED
}
