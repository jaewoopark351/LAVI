package adris.altoclef.chains.food;

import adris.altoclef.AltoClef;
import adris.altoclef.Debug;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Optional;

//20260727_kpopmodder: Keeps unsafe food targets out of the main collection task flow.
public final class FoodCollectionBlacklist {
    private FoodCollectionBlacklist() {
    }

    public static void applyKnownFoodBlacklists(AltoClef mod) {
        blackListChickenJockeys(mod);
        blackListPillageHayBales(mod);
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
