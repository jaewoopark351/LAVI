package adris.altoclef.tasks.resources.food;

import adris.altoclef.AltoClef;
import adris.altoclef.chains.food.FoodCollectionTargets;
import adris.altoclef.chains.food.FoodConversionPlanner;

import java.util.Optional;

//20260729_kpopmodder: Added this decision object to isolate raw-food conversion planning from CollectFoodTask flow.
final class FoodCookingDecision {
    private final FoodCollectionState state;
    private final boolean enoughPotential;
    private final Optional<FoodConversionPlanner.Plan> conversionPlan;

    private FoodCookingDecision(FoodCollectionState state, boolean enoughPotential, Optional<FoodConversionPlanner.Plan> conversionPlan) {
        this.state = state;
        this.enoughPotential = enoughPotential;
        this.conversionPlan = conversionPlan;
    }

    static FoodCookingDecision decide(
            AltoClef mod,
            FoodCollectionState state,
            FoodCollectionTargets.CookableFoodTarget[] cookableFoods
    ) {
        if (!state.hasEnoughPotential()) {
            return new FoodCookingDecision(state, false, Optional.empty());
        }
        return new FoodCookingDecision(state, true, FoodConversionPlanner.plan(mod, cookableFoods));
    }

    boolean hasEnoughPotential() {
        return enoughPotential;
    }

    Optional<FoodConversionPlanner.Plan> getConversionPlan() {
        return conversionPlan;
    }

    boolean hasRawFood() {
        return state.hasRawFood();
    }

    String describePotentialEnough() {
        return state.describePotentialEnough();
    }

    String describeRawFoodNotSelected() {
        return "raw food not selected for cooking: " + state.describeResources();
    }

    String describePotentialMetButNoTarget() {
        return "potential met but no conversion target: " + state.describeResources();
    }
}
