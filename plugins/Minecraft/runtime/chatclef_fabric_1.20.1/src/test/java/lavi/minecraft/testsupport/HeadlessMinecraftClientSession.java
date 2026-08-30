package lavi.minecraft.testsupport;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;

import java.lang.reflect.Field;

//20260829_kpopmodder: Provide a bounded headless client session for TaskRunner lifecycle tests.
public final class HeadlessMinecraftClientSession implements AutoCloseable {
    private final Object previousClient;

    private HeadlessMinecraftClientSession(boolean inGame) {
        previousClient = readInstance();
        TestMinecraftClient client = TestObjects.allocate(TestMinecraftClient.class);
        if (inGame) {
            Object playerSentinel = new Object();
            ClientPlayNetworkHandler networkHandler =
                    TestObjects.allocate(ClientPlayNetworkHandler.class);
            TestObjects.setField(client, MinecraftClient.class, "player", playerSentinel);
            TestObjects.setField(
                    client,
                    TestMinecraftClient.class,
                    "networkHandler",
                    networkHandler
            );
        }
        writeInstance(client);
    }

    public static HeadlessMinecraftClientSession inGame() {
        return new HeadlessMinecraftClientSession(true);
    }

    public static HeadlessMinecraftClientSession outOfGame() {
        return new HeadlessMinecraftClientSession(false);
    }

    @Override
    public void close() {
        writeInstance(previousClient);
    }

    private static Object readInstance() {
        try {
            Field field = MinecraftClient.class.getDeclaredField("instance");
            field.setAccessible(true);
            return field.get(null);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to read MinecraftClient.instance", exception);
        }
    }

    private static void writeInstance(Object value) {
        try {
            Field field = MinecraftClient.class.getDeclaredField("instance");
            field.setAccessible(true);
            field.set(null, value);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to write MinecraftClient.instance", exception);
        }
    }

    private static final class TestMinecraftClient extends MinecraftClient {
        private ClientPlayNetworkHandler networkHandler;

        private TestMinecraftClient() {
            super(null);
        }

        @Override
        public ClientPlayNetworkHandler getNetworkHandler() {
            return networkHandler;
        }
    }
}
