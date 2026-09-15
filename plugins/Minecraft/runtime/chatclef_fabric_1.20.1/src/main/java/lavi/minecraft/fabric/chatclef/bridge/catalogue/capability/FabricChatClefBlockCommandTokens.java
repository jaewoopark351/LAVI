//#if MC == 12001
package lavi.minecraft.fabric.chatclef.bridge.catalogue.capability;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

//20260915_kpopmodder: SCAN consumes actual runtime Blocks fields rather than registry IDs.
public final class FabricChatClefBlockCommandTokens {
    public Map<Block, String> read() {
        Map<Block, String> tokens = new HashMap<>();
        for (var field : Blocks.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || !Block.class.isAssignableFrom(field.getType())) continue;
            try {
                if (field.trySetAccessible()) tokens.put((Block) field.get(null), field.getName());
            } catch (IllegalAccessException error) {
                // Missing native access is a capability gap, never a guessed registry-path token.
            }
        }
        return Map.copyOf(tokens);
    }
}
//#endif
