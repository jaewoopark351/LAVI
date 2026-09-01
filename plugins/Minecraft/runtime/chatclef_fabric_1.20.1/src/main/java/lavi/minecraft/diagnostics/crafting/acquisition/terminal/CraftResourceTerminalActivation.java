package lavi.minecraft.diagnostics.crafting.acquisition.terminal;

import java.util.Objects;

/**
 * Immutable activation observation; it does not start or change a command.
 */
public record CraftResourceTerminalActivation(
        CraftResourceTerminalActivationStatus status,
        CraftResourceTerminalKey key,
        String reason,
        int activeLedgerCount,
        int tombstoneCount,
        long activeCapacityRefusalCount,
        boolean counterSaturated) {

    public CraftResourceTerminalActivation {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(reason, "reason");
    }
}
