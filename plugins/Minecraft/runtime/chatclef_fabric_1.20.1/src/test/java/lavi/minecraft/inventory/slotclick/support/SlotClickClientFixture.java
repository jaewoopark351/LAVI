package lavi.minecraft.inventory.slotclick.support;

import adris.altoclef.AltoClef;
import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.Subscription;
import adris.altoclef.eventbus.events.SlotClickChangedEvent;
import adris.altoclef.tasks.container.ContainerStoredTracker;
import lavi.minecraft.testsupport.TestObjects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.GenericContainerScreenHandler;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

//20260916_kpopmodder: Reuse TestObjects to bind real chest/player slots without starting a game client.
public final class SlotClickClientFixture implements AutoCloseable {
    public final MinecraftClient client;
    public final ClientPlayerEntity player;
    public final PlayerInventory inventory;
    public final GenericContainerScreenHandler handler;
    public final ContainerStoredTracker tracker = new ContainerStoredTracker(slot -> true);
    public final List<SlotClickChangedEvent> events = new ArrayList<>();

    private final Field clientField;
    private final Field modField;
    private final Object previousClient;
    private final Object previousMod;
    private final Subscription<SlotClickChangedEvent> subscription;

    public SlotClickClientFixture() throws Exception {
        clientField = MinecraftClient.class.getDeclaredField("instance");
        clientField.setAccessible(true);
        modField = AltoClef.class.getDeclaredField("instance");
        modField.setAccessible(true);
        previousClient = clientField.get(null);
        previousMod = modField.get(null);
        TestClient testClient = TestObjects.allocate(TestClient.class);
        testClient.owner = Thread.currentThread();
        client = testClient;
        client.world = TestObjects.allocate(ClientWorld.class);
        player = TestObjects.allocate(ClientPlayerEntity.class);
        client.player = player;
        inventory = new PlayerInventory(player);
        TestObjects.setField(player, PlayerEntity.class, "inventory", inventory);
        handler = GenericContainerScreenHandler.createGeneric9x3(7, inventory);
        player.currentScreenHandler = handler;
        GenericContainerScreen screen = TestObjects.allocate(GenericContainerScreen.class);
        TestObjects.setField(screen, HandledScreen.class, "handler", handler);
        client.currentScreen = screen;
        clientField.set(null, client);
        modField.set(null, TestObjects.allocate(AltoClef.class));
        subscription = EventBus.subscribe(SlotClickChangedEvent.class, events::add);
    }

    @Override
    public void close() throws Exception {
        try {
            tracker.stopTracking();
        } finally {
            try {
                EventBus.unsubscribe(subscription);
            } finally {
                clientField.set(null, previousClient);
                modField.set(null, previousMod);
            }
        }
    }

    private static final class TestClient extends MinecraftClient {
        private Thread owner;

        private TestClient() { super(null); }

        @Override
        public Thread getThread() { return owner; }
    }
}
