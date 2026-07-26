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
    public float runDontEatMaxHealth = 3;
    public int runDontEatMaxHunger = 3;
    public int canTankHitsAndEatArmor = 15;
    public int canTankHitsAndEatMaxHunger = 3;
}
