package lavi.minecraft.diagnostics.crafting.acquisition.terminal;

import java.util.Objects;

/**
 * Retains one finalized ledger only for exact-key late and admission accounting.
 */
final class CraftResourceTerminalTombstone {
    private static final long RETENTION_TICKS = 200L;
    private static final long RETENTION_NANOS = 10_000_000_000L;

    private final CraftResourceTerminalKey key;
    private final CraftResourceTerminalLedger ledger;
    private final long retiredAtTick;
    private final long retiredAtNanos;
    private final long retirementSequence;

    CraftResourceTerminalTombstone(
            CraftResourceTerminalKey key,
            CraftResourceTerminalLedger ledger,
            long retiredAtTick,
            long retiredAtNanos,
            long retirementSequence) {
        this.key = Objects.requireNonNull(key, "key");
        this.ledger = Objects.requireNonNull(ledger, "ledger");
        this.retiredAtTick = retiredAtTick;
        this.retiredAtNanos = retiredAtNanos;
        this.retirementSequence = retirementSequence;
    }

    CraftResourceTerminalKey key() {
        return key;
    }

    CraftResourceTerminalLedger ledger() {
        return ledger;
    }

    long retirementSequence() {
        return retirementSequence;
    }

    boolean expired(long clientTick, long monotonicNanos) {
        return elapsedAtLeast(clientTick, retiredAtTick, RETENTION_TICKS)
                || elapsedAtLeast(monotonicNanos, retiredAtNanos, RETENTION_NANOS);
    }

    private static boolean elapsedAtLeast(long current, long start, long bound) {
        return current >= start && current - start >= bound;
    }
}
