package lavi.minecraft.diagnostics.container.gui.correlation;

import lavi.minecraft.testsupport.TestObjects;
import net.minecraft.client.gui.screen.ingame.FurnaceScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.screen.FurnaceScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260904_kpopmodder: Keep diagnostic correlation limited to the explicitly named container families.
class ContainerTargetFamilyTest {
    @Test
    void classifiesOnlyExactApprovedBlockIds() {
        assertEquals(ContainerTargetFamily.CHEST, ContainerTargetFamily.fromBlockId("minecraft:chest"));
        assertEquals(
                ContainerTargetFamily.CHEST,
                ContainerTargetFamily.fromBlockId("minecraft:trapped_chest")
        );
        assertEquals(ContainerTargetFamily.FURNACE, ContainerTargetFamily.fromBlockId("minecraft:furnace"));

        assertEquals(ContainerTargetFamily.UNSUPPORTED, ContainerTargetFamily.fromBlockId("minecraft:barrel"));
        assertEquals(
                ContainerTargetFamily.UNSUPPORTED,
                ContainerTargetFamily.fromBlockId("minecraft:shulker_box")
        );
        assertEquals(ContainerTargetFamily.UNSUPPORTED, ContainerTargetFamily.fromBlockId("minecraft:smoker"));
        assertEquals(
                ContainerTargetFamily.UNSUPPORTED,
                ContainerTargetFamily.fromBlockId("minecraft:blast_furnace")
        );
        assertEquals(ContainerTargetFamily.UNSUPPORTED, ContainerTargetFamily.fromBlockId(null));
    }

    @Test
    void requiresTheMatchingScreenAndHandlerPair() {
        GenericContainerScreen chestScreen = TestObjects.allocate(GenericContainerScreen.class);
        GenericContainerScreenHandler chestHandler = TestObjects.allocate(GenericContainerScreenHandler.class);
        FurnaceScreen furnaceScreen = TestObjects.allocate(FurnaceScreen.class);
        FurnaceScreenHandler furnaceHandler = TestObjects.allocate(FurnaceScreenHandler.class);

        assertTrue(ContainerTargetFamily.CHEST.matches(chestScreen, chestHandler));
        assertFalse(ContainerTargetFamily.CHEST.matches(chestScreen, furnaceHandler));
        assertFalse(ContainerTargetFamily.CHEST.matches(furnaceScreen, chestHandler));

        assertTrue(ContainerTargetFamily.FURNACE.matches(furnaceScreen, furnaceHandler));
        assertFalse(ContainerTargetFamily.FURNACE.matches(furnaceScreen, chestHandler));
        assertFalse(ContainerTargetFamily.FURNACE.matches(chestScreen, furnaceHandler));

        assertFalse(ContainerTargetFamily.UNSUPPORTED.matches(chestScreen, chestHandler));
        assertFalse(ContainerTargetFamily.CHEST.matches(null, chestHandler));
        assertFalse(ContainerTargetFamily.FURNACE.matches(furnaceScreen, null));
    }
}
