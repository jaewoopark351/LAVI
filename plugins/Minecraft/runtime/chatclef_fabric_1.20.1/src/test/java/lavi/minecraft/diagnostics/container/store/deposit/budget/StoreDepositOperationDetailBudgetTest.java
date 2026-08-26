package lavi.minecraft.diagnostics.container.store.deposit.budget;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoreDepositOperationDetailBudgetTest {
    private static final String CANDIDATE_EVENT = "STORE_CONTAINER_PARENT_CANDIDATE_DECISION";
    private static final String INTERACTION_EVENT = "CONTAINER_OPEN_INTERACTION_OUTCOME_WINDOW";

    @Test
    void candidateFamilyCapIsIndependentForEachOperation() {
        StoreDepositOperationDetailBudget budget = new StoreDepositOperationDetailBudget();

        for (int index = 0; index < 44; index++) {
            assertTrue(budget.shouldEmit("operation-a", CANDIDATE_EVENT, "candidate-" + index));
        }
        assertFalse(budget.shouldEmit("operation-a", CANDIDATE_EVENT, "candidate-over-cap"));

        assertTrue(budget.shouldEmit("operation-b", CANDIDATE_EVENT, "candidate-0"));

        Map<String, Object> operationA = fields(budget.summaryFields("operation-a"));
        Map<String, Object> operationB = fields(budget.summaryFields("operation-b"));
        assertEquals(44, operationA.get("storeBudgetOperationDetailEmittedCount"));
        assertEquals(1, operationB.get("storeBudgetOperationDetailEmittedCount"));
        assertTrue(String.valueOf(operationA.get("detailSuppressedCountsByFamily"))
                .contains("CANDIDATE=1"));
    }

    @Test
    void duplicateSemanticStateIsSuppressedAndCounted() {
        StoreDepositOperationDetailBudget budget = new StoreDepositOperationDetailBudget();

        assertTrue(budget.shouldEmit("operation-a", CANDIDATE_EVENT, "same-state"));
        assertFalse(budget.shouldEmit("operation-a", CANDIDATE_EVENT, "same-state"));

        Map<String, Object> summary = fields(budget.summaryFields("operation-a"));
        assertEquals(1, summary.get("storeBudgetOperationDetailEmittedCount"));
        assertTrue(String.valueOf(summary.get("storeBudgetOperationSuppressedCounts"))
                .contains(CANDIDATE_EVENT + "=1"));
        assertTrue(String.valueOf(summary.get("detailSuppressedCountsByFamily"))
                .contains("CANDIDATE=1"));
    }

    @Test
    void interactionFamilyUsesItsOwnFortyFourEventCap() {
        StoreDepositOperationDetailBudget budget = new StoreDepositOperationDetailBudget();

        for (int index = 0; index < 44; index++) {
            assertTrue(budget.shouldEmit("operation-a", INTERACTION_EVENT, "interaction-" + index));
        }
        assertFalse(budget.shouldEmit("operation-a", INTERACTION_EVENT, "interaction-over-cap"));

        Map<String, Object> summary = fields(budget.summaryFields("operation-a"));
        assertTrue(String.valueOf(summary.get("detailSuppressedCountsByFamily"))
                .contains("INTERACTION=1"));
    }

    private static Map<String, Object> fields(Object[] values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index + 1 < values.length; index += 2) {
            result.put(String.valueOf(values[index]), values[index + 1]);
        }
        return result;
    }
}
