//#if MC == 12001
//$$ package lavi.minecraft.find.catalog;

//$$ import com.fasterxml.jackson.core.JsonProcessingException;
//$$ import com.fasterxml.jackson.databind.ObjectMapper;
//$$ import java.util.ArrayList;
//$$ import java.util.Comparator;
//$$ import java.util.HashSet;
//$$ import java.util.LinkedHashMap;
//$$ import java.util.List;
//$$ import java.util.Map;
//$$ import lavi.minecraft.find.observation.MinecraftFindObservationPort;

//$$ //20260914_kpopmodder: Publish only a complete sorted catalog with bounded immutable wire pages.
//$$ public final class FindCatalogSnapshot {
//$$     public static final int MAX_RECORDS = 50000, MAX_PAGES = 256, MAX_PAGE_RECORDS = 256;
//$$     public static final int MAX_PAGE_BYTES = 65536, MAX_TOTAL_BYTES = 8 * 1024 * 1024;
//$$     private final long resourceGeneration;
//$$     private final String digest;
//$$     private final List<FindCatalogRecord> records;
//$$     private final List<Map<String, Object>> pages;
//$$     private final boolean complete;
//$$     private final String reason;

//$$     private FindCatalogSnapshot(long generation, String digest, List<FindCatalogRecord> records,
//$$                                 List<Map<String, Object>> pages, boolean complete, String reason) {
//$$         this.resourceGeneration = generation; this.digest = digest; this.records = List.copyOf(records);
//$$         this.pages = List.copyOf(pages); this.complete = complete; this.reason = reason;
//$$     }
//$$     public static FindCatalogSnapshot incomplete(long generation, String reason) {
//$$         return new FindCatalogSnapshot(generation, "", List.of(), List.of(), false, reason);
//$$     }
//$$     public static FindCatalogSnapshot create(long generation, List<FindCatalogRecord> input) {
//$$         if (generation < 0 || input.size() > MAX_RECORDS) return incomplete(generation, "catalog_record_bound");
//$$         List<FindCatalogRecord> sorted = new ArrayList<>(input);
//$$         sorted.sort(Comparator.comparing(FindCatalogRecord::targetKind).thenComparing(FindCatalogRecord::canonicalTargetId));
//$$         HashSet<String> identities = new HashSet<>();
//$$         StringBuilder digestInput = new StringBuilder();
//$$         long digestBytes = 0;
//$$         for (FindCatalogRecord record : sorted) {
//$$             if (!identities.add(record.targetKind() + "\t" + record.canonicalTargetId())) return incomplete(generation, "duplicate_catalog_identity");
//$$             String line = record.digestLine();
//$$             digestBytes += line.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
//$$             if (digestBytes > MAX_TOTAL_BYTES) return incomplete(generation, "catalog_total_bound");
//$$             digestInput.append(line);
//$$         }
//$$         String digest = MinecraftFindObservationPort.digest(digestInput.toString());
//$$         ObjectMapper mapper = new ObjectMapper();
//$$         ArrayList<List<Map<String, Object>>> groups = new ArrayList<>();
//$$         ArrayList<Map<String, Object>> group = new ArrayList<>();
//$$         try {
//$$             int groupBytes = mapper.writeValueAsBytes(page(generation, digest, MAX_PAGES, MAX_PAGES, sorted.size(), List.of())).length;
//$$             final int headerBytes = groupBytes;
//$$             for (FindCatalogRecord record : sorted) {
//$$                 Map<String, Object> recordMap = record.toMap();
//$$                 int recordBytes = mapper.writeValueAsBytes(recordMap).length + 1;
//$$                 if (group.size() == MAX_PAGE_RECORDS || groupBytes + recordBytes > MAX_PAGE_BYTES - 1024) {
//$$                     if (group.isEmpty()) return incomplete(generation, "catalog_record_page_bound");
//$$                     groups.add(List.copyOf(group));
//$$                     group.clear(); groupBytes = headerBytes;
//$$                 }
//$$                 if (groupBytes + recordBytes > MAX_PAGE_BYTES - 1024) return incomplete(generation, "catalog_record_page_bound");
//$$                 group.add(recordMap); groupBytes += recordBytes;
//$$                 if (groups.size() >= MAX_PAGES) return incomplete(generation, "catalog_page_bound");
//$$             }
//$$             if (!group.isEmpty() || groups.isEmpty()) groups.add(List.copyOf(group));
//$$             if (groups.size() > MAX_PAGES) return incomplete(generation, "catalog_page_bound");
//$$             ArrayList<Map<String, Object>> pages = new ArrayList<>();
//$$             long total = 0;
//$$             for (int index = 0; index < groups.size(); index++) {
//$$                 Map<String, Object> page = page(generation, digest, index, groups.size(), sorted.size(), groups.get(index));
//$$                 int bytes = mapper.writeValueAsBytes(page).length;
//$$                 total += bytes + 1024;
//$$                 if (bytes > MAX_PAGE_BYTES - 1024 || total > MAX_TOTAL_BYTES) return incomplete(generation, "catalog_byte_bound");
//$$                 pages.add(page);
//$$             }
//$$             return new FindCatalogSnapshot(generation, digest, sorted, pages, true, "complete_runtime_registry");
//$$         } catch (JsonProcessingException error) {
//$$             return incomplete(generation, "catalog_encoding_failed");
//$$         }
//$$     }
//$$     private static Map<String, Object> page(long generation, String digest, int index, int count, int total, List<Map<String, Object>> records) {
//$$         Map<String, Object> page = new LinkedHashMap<>();
//$$         page.put("catalog_version", 1); page.put("catalog_digest", digest); page.put("resource_generation", generation);
//$$         page.put("page_index", index); page.put("page_count", count); page.put("record_count", total);
//$$         page.put("complete", true); page.put("records", List.copyOf(records));
//$$         return Map.copyOf(page);
//$$     }
//$$     public long resourceGeneration() { return resourceGeneration; }
//$$     public String digest() { return digest; }
//$$     public List<FindCatalogRecord> records() { return records; }
//$$     public List<Map<String, Object>> pages() { return pages; }
//$$     public boolean complete() { return complete; }
//$$     public String reason() { return reason; }
//$$     public FindCatalogRecord record(String kind, String canonicalId) {
//$$         for (FindCatalogRecord record : records) if (record.targetKind().equals(kind) && record.canonicalTargetId().equals(canonicalId)) return record;
//$$         return null;
//$$     }
//$$ }

//#endif
