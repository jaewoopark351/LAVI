//#if MC == 12001
package lavi.minecraft.fabric.chatclef.bridge.command.result.gotoresult;

import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;
import lavi.minecraft.task.movement.gotoresult.model.GotoTerminalSnapshot;

import java.util.LinkedHashMap;
import java.util.Map;

//20260913_kpopmodder: Versioned optional evidence uses the existing command-result data extension boundary.
public final class GotoCommandResultDataPayload implements FabricChatClefCommandResultDataPayload {
    private final FabricChatClefCommandResultDataPayload base;
    private final GotoCommandBinding binding;
    private final GotoTerminalSnapshot terminal;

    public GotoCommandResultDataPayload(FabricChatClefCommandResultDataPayload base,
                                        GotoCommandBinding binding, GotoTerminalSnapshot terminal) {
        this.base = base;
        this.binding = binding;
        this.terminal = terminal;
    }

    @Override public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>(base.toMap());
        result.put("goto_profile_id", "fabric_chatclef_goto_terminal");
        result.put("goto_profile_version", 1);
        Map<String, Object> evidence = GotoCommandBindingPayload.toMap(binding);
        if (terminal == null) {
            result.put("goto_binding", evidence);
        } else {
            evidence.put("outcome", terminal.outcome());
            evidence.put("failure_reason", terminal.failureReason());
            evidence.put("goal_satisfied", terminal.goalSatisfied());
            evidence.put("binding_valid", terminal.bindingValid());
            evidence.put("children_quiescent", terminal.childrenQuiescent());
            evidence.put("evidence_kind", terminal.evidenceKind());
            evidence.put("terminal_dimension", terminal.terminalDimension());
            result.put("goto_terminal", evidence);
        }
        return result;
    }
}
//#endif
