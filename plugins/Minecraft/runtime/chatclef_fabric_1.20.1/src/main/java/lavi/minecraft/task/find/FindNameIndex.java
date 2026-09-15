//#if MC == 12001
//20260915_kpopmodder: Resolve exact runtime IDs; Python owns human names, aliases and normalization.
package lavi.minecraft.task.find;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class FindNameIndex {
    public record Entry(String kind, String id, String label, String placedBlockId) { }
    public record Resolution(List<Entry> candidates) {
        public Resolution { candidates = List.copyOf(candidates); }
        public Entry unique() { return candidates.size() == 1 ? candidates.get(0) : null; }
    }
    private final Map<String, LinkedHashSet<Entry>> ids = new LinkedHashMap<>();

    public void add(Entry entry) {
        ids.computeIfAbsent(entry.id(), ignored -> new LinkedHashSet<>()).add(entry);
    }

    public Resolution resolve(FindRequest request) {
        String id = request.query().contains(":") ? request.query() : "minecraft:" + request.query();
        var matches = new ArrayList<Entry>();
        for (Entry entry : ids.getOrDefault(id, new LinkedHashSet<>())) {
            if (request.kind().equals("auto") || request.kind().equals(entry.kind())) matches.add(entry);
        }
        if (request.kind().equals("auto")) {
            var blocks = matches.stream().filter(e -> e.kind().equals("block")).map(Entry::id).toList();
            matches.removeIf(e -> e.kind().equals("item") && blocks.contains(e.placedBlockId()));
        }
        matches.sort(Comparator.comparing(Entry::kind).thenComparing(Entry::id));
        return new Resolution(matches);
    }
}
//#endif
