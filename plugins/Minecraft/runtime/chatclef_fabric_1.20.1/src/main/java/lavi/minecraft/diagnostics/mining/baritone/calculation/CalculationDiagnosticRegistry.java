package lavi.minecraft.diagnostics.mining.baritone.calculation;

import baritone.api.pathing.calc.IPath;
import baritone.pathing.calc.AbstractNodeCostSearch;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

//20260730_kpopmodder: Added diagnostic logging to prove the Carry On interaction failure boundary.
final class CalculationDiagnosticRegistry {
    private static final int RECORD_HARD_CAP = 1024;
    private static final AtomicLong NEXT_GENERATION = new AtomicLong(1);
    private static final ConcurrentMap<Integer, CalculationDiagnosticRecord> RECORDS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Integer, CalculationDiagnosticRecord> PATH_RECORDS = new ConcurrentHashMap<>();
    private static final ThreadLocal<CalculationDiagnosticRecord> ACTIVE_CALCULATION = new ThreadLocal<>();

    private CalculationDiagnosticRegistry() {
    }

    static CalculationDiagnosticRecord recordFor(AbstractNodeCostSearch pathfinder) {
        if (RECORDS.size() > RECORD_HARD_CAP) {
            RECORDS.clear();
            PATH_RECORDS.clear();
        }
        return RECORDS.computeIfAbsent(System.identityHashCode(pathfinder),
                ignored -> new CalculationDiagnosticRecord(NEXT_GENERATION.getAndIncrement(), pathfinder));
    }

    static void activate(CalculationDiagnosticRecord record) {
        ACTIVE_CALCULATION.set(record);
    }

    static void clearActive() {
        ACTIVE_CALCULATION.remove();
    }

    static CalculationDiagnosticRecord activeRecordForPath(Object path) {
        CalculationDiagnosticRecord active = ACTIVE_CALCULATION.get();
        if (active != null) {
            return active;
        }
        if (path == null) {
            return null;
        }
        return PATH_RECORDS.get(System.identityHashCode(path));
    }

    static void bindPath(Object path, CalculationDiagnosticRecord record) {
        if (path == null || record == null) {
            return;
        }
        if (PATH_RECORDS.size() > RECORD_HARD_CAP) {
            PATH_RECORDS.clear();
        }
        PATH_RECORDS.put(System.identityHashCode(path), record);
    }

    static void complete(AbstractNodeCostSearch pathfinder, CalculationDiagnosticRecord record) {
        removePath(record.rawPath);
        removePath(record.postProcessedPath);
        removePath(record.resultPath);
        RECORDS.remove(System.identityHashCode(pathfinder), record);
    }

    private static void removePath(IPath path) {
        if (path != null) {
            PATH_RECORDS.remove(System.identityHashCode(path));
        }
    }
}
