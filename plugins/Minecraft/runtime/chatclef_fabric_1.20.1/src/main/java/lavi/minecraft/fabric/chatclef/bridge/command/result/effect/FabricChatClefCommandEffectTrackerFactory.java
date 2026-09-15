package lavi.minecraft.fabric.chatclef.bridge.command.result.effect;

import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.get.FabricChatClefGetItemEffectTracker;

//20260907_kpopmodder: Select a closed effect tracker without changing command execution behavior.
public final class FabricChatClefCommandEffectTrackerFactory {
    private FabricChatClefCommandEffectTrackerFactory() {
    }

    public static FabricChatClefCommandEffectTracker capture(String normalizedCommand) {
        return capture(normalizedCommand, null);
    }

    //20260915_kpopmodder: Bind EQUIP observations to the existing request/session without changing GET ownership.
    public static FabricChatClefCommandEffectTracker capture(String normalizedCommand,
            lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext context) {
        String effectCommand = normalizedCommand == null ? "" : normalizedCommand.trim();
        if (effectCommand.startsWith("@")) effectCommand = effectCommand.substring(1);
        if (effectCommand.equals("equip") || effectCommand.startsWith("equip ")) {
            return lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip.EquipEffectTracker.capture(effectCommand, context);
        }
        FabricChatClefGetItemEffectTracker getTracker =
                FabricChatClefGetItemEffectTracker.capture(normalizedCommand);
        if (getTracker.tracked()) {
            return getTracker;
        }
        return FabricChatClefNoEffectTracker.instance();
    }
}
