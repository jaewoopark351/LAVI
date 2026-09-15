package lavi.minecraft.fabric.chatclef.bridge.catalogue;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.Command;
import adris.altoclef.commandsystem.CommandExecutor;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import lavi.minecraft.testsupport.HeadlessMinecraftClientSession;
import lavi.minecraft.testsupport.TestObjects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ReloadableResourceManagerImpl;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

class FabricChatClefCatalogueCaptureTest {
    @Test void tickRegisteredCommandsRefreshContextWithoutReReadingRegistryOrLanguage() throws Exception {
        Field modField = field(AltoClef.class, "instance");
        Field executorField = field(AltoClef.class, "commandExecutor");
        Object priorMod = modField.get(null), priorExecutor = executorField.get(null);
        var priorSnapshot = FabricChatClefCatalogueSnapshotStore.current();
        try (var session = HeadlessMinecraftClientSession.inGame()) {
            AltoClef mod = TestObjects.allocate(AltoClef.class);
            CommandExecutor executor = new CommandExecutor(mod);
            modField.set(null, mod);
            executorField.set(null, executor);
            TestObjects.setField(MinecraftClient.getInstance(), MinecraftClient.class, "resourceManager",
                    TestObjects.allocate(ReloadableResourceManagerImpl.class));
            var capture = new FabricChatClefCatalogueCapture(new FabricChatClefBridgeDiagnostics());
            var entries = List.of(Map.<String, Object>of("kind", "item", "id", "minecraft:stone"));
            field(FabricChatClefCatalogueCapture.class, "entries").set(capture, entries);
            field(FabricChatClefCatalogueCapture.class, "butlerUser").set(capture, "");
            ((AtomicBoolean) field(FabricChatClefCatalogueCapture.class, "dirty").get(capture)).set(false);
            executor.registerNewCommand(new Command("overlay", "fixture") {
                @Override protected void call(AltoClef ignored, ArgParser parser) { fail("No command may execute during metadata capture"); }
            });
            capture.captureIfChanged(MinecraftClient.getInstance());
            var first = FabricChatClefCatalogueSnapshotStore.current();
            assertEquals(true, first.wireValue().get("available"));
            assertEquals(List.of("overlay"), field(FabricChatClefCatalogueCapture.class, "commands").get(capture));
            assertSame(entries, field(FabricChatClefCatalogueCapture.class, "entries").get(capture));
            capture.captureIfChanged(MinecraftClient.getInstance());
            assertSame(first, FabricChatClefCatalogueSnapshotStore.current());
            executor.registerNewCommand(new Command("store_home", "fixture") {
                @Override protected void call(AltoClef ignored, ArgParser parser) { fail("No command may execute during metadata capture"); }
            });
            capture.captureIfChanged(MinecraftClient.getInstance());
            assertNotEquals(first.revision(), FabricChatClefCatalogueSnapshotStore.current().revision());
            assertSame(entries, field(FabricChatClefCatalogueCapture.class, "entries").get(capture));
            capture.invalidate();
            assertEquals(false, FabricChatClefCatalogueSnapshotStore.current().wireValue().get("available"));
        } finally {
            modField.set(null, priorMod);
            executorField.set(null, priorExecutor);
            FabricChatClefCatalogueSnapshotStore.publish(priorSnapshot);
        }
    }
    private static Field field(Class<?> owner, String name) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
}
