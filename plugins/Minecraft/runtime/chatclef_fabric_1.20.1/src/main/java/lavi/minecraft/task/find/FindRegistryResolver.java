//#if MC == 12001
//20260914_kpopmodder: Read all three live registries and current resource packs on the client thread, once per request.
package lavi.minecraft.task.find;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

public final class FindRegistryResolver {
    private static final int MAX_RESOURCE_CHARS = 4 * 1024 * 1024;
    private static final int MAX_TOTAL_CHARS = 16 * 1024 * 1024;
    private int readChars;
    private int resourceWarnings;

    public int resourceWarnings() { return resourceWarnings; }

    public FindNameIndex.Resolution resolve(FindRequest request) {
        if (request.kind().equals("player")) {
            return new FindNameIndex.Resolution(java.util.List.of(
                    new FindNameIndex.Entry("player", request.query(), request.query(), "")));
        }
        Map<String, String> ko = language("ko_kr");
        Map<String, String> en = language("en_us");
        var index = new FindNameIndex();
        for (var type : Registries.ENTITY_TYPE) {
            add(index, "entity", Registries.ENTITY_TYPE.getId(type).toString(), type.getTranslationKey(), "", ko, en);
        }
        for (var block : Registries.BLOCK) {
            add(index, "block", Registries.BLOCK.getId(block).toString(), block.getTranslationKey(), "", ko, en);
        }
        for (var item : Registries.ITEM) {
            String blockId = item instanceof BlockItem placed ? Registries.BLOCK.getId(placed.getBlock()).toString() : "";
            add(index, "item", Registries.ITEM.getId(item).toString(), item.getTranslationKey(), blockId, ko, en);
        }
        return index.resolve(request);
    }

    private void add(FindNameIndex index, String kind, String id, String key, String blockId,
                     Map<String, String> ko, Map<String, String> en) {
        String current = Language.getInstance().get(key);
        String label = ko.getOrDefault(key, current.equals(key) ? id : current);
        label = label.replaceAll("[\\p{Cc}\\p{Cf}]", " ").trim();
        if (label.length() > 128) label = id;
        var entry = new FindNameIndex.Entry(kind, id, label, blockId);
        index.add(entry, ko.get(key), en.get(key), current);
        // Only colloquial spelling supplements; the corresponding live registration must already exist.
        if (kind.equals("entity") && id.equals("minecraft:villager")) index.alias("마을주민", entry);
    }

    private Map<String, String> language(String locale) {
        var result = new HashMap<String, String>();
        var manager = MinecraftClient.getInstance().getResourceManager();
        for (String namespace : new TreeSet<>(manager.getAllNamespaces())) {
            for (Resource resource : manager.getAllResources(new Identifier(namespace, "lang/" + locale + ".json"))) {
                // Packs arrive in increasing priority; later translations replace earlier ones like LanguageManager.
                try (Reader reader = resource.getReader()) {
                    var text = new StringBuilder();
                    char[] buffer = new char[4096];
                    int size;
                    while ((size = reader.read(buffer)) != -1) {
                        readChars += size;
                        if (text.length() + size > MAX_RESOURCE_CHARS || readChars > MAX_TOTAL_CHARS) {
                            throw new IllegalArgumentException("find_language_resource_budget");
                        }
                        text.append(buffer, 0, size);
                    }
                    JsonObject json = JsonParser.parseString(text.toString()).getAsJsonObject();
                    for (var value : json.entrySet()) {
                        if (value.getValue().isJsonPrimitive() && value.getValue().getAsJsonPrimitive().isString()) {
                            result.put(value.getKey(), value.getValue().getAsString());
                        }
                    }
                } catch (Exception unavailable) {
                    resourceWarnings++;
                    // A malformed/missing translation never authorizes an ID absent from the registry.
                }
                if (readChars > MAX_TOTAL_CHARS) return result;
            }
        }
        return result;
    }
}
//#endif
