package lavi.minecraft.task.container.deposit.auto.maintenance;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.occurrences;
import static lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.source;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260907_kpopmodder: Isolate automatic-maintenance child registration ordering from generic Task ordering.
class AutoDepositMaintenanceChildRegistrationOrderTest {
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
    @DisplayName("scenario 10 [assertion 15b]: maintenance child registration precedes return")
    void maintenanceRegistersChildBeforeReturningIt() throws IOException {
        String maintenance = source(
                "src/main/java/lavi/minecraft/task/container/deposit/auto/maintenance/AutoDepositMaintenanceTask.java"
        );
        String maintenanceDiagnostics = source(
                "src/main/java/lavi/minecraft/task/container/deposit/auto/maintenance/diagnostics/AutoDepositMaintenanceDiagnostics.java"
        );
        int trustedBranch = maintenance.indexOf("case DEPOSIT_TRUSTED");
        int trustedRegistration = maintenance.indexOf("diagnostics.registerTrustedChild(", trustedBranch);
        int trustedReturn = maintenance.indexOf("return trustedStore;", trustedRegistration);

        assertTrue(trustedBranch >= 0);
        assertTrue(trustedRegistration > trustedBranch);
        assertTrue(trustedReturn > trustedRegistration);
        assertTrue(maintenance.contains("diagnostics.registerGeneralChild("));
        assertTrue(maintenanceDiagnostics.contains("? generalTaskIndex + 1"));
        assertTrue(maintenanceDiagnostics.contains("ChatClefDiagnostics.isBoundaryEnabled()"));
        assertTrue(maintenanceDiagnostics.contains("registeredChild == childTask"));
        assertEquals(1, occurrences(
                maintenanceDiagnostics,
                "StoreDepositDiagnostics.registerAutomaticMaintenanceChild("
        ));

        String facade = source(
                "src/main/java/lavi/minecraft/diagnostics/container/store/deposit/StoreDepositDiagnostics.java"
        );
        int automaticChildMethod = facade.indexOf("public static void registerAutomaticMaintenanceChild(");
        int existingChildGate = facade.indexOf(
                "StoreDepositOperationState existing = BINDINGS.stateFor(childTask);",
                automaticChildMethod
        );
        int terminalChildRegistration = facade.indexOf(
                "AUTOMATIC_TERMINALS.registerChild(maintenanceTask, childTask, childIndex);",
                existingChildGate
        );
        assertTrue(existingChildGate > automaticChildMethod);
        assertTrue(terminalChildRegistration > existingChildGate);
        assertTrue(facade.contains("if (primaryChild != null)"));
    }
}
