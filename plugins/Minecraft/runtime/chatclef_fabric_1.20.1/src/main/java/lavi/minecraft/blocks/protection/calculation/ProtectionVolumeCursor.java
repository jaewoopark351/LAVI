//#if MC == 12001
package lavi.minecraft.blocks.protection.calculation;

import net.minecraft.util.math.BlockPos;

//20260913_kpopmodder: Inclusive ±16 policy visits exactly 33 cubed coordinates without restarting on tracker dirtiness.
public final class ProtectionVolumeCursor {
    public static final int VOLUME = 33 * 33 * 33;
    private final BlockPos marker;
    private int index;
    public ProtectionVolumeCursor(BlockPos marker) { this.marker = marker.toImmutable(); }
    public BlockPos marker() { return marker; }
    public boolean hasNext() { return index < VOLUME; }
    public BlockPos next() {
        if (!hasNext()) throw new IllegalStateException("volume_complete");
        int n = index++;
        return marker.add(n / (33 * 33) - 16, (n / 33) % 33 - 16, n % 33 - 16);
    }
}

//#endif
