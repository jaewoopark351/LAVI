package lavi.minecraft.diagnostics.interaction;

import net.minecraft.util.math.BlockPos;

//20260805_kpopmodder: Keep block-interaction diagnostic field assembly out of tracking and observers.
public final class BlockInteractionDiagnosticFields {
    private BlockInteractionDiagnosticFields() {
    }

    public static Object[] interactionFields(BlockInteractionContext context,
                                             String phase,
                                             Object result,
                                             BlockInteractionScreenSnapshot screenAfter,
                                             int suppressedRepeatCount) {
        BlockInteractionScreenSnapshot screenBefore = context == null ? null : context.screenBefore();
        return new Object[]{
                "interactionId", context == null ? "unavailable" : context.interactionId(),
                "interactionStartClientTick", context == null ? "unavailable" : context.startClientTickId(),
                "phase", phase,
                "matchedHead", context != null && context.matchedHead(),
                "targetKind", context == null ? "unavailable" : context.targetKind(),
                "targetBlockId", context == null ? "unavailable" : context.targetBlockId(),
                "targetBlockDescription", context == null ? "unavailable" : context.targetBlockDescription(),
                "targetBlockState", context == null ? "unavailable" : context.targetBlockState(),
                "targetPosition", blockPos(context == null ? null : context.targetPosition()),
                "hand", context == null ? "unavailable" : context.hand(),
                "hitSide", context == null ? "unavailable" : context.hitSide(),
                "hitType", context == null ? "unavailable" : context.hitType(),
                "screenNameBefore", screenName(screenBefore),
                "screenHandlerBefore", screenHandlerName(screenBefore),
                "screenHandlerSyncIdBefore", screenHandlerSyncId(screenBefore),
                "screenNameAfter", screenName(screenAfter),
                "screenHandlerAfter", screenHandlerName(screenAfter),
                "screenHandlerSyncIdAfter", screenHandlerSyncId(screenAfter),
                "screenHandlerChanged", screenHandlerChanged(screenBefore, screenAfter),
                "interactResult", result == null ? "unavailable" : String.valueOf(result),
                "suppressedRepeatCountBefore", suppressedRepeatCount
        };
    }

    public static Object[] repeatSummaryFields(String repeatKey, int suppressedRepeatCount) {
        return new Object[]{
                "repeatKey", repeatKey,
                "suppressedRepeatCount", suppressedRepeatCount
        };
    }

    private static String screenName(BlockInteractionScreenSnapshot snapshot) {
        return snapshot == null ? "unavailable" : snapshot.screenName();
    }

    private static String screenHandlerName(BlockInteractionScreenSnapshot snapshot) {
        return snapshot == null ? "unavailable" : snapshot.screenHandlerName();
    }

    private static String screenHandlerSyncId(BlockInteractionScreenSnapshot snapshot) {
        return snapshot == null ? "unavailable" : snapshot.screenHandlerSyncId();
    }

    private static boolean screenHandlerChanged(BlockInteractionScreenSnapshot before,
                                                BlockInteractionScreenSnapshot after) {
        if (before == null || after == null) {
            return false;
        }
        return !before.screenHandlerName().equals(after.screenHandlerName())
                || !before.screenHandlerSyncId().equals(after.screenHandlerSyncId());
    }

    private static String blockPos(BlockPos pos) {
        if (pos == null) {
            return "unavailable";
        }
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }
}
