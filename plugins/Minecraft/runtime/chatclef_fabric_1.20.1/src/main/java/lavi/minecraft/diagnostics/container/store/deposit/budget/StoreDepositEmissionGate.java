package lavi.minecraft.diagnostics.container.store.deposit.budget;

public final class StoreDepositEmissionGate {
    private final StoreDepositOperationDetailBudget detailBudget = new StoreDepositOperationDetailBudget();
    private final StoreDepositCriticalBudget criticalBudget = new StoreDepositCriticalBudget();

    public boolean shouldEmitDetail(String operationId, String eventName, String semanticKey) {
        return detailBudget.shouldEmit(operationId, eventName, semanticKey);
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

    public boolean shouldEmitCoverageSuppressionSummary() {
        return criticalBudget.shouldEmitCoverageSuppressionSummary();
    }

    public StoreDepositTerminalReservation reserveTerminalGroup(String operationId) {
        return criticalBudget.reserveTerminalGroup(operationId);
    }

    public Object[] budgetSummaryFields(String operationId) {
        Object[] operationFields = detailBudget.summaryFields(operationId);
        Object[] sessionFields = new Object[]{
                "storeBudgetTotalSessionCap", StoreDepositBudgetConstants.TOTAL_SESSION_CAP,
                "storeBudgetNoncriticalDetailCap", StoreDepositBudgetConstants.NONCRITICAL_DETAIL_CAP,
                "storeBudgetCriticalReserveCap", StoreDepositBudgetConstants.CRITICAL_RESERVE_CAP,
                "storeBudgetTerminalGroupCount", criticalBudget.terminalGroupCount(),
                "storeBudgetTerminalGroupMax", StoreDepositBudgetConstants.MAX_TERMINAL_GROUPS,
                "storeBudgetExceptionSignatureCount", criticalBudget.exceptionSignatureCount(),
                "storeBudgetExceptionSignatureMax", StoreDepositBudgetConstants.MAX_EXCEPTION_SIGNATURES,
                "storeBudgetLateSummaryCount", criticalBudget.lateSummaryCount(),
                "storeBudgetLateSummaryMax", StoreDepositBudgetConstants.MAX_LATE_SUMMARIES,
                "storeBudgetCoverageSuppressionSummaryCount",
                criticalBudget.coverageSuppressionSummaryCount(),
                "storeBudgetCoverageSuppressionSummaryMax",
                StoreDepositBudgetConstants.MAX_COVERAGE_SUPPRESSION_SUMMARIES,
                "storeBudgetControlEventCount", criticalBudget.controlEventCount(),
                "storeBudgetControlEventMax", StoreDepositBudgetConstants.MAX_CONTROL_EVENTS,
                "storeBudgetTerminalReserveExhaustedOperations", criticalBudget.exhaustedTerminalOperationCount()
        };
        Object[] merged = new Object[operationFields.length + sessionFields.length];
        System.arraycopy(operationFields, 0, merged, 0, operationFields.length);
        System.arraycopy(sessionFields, 0, merged, operationFields.length, sessionFields.length);
        return merged;
    }

    public void purgeOperation(String operationId) {
        detailBudget.purge(operationId);
    }
}
