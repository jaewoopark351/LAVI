package lavi.minecraft.diagnostics.observation.state;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//20260913_kpopmodder: Own bounded first evidence, pins, and event-specific recent-history fingerprints.
public final class ObservationEvidenceMemory {
    private final int firstLimit;
    private final Map<String, String> first = new LinkedHashMap<>();
    private final Map<String, String> pins = new LinkedHashMap<>();
    private final Map<String, String> fingerprints = new LinkedHashMap<>();
    private final ArrayDeque<String> recent = new ArrayDeque<>();
    private long firstOverflow;

    public ObservationEvidenceMemory(int firstLimit) {
        this.firstLimit = firstLimit;
    }

    public void pin(String slot, String frozen) {
        if (pins.containsKey(slot)) return;
        if (pins.size() < 4) pins.put(slot, frozen);
    }

    public boolean remember(String event, String reason, String fingerprint, boolean terminal, String snapshot) {
        String signature = event + ":" + reason;
        String meaning = reason + ":" + fingerprint;
        boolean knownEvent = fingerprints.containsKey(event);
        boolean changed = (knownEvent || fingerprints.size() < firstLimit)
                && !meaning.equals(fingerprints.get(event));
        if (changed) {
            if (recent.size() == 32) recent.removeFirst();
            recent.addLast(snapshot);
            fingerprints.put(event, meaning);
        }
        boolean newFirst = !terminal && !first.containsKey(signature) && first.size() < firstLimit;
        if (newFirst) first.put(signature, snapshot);
        else if (!terminal && !first.containsKey(signature)) firstOverflow++;
        return newFirst;
    }

    public int firstCount() { return first.size(); }
    public long firstOverflow() { return firstOverflow; }
    public int recentCount() { return recent.size(); }
    public List<String> recentTransitions() { return List.copyOf(recent); }
    public Map<String, String> pins() { return new LinkedHashMap<>(pins); }
}
