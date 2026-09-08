package lavi.minecraft.diagnostics.container.store.deposit.terminal.automatic;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticContext;
import lavi.minecraft.diagnostics.container.store.deposit.terminal.StoreDepositAutomaticLifecycleLedger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.occurrences;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260907_kpopmodder: Prove the split ledger retains one synchronized facade and one production component port.
class StoreDepositAutomaticLedgerStructureTest {
    private static final String COMPONENT_NAME = "StoreDepositAutomaticLedgerComponent";
    private static final String FACADE_RELATIVE_PATH =
            "lavi/minecraft/diagnostics/container/store/deposit/terminal/"
                    + "StoreDepositAutomaticLifecycleLedger.java";
    private static final String COMPONENT_RELATIVE_PATH =
            "lavi/minecraft/diagnostics/container/store/deposit/terminal/automatic/"
                    + COMPONENT_NAME + ".java";

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
    void onlyFacadeReferencesThePublicComponentPortInProduction() throws IOException {
        Path mainJava = locateRuntimeMainJava();
        List<String> references;
        try (var paths = Files.walk(mainJava)) {
            references = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.equals(mainJava.resolve(COMPONENT_RELATIVE_PATH)))
                    .filter(path -> contains(path, COMPONENT_NAME))
                    .map(mainJava::relativize)
                    .map(Path::toString)
                    .map(path -> path.replace('\\', '/'))
                    .sorted()
                    .toList();
        }

        assertEquals(List.of(
                "lavi/minecraft/diagnostics/container/store/deposit/terminal/"
                        + "StoreDepositAutomaticLifecycleLedger.java"
        ), references);
        String facade = read(mainJava.resolve(FACADE_RELATIVE_PATH));
        assertTrue(facade.contains("private final " + COMPONENT_NAME + " component"));
        assertEquals(1, occurrences(facade, "new " + COMPONENT_NAME + "()"));
    }

    @Test
    void facadeRetainsTheOnlySynchronizationBoundary() throws NoSuchMethodException {
        for (Method method : StoreDepositAutomaticLifecycleLedger.class.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                assertTrue(Modifier.isSynchronized(method.getModifiers()), method::toString);
            }
        }
        Method purge = StoreDepositAutomaticLifecycleLedger.class.getDeclaredMethod(
                "purgeClosedRun",
                StoreDepositAutomaticContext.class
        );
        assertFalse(Modifier.isPublic(purge.getModifiers()));
        assertTrue(Modifier.isSynchronized(purge.getModifiers()));

        for (Method method : StoreDepositAutomaticLedgerComponent.class.getDeclaredMethods()) {
            assertFalse(Modifier.isSynchronized(method.getModifiers()), method::toString);
        }
    }

    @Test
    void packagePrivateHelpersKeepTheirExactResponsibilities() throws IOException {
        assertFalse(Modifier.isPublic(StoreDepositAutomaticRunRegistry.class.getModifiers()));
        assertFalse(Modifier.isPublic(StoreDepositAutomaticRunState.class.getModifiers()));
        assertFalse(Modifier.isPublic(StoreDepositAutomaticTerminalCoverage.class.getModifiers()));
        assertFalse(Modifier.isPublic(StoreDepositAutomaticCoverageEvaluator.class.getModifiers()));

        Path mainJava = locateRuntimeMainJava();
        String automaticPackage =
                "lavi/minecraft/diagnostics/container/store/deposit/terminal/automatic/";
        String evaluator = read(mainJava.resolve(
                automaticPackage + "StoreDepositAutomaticCoverageEvaluator.java"
        ));
        String terminalCoverage = read(mainJava.resolve(
                automaticPackage + "StoreDepositAutomaticTerminalCoverage.java"
        ));
        String registry = read(mainJava.resolve(
                automaticPackage + "StoreDepositAutomaticRunRegistry.java"
        ));
        assertFalse(evaluator.contains("MAX_TERMINAL_SCOPES"));
        assertFalse(evaluator.contains("withEmissionSuppressed"));
        assertFalse(evaluator.contains("LinkedHashMap"));
        assertTrue(evaluator.contains("terminalIdentityCoverageComplete"));
        assertTrue(evaluator.contains("missingLifecycleBoundaries"));
        assertTrue(terminalCoverage.contains("withEmissionSuppressed"));
        assertTrue(terminalCoverage.contains("MAX_TERMINAL_DEDUPE_IDENTITIES = 1024"));
        assertTrue(terminalCoverage.contains("MAX_EXPECTED_TERMINAL_IDENTITIES = 1024"));
        assertTrue(terminalCoverage.contains("MAX_TERMINAL_SCOPES = 32"));
        assertTrue(terminalCoverage.contains("MAX_DIAGNOSTIC_COVERAGE_GAPS = 256"));
        assertTrue(registry.contains("MAX_ACTIVE_RUNS = 16"));
        assertTrue(registry.contains("MAX_PENDING_EVICTIONS = 16"));
    }

    private static boolean contains(Path path, String token) {
        try {
            return Files.readString(path).contains(token);
        } catch (IOException error) {
            throw new IllegalStateException("Unable to inspect " + path, error);
        }
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path)
                .replace("\r\n", "\n")
                .replace('\r', '\n');
    }

    private static Path locateRuntimeMainJava() throws IOException {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (current != null) {
            for (String relativeRoot : List.of(
                    "src/main/java",
                    "plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java"
            )) {
                Path candidate = current.resolve(relativeRoot);
                if (Files.isRegularFile(candidate.resolve(FACADE_RELATIVE_PATH))
                        && Files.isRegularFile(candidate.resolve(COMPONENT_RELATIVE_PATH))) {
                    return candidate;
                }
            }
            current = current.getParent();
        }
        throw new IOException("Unable to locate exact Fabric 1.20.1 automatic-ledger source root");
    }
}
