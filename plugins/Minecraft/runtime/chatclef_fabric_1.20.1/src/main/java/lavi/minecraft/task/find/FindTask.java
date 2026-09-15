//#if MC == 12001
//20260915_kpopmodder: Search until a target is found or STOP; keep a separate active-approach budget and defense priority.
package lavi.minecraft.task.find;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class FindTask extends Task {
    static final int BLOCK_RADIUS = 32;
    static final int ENTITY_RADIUS = 64;
    static final int BLOCKS_PER_TICK = 4096;
    static final int MAX_ENTITIES = 4096;
    // Report wall time and selected-target active approach time, never an exploration lifetime.
    static final long MAX_NANOS = 90_000_000_000L;
    private final FindRequest request;
    private final String operationId = UUID.randomUUID().toString();
    private ClientWorld world;
    private BlockPos origin;
    private FindNameIndex.Entry target;
    private Entity entity;
    private UUID entityUuid;
    private BlockPos block;
    private FindApproachTask approach;
    private FindOutcome outcome;
    private long startedNanos;
    private long approachActiveNanos;
    private long approachTickNanos;
    private boolean approachClockRunning;
    private boolean initialized;
    private Object playerIdentity;
    private FindEntitySearchTask entitySearch;
    private FindBlockSearchTask blockSearch;
    private boolean defenseWaiting;
    private boolean scanLimited;
    private boolean pendingObservation;
    private long observationPass;
    private String phase = "RESOLVE";
    // Diagnostics only: finite reason/phase keys, capped repetition, no gameplay decisions.
    private final Map<String, Integer> diagnosticCounts = new HashMap<>();
    private int suppressedDiagnostics;
    private boolean scanComplete;
    private int scanCursor;
    private int scanned;
    private int languageWarnings;
    private int interruptions;
    private double bestDistance = Double.POSITIVE_INFINITY;

    public FindTask(FindRequest request) { this.request = Objects.requireNonNull(request, "request"); }
    public FindRequest request() { return request; }
    public FindOutcome outcome() { return outcome; }

    @Override protected void onStart() {
        if (initialized) {
            // Resume the same operation and spent approach budget; do not charge a defense interruption.
            boundary("resume", "interruptions", interruptions);
            return;
        }
        initialized = true;
        startedNanos = System.nanoTime();
        var mod = AltoClef.getInstance();
        world = mod.getWorld();
        if (world == null || mod.getPlayer() == null || !mod.getPlayer().isAlive()) {
            finish("PLAYER_UNAVAILABLE", List.of());
            return;
        }
        playerIdentity = mod.getPlayer();
        origin = mod.getPlayer().getBlockPos().toImmutable();
        boundary("start", "command", request.command(), "origin", origin.toShortString(),
                "searchLifetime", request.mode().equals("approach") ? "UNTIL_FOUND_OR_STOP" : "REPORT_ONLY",
                "reportTimeoutMs", MAX_NANOS / 1_000_000L,
                "approachActiveTimeoutMs", MAX_NANOS / 1_000_000L);
        try {
            var resolver = new FindRegistryResolver();
            var resolution = resolver.resolve(request);
            languageWarnings = resolver.resourceWarnings();
            target = resolution.unique();
            if (target == null) {
                finish(resolution.candidates().isEmpty() ? "UNKNOWN_TARGET" : "AMBIGUOUS_TARGET",
                        resolution.candidates().stream().limit(5).map(e -> e.kind() + " " + e.id()).toList());
                return;
            }
            //20260915_kpopmodder: Observe the ID-only resolver at its operation-owned boundary.
            boundary("resolved", "kind", target.kind(), "id", target.id(), "languageWarnings", languageWarnings,
                    "resolutionSource", "registry_id", "requestedKind", request.kind(), "query", request.query());
            if (request.mode().equals("approach")) {
                if (target.kind().equals("block")) blockSearch = new FindBlockSearchTask(this, target.id());
                else entitySearch = new FindEntitySearchTask(this);
                phase = "SEARCH";
            } else phase = "REPORT";
        } catch (RuntimeException error) {
            boundary("resolver_error", "exception", error.getClass().getSimpleName());
            finish("INTERNAL_ERROR", List.of());
        }
    }

    @Override protected Task onTick() {
        if (outcome != null) return null;
        var mod = AltoClef.getInstance();
        if (mod.getWorld() != world || (mod.getPlayer() != null && mod.getPlayer() != playerIdentity)) {
            finish("SCOPE_CHANGED", List.of());
            return null;
        }
        if (mod.getPlayer() == null || !mod.getPlayer().isAlive()) {
            finish("PLAYER_UNAVAILABLE", List.of());
            return null;
        }
        if (request.mode().equals("report") && System.nanoTime() - startedNanos >= MAX_NANOS) {
            boundary("report_time_limit", "limitMs", MAX_NANOS / 1_000_000L);
            finish("SEARCH_LIMIT", List.of());
            return null;
        }
        try {
            if (request.mode().equals("report")) return reportTick();

            // TaskRunner has ALREADY evaluated defense this tick. Never reevaluate its priority here.
            // This also yields to force-field attacks/shielding which can run without selecting its chain.
            if (mod.getMobDefenseChain().isToolInputClaimed()) {
                approachClockRunning = false;
                if (!defenseWaiting) {
                    defenseWaiting = true;
                    phase = "DEFENSE_WAIT";
                    boundary("defense_yield", "targetPosition", position(),
                            "approachActiveMs", approachActiveNanos / 1_000_000L);
                }
                return null; // Native child reconciliation releases only our movement process/policy.
            }
            if (defenseWaiting) {
                defenseWaiting = false;
                phase = "SEARCH";
                boundary("defense_resume", "targetPosition", position(),
                        "approachActiveMs", approachActiveNanos / 1_000_000L);
            }
            if ((entity != null || block != null) && !targetStillValid()) {
                boundary("target_lost_research", "previousPosition", position(), "previousEntityUuid", entityUuid);
                entity = null;
                entityUuid = null;
                block = null;
                approach = null;
                resetApproachBudget();
                if (entitySearch != null) entitySearch.resetSearch();
                if (blockSearch != null) blockSearch.resetSearch();
                phase = "SEARCH";
            }
            if ((entity != null || block != null) && arrived()) {
                finish("ARRIVED", List.of());
                return null;
            }
            if (entity != null || block != null) {
                if (approachTimeLimitReached()) {
                    boundary("approach_time_limit", "approachActiveMs", approachActiveNanos / 1_000_000L,
                            "limitMs", MAX_NANOS / 1_000_000L, "targetPosition", position());
                    finish("APPROACH_TIMEOUT", List.of());
                    return null;
                }
            } else approachClockRunning = false;
            return entitySearch != null ? entitySearch : blockSearch;
        } catch (RuntimeException error) {
            movementFailed(error);
            return null;
        }
    }

    private void resetApproachBudget() {
        approachActiveNanos = 0;
        approachClockRunning = false;
    }

    private boolean approachTimeLimitReached() {
        long now = System.nanoTime();
        if (approachClockRunning) approachActiveNanos += Math.max(0L, now - approachTickNanos);
        approachTickNanos = now;
        approachClockRunning = true;
        return approachActiveNanos >= MAX_NANOS;
    }

    private Task reportTick() {
        if (!scanComplete) {
            if (target.kind().equals("block")) scanBlocks(); else scanEntities();
            if (outcome != null || !scanComplete) return null;
            if (entity == null && block == null) {
                finish("NOT_FOUND", List.of());
                return null;
            }
            boundary("selected", "targetPosition", position(), "entityUuid", entityUuid,
                    "approachActiveMs", approachActiveNanos / 1_000_000L);
        }
        finish(targetStillValid() ? "FOUND" : "TARGET_LOST", List.of());
        return null;
    }

    void beginObservation() {
        scanned = 0;
        scanComplete = false;
        scanLimited = false;
        pendingObservation = false;
        observationPass++;
    }

    void observationPending() { pendingObservation = true; }

    void endObservation(boolean candidatePresent) {
        scanComplete = !scanLimited && !pendingObservation;
        boundary("scan_complete", "observationPass", observationPass, "scanned", scanned,
                "scanComplete", scanComplete, "candidatePresent", candidatePresent,
                "scanCenter", AltoClef.getInstance().getPlayer().getBlockPos().toShortString());
        if (scanLimited) finish("SEARCH_LIMIT", List.of());
    }

    boolean considerEntity(Entity candidate) {
        if (scanned >= MAX_ENTITIES) { scanLimited = true; return false; }
        scanned++;
        return entityMatches(candidate) && withinEntityRadius(candidate);
    }

    boolean considerBlock(BlockPos candidate) {
        // The native scanner owns its snapshot. Count predicates in THIS query, not cumulative world coverage.
        if (scanned >= 65 * 65 * 65) { scanLimited = true; return false; }
        scanned++;
        BlockPos center = AltoClef.getInstance().getPlayer().getBlockPos();
        return blockMatches(candidate) && Math.abs((long) candidate.getX() - center.getX()) <= BLOCK_RADIUS
                && Math.abs((long) candidate.getY() - center.getY()) <= BLOCK_RADIUS
                && Math.abs((long) candidate.getZ() - center.getZ()) <= BLOCK_RADIUS;
    }

    boolean entityMatches(Entity candidate) {
        if (candidate == null || candidate == AltoClef.getInstance().getPlayer() || !candidate.isAlive()
                || candidate.isRemoved() || world.getEntityById(candidate.getId()) != candidate) return false;
        return switch (target.kind()) {
            case "item" -> candidate instanceof ItemEntity dropped && !dropped.getStack().isEmpty()
                    && Registries.ITEM.getId(dropped.getStack().getItem()).toString().equals(target.id());
            case "player" -> candidate instanceof PlayerEntity player
                    && player.getGameProfile().getName().equalsIgnoreCase(target.id());
            default -> Registries.ENTITY_TYPE.getId(candidate.getType()).toString().equals(target.id());
        };
    }

    private boolean withinEntityRadius(Entity candidate) {
        Vec3d center = request.mode().equals("report") ? Vec3d.ofCenter(origin)
                : AltoClef.getInstance().getPlayer().getPos();
        return candidate.getPos().squaredDistanceTo(center) <= ENTITY_RADIUS * ENTITY_RADIUS;
    }

    boolean blockMatches(BlockPos candidate) {
        return candidate != null && candidate.getY() >= world.getBottomY()
                && candidate.getY() < world.getTopY() && loaded(candidate)
                && Registries.BLOCK.getId(world.getBlockState(candidate).getBlock()).toString().equals(target.id());
    }

    Task selectEntity(Entity candidate) {
        if (outcome != null || !entityMatches(candidate)) return null;
        if (entity != candidate) {
            entity = candidate;
            entityUuid = candidate.getUuid();
            block = null;
            approach = new FindApproachTask(this, candidate);
            resetApproachBudget();
            phase = "APPROACH";
            boundary("selected", "targetPosition", position(), "entityUuid", entityUuid,
                    "approachActiveMs", approachActiveNanos / 1_000_000L);
        }
        if (arrived()) { finish("ARRIVED", List.of()); return null; }
        return approach;
    }

    Task selectBlock(BlockPos candidate) {
        if (outcome != null || !blockMatches(candidate)) return null;
        if (!candidate.equals(block)) {
            block = candidate.toImmutable();
            entity = null;
            entityUuid = null;
            resetApproachBudget();
            phase = "APPROACH";
            boundary("selected", "targetPosition", position(),
                    "approachActiveMs", approachActiveNanos / 1_000_000L);
            if (arrived()) { finish("ARRIVED", List.of()); return null; }
            BlockPos stand = standPosition();
            if (stand == null) { finish("NO_APPROACH", List.of()); return null; }
            approach = new FindApproachTask(this, stand);
        }
        return approach;
    }

    private boolean arrived() {
        Vec3d targetPosition = entity != null ? entity.getPos() : Vec3d.ofCenter(block);
        return AltoClef.getInstance().getPlayer().getPos().squaredDistanceTo(targetPosition) <= 16.0;
    }

    BlockPos protectedBlock() { return block; }
    Entity droppedItemTarget() { return target.kind().equals("item") ? entity : null; }

    void searching() {
        if (!phase.equals("SEARCH")) phase = "SEARCH";
        boundary("no_candidate_explore", "observationPass", observationPass);
    }

    void movementFailed(RuntimeException error) {
        boundary("execution_error", "exception", error.getClass().getSimpleName());
        finish("INTERNAL_ERROR", List.of());
    }

    private void scanEntities() {
        // No server requests, class whitelist, inventory access, death subscription, or item pickup.
        for (Entity candidate : world.getEntities()) {
            if (scanned >= MAX_ENTITIES) { finish("SEARCH_LIMIT", List.of()); return; }
            scanned++;
            if (candidate == AltoClef.getInstance().getPlayer() || !candidate.isAlive() || candidate.isRemoved()
                    || candidate.getPos().squaredDistanceTo(Vec3d.ofCenter(origin)) > ENTITY_RADIUS * ENTITY_RADIUS) continue;
            boolean matches = switch (target.kind()) {
                case "item" -> candidate instanceof ItemEntity dropped && !dropped.getStack().isEmpty()
                        && Registries.ITEM.getId(dropped.getStack().getItem()).toString().equals(target.id());
                case "player" -> candidate instanceof PlayerEntity player && player.getGameProfile().getName().equalsIgnoreCase(target.id());
                default -> Registries.ENTITY_TYPE.getId(candidate.getType()).toString().equals(target.id());
            };
            double distance = candidate.getPos().squaredDistanceTo(Vec3d.ofCenter(origin));
            if (matches && distance < bestDistance) {
                bestDistance = distance; entity = candidate; entityUuid = candidate.getUuid();
            }
        }
        scanComplete = true;
    }

    private void scanBlocks() {
        int side = BLOCK_RADIUS * 2 + 1;
        int end = side * side * side;
        int steps = 0;
        long sliceStart = System.nanoTime();
        while (scanCursor < end && steps++ < BLOCKS_PER_TICK && System.nanoTime() - sliceStart < 2_000_000L) {
            int cursor = scanCursor++;
            BlockPos pos = origin.add(cursor % side - BLOCK_RADIUS, cursor / (side * side) - BLOCK_RADIUS,
                    (cursor / side) % side - BLOCK_RADIUS);
            if (pos.getY() < world.getBottomY() || pos.getY() >= world.getTopY() || !loaded(pos)) continue;
            scanned++;
            if (!Registries.BLOCK.getId(world.getBlockState(pos).getBlock()).toString().equals(target.id())) continue;
            double distance = pos.getSquaredDistance(origin);
            if (distance < bestDistance) { bestDistance = distance; block = pos.toImmutable(); }
        }
        scanComplete = scanCursor >= end;
    }

    private boolean loaded(BlockPos pos) {
        // Explicit create=false: a scan cannot load/generate chunks or consult a stale global cache.
        return world.getChunkManager().getChunk(pos.getX() >> 4, pos.getZ() >> 4,
                net.minecraft.world.chunk.ChunkStatus.FULL, false) != null;
    }

    private boolean targetStillValid() {
        if (entity != null) return entityMatches(entity) && entity.getUuid().equals(entityUuid)
                && withinEntityRadius(entity);
        return blockMatches(block);
    }

    private BlockPos standPosition() {
        BlockPos best = null;
        double distance = Double.POSITIVE_INFINITY;
        for (int dy = -2; dy <= 2; dy++) for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
            BlockPos pos = block.add(dx, dy, dz);
            if (pos.equals(block) || Vec3d.ofBottomCenter(pos).squaredDistanceTo(Vec3d.ofCenter(block)) > 12.25
                    || !loaded(pos) || !loaded(pos.down()) || !world.isAir(pos) || !world.isAir(pos.up())) continue;
            var floor = world.getBlockState(pos.down());
            if (!floor.isSideSolidFullSquare(world, pos.down(), Direction.UP) || !floor.getFluidState().isEmpty()) continue;
            // Exclude obvious damaging floors; Baritone retains its existing path safety policy.
            String floorId = Registries.BLOCK.getId(floor.getBlock()).toString();
            if (floorId.equals("minecraft:magma_block") || floorId.equals("minecraft:campfire")
                    || floorId.equals("minecraft:soul_campfire") || floorId.equals("minecraft:cactus")) continue;
            double candidateDistance = AltoClef.getInstance().getPlayer().getPos().squaredDistanceTo(Vec3d.ofBottomCenter(pos));
            if (candidateDistance < distance) { distance = candidateDistance; best = pos.toImmutable(); }
        }
        return best;
    }

    @Override protected void onStop(Task interruptTask) {
        // No parent-level behavior stack or global path/input cleanup: the active leaf owns them.
        approachClockRunning = false;
        if (outcome == null) interruptions++;
        boundary(outcome == null ? "yield_or_stop" : "cleanup", "interruptTask",
                interruptTask == null ? "none" : interruptTask.getClass().getSimpleName(),
                "interruptions", interruptions, "approachActiveMs", approachActiveNanos / 1_000_000L);
    }

    private void finish(String code, List<String> suggestions) {
        if (outcome != null) return;
        outcome = new FindOutcome(operationId, request, code,
                target == null ? request.kind() : target.kind(), target == null ? "" : target.id(),
                target == null ? "" : target.label(), world == null ? "" : world.getRegistryKey().getValue().toString(),
                position(), (target == null ? request.kind() : target.kind()).equals("block") ? BLOCK_RADIUS : ENTITY_RADIUS,
                scanned, scanComplete, code.equals("FOUND") || code.equals("ARRIVED"),
                entityUuid == null ? "" : entityUuid.toString(), languageWarnings, suggestions);
        phase = "TERMINAL";
        boundary("terminal", "code", code, "position", outcome.position(), "scanned", scanned,
                "scanComplete", scanComplete, "observed", outcome.observed(), "interruptions", interruptions,
                "observationPass", observationPass, "suppressedDiagnostics", suppressedDiagnostics,
                "approachActiveMs", approachActiveNanos / 1_000_000L);
    }

    private List<Integer> position() {
        BlockPos pos = entity == null ? block : entity.getBlockPos();
        return pos == null ? List.of() : List.of(pos.getX(), pos.getY(), pos.getZ());
    }

    void boundary(String reason, Object... fields) {
        // Owner observations are bounded; diagnostics never supplies a decision or changes the result.
        String key = phase + ":" + reason;
        int count = diagnosticCounts.getOrDefault(key, 0);
        if (count >= 8) { suppressedDiagnostics++; return; }
        diagnosticCounts.put(key, count + 1);
        try {
            Object[] data = new Object[fields.length + 6];
            data[0] = "findOperationId"; data[1] = operationId;
            data[2] = "phase"; data[3] = phase;
            data[4] = "elapsedMs"; data[5] = (System.nanoTime() - startedNanos) / 1_000_000L;
            System.arraycopy(fields, 0, data, 6, fields.length);
            String event = reason.equals("terminal") ? "FIND_TERMINAL"
                    : reason.endsWith("error") ? "FIND_EXCEPTION" : "FIND";
            ChatClefDiagnostics.logBoundary(event, reason, this, data);
        } catch (RuntimeException ignored) { }
    }
    @Override public boolean isFinished() { return outcome != null; }
    @Override protected boolean isEqual(Task other) { return this == other; }
    @Override protected String toDebugString() { return "FIND " + request.kind() + " " + request.query(); }
}
//#endif
