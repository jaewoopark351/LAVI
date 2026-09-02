package lavi.minecraft.diagnostics.container.store.deposit.candidate.range;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositBoundedEventLogger;
import lavi.minecraft.diagnostics.container.store.deposit.budget.StoreDepositEmissionGate;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.event.StoreDepositEventFields;
import net.minecraft.util.math.BlockPos;

//20260902_kpopmodder: Keep null user-block range observation in one focused diagnostic collaborator.
public final class StoreDepositUserBlockRangeNullDiagnostics {
    private final StoreDepositEmissionGate emissionGate;

    public StoreDepositUserBlockRangeNullDiagnostics(StoreDepositEmissionGate emissionGate) {
        this.emissionGate = emissionGate;
    }

    public void logNullInput(Object owner, BlockPos observedPosition) {
        try {
            String signature = "UserBlockRangeTracker.updateState|null_block_pos";
            StoreDepositOperationState state = null;
            if (!emissionGate.shouldEmitExceptionSignature(signature)) {
                return;
            }
            StoreDepositBoundedEventLogger.log("USER_BLOCK_RANGE_NULL_INPUT_OBSERVED",
                    "user_block_range_null_input_observed",
                    null,
                    ChatClefDiagnostics.withCommandContextFields(
                            StoreDepositEventFields.exceptionFields(
                                    state,
                                    owner,
                                    observedPosition,
                                    signature
                            )
                    ));
        } catch (RuntimeException | LinkageError ignored) {
        }
    }
}
