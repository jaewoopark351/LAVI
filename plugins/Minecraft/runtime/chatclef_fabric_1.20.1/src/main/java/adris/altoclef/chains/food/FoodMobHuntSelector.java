package adris.altoclef.chains.food;

import adris.altoclef.AltoClef;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;

import java.util.Optional;
import java.util.function.Predicate;

//20260727_kpopmodder: Isolates mob food target scoring from CollectFoodTask control flow.
public final class FoodMobHuntSelector {
    private static final Predicate<Entity> NOT_BABY = entity -> entity instanceof LivingEntity livingEntity && !livingEntity.isBaby();
    private static final Predicate<Entity> HUNTABLE_FOOD_MOB = entity -> NOT_BABY.test(entity)
            && !FoodCollectionBlacklist.shouldSkipHuntTarget(entity);

    private FoodMobHuntSelector() {
    }

    @SuppressWarnings("unchecked")
    public static Optional<HuntTarget> selectBest(AltoClef mod, FoodCollectionTargets.CookableFoodTarget[] cookableFoods) {
        double bestScore = 0;
        Entity bestEntity = null;
        Item bestRawFood = null;

        for (FoodCollectionTargets.CookableFoodTarget cookable : cookableFoods) {
            if (!mod.getEntityTracker().entityFound(cookable.mobToKill)) continue;
            Optional<Entity> nearest = mod.getEntityTracker().getClosestEntity(mod.getPlayer().getPos(), HUNTABLE_FOOD_MOB, cookable.mobToKill);
            if (nearest.isEmpty()) continue;
            if (!nearest.get().isAlive()) continue;
            int hungerPerformance = cookable.getCookedUnits();
            double sqDistance = nearest.get().squaredDistanceTo(mod.getPlayer());
            double score = (double) 100 * hungerPerformance / sqDistance;
            if (cookable.isFish()) {
                score = 0;
            }
            if (score > bestScore) {
                bestScore = score;
                bestEntity = nearest.get();
                bestRawFood = cookable.getRaw();
            }
        }

        if (bestEntity == null) {
            return Optional.empty();
        }
        return Optional.of(new HuntTarget(bestEntity, bestRawFood, bestScore, HUNTABLE_FOOD_MOB));
    }

    public static final class HuntTarget {
        private final Entity entity;
        private final Item rawFood;
        private final double score;
        private final Predicate<Entity> entityPredicate;

        private HuntTarget(Entity entity, Item rawFood, double score, Predicate<Entity> entityPredicate) {
            this.entity = entity;
            this.rawFood = rawFood;
            this.score = score;
            this.entityPredicate = entityPredicate;
        }

        public Entity getEntity() {
            return entity;
        }

        public Item getRawFood() {
            return rawFood;
        }

        public double getScore() {
            return score;
        }

        public Predicate<Entity> getEntityPredicate() {
            return entityPredicate;
        }
    }
}
