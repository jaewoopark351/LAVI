//#if MC == 12001
package lavi.minecraft.fabric.chatclef.bridge.command.result.gotoresult;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandResult;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultPayload;
import lavi.minecraft.task.movement.gotopreflight.GotoMaterialPlan.FailureReason;
import lavi.minecraft.task.movement.gotoresult.model.GotoTerminalSnapshot;

import java.util.Objects;

//20260913_kpopmodder: Project status and family evidence together after independent identity/STOP gates.
final class GotoCommandTerminalProjector {
    private GotoCommandTerminalProjector() { }

    static FabricChatClefCommandResultPayload project(FabricChatClefCommandResultDataPayload base,
                                                       GotoCommandBinding binding, GotoTerminalSnapshot outcome) {
        if (outcome == null) return null;
        FabricChatClefCommandResultDataPayload data = new GotoCommandResultDataPayload(base, binding, outcome);
        if ("ARRIVED".equals(outcome.outcome()) && "NONE".equals(outcome.failureReason())
                && outcome.goalSatisfied() && outcome.bindingValid() && outcome.childrenQuiescent()
                && ("prepared_goto_terminal".equals(outcome.evidenceKind())
                    || "legacy_get_to_block_terminal".equals(outcome.evidenceKind()))
                && Objects.equals(binding.target().worldDimension(), outcome.terminalDimension())) {
            return FabricChatClefCommandResult.completed(binding.requestId(), "GOTO task confirmed arrival.", data);
        }
        if ("FAILED".equals(outcome.outcome()) && !outcome.goalSatisfied()
                && knownFailure(outcome.failureReason()) && "prepared_goto_terminal".equals(outcome.evidenceKind())) {
            return FabricChatClefCommandResult.failed(binding.requestId(), "GOTO task confirmed failure.", data);
        }
        return FabricChatClefCommandResult.unknown(binding.requestId(), "GOTO terminal evidence was inconsistent.", data);
    }

    private static boolean knownFailure(String reason) {
        if (reason == null) return false;
        try { FailureReason.valueOf(reason); return true; }
        catch (IllegalArgumentException unknown) { return false; }
    }
}
//#endif
