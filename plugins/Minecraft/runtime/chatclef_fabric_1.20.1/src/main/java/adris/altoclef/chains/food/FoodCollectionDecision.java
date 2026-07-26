package adris.altoclef.chains.food;

//20260727_kpopmodder: Added a small value object for food-collection decisions.
public class FoodCollectionDecision {
    private final boolean needsFood;
    private final boolean shouldCollect;
    private final int targetFoodUnits;

    FoodCollectionDecision(boolean needsFood, boolean shouldCollect, int targetFoodUnits) {
        this.needsFood = needsFood;
        this.shouldCollect = shouldCollect;
        this.targetFoodUnits = targetFoodUnits;
    }

    public boolean needsFood() {
        return needsFood;
    }

    public boolean shouldCollect() {
        return shouldCollect;
    }

    public int getTargetFoodUnits() {
        return targetFoodUnits;
    }
}
