package lavi.minecraft.diagnostics.container.gui.correlation;

import net.minecraft.util.math.BlockPos;

import java.util.Arrays;

//20260904_kpopmodder: Freeze one diagnostics-only world interaction without creating a GUI attempt state.
public record ContainerOpenInteractionObservation(
        long interactionId,
        long interactionStartClientTick,
        ContainerTargetFamily targetFamily,
        BlockPos target,
        String targetBlockId,
        String worldIdentity,
        String dimension,
        String routeOwnerClass,
        String routeOwnerIdentity,
        String rootAssignmentId,
        String interactResult,
        Object[] commandContextFields) {

    public ContainerOpenInteractionObservation {
        target = target == null ? null : target.toImmutable();
        targetBlockId = value(targetBlockId);
        worldIdentity = value(worldIdentity);
        dimension = value(dimension);
        routeOwnerClass = value(routeOwnerClass);
        routeOwnerIdentity = value(routeOwnerIdentity);
        rootAssignmentId = value(rootAssignmentId);
        interactResult = value(interactResult);
        commandContextFields = commandContextFields == null
                ? new Object[0]
                : Arrays.copyOf(commandContextFields, commandContextFields.length);
    }

    public ContainerOpenInteractionObservation(
            long interactionId,
            long interactionStartClientTick,
            ContainerTargetFamily targetFamily,
            BlockPos target,
            String targetBlockId,
            String worldIdentity,
            String dimension,
            String routeOwnerClass,
            String routeOwnerIdentity,
            String interactResult,
            Object[] commandContextFields) {
        this(
                interactionId,
                interactionStartClientTick,
                targetFamily,
                target,
                targetBlockId,
                worldIdentity,
                dimension,
                routeOwnerClass,
                routeOwnerIdentity,
                "unavailable",
                interactResult,
                commandContextFields
        );
    }

    @Override
    public Object[] commandContextFields() {
        return Arrays.copyOf(commandContextFields, commandContextFields.length);
    }

    public ContainerOpenInteractionObservation withInteractResult(Object result) {
        return new ContainerOpenInteractionObservation(
                interactionId,
                interactionStartClientTick,
                targetFamily,
                target,
                targetBlockId,
                worldIdentity,
                dimension,
                routeOwnerClass,
                routeOwnerIdentity,
                rootAssignmentId,
                result == null ? "unavailable" : String.valueOf(result),
                commandContextFields
        );
    }

    private static String value(String raw) {
        return raw == null ? "unavailable" : raw;
    }
}
