//#if MC == 12001
//$$ package lavi.minecraft.find.catalog;

//$$ import com.fasterxml.jackson.databind.JsonNode;
//$$ import com.fasterxml.jackson.databind.ObjectMapper;
//$$ import java.io.IOException;
//$$ import java.io.InputStream;
//$$ import java.util.HashMap;
//$$ import java.util.Map;
//$$ import java.util.Set;
//$$ import java.util.TreeSet;
//$$ import net.minecraft.resource.ResourceManager;
//$$ import net.minecraft.util.Identifier;

//$$ //20260914_kpopmodder: Explicit language resource reads merge pack priority and close bounded input streams.
//$$ public final class FindTranslationResources {
//$$     private FindTranslationResources() { }
//$$     public static Map<String, String> read(ResourceManager manager, String locale, Set<String> neededKeys, int[] remaining) throws IOException {
//$$         HashMap<String, String> result = new HashMap<>();
//$$         ObjectMapper mapper = new ObjectMapper();
//$$         int resources = 0;
//$$         for (String namespace : new TreeSet<>(manager.getAllNamespaces())) {
//$$             for (var resource : manager.getAllResources(new Identifier(namespace, "lang/" + locale + ".json"))) {
//$$                 if (++resources > 1024) throw new IOException("resource_count_bound");
//$$                 byte[] bytes;
//$$                 try (InputStream stream = resource.getInputStream()) { bytes = stream.readNBytes(remaining[0] + 1); }
//$$                 if (bytes.length > remaining[0]) throw new IOException("translation_resource_byte_bound");
//$$                 remaining[0] -= bytes.length;
//$$                 JsonNode root = mapper.readTree(bytes);
//$$                 if (root == null || !root.isObject()) throw new IOException("translation_resource_shape");
//$$                 var fields = root.fields();
//$$                 while (fields.hasNext()) {
//$$                     var field = fields.next();
//$$                     if (!neededKeys.contains(field.getKey())) continue;
//$$                     if (!field.getValue().isTextual()) throw new IOException("translation_resource_value");
//$$                     result.put(field.getKey(), field.getValue().textValue());
//$$                 }
//$$             }
//$$         }
//$$         return Map.copyOf(result);
//$$     }
//$$ }

//#endif
