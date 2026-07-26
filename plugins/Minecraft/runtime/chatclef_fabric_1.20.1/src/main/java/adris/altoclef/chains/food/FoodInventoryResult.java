package adris.altoclef.chains.food;

import net.minecraft.item.Item;

import java.util.Optional;

//20260727_kpopmodder: Holds the current inventory food score and best food candidate.
public class FoodInventoryResult {
    private final int foodScore;
    private final Optional<Item> bestFood;

    FoodInventoryResult(int foodScore, Optional<Item> bestFood) {
        this.foodScore = foodScore;
        this.bestFood = bestFood;
    }

    public int getFoodScore() {
        return foodScore;
    }

    public Optional<Item> getBestFood() {
        return bestFood;
    }

    public boolean hasFood() {
        return foodScore > 0;
    }
}
