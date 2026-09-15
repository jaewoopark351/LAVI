//#if MC == 12001
//20260914_kpopmodder: Read all three live registries on the client thread, once per request.
//20260915_kpopmodder: Python owns language JSON and aliases; Java accepts only exact registry IDs.
package lavi.minecraft.task.find;

import net.minecraft.item.BlockItem;
import net.minecraft.registry.Registries;

public final class FindRegistryResolver {
    // Retained wire-schema field for older consumers; language resources are no longer read here.
    public int resourceWarnings() { return 0; }

    public FindNameIndex.Resolution resolve(FindRequest request) {
        if (request.kind().equals("player")) {
            return new FindNameIndex.Resolution(java.util.List.of(
                    new FindNameIndex.Entry("player", request.query(), request.query(), "")));
        }
        String id = request.query().contains(":") ? request.query() : "minecraft:" + request.query();
        var index = new FindNameIndex();
        // Iteration verifies actual membership, never DefaultedRegistry.get's air/pig fallback.
        // No static species or item whitelist; any live mod namespace is allowed.
        if (request.kind().equals("auto") || request.kind().equals("entity")) {
            for (var type : Registries.ENTITY_TYPE) {
                if (Registries.ENTITY_TYPE.getId(type).toString().equals(id))
                    index.add(new FindNameIndex.Entry("entity", id, id, ""));
            }
        }
        if (request.kind().equals("auto") || request.kind().equals("block")) {
            for (var block : Registries.BLOCK) {
                if (Registries.BLOCK.getId(block).toString().equals(id))
                    index.add(new FindNameIndex.Entry("block", id, id, ""));
            }
        }
        if (request.kind().equals("auto") || request.kind().equals("item")) {
            for (var item : Registries.ITEM) {
                if (!Registries.ITEM.getId(item).toString().equals(id)) continue;
                String blockId = item instanceof BlockItem placed
                        ? Registries.BLOCK.getId(placed.getBlock()).toString() : "";
                index.add(new FindNameIndex.Entry("item", id, id, blockId));
            }
        }
        // Always take the exact namespaced-ID branch, never normalized alias matching.
        return index.resolve(new FindRequest(request.kind(), id, request.mode()));
    }
}
//#endif
