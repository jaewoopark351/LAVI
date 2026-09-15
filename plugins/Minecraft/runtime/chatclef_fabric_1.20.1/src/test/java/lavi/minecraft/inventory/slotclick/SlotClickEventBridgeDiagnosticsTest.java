package lavi.minecraft.inventory.slotclick;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.inventory.slotclick.support.SlotClickClientFixture;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;

import static lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.captureOutput;
import static org.junit.jupiter.api.Assertions.*;

//20260916_kpopmodder: Check the actual formatted diagnostic sink separately from slot-change behavior.
class SlotClickEventBridgeDiagnosticsTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
    }

    @BeforeEach
    void prepareIsolatedDiagnosticSession() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @AfterEach
    void disableDiagnostics() { ChatClefDiagnostics.setBoundaryEnabled(false); }

    @Test
    void offBoundaryAndFailingOutputPreserveNativeCallsEventsAndStoredDeltas() throws Exception {
        // One real activation uses the existing finite reservation; no reflection clears its process budget.
        try (var fixture = new SlotClickClientFixture()) {
            AtomicInteger nativeCalls = new AtomicInteger();
            fixture.tracker.startTracking();
            String offOutput = captureOutput(() -> SlotClickEventBridge.run(
                    fixture.handler, 0, 0, SlotActionType.PICKUP, fixture.player, () -> {
                        nativeCalls.incrementAndGet();
                        fixture.handler.getInventory().setStack(0, new ItemStack(Items.DIRT, 64));
                    }));
            assertEquals(1, nativeCalls.get());
            assertEquals(1, fixture.events.size());
            assertEquals(64, fixture.tracker.getStoredCount(Items.DIRT));
            assertFalse(offOutput.contains("STORE_COUNTER_"), offOutput);

            fixture.tracker.stopTracking();
            ChatClefDiagnostics.setBoundaryEnabled(true);
            assertTrue(ChatClefDiagnostics.isBoundaryEnabled());
            String boundaryOutput = captureOutput(() -> {
                // The same tracker retains its native total while its new subscription captures the enabled trace.
                fixture.tracker.startTracking();
                SlotClickEventBridge.run(fixture.handler, 1, 0, SlotActionType.PICKUP, fixture.player, () -> {
                    nativeCalls.incrementAndGet();
                    fixture.handler.getInventory().setStack(1, new ItemStack(Items.DIRT, 64));
                });
            });
            assertEquals(2, nativeCalls.get());
            assertEquals(2, fixture.events.size());
            assertEquals(128, fixture.tracker.getStoredCount(Items.DIRT));
            assertTrue(boundaryOutput.contains("RESOURCE_OBSERVATION_DEPOSIT_FIRST"), boundaryOutput);
            assertTrue(boundaryOutput.contains("OUTER_REDIRECT_ENTER"), boundaryOutput);
            assertTrue(boundaryOutput.contains("OUTER_ACTION_RETURN"), boundaryOutput);
            assertTrue(boundaryOutput.contains("STORE_COUNTER_COUNTER_UPDATE"), boundaryOutput);
            assertTrue(boundaryOutput.contains("counterBefore=64"), boundaryOutput);
            assertTrue(boundaryOutput.contains("deltaApplied=64"), boundaryOutput);
            assertTrue(boundaryOutput.contains("counterAfter=128"), boundaryOutput);
            assertTrue(boundaryOutput.contains("CLIENT_LOCAL_SLOT_DIFF_NOT_SERVER_ACK"), boundaryOutput);

            // A new negative-delta boundary attempts physical output even when repeated detail is suppressed.
            AtomicInteger failedWrites = new AtomicInteger();
            PrintStream original = System.out;
            try (PrintStream failingOutput = new PrintStream(OutputStream.nullOutputStream()) {
                @Override
                public void println(String value) {
                    failedWrites.incrementAndGet();
                    throw new IllegalStateException("slotclick diagnostic sink failure fixture");
                }
            }) {
                System.setOut(failingOutput);
                try {
                    SlotClickEventBridge.run(fixture.handler, 1, 0, SlotActionType.PICKUP, fixture.player, () -> {
                        nativeCalls.incrementAndGet();
                        fixture.handler.getInventory().setStack(1, new ItemStack(Items.STONE, 64));
                    });
                } finally {
                    System.setOut(original);
                }
            }
            assertTrue(failedWrites.get() > 0, "the failing physical output seam was not exercised");
            assertEquals(3, nativeCalls.get());
            assertEquals(3, fixture.events.size());
            assertEquals(64, fixture.tracker.getStoredCount(Items.DIRT));
            assertEquals(64, fixture.tracker.getStoredCount(Items.STONE));

            IllegalArgumentException expected = new IllegalArgumentException("slotclick native diagnostic fixture");
            String exceptionalOutput = captureOutput(() -> assertSame(expected,
                    assertThrows(IllegalArgumentException.class, () -> SlotClickEventBridge.run(
                            fixture.handler, 2, 0, SlotActionType.PICKUP, fixture.player, () -> {
                                nativeCalls.incrementAndGet();
                                throw expected;
                            }))));
            assertEquals(4, nativeCalls.get());
            assertEquals(3, fixture.events.size());
            assertTrue(exceptionalOutput.contains("OUTER_ACTION_NOT_RETURNED"), exceptionalOutput);
            assertEquals(64, fixture.tracker.getStoredCount(Items.DIRT));
            assertEquals(64, fixture.tracker.getStoredCount(Items.STONE));
        }
    }
}
