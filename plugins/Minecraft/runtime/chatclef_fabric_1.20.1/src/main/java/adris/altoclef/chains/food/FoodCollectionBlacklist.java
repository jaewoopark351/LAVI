package adris.altoclef.chains.food;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

//20260727_kpopmodder: Keeps unsafe food targets out of the main collection task flow.
public final class FoodCollectionBlacklist {
    private static final int HUNT_TARGET_SKIP_TICKS = 20 * 60;
    private static final int HUNT_TARGET_AREA_SKIP_RADIUS_BLOCKS = 12;
    private static final Map<UUID, Integer> temporarilySkippedHuntTargets = new HashMap<>();
    private static final Map<BlockPos, Integer> temporarilySkippedHuntAreas = new HashMap<>();

    private FoodCollectionBlacklist() {
    }

    public static void applyKnownFoodBlacklists(AltoClef mod) {
        tickTemporarySkips(temporarilySkippedHuntTargets);
        tickTemporarySkips(temporarilySkippedHuntAreas);
        blackListChickenJockeys(mod);
        blackListPillageHayBales(mod);
    }

    public static boolean shouldSkipHuntTarget(Entity entity) {
        return temporarilySkippedHuntTargets.containsKey(entity.getUuid())
                || isNearSkippedHuntArea(entity.getBlockPos());
    }

    public static void temporarilySkipHuntTarget(AltoClef mod, Entity entity, String reason) {
        temporarilySkippedHuntTargets.put(entity.getUuid(), HUNT_TARGET_SKIP_TICKS);
        temporarilySkippedHuntAreas.put(entity.getBlockPos().toImmutable(), HUNT_TARGET_SKIP_TICKS);
        mod.getEntityTracker().requestEntityUnreachable(entity);
        Debug.logMessage("Temporarily skipping food hunt target: "
                + entity.getType().getTranslationKey()
                + " at " + entity.getBlockPos().toShortString()
                + " for " + (HUNT_TARGET_SKIP_TICKS / 20) + "s"
                + ", areaRadius=" + HUNT_TARGET_AREA_SKIP_RADIUS_BLOCKS
                + " (" + reason + ")");
    }

    private static <T> void tickTemporarySkips(Map<T, Integer> skips) {
        Iterator<Map.Entry<T, Integer>> iterator = skips.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<T, Integer> entry = iterator.next();
            int ticksLeft = entry.getValue() - 1;
            if (ticksLeft <= 0) {
                iterator.remove();
            } else {
                entry.setValue(ticksLeft);
            }
        }
    }

    private static boolean isNearSkippedHuntArea(BlockPos target) {
        for (BlockPos skippedArea : temporarilySkippedHuntAreas.keySet()) {
            if (isWithinBlockRadius(target, skippedArea, HUNT_TARGET_AREA_SKIP_RADIUS_BLOCKS)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isWithinBlockRadius(BlockPos a, BlockPos b, int radius) {
        long dx = a.getX() - b.getX();
        long dy = a.getY() - b.getY();
        long dz = a.getZ() - b.getZ();
        long radiusSq = (long) radius * radius;
        return dx * dx + dy * dy + dz * dz <= radiusSq;
    }

    public static void blackListChickenJockeys(AltoClef mod) {
        if (mod.getEntityTracker().entityFound(ChickenEntity.class)) {
            Optional<Entity> chickens = mod.getEntityTracker().getClosestEntity(ChickenEntity.class);
            if (chickens.isPresent()) {
                Iterable<Entity> entities = mod.getWorld().getEntities();
                for (Entity entity : entities) {
                    if (entity instanceof HostileEntity || entity instanceof SlimeEntity) {
                        if (chickens.get().hasPassenger(entity)) {
                            if (mod.getEntityTracker().isEntityReachable(entity)) {
                                Debug.logMessage("Blacklisting chicken jockey.");
                                mod.getEntityTracker().requestEntityUnreachable(chickens.get());
                            }
                        }
                    }
                }
            }
        }
    }

    private static void blackListPillageHayBales(AltoClef mod) {
        List<BlockPos> hayPositions = mod.getBlockScanner().getKnownLocations(Blocks.HAY_BLOCK);
        for (BlockPos hayPos : hayPositions) {
            BlockPos haysUpPos = hayPos.up();
            if (mod.getWorld().getBlockState(haysUpPos).getBlock() == Blocks.CARVED_PUMPKIN) {
                Debug.logMessage("Blacklisting pillage hay bales.");
                mod.getBlockScanner().requestBlockUnreachable(hayPos, 0);
            }
        }
    }
}
