package lavi.minecraft.diagnostics.container.home.timeout.operation;

import adris.altoclef.util.Dimension;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.container.home.timeout.StoreHomeTimeoutDiagnostics;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestination;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationCandidate;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBinding;
import lavi.minecraft.task.container.deposit.auto.trusted.interaction.AutoDepositExactOpenContainerBindingDiagnosticView;
import lavi.minecraft.task.container.home.execution.HomeStorageScreenSlotResolver;
import lavi.minecraft.task.container.home.execution.HomeStorageTransferExecutor;
import lavi.minecraft.task.container.home.execution.StoreHomePhase;
import lavi.minecraft.task.container.home.execution.candidate.StoreHomeCandidateAttempt;
import lavi.minecraft.task.container.home.execution.operation.StoreHomeOperationProgress;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutObservation;
import lavi.minecraft.task.container.home.execution.timeout.StoreHomeTimeoutPolicy;
import lavi.minecraft.testsupport.TestObjects;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

//20260907_kpopmodder: Fix the STORE_HOME facade boundary, instance ownership, and observed exception contract.
class StoreHomeTimeoutLifecycleCoordinatorContractTest {
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
    void facadeKeepsItsPublicMethodFamiliesAndOnlyOneCoordinatorField() {
        List<String> publicMethods = Arrays.stream(
                        StoreHomeTimeoutDiagnostics.class.getDeclaredMethods()
                )
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .map(Method::getName)
                .sorted()
                .toList();
        assertEquals(
                List.of(
                        "recordCandidateActivated",
                        "recordCandidateCatalog",
                        "recordCandidateRejected",
                        "recordCandidateRejectedBeforeAttempt",
                        "recordCandidateStarted",
                        "recordCandidateTimeoutDecision",
                        "recordOperationStarted",
                        "recordOperationTimeoutDecision",
                        "recordProgress",
                        "recordTerminal"
                ),
                publicMethods
        );
        List<Field> fields = Arrays.asList(
                StoreHomeTimeoutDiagnostics.class.getDeclaredFields()
        );
        assertEquals(1, fields.size());
        assertEquals("lifecycleCoordinator", fields.get(0).getName());
        assertEquals(
                StoreHomeTimeoutLifecycleCoordinator.class,
                fields.get(0).getType()
        );
    }

    @Test
    void coordinatorOwnsNoStaticMutableStateOrIndependentLock() {
        assertFalse(Arrays.stream(
                        StoreHomeTimeoutLifecycleCoordinator.class
                                .getDeclaredFields()
                )
                .anyMatch(field -> Modifier.isStatic(field.getModifiers())
                        && !Modifier.isFinal(field.getModifiers())));
        assertFalse(Arrays.stream(
                        StoreHomeTimeoutLifecycleCoordinator.class
                                .getDeclaredMethods()
                )
                .anyMatch(method -> Modifier.isSynchronized(
                        method.getModifiers()
                )));
    }

