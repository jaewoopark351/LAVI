package lavi.minecraft.fabric.chatclef.bridge.command.result.effect.get;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260907_kpopmodder: Keep Java catalogue match sets inside the Python verifier's closed bounds.
class FabricChatClefGetItemCatalogueResolutionTest {
    @Test
    void acceptsExactlyTheCrossLanguageMatchIdLimit() {
        FabricChatClefGetItemCatalogueResolution resolution =
                FabricChatClefGetItemCatalogueResolution.resolved(generatedIds(2048));

        assertTrue(resolution.resolved());
        assertEquals(2048, resolution.targetMatchIds().size());
        assertEquals("minecraft:generated_0000", resolution.targetMatchIds().get(0));
        assertEquals("minecraft:generated_2047", resolution.targetMatchIds().get(2047));
    }

    @Test
    void rejectsMatchSetsPastTheCrossLanguageLimit() {
        assertFalse(
                FabricChatClefGetItemCatalogueResolution.resolved(generatedIds(2049))
                        .resolved()
        );
    }

    @Test
    void canonicalizesDynamicGroupMatchesAndRejectsNonVanillaWireIds() {
        FabricChatClefGetItemCatalogueResolution resolution =
                FabricChatClefGetItemCatalogueResolution.resolved(
                        List.of(
                                "minecraft:spruce_log",
                                "minecraft:oak_log",
                                "minecraft:oak_log"
                        )
                );

        assertTrue(resolution.resolved());
        assertEquals(
                List.of("minecraft:oak_log", "minecraft:spruce_log"),
                resolution.targetMatchIds()
        );
        assertFalse(
                FabricChatClefGetItemCatalogueResolution.resolved(
                        List.of("example:modded_log")
                ).resolved()
        );
    }

    private static List<String> generatedIds(int count) {
        List<String> ids = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            ids.add(String.format("minecraft:generated_%04d", index));
        }
        return ids;
    }
}
