package lavi.minecraft.integration.carryon.container;

import lavi.minecraft.diagnostics.interaction.BlockInteractionContext;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

//20260815_kpopmodder: Read target block identity for diagnostics without mutating world state.
final class CarryOnContainerTargetSnapshot {
    private final String blockId;
    private final String blockDescription;
    private final String blockState;

    private CarryOnContainerTargetSnapshot(String blockId, String blockDescription, String blockState) {
        this.blockId = value(blockId);
        this.blockDescription = value(blockDescription);
        this.blockState = value(blockState);
    }

    static CarryOnContainerTargetSnapshot current(MinecraftClient client, BlockPos targetPosition) {
        if (client == null || client.world == null || targetPosition == null) {
            return unavailable();
        }
        try {
            BlockState state = client.world.getBlockState(targetPosition);
            Block block = state.getBlock();
            return new CarryOnContainerTargetSnapshot(
                    block.getTranslationKey(),
                    String.valueOf(block),
                    String.valueOf(state)
            );
        } catch (RuntimeException | LinkageError error) {
            return new CarryOnContainerTargetSnapshot(
                    "unavailable",
                    "unavailable",
                    "unavailable:" + error.getClass().getSimpleName()
            );
        }
    }

    String blockId() {
        return blockId;
    }

    String blockDescription() {
        return blockDescription;
    }

    String blockState() {
        return blockState;
    }

    boolean removed() {
        return containsAir(blockId) || containsAir(blockDescription) || containsAir(blockState);
    }

    boolean stillMatches(BlockInteractionContext context) {
        if (context == null) {
            return false;
        }
        return matches(context.targetBlockId(), blockId)
                || matches(context.targetBlockDescription(), blockDescription)
                || matches(context.targetBlockState(), blockState);
    }

    String fingerprint() {
        return blockId + "|" + blockDescription + "|" + blockState;
    }

    private static CarryOnContainerTargetSnapshot unavailable() {
        return new CarryOnContainerTargetSnapshot("unavailable", "unavailable", "unavailable");
    }

    private static boolean matches(String expected, String actual) {
        return expected != null
                && actual != null
                && !"unavailable".equals(expected)
                && !"unavailable".equals(actual)
                && expected.equals(actual);
    }

    private static boolean containsAir(String value) {
        return value != null && value.toLowerCase(java.util.Locale.ROOT).contains("minecraft:air");
    }

    private static String value(String rawValue) {
        return rawValue == null ? "unavailable" : rawValue;
    }
}
