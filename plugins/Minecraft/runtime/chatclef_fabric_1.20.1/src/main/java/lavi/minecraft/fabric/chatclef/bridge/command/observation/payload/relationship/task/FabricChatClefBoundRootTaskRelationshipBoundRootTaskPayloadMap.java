package lavi.minecraft.fabric.chatclef.bridge.command.observation.payload.relationship.task;

import lavi.minecraft.fabric.chatclef.bridge.command.observation.FabricChatClefTaskSnapshot;

import java.util.Map;

//20260809_kpopmodder: Keep bound-root task snapshots separate without changing emitted keys.
public final class FabricChatClefBoundRootTaskRelationshipBoundRootTaskPayloadMap {
    private static final String BOUND_ROOT_TASK = "bound_root_task";

    private FabricChatClefBoundRootTaskRelationshipBoundRootTaskPayloadMap() {
    }

    public static void writeTo(
            Map<String, Object> payload,
            FabricChatClefTaskSnapshot boundRootTask
    ) {
        payload.put(BOUND_ROOT_TASK, boundRootTask.toMap());
    }
}
