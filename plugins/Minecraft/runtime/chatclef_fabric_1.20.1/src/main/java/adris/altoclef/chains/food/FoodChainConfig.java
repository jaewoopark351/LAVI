package adris.altoclef.chains.food;

//20260727_kpopmodder: Extracted food-chain config so FoodChain only coordinates behavior.
public class FoodChainConfig {
    public int alwaysEatWhenWitherOrFireAndHealthBelow = 6;
    public int alwaysEatWhenBelowHunger = 10;
    public int alwaysEatWhenBelowHealth = 14;
    public int alwaysEatWhenBelowHungerAndPerfectFit = 20 - 5;
    public int prioritizeSaturationWhenBelowHealth = 8;
    public float foodPickPrioritizeSaturationSaturationMultiplier = 8;
    public float foodPickSaturationWastePenaltyMultiplier = 1;
    public float foodPickHungerWastePenaltyMultiplier = 2;
    public float foodPickHungerNotFilledPenaltyMultiplier = 1;
    public float foodPickRottenFleshPenalty = 100;
    public float foodPickCookedFoodBonus = 2;
    public float runDontEatMaxHealth = 3;
    public int runDontEatMaxHunger = 3;
    public int canTankHitsAndEatArmor = 15;
    public int canTankHitsAndEatMaxHunger = 3;
    public boolean prepareFoodWhenSafe = true;
    public int preparedFoodMinimumUnits = 80;
    public int preparedFoodUnitsToCollect = 160;
    public float preparedFoodPriority = 54;
    public float cookingFoodPriority = 56;
    public boolean cookRawFoodWhenSafe = true;
    public float preparedFoodCookingContainerRange = 32;
    public int rawFoodCookingBatchMinimumItems = 4;
    public int rawFoodCookingPreferredFurnaces = 4;
    public boolean deferFoodActionsNearHostiles = true;
    public float deferFoodActionsHostileRange = 8;
    public float deferFoodActionsHostileRangeWhileEating = 14;
    public boolean deferUserTasksDuringFoodEmergency = true;
    public int deferUserTasksWhenHungerAtOrBelow = 10;
    public float deferUserTasksWhenHealthAtOrBelow = 10;
}
