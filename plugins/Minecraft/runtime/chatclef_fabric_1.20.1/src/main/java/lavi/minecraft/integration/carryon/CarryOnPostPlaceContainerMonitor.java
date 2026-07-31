package lavi.minecraft.integration.carryon;

import baritone.api.utils.input.Input;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.postplace.PostPlaceContainerInteractionObserver;
import lavi.minecraft.diagnostics.postplace.PostPlaceContainerOpenIntent;
import net.minecraft.client.network.ClientPlayerEntity;

//20260731_kpopmodder: Keep Carry On post-place pickup warnings in the optional integration layer.
public final class CarryOnPostPlaceContainerMonitor implements PostPlaceContainerInteractionObserver {
    private long pendingOperationId = -1;
    private int pendingAttempt = -1;
    private CarryOnObservation stateBefore;

    @Override
    public void beforeInteract(PostPlaceContainerOpenIntent intent) {
        if (intent == null) {
            clearPending();
            return;
        }
        pendingOperationId = intent.operationId();
        pendingAttempt = intent.attemptCount();
        stateBefore = CarryOnDiagnostics.observe();
    }

    @Override
    public void afterInteract(PostPlaceContainerOpenIntent intent,
                              ClientPlayerEntity player,
                              Object interactResult) {
        if (intent == null || pendingOperationId != intent.operationId()) {
            clearPending();
            return;
        }

        CarryOnObservation stateAfter = CarryOnDiagnostics.observe();
        if (unexpectedPickup(stateBefore, stateAfter)
                && !ChatClefDiagnostics.isPostPlaceContainerGuiOpened(intent.operationId())
                && ChatClefDiagnostics.markPostPlaceContainerWarningLogged(intent.operationId())) {
            ChatClefDiagnostics.logWarningEvent("CARRY_ON_UNEXPECTED_PICKUP", "post_place_container_carry_on_pickup",
                    null,
                    "operationId", intent.operationId(),
                    "attempt", pendingAttempt,
                    "targetPosition", ChatClefDiagnostics.blockPos(intent.targetPosition()),
                    "targetBlockState", intent.targetBlockState(),
                    "stateBefore", stateName(stateBefore),
                    "stateAfter", stateName(stateAfter),
                    "shiftClick", false,
                    "playerSneaking", ChatClefDiagnostics.safeValue(() -> player == null ? null : player.isSneaking()),
                    "rawSneakKeyPressed", ChatClefDiagnostics.rawInputHeldState(Input.SNEAK),
                    "interactResult", interactResult);
        }
        clearPending();
    }

    private boolean unexpectedPickup(CarryOnObservation before, CarryOnObservation after) {
        return before != null
                && after != null
                && before.state() == CarryOnCarryState.AVAILABLE_NOT_CARRYING
                && after.state() == CarryOnCarryState.AVAILABLE_CARRYING;
    }

    private String stateName(CarryOnObservation observation) {
        return observation == null ? "unavailable" : String.valueOf(observation.state());
    }

    private void clearPending() {
        pendingOperationId = -1;
        pendingAttempt = -1;
        stateBefore = null;
    }
}
