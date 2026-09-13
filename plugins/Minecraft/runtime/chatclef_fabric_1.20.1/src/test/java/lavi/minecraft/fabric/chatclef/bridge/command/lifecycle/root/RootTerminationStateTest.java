package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.root;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.root.capture.UserRootSnapshot;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.root.capture.UserRootReadStatus;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.root.state.RootTerminationState;
import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.observation.queue.CompletionObservationQueue;
import lavi.minecraft.integration.lifecycle.root.UserRootOwnership;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

//20260913_kpopmodder: Check retirement, absent/failed reads and a finite queued prefix across several ticks.
class RootTerminationStateTest {
    private final Object engine = new Object(), chain = new Object(), world = new Object(), player = new Object();
    private UserRootSnapshot snapshot(Object root, UserRootOwnership owner) {
        return new UserRootSnapshot(root == null ? UserRootReadStatus.ROOT_ABSENT : UserRootReadStatus.PRESENT,
                engine, chain, world, player, root, owner.currentFor(root), "");
    }
    @Test void pendingThirtyThirdCompletionIsNotLostToLaterUnrelatedEvents() {
        Object root = new Object(), idle = new Object();
        var owner = new UserRootOwnership();
        owner.assigned(root, null, world, player);
        var state = new RootTerminationState();
        state.bind(snapshot(root, owner), root);
        var queue = new CompletionObservationQueue<Integer>();
        for (int i = 1; i <= 33; i++) queue.offer(i);
        for (int i = 1; i <= 32; i++) assertEquals(i, queue.poll());
        owner.assigned(idle, null, world, player);
        state.observe(snapshot(idle, owner), queue.acceptedThrough(), queue.dequeuedThrough());
        assertTrue(state.pending());
        assertFalse(state.ready());
        for (int i = 34; i <= 500; i++) queue.offer(i);
        assertEquals(33, queue.poll());
        state.observe(snapshot(idle, owner), queue.acceptedThrough(), queue.dequeuedThrough());
        assertEquals(33, state.pendingThrough());
        assertTrue(state.ready());
        assertEquals("command_root_replaced", state.reason());
    }
    @Test void failedAndMissingEnvironmentReadsNeverProveRootRemoval() {
        Object root = new Object();
        var owner = new UserRootOwnership();
        owner.assigned(root, null, world, player);
        var state = new RootTerminationState();
        state.bind(snapshot(root, owner), root);
        for (var status : UserRootReadStatus.values()) {
            if (status == UserRootReadStatus.PRESENT || status == UserRootReadStatus.ROOT_ABSENT) continue;
            state.observe(UserRootSnapshot.unavailable(status, "test"), 0, 0);
            assertFalse(state.ready());
            assertNull(state.retirementSnapshot());
        }
        state.observe(snapshot(null, owner), 0, 0);
        assertTrue(state.ready());
        assertEquals("command_root_removed", state.reason());
    }
    @Test void defenseOrDepositLeavesSameUserRootAndCallbackOwnership() {
        Object root = new Object(), callback = new Object();
        var owner = new UserRootOwnership();
        owner.assigned(root, callback, world, player);
        var state = new RootTerminationState();
        state.bind(snapshot(root, owner), root);
        for (int i = 0; i < 100; i++) state.observe(snapshot(root, owner), i, i);
        assertFalse(state.ready());
        assertNull(state.retirementSnapshot());
        owner.assigned(root, new Object(), world, player);
        state.observe(snapshot(root, owner), 100, 100);
        assertTrue(state.ready());
        assertEquals("command_callback_ownership_replaced", state.reason());
    }
    @Test void oldEnvironmentCannotRetireACommandInAnotherWorld() {
        Object root = new Object();
        var owner = new UserRootOwnership();
        owner.assigned(root, null, world, player);
        var state = new RootTerminationState();
        state.bind(snapshot(root, owner), root);
        state.observe(new UserRootSnapshot(UserRootReadStatus.ROOT_ABSENT,
                engine, chain, new Object(), player, null, null, ""), 0, 0);
        assertFalse(state.ready());
        assertNull(state.retirementSnapshot());
    }
    @Test void reusedTaskReferenceRejectsEventsFromOldCallbackLifetime() {
        Object root = new Object();
        var owner = new UserRootOwnership();
        owner.assigned(root, new Object(), world, player);
        var old = owner.currentFor(root);
        owner.assigned(root, new Object(), world, player);
        var state = new RootTerminationState();
        state.bind(snapshot(root, owner), root);
        assertFalse(state.accepts(root, old));
        assertTrue(state.accepts(root, owner.currentFor(root)));
        assertFalse(state.accepts(root, null));
    }
}
