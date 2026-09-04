package lavi.minecraft.diagnostics.container.gui.correlation;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.FurnaceScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.screen.FurnaceScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;

//20260904_kpopmodder: Keep the logging investigation limited to exact chest/trapped-chest/furnace targets.
public enum ContainerTargetFamily {
    CHEST("GenericContainerScreen", "GenericContainerScreenHandler"),
    FURNACE("FurnaceScreen", "FurnaceScreenHandler"),
    UNSUPPORTED("UNAVAILABLE", "UNAVAILABLE");

    private final String expectedScreenType;
    private final String expectedHandlerType;

    ContainerTargetFamily(String expectedScreenType, String expectedHandlerType) {
        this.expectedScreenType = expectedScreenType;
        this.expectedHandlerType = expectedHandlerType;
    }

    public static ContainerTargetFamily fromBlockId(String blockId) {
        if ("minecraft:chest".equals(blockId) || "minecraft:trapped_chest".equals(blockId)) {
            return CHEST;
        }
        if ("minecraft:furnace".equals(blockId)) {
            return FURNACE;
        }
        return UNSUPPORTED;
    }

    public boolean matches(Screen screen, ScreenHandler handler) {
        return switch (this) {
            case CHEST -> screen instanceof GenericContainerScreen
                    && handler instanceof GenericContainerScreenHandler;
            case FURNACE -> screen instanceof FurnaceScreen
                    && handler instanceof FurnaceScreenHandler;
            case UNSUPPORTED -> false;
        };
    }

    public String expectedScreenType() {
        return expectedScreenType;
    }

    public String expectedHandlerType() {
        return expectedHandlerType;
    }
}