    @Test
    void facadeIsTheOnlyProductionConsumerOfTheCoordinator()
            throws IOException {
        String facadeRelativePath =
                "lavi/minecraft/diagnostics/container/home/timeout/"
                        + "StoreHomeTimeoutDiagnostics.java";
        Path facadeSource = locate(
                "src/main/java/" + facadeRelativePath
        );
        Path mainRoot = facadeSource;
        for (int remaining = Path.of(facadeRelativePath).getNameCount();
                remaining > 0;
                remaining--) {
            mainRoot = mainRoot.getParent();
        }
        Path sourceRoot = mainRoot;
        List<String> consumers;
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            consumers = files
                    .filter(path -> Files.isRegularFile(path)
                            && path.toString().endsWith(".java"))
                    .filter(path -> source(path).contains(
                            "StoreHomeTimeoutLifecycleCoordinator"
                    ))
                    .map(path -> sourceRoot.relativize(path)
                            .toString()
                            .replace('\\', '/'))
                    .sorted()
                    .toList();
        }
        assertEquals(
                List.of(
                        "lavi/minecraft/diagnostics/container/home/timeout/StoreHomeTimeoutDiagnostics.java",
                        "lavi/minecraft/diagnostics/container/home/timeout/operation/StoreHomeTimeoutLifecycleCoordinator.java"
                ),
                consumers
        );
    }

    @Test
    void rejectionAndTerminalEmitBeforeTheirLifecycleClear()
            throws IOException {
        String coordinator = source(locate(
                "src/main/java/lavi/minecraft/diagnostics/container/home/timeout/operation/StoreHomeTimeoutLifecycleCoordinator.java"
        ));
        String rejection = slice(
                coordinator,
                "public void recordCandidateRejected(",
                "public void recordCandidateActivated("
        );
        assertFalse(rejection.contains("catch ("));
        assertEquals(
                1,
                occurrencesAfter(
                        rejection,
                        "candidateProgress.clearActive();",
                        "candidateBoundaryDiagnostics.emitRejected("
                )
        );

        String terminal = slice(
                coordinator,
                "public void recordTerminal(",
                "private void startCandidate("
        );
        assertFalse(terminal.contains("catch ("));
        assertEquals(
                1,
                occurrencesAfter(
                        terminal,
                        "candidateProgress.clearActive();",
                        "terminalSummaryEmitter.emit("
                )
        );
    }

    @Test
    void strictOffSkipsEngineReadButEligibleRuntimeExceptionPropagates() {
        StoreHomeTimeoutPolicy policy = StoreHomeTimeoutPolicy.standard();
        StoreHomeTimeoutDiagnostics diagnostics = new StoreHomeTimeoutDiagnostics(
                51L,
                policy,
                AutoDepositExactOpenContainerBinding.UNAVAILABLE,
                TestObjects.allocate(HomeStorageTransferExecutor.class)
        );
        StoreHomeCandidateAttempt attempt = attempt(candidate());

        assertDoesNotThrow(() -> diagnostics.recordCandidateStarted(
                null,
                null,
                StoreHomePhase.NAVIGATE_TO_CANDIDATE,
                StoreHomeOperationProgress.start(51L),
                StoreHomeTimeoutObservation.initial(policy),
                null,
                attempt,
                1
        ));

        ChatClefDiagnostics.setBoundaryEnabled(true);
        assertThrows(NullPointerException.class, () ->
                diagnostics.recordCandidateStarted(
                        null,
                        null,
                        StoreHomePhase.NAVIGATE_TO_CANDIDATE,
                        StoreHomeOperationProgress.start(51L),
                        StoreHomeTimeoutObservation.initial(policy),
                        null,
                        attempt,
                        1
                )
        );
    }

    @Test
    void eligibleEngineReadLinkageErrorIsNotConvertedOrSuppressed() {
        StoreHomeTimeoutPolicy policy = StoreHomeTimeoutPolicy.standard();
        StoreHomeTimeoutDiagnostics diagnostics = new StoreHomeTimeoutDiagnostics(
                52L,
                policy,
                new LinkageFailingBinding(),
                new HomeStorageTransferExecutor(
                        new HomeStorageScreenSlotResolver()
                )
        );
        StoreHomeCandidateAttempt attempt = attempt(candidate());
        ChatClefDiagnostics.setBoundaryEnabled(true);

        assertThrows(LinkageError.class, () ->
                diagnostics.recordCandidateStarted(
                        null,
                        null,
                        StoreHomePhase.NAVIGATE_TO_CANDIDATE,
                        StoreHomeOperationProgress.start(52L),
                        StoreHomeTimeoutObservation.initial(policy),
                        null,
                        attempt,
                        1
                )
        );
    }

    private static AutoDepositTrustedDestinationCandidate candidate() {
        return new AutoDepositTrustedDestinationCandidate(
                new AutoDepositTrustedDestination(
                        "test-world",
                        Dimension.OVERWORLD,
                        new BlockPos(1, 64, 0),
                        true
                ),
                0,
                1,
                "test"
        );
    }

    private static StoreHomeCandidateAttempt attempt(
            AutoDepositTrustedDestinationCandidate candidate) {
        StoreHomeCandidateAttempt attempt =
                TestObjects.allocate(StoreHomeCandidateAttempt.class);
        TestObjects.setField(
                attempt,
                StoreHomeCandidateAttempt.class,
                "candidate",
                candidate
        );
        return attempt;
    }

    private static String source(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }

    private static String slice(
            String source,
            String startToken,
            String endToken) {
        int start = source.indexOf(startToken);
        int end = source.indexOf(endToken, start + startToken.length());
        if (start < 0 || end < 0) {
            throw new AssertionError(
                    "Unable to locate source slice: "
                            + startToken + " -> " + endToken
            );
        }
        return source.substring(start, end);
    }

    private static int occurrencesAfter(
            String source,
            String token,
            String boundaryToken) {
        int boundary = source.indexOf(boundaryToken);
        if (boundary < 0) {
            return 0;
        }
        int count = 0;
        int offset = boundary + boundaryToken.length();
        while ((offset = source.indexOf(token, offset)) >= 0) {
            count++;
            offset += token.length();
        }
        return count;
    }

    private static Path locate(String relativePath) throws IOException {
        Path current = Path.of(System.getProperty("user.dir"))
                .toAbsolutePath()
                .normalize();
        while (current != null) {
            Path candidate = current.resolve(relativePath);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IOException("Unable to locate source path: " + relativePath);
    }

    private static final class LinkageFailingBinding
            implements AutoDepositExactOpenContainerBinding,
            AutoDepositExactOpenContainerBindingDiagnosticView {
        @Override
        public Optional<BlockPos> currentExactPosition() {
            return Optional.empty();
        }

        @Override
        public Optional<BlockPos> peekCurrentExactPositionForDiagnostics() {
            throw new LinkageError("test_engine_read_linkage_failure");
        }
    }
}
