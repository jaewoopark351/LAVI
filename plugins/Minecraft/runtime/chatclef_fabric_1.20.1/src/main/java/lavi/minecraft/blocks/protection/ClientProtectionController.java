//#if MC == 12001
package lavi.minecraft.blocks.protection;

import lavi.minecraft.blocks.protection.calculation.ProtectionCalculation;
import lavi.minecraft.blocks.protection.diagnostics.ProtectionPublicationDiagnostics;
import lavi.minecraft.blocks.protection.state.ProtectionSnapshot;
import lavi.minecraft.blocks.scanner.snapshot.BlockLocationSnapshot;
import net.minecraft.util.math.BlockPos;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

//20260913_kpopmodder: Only client callbacks change ownership/calculation; workers read a single immutable publication.
public final class ClientProtectionController {
    private volatile ProtectionSnapshot published = ProtectionSnapshot.unavailable(0);
    private final ProtectionPublicationDiagnostics diagnostics = new ProtectionPublicationDiagnostics();
    private final Set<BlockPos> observedIndicators = new HashSet<>();
    private ProtectionCalculation calculation = new ProtectionCalculation();
    private Object world, player, dimension;
    private long publicationRevision, sourceRevision;

    public ProtectionSnapshot snapshot() { return published; }
    public void sourceFailed() { failed(); diagnostics.observe(published, 0, 0); }
    public void invalidate() {
        world = null; player = null; dimension = null;
        calculation = new ProtectionCalculation();
        observedIndicators.clear();
        published = ProtectionSnapshot.unavailable(++publicationRevision);
        diagnostics.observe(published, 0, 0);
    }
    public void tick(Object currentWorld, Object currentPlayer, Object currentDimension,
                     BlockLocationSnapshot source, Predicate<BlockPos> isIndicator,
                     Predicate<BlockPos> isProtectedType) {
        long started = System.nanoTime();
        if (currentWorld == null || currentPlayer == null) { invalidate(); return; }
        if (world != currentWorld || player != currentPlayer || !java.util.Objects.equals(dimension, currentDimension)) {
            invalidate(); world = currentWorld; player = currentPlayer; dimension = currentDimension;
        }
        if (source == null || source.world() != world || source.player() != player
                || !java.util.Objects.equals(source.dimension(), dimension)) {
            failed(); return;
        }
        try {
            HashSet<BlockPos> indicators = new HashSet<>();
            for (BlockPos pos : source.positions()) {
                if (pos == null) throw new IllegalStateException("null_indicator_source");
                if (isIndicator.test(pos)) indicators.add(pos.toImmutable());
            }
            // A current client block update must survive an older scanner source until that source catches up.
            for (BlockPos pos : Set.copyOf(observedIndicators)) {
                if (isIndicator.test(pos)) indicators.add(pos); else observedIndicators.remove(pos);
            }
            if (world != currentWorld || player != currentPlayer) return;
            sourceRevision = source.revision();
            boolean changed = calculation.indicators(indicators);
            Set<BlockPos> before = calculation.pending();
            if (changed || "UNAVAILABLE".equals(published.status()) || "FAILED".equals(published.status())) publish();
            int visited = calculation.advance(isProtectedType);
            if (world != currentWorld || player != currentPlayer) return;
            if (!before.equals(calculation.pending())) publish();
            diagnostics.observe(published, visited, System.nanoTime() - started);
        } catch (RuntimeException failure) {
            // This is only protection data capture; preserve explicit uncertainty, never claim empty protection.
            failed();
            diagnostics.observe(published, 0, System.nanoTime() - started);
        }
    }
    public void blockChanged(Object sourceWorld, BlockPos pos, boolean protectedType) {
        if (availableForUpdates(sourceWorld) && pos != null && calculation.blockChanged(pos, protectedType)) publish();
    }
    public void indicatorChanged(Object sourceWorld, BlockPos pos, boolean present) {
        if (!availableForUpdates(sourceWorld) || pos == null) return;
        if (present) observedIndicators.add(pos.toImmutable()); else observedIndicators.remove(pos);
        if (calculation.indicatorChanged(pos, present)) publish();
    }
    public void chunkChanged(Object sourceWorld, int x, int z) {
        if (!availableForUpdates(sourceWorld)) return;
        calculation.chunkChanged(x, z);
        publish();
    }
    private void publish() {
        Set<BlockPos> pending = calculation.pending();
        published = new ProtectionSnapshot(world, player, dimension, sourceRevision, ++publicationRevision,
                pending.isEmpty() ? "READY" : "PREPARING", calculation.completeBlocks(), pending);
    }
    private void failed() {
        calculation = new ProtectionCalculation();
        published = new ProtectionSnapshot(world, player, dimension, sourceRevision, ++publicationRevision,
                "FAILED", Set.of(), Set.of());
        diagnostics.observe(published, 0, 0);
    }
    private boolean availableForUpdates(Object sourceWorld) {
        return sourceWorld == world && !"FAILED".equals(published.status()) && !"UNAVAILABLE".equals(published.status());
    }
}

//#endif
