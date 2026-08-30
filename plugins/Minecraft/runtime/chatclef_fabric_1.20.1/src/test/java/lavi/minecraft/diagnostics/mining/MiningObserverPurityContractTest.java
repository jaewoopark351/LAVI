package lavi.minecraft.diagnostics.mining;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260830_kpopmodder: Lock diagnostics-only purity and upstream call-count preservation as source contracts.
class MiningObserverPurityContractTest {
    @Test
    void observerDoesNotReinvokeFinishEqualityBreakabilityOrProgressBehavior() throws IOException {
        String closest = source(
                "src/main/java/adris/altoclef/tasks/AbstractDoToClosestObjectTask.java");
        assertFalse(closest.contains("goalTask.isFinished()"));

        String goalRequest = source(
                "src/main/java/lavi/minecraft/diagnostics/mining/MineTargetGoalRequestDiagnostics.java");
        assertFalse(goalRequest.contains("getBlockState("));
        assertFalse(goalRequest.contains("WorldHelper.canBreak("));
        assertTrue(goalRequest.contains("EXISTING_GET_GOAL_TASK_LOCAL"));
        assertEquals(1, occurrences(goalRequest, "getBlockScanner().isUnreachable(target)"));
        assertOrdered(goalRequest, "String scannerUnreachableBefore", "String fingerprint");
        assertTrue(slice(goalRequest, "String fingerprint", "MiningDiagnosticEmitter.emitLazy", 0)
                .contains("scannerUnreachableBefore"));

        String reconciliationSnapshot = source(
                "src/main/java/lavi/minecraft/diagnostics/mining/reconciliation/ReconciliationTaskSnapshot.java");
        assertFalse(reconciliationSnapshot.contains("WorldHelper.canBreak("));
        String navigation = source(
                "src/main/java/lavi/minecraft/diagnostics/mining/DestroyNavigationDiagnostics.java");
        assertFalse(navigation.contains("WorldHelper.canBreak("));
        String selection = source(
                "src/main/java/lavi/minecraft/diagnostics/mining/MineTargetSelectionDiagnostics.java");
        assertFalse(selection.contains("WorldHelper.canBreak("));

        String progressObserver = source(
                "src/main/java/lavi/minecraft/diagnostics/mining/MovementProgressDiagnostics.java");
        assertFalse(progressObserver.contains(".isPathing()"));
        assertFalse(progressObserver.contains(".reset()"));
        assertFalse(progressObserver.contains(".check("));

        String finishObserver = source(
                "src/main/java/lavi/minecraft/diagnostics/mining/DestroyFinishEvaluationDiagnostics.java");
        assertFalse(finishObserver.contains("getBlockState("));
        assertFalse(finishObserver.contains(".isFinished()"));
        assertFalse(finishObserver.contains("DestroyBlockDiagnosticState.getOrCreate"));
        assertFalse(finishObserver.contains("MineOrCollectTask"));
        assertTrue(finishObserver.contains("exactDiagnosticProbeEvidence = false"));
        assertTrue(finishObserver.contains("runIdIfPresent"));
    }

    @Test
    void cheapGatePrecedesDeepReconciliationAndPhaseDetail() throws IOException {
        String miningFacade = source(
                "src/main/java/lavi/minecraft/diagnostics/mining/MiningPathDiagnostics.java");
        String captureFacade = slice(miningFacade, "capturePreCancelBaritoneState",
                "logTaskChildReconciliation", 0);
        assertFalse(captureFacade.contains("BaritonePathDiagnosticSnapshot.capture"));
        assertTrue(captureFacade.contains("PreCancelBaritoneState.capture"));

        String reconciliation = source(
                "src/main/java/lavi/minecraft/diagnostics/mining/TaskChildReconciliationDiagnostics.java");
        assertOrdered(reconciliation, "ReconciliationTaskIdentity.capture", "MiningDiagnosticEmitter.emitLazy");
        assertOrdered(reconciliation, "MiningDiagnosticEmitter.emitLazy", "ReconciliationTaskSnapshot.capture");

        String phase = source(
                "src/main/java/lavi/minecraft/diagnostics/mining/DestroyBlockPhaseDiagnostics.java");
        assertOrdered(phase, "if (normalizedPhase.equals(previousPhase))",
                "BaritonePathDiagnosticSnapshot.capture");

        String orchestrator = source(
                "src/main/java/lavi/minecraft/diagnostics/mining/emission/MiningDiagnosticEmissionOrchestrator.java");
        assertOrdered(orchestrator, "MiningDiagnosticEventGate.evaluate", "eventFieldsSupplier.get()");
        assertFalse(orchestrator.contains("logBoundary(\"MINING_DIAGNOSTIC_GATE_EXHAUSTED\""));
    }

