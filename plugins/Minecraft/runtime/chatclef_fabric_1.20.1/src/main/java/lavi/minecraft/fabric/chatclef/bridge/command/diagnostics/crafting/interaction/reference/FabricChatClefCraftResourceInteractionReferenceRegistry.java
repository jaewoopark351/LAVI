package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.interaction.reference;

import lavi.minecraft.diagnostics.crafting.acquisition.scope.IronPickaxeAcquisitionScopeKey;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

//20260901_kpopmodder: Bound in-flight HEAD-to-RETURN diagnostic references to eight.
public final class FabricChatClefCraftResourceInteractionReferenceRegistry {
    private static final int ACTIVE_REFERENCE_LIMIT = 8;
    private final Map<Long, FabricChatClefCraftResourceInteractionAttemptReference>
            references = new LinkedHashMap<>();

    public synchronized boolean begin(
            FabricChatClefCraftResourceInteractionAttemptReference reference) {
        if (reference == null
                || references.containsKey(reference.interactionId())
                || references.size() >= ACTIVE_REFERENCE_LIMIT) {
            return false;
        }
        references.put(reference.interactionId(), reference);
        return true;
    }

    public synchronized Optional<FabricChatClefCraftResourceInteractionAttemptReference>
            complete(long interactionId) {
        return Optional.ofNullable(references.remove(interactionId));
    }

    public synchronized int retireScope(IronPickaxeAcquisitionScopeKey scopeKey) {
        if (scopeKey == null) {
            return 0;
        }
        int removed = 0;
        Iterator<FabricChatClefCraftResourceInteractionAttemptReference> iterator =
                references.values().iterator();
        while (iterator.hasNext()) {
            if (scopeKey.equals(iterator.next().scopeKey())) {
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }

    public synchronized void clearForModeOff() {
        references.clear();
    }

    public synchronized int size() {
        return references.size();
    }
}
