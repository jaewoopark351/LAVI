package lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip;

import adris.altoclef.AltoClef;
import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip.observation.EquipSlotObservation;
import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip.observation.EquipSlotReader;
import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip.observation.EquipSlotValue;
import lavi.minecraft.testsupport.TestObjects;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class EquipSlotReaderTest {
    @BeforeAll static void bootstrap() { SharedConstants.createGameVersion(); Bootstrap.initialize(); }
    @Test void productionReaderReadsArmorAndOffhandOnClientThreadAndFreezesValues() throws Exception {
        try (var fixture = new ClientFixture()) {
            fixture.inventory.armor.set(3, new ItemStack(Items.DIAMOND_HELMET));
            fixture.inventory.armor.set(2, new ItemStack(Items.DIAMOND_CHESTPLATE));
            fixture.inventory.armor.set(1, new ItemStack(Items.DIAMOND_LEGGINGS));
            fixture.inventory.armor.set(0, new ItemStack(Items.DIAMOND_BOOTS));
            fixture.inventory.offHand.set(0, new ItemStack(Items.SHIELD));
            var result = new EquipSlotReader().get();
            assertTrue(result.available(), result.reason());
            assertEquals(EquipSlotObservation.SLOT_ORDER, result.slots().stream().map(EquipSlotValue::slot).toList());
            assertEquals("minecraft:diamond_helmet", result.slots().get(0).itemId());
            assertEquals("minecraft:shield", result.slots().get(4).itemId());
            fixture.inventory.armor.set(3, ItemStack.EMPTY);
            assertEquals("minecraft:diamond_helmet", result.slots().get(0).itemId());
            var changed = new EquipSlotReader().get();
            assertEquals("minecraft:air", changed.slots().get(0).itemId());
            assertEquals(0, changed.slots().get(0).count());
            assertSame(result.world(), changed.world());
            assertSame(result.player(), changed.player());
        }
    }
    @Test void productionReaderRefusesWorkerThreadAndMissingWorld() throws Exception {
        try (var fixture = new ClientFixture()) {
            var observed = new AtomicReference<EquipSlotObservation>();
            Thread worker = new Thread(() -> observed.set(new EquipSlotReader().get()), "equip-observation-test-worker");
            worker.start(); worker.join(2000);
            assertNotNull(observed.get());
            assertEquals("minecraft_client_thread_required", observed.get().reason());
            fixture.client.world = null;
            assertEquals("client_world_or_player_unavailable", new EquipSlotReader().get().reason());
        }
    }
    private static final class ClientFixture implements AutoCloseable {
        final Field clientField, modField;
        final Object previousClient, previousMod;
        final TestClient client;
        final PlayerInventory inventory;
        ClientFixture() throws Exception {
            clientField = MinecraftClient.class.getDeclaredField("instance"); clientField.setAccessible(true);
            modField = AltoClef.class.getDeclaredField("instance"); modField.setAccessible(true);
            previousClient = clientField.get(null); previousMod = modField.get(null);
            client = TestObjects.allocate(TestClient.class);
            client.owner = Thread.currentThread();
            client.world = TestObjects.allocate(ClientWorld.class);
            client.player = TestObjects.allocate(ClientPlayerEntity.class);
            inventory = new PlayerInventory(client.player);
            TestObjects.setField(client.player, PlayerEntity.class, "inventory", inventory);
            clientField.set(null, client); modField.set(null, TestObjects.allocate(AltoClef.class));
        }
        @Override public void close() throws Exception { clientField.set(null, previousClient); modField.set(null, previousMod); }
    }
    private static final class TestClient extends MinecraftClient {
        Thread owner;
        private TestClient() { super(null); }
        @Override public Thread getThread() { return owner; }
    }
}
