package lavi.minecraft.fabric.chatclef.bridge.command.result.effect;

import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.get.FabricChatClefGetItemEffectTracker;

//20260907_kpopmodder: Select a closed effect tracker without changing command execution behavior.
public final class FabricChatClefCommandEffectTrackerFactory {
    private FabricChatClefCommandEffectTrackerFactory() {
    }

    public static FabricChatClefCommandEffectTracker capture(String normalizedCommand) {
        FabricChatClefGetItemEffectTracker getTracker =
                FabricChatClefGetItemEffectTracker.capture(normalizedCommand);
        if (getTracker.tracked()) {
            return getTracker;
        }
        return FabricChatClefNoEffectTracker.instance();
    }
}
