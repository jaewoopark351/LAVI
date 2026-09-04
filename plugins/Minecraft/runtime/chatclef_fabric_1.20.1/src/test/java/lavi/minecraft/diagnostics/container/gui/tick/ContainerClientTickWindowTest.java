package lavi.minecraft.diagnostics.container.gui.tick;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260904_kpopmodder: Prove diagnostic tick windows report ownership without prediction or backfill.
class ContainerClientTickWindowTest {
    @Test
    void distinguishesUnavailableActiveClaimedAndBetweenTickWindows() {
        ContainerClientTickWindow window = new ContainerClientTickWindow();

        assertSnapshot(
                window.snapshot(),
                ContainerClientTickWindowState.UNAVAILABLE,
                false,
                -1L,
                -1L,
                -1L
        );

        window.onHead(41L);
        assertSnapshot(
                window.snapshot(),
                ContainerClientTickWindowState.ACTIVE_BEFORE_RETURN,
                true,
                41L,
                41L,
                -1L
        );

        window.onBoundaryPublished(42L);
        assertEquals(ContainerClientTickWindowState.ACTIVE_BEFORE_RETURN, window.snapshot().state());

        window.onBoundaryPublished(41L);
        assertSnapshot(
                window.snapshot(),
                ContainerClientTickWindowState.ACTIVE_BOUNDARY_CLAIMED,
                true,
                41L,
                41L,
                41L
        );

        window.onReturn(42L);
        assertTrue(window.snapshot().activeClientTickSerialPresent());

        window.onReturn(41L);
        assertSnapshot(
                window.snapshot(),
                ContainerClientTickWindowState.BETWEEN_TICKS,
                false,
                -1L,
                41L,
                41L
        );

        window.onBoundaryPublished(42L);
        ContainerClientTickWindowSnapshot betweenTicks = window.snapshot();
        assertFalse(betweenTicks.activeClientTickSerialPresent());
        assertEquals(-1L, betweenTicks.activeClientTickSerial());
        assertEquals(41L, betweenTicks.lastPublishedClientTickBoundarySerial());

        window.clear();
        assertSnapshot(
                window.snapshot(),
                ContainerClientTickWindowState.UNAVAILABLE,
                false,
                -1L,
                -1L,
                -1L
        );
    }

    private static void assertSnapshot(
            ContainerClientTickWindowSnapshot snapshot,
            ContainerClientTickWindowState state,
            boolean activePresent,
            long activeSerial,
            long lastIssuedSerial,
            long lastPublishedSerial) {
        assertEquals(state, snapshot.state());
        assertEquals(activePresent, snapshot.activeClientTickSerialPresent());
        assertEquals(activeSerial, snapshot.activeClientTickSerial());
        assertEquals(lastIssuedSerial, snapshot.lastIssuedClientTickSerial());
        assertEquals(lastPublishedSerial, snapshot.lastPublishedClientTickBoundarySerial());
    }
}
