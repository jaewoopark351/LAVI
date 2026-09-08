package lavi.minecraft.diagnostics.container.store.deposit.interaction;

import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationContext;
import lavi.minecraft.diagnostics.container.store.deposit.context.StoreDepositOperationState;
import lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.TestTask;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.fields;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

//20260907_kpopmodder: Keep each migrated Slice A characterization scenario with its owning responsibility.
class StoreDepositInteractionDiagnosticFieldsTest {

    @BeforeEach
    void startWithFreshDiagnosticSession() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @AfterEach
    void disableDiagnostics() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @Test
    @DisplayName("scenario 12 [assertion 19]: HEAD binding verdict differs from observation-time verdict")
    void headCaptureVerdictAndObservationLookupVerdictRemainTypedAndIndependent() {
        Task root = new TestTask("root");
        StoreDepositOperationState state = new StoreDepositOperationState(
                new StoreDepositOperationContext("operation", "AUTO_DEPOSIT_ALL_CHAIN", root, 0L, 0L)
        );
        BlockPos target = new BlockPos(1, 64, 2);
        StoreDepositInteractionContext head = new StoreDepositInteractionContext(
                1L,
                "operation",
                "operation-attempt-1",
                1L,
                1L,
                1L,
                "OPEN_EXISTING",
                1L,
                "route.Child",
                "route-child",
                "route.Leaf",
                "route-leaf",
                0,
                "CURRENT_PURSUIT",
                target,
                "ACTIVE_TASK_IDENTITY_BINDING_AND_TARGET_VALUE",
                "EXACT",
                10L
        );
        state.routeState().recordParentDecision(
                "OBTAIN_CHEST", false, null, false, false, false, false,
                null, "NO_RAW_CLOSEST", "hash"
        );

        Map<String, Object> observation = fields(StoreDepositInteractionDiagnosticFields.fields(head, state));
        assertEquals("EXACT", observation.get("headBindingVerdict"));
        assertEquals("ROUTE_BRANCH_CHANGED", observation.get("observationBindingVerdict"));
        assertNotEquals(observation.get("headBindingVerdict"), observation.get("observationBindingVerdict"));
        StoreDepositInteractionBindingRegistry registry =
                new StoreDepositInteractionBindingRegistry();
        registry.bind(head, 10L);
        assertEquals(
                StoreDepositInteractionBindingRegistry.LookupStatus.FOUND,
                registry.lookup(head.interactionId(), 50L).status()
        );
        assertEquals(
                StoreDepositInteractionBindingRegistry.LookupStatus.EXPIRED,
                registry.lookup(head.interactionId(), 51L).status()
        );
        Map<String, Object> expired = fields(
                StoreDepositInteractionDiagnosticFields.unavailableObservationFields(
                        registry.lookup(head.interactionId(), 51L).context(),
                        StoreDepositInteractionBindingRegistry.LookupStatus.EXPIRED
                )
        );
        assertEquals("EXACT", expired.get("headBindingVerdict"));
        assertEquals("BINDING_EXPIRED", expired.get("observationBindingVerdict"));
    }
}
