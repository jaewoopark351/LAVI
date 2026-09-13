package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import adris.altoclef.eventbus.EventBus;
import adris.altoclef.eventbus.events.BlockPlaceEvent;
import adris.altoclef.multiversion.blockpos.BlockPosVer;
import adris.altoclef.trackers.blacklisting.WorldLocateBlacklist;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.helpers.BaritoneHelper;
import adris.altoclef.util.helpers.WorldHelper;
import adris.altoclef.util.time.TimerGame;
import lavi.minecraft.diagnostics.blocks.collection.BlockCollectionDiagnostics;
import lavi.minecraft.diagnostics.blocks.collection.state.BlockCollectionOperation;
import lavi.minecraft.diagnostics.blocks.collection.state.BlockCollectionToken;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.util.*;
//#if MC == 12001
import lavi.minecraft.blocks.scanner.state.ScanRunLease;
import lavi.minecraft.blocks.scanner.state.ScanRunCompletion;
import lavi.minecraft.blocks.scanner.snapshot.ScanResultSnapshot;
import lavi.minecraft.blocks.scanner.diagnostics.ScannerLifecycleDiagnostics;
import lavi.minecraft.blocks.scanner.state.ScanWorldBinding;
import lavi.minecraft.blocks.scanner.state.ScanChunkRequest;
import lavi.minecraft.blocks.scanner.snapshot.ScanChunkSnapshot;
import lavi.minecraft.blocks.scanner.snapshot.ClientChunkSnapshotCapture;
import lavi.minecraft.blocks.scanner.snapshot.BlockLocationSnapshot;
//#endif
import java.util.function.Predicate;

public class BlockScanner {

    private static final boolean LOG = false;
    private static final int RESCAN_TICK_DELAY = 4 * 20;
    private static final int CACHED_POSITIONS_PER_BLOCK = 40;


    private final AltoClef mod;
    private final TimerGame rescanTimer = new TimerGame(1);

    private final HashMap<Block, HashSet<BlockPos>> trackedBlocks = new HashMap<>();
    private final HashMap<Block, HashSet<BlockPos>> scannedBlocks = new HashMap<>();
    private final HashMap<ChunkPos, Long> scannedChunks = new HashMap<>();
    private final WorldLocateBlacklist blacklist = new WorldLocateBlacklist();
    // used while scanning
    private HashMap<Block, HashSet<BlockPos>> cachedScannedBlocks = new HashMap<>();
    private Dimension scanDimension = Dimension.OVERWORLD;
    private World scanWorld = null;

//#if MC == 12001
    //20260913_kpopmodder: All shared index writers, copies, world replacement and run publication use one lock.
    private final Object collectionLock = new Object();
    private Object scanPlayer;
    private long contentRevision;
    private long lifetimeGeneration;
    private final Set<BlockPos> unreachablePositions = new HashSet<>();
    private ScanRunLease activeRun;
    private boolean runCompletionPending;
    private ScanRunCompletion completedRun;
    private final ScannerLifecycleDiagnostics lifecycleDiagnostics = new ScannerLifecycleDiagnostics();

//#else
//$$     private boolean scanning = false;
//$$     private boolean forceStop = false;
//#endif


    public BlockScanner(AltoClef mod) {
        this.mod = mod;

        EventBus.subscribe(BlockPlaceEvent.class, evt -> addBlock(evt.blockState.getBlock(), evt.blockPos));
    }


