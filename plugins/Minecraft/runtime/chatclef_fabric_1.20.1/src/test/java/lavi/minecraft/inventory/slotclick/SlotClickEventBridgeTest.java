package lavi.minecraft.inventory.slotclick;

import adris.altoclef.eventbus.events.SlotClickChangedEvent;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.inventory.slotclick.support.SlotClickClientFixture;
import lavi.minecraft.testsupport.TestObjects;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

//20260916_kpopmodder: Exercise the real bridge, native EventBus and stored tracker with registered Minecraft stacks.
class SlotClickEventBridgeTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
    }

    @BeforeEach
    void disableDiagnostics() { ChatClefDiagnostics.setBoundaryEnabled(false); }

    @AfterEach
    void preserveDisabledDiagnostics() { ChatClefDiagnostics.setBoundaryEnabled(false); }

    @Test
    void everyNativeActionTypeDelegatesExactlyOnceWithoutInventingChanges() throws Exception {
        try (var fixture = new SlotClickClientFixture()) {
            fixture.tracker.startTracking();
            AtomicInteger nativeCalls = new AtomicInteger();
            for (SlotActionType action : SlotActionType.values()) {
                int previous = nativeCalls.get();
                SlotClickEventBridge.run(fixture.handler, 0, 0, action, fixture.player,
                        nativeCalls::incrementAndGet);
                assertEquals(previous + 1, nativeCalls.get(), action.name());
                assertTrue(fixture.events.isEmpty(), action.name());
                assertEquals(0, fixture.tracker.getStoredCount(Items.DIRT), action.name());
            }
        }
    }

    @Test
    void cursorPickupDoesNotCountStorageAndContainerPlacementCountsExactlyOnce() throws Exception {
        try (var fixture = new SlotClickClientFixture()) {
            fixture.tracker.startTracking();
            fixture.inventory.main.set(9, new ItemStack(Items.DIRT, 64));
            AtomicInteger nativeCalls = new AtomicInteger();
            SlotClickEventBridge.run(fixture.handler, 27, 0, SlotActionType.PICKUP, fixture.player, () -> {
                nativeCalls.incrementAndGet();
                fixture.inventory.main.set(9, ItemStack.EMPTY);
                fixture.handler.setCursorStack(new ItemStack(Items.DIRT, 64));
            });
            assertEquals(1, nativeCalls.get());
            assertEquals(1, fixture.events.size());
            assertEquals(27, fixture.events.get(0).slot.getWindowSlot());
            assertTrue(fixture.events.get(0).slot.isSlotInPlayerInventory());
            assertEquals(64, fixture.events.get(0).before.getCount());
            assertTrue(fixture.events.get(0).after.isEmpty());
            assertEquals(64, fixture.handler.getCursorStack().getCount());
            assertEquals(0, fixture.tracker.getStoredCount(Items.DIRT));

            SlotClickEventBridge.run(fixture.handler, 0, 0, SlotActionType.PICKUP, fixture.player, () -> {
                nativeCalls.incrementAndGet();
                fixture.handler.getInventory().setStack(0, fixture.handler.getCursorStack());
                fixture.handler.setCursorStack(ItemStack.EMPTY);
            });
            assertEquals(2, nativeCalls.get());
            assertEquals(2, fixture.events.size());
            assertEquals(0, fixture.events.get(1).slot.getWindowSlot());
            assertFalse(fixture.events.get(1).slot.isSlotInPlayerInventory());
            assertEquals(64, fixture.tracker.getStoredCount(Items.DIRT));
            assertTrue(fixture.handler.getCursorStack().isEmpty());

            SlotClickEventBridge.run(fixture.handler, 0, 0, SlotActionType.PICKUP, fixture.player,
                    nativeCalls::incrementAndGet);
            assertEquals(3, nativeCalls.get());
            assertEquals(2, fixture.events.size());
            assertEquals(64, fixture.tracker.getStoredCount(Items.DIRT));
        }
    }

    @Test
    void eachChangedSlotPublishesOneEventWithIndependentBeforeSnapshot() throws Exception {
        try (var fixture = new SlotClickClientFixture()) {
            fixture.tracker.startTracking();
            ItemStack retainedStack = new ItemStack(Items.DIRT, 3);
            fixture.handler.getInventory().setStack(0, retainedStack);
            fixture.handler.getInventory().setStack(1, new ItemStack(Items.STONE, 4));
            AtomicInteger nativeCalls = new AtomicInteger();
            SlotClickEventBridge.run(fixture.handler, 0, 0, SlotActionType.PICKUP, fixture.player, () -> {
                nativeCalls.incrementAndGet();
                retainedStack.increment(2);
                fixture.handler.getInventory().setStack(1, new ItemStack(Items.DIRT, 7));
            });
            assertEquals(1, nativeCalls.get());
            assertEquals(2, fixture.events.size());
            SlotClickChangedEvent changed = fixture.events.get(0);
            assertEquals(0, changed.slot.getWindowSlot());
            assertEquals(3, changed.before.getCount());
            assertNotSame(retainedStack, changed.before);
            assertEquals(5, changed.after.getCount());
            assertEquals(1, fixture.events.get(1).slot.getWindowSlot());
            assertSame(Items.STONE, fixture.events.get(1).before.getItem());
            assertSame(Items.DIRT, fixture.events.get(1).after.getItem());
            assertEquals(9, fixture.tracker.getStoredCount(Items.DIRT));
            assertEquals(-4, fixture.tracker.getStoredCount(Items.STONE));
            retainedStack.increment(1);
            assertEquals(3, changed.before.getCount());
        }
    }

    @Test
    void equalReplacementAndCursorOnlyChangeDoNotPublishSlotEvents() throws Exception {
        try (var fixture = new SlotClickClientFixture()) {
            fixture.tracker.startTracking();
            fixture.handler.getInventory().setStack(0, new ItemStack(Items.DIRT, 3));
            AtomicInteger nativeCalls = new AtomicInteger();
            SlotClickEventBridge.run(fixture.handler, 0, 0, SlotActionType.PICKUP, fixture.player, () -> {
                nativeCalls.incrementAndGet();
                fixture.handler.getInventory().setStack(0, new ItemStack(Items.DIRT, 3));
                fixture.handler.setCursorStack(new ItemStack(Items.DIRT, 1));
            });
            assertEquals(1, nativeCalls.get());
            assertTrue(fixture.events.isEmpty());
            assertEquals(0, fixture.tracker.getStoredCount(Items.DIRT));
        }
    }

    @Test
    void nbtOnlyChangeRetainsNativeStackEqualityAndDoesNotInventStoredQuantity() throws Exception {
        try (var fixture = new SlotClickClientFixture()) {
            fixture.tracker.startTracking();
            fixture.handler.getInventory().setStack(0, new ItemStack(Items.DIRT, 3));
            SlotClickEventBridge.run(fixture.handler, 0, 0, SlotActionType.PICKUP, fixture.player,
                    () -> fixture.handler.getInventory().getStack(0).getOrCreateNbt().putInt("slotclick_test", 1));
            assertEquals(1, fixture.events.size());
            assertFalse(fixture.events.get(0).before.hasNbt());
            assertEquals(1, fixture.events.get(0).after.getNbt().getInt("slotclick_test"));
            assertEquals(0, fixture.tracker.getStoredCount(Items.DIRT));
        }
    }

    @Test
    void nativeExceptionPropagatesWithoutPublishingPartialChangesOrLeakingNextSnapshot() throws Exception {
        try (var fixture = new SlotClickClientFixture()) {
            fixture.tracker.startTracking();
            AtomicInteger nativeCalls = new AtomicInteger();
            IllegalStateException expected = new IllegalStateException("slotclick native failure fixture");
            IllegalStateException actual = assertThrows(IllegalStateException.class, () ->
                    SlotClickEventBridge.run(fixture.handler, 0, 0, SlotActionType.PICKUP, fixture.player, () -> {
                        nativeCalls.incrementAndGet();
                        fixture.handler.getInventory().setStack(0, new ItemStack(Items.DIRT, 1));
                        throw expected;
                    }));
            assertSame(expected, actual);
            assertEquals(1, nativeCalls.get());
            assertTrue(fixture.events.isEmpty());
            assertEquals(0, fixture.tracker.getStoredCount(Items.DIRT));
            SlotClickEventBridge.run(fixture.handler, 0, 0, SlotActionType.PICKUP, fixture.player,
                    () -> fixture.handler.getInventory().getStack(0).increment(1));
            assertEquals(1, fixture.events.size());
            assertEquals(1, fixture.events.get(0).before.getCount());
            assertEquals(1, fixture.tracker.getStoredCount(Items.DIRT));
        }
    }

    @Test
    void serverPlayerCallsNativeExactlyOnceWithoutPublishingClientEvents() throws Exception {
        try (var fixture = new SlotClickClientFixture()) {
            fixture.tracker.startTracking();
            ServerPlayerEntity serverPlayer = TestObjects.allocate(ServerPlayerEntity.class);
            AtomicInteger nativeCalls = new AtomicInteger();
            SlotClickEventBridge.run(fixture.handler, 0, 0, SlotActionType.PICKUP, serverPlayer, () -> {
                nativeCalls.incrementAndGet();
                fixture.handler.getInventory().setStack(0, new ItemStack(Items.DIRT, 64));
            });
            assertEquals(1, nativeCalls.get());
            assertTrue(fixture.events.isEmpty());
            assertEquals(0, fixture.tracker.getStoredCount(Items.DIRT));
        }
    }

    @Test
    void differentClientPlayerCallsNativeExactlyOnceWithoutPublishing() throws Exception {
        try (var fixture = new SlotClickClientFixture()) {
            fixture.tracker.startTracking();
            ClientPlayerEntity otherPlayer = TestObjects.allocate(ClientPlayerEntity.class);
            otherPlayer.currentScreenHandler = fixture.handler;
            AtomicInteger nativeCalls = new AtomicInteger();
            SlotClickEventBridge.run(fixture.handler, 0, 0, SlotActionType.PICKUP, otherPlayer, () -> {
                nativeCalls.incrementAndGet();
                fixture.handler.getInventory().setStack(0, new ItemStack(Items.DIRT, 64));
            });
            assertEquals(1, nativeCalls.get());
            assertTrue(fixture.events.isEmpty());
            assertEquals(0, fixture.tracker.getStoredCount(Items.DIRT));
        }
    }

    @Test
    void nonCurrentHandlerCallsNativeExactlyOnceWithoutPublishing() throws Exception {
        try (var fixture = new SlotClickClientFixture()) {
            fixture.tracker.startTracking();
            GenericContainerScreenHandler other = GenericContainerScreenHandler.createGeneric9x3(8, fixture.inventory);
            AtomicInteger nativeCalls = new AtomicInteger();
            SlotClickEventBridge.run(other, 0, 0, SlotActionType.PICKUP, fixture.player, () -> {
                nativeCalls.incrementAndGet();
                other.getInventory().setStack(0, new ItemStack(Items.DIRT, 64));
            });
            assertEquals(1, nativeCalls.get());
            assertTrue(fixture.events.isEmpty());
            assertEquals(0, fixture.tracker.getStoredCount(Items.DIRT));
        }
    }

    @Test
    void workerThreadCallsNativeExactlyOnceWithoutPublishing() throws Exception {
        try (var fixture = new SlotClickClientFixture()) {
            fixture.tracker.startTracking();
            AtomicInteger nativeCalls = new AtomicInteger();
            AtomicReference<Throwable> failure = new AtomicReference<>();
            Thread worker = new Thread(() -> {
                try {
                    SlotClickEventBridge.run(fixture.handler, 0, 0, SlotActionType.PICKUP, fixture.player, () -> {
                        nativeCalls.incrementAndGet();
                        fixture.handler.getInventory().setStack(0, new ItemStack(Items.DIRT, 64));
                    });
                } catch (Throwable caught) { failure.set(caught); }
            }, "slotclick-event-bridge-test-worker");
            worker.start();
            worker.join(2000);
            assertFalse(worker.isAlive(), "worker did not finish");
            assertNull(failure.get());
            assertEquals(1, nativeCalls.get());
            assertTrue(fixture.events.isEmpty());
            assertEquals(0, fixture.tracker.getStoredCount(Items.DIRT));
        }
    }
}
