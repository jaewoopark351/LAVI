package lavi.minecraft.diagnostics.container.store.deposit.budget;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

final class StoreDepositCriticalBudget {
    private final Set<String> terminalOperations = new LinkedHashSet<>();
    private long exhaustedTerminalOperationCount;
    private final Set<String> controlEvents = new HashSet<>();
    private final Set<String> exceptionSignatures = new HashSet<>();
    private final Set<String> lateSummaries = new HashSet<>();
    private boolean coverageSuppressionSummaryEmitted;

    synchronized StoreDepositTerminalReservation reserveTerminalGroup(String operationId) {
        String normalized = StoreDepositDetailBudget.normalize(operationId);
        if (terminalOperations.contains(normalized)) {
            return StoreDepositTerminalReservation.duplicate(normalized);
        }
        if (terminalOperations.size() >= StoreDepositBudgetConstants.MAX_TERMINAL_GROUPS) {
            if (exhaustedTerminalOperationCount < Long.MAX_VALUE) {
                exhaustedTerminalOperationCount++;
            }
            return StoreDepositTerminalReservation.exhausted(normalized);
        }
        terminalOperations.add(normalized);
        return StoreDepositTerminalReservation.reserved(normalized);
    }

    synchronized boolean shouldEmitControl(String operationId, String controlEventName) {
        String key = StoreDepositDetailBudget.normalize(operationId) + "|" + StoreDepositDetailBudget.normalize(controlEventName);
        if (controlEvents.contains(key)) {
            return false;
        }
        if (controlEvents.size() >= StoreDepositBudgetConstants.MAX_CONTROL_EVENTS) {
            return false;
        }
        controlEvents.add(key);
        return true;
    }

    synchronized boolean shouldEmitExceptionSignature(String signature) {
        String normalized = StoreDepositDetailBudget.normalize(signature);
        if (exceptionSignatures.contains(normalized)) {
            return false;
        }
        if (exceptionSignatures.size() >= StoreDepositBudgetConstants.MAX_EXCEPTION_SIGNATURES) {
            return false;
        }
        exceptionSignatures.add(normalized);
        return true;
    }

    synchronized boolean shouldEmitLateSummary(String operationId) {
        String normalized = StoreDepositDetailBudget.normalize(operationId);
        if (lateSummaries.contains(normalized)) {
            return false;
        }
        if (lateSummaries.size() >= StoreDepositBudgetConstants.MAX_LATE_SUMMARIES) {
            return false;
        }
        lateSummaries.add(normalized);
        return true;
    }

    synchronized boolean shouldEmitCoverageSuppressionSummary() {
        if (coverageSuppressionSummaryEmitted) {
            return false;
        }
        coverageSuppressionSummaryEmitted = true;
        return true;
    }

    synchronized int terminalGroupCount() {
        return terminalOperations.size();
    }

    synchronized int controlEventCount() {
        return controlEvents.size();
    }

    synchronized int exceptionSignatureCount() {
        return exceptionSignatures.size();
    }

    synchronized int lateSummaryCount() {
        return lateSummaries.size();
    }

    synchronized int coverageSuppressionSummaryCount() {
        return coverageSuppressionSummaryEmitted ? 1 : 0;
    }

    synchronized long exhaustedTerminalOperationCount() {
        return exhaustedTerminalOperationCount;
    }

    synchronized int clearForModeTransition() {
        int invalidated = terminalOperations.size()
                + controlEvents.size()
                + exceptionSignatures.size()
                + lateSummaries.size()
                + (coverageSuppressionSummaryEmitted ? 1 : 0);
        terminalOperations.clear();
        controlEvents.clear();
        exceptionSignatures.clear();
        lateSummaries.clear();
        coverageSuppressionSummaryEmitted = false;
        return invalidated;
    }
}