    public void addBlock(Block block, BlockPos pos) {
//#if MC == 12001
        synchronized (collectionLock) {
//#endif
        if (!isBlockAtPosition(pos, block)) {
            Debug.logInternal("INVALID SET: " + block + " " + pos);
            return;
        }

        if (trackedBlocks.containsKey(block)) {
            //20260913_kpopmodder: Observe the existing native write without serializing it with readers.
            HashSet<BlockPos> target = trackedBlocks.get(block);
            BlockCollectionToken write = BlockCollectionDiagnostics.begin(this, trackedBlocks, target,
                    BlockCollectionOperation.WRITE_ADD, "BlockScanner.addBlock.existing.add");
            boolean written = false;
            try {
//#if MC == 12001
                if (target.add(pos.toImmutable())) contentRevision++;
//#else
//$$                 target.add(pos);
//#endif
                written = true;
            } finally {
                BlockCollectionDiagnostics.end(write, written, null);
            }
        } else {
            HashSet<BlockPos> set = new HashSet<>();
//#if MC == 12001
            set.add(pos.toImmutable());
//#else
//$$             set.add(pos);
//#endif

            BlockCollectionToken write = BlockCollectionDiagnostics.begin(this, trackedBlocks, set,
                    BlockCollectionOperation.WRITE_PUT, "BlockScanner.addBlock.new.put");
            boolean written = false;
            try {
                trackedBlocks.put(block, set);
//#if MC == 12001
                contentRevision++;
//#endif
                written = true;
            } finally {
                BlockCollectionDiagnostics.end(write, written, null);
            }
        }
//#if MC == 12001

        }
//#endif
    }


    public void requestBlockUnreachable(BlockPos pos, int allowedFailures) {
//#if MC == 12001
        synchronized (collectionLock) {
//#endif
        blacklist.blackListItem(mod, pos, allowedFailures);
//#if MC == 12001
        if (blacklist.unreachable(pos)) unreachablePositions.add(pos.toImmutable()); else unreachablePositions.remove(pos);

        }
//#endif
    }

    //TODO replace four with config
    public void requestBlockUnreachable(BlockPos pos) {
        blacklist.blackListItem(mod, pos, 4);
    }


    public boolean isUnreachable(BlockPos pos) {
//#if MC == 12001
        synchronized (collectionLock) {
//#endif
        return blacklist.unreachable(pos);
//#if MC == 12001

        }
//#endif
    }

    public List<BlockPos> getKnownLocationsIncludeUnreachable(Block... blocks) {
//#if MC == 12001
        synchronized (collectionLock) {
//#endif
        List<BlockPos> locations = new LinkedList<>();
        //20260913_kpopmodder: Keep the original collection copy and exception; retain only its read interval.
        BlockCollectionToken query = BlockCollectionDiagnostics.begin(this, trackedBlocks, null,
                BlockCollectionOperation.READ_QUERY, "BlockScanner.getKnownLocationsIncludeUnreachable");
        boolean returnedNormally = false;
        try {
            for (Block block : blocks) {
                if (!trackedBlocks.containsKey(block)) continue;
                HashSet<BlockPos> source = trackedBlocks.get(block);
                BlockCollectionToken copy = BlockCollectionDiagnostics.begin(this, trackedBlocks, source,
                        BlockCollectionOperation.READ_COPY, "BlockScanner.getKnownLocationsIncludeUnreachable.addAll");
                boolean copied = false;
                try {
                    locations.addAll(source);
                    copied = true;
                } finally {
                    BlockCollectionDiagnostics.end(copy, copied, null);
                }
            }
            returnedNormally = true;
            return locations;
        } finally {
            BlockCollectionDiagnostics.end(query, returnedNormally, locations);
//#if MC == 12001
        }

//#endif
        }
    }

    public List<BlockPos> getKnownLocations(Block... blocks) {
        List<BlockPos> locations = getKnownLocationsIncludeUnreachable(blocks);
        locations.removeIf(this::isUnreachable);
        return locations;
    }

    /**
     * Scans a radius for the closest block of a given type .
     *
     * @param pos    The center of this radius
     * @param range  Radius to scan for
     * @param blocks What blocks to check for
     */
    public Optional<BlockPos> getNearestWithinRange(Vec3d pos, double range, Block... blocks) {
        Optional<BlockPos> nearest = getNearestBlock(pos, blocks);

        if (nearest.isEmpty() || nearest.get().isWithinDistance(pos, range)) return nearest;

        return Optional.empty();
    }

    public Optional<BlockPos> getNearestWithinRange(BlockPos pos, double range, Block... blocks) {
        return getNearestWithinRange(new Vec3d(pos.getX(), pos.getY(), pos.getZ()), range, blocks);
    }


    public boolean anyFound(Block... blocks) {
        return anyFound((block) -> true, blocks);
    }


