package adris.altoclef.chains.food;

import adris.altoclef.catalogue.TaskCatalogue;
import adris.altoclef.multiversion.item.ItemVer;
import net.minecraft.block.Block;
import net.minecraft.item.Item;

import java.util.Objects;

//20260727_kpopmodder: Extracted food collection target metadata from CollectFoodTask.
public final class FoodCollectionTargets {
    private FoodCollectionTargets() {
    }

    @SuppressWarnings("rawtypes")
    public static class CookableFoodTarget {
        public String rawFood;
        public String cookedFood;
        public Class mobToKill;

        public CookableFoodTarget(String rawFood, String cookedFood, Class mobToKill) {
            this.rawFood = rawFood;
            this.cookedFood = cookedFood;
            this.mobToKill = mobToKill;
        }

        public CookableFoodTarget(String rawFood, Class mobToKill) {
            this(rawFood, "cooked_" + rawFood, mobToKill);
        }

        public Item getRaw() {
            return Objects.requireNonNull(TaskCatalogue.getItemMatches(rawFood))[0];
        }

        public Item getCooked() {
            return Objects.requireNonNull(TaskCatalogue.getItemMatches(cookedFood))[0];
        }

        public int getCookedUnits() {
            assert ItemVer.getFoodComponent(getCooked()) != null;
            return ItemVer.getFoodComponent(getCooked()).getHunger();
        }

        public boolean isFish() {
            return false;
        }
    }

    @SuppressWarnings("rawtypes")
    public static class CookableFoodTargetFish extends CookableFoodTarget {
        public CookableFoodTargetFish(String rawFood, Class mobToKill) {
            super(rawFood, mobToKill);
        }

        @Override
        public boolean isFish() {
            return true;
        }
    }

    public static class CropTarget {
        public Item cropItem;
        public Block cropBlock;

        public CropTarget(Item cropItem, Block cropBlock) {
            this.cropItem = cropItem;
            this.cropBlock = cropBlock;
        }
    }
}
