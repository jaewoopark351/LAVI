package lavi.minecraft.integration.carryon.container;

import lavi.minecraft.diagnostics.interaction.BlockInteractionContext;
import lavi.minecraft.integration.carryon.CarryOnObservation;

//20260805_kpopmodder: Classify Carry On pickup attribution strength without deciding behavior.
enum CarryOnContainerPickupEvidence {
    CONFIRMED_TARGET_IDENTITY,
    STRONG_TEMPORAL_ATTRIBUTION,
    INSUFFICIENT;

    static CarryOnContainerPickupEvidence classify(BlockInteractionContext context, CarryOnObservation after) {
        if (context == null || after == null) {
            return INSUFFICIENT;
        }
        if (identityMatches(context.targetBlockId(), after.carriedBlockId())
                || identityMatches(context.targetBlockDescription(), after.carriedBlockDescription())
                || identityMatches(context.targetBlockState(), after.carriedBlockState())) {
            return CONFIRMED_TARGET_IDENTITY;
        }
        if (identityUnavailable(after)) {
            return STRONG_TEMPORAL_ATTRIBUTION;
        }
        return INSUFFICIENT;
    }

    private static boolean identityMatches(String expected, String actual) {
        return expected != null
                && actual != null
                && !"unavailable".equals(expected)
                && !"unavailable".equals(actual)
                && expected.equals(actual);
    }

    private static boolean identityUnavailable(CarryOnObservation observation) {
        return "unavailable".equals(observation.carriedBlockId())
                && "unavailable".equals(observation.carriedBlockDescription())
                && "unavailable".equals(observation.carriedBlockState());
    }
}
