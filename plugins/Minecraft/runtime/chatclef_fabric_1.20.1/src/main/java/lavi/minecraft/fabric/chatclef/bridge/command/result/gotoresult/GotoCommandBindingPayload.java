//#if MC == 12001
package lavi.minecraft.fabric.chatclef.bridge.command.result.gotoresult;

import java.util.LinkedHashMap;
import java.util.Map;

//20260913_kpopmodder: Keep the running and terminal identity serialization identical.
final class GotoCommandBindingPayload {
    private GotoCommandBindingPayload() { }

    static Map<String, Object> toMap(GotoCommandBinding value) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("request_id", value.requestId());
        result.put("command_message_id", value.commandMessageId());
        result.put("session_id", value.sessionId());
        result.put("server_connection_generation", value.serverConnectionGeneration());
        result.put("java_socket_generation", value.javaSocketGeneration());
        result.put("task_owner", value.taskOwner());
        result.put("task_identity", value.taskIdentity());
        result.put("operation_id", value.operationId());
        result.put("request_shape", "XYZ");
        result.put("target_x", value.target().x());
        result.put("target_y", value.target().y());
        result.put("target_z", value.target().z());
        result.put("requested_dimension", value.target().requestedDimension());
        result.put("world_dimension", value.target().worldDimension());
        return result;
    }
}
//#endif
