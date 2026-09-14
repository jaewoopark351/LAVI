//#if MC == 12001
//20260914_kpopmodder: Resolve exact runtime aliases without default-registry fallback or fuzzy guessing.
package lavi.minecraft.task.find;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class FindNameIndex {
    public record Entry(String kind, String id, String label, String placedBlockId) { }
    public record Resolution(List<Entry> candidates) {
        public Resolution { candidates = List.copyOf(candidates); }
        public Entry unique() { return candidates.size() == 1 ? candidates.get(0) : null; }
    }
    private final Map<String, LinkedHashSet<Entry>> aliases = new LinkedHashMap<>();
    private final Map<String, LinkedHashSet<Entry>> ids = new LinkedHashMap<>();

    public void add(Entry entry, String... names) {
        ids.computeIfAbsent(entry.id(), ignored -> new LinkedHashSet<>()).add(entry);
        alias(entry.id(), entry);
        alias(entry.id().substring(entry.id().indexOf(':') + 1), entry);
        alias(entry.label(), entry);
        for (String name : names) alias(name, entry);
    }

    public void alias(String alias, Entry entry) {
        if (alias != null && !alias.isBlank() && alias.length() <= 256) {
            aliases.computeIfAbsent(normalize(alias), ignored -> new LinkedHashSet<>()).add(entry);
        }
    }

    public Resolution resolve(FindRequest request) {
        String query = normalize(request.query());
        List<Entry> matches;
        if (request.query().contains(":")) {
            // An explicit ID is exact, not an alias. minecraft:foo_bar must never become minecraft:foobar.
            matches = new ArrayList<>();
            for (Entry entry : ids.getOrDefault(request.query().toLowerCase(Locale.ROOT), new LinkedHashSet<>())) {
                if (request.kind().equals("auto") || request.kind().equals(entry.kind())) matches.add(entry);
            }
        } else {
            matches = matching(query, request.kind());
        }
        // Preserve official '다이아몬드 블록' first; only remove an explicit extra category suffix on miss.
        if (matches.isEmpty() && !request.query().contains(":") && request.kind().equals("block") && query.endsWith("블록")) {
            matches = matching(query.substring(0, query.length() - 2), "block");
        }
        if (request.kind().equals("auto")) {
            var blocks = matches.stream().filter(e -> e.kind().equals("block")).map(Entry::id).toList();
            matches.removeIf(e -> e.kind().equals("item") && blocks.contains(e.placedBlockId()));
        }
        matches.sort(Comparator.comparing(Entry::kind).thenComparing(Entry::id));
        return new Resolution(matches);
    }

    private List<Entry> matching(String query, String kind) {
        var found = new ArrayList<Entry>();
        for (Entry entry : aliases.getOrDefault(query, new LinkedHashSet<>())) {
            if (kind.equals("auto") || kind.equals(entry.kind())) found.add(entry);
        }
        return found;
    }

    public static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
                .replaceAll("[\\s_]+", "");
    }
}
//#endif
