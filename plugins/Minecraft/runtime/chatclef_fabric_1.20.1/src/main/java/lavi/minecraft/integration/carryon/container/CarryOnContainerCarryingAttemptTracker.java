package lavi.minecraft.integration.carryon.container;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.interaction.BlockInteractionContext;
import lavi.minecraft.integration.carryon.CarryOnCarryState;
import lavi.minecraft.integration.carryon.CarryOnObservation;

//20260815_kpopmodder: Bound logs for container-open attempts made while Carry On is already carrying a block.
final class CarryOnContainerCarryingAttemptTracker {
    private static final long SUMMARY_INTERVAL_TICKS = 200;
    private static final int SESSION_HARD_CAP = 256;

    private final CarryOnContainerInteractionLogger logger;
    private String lastAttemptKey = "";
    private long lastEmissionTick = -1;
    private int suppressedRepeatCount;
    private int emittedCount;
    private boolean capLogged;

    CarryOnContainerCarryingAttemptTracker(CarryOnContainerInteractionLogger logger) {
        this.logger = logger;
    }

    void observe(BlockInteractionContext context, CarryOnObservation observation) {
        if (context == null
                || observation == null
                || observation.state() != CarryOnCarryState.AVAILABLE_CARRYING) {
            return;
        }
        long tick = ChatClefDiagnostics.currentClientTickId();
        String attemptKey = key(context, observation);
        if (!attemptKey.equals(lastAttemptKey)) {
            lastAttemptKey = attemptKey;
            suppressedRepeatCount = 0;
            emit(context, observation, "state_changed", false);
            return;
        }
        suppressedRepeatCount++;
        if (lastEmissionTick < 0 || tick - lastEmissionTick >= SUMMARY_INTERVAL_TICKS) {
            emit(context, observation, "repeat_summary", true);
            suppressedRepeatCount = 0;
        }
    }

    private void emit(BlockInteractionContext context,
                      CarryOnObservation observation,
                      String trigger,
                      boolean summary) {
        if (emittedCount >= SESSION_HARD_CAP) {
            if (!capLogged) {
                capLogged = true;
                logger.logAttemptWhileCarryingCap(SESSION_HARD_CAP);
            }
            return;
        }
        emittedCount++;
        lastEmissionTick = ChatClefDiagnostics.currentClientTickId();
        logger.logAttemptWhileCarrying(context, observation, trigger, summary, suppressedRepeatCount);
    }

    private String key(BlockInteractionContext context, CarryOnObservation observation) {
        return context.targetKind()
                + "|" + context.targetBlockId()
                + "|" + ChatClefDiagnostics.blockPos(context.targetPosition())
                + "|" + observation.state()
                + "|" + observation.carriedBlockId()
                + "|" + observation.carriedBlockState()
                + "|" + screenHandler(context);
    }

    private String screenHandler(BlockInteractionContext context) {
        return context.screenBefore() == null ? "unavailable" : context.screenBefore().screenHandlerName();
    }
}