    public boolean anyFound(Predicate<BlockPos> isValidTest, Block... blocks) {
        for (Block block : blocks) {
//#if MC == 12001
            for (BlockPos pos : getKnownLocationsIncludeUnreachable(block)) {
//#else
//$$             if (!trackedBlocks.containsKey(block)) continue;

//$$             for (BlockPos pos : trackedBlocks.get(block)) {
//#endif
                if (isValidTest.test(pos) && mod.getWorld().getBlockState(pos).getBlock().equals(block) && !this.isUnreachable(pos))
                    return true;
            }
        }

        return false;
//#if MC == 12001


//#endif
    }

    public Optional<BlockPos> getNearestBlock(Block... blocks) {
        // Add juuust a little, to prevent digging down all the time/bias towards blocks BELOW the player
        return getNearestBlock(mod.getPlayer().getPos().add(0, 0.6f, 0), blocks);
    }

    public Optional<BlockPos> getNearestBlock(Vec3d pos, Block... blocks) {
        return getNearestBlock(pos, p -> true, blocks);
    }

    public Optional<BlockPos> getNearestBlock(Predicate<BlockPos> isValidTest, Block... blocks) {
        return getNearestBlock(mod.getPlayer().getPos().add(0, 0.6f, 0), isValidTest, blocks);
    }

    public Optional<BlockPos> getNearestBlock(Vec3d pos, Predicate<BlockPos> isValidTest, Block... blocks) {
        Optional<BlockPos> closest = Optional.empty();

        for (Block block : blocks) {
            Optional<BlockPos> p = getNearestBlock(block, isValidTest, pos);

            if (p.isPresent()) {
                if (closest.isEmpty()) closest = p;
                else {
                    if (BaritoneHelper.calculateGenericHeuristic(pos, WorldHelper.toVec3d(closest.get())) > BaritoneHelper.calculateGenericHeuristic(pos, WorldHelper.toVec3d(p.get()))) {
                        closest = p;
                    }
                }
            }
        }

        return closest;
    }

    public Optional<BlockPos> getNearestBlock(Block block, Vec3d fromPos) {
        return getNearestBlock(block, (pos) -> true, fromPos);
    }

    public Optional<BlockPos> getNearestBlock(Block block, Predicate<BlockPos> isValidTest, Vec3d fromPos) {
        BlockPos pos = null;
        double nearest = Double.POSITIVE_INFINITY;

//#if MC == 12001
        for (BlockPos p : getKnownLocationsIncludeUnreachable(block)) {
//#else
//$$         if (!trackedBlocks.containsKey(block)) {
//$$             return Optional.empty();
//$$         }

//$$         for (BlockPos p : trackedBlocks.get(block)) {
//#endif
            //ensure the block is there (can change upon rescan)
            if (!mod.getWorld().getBlockState(p).getBlock().equals(block)) continue;
            if (!isValidTest.test(p) || isUnreachable(p)) continue;

            double dist = BaritoneHelper.calculateGenericHeuristic(fromPos, WorldHelper.toVec3d(p));

            if (dist < nearest) {
                nearest = dist;
                pos = p;
            }
        }

        return pos != null ? Optional.of(pos) : Optional.empty();
//#if MC == 12001


//#endif
    }

    public boolean anyFoundWithinDistance(double distance, Block... blocks) {
        return anyFoundWithinDistance(mod.getPlayer().getPos().add(0, 0.6f, 0), distance, blocks);
    }

    public boolean anyFoundWithinDistance(Vec3d pos, double distance, Block... blocks) {
        Optional<BlockPos> blockPos = getNearestBlock(blocks);
        return blockPos.map(value -> value.isWithinDistance(pos, distance)).orElse(false);
    }

    public double distanceToClosest(Block... blocks) {
        return distanceToClosest(mod.getPlayer().getPos().add(0, 0.6f, 0), blocks);
    }

    public double distanceToClosest(Vec3d pos, Block... blocks) {
        Optional<BlockPos> blockPos = getNearestBlock(blocks);
        return blockPos.map(value ->  Math.sqrt(BlockPosVer.getSquaredDistance(value, pos))).orElse(Double.POSITIVE_INFINITY);
    }

