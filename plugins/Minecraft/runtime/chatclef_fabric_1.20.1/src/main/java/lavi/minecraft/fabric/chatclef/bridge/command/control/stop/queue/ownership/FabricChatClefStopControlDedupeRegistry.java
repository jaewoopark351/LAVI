package lavi.minecraft.fabric.chatclef.bridge.command.control.stop.queue.ownership;

//20260905_kpopmodder: Own the non-evicting 4096-entry STOP replay registry for one bridge graph.

import java.util.LinkedHashMap;
import java.util.Map;

public final class FabricChatClefStopControlDedupeRegistry {
    public static final int MAX_IDENTITIES = 4096;

    private final Map<FabricChatClefStopControlIdentity, FabricChatClefStopControlDedupeEntry> entries =
            new LinkedHashMap<>();
    private long acceptedServerGeneration;

    public synchronized void selectAcceptedGeneration(long serverConnectionGeneration) {
        if (serverConnectionGeneration <= 0L || acceptedServerGeneration == serverConnectionGeneration) {
            return;
        }
        entries.clear();
        acceptedServerGeneration = serverConnectionGeneration;
    }

    public synchronized FabricChatClefStopControlDedupeDecision reserve(
            FabricChatClefStopControlIdentity identity,
            String fingerprint
    ) {
        FabricChatClefStopControlDedupeEntry existing = entries.get(identity);
        if (existing != null) {
            if (!existing.matchesFingerprint(fingerprint)) {
                return FabricChatClefStopControlDedupeDecision.DUPLICATE_PAYLOAD_MISMATCH;
            }
            return existing.terminal()
                    ? FabricChatClefStopControlDedupeDecision.DUPLICATE_TOMBSTONED
                    : FabricChatClefStopControlDedupeDecision.DUPLICATE_LIVE;
        }
        if (entries.size() >= MAX_IDENTITIES) {
            return FabricChatClefStopControlDedupeDecision.CAPACITY_EXHAUSTED;
        }
        entries.put(identity, new FabricChatClefStopControlDedupeEntry(fingerprint));
        return FabricChatClefStopControlDedupeDecision.RESERVED;
    }

    public synchronized boolean markTerminal(FabricChatClefStopControlIdentity identity) {
        FabricChatClefStopControlDedupeEntry entry = entries.get(identity);
        if (entry == null || entry.terminal()) {
            return false;
        }
        entry.markTerminal();
        return true;
    }

    public synchronized int size() {
        return entries.size();
    }

    public synchronized void resetForShutdown() {
        entries.clear();
        acceptedServerGeneration = 0L;
    }
}
