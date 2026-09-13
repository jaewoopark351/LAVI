package adris.altoclef.trackers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Streams;

import adris.altoclef.AltoClef;
import lavi.minecraft.diagnostics.container.store.deposit.StoreDepositDiagnostics;
import lavi.minecraft.diagnostics.blocks.collection.BlockCollectionDiagnostics;
import lavi.minecraft.diagnostics.blocks.collection.state.BlockCollectionReadOrigin;
import adris.altoclef.util.helpers.BaritoneHelper;
import adris.altoclef.util.helpers.ItemHelper;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
//#if MC == 12001
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.World;
import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.events.ChunkLoadEvent;
import adris.altoclef.eventbus.events.ChunkUnloadEvent;
import adris.altoclef.util.helpers.WorldHelper;
import lavi.minecraft.blocks.protection.ClientProtectionController;
//#endif

public class UserBlockRangeTracker extends Tracker {

    // TODO: Config

    final int AVOID_BREAKING_RANGE = 16;

    final Block[] USER_INDICATOR_BLOCKS = Streams.concat(
        Arrays.stream(ItemHelper.itemsToBlocks(ItemHelper.BED))
        // maybe add these in later, no need
        // Arrays.asList(Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.FLETCHING_TABLE, Blocks.ANVIL).stream()
    ).toArray(Block[]::new);

    final Block[] USER_BLOCKS_TO_AVOID_BREAKING = Streams.concat(
        Arrays.asList(Blocks.COBBLESTONE).stream(),
        Arrays.stream(ItemHelper.itemsToBlocks(ItemHelper.LOG))
    ).toArray(Block[]::new);

//#if MC == 12001
    //20260913_kpopmodder: Game protection state is instance owned and independent of diagnostic availability.
    private final ClientProtectionController protection = new ClientProtectionController();
    private final Set<Block> indicatorTypes = Set.copyOf(Arrays.asList(USER_INDICATOR_BLOCKS));
    private final Set<Block> protectedTypes = Set.copyOf(Arrays.asList(USER_BLOCKS_TO_AVOID_BREAKING));
//#else
//$$     private final Set<BlockPos> _dontBreakBlocks = new HashSet<>();
//#endif

    public UserBlockRangeTracker(TrackerManager manager) {
        super(manager);
//#if MC == 12001
        EventBus.subscribe(ChunkLoadEvent.class, event -> {
            if (event.chunk != null && MinecraftClient.getInstance().isOnThread())
                protection.chunkChanged(mod.getWorld(), event.chunk.getPos().x, event.chunk.getPos().z);
        });
        EventBus.subscribe(ChunkUnloadEvent.class, event -> {
            if (MinecraftClient.getInstance().isOnThread()) protection.chunkChanged(mod.getWorld(), event.chunkPos.x, event.chunkPos.z);
        });
//#endif
    }

    public boolean isNearUserTrackedBlock(BlockPos pos) {
//#if MC == 12001
        //20260913_kpopmodder: Never ensureUpdated or read live world state from a path calculation worker.
        return protection.snapshot().avoids(pos);
    }

    public void onClientTick() {
        if (!MinecraftClient.getInstance().isOnThread()) throw new IllegalStateException("client_thread_required");
        var world = mod.getWorld();
        if (world == null || mod.getPlayer() == null) { protection.invalidate(); return; }
        lavi.minecraft.blocks.scanner.snapshot.BlockLocationSnapshot source;
        try { source = mod.getBlockScanner().snapshotLocations(USER_INDICATOR_BLOCKS); }
        catch (RuntimeException failure) {
            protection.sourceFailed();
            return;
        }
        protection.tick(world, mod.getPlayer(), WorldHelper.getCurrentDimension(), source,
                pos -> indicatorTypes.contains(world.getBlockState(pos).getBlock()),
                pos -> protectedTypes.contains(world.getBlockState(pos).getBlock()));
    }

    public void onBlockChanged(World world, BlockPos pos, BlockState state) {
        if (MinecraftClient.getInstance().isOnThread() && world == mod.getWorld()) {
            boolean marker = indicatorTypes.contains(state.getBlock());
            if (marker) mod.getBlockScanner().addBlock(state.getBlock(), pos);
            protection.indicatorChanged(world, pos, marker);
            protection.blockChanged(world, pos, protectedTypes.contains(state.getBlock()));
//#else
//$$         ensureUpdated();
//$$         synchronized (BaritoneHelper.MINECRAFT_LOCK) {
//$$             return _dontBreakBlocks.contains(pos);
//#endif
        }
    }

    @Override
    protected void updateState() {
//#if MC == 12001
        //20260913_kpopmodder: Disabled former lazy clear/scan/add: immutable publication is advanced only by onClientTick.
        //20260913_kpopmodder: Former readOrigin/null-consumer diagnostic boundary is replaced by checked source snapshot capture.
//#else
//$$         _dontBreakBlocks.clear();
//$$         List<BlockPos> userBlocks = AltoClef.getInstance().getBlockScanner().getKnownLocationsIncludeUnreachable(USER_INDICATOR_BLOCKS);
//$$         //20260913_kpopmodder: Bind the returned list without querying or filtering its contents for diagnostics.
//$$         BlockCollectionReadOrigin readOrigin = BlockCollectionDiagnostics.readOrigin(userBlocks);

//$$         Set<Block> userIndicatorBlocks = new HashSet<>(Arrays.asList(USER_INDICATOR_BLOCKS));
//$$         Set<Block> userBlocksToAvoidMining = new HashSet<>(Arrays.asList(USER_BLOCKS_TO_AVOID_BREAKING));

//$$         // filter out user blocks
//$$         // TODO: for some reason we haven't been validating in the world for block tracking... so we do it manually.
//$$         //      would "fixing" it cause problems?
//$$         userBlocks.removeIf(bpos -> {
//$$             if (bpos == null) {
//$$                 BlockCollectionDiagnostics.nullConsumed(readOrigin, this);
//$$                 StoreDepositDiagnostics.logUserBlockRangeNullInput(this, null);
//$$             }
//$$             Block b = AltoClef.getInstance().getWorld().getBlockState(bpos).getBlock();
//$$             return !userIndicatorBlocks.contains(b);
//$$         });


//$$         for (BlockPos userBlockPos : userBlocks) {

//$$             BlockPos min = userBlockPos.add(-AVOID_BREAKING_RANGE, -AVOID_BREAKING_RANGE, -AVOID_BREAKING_RANGE);
//$$             BlockPos max = userBlockPos.add(AVOID_BREAKING_RANGE, AVOID_BREAKING_RANGE, AVOID_BREAKING_RANGE);

//$$             // Range
//$$             for (BlockPos possible : adris.altoclef.util.helpers.WorldHelper.scanRegion(min, max)) {
//$$                 Block b = AltoClef.getInstance().getWorld().getBlockState(possible).getBlock();
//$$                 if (userBlocksToAvoidMining.contains(b)) {
//$$                     _dontBreakBlocks.add(possible);
//$$                 }
//$$             }
//$$         }
//#endif
    }

    @Override
//#if MC == 12001
    protected void reset() { protection.invalidate(); }
//#else
//$$     protected void reset() {
//$$         _dontBreakBlocks.clear();
//$$     }

//#endif
}