    // Checks if 'pos' one of 'blocks' block
    // Returns false if incorrect or undetermined/unsure
    public boolean isBlockAtPosition(BlockPos pos, Block... blocks) {
        if (isUnreachable(pos)) {
            return false;
        }

        if (!mod.getChunkTracker().isChunkLoaded(pos)) {
            return false;
        }

        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) {
            return false;
        }
        try {
            for (Block block : blocks) {
                if (world.isAir(pos) && WorldHelper.isAir(block)) {
                    return true;
                }
                BlockState state = world.getBlockState(pos);
                if (state.getBlock() == block) {
                    return true;
                }
            }
            return false;
        } catch (NullPointerException e) {
            // Probably out of chunk. This means we can't judge its state.
            return false;
        }
    }

    public void reset() {
//#if MC == 12001
        synchronized (collectionLock) {
            //20260913_kpopmodder: A reset is an observed write attempt, not a diagnostic cleanup policy.
            BlockCollectionToken reset = BlockCollectionDiagnostics.begin(this, trackedBlocks, trackedBlocks,
                    BlockCollectionOperation.RESET, "BlockScanner.reset.trackedBlocks.clear");
            boolean cleared = false;
            try {
                trackedBlocks.clear();
                scannedBlocks.clear();
                scannedChunks.clear();
                cachedScannedBlocks = new HashMap<>();
                contentRevision++;
                lifetimeGeneration++;
                unreachablePositions.clear();
                if (lifecycleDiagnostics != null) lifecycleDiagnostics.observe("RESET_INVALIDATED", lifetimeGeneration, contentRevision, "retire_owned_run");
                if (activeRun != null) activeRun.cancel();
                activeRun = null;
                runCompletionPending = false;
                completedRun = null;
                cleared = true;
            } finally { BlockCollectionDiagnostics.end(reset, cleared, null); }
            rescanTimer.forceElapse();
            blacklist.clear();
//#else
//$$         //20260913_kpopmodder: A reset is an observed write attempt, not a diagnostic cleanup policy.
//$$         BlockCollectionToken reset = BlockCollectionDiagnostics.begin(this, trackedBlocks, trackedBlocks,
//$$                 BlockCollectionOperation.RESET, "BlockScanner.reset.trackedBlocks.clear");
//$$         boolean cleared = false;
//$$         try {
//$$             trackedBlocks.clear();
//$$             cleared = true;
//$$         } finally {
//$$             BlockCollectionDiagnostics.end(reset, cleared, null);
//#endif
        }
//#if MC == 12001
    }

    public BlockLocationSnapshot snapshotLocations(Block... blocks) {
        synchronized (collectionLock) {
            return new BlockLocationSnapshot(scanWorld, scanPlayer, scanDimension, contentRevision,
                    getKnownLocationsIncludeUnreachable(blocks));
        }
//#else
//$$         scannedBlocks.clear();
//$$         scannedChunks.clear();
//$$         rescanTimer.forceElapse();
//$$         blacklist.clear();
//$$         forceStop = true;
//#endif
    }

    public void tick() {
//#if MC == 12001
        if (!MinecraftClient.getInstance().isOnThread()) throw new IllegalStateException("client_thread_required");
        ClientWorld world = mod.getWorld();
        Object player = mod.getPlayer();
        synchronized (collectionLock) {
            if (world != scanWorld || player != scanPlayer || (world != null && scanDimension != WorldHelper.getCurrentDimension())) {
                reset();
                scanWorld = world;
                scanPlayer = player;
                if (world != null) scanDimension = WorldHelper.getCurrentDimension();
            }
        }
        if (world == null || player == null) return;
        commitClientCompletion();
        serviceClientChunkRequest(world);
//#else
//$$         if (mod.getWorld() == null || mod.getPlayer() == null) return;
//$$         //be maximally aware of the closest blocks around you
//#endif
        scanCloseBlocks();
//#if MC == 12001
        synchronized (collectionLock) {
            if (runCompletionPending) {
                rescanTimer.reset();
                runCompletionPending = false;
            }
            if (!rescanTimer.elapsed() || activeRun != null) return;
            ScanRunLease run = new ScanRunLease(new ScanWorldBinding(scanWorld, scanPlayer, scanDimension, lifetimeGeneration));
            activeRun = run;
            HashMap<Block, HashSet<BlockPos>> runBlocks = new HashMap<>();
            for (var entry : scannedBlocks.entrySet()) runBlocks.put(entry.getKey(), new HashSet<>(entry.getValue()));
            HashMap<ChunkPos, Long> runChunks = new HashMap<>(scannedChunks);
            ChunkPos playerChunk = mod.getPlayer().getChunkPos();
            Vec3d playerPosition = mod.getPlayer().getPos();
            Set<BlockPos> runUnreachable = Set.copyOf(unreachablePositions);
            Thread worker = new Thread(() -> {
                ScanResultSnapshot result = null;
                String failure = "WORKER_ABORTED";
                try {
                    rescan(Integer.MAX_VALUE, Integer.MAX_VALUE, run, runBlocks, runChunks, playerChunk, playerPosition, runUnreachable);
                    if (!run.cancelled()) result = new ScanResultSnapshot(runBlocks, runChunks);
                    failure = run.cancelled() ? "RUN_CANCELLED" : "NONE";
                } catch (RuntimeException problem) {
                    failure = problem.getClass().getName();
                } finally {
                    //20260913_kpopmodder: Worker completion is only a mailbox offer, never a world/index commit.
                    synchronized (collectionLock) {
                        if (activeRun == run) completedRun = new ScanRunCompletion(run, result, failure);
                    }
                }
            }, "ChatClef-BlockScanner");
            worker.setDaemon(true);
            worker.start();
            lifecycleDiagnostics.observe("RUN_STARTED", lifetimeGeneration, contentRevision, "client_chunk_mailbox_limit_1");
        }
    }
