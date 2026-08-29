package lavi.minecraft.fabric.chatclef.bridge.command.result.storehome;

import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;
import lavi.minecraft.task.container.home.result.StoreHomeOutcome;

import java.util.HashMap;
import java.util.Map;

//20260827_kpopmodder: Own STORE_HOME wire keys at the existing command_result.data edge.
public final class FabricChatClefStoreHomeResultDataPayloadMap {
    private static final String OPERATION = "operation";
    private static final String STORE_HOME_RESULT = "store_home_result";
    private static final String STORED_ITEMS = "stored_items";
    private static final String REMAINING_STACKS = "remaining_stacks";
    private static final String REASON = "reason";
    private static final String GOAL_SATISFIED = "goal_satisfied";

    private FabricChatClefStoreHomeResultDataPayloadMap() {
    }

    public static Map<String, Object> toMap(
            FabricChatClefCommandResultDataPayload basePayload,
            StoreHomeOutcome outcome
    ) {
        Map<String, Object> payload = new HashMap<>(basePayload.toMap());
        payload.put(OPERATION, "store_home");
        payload.put(STORE_HOME_RESULT, outcome.result().name());
        payload.put(STORED_ITEMS, outcome.storedItems());
        payload.put(REMAINING_STACKS, outcome.remainingStacks());
        payload.put(REASON, outcome.reason());
        payload.put(GOAL_SATISFIED, outcome.goalSatisfied());
        return payload;
    }
}
