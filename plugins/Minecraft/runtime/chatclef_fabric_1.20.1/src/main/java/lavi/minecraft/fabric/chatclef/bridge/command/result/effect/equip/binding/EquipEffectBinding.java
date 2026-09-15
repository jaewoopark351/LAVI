package lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip.binding;

import lavi.minecraft.fabric.chatclef.bridge.command.FabricChatClefCommandContext;
import java.util.Map;
import java.util.Objects;

//20260915_kpopmodder: Reuse existing terminal proof and context fencing; this does not choose lifecycle outcomes.
public record EquipEffectBinding(String requestId, String sessionId, long serverGeneration, long socketGeneration) {
    public static EquipEffectBinding capture(FabricChatClefCommandContext context) {
        return context == null ? new EquipEffectBinding("", "", 0, 0)
                : new EquipEffectBinding(context.requestId(), context.sessionId(),
                    context.serverConnectionGeneration(), context.connectionGeneration());
    }
    public String validate(FabricChatClefCommandContext context, Map<String, Object> base, String command) {
        if (context == null || requestId.isEmpty() || sessionId.isEmpty()) return "terminal_binding_mismatch";
        if (Boolean.TRUE.equals(context.ownershipPayload().toMap().get("detached"))) return "context_detached";
        if (!equals(capture(context))) return "terminal_binding_mismatch";
        String actualCommand = context.request() == null || context.request().command == null ? "" : context.request().command.trim();
        if (actualCommand.startsWith("@")) actualCommand = actualCommand.substring(1);
        if (!Objects.equals(command, actualCommand)) return "terminal_binding_mismatch";
        if (!(base.get("ownership") instanceof Map<?, ?> ownership)
                || !Objects.equals(requestId, ownership.get("request_id"))
                || !Objects.equals(sessionId, ownership.get("session_id"))
                || !(ownership.get("connection_generation") instanceof Number generation)
                || generation.longValue() != serverGeneration || !Boolean.FALSE.equals(ownership.get("detached"))
                || !"matching_task_finished".equals(base.get("result_reason"))
                || !"callback_plus_matching_user_task_event".equals(base.get("result_fidelity"))
                || !Boolean.TRUE.equals(base.get("dispatch_returned"))
                || !Boolean.TRUE.equals(base.get("finish_callback_received"))
                || !Boolean.TRUE.equals(base.get("task_finished_event_received"))
                || !"".equals(base.get("failure_type"))) return "terminal_binding_mismatch";
        if (!(base.get("bound_root_task") instanceof Map<?, ?> task)
                || !Boolean.TRUE.equals(task.get("available"))
                || !"adris.altoclef.tasks.misc.EquipArmorTask".equals(task.get("class_name"))
                || !(task.get("identity") instanceof String identity) || !identity.matches("[0-9a-f]{1,16}"))
            return "terminal_binding_mismatch";
        return "valid";
    }
    public static String taskIdentity(Map<String, Object> base) {
        if (base.get("bound_root_task") instanceof Map<?, ?> task && task.get("identity") instanceof String identity
                && identity.matches("[0-9a-f]{1,16}")) return identity;
        return "";
    }
}
