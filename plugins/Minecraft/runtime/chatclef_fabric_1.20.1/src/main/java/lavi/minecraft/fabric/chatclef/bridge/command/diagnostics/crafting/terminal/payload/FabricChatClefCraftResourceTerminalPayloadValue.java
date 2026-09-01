package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.terminal.payload;

import java.util.OptionalInt;

//20260901_kpopmodder: Normalize unavailable terminal scalars without reevaluating gameplay.
public final class FabricChatClefCraftResourceTerminalPayloadValue {
    private FabricChatClefCraftResourceTerminalPayloadValue() {
    }

    public static Object value(OptionalInt value) {
        return value != null && value.isPresent() ? value.getAsInt() : "UNAVAILABLE";
    }

    public static Object unavailableLong(long value) {
        return value < 0L ? "UNAVAILABLE" : value;
    }

    public static String unavailableIfBlank(String value) {
        return value == null || value.isBlank() ? "UNAVAILABLE" : value;
    }
}
