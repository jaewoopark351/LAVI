package lavi.minecraft.diagnostics.container.store.deposit;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260829_kpopmodder: Characterize pursuit, target-callback, and route-event facade wiring before extraction.
class StoreDepositDiagnosticsRouteContractTest {
    @AfterEach
    void disableDiagnostics() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @Test
    void keepsAllRouteObserversOnOneOperationAndRecordsBeforeDeduplication() {
        ChatClefDiagnostics.setBoundaryEnabled(true);
        Task root = new TestTask();
        BlockPos target = new BlockPos(-533, 50, 126);
        Object[] operationFields = StoreDepositDiagnostics.registerBareDepositInvocation(
                null,
                false,
                new ItemTarget[0],
                root,
                "BARE_DEPOSIT_ALL_COMMAND"
        );
        String operationId = String.valueOf(field(operationFields, "storeOperationId"));

        String output = captureOutput(() -> {
            StoreDepositDiagnostics.onStoreRootStart(root, false, new ItemTarget[0]);
            StoreDepositDiagnostics.logPursuitDecision(
                    root, null, target, "PURSUIT_VALIDATION_ACCEPTED"
            );
            StoreDepositDiagnostics.logPursuitDecision(
                    root, null, target, "PURSUIT_VALIDATION_ACCEPTED"
            );
            StoreDepositDiagnostics.logTargetCallbackDecision(
                    root, target, null, false, true, new ItemTarget[0]
            );
            StoreDepositDiagnostics.logTargetCallbackDecision(
                    root, target, null, true, true, new ItemTarget[0]
            );
            StoreDepositDiagnostics.observeContainerRouteEvent(
                    "CONTAINER_TASK_BRANCH_OBSERVED",
                    "OPEN_EXISTING",
                    root,
                    new Object[0]
            );
            Object[] branchFields = new Object[]{
                    "decision", "OPEN_EXISTING",
                    "costToWalk", 12.5,
                    "costToMakeNew", 20.0,
                    "nearestPresent", true,
                    "nearestPosition", target.toShortString(),
                    "cachedContainerPosition", "none",
                    "openTableTask", false
            };
            StoreDepositDiagnostics.observeContainerRouteEvent(
                    "CONTAINER_TASK_TARGET_DECISION",
                    "OPEN_EXISTING",
                    root,
                    branchFields
            );
            StoreDepositDiagnostics.observeContainerRouteEvent(
                    "CONTAINER_TASK_TARGET_DECISION",
                    "OPEN_EXISTING",
                    root,
                    branchFields
            );
            StoreDepositDiagnostics.logNaturalFinish(root);
        });

        assertEquals(1, occurrences(output, "event=STORE_CONTAINER_PURSUIT_DECISION"));
        assertEquals(1, occurrences(output, "event=STORE_CONTAINER_TARGET_CALLBACK_DECISION"));
        assertEquals(1, occurrences(output, "event=STORE_CRAFT_ROUTE_EVALUATION_ENTERED"));
        assertTrue(eventLine(output, "STORE_CONTAINER_PURSUIT_DECISION").contains("storeOperationId=" + operationId));
        assertTrue(eventLine(output, "STORE_CONTAINER_TARGET_CALLBACK_DECISION").contains("storeOperationId=" + operationId));
        assertTrue(eventLine(output, "STORE_CRAFT_ROUTE_EVALUATION_ENTERED").contains("storeOperationId=" + operationId));
        assertTrue(output.contains("pursuitDecisionCount=2"));
        assertTrue(output.contains("targetCallbackDecisionCount=2"));
        assertTrue(output.contains("craftRouteEventCount=3"));
        assertTrue(output.contains("returnedAction=PURSUIT_VALIDATION_ACCEPTED"));
        assertTrue(output.contains("progressResetBecauseReferenceChanged=true"));
        assertTrue(output.contains("decision=OPEN_EXISTING"));
        assertTrue(output.contains("costToWalk=12.5"));
    }

