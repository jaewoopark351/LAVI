//#if MC == 12001
package lavi.minecraft.task.movement.gotoresult.tracking;

import org.junit.jupiter.api.Test;
import adris.altoclef.eventbus.events.TaskFinishedEvent;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.FabricChatClefCommandTerminationObservation;
import lavi.minecraft.testsupport.TestObjects;
import static org.junit.jupiter.api.Assertions.*;

//20260913_kpopmodder: Exercise the native event gate separately from Minecraft's unchanged pathfinder.
class GotoNativeCompletionTrackerTest {
    @Test void goalAloneIsProvisionalAndEveryNaturalCompletionGuardIsRequired() {
        for (int mask = 0; mask < 32; mask++) {
            var state = new GotoNativeCompletionTracker();
            boolean goal = (mask & 1) != 0;
            boolean stopped = (mask & 2) != 0;
            boolean stopKnown = (mask & 4) != 0;
            boolean worldMatches = (mask & 8) != 0;
            boolean rootReleased = (mask & 16) != 0;
            state.observeGoal(goal);
            assertNull(state.snapshot());
            state.complete(stopped, stopKnown, worldMatches, rootReleased, "minecraft:overworld");
            assertEquals(goal && !stopped && stopKnown && worldMatches && rootReleased,
                    state.snapshot() != null, "mask=" + mask);
        }
    }

    @Test void latestFalseGoalCannotReuseAnEarlierCrossingAndCommittedArrivalRemainsFrozen() {
        var state = new GotoNativeCompletionTracker();
        state.observeGoal(true);
        state.observeGoal(false);
        state.complete(false, true, true, true, "minecraft:overworld");
        assertNull(state.snapshot());
        state.observeGoal(true);
        state.complete(false, true, true, true, "minecraft:overworld");
        var frozen = state.snapshot();
        state.observeGoal(false);
        state.complete(true, true, false, false, "minecraft:the_nether");
        assertSame(frozen, state.snapshot());
        assertEquals("minecraft:overworld", frozen.terminalDimension());
    }

    @Test void optionalSnapshotCaptureFailureDoesNotChangeExistingTaskStopFacts() {
        // A constructor-free fixture deliberately lacks the binding needed by the optional snapshot.
        var task = TestObjects.allocate(ReportedGotoBlockTask.class);
        var observation = FabricChatClefCommandTerminationObservation.fromTaskFinishedEvent(new TaskFinishedEvent(1, task));
        assertTrue(observation.stopStateAvailable());
        assertFalse(observation.taskStopped());
        assertEquals("finished", observation.terminationKind());
        assertSame(task, observation.task());
    }
}
//#endif
