package lavi.minecraft.diagnostics.container.gui.dispatch;

import adris.altoclef.eventbus.Subscription;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260904_kpopmodder: Lock listener eligibility to the immutable dispatch-capture boundary.
class ContainerScreenListenerSnapshotTest {
    @Test
    void capturesZeroListeners() {
        ContainerScreenListenerSnapshot snapshot =
                ContainerScreenListenerSnapshot.capture(List.of());

        assertEquals(0, snapshot.registeredCount());
        assertEquals(0, snapshot.eligibleCount());
        assertFalse(snapshot.wasEligible(new Subscription<>(ignored -> { })));
    }

    @Test
    void capturesAllDeletedListenersAsRegisteredButIneligible() {
        Subscription<Object> first = new Subscription<>(ignored -> { });
        Subscription<Object> second = new Subscription<>(ignored -> { });
        first.delete();
        second.delete();

        ContainerScreenListenerSnapshot snapshot =
                ContainerScreenListenerSnapshot.capture(List.of(first, second));

        assertEquals(2, snapshot.registeredCount());
        assertEquals(0, snapshot.eligibleCount());
        assertFalse(snapshot.wasEligible(first));
        assertFalse(snapshot.wasEligible(second));
    }

    @Test
    void distinguishesActiveAndDeletedListenersByIdentity() {
        Subscription<Object> active = new Subscription<>(ignored -> { });
        Subscription<Object> deleted = new Subscription<>(ignored -> { });
        deleted.delete();

        ContainerScreenListenerSnapshot snapshot =
                ContainerScreenListenerSnapshot.capture(List.of(active, deleted));

        assertEquals(2, snapshot.registeredCount());
        assertEquals(1, snapshot.eligibleCount());
        assertTrue(snapshot.wasEligible(active));
        assertFalse(snapshot.wasEligible(deleted));
    }

    @Test
    void preservesCaptureTimeEligibilityAfterListenerIsDeleted() {
        Subscription<Object> listener = new Subscription<>(ignored -> { });
        ContainerScreenListenerSnapshot snapshot =
                ContainerScreenListenerSnapshot.capture(List.of(listener));

        listener.delete();

        assertTrue(listener.shouldDelete());
        assertEquals(1, snapshot.registeredCount());
        assertEquals(1, snapshot.eligibleCount());
        assertTrue(snapshot.wasEligible(listener));
    }
}
