package adris.altoclef.chains.food;

import adris.altoclef.AltoClef;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

//20260727_kpopmodder: Isolates mob food target scoring from CollectFoodTask control flow.
public final class FoodMobHuntSelector {
    private static final int[] SEARCH_RADII = new int[]{32, 48, 64, 96, 160};
    private static final int MIN_CHASE_RADIUS = 64;
    private static final int CHASE_RADIUS_PADDING = 24;
    private static final double DISTANCE_TIE_EPSILON = 4.0;
    private static final Predicate<Entity> NOT_BABY = entity -> {
        if (!(entity instanceof LivingEntity)) {
            return false;
        }
        LivingEntity livingEntity = (LivingEntity) entity;
        return !livingEntity.isBaby();
    };

    private FoodMobHuntSelector() {
    }

    @SuppressWarnings("unchecked")
    public static Optional<HuntTarget> selectBest(AltoClef mod, FoodCollectionTargets.CookableFoodTarget[] cookableFoods) {
        for (int radius : SEARCH_RADII) {
            HuntTarget best = null;
            double radiusSq = (double) radius * radius;

            for (FoodCollectionTargets.CookableFoodTarget cookable : cookableFoods) {
                if (cookable.isFish()) {
                    continue;
                }
                if (!mod.getEntityTracker().entityFound(cookable.mobToKill)) {
                    continue;
                }

                List<Entity> entities = (List<Entity>) mod.getEntityTracker().getTrackedEntities(cookable.mobToKill);
                for (Entity entity : entities) {
                    if (!isHuntableFoodMob(entity)) {
                        continue;
                    }

                    double sqDistance = entity.squaredDistanceTo(mod.getPlayer());
                    if (sqDistance > radiusSq) {
                        continue;
                    }

                    HuntTarget candidate = createTarget(mod, entity, cookable, sqDistance, radius);
                    if (isBetterCandidate(candidate, best)) {
                        best = candidate;
                    }
                }
            }

            if (best != null) {
                return Optional.of(best);
            }
        }

        return Optional.empty();
    }

    private static HuntTarget createTarget(AltoClef mod, Entity entity, FoodCollectionTargets.CookableFoodTarget cookable, double distanceSq, int searchRadius) {
        int cookedUnits = cookable.getCookedUnits();
        double distance = Math.sqrt(distanceSq);
        double score = cookedUnits * 100.0 - distance;
        int chaseRadius = Math.max(MIN_CHASE_RADIUS, searchRadius + CHASE_RADIUS_PADDING);
        UUID selectedUuid = entity.getUuid();
        Predicate<Entity> predicate = candidate -> isHuntableFoodMob(candidate)
                && (candidate.getUuid().equals(selectedUuid)
                || candidate.squaredDistanceTo(mod.getPlayer()) <= (double) chaseRadius * chaseRadius);
        return new HuntTarget(entity, cookable.getRaw(), score, predicate, searchRadius, distanceSq, cookedUnits);
    }

    private static boolean isHuntableFoodMob(Entity entity) {
        return entity != null
                && entity.isAlive()
                && NOT_BABY.test(entity)
                && !FoodCollectionBlacklist.shouldSkipHuntTarget(entity);
    }

    private static boolean isBetterCandidate(HuntTarget candidate, HuntTarget best) {
        if (best == null) {
            return true;
        }
        double distanceDelta = candidate.distanceSq - best.distanceSq;
        if (distanceDelta < -DISTANCE_TIE_EPSILON) {
            return true;
        }
        if (Math.abs(distanceDelta) <= DISTANCE_TIE_EPSILON && candidate.cookedUnits > best.cookedUnits) {
            return true;
        }
        return Math.abs(distanceDelta) <= DISTANCE_TIE_EPSILON
                && candidate.cookedUnits == best.cookedUnits
                && candidate.score > best.score;
    }

    public static final class HuntTarget {
        private final Entity entity;
        private final Item rawFood;
        private final double score;
        private final Predicate<Entity> entityPredicate;
        private final int searchRadius;
        private final double distanceSq;
        private final int cookedUnits;

        private HuntTarget(Entity entity, Item rawFood, double score, Predicate<Entity> entityPredicate, int searchRadius, double distanceSq, int cookedUnits) {
            this.entity = entity;
            this.rawFood = rawFood;
            this.score = score;
            this.entityPredicate = entityPredicate;
            this.searchRadius = searchRadius;
            this.distanceSq = distanceSq;
            this.cookedUnits = cookedUnits;
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

        public int getSearchRadius() {
            return searchRadius;
        }

        public double getDistance() {
            return Math.sqrt(distanceSq);
        }

        public int getCookedUnits() {
            return cookedUnits;
        }
    }
}