    @Test
    void preservesParentThenFilteredSearchEventOrderAndOrderedPayloadSegments() {
        ChatClefDiagnostics.setBoundaryEnabled(true);
        Task root = new TestTask();
        BlockPos raw = new BlockPos(-679, 59, 105);
        BlockPos filtered = new BlockPos(-533, 50, 126);
        Object[] operationFields = StoreDepositDiagnostics.registerBareDepositInvocation(
                null,
                false,
                new ItemTarget[0],
                root,
                "BARE_DEPOSIT_ALL_COMMAND"
        );
        String operationId = String.valueOf(field(operationFields, "storeOperationId"));

        String output = captureOutput(() -> {
            StoreDepositDiagnostics.beginDepositAllParentFilteredSearchObservation(root, raw);
            StoreDepositDiagnostics.observeDepositAllContainerEligibility(
                    raw,
                    "CACHED_DUNGEON_CHEST"
            );
            StoreDepositDiagnostics.observeDepositAllContainerEligibility(filtered, "ACCEPTED");
            StoreDepositDiagnostics.endDepositAllParentFilteredSearchObservation(root, true);
            StoreDepositDiagnostics.logDepositAllParentCandidateDecision(
                    root,
                    "OPEN_EXISTING",
                    true,
                    raw,
                    true,
                    true,
                    true,
                    false,
                    null,
                    new ItemTarget[0],
                    "candidateDecisionOutcome", "RAW_CLOSEST_WITHIN_50",
                    "fallbackContainerItemPresent", true
            );
            StoreDepositDiagnostics.logFilteredSearchResult(
                    root,
                    Optional.of(filtered),
                    new net.minecraft.block.Block[0]
            );
            StoreDepositDiagnostics.logNaturalFinish(root);
        });

        assertEquals(1, occurrences(output, "event=STORE_CONTAINER_PARENT_CANDIDATE_DECISION"));
        assertEquals(1, occurrences(output, "event=STORE_CONTAINER_FILTERED_SEARCH_RESULT"));
        String parent = eventLine(output, "STORE_CONTAINER_PARENT_CANDIDATE_DECISION");
        String filteredSearch = eventLine(output, "STORE_CONTAINER_FILTERED_SEARCH_RESULT");
        assertInOrder(
                parent,
                "storeOperationId=" + operationId,
                "storeContextAvailable=true",
                "requestSource=BARE_DEPOSIT_ALL_COMMAND",
                "diagnosticScope=store_deposit_parent_candidate",
                "owner=store_container_candidate_observer",
                "candidateDecisionSequence=1",
                "branchEpoch=1",
                "selectedBranch=OPEN_EXISTING",
                "rawClosestContainerPosition=" + ChatClefDiagnostics.blockPos(raw),
                "rangeDecisionOutcome=RAW_CLOSEST_WITHIN_50",
                "candidateDecisionOutcome=RAW_CLOSEST_WITHIN_50",
                "fallbackContainerItemPresent=true"
        );
        assertInOrder(
                filteredSearch,
                "storeOperationId=" + operationId,
                "storeContextAvailable=true",
                "requestSource=BARE_DEPOSIT_ALL_COMMAND",
                "diagnosticScope=store_deposit_filtered_search",
                "owner=store_container_candidate_observer",
                "filteredSearchId=" + operationId + "-filtered-1",
                "parentDecisionSequenceAtScan=1",
                "originatingBranchEpoch=1",
                "originatingParentRawClosestPosition=" + ChatClefDiagnostics.blockPos(raw),
                "filteredResultPosition=" + ChatClefDiagnostics.blockPos(filtered),
                "rawAndFilteredRelation=DIFFERENT_POSITION",
                "candidateEvaluationCount=2",
                "predicateAcceptedCount=1",
                "predicateRejectedCount=1",
                "rawCandidatePredicateOutcome=RAW_REJECTED",
                "scannerCallCompletedNormally=true"
        );
        assertTrue(output.indexOf("event=STORE_CONTAINER_PARENT_CANDIDATE_DECISION")
                < output.indexOf("event=STORE_CONTAINER_FILTERED_SEARCH_RESULT"));
        assertTrue(output.indexOf("event=STORE_CONTAINER_FILTERED_SEARCH_RESULT")
                < output.indexOf("event=STORE_DEPOSIT_TERMINAL_SUMMARY"));
    }

    @Test
    void offTransitionSuppressesParentAndFilteredSearchFacadeOutput() {
        ChatClefDiagnostics.setBoundaryEnabled(true);
        Task root = new TestTask();
        StoreDepositDiagnostics.registerBareDepositInvocation(
                null,
                false,
                new ItemTarget[0],
                root,
                "BARE_DEPOSIT_ALL_COMMAND"
        );
        ChatClefDiagnostics.setBoundaryEnabled(false);

        String output = captureOutput(() -> {
            StoreDepositDiagnostics.logDepositAllParentCandidateDecision(
                    root,
                    "OBTAIN_CHEST",
                    false,
                    null,
                    false,
                    false,
                    false,
                    false,
                    null,
                    new ItemTarget[0]
            );
            StoreDepositDiagnostics.logFilteredSearchResult(
                    root,
                    Optional.empty(),
                    new net.minecraft.block.Block[0]
            );
        });

        assertEquals("", output.trim());
    }

    private static Object field(Object[] fields, String key) {
        for (int index = 0; index + 1 < fields.length; index += 2) {
            if (key.equals(fields[index])) {
                return fields[index + 1];
            }
        }
        throw new AssertionError("Missing field: " + key);
    }

    private static String eventLine(String output, String eventName) {
        for (String line : output.split("\\R")) {
            if (line.contains("event=" + eventName)) {
                return line;
            }
        }
        throw new AssertionError("Missing event: " + eventName);
    }

    private static String captureOutput(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
        try {
            action.run();
        } finally {
            System.setOut(original);
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    private static int occurrences(String text, String token) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }

    private static void assertInOrder(String text, String... tokens) {
        int offset = -1;
        for (String token : tokens) {
            int next = text.indexOf(token, offset + 1);
            assertTrue(next > offset, "Missing or out-of-order token: " + token + " in " + text);
            offset = next;
        }
    }

    private static final class TestTask extends Task {
        @Override
        protected void onStart() {
        }

        @Override
        protected Task onTick() {
            return null;
        }

        @Override
        protected void onStop(Task interruptTask) {
        }

        @Override
        protected boolean isEqual(Task other) {
            return this == other;
        }

        @Override
        protected String toDebugString() {
            return "store-deposit-route-contract-test";
        }
    }
}
