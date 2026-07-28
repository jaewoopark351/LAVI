package adris.altoclef.tasks.resources.food;

import adris.altoclef.AltoClef;
import adris.altoclef.chains.food.FoodCollectionPlan;
import adris.altoclef.chains.food.FoodCollectionTargets;
import adris.altoclef.chains.food.FoodCollectionTaskSelector;
import adris.altoclef.util.logging.StateChangeLogger;
import net.minecraft.item.Item;

import java.util.Optional;

//20260729_kpopmodder: Added this selector facade so CollectFoodTask does not own pickup/crop/hunt target choice.
final class FoodTargetSelector {
    private final FoodCollectionTargets.CookableFoodTarget[] cookableFoods;
    private final Item[] itemsToPickUp;
    private final FoodCollectionTargets.CropTarget[] crops;

    FoodTargetSelector(
            FoodCollectionTargets.CookableFoodTarget[] cookableFoods,
            Item[] itemsToPickUp,
            FoodCollectionTargets.CropTarget[] crops
    ) {
        this.cookableFoods = cookableFoods;
        this.itemsToPickUp = itemsToPickUp;
        this.crops = crops;
    }

    Optional<FoodCollectionPlan> select(AltoClef mod, StateChangeLogger debugLogger, FoodCollectionState state) {
        return FoodCollectionTaskSelector.select(
                mod,
                cookableFoods,
                itemsToPickUp,
                crops,
                debugLogger,
                state.describeResources()
        );
    }
}
