package lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan;

import net.minecraft.util.math.BlockPos;

import java.util.Optional;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
public interface AutoDepositBulkWorldView {
    Optional<AutoDepositBulkWorldProvenance> provenance();

    Optional<AutoDepositBulkBuildHeight> buildHeight();

    boolean isChunkLoaded(int chunkX, int chunkZ);

    Optional<AutoDepositBulkBlockObservation> observeLoaded(BlockPos position);
}
