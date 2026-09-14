package lavi.minecraft.fabric.chatclef.bridge.command.admission.find;

import adris.altoclef.commandsystem.CommandException;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandRequest;
import lavi.minecraft.find.catalog.FindCatalogSnapshot;
import lavi.minecraft.find.catalog.FindCatalogRecord;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

//20260914_kpopmodder: Stale translated bindings must fail before engine invocation, including custom prefixes.
class FabricChatClefFindAdmissionBindingGuardTest {
    private static final FindCatalogSnapshot CATALOG = FindCatalogSnapshot.create(7,
            List.of(new FindCatalogRecord("block", "minecraft:stone", "block.minecraft.stone", "돌", "Stone", "")));
    private static FabricChatClefCommandContext context(Object resourceGeneration, String session) {
        var request = new FabricChatClefCommandRequest(); request.requestId = "test-request";
        request.metadata = Map.of("natural_language", Map.of("translation", Map.of(
                "intent", Map.of("intent_type", "find"), "data", Map.of(
                        "find_catalog_digest", CATALOG.digest(), "find_resource_generation", resourceGeneration,
                        "find_session_id", session, "find_connection_generation", 41L))));
        return new FabricChatClefCommandContext(request, "correlation", "accepted-session", 41L, 3L);
    }
    @Test void acceptedBindingUsesServerGenerationWithConfiguredPrefix() {
        assertDoesNotThrow(() -> FabricChatClefFindAdmissionBindingGuard.validate(
                "!!find block minecraft:stone report", "!!", context(7L, "accepted-session"), CATALOG));
    }
    @Test void staleOrNonIntegralBindingsRejectIncludingCustomPrefix() {
        assertThrows(CommandException.class, () -> FabricChatClefFindAdmissionBindingGuard.validate(
                "!!find block minecraft:stone", "!!", context(6L, "accepted-session"), CATALOG));
        assertThrows(CommandException.class, () -> FabricChatClefFindAdmissionBindingGuard.validate(
                "@find block minecraft:stone", "@", context(7.0, "accepted-session"), CATALOG));
        assertThrows(CommandException.class, () -> FabricChatClefFindAdmissionBindingGuard.validate(
                "@find block minecraft:stone", "@", context(7L, "replaced-session"), CATALOG));
        assertThrows(CommandException.class, () -> FabricChatClefFindAdmissionBindingGuard.validate(
                "@find block minecraft:stone", "@", context(7L, "accepted-session"), null));
    }
    @Test void PlayerAndUninterpretedCanonicalCommandsDoNotRequireResourceCatalog() {
        assertDoesNotThrow(() -> FabricChatClefFindAdmissionBindingGuard.validate(
                "@find player ExamplePlayer", "@", context(0L, "stale"), null));
        var request = new FabricChatClefCommandRequest();
        var nativeContext = new FabricChatClefCommandContext(request, "correlation", "accepted-session", 41L);
        assertDoesNotThrow(() -> FabricChatClefFindAdmissionBindingGuard.validate(
                "@find block minecraft:stone", "@", nativeContext, null));
        assertDoesNotThrow(() -> FabricChatClefFindAdmissionBindingGuard.validate(
                "@goto 1 2 3", "@", context(0L, "stale"), null));
    }
}
