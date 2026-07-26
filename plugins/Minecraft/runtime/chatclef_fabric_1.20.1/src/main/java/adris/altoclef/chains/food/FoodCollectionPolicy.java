package adris.altoclef.chains.food;

import adris.altoclef.Settings;

//20260727_kpopmodder: Isolates when FoodChain should start or continue food collection.
public class FoodCollectionPolicy {
    public FoodCollectionDecision decide(int cachedFoodScore, boolean wasNeedingFood, Settings settings) {
        int targetFoodUnits = settings.getFoodUnitsToCollect();
        if (wasNeedingFood || cachedFoodScore < settings.getMinimumFoodAllowed()) {
            boolean stillNeedsFood = cachedFoodScore < targetFoodUnits;
            return new FoodCollectionDecision(stillNeedsFood, stillNeedsFood, targetFoodUnits);
        }
        return new FoodCollectionDecision(false, false, targetFoodUnits);
    }
}
