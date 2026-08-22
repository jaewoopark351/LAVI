package lavi.minecraft.fabric.chatclef.bridge.command;

import adris.altoclef.tasks.movement.IdleTask;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefRootOwnershipClassification;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefRootOwnershipClassifier;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class FabricChatClefRootOwnershipClassifierTest {
    private final FabricChatClefRootOwnershipClassifier classifier = new FabricChatClefRootOwnershipClassifier();

    @Test
    void sameIdleRootAssignmentAndGenerationClassifiesAsPreexistingUnchangedIdleRoot() {
        IdleTask idle = new IdleTask();

        FabricChatClefRootOwnershipClassification classification = classifier.classify(
                evidence(idle, "user-root-1", 1L, true, false, 10L),
                evidence(idle, "user-root-1", 1L, true, false, 10L)
        );

        assertEquals(FabricChatClefRootOwnershipClassification.PREEXISTING_UNCHANGED_IDLE_ROOT, classification);
    }

    @Test
    void sameIdleRootAcrossTickBoundaryClassifiesAsOwnershipUnknown() {
        IdleTask idle = new IdleTask();

        FabricChatClefRootOwnershipClassification classification = classifier.classify(
                evidence(idle, "user-root-1", 1L, true, false, 10L),
                evidence(idle, "user-root-1", 1L, true, false, 11L)
        );

        assertEquals(FabricChatClefRootOwnershipClassification.OWNERSHIP_UNKNOWN, classification);
    }

    @Test
    void sameIdleRootAcrossThreadBoundaryClassifiesAsOwnershipUnknown() {
        IdleTask idle = new IdleTask();

        FabricChatClefRootOwnershipClassification classification = classifier.classify(
                evidence(idle, "user-root-1", 1L, true, false, 10L, "Render thread"),
                evidence(idle, "user-root-1", 1L, true, false, 10L, "Worker thread")
        );

        assertEquals(FabricChatClefRootOwnershipClassification.OWNERSHIP_UNKNOWN, classification);
    }

    @Test
    void blankCaptureThreadClassifiesAsOwnershipUnknown() {
        IdleTask idle = new IdleTask();

        FabricChatClefRootOwnershipClassification classification = classifier.classify(
                evidence(idle, "user-root-1", 1L, true, false, 10L, ""),
                evidence(idle, "user-root-1", 1L, true, false, 10L, "Render thread")
        );

        assertEquals(FabricChatClefRootOwnershipClassification.OWNERSHIP_UNKNOWN, classification);
    }

    @Test
    void unavailableBeforeOrAfterEvidenceClassifiesAsOwnershipUnknown() {
        IdleTask idle = new IdleTask();

        assertEquals(
                FabricChatClefRootOwnershipClassification.OWNERSHIP_UNKNOWN,
                classifier.classify(FabricChatClefTaskOwnershipEvidence.empty(), evidence(idle, "user-root-1", 1L, true, false, 10L))
        );
        assertEquals(
                FabricChatClefRootOwnershipClassification.OWNERSHIP_UNKNOWN,
                classifier.classify(evidence(idle, "user-root-1", 1L, true, false, 10L), FabricChatClefTaskOwnershipEvidence.empty())
        );
    }

    @Test
    void sameIdleClassDifferentObjectWithoutAssignmentTransitionIsUnknown() {
        FabricChatClefRootOwnershipClassification classification = classifier.classify(
                evidence(new IdleTask(), "user-root-1", 1L, true, false, 10L),
                evidence(new IdleTask(), "user-root-1", 1L, true, false, 10L)
        );

        assertEquals(FabricChatClefRootOwnershipClassification.OWNERSHIP_UNKNOWN, classification);
    }

    @Test
    void changedRootWithAssignmentTransitionClassifiesAsCommandOwned() {
        FabricChatClefRootOwnershipClassification classification = classifier.classify(
                evidence(new IdleTask(), "user-root-1", 1L, true, false, 10L),
                evidence(new IdleTask(), "user-root-2", 2L, true, false, 10L)
        );

        assertEquals(FabricChatClefRootOwnershipClassification.COMMAND_OWNED_ROOT, classification);
    }

    @Test
    void nextIdleFlagDisqualifiesPreexistingIdleRoot() {
        IdleTask idle = new IdleTask();

        FabricChatClefRootOwnershipClassification classification = classifier.classify(
                evidence(idle, "user-root-1", 1L, true, false, 10L),
                evidence(idle, "user-root-1", 1L, true, true, 10L)
        );

        assertNotEquals(FabricChatClefRootOwnershipClassification.PREEXISTING_UNCHANGED_IDLE_ROOT, classification);
    }

    @Test
    void sameObjectWithChangedAssignmentDoesNotClassifyAsPreexistingIdleRoot() {
        IdleTask idle = new IdleTask();

        FabricChatClefRootOwnershipClassification classification = classifier.classify(
                evidence(idle, "user-root-1", 1L, true, false, 10L),
                evidence(idle, "user-root-2", 1L, true, false, 10L)
        );

        assertNotEquals(FabricChatClefRootOwnershipClassification.PREEXISTING_UNCHANGED_IDLE_ROOT, classification);
    }

    @Test
    void sameObjectWithChangedGenerationDoesNotClassifyAsPreexistingIdleRoot() {
        IdleTask idle = new IdleTask();

        FabricChatClefRootOwnershipClassification classification = classifier.classify(
                evidence(idle, "user-root-1", 1L, true, false, 10L),
                evidence(idle, "user-root-1", 2L, true, false, 10L)
        );

        assertNotEquals(FabricChatClefRootOwnershipClassification.PREEXISTING_UNCHANGED_IDLE_ROOT, classification);
    }

    @Test
    void contradictoryRawRootAndSnapshotClassClassifiesAsOwnershipUnknown() {
        IdleTask idle = new IdleTask();

        FabricChatClefRootOwnershipClassification classification = classifier.classify(
                evidence(idle, "user-root-1", 1L, true, false, 10L),
                evidence(idle, "user-root-1", 1L, true, false, 10L, "test", "not.IdleTask")
        );

        assertEquals(FabricChatClefRootOwnershipClassification.OWNERSHIP_UNKNOWN, classification);
    }

    @Test
    void absentPostDispatchRootKeepsNoRootVisiblePath() {
        FabricChatClefRootOwnershipClassification classification = classifier.classify(
                evidence(new IdleTask(), "user-root-1", 1L, true, false, 10L),
                evidence(null, "none", 0L, false, false, 10L)
        );

        assertEquals(FabricChatClefRootOwnershipClassification.NO_ROOT_VISIBLE, classification);
    }

    private static FabricChatClefTaskOwnershipEvidence evidence(
            Task root,
            String assignmentId,
            long generation,
            boolean runningIdle,
            boolean nextIdle,
            long clientTick
    ) {
        return evidence(root, assignmentId, generation, runningIdle, nextIdle, clientTick, "test");
    }

    private static FabricChatClefTaskOwnershipEvidence evidence(
            Task root,
            String assignmentId,
            long generation,
            boolean runningIdle,
            boolean nextIdle,
            long clientTick,
            String captureThread
    ) {
        return evidence(
                root,
                assignmentId,
                generation,
                runningIdle,
                nextIdle,
                clientTick,
                captureThread,
                root == null ? "" : root.getClass().getName()
        );
    }

    private static FabricChatClefTaskOwnershipEvidence evidence(
            Task root,
            String assignmentId,
            long generation,
            boolean runningIdle,
            boolean nextIdle,
            long clientTick,
            String captureThread,
            String snapshotClass
    ) {
        FabricChatClefTaskSnapshot rootSnapshot = FabricChatClefTaskSnapshot.capture(root);
        FabricChatClefTaskOwnershipSnapshot ownership = FabricChatClefTaskOwnershipSnapshot.of(
                1000L,
                clientTick,
                captureThread,
                rootSnapshot,
                snapshotClass,
                root == null ? "none" : Integer.toHexString(System.identityHashCode(root)),
                assignmentId,
                generation,
                runningIdle,
                nextIdle,
                false,
                "",
                "",
                false,
                ""
        );
        return FabricChatClefTaskOwnershipEvidence.of(root, rootSnapshot, ownership, 1000L, 1000L, clientTick, captureThread);
    }
}
