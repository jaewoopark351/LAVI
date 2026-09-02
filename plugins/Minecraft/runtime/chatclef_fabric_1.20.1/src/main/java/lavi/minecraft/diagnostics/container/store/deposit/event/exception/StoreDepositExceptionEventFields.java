package lavi.minecraft.diagnostics.container.store.deposit.event.exception;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.event.common.StoreDepositOperationCorrelationFields;
import lavi.minecraft.diagnostics.container.store.deposit.event.common.StoreDepositOrderedFieldSupport;
import net.minecraft.util.math.BlockPos;

//20260902_kpopmodder: Own exception-family diagnostic payload assembly behind the stable public facade.
public final class StoreDepositExceptionEventFields {
    private StoreDepositExceptionEventFields() {
    }

    public static Object[] exceptionFields(StoreDepositOperationState state,
                                           Object owner,
                                           BlockPos observedPosition,
                                           String signature) {
        return StoreDepositOrderedFieldSupport.merge(
                StoreDepositOperationCorrelationFields.operationFields(state),
                new Object[]{
                        "diagnosticScope", "store_deposit_exception",
                        "owner", "store_deposit_exception_observer",
                        "mode", "BOUNDARY",
                        "trigger", "null_user_block_position",
                        "dedupe_key", "user_block_range_null_input|" + signature,
                        "max_emission", "first_signature_only,session=16",
                        "correlation", "storeOperationId=" + StoreDepositOperationCorrelationFields.operationId(state),
                        "payload", "flat_fields",
                        "terminal", false,
                        "behavior_effect", "none",
                        "exceptionBoundary", owner == null ? "unavailable" : owner.getClass().getName(),
                        "observedPosition", ChatClefDiagnostics.blockPos(observedPosition),
                        "exceptionSignature", signature,
                        "threadName", Thread.currentThread().getName()
                }
        );
    }
}
