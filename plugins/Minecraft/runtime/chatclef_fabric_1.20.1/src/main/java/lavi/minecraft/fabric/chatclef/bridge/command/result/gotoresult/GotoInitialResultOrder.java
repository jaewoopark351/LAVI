//#if MC == 12001
package lavi.minecraft.fabric.chatclef.bridge.command.result.gotoresult;

import adris.altoclef.AltoClef;
import lavi.minecraft.task.movement.gotoresult.binding.GotoTaskBinding;
import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;

//20260913_kpopmodder: Bind the supported GOTO task into its single initial running frame, avoiding concurrent sendText.
public final class GotoInitialResultOrder {
    private GotoInitialResultOrder() { }

    public static boolean afterTaskAdmission(String command) {
        AltoClef mod = AltoClef.getInstance();
        return mod != null && mod.getPlayer() != null
                && GotoCommandTargetMatcher.sameDimensionDirectXyz(command, GotoTaskBinding.dimension(mod));
    }

    public static boolean publishAfterAdmission(boolean deferred, FabricChatClefCommandContext context) {
        return deferred && !context.terminalPayloadCommitted();
    }
}
//#endif
