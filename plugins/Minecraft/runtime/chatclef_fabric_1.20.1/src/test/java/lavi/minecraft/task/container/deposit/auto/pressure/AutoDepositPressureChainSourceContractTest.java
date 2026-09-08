package lavi.minecraft.task.container.deposit.auto.pressure;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.source;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260907_kpopmodder: Isolate the pressure-chain source-shape contract from lifecycle ordering tests.
class AutoDepositPressureChainSourceContractTest {
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
    @DisplayName("scenario 10 [assertion 15c]: pressure chain keeps the user-root binding shape")
    void pressureChainKeepsUserRootBindingShape() throws IOException {
        String pressureChain = source(
                "src/main/java/lavi/minecraft/task/container/deposit/auto/DepositAllInventoryPressureChain.java"
        );
        assertFalse(pressureChain.contains("Task primaryDepositTask = task.primaryDepositTask()"));
        assertTrue(pressureChain.contains("plan.context().epoch(),\n                    null"));
    }
}
