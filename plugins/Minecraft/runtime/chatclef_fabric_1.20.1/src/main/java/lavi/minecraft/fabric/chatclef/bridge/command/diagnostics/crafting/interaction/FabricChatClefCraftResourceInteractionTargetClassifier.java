package lavi.minecraft.fabric.chatclef.bridge.command.diagnostics.crafting.interaction;

import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceStage;
import lavi.minecraft.diagnostics.crafting.acquisition.target.CraftResourceTargetRole;
import lavi.minecraft.diagnostics.interaction.BlockInteractionContext;
import net.minecraft.util.math.BlockPos;

import java.util.Locale;
import java.util.Optional;

//20260901_kpopmodder: Classify only the immutable target facts captured by the existing observer boundary.
final class FabricChatClefCraftResourceInteractionTargetClassifier {
    Optional<FabricChatClefCraftResourceInteractionTarget> classify(
            BlockInteractionContext context) {
        if (context == null || !context.screenOpeningTarget()) {
            return Optional.empty();
        }
        BlockPos position = context.targetPosition();
        if (position == null) {
            return Optional.empty();
        }

        String targetKind = normalize(context.targetKind());
        return switch (targetKind) {
            case "furnace", "blast_furnace" -> Optional.of(target(
                    context,
                    position,
                    CraftResourceStage.RAW_IRON_SMELTING,
                    CraftResourceTargetRole.FURNACE_INTERACTION,
                    targetKind
            ));
            case "crafting_table" -> Optional.of(target(
                    context,
                    position,
                    CraftResourceStage.FINAL_CRAFTING,
                    CraftResourceTargetRole.CRAFTING_TABLE_INTERACTION,
                    targetKind
            ));
            default -> Optional.of(target(
                    context,
                    position,
                    CraftResourceStage.UNKNOWN,
                    CraftResourceTargetRole.UNKNOWN,
                    targetKind
            ));
        };
    }

    private static FabricChatClefCraftResourceInteractionTarget target(
            BlockInteractionContext context,
            BlockPos position,
            CraftResourceStage stage,
            CraftResourceTargetRole role,
            String targetKind) {
        return new FabricChatClefCraftResourceInteractionTarget(
                stage,
                role,
                position.getX() + "," + position.getY() + "," + position.getZ(),
                available(context.targetBlockId()),
                targetKind
        );
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String available(String value) {
        return value == null || value.isBlank()
                ? "UNAVAILABLE"
                : value.trim().toLowerCase(Locale.ROOT);
    }
}
