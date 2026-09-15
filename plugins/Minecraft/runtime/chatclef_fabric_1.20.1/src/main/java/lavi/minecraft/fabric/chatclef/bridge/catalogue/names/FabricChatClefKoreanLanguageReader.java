//#if MC == 12001
package lavi.minecraft.fabric.chatclef.bridge.catalogue.names;

import com.google.gson.JsonParser;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

//20260915_kpopmodder: Read actual ko_kr resources regardless of the selected client language.
public final class FabricChatClefKoreanLanguageReader {
    public FabricChatClefKoreanLanguageSnapshot read(ResourceManager resources) {
        Map<String, String> names = new HashMap<>();
        int resourceCount = 0;
        int failedCount = 0;
        for (String namespace : new TreeSet<>(resources.getAllNamespaces())) {
            //#if MC >= 12100
            Identifier languageId = Identifier.of(namespace, "lang/ko_kr.json");
            //#else
            //$$ Identifier languageId = new Identifier(namespace, "lang/ko_kr.json");
            //#endif
            for (var resource : resources.getAllResources(languageId)) {
                resourceCount++;
                try (var reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                    var object = JsonParser.parseReader(reader).getAsJsonObject();
                    for (var entry : object.entrySet()) {
                        if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isString()) continue;
                        String value = entry.getValue().getAsString();
                        if (entry.getKey().length() <= 256 && value.length() <= 256)
                            names.put(entry.getKey(), value);
                    }
                } catch (Exception error) {
                    // One malformed resource must not invent a translation or erase other packs.
                    failedCount++;
                }
            }
        }
        return new FabricChatClefKoreanLanguageSnapshot(names, resourceCount, failedCount);
    }
}
//#endif
