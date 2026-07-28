package adris.altoclef.tasks.resources.food;

import adris.altoclef.AltoClef;
import adris.altoclef.chains.food.FoodCollectionTargets;
import adris.altoclef.chains.food.FoodPotentialCalculator;
import adris.altoclef.chains.food.FoodResourceSnapshot;
import adris.altoclef.util.helpers.StorageHelper;

import java.util.Locale;

//20260729_kpopmodder: Added this snapshot to keep CollectFoodTask runtime decisions separate from task ordering.
final class FoodCollectionState {
    private final double unitsNeeded;
    private final double potentialFood;
    private final int readyFoodScore;
    private final int rawFoodCount;
    private final String resourceDescription;

    private FoodCollectionState(double unitsNeeded, double potentialFood, int readyFoodScore, int rawFoodCount, String resourceDescription) {
        this.unitsNeeded = unitsNeeded;
        this.potentialFood = potentialFood;
        this.readyFoodScore = readyFoodScore;
        this.rawFoodCount = rawFoodCount;
        this.resourceDescription = resourceDescription;
    }

    static FoodCollectionState capture(AltoClef mod, double unitsNeeded, FoodCollectionTargets.CookableFoodTarget[] cookableFoods) {
        return new FoodCollectionState(
                unitsNeeded,
                FoodPotentialCalculator.calculateFoodPotential(mod, cookableFoods),
                StorageHelper.calculateInventoryFoodScore(),
                FoodResourceSnapshot.getTotalRawFoodCount(mod, cookableFoods),
                FoodResourceSnapshot.capture(mod, cookableFoods).describe()
        );
    }

    double getPotentialFood() {
        return potentialFood;
    }

    boolean hasEnoughPotential() {
        return potentialFood >= unitsNeeded;
    }

    boolean hasRawFood() {
        return rawFoodCount > 0;
    }

    String describeResources() {
        return resourceDescription;
    }

    String describePotentialEnough() {
        return "potential enough: potential="
                + formatDouble(potentialFood)
                + ", target=" + formatDouble(unitsNeeded)
                + ", readyScore=" + readyFoodScore
                + ", " + resourceDescription;
    }

    String describeSearch() {
        return "searching for food: potential=" + formatDouble(potentialFood)
                + ", target=" + formatDouble(unitsNeeded)
                + ", " + resourceDescription;
    }

    private String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
