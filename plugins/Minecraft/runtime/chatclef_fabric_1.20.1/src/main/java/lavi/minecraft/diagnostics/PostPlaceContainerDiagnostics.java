package lavi.minecraft.diagnostics;

import baritone.api.utils.input.Input;
import lavi.minecraft.diagnostics.formatting.DiagnosticGameStateFormatter;
import lavi.minecraft.diagnostics.formatting.DiagnosticValueFormatter;
import lavi.minecraft.diagnostics.postplace.PostPlaceContainerDiagnosticState;
import lavi.minecraft.diagnostics.postplace.PostPlaceContainerInteractionObserver;
import lavi.minecraft.diagnostics.postplace.PostPlaceContainerOpenIntent;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;

import java.util.function.LongSupplier;
import java.util.function.Supplier;

//20260803_kpopmodder: Keep post-place container diagnostic ownership out of the public ChatClef facade.
final class PostPlaceContainerDiagnostics {
    private static final String PHASE_HEAD = "HEAD";
    private static final String PHASE_RETURN = "RETURN";

    private final DiagnosticModeController mode;
    private final DiagnosticEventEmitter events;
    private final LongSupplier currentClientTickId;
    private final PostPlaceContainerDiagnosticState state = new PostPlaceContainerDiagnosticState();

    PostPlaceContainerDiagnostics(DiagnosticModeController mode,
                                  DiagnosticEventEmitter events,
                                  LongSupplier currentClientTickId) {
        this.mode = mode;
        this.events = events;
        this.currentClientTickId = currentClientTickId;
    }

    void begin(long operationId,
               Object containerType,
               BlockPos targetPosition,
               Object targetBlockState) {
        if (mode.isOff()) {
            return;
        }
        state.begin(
                operationId,
                value(containerType),
                targetPosition,
                value(targetBlockState),
                currentClientTickId()
        );
    }

    PostPlaceContainerOpenIntent active(BlockPos targetPosition) {
        if (mode.isOff()) {
            return null;
        }
        return state.active(targetPosition);
    }

    int attemptCount(long operationId) {
        if (mode.isOff()) {
            return 0;
        }
        return state.attemptCount(operationId);
    }

    String lastInteractResult(long operationId) {
        if (mode.isOff()) {
            return "unavailable";
        }
        return state.lastInteractResult(operationId);
    }

    long elapsedTicks(long operationId) {
        if (mode.isOff()) {
            return -1;
        }
        return state.elapsedTicks(operationId, currentClientTickId());
    }

    boolean markGuiOpened(long operationId) {
        if (mode.isOff()) {
            return false;
        }
        return state.markGuiOpened(operationId);
    }

    boolean markGuiTimeout(long operationId) {
        if (mode.isOff()) {
            return false;
        }
        return state.markGuiTimeout(operationId);
    }

    boolean markWarningLogged(long operationId) {
        if (mode.isOff()) {
            return false;
        }
        return state.markWarningLogged(operationId);
    }

    boolean isGuiOpened(long operationId) {
        if (mode.isOff()) {
            return false;
        }
        return state.isGuiOpened(operationId);
    }

    void clear(long operationId) {
        if (mode.isOff()) {
            return;
        }
        state.clear(operationId);
    }

    void registerObserver(PostPlaceContainerInteractionObserver observer) {
        state.registerObserver(observer);
    }

    void logInteractIfMatching(String phase,
                               ClientPlayerEntity player,
                               Object hand,
                               BlockHitResult hitResult,
                               Object result) {
        if (mode.isOff() || hitResult == null) {
            return;
        }
        PostPlaceContainerOpenIntent intent = active(hitResult.getBlockPos());
        if (intent == null) {
            return;
        }

        int attempt = state.recordInteraction(intent, phase, value(result));
        if (attempt < 0) {
            return;
        }

        if (PHASE_HEAD.equals(phase)) {
            state.notifyBeforeInteract(intent);
            emitBoundary("CONTAINER_INTERACT_ATTEMPT", "post_place_container_interact_attempt",
                    "operationId", intent.operationId(),
                    "attempt", attempt,
                    "targetPosition", blockPos(intent.targetPosition()),
                    "targetBlockState", intent.targetBlockState(),
                    "hand", value(hand),
                    "shiftClick", false,
                    "playerSneaking", safeValue(() -> player == null ? null : player.isSneaking()),
                    "rawSneakKeyPressed", DiagnosticInputState.rawKeyHeld(Input.SNEAK));
            return;
        }

        if (PHASE_RETURN.equals(phase)) {
            emitBoundary("CONTAINER_INTERACT_RESULT", "post_place_container_interact_result",
                    "operationId", intent.operationId(),
                    "attempt", attempt,
                    "targetPosition", blockPos(intent.targetPosition()),
                    "targetBlockState", intent.targetBlockState(),
                    "result", value(result));
            state.notifyAfterInteract(intent, player, result);
        }
    }

    private long currentClientTickId() {
        return currentClientTickId.getAsLong();
    }

    private void emitBoundary(String eventName, String reason, Object... fields) {
        events.emitEvent("BOUNDARY", "[LAVI ChatClefBoundary]", eventName, reason, null, fields, false);
    }

    private String safeValue(Supplier<?> supplier) {
        return DiagnosticValueFormatter.safeValue(
                () -> supplier == null ? null : supplier.get(),
                !mode.isOff()
        );
    }

    private String blockPos(BlockPos pos) {
        return DiagnosticGameStateFormatter.blockPos(pos);
    }

    private String value(Object rawValue) {
        return DiagnosticValueFormatter.value(rawValue);
    }
}
