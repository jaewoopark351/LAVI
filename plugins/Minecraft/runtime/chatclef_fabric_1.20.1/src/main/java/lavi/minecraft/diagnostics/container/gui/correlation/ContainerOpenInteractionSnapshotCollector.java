package lavi.minecraft.diagnostics.container.gui.correlation;

import adris.altoclef.tasksystem.Task;
import adris.altoclef.AltoClef;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.interaction.BlockInteractionContext;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;

//20260904_kpopmodder: Read exact target and command metadata separately from correlation storage.
public final class ContainerOpenInteractionSnapshotCollector {
    public ContainerOpenInteractionObservation capture(BlockInteractionContext context) {
        if (context == null || context.targetPosition() == null) {
            return null;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) {
            return null;
        }
        BlockPos target = context.targetPosition();
        String blockId;
        try {
            Block block = client.world.getBlockState(target).getBlock();
            blockId = Registries.BLOCK.getId(block).toString();
        } catch (RuntimeException | LinkageError error) {
            return null;
        }
        ContainerTargetFamily family = ContainerTargetFamily.fromBlockId(blockId);
        if (family == ContainerTargetFamily.UNSUPPORTED) {
            return null;
        }
        Task owner = context.sourceTask();
        return new ContainerOpenInteractionObservation(
                context.interactionId(),
                context.startClientTickId(),
                family,
                target,
                blockId,
                identity(client.world),
                dimension(client),
                owner == null ? "unavailable" : owner.getClass().getName(),
                identity(owner),
                currentRootAssignmentId(),
                "unavailable",
                ChatClefDiagnostics.currentCommandContextFields()
        );
    }

    private static String currentRootAssignmentId() {
        try {
            AltoClef mod = AltoClef.getInstance();
            if (mod == null || mod.getUserTaskChain() == null) {
                return "unavailable";
            }
            String value = mod.getUserTaskChain().diagnosticRootAssignmentId();
            return value == null || value.isBlank() || "none".equals(value)
                    ? "unavailable"
                    : value;
        } catch (RuntimeException | LinkageError error) {
            return "unavailable";
        }
    }

    private static String dimension(MinecraftClient client) {
        try {
            return client.world == null
                    ? "unavailable"
                    : client.world.getRegistryKey().getValue().toString();
        } catch (RuntimeException | LinkageError error) {
            return "unavailable:" + error.getClass().getSimpleName();
        }
    }

    private static String identity(Object value) {
        return value == null
                ? "unavailable"
                : value.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(value));
    }
}
