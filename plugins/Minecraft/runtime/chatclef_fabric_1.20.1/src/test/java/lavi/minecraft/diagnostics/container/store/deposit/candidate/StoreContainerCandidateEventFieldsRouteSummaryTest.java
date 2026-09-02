package lavi.minecraft.diagnostics.container.store.deposit.candidate;

import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationContext;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

//20260902_kpopmodder: Lock final route-summary field ordering and values before atomic snapshot extraction.
class StoreContainerCandidateEventFieldsRouteSummaryTest {
    @Test
    void preservesFinalRouteSummaryFieldOrderAndValues() {
        StoreDepositOperationState state = state();
        StoreContainerRouteState route = state.routeState();
        BlockPos raw = new BlockPos(-679, 59, 105);
        BlockPos filtered = new BlockPos(-533, 50, 126);
        Object routeChild = new Object();

        StoreContainerParentDecision parent = route.recordParentDecision(
                "OPEN_EXISTING",
                true,
                raw,
                true,
                true,
                false,
                false,
                null,
                "RAW_CLOSEST_WITHIN_50",
                "targets-a"
        );
        route.recordFilteredSearch(Optional.of(filtered), observation(parent, raw));
        route.recordPursuit(filtered, "OPEN_CHEST");
        route.recordChildReconciliation("ROOT_ROUTE", routeChild, true);
        route.recordTransferDecision();
        route.checkpoint(1200).orElseThrow();

        Object[] fields = StoreContainerCandidateEventFields.routeSummaryFields(state);

        assertEquals(List.of(
                "routeSummaryAvailable",
                "finalCandidateDecisionSequence",
                "finalBranchEpoch",
                "finalBranch",
                "finalRawCandidate",
                "finalFilteredCandidate",
                "finalPursuit",
                "branchCounts",
                "branchTransitionCounts",
                "childReplacementCount",
                "rootRouteChildReplacementCount",
                "childLifecycleSequence",
                "finalRouteChildIdentity",
                "finalRouteChildClass",
                "routeTransferDecisionCount",
                "candidateEvaluationCount",
                "predicateAcceptedCount",
                "predicateRejectedCount",
                "predicateRejectionCounts",
                "checkpointCount",
                "lastSuccessfulBoundary",
                "firstExplicitFailureBoundary",
                "firstUnobservedBoundary",
                "totalRaw50CrossingCount",
                "totalCurrentTry70CrossingCount",
                "totalBranchChangeAtRangeCrossingCount",
                "totalResourceChildInterruptedAtRangeCrossingCount",
                "totalFirstRangeCrossingSample",
                "totalLastRangeCrossingSample"
        ), keys(fields));
        assertEquals(true, field(fields, "routeSummaryAvailable"));
        assertEquals(1L, field(fields, "finalCandidateDecisionSequence"));
        assertEquals(1L, field(fields, "finalBranchEpoch"));
        assertEquals("OPEN_EXISTING", field(fields, "finalBranch"));
        assertEquals("-679,59,105", field(fields, "finalRawCandidate"));
        assertEquals("-533,50,126", field(fields, "finalFilteredCandidate"));
        assertEquals("-533,50,126", field(fields, "finalPursuit"));
        assertEquals("{OPEN_EXISTING=1}", field(fields, "branchCounts"));
        assertEquals("{NONE->OPEN_EXISTING=1}", field(fields, "branchTransitionCounts"));
        assertEquals(1, field(fields, "childReplacementCount"));
        assertEquals(1, field(fields, "rootRouteChildReplacementCount"));
        assertEquals(1L, field(fields, "childLifecycleSequence"));
        assertEquals(Integer.toHexString(System.identityHashCode(routeChild)),
                field(fields, "finalRouteChildIdentity"));
        assertEquals(Object.class.getName(), field(fields, "finalRouteChildClass"));
        assertEquals(1, field(fields, "routeTransferDecisionCount"));
        assertEquals(3, field(fields, "candidateEvaluationCount"));
        assertEquals(1, field(fields, "predicateAcceptedCount"));
        assertEquals(2, field(fields, "predicateRejectedCount"));
        assertEquals(
                "{CHEST_ABOVE_BLOCKED_UNBREAKABLE=1, CONTAINER_CACHE_FULL=1}",
                field(fields, "predicateRejectionCounts")
        );
        assertEquals(1, field(fields, "checkpointCount"));
        assertEquals("TRANSFER_DECISION", field(fields, "lastSuccessfulBoundary"));
        assertEquals("NONE", field(fields, "firstExplicitFailureBoundary"));
        assertEquals("BLOCK_SCANNER_INTERNAL_FILTER_UNOBSERVED",
                field(fields, "firstUnobservedBoundary"));
        assertEquals(0, field(fields, "totalRaw50CrossingCount"));
        assertEquals(0, field(fields, "totalCurrentTry70CrossingCount"));
        assertEquals(0, field(fields, "totalBranchChangeAtRangeCrossingCount"));
        assertEquals(0, field(fields, "totalResourceChildInterruptedAtRangeCrossingCount"));
        assertEquals("none", field(fields, "totalFirstRangeCrossingSample"));
        assertEquals("none", field(fields, "totalLastRangeCrossingSample"));
    }

    private static StoreDepositOperationState state() {
        return new StoreDepositOperationState(new StoreDepositOperationContext(
                "operation-route-summary",
                "BARE_DEPOSIT_ALL_COMMAND",
                null,
                0L,
                0L
        ));
    }

    private static StoreContainerCandidateObservation observation(StoreContainerParentDecision parent,
                                                                  BlockPos raw) {
        return new StoreContainerCandidateObservation(
                true,
                true,
                "operation-route-summary",
                "route-child",
                parent.sequence(),
                parent.branchEpoch(),
                raw,
                3,
                1,
                2,
                1,
                1,
                0,
                0,
                0,
                "{CHEST_ABOVE_BLOCKED_UNBREAKABLE=1, CONTAINER_CACHE_FULL=1}",
                raw,
                "CHEST_ABOVE_BLOCKED_UNBREAKABLE",
                raw.add(1, 0, 0),
                "CONTAINER_CACHE_FULL",
                1,
                1,
                StoreContainerRawCandidateOutcome.RAW_REJECTED.name(),
                StoreContainerCandidateRejectionReason.CHEST_ABOVE_BLOCKED_UNBREAKABLE.name(),
                true,
                "EXACT_STORE_PREDICATE_RESULT"
        );
    }

    private static List<String> keys(Object[] fields) {
        assertEquals(0, fields.length & 1, "Object[] fields must contain ordered key/value pairs");
        List<String> keys = new ArrayList<>();
        for (int index = 0; index < fields.length; index += 2) {
            keys.add(String.valueOf(fields[index]));
        }
        return keys;
    }

    private static Object field(Object[] fields, String expectedKey) {
        for (int index = 0; index + 1 < fields.length; index += 2) {
            if (expectedKey.equals(fields[index])) {
                return fields[index + 1];
            }
        }
        throw new AssertionError("Missing field: " + expectedKey);
    }
}
