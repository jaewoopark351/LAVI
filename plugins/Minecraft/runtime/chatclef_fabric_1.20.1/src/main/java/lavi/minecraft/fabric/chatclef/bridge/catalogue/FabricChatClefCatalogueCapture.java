//#if MC == 12001
package lavi.minecraft.fabric.chatclef.bridge.catalogue;

import adris.altoclef.AltoClef;
import lavi.minecraft.fabric.chatclef.bridge.catalogue.encoding.FabricChatClefCatalogueEncoder;
import lavi.minecraft.fabric.chatclef.bridge.catalogue.names.FabricChatClefKoreanLanguageReader;
import lavi.minecraft.fabric.chatclef.bridge.catalogue.registry.FabricChatClefRegistryEntryReader;
import lavi.minecraft.fabric.chatclef.bridge.diagnostics.FabricChatClefBridgeDiagnostics;
import net.minecraft.client.MinecraftClient;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

//20260915_kpopmodder: Main-thread capture; resource callbacks only invalidate immutable catalogue data.
public final class FabricChatClefCatalogueCapture {
    private final AtomicBoolean dirty = new AtomicBoolean(true);
    private final FabricChatClefBridgeDiagnostics diagnostics;
    private List<Map<String, Object>> entries;
    private List<String> commands = List.of();
    private String butlerUser;
    private Map<String, Object> languageCoverage = Map.of();

    public FabricChatClefCatalogueCapture(FabricChatClefBridgeDiagnostics diagnostics) {
        this.diagnostics = diagnostics;
    }

    public synchronized void invalidate() {
        FabricChatClefCatalogueSnapshotStore.publish(FabricChatClefCatalogueSnapshot.unavailable("resource_reload_pending"));
        dirty.set(true);
    }

    public synchronized void captureIfChanged(MinecraftClient client) {
        var mod = AltoClef.getInstance();
        if (mod == null || AltoClef.getCommandExecutor() == null || client.getResourceManager() == null) return;
        String currentUser = mod.getButler() != null && mod.getButler().hasCurrentUser()
                ? mod.getButler().getCurrentUser() : "";
        // Existing overlay/storage registrars run on ticks after CLIENT_STARTED.
        List<String> currentCommands = AltoClef.getCommandExecutor().allCommands().stream()
                .map(command -> command.getName()).sorted().toList();
        boolean refresh = dirty.getAndSet(false);
        if (!refresh && Objects.equals(butlerUser, currentUser) && commands.equals(currentCommands)) return;
        try {
            if (refresh || entries == null) {
                var language = new FabricChatClefKoreanLanguageReader().read(client.getResourceManager());
                entries = new FabricChatClefRegistryEntryReader().read(language.names());
                languageCoverage = Map.of("resource_count", language.resourceCount(), "failed_resource_count", language.failedCount(),
                        "translated_entries", entries.stream().filter(entry -> entry.get("korean_name") != null).count());
            }
            commands = currentCommands;
            butlerUser = currentUser;
            Map<String, Object> document = new LinkedHashMap<>();
            document.put("schema_version", 1);
            document.put("minecraft_version", "1.20.1");
            document.put("entries", entries);
            document.put("registered_commands", commands);
            document.put("butler_user", currentUser.isEmpty() ? null : currentUser);
            document.put("language_coverage", languageCoverage);
            FabricChatClefCatalogueSnapshot snapshot = new FabricChatClefCatalogueEncoder().encode(document, entries.size());
            FabricChatClefCatalogueSnapshotStore.publish(snapshot);
            try {
                diagnostics.info("catalogue_capture revision=" + snapshot.revision() + " entries=" + entries.size()
                        + " commands=" + commands.size() + " refresh=" + refresh + " language=" + languageCoverage);
            } catch (RuntimeException ignored) { /* diagnostics cannot invalidate a valid capture */ }
        } catch (Exception error) {
            FabricChatClefCatalogueSnapshotStore.publish(FabricChatClefCatalogueSnapshot.unavailable("capture_failed"));
            try { diagnostics.warn("catalogue_capture result=unavailable reason=capture_failed"); }
            catch (RuntimeException ignored) { /* retain the actual capture decision */ }
        }
    }
}
//#endif