//#else
//$$         if (!rescanTimer.elapsed() || scanning) return;
//#endif

//#if MC == 12001
    private void commitClientCompletion() {
        synchronized (collectionLock) {
            ScanRunCompletion completion = completedRun;
            if (completion == null) return;
            completedRun = null;
            ScanRunLease run = completion.run();
            if (!isCurrentRun(run)) {
                lifecycleDiagnostics.observe("STALE_RUN_DISCARDED", lifetimeGeneration, contentRevision, "client_commit_owner_or_world_mismatch");
                return;
//#else
//$$         if (scanDimension != WorldHelper.getCurrentDimension() || mod.getWorld() != scanWorld) {
//$$             if (LOG) {
//$$                 mod.log("BlockScanner: new dimension or world detected, resetting data!");
//#endif
            }
//#if MC == 12001
            if (completion.result() != null) {
                scannedBlocks.clear();
                for (var entry : completion.result().blocks().entrySet()) scannedBlocks.put(entry.getKey(), new HashSet<>(entry.getValue()));
                scannedChunks.clear();
                scannedChunks.putAll(completion.result().chunks());
                cachedScannedBlocks = new HashMap<>();
                for (var entry : completion.result().blocks().entrySet()) cachedScannedBlocks.put(entry.getKey(), new HashSet<>(entry.getValue()));
                lifecycleDiagnostics.observe("RUN_PUBLISHED", lifetimeGeneration, contentRevision, "client_tick_world_player_generation_matched");
            } else {
                lifecycleDiagnostics.observe("RUN_FAILED", lifetimeGeneration, contentRevision, completion.failure());
            }
            activeRun = null;
            runCompletionPending = true;
//#else
//$$             reset();
//$$             scanWorld = mod.getWorld();
//$$             scanDimension = WorldHelper.getCurrentDimension();
//$$             return;
//#endif
        }
//#if MC == 12001
    }
//#endif

