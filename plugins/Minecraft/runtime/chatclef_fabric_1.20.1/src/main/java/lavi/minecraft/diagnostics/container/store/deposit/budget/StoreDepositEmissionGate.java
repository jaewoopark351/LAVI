package lavi.minecraft.diagnostics.container.store.deposit.budget;

public final class StoreDepositEmissionGate {
    private final StoreDepositDetailBudget detailBudget = new StoreDepositDetailBudget();
    private final StoreDepositCriticalBudget criticalBudget = new StoreDepositCriticalBudget();

    public boolean shouldEmitDetail(String eventName, String semanticKey) {
        return detailBudget.shouldEmit(eventName, semanticKey);
    }

    public boolean shouldEmitExceptionSignature(String signature) {
        return criticalBudget.shouldEmitExceptionSignature(signature);
    }

    public boolean shouldEmitLateSummary(String operationId) {
        return criticalBudget.shouldEmitLateSummary(operationId);
    }

    public boolean shouldEmitControl(String operationId, String controlEventName) {
        return criticalBudget.shouldEmitControl(operationId, controlEventName);
    }

    public StoreDepositTerminalReservation reserveTerminalGroup(String operationId) {
        return criticalBudget.reserveTerminalGroup(operationId);
    }

    public Object[] budgetSummaryFields() {
        return new Object[]{
                "storeBudgetTotalSessionCap", StoreDepositBudgetConstants.TOTAL_SESSION_CAP,
                "storeBudgetNoncriticalDetailCap", StoreDepositBudgetConstants.NONCRITICAL_DETAIL_CAP,
                "storeBudgetCriticalReserveCap", StoreDepositBudgetConstants.CRITICAL_RESERVE_CAP,
                "storeBudgetDetailEmittedCount", detailBudget.emittedCount(),
                "storeBudgetDetailBucketSizes", detailBudget.bucketSizes(),
                "storeBudgetDetailSuppressedCounts", detailBudget.suppressedCounts(),
                "storeBudgetTerminalGroupCount", criticalBudget.terminalGroupCount(),
                "storeBudgetTerminalGroupMax", StoreDepositBudgetConstants.MAX_TERMINAL_GROUPS,
                "storeBudgetExceptionSignatureCount", criticalBudget.exceptionSignatureCount(),
                "storeBudgetExceptionSignatureMax", StoreDepositBudgetConstants.MAX_EXCEPTION_SIGNATURES,
                "storeBudgetLateSummaryCount", criticalBudget.lateSummaryCount(),
                "storeBudgetLateSummaryMax", StoreDepositBudgetConstants.MAX_LATE_SUMMARIES,
                "storeBudgetControlEventCount", criticalBudget.controlEventCount(),
                "storeBudgetControlEventMax", StoreDepositBudgetConstants.MAX_CONTROL_EVENTS,
                "storeBudgetTerminalReserveExhaustedOperations", criticalBudget.exhaustedTerminalOperationCount()
        };
    }
}
