package lavi.minecraft.integration.mining.operation.hotbar;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.tasksystem.TaskChain;
import lavi.minecraft.testsupport.TestObjects;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

//20260913_kpopmodder: Use the real Task.tick/stop implementation to verify failure-owner retention until root completion.
class MiningFailureOwnerRetentionTest {
    @Test void rootFailureDuringOwnerEvaluationDoesNotMakeAncestorsReplaceThatOwnerAfterReturning() {
        TreeTask root = new TreeTask(), middle = new TreeTask(), owner = new TreeTask();
        root.next = middle; middle.next = owner;
        owner.action = () -> { owner.failure = "HOTBAR_LAYOUT_UNAVAILABLE"; root.fail(owner.failure); };
        TaskChain chain = TestObjects.allocate(Chain.class);
        TestObjects.setField(chain, TaskChain.class, "cachedTaskChain", new ArrayList<Task>());
        root.tick(chain);
        assertTrue(root.stopped());
        assertTrue(root.thisOrChildSatisfies(task -> task == owner), "Failure owner must remain reachable at the next root completion boundary");
        assertEquals("HOTBAR_LAYOUT_UNAVAILABLE", owner.failure);
        assertEquals(1, root.evaluations); assertEquals(1, middle.evaluations); assertEquals(1, owner.evaluations);
    }
    private static final class TreeTask extends Task {
        TreeTask next; Runnable action; String failure; int evaluations;
        @Override protected void onStart() { }
        @Override protected Task onTick() { evaluations++; if (action != null) action.run(); return next; }
        @Override protected void onStop(Task interruptTask) { }
        @Override protected boolean isEqual(Task other) { return this == other; }
        @Override protected String toDebugString() { return "failure-owner-fixture"; }
    }
    private static final class Chain extends TaskChain {
        private Chain() { super(null); }
        @Override protected void onStop() { }
        @Override public void onInterrupt(TaskChain other) { }
        @Override protected void onTick() { }
        @Override public float getPriority() { return 0; }
        @Override public boolean isActive() { return true; }
        @Override public String getName() { return "fixture"; }
    }
}
