package lavi.minecraft.diagnostics.container.store.deposit.lifecycle;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.source;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260907_kpopmodder: Isolate the generic parent/reconciliation/child ordering characterization.
class StoreDepositTaskLifecycleOrderingContractTest {
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
    @DisplayName("scenario 10 [assertion 15a]: parent onTick and reconciliation precede child tick")
    void parentReconciliationPrecedesChildTick() throws IOException {
        String task = source("src/main/java/adris/altoclef/tasksystem/Task.java");
        int parentTick = task.indexOf("Task newSub = onTick();");
        int reconciliation = task.indexOf("StoreDepositDiagnostics.logChildReconciliation(", parentTick);
        int childTick = task.indexOf("sub.tick(parentChain);", reconciliation);

        assertTrue(parentTick >= 0);
        assertTrue(reconciliation > parentTick);
        assertTrue(childTick > reconciliation);
    }
}