//#if MC == 12001
    private boolean isCurrentRun(ScanRunLease run) {
        return activeRun == run && !run.cancelled()
                && run.binding().matches(scanWorld, scanPlayer, scanDimension, lifetimeGeneration)
                && mod.getWorld() == scanWorld && mod.getPlayer() == scanPlayer;
    }

    private void serviceClientChunkRequest(ClientWorld world) {
        ScanRunLease run;
        ScanChunkRequest request;
        synchronized (collectionLock) {
            run = activeRun;
            request = run == null ? null : run.takeRequest();
//#else
//$$         cachedScannedBlocks = new HashMap<>(scannedBlocks.size());
//$$         for (Map.Entry<Block, HashSet<BlockPos>> entry : scannedBlocks.entrySet()) {
//$$             cachedScannedBlocks.put(entry.getKey(), (HashSet<BlockPos>) entry.getValue().clone());
//#endif
        }
//#if MC == 12001
        if (request == null) return;
        try {
            ScanChunkSnapshot snapshot = ClientChunkSnapshotCapture.capture(world, request.position());
            synchronized (collectionLock) {
                request.result().complete(isCurrentRun(run) && scanWorld == world ? snapshot : null);
//#else

//$$         if (LOG) {
//$$             mod.log("Updating BlockScanner.. size: " + trackedBlocks.size() + " : " + cachedScannedBlocks.size());
//$$         }

//$$         scanning = true;
//$$         forceStop = false;
//$$         new Thread(() -> {
//$$             try {
//$$                 rescan(Integer.MAX_VALUE, Integer.MAX_VALUE);
//$$             } catch (Exception e) {
//$$                 e.printStackTrace();
//$$             } finally {
//$$                 rescanTimer.reset();
//$$                 scanning = false;
//#endif
            }
//#if MC == 12001
        } catch (RuntimeException failure) { request.result().completeExceptionally(failure); }
//#else
//$$         }).start();
//#endif
    }

    private void scanCloseBlocks() {
//#if MC == 12001
        synchronized (collectionLock) {
        boolean cachedChanged = false;
//#endif
        for (Map.Entry<Block, HashSet<BlockPos>> entry : cachedScannedBlocks.entrySet()) {
//#if MC == 12001
            cachedChanged |= !entry.getValue().equals(trackedBlocks.get(entry.getKey()));
//#endif
            if (!trackedBlocks.containsKey(entry.getKey())) {
                BlockCollectionToken put = BlockCollectionDiagnostics.begin(this, trackedBlocks, null,
                        BlockCollectionOperation.WRITE_PUT, "BlockScanner.scanCloseBlocks.cached.put");
                boolean inserted = false;
                try {
                    trackedBlocks.put(entry.getKey(), new HashSet<>());
                    inserted = true;
                } finally {
                    BlockCollectionDiagnostics.end(put, inserted, null);
                }
            }
            HashSet<BlockPos> clearTarget = trackedBlocks.get(entry.getKey());
            BlockCollectionToken clear = BlockCollectionDiagnostics.begin(this, trackedBlocks, clearTarget,
                    BlockCollectionOperation.WRITE_CLEAR, "BlockScanner.scanCloseBlocks.cached.clear");
            boolean cleared = false;
            try {
                clearTarget.clear();
                cleared = true;
            } finally {
                BlockCollectionDiagnostics.end(clear, cleared, null);
            }
            HashSet<BlockPos> addTarget = trackedBlocks.get(entry.getKey());
            BlockCollectionToken add = BlockCollectionDiagnostics.begin(this, trackedBlocks, addTarget,
                    BlockCollectionOperation.WRITE_ADD_ALL, "BlockScanner.scanCloseBlocks.cached.addAll");
            boolean added = false;
            try {
                addTarget.addAll(entry.getValue());
                added = true;
            } finally {
                BlockCollectionDiagnostics.end(add, added, null);
            }
        }

//#if MC == 12001
        if (cachedChanged) contentRevision++;
        }
//#endif
        HashMap<Block, HashSet<BlockPos>> map = new HashMap<>();

        BlockPos pos = mod.getPlayer().getBlockPos();
        World world = mod.getPlayer().getWorld();

        for (int x = pos.getX() - 8; x <= pos.getX() + 8; x++) {
            for (int y = pos.getY() - 8; y < pos.getY() + 8; y++) {
                for (int z = pos.getZ() - 8; z <= pos.getZ() + 8; z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(p);
                    if (world.getBlockState(p).isAir()) continue;

                    Block block = state.getBlock();

                    if (map.containsKey(block)) {
                        map.get(block).add(p);
                    } else {
                        HashSet<BlockPos> set = new HashSet<>();
                        set.add(p);
                        map.put(block, set);
                    }
                }
            }
        }

//#if MC == 12001
        synchronized (collectionLock) {
        boolean localChanged = false;
//#endif
        for (Map.Entry<Block, HashSet<BlockPos>> entry : map.entrySet()) {
            getFirstFewPositions(entry.getValue(),mod.getPlayer().getPos());

            if (!trackedBlocks.containsKey(entry.getKey())) {
                BlockCollectionToken put = BlockCollectionDiagnostics.begin(this, trackedBlocks, null,
                        BlockCollectionOperation.WRITE_PUT, "BlockScanner.scanCloseBlocks.local.put");
                boolean inserted = false;
                try {
                    trackedBlocks.put(entry.getKey(), new HashSet<>());
                    inserted = true;
                } finally {
                    BlockCollectionDiagnostics.end(put, inserted, null);
                }
            }
            HashSet<BlockPos> addTarget = trackedBlocks.get(entry.getKey());
            BlockCollectionToken add = BlockCollectionDiagnostics.begin(this, trackedBlocks, addTarget,
                    BlockCollectionOperation.WRITE_ADD_ALL, "BlockScanner.scanCloseBlocks.local.addAll");
            boolean added = false;
            try {
//#if MC == 12001
                localChanged |= addTarget.addAll(entry.getValue());
//#else
//$$                 addTarget.addAll(entry.getValue());
//#endif
                added = true;
            } finally {
                BlockCollectionDiagnostics.end(add, added, null);
            }
        }
//#if MC == 12001
        if (localChanged) contentRevision++;
        }
//#endif
    }

