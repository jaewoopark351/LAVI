package lavi.minecraft.diagnostics.container.store.deposit.budget;

import java.util.LinkedHashMap;

final class StoreDepositOperationDetailBudget {
    private final LinkedHashMap<String, StoreDepositDetailBudget> operations = new LinkedHashMap<>();
    private int sessionEmittedCount;
    private int evictedOperationCount;

    synchronized boolean shouldEmit(String operationId, String eventName, String semanticKey) {
        StoreDepositDetailBudget operation = operationBudget(StoreDepositDetailBudget.normalize(operationId));
        if (sessionEmittedCount >= StoreDepositBudgetConstants.NONCRITICAL_DETAIL_CAP) {
            operation.recordSuppressed(eventName);
            return false;
        }
        if (!operation.shouldEmit(eventName, semanticKey)) {
            return false;
        }
        sessionEmittedCount++;
        return true;
    }

    synchronized Object[] summaryFields(String operationId) {
        StoreDepositDetailBudget operation = operations.get(StoreDepositDetailBudget.normalize(operationId));
        return new Object[]{
                "storeBudgetOperationDetailCap", StoreDepositBudgetConstants.OPERATION_DETAIL_CAP,
                "storeBudgetOperationDetailEmittedCount", operation == null ? 0 : operation.emittedCount(),
                "storeBudgetOperationFamilySizes", operation == null ? "{}" : operation.familySizes(),
                "storeBudgetOperationEventSizes", operation == null ? "{}" : operation.eventSizes(),
                "storeBudgetOperationSuppressedCounts", operation == null ? "{}" : operation.suppressedCounts(),
                "storeBudgetOperationSuppressedFamilyCounts", operation == null ? "{}" : operation.suppressedFamilyCounts(),
                "detailSuppressedCountsByFamily", operation == null ? "{}" : operation.suppressedFamilyCounts(),
                "storeBudgetSessionDetailEmittedCount", sessionEmittedCount,
                "storeBudgetActiveOperationCount", operations.size(),
                "storeBudgetEvictedOperationCount", evictedOperationCount
        };
    }

    synchronized void purge(String operationId) {
        operations.remove(StoreDepositDetailBudget.normalize(operationId));
    }

    private StoreDepositDetailBudget operationBudget(String operationId) {
        StoreDepositDetailBudget existing = operations.get(operationId);
        if (existing != null) {
            return existing;
        }
        if (operations.size() >= StoreDepositBudgetConstants.MAX_ACTIVE_OPERATION_BUDGETS) {
            String oldest = operations.keySet().iterator().next();
            operations.remove(oldest);
            evictedOperationCount++;
        }
        StoreDepositDetailBudget created = new StoreDepositDetailBudget();
        operations.put(operationId, created);
        return created;
    }
}
