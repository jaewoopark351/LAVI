package lavi.minecraft.fabric.chatclef.bridge.catalogue;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import lavi.minecraft.fabric.chatclef.bridge.catalogue.capability.FabricChatClefItemCommandTokens;
import lavi.minecraft.fabric.chatclef.bridge.catalogue.registry.FabricChatClefRegistryEntryReader;
import lavi.minecraft.testsupport.HeadlessMinecraftClientSession;
import lavi.minecraft.testsupport.TestObjects;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class FabricChatClefRegistryCatalogueTest {
    @BeforeAll static void bootstrap() { SharedConstants.createGameVersion(); Bootstrap.initialize(); }

    @Test void actualRegistryProjectionCoversEveryItemBlockAndEntityWithoutFabricatingNames() throws Exception {
        Map<String, String> language = new HashMap<>();
        String languageFile = System.getProperty("allCommands.koreanLanguageFile");
        if (languageFile != null) {
            try (var reader = Files.newBufferedReader(Path.of(languageFile))) {
                for (var value : JsonParser.parseReader(reader).getAsJsonObject().entrySet())
                    if (value.getValue().isJsonPrimitive() && value.getValue().getAsJsonPrimitive().isString())
                        language.put(value.getKey(), value.getValue().getAsString());
            }
        }
        try (var client = HeadlessMinecraftClientSession.inGame()) {
            var entries = new FabricChatClefRegistryEntryReader().read(language);
            assertEquals(Registries.ITEM.size() + Registries.BLOCK.size() + Registries.ENTITY_TYPE.size(), entries.size());
            assertEquals(entries.size(), entries.stream().map(entry -> entry.get("kind") + ":" + entry.get("id")).distinct().count());
            for (var entry : entries) assertEquals(language.get(entry.get("translation_key")), entry.get("korean_name"));
            var zombie = entries.stream().filter(entry -> entry.get("id").equals("minecraft:zombie") && entry.get("kind").equals("entity")).findFirst().orElseThrow();
            assertEquals("zombie", ((Map<?, ?>) zombie.get("tokens")).get("attack"));
            var player = entries.stream().filter(entry -> entry.get("id").equals("minecraft:player") && entry.get("kind").equals("entity")).findFirst().orElseThrow();
            assertFalse(((Map<?, ?>) player.get("tokens")).containsKey("attack"));
            var stone = entries.stream().filter(entry -> entry.get("id").equals("minecraft:stone") && entry.get("kind").equals("block")).findFirst().orElseThrow();
            String scanToken = (String) ((Map<?, ?>) stone.get("tokens")).get("scan");
            assertSame(Blocks.STONE, Blocks.class.getDeclaredField(scanToken).get(null));
            String export = System.getProperty("allCommands.catalogueOutput");
            if (export != null) {
                Map<String, Object> document = new LinkedHashMap<>();
                document.put("schema_version", 1);
                document.put("minecraft_version", "1.20.1");
                document.put("entries", entries);
                document.put("butler_user", null);
                document.put("registered_commands", List.of());
                document.put("verification_scope", "isolated_vanilla_registry_not_modded_client");
                Files.writeString(Path.of(export), new GsonBuilder().serializeNulls().create().toJson(document));
                System.out.println("ALL_COMMANDS_VANILLA_CATALOGUE entries=" + entries.size() + " translated="
                        + entries.stream().filter(entry -> entry.get("korean_name") != null).count() + " output=" + export);
            }
        }
    }

    @Test void equipMatchesActualArmorTaskAndGivePreservesModTranslationToken() {
        try (var client = HeadlessMinecraftClientSession.inGame()) {
            var tokens = new FabricChatClefItemCommandTokens();
            assertTrue(tokens.tokens(Items.DIAMOND_CHESTPLATE, "diamond_chestplate").containsKey("equip"));
            assertTrue(tokens.tokens(Items.SHIELD, "shield").containsKey("equip"));
            for (Item item : List.of(Items.ELYTRA, Items.CARVED_PUMPKIN, Items.PLAYER_HEAD))
                assertFalse(tokens.tokens(item, Registries.ITEM.getId(item).getPath()).containsKey("equip"));
            Item modItem = TestObjects.allocate(ModItem.class);
            assertEquals("item.fixture.verifiable_widget", tokens.tokens(modItem, "verifiable_widget").get("give"));
            assertFalse(tokens.tokens(modItem, "verifiable_widget").containsKey("get"));
        }
    }

    private static final class ModItem extends Item {
        private ModItem() { super(new Item.Settings()); }
        @Override public String getTranslationKey() { return "item.fixture.verifiable_widget"; }
    }
}
