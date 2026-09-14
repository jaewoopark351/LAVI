//#if MC == 12001
//$$ package lavi.minecraft.find.catalog;

//$$ import java.io.IOException;
//$$ import java.util.ArrayList;
//$$ import java.util.HashSet;
//$$ import java.util.Map;
//$$ import net.minecraft.client.MinecraftClient;
//$$ import net.minecraft.registry.Registries;
//$$ import net.minecraft.resource.ResourceManager;

//$$ //20260914_kpopmodder: The active runtime registries and explicit ko/en resources own FIND vocabulary.
//$$ public final class FindCatalogCapture {
//$$     private FindCatalogCapture() { }
//$$     public static FindCatalogSnapshot capture(MinecraftClient client, long generation) {
//$$         if (!client.isOnThread()) throw new IllegalStateException("client_thread_required");
//$$         try {
//$$             ResourceManager manager = client.getResourceManager();
//$$             HashSet<String> neededKeys = new HashSet<>();
//$$             int totalRegistryRecords = Registries.ENTITY_TYPE.size() + Registries.BLOCK.size() + Registries.ITEM.size();
//$$             if (totalRegistryRecords > FindCatalogSnapshot.MAX_RECORDS) return FindCatalogSnapshot.incomplete(generation, "catalog_record_bound");
//$$             for (var type : Registries.ENTITY_TYPE) neededKeys.add(type.getTranslationKey());
//$$             for (var block : Registries.BLOCK) neededKeys.add(block.getTranslationKey());
//$$             for (var item : Registries.ITEM) neededKeys.add(item.getTranslationKey());
//$$             int[] remainingResourceBytes = {FindCatalogSnapshot.MAX_TOTAL_BYTES};
//$$             Map<String, String> korean = FindTranslationResources.read(manager, "ko_kr", neededKeys, remainingResourceBytes);
//$$             Map<String, String> english = FindTranslationResources.read(manager, "en_us", neededKeys, remainingResourceBytes);
//$$             var eligibility = FindEntityEligibility.captureDeclaredVanillaTypes();
//$$             ArrayList<FindCatalogRecord> records = new ArrayList<>();
//$$             for (var type : Registries.ENTITY_TYPE) {
//$$                 records.add(record("entity", Registries.ENTITY_TYPE.getId(type).toString(), type.getTranslationKey(), korean, english, eligibility.getOrDefault(type, "unknown")));
//$$                 if (records.size() > FindCatalogSnapshot.MAX_RECORDS) return FindCatalogSnapshot.incomplete(generation, "catalog_record_bound");
//$$             }
//$$             for (var block : Registries.BLOCK) {
//$$                 records.add(record("block", Registries.BLOCK.getId(block).toString(), block.getTranslationKey(), korean, english, ""));
//$$                 if (records.size() > FindCatalogSnapshot.MAX_RECORDS) return FindCatalogSnapshot.incomplete(generation, "catalog_record_bound");
//$$             }
//$$             for (var item : Registries.ITEM) {
//$$                 records.add(record("item", Registries.ITEM.getId(item).toString(), item.getTranslationKey(), korean, english, ""));
//$$                 if (records.size() > FindCatalogSnapshot.MAX_RECORDS) return FindCatalogSnapshot.incomplete(generation, "catalog_record_bound");
//$$             }
//$$             return FindCatalogSnapshot.create(generation, records);
//$$         } catch (IOException | IllegalArgumentException error) {
//$$             return FindCatalogSnapshot.incomplete(generation, "catalog_resource_or_record_invalid");
//$$         }
//$$     }
//$$     private static FindCatalogRecord record(String kind, String id, String key, Map<String, String> korean, Map<String, String> english, String eligibility) {
//$$         return new FindCatalogRecord(kind, id, key, korean.getOrDefault(key, ""), english.getOrDefault(key, ""), eligibility);
//$$     }
//$$ }

//#endif
