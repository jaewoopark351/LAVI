//#if MC == 12001
package lavi.minecraft.find.observation.entity;

import adris.altoclef.AltoClef;
import adris.altoclef.trackers.EntityTracker;
import adris.altoclef.trackers.TrackerManager;
import com.mojang.authlib.GameProfile;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import lavi.minecraft.find.observation.FindObservationPort.Binding;
import lavi.minecraft.testsupport.TestObjects;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.DummyProfiler;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.World;
import net.minecraft.world.Difficulty;

//20260914_kpopmodder: Exercise production entity reads with real vanilla entity state in a controlled headless world.
// This fixture never launches Minecraft or proves live-world acceptance.
final class FindEntityObservationFixture implements AutoCloseable {
    final TestClient client;
    final ObservedWorld world;
    final ClientPlayerEntity self;
    final TestMod mod;
    final CountingTracker tracker;
    private final Object previousClient;

    FindEntityObservationFixture() {
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
        // World/RegistryKeys must not initialize through a field initializer before Bootstrap.
        client = TestObjects.allocate(TestClient.class);
        world = world();
        self = TestObjects.allocate(ClientPlayerEntity.class);
        mod = TestObjects.allocate(TestMod.class);
        client.onThread = true;
        client.world = world;
        client.player = self;
        mod.client = client;
        tracker = new CountingTracker(new TrackerManager(mod));
        tracker.source = world;
        previousClient = singleton();
        singleton(client);
    }

    Binding binding() { return new Binding(world, self, "minecraft:overworld", 0, 0, 0, -64, 320); }

    VillagerEntity villager(double x, double y, double z) {
        return add(new VillagerEntity(EntityType.VILLAGER, world), x, y, z);
    }

    VillagerEntity subclassVillager(double x, double y, double z) {
        return add(new SpecializedVillager(world), x, y, z);
    }

    PlayerEntity player(String name, double x, double y, double z) {
        return add(new TestPlayer(world, name), x, y, z);
    }

    ItemEntity diamond(double x, double y, double z) {
        return add(new ItemEntity(world, x, y, z, new ItemStack(Items.DIAMOND)), x, y, z);
    }

    <T extends Entity> T add(T entity, double x, double y, double z) {
        entity.setPosition(x, y, z);
        world.members.add(entity);
        world.byId.put(entity.getId(), entity);
        return entity;
    }

    void members(List<Entity> entities) {
        world.members = new ArrayList<>(entities);
        world.byId.clear();
        for (Entity entity : entities) world.byId.put(entity.getId(), entity);
    }

    @Override public void close() { singleton(previousClient); }

    static ObservedWorld world() {
        ObservedWorld result = TestObjects.allocate(ObservedWorld.class);
        result.members = new ArrayList<>();
        result.byId = new HashMap<>();
        TestObjects.setField(result, World.class, "registryKey", World.OVERWORLD);
        TestObjects.setField(result, World.class, "properties", new ClientWorld.Properties(Difficulty.NORMAL, false, false));
        TestObjects.setField(result, World.class, "random", net.minecraft.util.math.random.Random.create(20260914L));
        TestObjects.setField(result, World.class, "profiler", (Supplier<Profiler>) () -> DummyProfiler.INSTANCE);
        try {
            Field field = World.class.getDeclaredField("isClient");
            field.setAccessible(true);
            field.setBoolean(result, true);
        } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
        return result;
    }

    private static Object singleton() {
        try {
            Field field = MinecraftClient.class.getDeclaredField("instance");
            field.setAccessible(true);
            return field.get(null);
        } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }

    private static void singleton(Object value) {
        try {
            Field field = MinecraftClient.class.getDeclaredField("instance");
            field.setAccessible(true);
            field.set(null, value);
        } catch (ReflectiveOperationException failure) { throw new AssertionError(failure); }
    }

    static final class TestClient extends MinecraftClient {
        boolean onThread;
        private TestClient() { super(null); }
        @Override public boolean isOnThread() { return onThread; }
    }

    static final class ObservedWorld extends ClientWorld {
        List<Entity> members;
        Map<Integer, Entity> byId;
        int sourceCalls, iteratorCalls, nextCalls, hasNextCalls;
        private ObservedWorld() { super(null, null, World.OVERWORLD, null, 0, 0,
                () -> DummyProfiler.INSTANCE, null, false, 0); }
        @Override public Iterable<Entity> getEntities() {
            sourceCalls++;
            return () -> {
                iteratorCalls++;
                Iterator<Entity> delegate = members.iterator();
                return new Iterator<>() {
                    @Override public boolean hasNext() { hasNextCalls++; return delegate.hasNext(); }
                    @Override public Entity next() { nextCalls++; return delegate.next(); }
                };
            };
        }
        @Override public Entity getEntityById(int id) { return byId.get(id); }
    }

    static final class TestMod extends AltoClef {
        TestClient client;
        @Override public ClientWorld getWorld() { return client.world; }
        @Override public ClientPlayerEntity getPlayer() { return client.player; }
    }

    static final class CountingTracker extends EntityTracker {
        ObservedWorld source;
        int getterCalls, refreshCalls, reachabilityCalls;
        CountingTracker(TrackerManager manager) { super(manager); }
        @Override public Iterable<Entity> getClientObservedEntities() { getterCalls++; return source.getEntities(); }
        @Override protected void updateState() { refreshCalls++; throw new AssertionError("FIND refreshed unrelated tracker caches"); }
        @Override public boolean isEntityReachable(Entity entity) { reachabilityCalls++; return false; }
    }

    private static final class SpecializedVillager extends VillagerEntity {
        SpecializedVillager(World world) { super(EntityType.VILLAGER, world); }
    }

    private static final class TestPlayer extends PlayerEntity {
        TestPlayer(World world, String name) { super(world, BlockPos.ORIGIN, 0,
                new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)), name)); }
        @Override public boolean isSpectator() { return false; }
        @Override public boolean isCreative() { return false; }
    }
}
//#endif
