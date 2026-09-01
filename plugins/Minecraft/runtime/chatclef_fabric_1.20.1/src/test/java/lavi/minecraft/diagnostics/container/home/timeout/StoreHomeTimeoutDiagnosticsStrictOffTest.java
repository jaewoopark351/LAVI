package lavi.minecraft.diagnostics.container.home.timeout;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.home.timeout.state.StoreHomeDiagnosticCandidateCatalogState;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.home.execution.HomeStorageTransferExecutor;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutPolicy;
import lavi.minecraft.testsupport.TestObjects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260831_kpopmodder: Prove STORE_HOME bookkeeping cannot mutate while diagnostics are OFF.
class StoreHomeTimeoutDiagnosticsStrictOffTest {
    @BeforeEach
    void resetDiagnostics() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @AfterEach
    void leaveDiagnosticsOff() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }

    @Test
    void candidateCatalogIsMutationFreeOffAndCapturedWhenEligible() throws Exception {
        StoreHomeTimeoutDiagnostics diagnostics = new StoreHomeTimeoutDiagnostics(
                17L,
                StoreHomeTimeoutPolicy.standard(),
                AutoDepositExactOpenContainerBinding.UNAVAILABLE,
                TestObjects.allocate(HomeStorageTransferExecutor.class)
        );

        diagnostics.recordCandidateCatalog(4);
        assertFalse(candidateCatalog(diagnostics).catalogCaptured());

        ChatClefDiagnostics.setBoundaryEnabled(true);
        diagnostics.recordCandidateCatalog(4);
        assertTrue(candidateCatalog(diagnostics).catalogCaptured());
    }

    private static StoreHomeDiagnosticCandidateCatalogState candidateCatalog(
            StoreHomeTimeoutDiagnostics diagnostics) throws Exception {
        Field field = StoreHomeTimeoutDiagnostics.class.getDeclaredField("candidateCatalog");
        field.setAccessible(true);
        return (StoreHomeDiagnosticCandidateCatalogState) field.get(diagnostics);
    }
}
