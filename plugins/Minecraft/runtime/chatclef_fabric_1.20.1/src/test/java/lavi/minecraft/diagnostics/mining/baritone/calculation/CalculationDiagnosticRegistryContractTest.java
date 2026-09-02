package lavi.minecraft.diagnostics.mining.baritone.calculation;

import baritone.api.pathing.calc.IPath;
import baritone.pathing.calc.AStarPathFinder;
import baritone.pathing.calc.AbstractNodeCostSearch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260902_kpopmodder: Characterize the single-owner generation, thread-local, cleanup, and capacity contracts.
@ResourceLock("CalculationDiagnosticRegistry")
class CalculationDiagnosticRegistryContractTest {
    private static final int RECORD_HARD_CAP = 1024;

    private ConcurrentMap<Integer, CalculationDiagnosticRecord> records;
    private ConcurrentMap<Integer, CalculationDiagnosticRecord> pathRecords;

    @BeforeEach
    void clearRegistryState() throws ReflectiveOperationException {
        records = registryMap("RECORDS");
        pathRecords = registryMap("PATH_RECORDS");
        records.clear();
        pathRecords.clear();
        CalculationDiagnosticRegistry.clearActive();
    }

    @AfterEach
    void releaseRegistryState() {
        CalculationDiagnosticRegistry.clearActive();
        records.clear();
        pathRecords.clear();
    }

    @Test
    void reusesOneGenerationUntilCompletionThenIssuesANewerGeneration() throws ReflectiveOperationException {
        AbstractNodeCostSearch pathfinder = newPathfinder();

        CalculationDiagnosticRecord first = CalculationDiagnosticRegistry.recordFor(pathfinder);
        CalculationDiagnosticRecord repeated = CalculationDiagnosticRegistry.recordFor(pathfinder);

        assertSame(first, repeated);

        CalculationDiagnosticRegistry.complete(pathfinder, first);
        CalculationDiagnosticRecord afterCompletion = CalculationDiagnosticRegistry.recordFor(pathfinder);

        assertTrue(afterCompletion.generationId > first.generationId);
    }

    @Test
    void activeCalculationIsThreadLocalAndExplicitlyCleared() throws Exception {
        CalculationDiagnosticRecord record = CalculationDiagnosticRegistry.recordFor(newPathfinder());
        CalculationDiagnosticRegistry.activate(record);

        assertSame(record, CalculationDiagnosticRegistry.activeRecordForPath(null));

        AtomicReference<CalculationDiagnosticRecord> otherThreadObservation = new AtomicReference<>();
        AtomicReference<Throwable> observerFailure = new AtomicReference<>();
        CountDownLatch observerCompleted = new CountDownLatch(1);
        Thread observer = new Thread(
                () -> {
                    try {
                        otherThreadObservation.set(CalculationDiagnosticRegistry.activeRecordForPath(null));
                    } catch (Throwable throwable) {
                        observerFailure.set(throwable);
                    } finally {
                        observerCompleted.countDown();
                    }
                },
                "calculation-registry-contract-observer"
        );
        observer.setDaemon(true);
        observer.start();
        assertTrue(observerCompleted.await(5, TimeUnit.SECONDS), "observer did not complete");
        observer.join(1000L);

        assertFalse(observer.isAlive(), "observer thread remained alive");
        assertNull(observerFailure.get(), () -> "observer failed: " + observerFailure.get());
        assertNull(otherThreadObservation.get());

        CalculationDiagnosticRegistry.clearActive();
        assertNull(CalculationDiagnosticRegistry.activeRecordForPath(null));
    }

    @Test
    void completionRemovesEveryPathIdentityOwnedByTheRecord() throws ReflectiveOperationException {
        AbstractNodeCostSearch pathfinder = newPathfinder();
        CalculationDiagnosticRecord record = CalculationDiagnosticRegistry.recordFor(pathfinder);
        IPath rawPath = newPath();
        IPath postProcessedPath = newPath();
        IPath resultPath = newPath();
        record.rawPath = rawPath;
        record.postProcessedPath = postProcessedPath;
        record.resultPath = resultPath;
        CalculationDiagnosticRegistry.bindPath(rawPath, record);
        CalculationDiagnosticRegistry.bindPath(postProcessedPath, record);
        CalculationDiagnosticRegistry.bindPath(resultPath, record);
        CalculationDiagnosticRegistry.clearActive();

        assertSame(record, CalculationDiagnosticRegistry.activeRecordForPath(rawPath));
        assertSame(record, CalculationDiagnosticRegistry.activeRecordForPath(postProcessedPath));
        assertSame(record, CalculationDiagnosticRegistry.activeRecordForPath(resultPath));

        CalculationDiagnosticRegistry.complete(pathfinder, record);

        assertNull(CalculationDiagnosticRegistry.activeRecordForPath(rawPath));
        assertNull(CalculationDiagnosticRegistry.activeRecordForPath(postProcessedPath));
        assertNull(CalculationDiagnosticRegistry.activeRecordForPath(resultPath));
        assertTrue(records.isEmpty());
    }

    @Test
    void recordOverflowClearsBothIdentityIndexesBeforeAdmittingANewGeneration()
            throws ReflectiveOperationException {
        CalculationDiagnosticRecord filler = new CalculationDiagnosticRecord(-1, null);
        for (int index = 1; index <= RECORD_HARD_CAP + 1; index++) {
            records.put(index, filler);
        }
        pathRecords.put(1, filler);
        AbstractNodeCostSearch admittedPathfinder = newPathfinder();

        CalculationDiagnosticRecord admitted = CalculationDiagnosticRegistry.recordFor(admittedPathfinder);

        assertEquals(1, records.size());
        assertSame(admitted, records.get(System.identityHashCode(admittedPathfinder)));
        assertTrue(pathRecords.isEmpty());
    }

    @Test
    void pathOverflowClearsOnlyPathBindingsAndPreservesGenerationRecords()
            throws ReflectiveOperationException {
        AbstractNodeCostSearch pathfinder = newPathfinder();
        CalculationDiagnosticRecord record = CalculationDiagnosticRegistry.recordFor(pathfinder);
        for (int index = 1; index <= RECORD_HARD_CAP + 1; index++) {
            pathRecords.put(index, record);
        }
        IPath admittedPath = newPath();

        CalculationDiagnosticRegistry.bindPath(admittedPath, record);

        assertEquals(1, pathRecords.size());
        assertSame(record, CalculationDiagnosticRegistry.activeRecordForPath(admittedPath));
        assertSame(record, records.get(System.identityHashCode(pathfinder)));
    }

    private static AbstractNodeCostSearch newPathfinder() throws ReflectiveOperationException {
        return (AbstractNodeCostSearch) unsafe().allocateInstance(AStarPathFinder.class);
    }

    private static IPath newPath() {
        return (IPath) Proxy.newProxyInstance(
                IPath.class.getClassLoader(),
                new Class<?>[]{IPath.class},
                (proxy, method, arguments) -> null
        );
    }

    @SuppressWarnings("unchecked")
    private static ConcurrentMap<Integer, CalculationDiagnosticRecord> registryMap(String fieldName)
            throws ReflectiveOperationException {
        Field field = CalculationDiagnosticRegistry.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (ConcurrentMap<Integer, CalculationDiagnosticRecord>) field.get(null);
    }

    private static Unsafe unsafe() throws ReflectiveOperationException {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    }
}
