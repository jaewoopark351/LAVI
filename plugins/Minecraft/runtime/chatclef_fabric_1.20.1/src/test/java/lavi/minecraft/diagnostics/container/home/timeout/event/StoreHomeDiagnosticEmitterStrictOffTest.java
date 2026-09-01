package lavi.minecraft.diagnostics.container.home.timeout.event;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.home.timeout.budget.StoreHomeOperationLogBudget;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

//20260831_kpopmodder: Prove STORE_HOME suppression accounting is covered by the session lease.
class StoreHomeDiagnosticEmitterStrictOffTest {
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
    void offSuppressionRequestDoesNotMutateOperationBudget() throws Exception {
        StoreHomeDiagnosticEmitter emitter = new StoreHomeDiagnosticEmitter(
                23L,
                () -> "strict-off-test"
        );

        emitter.recordProgressSuppressed("STORE_HOME_PROGRESS");

        assertEquals(0, field(
                operationBudget(emitter).summaryFields(),
                "operationDiagnosticSuppressedEventCount"
        ));
    }

    private static StoreHomeOperationLogBudget operationBudget(
            StoreHomeDiagnosticEmitter emitter) throws Exception {
        Field field = StoreHomeDiagnosticEmitter.class.getDeclaredField("operationBudget");
        field.setAccessible(true);
        return (StoreHomeOperationLogBudget) field.get(emitter);
    }

    private static Object field(Object[] fields, String name) {
        for (int index = 0; index + 1 < fields.length; index += 2) {
            if (name.equals(fields[index])) {
                return fields[index + 1];
            }
        }
        throw new AssertionError("Missing field: " + name);
    }
}