//#if MC == 12001
    private void rescan(int maxCount, int cutOffRadius, ScanRunLease run,
                        HashMap<Block, HashSet<BlockPos>> scannedBlocks, HashMap<ChunkPos, Long> scannedChunks,
                        ChunkPos playerChunkPos, Vec3d playerPos, Set<BlockPos> runUnreachable) {
//#else
//$$     private void rescan(int maxCount, int cutOffRadius) {
//#endif
        long ms = System.currentTimeMillis();

//#if MC == 12001
//#else
//$$         ChunkPos playerChunkPos = mod.getPlayer().getChunkPos();
//$$         Vec3d playerPos = mod.getPlayer().getPos();
//#endif

        HashSet<ChunkPos> visited = new HashSet<>();
        Queue<Node> queue = new ArrayDeque<>();
        queue.add(new Node(playerChunkPos, 0));

//#if MC == 12001
        while (!queue.isEmpty() && visited.size() < maxCount && !run.cancelled()) {
//#else
//$$         while (!queue.isEmpty() && visited.size() < maxCount && !forceStop) {
//#endif
            Node node = queue.poll();

//#if MC == 12001
            if (node.distance > cutOffRadius || visited.contains(node.pos))
//#else
//$$             if (node.distance > cutOffRadius || visited.contains(node.pos) || !mod.getWorld().getChunkManager().isChunkLoaded(node.pos.x, node.pos.z))
//#endif
                continue;

//#if MC == 12001
            ScanChunkSnapshot snapshot = run.request(node.pos);
            if (snapshot == null) continue;
//#endif
            boolean isPriorityChunk = getChunkDist(node.pos, playerChunkPos) <= 2;
//#if MC == 12001
            if (!isPriorityChunk && scannedChunks.containsKey(node.pos) && snapshot.worldTime() - scannedChunks.get(node.pos) < RESCAN_TICK_DELAY)
//#else
//$$             if (!isPriorityChunk && scannedChunks.containsKey(node.pos) && mod.getWorld().getTime() - scannedChunks.get(node.pos) < RESCAN_TICK_DELAY)
//#endif
                continue;

            visited.add(node.pos);
//#if MC == 12001
            scanChunk(snapshot, playerChunkPos, scannedBlocks, scannedChunks, runUnreachable, run);
//#else
//$$             scanChunk(node.pos, playerChunkPos);
//#endif

            queue.add(new Node(new ChunkPos(node.pos.x + 1, node.pos.z + 1), node.distance + 1));
            queue.add(new Node(new ChunkPos(node.pos.x - 1, node.pos.z + 1), node.distance + 1));
            queue.add(new Node(new ChunkPos(node.pos.x - 1, node.pos.z - 1), node.distance + 1));
            queue.add(new Node(new ChunkPos(node.pos.x + 1, node.pos.z - 1), node.distance + 1));
        }
//#if MC == 12001
        if (run.cancelled()) return;
//#else
//$$         if (forceStop) {
//$$             // reset again, might have changed some values from the time forceStop was called
//$$             reset();
//$$             forceStop = false;
//$$             return;
//$$         }
//#endif

        for (Iterator<ChunkPos> iterator = scannedChunks.keySet().iterator(); iterator.hasNext(); ) {
            ChunkPos pos = iterator.next();
            int distance = getChunkDist(pos, playerChunkPos);

            if (distance > cutOffRadius) {
                iterator.remove();
            }
        }

        for (HashSet<BlockPos> set : scannedBlocks.values()) {
            if (set.size() < CACHED_POSITIONS_PER_BLOCK) {
                continue;
            }

            getFirstFewPositions(set, playerPos);
        }

        if (LOG) {
            mod.log("Rescanned in: " + (System.currentTimeMillis() - ms) + " ms; visited: " + visited.size() + " chunks");
        }
    }

    private int getChunkDist(ChunkPos pos1, ChunkPos pos2) {
        return Math.abs(pos1.x - pos2.x) + Math.abs(pos1.z - pos2.z);
    }


    //TODO rename
    private void getFirstFewPositions(HashSet<BlockPos> set, Vec3d playerPos) {
        Queue<BlockPos> queue = new PriorityQueue<>(Comparator.comparingDouble((pos) -> -BaritoneHelper.calculateGenericHeuristic(playerPos, WorldHelper.toVec3d(pos))));

        for (BlockPos pos : set) {
            queue.add(pos);

            if (queue.size() > CACHED_POSITIONS_PER_BLOCK) {
                queue.poll();
            }
        }

        set.clear();

        for (int i = 0; i < CACHED_POSITIONS_PER_BLOCK && !queue.isEmpty(); i++) {
            set.add(queue.poll());
        }
    }

    /**
     * scans a chunk and adds block positions corresponding to a specific block in a list
     *
     * @param chunkPos position of the scanned chunk
     */
