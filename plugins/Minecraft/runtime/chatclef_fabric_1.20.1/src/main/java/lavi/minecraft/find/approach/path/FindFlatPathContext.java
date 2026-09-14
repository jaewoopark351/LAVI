//#if MC == 12001
//$$ package lavi.minecraft.find.approach.path;
//$$
//$$ import java.util.Set;
//$$ import baritone.api.IBaritone;
//$$ import baritone.pathing.movement.CalculationContext;
//$$ import net.minecraft.block.Block;
//$$ import net.minecraft.block.BlockState;
//$$ import net.minecraft.block.Blocks;
//$$ import net.minecraft.util.math.BlockPos;
//$$
//$$ //20260914_kpopmodder: A native calculation context exposes only captured safe flat cells, with finite reads.
//$$ public final class FindFlatPathContext extends CalculationContext {
//$$     public static final int MAX_READS = 50000;
//$$     private final Set<Long> cells;
//$$     private final int floorY;
//$$     private int reads;
//$$
//$$     public FindFlatPathContext(IBaritone baritone, Set<Long> cells, int floorY) {
//$$         super(baritone, false);
//$$         this.cells = Set.copyOf(cells); this.floorY = floorY;
//$$     }
//$$     @Override public BlockState get(int x, int y, int z) {
//$$         if (++reads > MAX_READS) throw new IllegalStateException("find_native_planner_read_limit");
//$$         if (!cells.contains(BlockPos.asLong(x, floorY, z))) return Blocks.BARRIER.getDefaultState();
//$$         if (y == floorY - 1) return Blocks.STONE.getDefaultState();
//$$         if (y >= floorY && y <= floorY + 2) return Blocks.AIR.getDefaultState();
//$$         return Blocks.BARRIER.getDefaultState();
//$$     }
//$$     @Override public BlockState get(BlockPos position) { return get(position.getX(), position.getY(), position.getZ()); }
//$$     @Override public Block getBlock(int x, int y, int z) { return get(x, y, z).getBlock(); }
//$$     @Override public boolean isLoaded(int x, int z) { return cells.contains(BlockPos.asLong(x, floorY, z)); }
//$$     @Override public double costOfPlacingAt(int x, int y, int z, BlockState state) { return 1_000_000; }
//$$     @Override public double breakCostMultiplierAt(int x, int y, int z, BlockState state) { return 1_000_000; }
//$$     public int reads() { return reads; }
//$$ }
//#endif
