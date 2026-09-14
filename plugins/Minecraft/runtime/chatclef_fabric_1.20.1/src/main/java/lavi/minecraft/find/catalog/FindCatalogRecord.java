//#if MC == 12001
//$$ package lavi.minecraft.find.catalog;

//$$ import java.text.Normalizer;
//$$ import java.util.LinkedHashMap;
//$$ import java.util.Map;

//$$ //20260914_kpopmodder: Catalog strings have closed field types, Unicode bounds and collision-preserving labels.
//$$ public record FindCatalogRecord(String targetKind, String canonicalTargetId, String translationKey,
//$$                                 String koreanName, String englishName, String eligibility) {
//$$     public FindCatalogRecord {
//$$         targetKind = clean(targetKind, 16);
//$$         canonicalTargetId = clean(canonicalTargetId, 128);
//$$         translationKey = clean(translationKey, 256);
//$$         koreanName = clean(koreanName, 256);
//$$         englishName = clean(englishName, 256);
//$$         eligibility = clean(eligibility, 16);
//$$         if (translationKey.isEmpty()) throw new IllegalArgumentException("empty_translation_key");
//$$         if (!targetKind.equals("entity") && !targetKind.equals("block") && !targetKind.equals("item")) {
//$$             throw new IllegalArgumentException("invalid_catalog_kind");
//$$         }
//$$         if (!canonicalTargetId.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) throw new IllegalArgumentException("invalid_catalog_id");
//$$         if (targetKind.equals("entity") && !eligibility.equals("mob") && !eligibility.equals("non_mob") && !eligibility.equals("unknown")) {
//$$             throw new IllegalArgumentException("invalid_entity_eligibility");
//$$         }
//$$         if (!targetKind.equals("entity") && !eligibility.isEmpty()) throw new IllegalArgumentException("unexpected_eligibility");
//$$     }
//$$     public Map<String, Object> toMap() {
//$$         Map<String, Object> values = new LinkedHashMap<>();
//$$         values.put("target_kind", targetKind); values.put("canonical_target_id", canonicalTargetId);
//$$         values.put("translation_key", translationKey); values.put("korean_name", koreanName); values.put("english_name", englishName);
//$$         if (targetKind.equals("entity")) values.put("eligibility", eligibility);
//$$         return Map.copyOf(values);
//$$     }
//$$     public String digestLine() {
//$$         return targetKind + "\t" + canonicalTargetId + "\t" + translationKey + "\t" + koreanName + "\t" + englishName + "\t" + eligibility + "\n";
//$$     }
//$$     private static String clean(String value, int maximum) {
//$$         if (value == null) throw new IllegalArgumentException("null_catalog_string");
//$$         String normalized = Normalizer.normalize(value, Normalizer.Form.NFC);
//$$         if (normalized.codePointCount(0, normalized.length()) > maximum
//$$                 || normalized.codePoints().anyMatch(cp -> Character.isISOControl(cp) || Character.getType(cp) == Character.FORMAT
//$$                         || Character.getType(cp) == Character.SURROGATE || cp == 0x2028 || cp == 0x2029)) throw new IllegalArgumentException("invalid_catalog_string");
//$$         return normalized;
//$$     }
//$$ }

//#endif
