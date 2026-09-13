//#if MC == 12001
package lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.root.payload;

import lavi.minecraft.fabric.chatclef.bridge.command.lifecycle.root.state.RootTerminationState;
import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;
import java.util.LinkedHashMap;
import java.util.Map;

//20260913_kpopmodder: Snapshot result evidence at the payload edge without retaining mutable execution state.
public final class RootRetirementPayload {
    private RootRetirementPayload() { }
    public static FabricChatClefCommandResultDataPayload attach(FabricChatClefCommandResultDataPayload base, RootTerminationState state) {
        Map<String, Object> result = new LinkedHashMap<>(base.toMap());
        result.put("root_retirement", fields(state));
        return FabricChatClefCommandResultDataPayload.fromMap(result);
    }
    public static Map<String, Object> fields(RootTerminationState state) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("reason", state.reason());
        values.put("pending_completion_through", state.pendingThrough());
        values.put("completion_prefix_pending", state.pending());
        values.put("owner_completion_observed", state.completion() != null);
        values.put("old_root_identity", identity(state.boundSnapshot() == null ? null : state.boundSnapshot().root()));
        values.put("new_root_identity", identity(state.retirementSnapshot() == null ? null : state.retirementSnapshot().root()));
        values.put("old_lifetime_identity", identity(state.boundSnapshot() == null ? null : state.boundSnapshot().lifetime()));
        values.put("new_lifetime_identity", identity(state.retirementSnapshot() == null ? null : state.retirementSnapshot().lifetime()));
        return Map.copyOf(values);
    }
    private static String identity(Object value) { return value == null ? "none" : Integer.toHexString(System.identityHashCode(value)); }
}
//#endif
