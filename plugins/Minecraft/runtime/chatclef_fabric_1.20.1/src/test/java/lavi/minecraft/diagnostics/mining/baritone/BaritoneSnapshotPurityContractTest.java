package lavi.minecraft.diagnostics.mining.baritone;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260830_kpopmodder: Lock finder purity, formatter compatibility, and permit-gated executor details.
class BaritoneSnapshotPurityContractTest {
    private static final String FINISH_SENTINEL = "NOT_CAPTURED_WITHOUT_BEHAVIOR_REEVALUATION";

    @Test
    void plannerAndExecutorObserversNeverReevaluatePathfinderFinish() throws IOException {
        String plannerFacade = source(
                "src/main/java/lavi/minecraft/diagnostics/mining/baritone/BaritonePlannerDiagnosticSnapshot.java");
        String plannerComponent = sourcesUnder(
                "src/main/java/lavi/minecraft/diagnostics/mining/baritone/planner");
        String formatterFacade = source(
                "src/main/java/lavi/minecraft/diagnostics/mining/baritone/BaritonePathObjectFormatters.java");
        String executorComponent = sourcesUnder(
                "src/main/java/lavi/minecraft/diagnostics/mining/baritone/executor");

        assertFalse(plannerFacade.contains("isFinished"));
        assertFalse(plannerComponent.contains("::isFinished"));
        assertFalse(plannerComponent.contains(".isFinished("));
        assertFalse(formatterFacade.contains("isFinished"));
        assertFalse(executorComponent.contains("::isFinished"));
        assertFalse(executorComponent.contains(".isFinished("));
        assertTrue(plannerComponent.contains(FINISH_SENTINEL));
        assertTrue(source(
                "src/main/java/lavi/minecraft/diagnostics/mining/baritone/BaritonePathDiagnosticFormatter.java")
                .contains(FINISH_SENTINEL));
    }

    @Test
    void formatterFacadeKeepsItsExistingMethodsPublicAndStatic() {
        Set<String> expected = Set.of(
                "identity", "className", "summarizeObject", "summarizeGoal", "summarizeFinder",
                "summarizePath", "summarizeExecutor", "summarizeProcess", "commandType",
                "commandGoalType", "commandGoalSummary", "safeValue", "executorUsesPath");
        Set<String> actual = Stream.of(BaritonePathObjectFormatters.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> Modifier.isStatic(method.getModifiers()))
                .map(Method::getName)
                .collect(Collectors.toSet());

        assertTrue(Modifier.isPublic(BaritonePathObjectFormatters.class.getModifiers()));
        assertEquals(expected, actual);
    }

    @Test
    void executorDeepReadsOccurOnlyBehindLazyEventSupplier() throws IOException {
        String cheapCapture = source(
                "src/main/java/lavi/minecraft/diagnostics/mining/baritone/executor/BaritoneExecutorSnapshotCapture.java");
        String detailCapture = source(
                "src/main/java/lavi/minecraft/diagnostics/mining/baritone/executor/BaritoneExecutorDetailCapture.java");
        String eventEmitter = source(
                "src/main/java/lavi/minecraft/diagnostics/mining/baritone/executor/BaritoneExecutorProgressEventEmitter.java");
        String fields = source(
                "src/main/java/lavi/minecraft/diagnostics/mining/baritone/executor/BaritoneExecutorSnapshotFields.java");

        assertFalse(cheapCapture.contains("estimatedTicksToGoal"));
        assertFalse(cheapCapture.contains("isSafeToCancel"));
        assertFalse(cheapCapture.contains("getPath()"));
        assertFalse(cheapCapture.contains("getVelocity"));
        assertFalse(cheapCapture.contains("getBlockPos"));
        assertTrue(detailCapture.contains("estimatedTicksToGoal"));
        assertTrue(detailCapture.contains("isSafeToCancel"));
        assertTrue(detailCapture.contains("getPath()"));
        assertTrue(detailCapture.contains("getVelocity"));
        assertTrue(eventEmitter.indexOf("MiningDiagnosticEmitter.emitLazy")
                < eventEmitter.indexOf("snapshot.fields(\"After\")"));
        assertTrue(fields.contains("PERMIT_GATED_FROM_RETAINED_REFERENCES"));
    }

    private static String sourcesUnder(String relativePath) throws IOException {
        try (Stream<Path> files = Files.walk(locate(relativePath))) {
            return files
                    .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"))
                    .map(BaritoneSnapshotPurityContractTest::readUnchecked)
                    .collect(Collectors.joining("\n"));
        }
    }

    private static String source(String relativePath) throws IOException {
        return Files.readString(locate(relativePath))
                .replace("\r\n", "\n")
                .replace('\r', '\n');
    }

    private static Path locate(String relativePath) throws IOException {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (current != null) {
            Path candidate = current.resolve(relativePath);
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IOException("Unable to locate source path: " + relativePath);
    }

    private static String readUnchecked(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }
}
