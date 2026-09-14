//#if MC == 12001
package lavi.minecraft.fabric.chatclef.bridge.command.result.find;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.eventbus.events.TaskFinishedEvent;
import lavi.minecraft.find.model.FindRequest;
import lavi.minecraft.find.model.FindCandidate;
import lavi.minecraft.find.result.FindOutcome;
import lavi.minecraft.find.result.FindTaskResultSource;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminationObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Verify exact root proof, STOP precedence, closed miss shape and player/registry separation at projection.
class FabricChatClefFindResultProjectorTest {
    private static final String DIGEST = "a".repeat(64);
    private static final class Root extends Task implements FindTaskResultSource {
        private final FindRequest request;
        private FindOutcome outcome;
        private boolean throwDiagnostics;
        Root(String kind, String result, boolean found) {
            request = new FindRequest(kind, kind.equals("player") ? "TestPlayer" : "minecraft:chest", "report", DIGEST, 1);
            var candidate = found ? new FindCandidate(-1, "b".repeat(64), "", 1, 64, -3, 4) : null;
            outcome = new FindOutcome("op", request, result, found, "minecraft:overworld", candidate, 4, found ? 1 : 0,
                    !result.equals("OBSERVATION_BOUNDS_EXHAUSTED"), found ? "complete_loaded_scope_candidate_revalidated"
                            : result.equals("OBSERVATION_BOUNDS_EXHAUSTED") ? "elapsed_budget_exhausted" : "complete_loaded_scope_no_match");
        }
        public FindRequest request() { return request; }
        public String operationId() { return "op"; }
        public FindOutcome outcome() { return outcome; }
        public void diagnosticRetired(String reason) { if (throwDiagnostics) throw new IllegalStateException("diagnostic sink"); }
        public void suppressNativePresentation() {}
        public boolean nativePresentationSuppressed() { return true; }
        protected void onStart() {}
        protected Task onTick() { return null; }
        protected void onStop(Task task) {}
        public boolean isFinished() { return true; }
        protected boolean isEqual(Task task) { return task == this; }
        protected String toDebugString() { return "find test"; }
    }
    private Map<String,Object> project(Root root) {
        return new FabricChatClefFindResultProjector(request -> true).fromMatchingCompletion("request",
                FabricChatClefCommandResultDataPayload.fromMap(Map.of("result_reason", "matching_task_finished",
                        "result_fidelity", "callback_plus_matching_user_task_event")), root,
                naturalObservation(root), false).toMap();
    }
    private FabricChatClefCommandTerminationObservation naturalObservation(Root root) {
        var ownership = new lavi.minecraft.integration.lifecycle.root.UserRootOwnership();
        ownership.assigned(root, new Object(), new Object(), new Object());
        var lifetime = ownership.currentFor(root);
        ownership.finished(lifetime, lavi.minecraft.integration.lifecycle.root.UserRootCompletion.known(false));
        return FabricChatClefCommandTerminationObservation.fromTaskFinishedEvent(new TaskFinishedEvent(1, root, lifetime));
    }
    @SuppressWarnings("unchecked") private Map<String,Object> effect(Map<String,Object> result) {
        return (Map<String,Object>) ((Map<String,Object>) result.get("data")).get("effect_payload");
    }
    @Test void completedMissIsUnsatisfiedAndHasNoCandidateCoordinates() {
        var result = project(new Root("block", "NOT_OBSERVED_IN_LOADED_SCOPE", false));
        assertEquals("completed", result.get("status"));
        var payload = effect(result);
        assertEquals(false, payload.get("find_satisfied"));
        assertFalse(payload.containsKey("x")); assertFalse(payload.containsKey("dimension"));
        assertEquals(DIGEST, payload.get("catalog_digest"));
    }
    @Test void boundsCannotBePromotedToDiscovery() {
        var result = project(new Root("block", "OBSERVATION_BOUNDS_EXHAUSTED", false));
        assertEquals("failed", result.get("status"));
        assertFalse(effect(result).containsKey("candidate_identity_digest"));
    }
    @Test void playerRequestAndObservedCandidateUseSeparateDigestsAndForbidRegistryFields() {
        var payload = effect(project(new Root("player", "FOUND_AND_REPORTED", true)));
        assertEquals(FabricChatClefFindResultProjector.playerRequestDigest("TestPlayer"), payload.get("player_identity_digest"));
        assertEquals("b".repeat(64), payload.get("candidate_identity_digest"));
        assertFalse(payload.containsKey("canonical_target_id")); assertFalse(payload.containsKey("catalog_digest"));
    }
    @Test void differentRootOrUserStopNeverProjectsLateSuccess() {
        Root root = new Root("block", "FOUND_AND_REPORTED", true);
        var projector = new FabricChatClefFindResultProjector(request -> true);
        var other = FabricChatClefCommandTerminationObservation.fromTaskFinishedEvent(new TaskFinishedEvent(1, new Root("block", "FOUND_AND_REPORTED", true)));
        assertNull(projector.fromMatchingCompletion("request", FabricChatClefCommandResultDataPayload.empty(), root, other, false));
        var matching = naturalObservation(root);
        assertNull(projector.fromMatchingCompletion("request", FabricChatClefCommandResultDataPayload.empty(), root, matching, true));
    }
    @Test void frozenNaturalCompletionIsPreservedWhenExistingCleanupLaterStopsTask() throws ReflectiveOperationException {
        Root root = new Root("block", "FOUND_AND_REPORTED", true);
        var ownership = new lavi.minecraft.integration.lifecycle.root.UserRootOwnership();
        ownership.assigned(root, new Object(), new Object(), new Object());
        var lifetime = ownership.currentFor(root);
        ownership.finished(lifetime, lavi.minecraft.integration.lifecycle.root.UserRootCompletion.known(false));
        // Represent the mutable post-cleanup Task snapshot independently of its already frozen natural completion.
        var stoppedField = Task.class.getDeclaredField("stopped");
        stoppedField.setAccessible(true); stoppedField.setBoolean(root, true);
        var observation = FabricChatClefCommandTerminationObservation.fromTaskFinishedEvent(new TaskFinishedEvent(1, root, lifetime));
        assertTrue(root.stopped());
        assertNotNull(new FabricChatClefFindResultProjector(request -> true).fromMatchingCompletion("request",
                FabricChatClefCommandResultDataPayload.empty(), root, observation, false));
    }
    @Test void diagnosticRetirementFailureCannotChangeVerifiedResultOrThrowIntoStop() {
        Root root = new Root("block", "FOUND_AND_REPORTED", true); root.throwDiagnostics = true;
        assertEquals("completed", project(root).get("status"));
        assertDoesNotThrow(() -> FabricChatClefFindDiagnosticRetirement.observe(root, "user_stop"));
    }
    @Test void lateResourceReplacementCannotPublishFrozenDiscoveryCoordinates() {
        Root root = new Root("block", "FOUND_AND_REPORTED", true);
        var result = new FabricChatClefFindResultProjector(request -> false).fromMatchingCompletion("request",
                FabricChatClefCommandResultDataPayload.empty(), root, naturalObservation(root), false).toMap();
        assertEquals("unknown", result.get("status"));
        assertFalse(((Map<?,?>) result.get("data")).containsKey("effect_payload"));
        assertTrue(root.outcome().satisfied()); // The operation's immutable outcome is never rewritten.
    }
    @Test void legacyUnboundEventCannotEstablishFindSuccess() {
        Root root = new Root("block", "FOUND_AND_REPORTED", true);
        var legacy = FabricChatClefCommandTerminationObservation.fromTaskFinishedEvent(new TaskFinishedEvent(1, root));
        assertNull(new FabricChatClefFindResultProjector(request -> true).fromMatchingCompletion("request",
                FabricChatClefCommandResultDataPayload.empty(), root, legacy, false));
    }
    @Test void explorationSnapshotIsCheckedBeforeAnyUnchangedWireEvidenceIsPublished() {
        Root root = new Root("entity", "FOUND_AND_REPORTED", true);
        var old = root.outcome;
        var proof = new lavi.minecraft.find.result.FindTerminalPhaseEvidence("op", root.request, root,
                lavi.minecraft.find.result.FindTerminalPhaseEvidence.Phase.STOPPING_EXPLORATION,
                true, true, true, false, false, true);
        root.outcome = new FindOutcome("op", root.request, old.findResult(), true, old.dimension(), old.candidate(),
                old.visited(), old.matched(), true, "discovery_target_revalidated_and_exploration_quiet", proof);
        var result = project(root);
        assertEquals("completed", result.get("status"));
        assertFalse(effect(result).containsKey("terminal_phase")); assertFalse(effect(result).containsKey("discovery_verified"));
        assertEquals(14, effect(result).size()); // Exactly the existing registry report-success fields.
        root.outcome = new FindOutcome("op", root.request, old.findResult(), true, old.dimension(), old.candidate(),
                old.visited(), old.matched(), true, root.outcome.reason(),
                new lavi.minecraft.find.result.FindTerminalPhaseEvidence("op", root.request, new Object(), proof.phase(),
                        true, true, true, false, false, true));
        var rejected = project(root);
        assertEquals("unknown", rejected.get("status"));
        assertFalse(((Map<?,?>) rejected.get("data")).containsKey("effect_payload"));
    }
}
//#endif
