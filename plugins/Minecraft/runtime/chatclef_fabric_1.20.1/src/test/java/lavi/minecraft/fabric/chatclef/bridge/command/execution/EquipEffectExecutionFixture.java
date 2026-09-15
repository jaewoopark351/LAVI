package lavi.minecraft.fabric.chatclef.bridge.command.execution;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskOwnershipEvidence;
import lavi.minecraft.fabric.chatclef.bridge.command.result.effect.FabricChatClefCommandEffectTracker;

//20260915_kpopmodder: Reach the existing package-private effect seam from test code only.
public final class EquipEffectExecutionFixture {
    private EquipEffectExecutionFixture() { }
    public static FabricChatClefCommandExecution create(FabricChatClefCommandContext context,
            String command, FabricChatClefCommandEffectTracker tracker) {
        return new FabricChatClefCommandExecution(context, command, FabricChatClefTaskOwnershipEvidence.empty(), ignored -> tracker);
    }
}
