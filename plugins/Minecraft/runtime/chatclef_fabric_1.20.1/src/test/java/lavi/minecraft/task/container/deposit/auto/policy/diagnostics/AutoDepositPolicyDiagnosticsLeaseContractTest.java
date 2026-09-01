package lavi.minecraft.task.container.deposit.auto.policy.diagnostics;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260831_kpopmodder: Keep policy bookkeeping inside the atomic diagnostics eligibility lease.
class AutoDepositPolicyDiagnosticsLeaseContractTest {
    @Test
    void policyEntryPointUsesAtomicEligibilityLeaseInsteadOfCheckThenRun() throws Exception {
        String relativePath =
                "src/main/java/lavi/minecraft/task/container/deposit/auto/policy/diagnostics/"
                        + "AutoDepositPolicyDiagnostics.java";
        String text = source(relativePath);

        assertTrue(text.contains("ChatClefDiagnostics.runIfDiagnosticsEligible(() -> logEligible("));
        assertFalse(text.contains("if (!ChatClefDiagnostics.isBoundaryEnabled())"));
    }

    private static String source(String relativePath) throws Exception {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (current != null) {
            Path candidate = current.resolve(relativePath);
            if (Files.isRegularFile(candidate)) {
                return Files.readString(candidate);
            }
            current = current.getParent();
        }
        throw new AssertionError("Unable to locate source file: " + relativePath);
    }
}
