package lavi.minecraft.diagnostics.interaction;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

import java.util.Locale;

//20260805_kpopmodder: Classify block-interaction targets for diagnostics without changing interaction routing.
public final class BlockInteractionTargetClassifier {
    private static final String[] SCREEN_OPENING_TARGET_TOKENS = {
            "blast_furnace",
            "trapped_chest",
            "furnace",
            "smoker",
            "chest",
            "barrel",
            "shulker_box",
            "crafting_table",
            "smithing_table",
            "anvil",
            "brewing_stand",
            "enchanting_table",
            "hopper",
            "dispenser",
            "dropper"
    };

    public BlockInteractionTargetInfo classify(MinecraftClient client, BlockHitResult hitResult) {
        if (client == null || client.world == null || hitResult == null) {
            return unavailable(hitResult == null ? null : hitResult.getBlockPos());
        }
        try {
            BlockPos targetPosition = hitResult.getBlockPos();
            BlockState targetState = client.world.getBlockState(targetPosition);
            Block targetBlock = targetState.getBlock();
            String targetBlockId = value(targetBlock.getTranslationKey());
            String targetBlockDescription = value(targetBlock);
            String targetBlockState = value(targetState);
            String targetKind = targetKind(targetBlockId, targetBlockDescription);
            return new BlockInteractionTargetInfo(
                    !"other".equals(targetKind),
                    targetKind,
                    targetBlockId,
                    targetBlockDescription,
                    targetBlockState,
                    targetPosition
            );
        } catch (RuntimeException | LinkageError error) {
            return new BlockInteractionTargetInfo(
                    false,
                    "unavailable:" + error.getClass().getSimpleName(),
                    "unavailable",
                    "unavailable",
                    "unavailable",
                    hitResult.getBlockPos()
            );
        }
    }

    private BlockInteractionTargetInfo unavailable(BlockPos targetPosition) {
        return new BlockInteractionTargetInfo(
                false,
                "unavailable",
                "unavailable",
                "unavailable",
                "unavailable",
                targetPosition
        );
    }

    private static String targetKind(String targetBlockId, String targetBlockDescription) {
        String normalized = (targetBlockId + " " + targetBlockDescription).toLowerCase(Locale.ROOT);
        for (String token : SCREEN_OPENING_TARGET_TOKENS) {
            if (normalized.contains(token)) {
                return token;
            }
        }
        return "other";
    }

    private static String value(Object rawValue) {
        return rawValue == null ? "unavailable" : String.valueOf(rawValue);
    }
}
