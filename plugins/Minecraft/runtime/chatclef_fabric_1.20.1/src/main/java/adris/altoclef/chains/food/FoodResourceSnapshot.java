package adris.altoclef.chains.food;

import adris.altoclef.AltoClef;
import adris.altoclef.util.helpers.StorageHelper;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.Locale;

//20260727_kpopmodder: Captures food resource counts for CollectFoodTask logging and decisions.
public final class FoodResourceSnapshot {
    private final int readyScore;
    private final double potential;
    private final String rawCounts;
    private final String cookedCounts;
    private final int wheatCount;
    private final int hayCount;

    private FoodResourceSnapshot(int readyScore, double potential, String rawCounts, String cookedCounts, int wheatCount, int hayCount) {
        this.readyScore = readyScore;
        this.potential = potential;
        this.rawCounts = rawCounts;
        this.cookedCounts = cookedCounts;
        this.wheatCount = wheatCount;
        this.hayCount = hayCount;
    }

    public static FoodResourceSnapshot capture(AltoClef mod, FoodCollectionTargets.CookableFoodTarget[] cookableFoods) {
        return new FoodResourceSnapshot(
                StorageHelper.calculateInventoryFoodScore(),
                FoodPotentialCalculator.calculateFoodPotential(mod, cookableFoods),
                describeCookableCounts(mod, cookableFoods, true),
                describeCookableCounts(mod, cookableFoods, false),
                mod.getItemStorage().getItemCount(Items.WHEAT),
                mod.getItemStorage().getItemCount(Items.HAY_BLOCK)
        );
    }

    public static int getTotalRawFoodCount(AltoClef mod, FoodCollectionTargets.CookableFoodTarget[] cookableFoods) {
        int total = 0;
        for (FoodCollectionTargets.CookableFoodTarget cookable : cookableFoods) {
            total += mod.getItemStorage().getItemCount(cookable.getRaw());
        }
        return total;
    }

    public String describe() {
        return "readyScore=" + readyScore
                + ", potential=" + formatDouble(potential)
                + ", raw=" + rawCounts
                + ", cooked=" + cookedCounts
                + ", wheat=" + wheatCount
                + ", hay=" + hayCount;
    }

    private static String describeCookableCounts(AltoClef mod, FoodCollectionTargets.CookableFoodTarget[] cookableFoods, boolean raw) {
        StringBuilder result = new StringBuilder();
        for (FoodCollectionTargets.CookableFoodTarget cookable : cookableFoods) {
            Item item = raw ? cookable.getRaw() : cookable.getCooked();
            int count = mod.getItemStorage().getItemCount(item);
            if (count <= 0) {
                continue;
            }
            if (result.length() > 0) {
                result.append(", ");
            }
            result.append(item.getTranslationKey()).append("=").append(count);
        }
        return result.length() == 0 ? "none" : result.toString();
    }

    private static String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