    @Test
    void upstreamCheckCountsAndDestroyFinishSingleReadRemainStable() throws IOException {
        String mine = source("src/main/java/adris/altoclef/tasks/resources/MineAndCollectTask.java");
        int mineClass = mine.indexOf("public static class MineOrCollectTask");
        String parentOnTick = slice(mine, "protected Task onTick()", "protected Task getGoalTask", mineClass);
        assertEquals(1, occurrences(parentOnTick, "progressChecker.check(mod)"));
        assertEquals(1, occurrences(parentOnTick,
                "mod.getClientBaritone().getPathingBehavior().isPathing()"));
        assertFalse(mine.contains("mine_or_collect_return_destroy_block_task"));

        String destroy = source("src/main/java/adris/altoclef/tasks/construction/DestroyBlockTask.java");
        String destroyOnTick = slice(destroy, "protected Task onTick()", "protected void onStop", 0);
        assertEquals(2, occurrences(destroyOnTick, "_moveChecker.check(mod)"));
        assertEquals(1, occurrences(destroyOnTick, "stuckCheck.check(mod)"));

        String finish = slice(destroy, "public boolean isFinished()", "protected boolean isEqual", 0);
        assertEquals(1, occurrences(finish, "getBlockState(pos)"));
        assertEquals(1, occurrences(finish, "blockState.isAir()"));
        assertTrue(finish.contains("logDestroyFinishEvaluation(mod, this, pos, blockState, isAir)"));
        assertTrue(finish.contains("return isAir"));
        assertFalse(finish.contains("Debug.logInternal"));
        assertFalse(finish.contains("VisibleTaskDiagnostics"));
    }

    @Test
    void miningDiagnosticsTreeContainsNoDiagnosticCanBreakProbeOrHundredTickHeartbeat() throws IOException {
        Path miningRoot = locate("src/main/java/lavi/minecraft/diagnostics/mining");
        try (Stream<Path> files = Files.walk(miningRoot)) {
            String allSources = files
                    .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"))
                    .map(MiningObserverPurityContractTest::readUnchecked)
                    .reduce("", (left, right) -> left + "\n" + right);
            assertFalse(allSources.contains("WorldHelper.canBreak("));
            assertFalse(allSources.contains("NO_PROGRESS_HEARTBEAT_100"));
            assertFalse(allSources.contains("no_progress_heartbeat_100"));
            assertTrue(allSources.contains("NO_PROGRESS_HEARTBEAT_200_TICKS_OR_10_SECONDS"));
            assertTrue(allSources.contains("DIAGNOSTIC_SESSION_CAP_REACHED"));
        }
    }

    @Test
    void semanticFingerprintsAndTerminalReserveFollowTheBoundedContract() throws IOException {
        String reconciliation = source(
                "src/main/java/lavi/minecraft/diagnostics/mining/reconciliation/ReconciliationSemanticFingerprint.java");
        assertTrue(reconciliation.contains("candidateChild.taskClass()"));
        assertTrue(reconciliation.contains("candidateChild.targetPositionText()"));
        assertFalse(reconciliation.contains("candidateChild.taskInstanceId()"));

        String lifetime = source(
                "src/main/java/lavi/minecraft/diagnostics/mining/DestroyBlockLifetimeDiagnostics.java");
        assertTrue(lifetime.contains("if (terminal)"));
        assertTrue(lifetime.contains("MiningDiagnosticEmitter.emitReservedLazy"));
        assertTrue(lifetime.contains("MiningDiagnosticEmitter.emitLazy"));
        String cancel = source(
                "src/main/java/lavi/minecraft/diagnostics/mining/ExistingCancelBoundaryDiagnostics.java");
        assertFalse(cancel.contains("emitReservedLazy"));

        String correlation = source(
                "src/main/java/lavi/minecraft/diagnostics/mining/correlation/MiningDiagnosticCorrelation.java");
        assertTrue(correlation.contains("\"storeOperationId\""));
        assertTrue(correlation.contains("\"UNAVAILABLE\""));
        assertFalse(correlation.contains("StoreDeposit"));
    }

    private static String slice(String source, String startToken, String endToken, int fromIndex) {
        int start = source.indexOf(startToken, fromIndex);
        int end = source.indexOf(endToken, start + startToken.length());
        if (start < 0 || end < 0) {
            throw new AssertionError("Unable to locate source slice: " + startToken + " -> " + endToken);
        }
        return source.substring(start, end);
    }

    private static void assertOrdered(String source, String firstToken, String secondToken) {
        int first = source.indexOf(firstToken);
        int second = source.indexOf(secondToken);
        assertTrue(first >= 0, "Missing source token: " + firstToken);
        assertTrue(second >= 0, "Missing source token: " + secondToken);
        assertTrue(first < second, "Expected source order: " + firstToken + " before " + secondToken);
    }

    private static int occurrences(String text, String token) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
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
