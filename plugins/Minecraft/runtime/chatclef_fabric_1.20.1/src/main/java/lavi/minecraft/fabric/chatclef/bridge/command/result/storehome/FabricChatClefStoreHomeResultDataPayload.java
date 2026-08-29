package lavi.minecraft.fabric.chatclef.bridge.command.result.storehome;

import lavi.minecraft.fabric.chatclef.bridge.command.result.FabricChatClefCommandResultDataPayload;
import lavi.minecraft.task.container.home.result.StoreHomeOutcome;

import java.util.Map;
import java.util.Objects;

//20260827_kpopmodder: Decorate existing lifecycle data with one typed STORE_HOME outcome.
public final class FabricChatClefStoreHomeResultDataPayload
        implements FabricChatClefCommandResultDataPayload {
    private final FabricChatClefCommandResultDataPayload basePayload;
    private final StoreHomeOutcome outcome;

    FabricChatClefStoreHomeResultDataPayload(
            FabricChatClefCommandResultDataPayload basePayload,
            StoreHomeOutcome outcome
    ) {
        this.basePayload = basePayload == null
                ? FabricChatClefCommandResultDataPayload.empty()
                : basePayload;
        this.outcome = Objects.requireNonNull(outcome, "outcome");
    }

    @Override
    public Map<String, Object> toMap() {
        return FabricChatClefStoreHomeResultDataPayloadMap.toMap(basePayload, outcome);
    }
}
