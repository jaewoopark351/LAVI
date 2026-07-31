package lavi.minecraft.integration.carryon;

import baritone.api.utils.input.Input;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.client.network.ClientPlayerEntity;

//20260731_kpopmodder: Keep Carry On post-place pickup warnings in the optional integration layer.
public final class CarryOnPostPlaceContainerMonitor {
    private static long pendingOperationId = -1;
    private static int pendingAttempt = -1;
    private static CarryOnObservation stateBefore;

    private CarryOnPostPlaceContainerMonitor() {
    }

    public static void beforeInteract(ChatClefDiagnostics.PostPlaceContainerOpenIntent intent) {
        if (intent == null) {
            clearPending();
            return;
        }
        pendingOperationId = intent.operationId();
        pendingAttempt = intent.attemptCount();
        stateBefore = CarryOnDiagnostics.observe();
    }

    public static void afterInteract(ChatClefDiagnostics.PostPlaceContainerOpenIntent intent,
                                     ClientPlayerEntity player,
                                     Object interactResult) {
        if (intent == null || pendingOperationId != intent.operationId()) {
            clearPending();
            return;
        }

        CarryOnObservation stateAfter = CarryOnDiagnostics.observe();
        if (unexpectedPickup(stateBefore, stateAfter)
                && !ChatClefDiagnostics.isPostPlaceContainerGuiOpened(intent.operationId())
                && ChatClefDiagnostics.markPostPlaceCarryOnWarningLogged(intent.operationId())) {
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

    private static boolean unexpectedPickup(CarryOnObservation before, CarryOnObservation after) {
        return before != null
                && after != null
                && before.state() == CarryOnCarryState.AVAILABLE_NOT_CARRYING
                && after.state() == CarryOnCarryState.AVAILABLE_CARRYING;
    }

    private static String stateName(CarryOnObservation observation) {
        return observation == null ? "unavailable" : String.valueOf(observation.state());
    }

    private static void clearPending() {
        pendingOperationId = -1;
        pendingAttempt = -1;
        stateBefore = null;
    }
}