//#if MC == 12001
    private void scanChunk(ScanChunkSnapshot snapshot, ChunkPos playerChunkPos,
                           HashMap<Block, HashSet<BlockPos>> scannedBlocks, HashMap<ChunkPos, Long> scannedChunks,
                           Set<BlockPos> runUnreachable, ScanRunLease run) {
        ChunkPos chunkPos = snapshot.position();
        scannedChunks.put(chunkPos, snapshot.worldTime());
//#else
//$$     private void scanChunk(ChunkPos chunkPos, ChunkPos playerChunkPos) {
//$$         World world = mod.getWorld();
//$$         WorldChunk chunk = mod.getWorld().getChunk(chunkPos.x, chunkPos.z);
//$$         scannedChunks.put(chunkPos, world.getTime());
//#endif

        boolean isPriorityChunk = getChunkDist(chunkPos, playerChunkPos) <= 2;

        for (int x = chunkPos.getStartX(); x <= chunkPos.getEndX(); x++) {
//#if MC == 12001
            for (int y = snapshot.bottomY(); y < snapshot.topY(); y++) {
//#else
//$$             for (int y = world.getBottomY(); y < world.getTopY(); y++) {
//#endif
                for (int z = chunkPos.getStartZ(); z <= chunkPos.getEndZ(); z++) {
                    BlockPos p = new BlockPos(x, y, z);
//#if MC == 12001
                    if (run.cancelled()) return;
                    if (runUnreachable.contains(p)) continue;
//#else
//$$                     if (this.isUnreachable(p) || world.isOutOfHeightLimit(p)) continue;
//#endif

//#if MC == 12001
                    BlockState state = snapshot.state(p);
//#else
//$$                     BlockState state = chunk.getBlockState(p);
//#endif
                    if (state.isAir()) continue;

                    Block block = state.getBlock();
                    if (scannedBlocks.containsKey(block)) {
                        HashSet<BlockPos> set = scannedBlocks.get(block);

                        if ((set.size() > CACHED_POSITIONS_PER_BLOCK * 750 && !isPriorityChunk)) continue;

                        set.add(p);
                    } else {
                        HashSet<BlockPos> set = new HashSet<>();
                        set.add(p);
                        scannedBlocks.put(block, set);
                    }
                }
            }
        }
    }

    private record Node(ChunkPos pos, int distance) {
    }


}
