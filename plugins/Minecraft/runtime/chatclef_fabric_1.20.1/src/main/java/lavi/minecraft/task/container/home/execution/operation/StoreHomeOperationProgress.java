package lavi.minecraft.task.container.home.execution.operation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

//20260828_kpopmodder: Own operation-wide failure and confirmed-transfer totals across container sessions.
public final class StoreHomeOperationProgress {
    private final long operationId;
    private final int activatedSessionCount;
    private final int storedItems;
    private final Set<Integer> touchedLogicalSlots;
    private final int latestRemainingStacks;
    private final int capacityFailures;
    private final int unavailableFailures;
    private final Map<HomeStorageTransferIdentity, Confirmation> confirmations;

    private StoreHomeOperationProgress(
            long operationId,
            int activatedSessionCount,
            int storedItems,
            Set<Integer> touchedLogicalSlots,
            int latestRemainingStacks,
            int capacityFailures,
            int unavailableFailures,
            Map<HomeStorageTransferIdentity, Confirmation> confirmations) {
        if (operationId <= 0L) {
            throw new IllegalArgumentException("operationId must be positive");
        }
        this.operationId = operationId;
        this.activatedSessionCount = Math.max(0, activatedSessionCount);
        this.storedItems = Math.max(0, storedItems);
        this.touchedLogicalSlots = Collections.unmodifiableSet(
                new LinkedHashSet<>(touchedLogicalSlots)
        );
        this.latestRemainingStacks = Math.max(0, latestRemainingStacks);
        this.capacityFailures = Math.max(0, capacityFailures);
        this.unavailableFailures = Math.max(0, unavailableFailures);
        this.confirmations = Collections.unmodifiableMap(
                new LinkedHashMap<>(confirmations)
        );
    }

    public static StoreHomeOperationProgress start(long operationId) {
        return new StoreHomeOperationProgress(
                operationId, 0, 0, Set.of(), 0, 0, 0, Map.of()
        );
    }

    public StoreHomeOperationProgress activatedSession(int remainingStacks) {
        return copy(
                saturatingAdd(activatedSessionCount, 1), storedItems,
                touchedLogicalSlots, remainingStacks, capacityFailures,
                unavailableFailures, confirmations
        );
    }

    public StoreHomeOperationProgress withLatestRemainingStacks(int remainingStacks) {
        return copy(
                activatedSessionCount, storedItems,
                touchedLogicalSlots, remainingStacks, capacityFailures,
                unavailableFailures, confirmations
        );
    }

    public StoreHomeOperationProgress confirmed(
            HomeStorageTransferIdentity identity,
            int transferredCount,
            int sourceCountAfter,
            int remainingStacks) {
        if (identity.operationId() != operationId) {
            throw new IllegalArgumentException("transfer belongs to another operation");
        }
        if (transferredCount <= 0 || sourceCountAfter < 0 || remainingStacks < 0) {
            throw new IllegalArgumentException("confirmed transfer values are invalid");
        }
        Confirmation requested = new Confirmation(
                transferredCount, sourceCountAfter, remainingStacks
        );
        Confirmation existing = confirmations.get(identity);
        if (existing != null) {
            if (!existing.equals(requested)) {
                throw new IllegalStateException("conflicting replay for confirmed transfer");
            }
            return this;
        }
        Map<HomeStorageTransferIdentity, Confirmation> updated =
                new LinkedHashMap<>(confirmations);
        updated.put(identity, requested);
        Set<Integer> touched = new LinkedHashSet<>(touchedLogicalSlots);
        touched.add(identity.logicalPlayerSlot());
        return copy(
                activatedSessionCount,
                saturatingAdd(storedItems, transferredCount),
                touched,
                remainingStacks,
                capacityFailures,
                unavailableFailures,
                updated
        );
    }

    public StoreHomeOperationProgress capacityFailure() {
        return copy(
                activatedSessionCount, storedItems,
                touchedLogicalSlots, latestRemainingStacks,
                saturatingAdd(capacityFailures, 1), unavailableFailures, confirmations
        );
    }

    public StoreHomeOperationProgress unavailableFailure() {
        return copy(
                activatedSessionCount, storedItems,
                touchedLogicalSlots, latestRemainingStacks,
                capacityFailures, saturatingAdd(unavailableFailures, 1), confirmations
        );
    }

    public long operationId() {
        return operationId;
    }

    public int nextContainerSessionOrdinal() {
        return saturatingAdd(activatedSessionCount, 1);
    }

    public int storedItems() {
        return storedItems;
    }

    public int touchedStackCount() {
        return touchedLogicalSlots.size();
    }

    public int latestRemainingStacks() {
        return latestRemainingStacks;
    }

    public int capacityFailures() {
        return capacityFailures;
    }

    public int unavailableFailures() {
        return unavailableFailures;
    }

    private StoreHomeOperationProgress copy(
            int newActivatedSessionCount,
            int newStoredItems,
            Set<Integer> newTouchedLogicalSlots,
            int newLatestRemainingStacks,
            int newCapacityFailures,
            int newUnavailableFailures,
            Map<HomeStorageTransferIdentity, Confirmation> newConfirmations) {
        return new StoreHomeOperationProgress(
                operationId,
                newActivatedSessionCount,
                newStoredItems,
                newTouchedLogicalSlots,
                newLatestRemainingStacks,
                newCapacityFailures,
                newUnavailableFailures,
                newConfirmations
        );
    }

    private static int saturatingAdd(int left, int right) {
        long sum = (long) left + right;
        return sum > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sum;
    }

    private record Confirmation(
            int transferredCount,
            int sourceCountAfter,
            int remainingStacks) {
    }
}
