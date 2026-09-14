//#if MC == 12001
package lavi.minecraft.find.result;

import lavi.minecraft.find.model.FindCandidate;
import lavi.minecraft.find.model.FindRequest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FindTerminalReasonContractTest {
    private final Object root = new Object();
    private final FindRequest report = new FindRequest("entity", "minecraft:villager", "report", "a".repeat(64), 1);
    private final FindRequest approach = new FindRequest("entity", "minecraft:villager", "approach", "a".repeat(64), 1);
    private FindOutcome result(FindRequest request, String reason, String result, boolean found, FindTerminalPhaseEvidence proof) {
        return new FindOutcome("op", request, result, found, "minecraft:overworld",
                found ? new FindCandidate(1, "b".repeat(64), "private-identity", 80, 64, 0, 6400) : null,
                4, found ? 1 : 0, true, reason, proof);
    }
    private FindTerminalPhaseEvidence proof(FindRequest request, FindTerminalPhaseEvidence.Phase phase,
            boolean discovered, boolean quiet, boolean revalidated, boolean handedOff, boolean arrived) {
        return new FindTerminalPhaseEvidence("op", request, root, phase, discovered, quiet, revalidated, handedOff, arrived, quiet);
    }
    @Test void reportSuccessRequiresSameRootRequestOperationLiveTargetAndCleanup() {
        var valid = proof(report, FindTerminalPhaseEvidence.Phase.STOPPING_EXPLORATION, true, true, true, false, false);
        var outcome = result(report, "discovery_target_revalidated_and_exploration_quiet", "FOUND_AND_REPORTED", true, valid);
        assertTrue(FindTerminalReasonContract.validates(outcome, root));
        assertFalse(FindTerminalReasonContract.validates(outcome, new Object()));
        assertFalse(FindTerminalReasonContract.validates(result(report, outcome.reason(), outcome.findResult(), true, null), root));
        assertFalse(FindTerminalReasonContract.validates(result(report, outcome.reason(), outcome.findResult(), true,
                proof(report, valid.phase(), true, false, true, false, false)), root));
        assertFalse(FindTerminalReasonContract.validates(result(report, outcome.reason(), outcome.findResult(), true,
                proof(report, valid.phase(), true, true, false, false, false)), root));
        var equalButNotSame = new FindRequest("entity", "minecraft:villager", "report", "a".repeat(64), 1);
        assertFalse(FindTerminalReasonContract.validates(result(report, outcome.reason(), outcome.findResult(), true,
                proof(equalButNotSame, valid.phase(), true, true, true, false, false)), root));
    }
    @Test void arrivalRequiresValidatedHandoffAndSameTargetSafeArrival() {
        var valid = proof(approach, FindTerminalPhaseEvidence.Phase.APPROACHING, true, true, true, true, true);
        assertTrue(FindTerminalReasonContract.validates(result(approach, "same_target_safe_range_and_owned_cleanup",
                "FOUND_AND_IN_SAFE_RANGE", true, valid), root));
        assertFalse(FindTerminalReasonContract.validates(result(approach, "same_target_safe_range_and_owned_cleanup",
                "FOUND_AND_IN_SAFE_RANGE", true, proof(approach, valid.phase(), true, true, true, false, false)), root));
    }
    @Test void unreachableAndDeadlineReasonsCannotInventDiscoveryOrCrossPhases() {
        var searching = proof(approach, FindTerminalPhaseEvidence.Phase.EXPLORING, false, true, false, false, false);
        var handedOff = proof(approach, FindTerminalPhaseEvidence.Phase.APPROACHING, true, true, false, true, false);
        assertTrue(FindTerminalReasonContract.validates(result(approach, "exploration_no_progress", "UNREACHABLE", false, searching), root));
        assertFalse(FindTerminalReasonContract.validates(result(approach, "post_discovery_approach_unreachable", "UNREACHABLE", false, searching), root));
        assertTrue(FindTerminalReasonContract.validates(result(approach, "post_discovery_approach_unreachable", "UNREACHABLE", false, handedOff), root));
        assertFalse(FindTerminalReasonContract.validates(result(approach, "discovery_deadline_exhausted", "TIMEOUT", false, handedOff), root));
        assertTrue(FindTerminalReasonContract.validates(result(approach, "approach_deadline_exhausted", "TIMEOUT", false, handedOff), root));
        assertFalse(FindTerminalReasonContract.validates(result(approach, "approach_deadline_exhausted", "TIMEOUT", false, searching), root));
        assertFalse(FindTerminalReasonContract.validates(result(approach, "unknown_discovery_claim", "UNREACHABLE", false, handedOff), root));
    }
    @Test void legacyErrorNamesRemainFailureOnlyAndNewParentCannotUseLegacyReasons() {
        var block = new FindRequest("block", "minecraft:chest", "report", "a".repeat(64), 1);
        assertTrue(FindTerminalReasonContract.validates(result(block, "CustomReadFailure", "INTERNAL_ERROR", false, null), root));
        assertFalse(FindTerminalReasonContract.validates(result(report, "CustomReadFailure", "FOUND_AND_REPORTED", true, null), root));
        assertFalse(FindTerminalReasonContract.validates(result(report, "complete_loaded_scope_candidate_revalidated", "FOUND_AND_REPORTED", true,
                proof(report, FindTerminalPhaseEvidence.Phase.STOPPING_EXPLORATION, true, true, true, false, false)), root));
    }
}

//#endif
