package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.relationship;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;

import java.util.Map;

//20260808_kpopmodder: Split bound-root relationship task snapshots without changing emitted keys.
public final class FabricChatClefBoundRootTaskRelationshipTaskPayloadMap {
    private static final String BOUND_ROOT_TASK = "bound_root_task";

    private FabricChatClefBoundRootTaskRelationshipTaskPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            String candidateName,
            FabricChatClefTaskSnapshot candidateTask,
            FabricChatClefTaskSnapshot boundRootTask
    ) {
        payload.put(candidateName, candidateTask.toMap());
        payload.put(BOUND_ROOT_TASK, boundRootTask.toMap());
    }
}
