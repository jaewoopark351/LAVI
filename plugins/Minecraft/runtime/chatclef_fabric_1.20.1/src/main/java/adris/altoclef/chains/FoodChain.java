package adris.altoclef.chains;

import adris.altoclef.AltoClef;
import adris.altoclef.Settings;
import adris.altoclef.chains.food.FoodChainConfig;
import adris.altoclef.chains.food.FoodCollectionDecision;
import adris.altoclef.chains.food.FoodCollectionPolicy;
import adris.altoclef.chains.food.FoodEatingController;
import adris.altoclef.chains.food.FoodEatingPolicy;
import adris.altoclef.chains.food.FoodInventoryEvaluator;
import adris.altoclef.chains.food.FoodInventoryResult;
import adris.altoclef.chains.food.FoodSafetyPolicy;
import adris.altoclef.tasks.resources.CollectFoodTask;
import adris.altoclef.tasks.speedrun.DragonBreathTracker;
import adris.altoclef.tasksystem.TaskRunner;
import adris.altoclef.util.helpers.ConfigHelper;
import adris.altoclef.util.helpers.LookHelper;
import adris.altoclef.util.logging.StateChangeLogger;
import net.minecraft.item.Item;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class FoodChain extends SingleTaskChain {
    private static FoodChainConfig config;
    private static boolean hasFood;

    static {
        ConfigHelper.loadConfig("configs/food_chain_settings.json", FoodChainConfig::new, FoodChainConfig.class, newConfig -> config = newConfig);
    }

    private final DragonBreathTracker dragonBreathTracker = new DragonBreathTracker();
    private final FoodSafetyPolicy safetyPolicy = new FoodSafetyPolicy();
    private final FoodInventoryEvaluator inventoryEvaluator = new FoodInventoryEvaluator();
    private final FoodEatingPolicy eatingPolicy = new FoodEatingPolicy();
    private final FoodEatingController eatingController = new FoodEatingController();
    private final FoodCollectionPolicy collectionPolicy = new FoodCollectionPolicy();
    private final StateChangeLogger decisionLogger = new StateChangeLogger("FoodChain.Decision");
    private final StateChangeLogger eatingLogger = new StateChangeLogger("FoodChain.Eating");
    private boolean needsFood = false;
    private Optional<Item> cachedPerfectFood = Optional.empty();
    private boolean shouldStop = false;

    public FoodChain(TaskRunner runner) {
        super(runner);
    }

    @Override
    protected void onTaskFinish(AltoClef mod) {
        // Nothing.
    }

    public boolean isTryingToEat() {
        return eatingController.isTryingToEat();
    }

    @Override
    public float getPriority() {
        AltoClef mod = AltoClef.getInstance();

        if (safetyPolicy.shouldPauseEating(mod, dragonBreathTracker, shouldStop)) {
            decisionLogger.state("paused by safety policy; shouldStop=" + shouldStop + ", " + describeVitals(mod));
            eatingController.stopEating();
            return Float.NEGATIVE_INFINITY;
        }

        FoodInventoryResult calculation = inventoryEvaluator.calculate(mod, config);
        int cachedFoodScore = calculation.getFoodScore();
        cachedPerfectFood = calculation.getBestFood();
        hasFood = calculation.hasFood();

        eatingController.updateFillupRequest(mod, hasFood);
        if (!updateEating(mod)) {
            return Float.NEGATIVE_INFINITY;
        }

        Settings settings = mod.getModSettings();
        return updateFoodCollection(settings, cachedFoodScore);
    }

    private boolean updateEating(AltoClef mod) {
        //FIXME should check if currently fighting
        if (hasFood && (needsToEat() || eatingController.isFillupRequested()) && cachedPerfectFood.isPresent()
                && !mod.getMLGBucketChain().isChorusFruiting()
                && !mod.getPlayer().isBlocking()/* && !safetyPolicy.areEnemiesNearby(mod, isTryingToEat())*/) {
            if (!LookHelper.tryAvoidingInteractable(mod)) {
                eatingLogger.state("eating deferred: could not avoid interactable; food=" + cachedPerfectFood.get().getTranslationKey() + ", " + describeVitals(mod));
                return false;
            }
            eatingLogger.state("eating: food=" + cachedPerfectFood.get().getTranslationKey() + ", " + describeVitals(mod));
            eatingController.startEating(mod, cachedPerfectFood.get());
        } else {
            eatingController.stopEating();
        }
        return true;
    }

    private float updateFoodCollection(Settings settings, int cachedFoodScore) {
        boolean wasNeedingFood = needsFood;
        FoodCollectionDecision decision = collectionPolicy.decide(cachedFoodScore, needsFood, settings);
        needsFood = decision.needsFood();

        if (decision.shouldCollect()) {
            decisionLogger.state("collecting food: score=" + cachedFoodScore
                    + ", min=" + settings.getMinimumFoodAllowed()
                    + ", target=" + decision.getTargetFoodUnits()
                    + ", wasNeedingFood=" + wasNeedingFood
                    + ", needsFood=" + needsFood);
            setTask(new CollectFoodTask(decision.getTargetFoodUnits()));
            return 55f;
        }
        decisionLogger.state("idle: score=" + cachedFoodScore
                + ", min=" + settings.getMinimumFoodAllowed()
                + ", target=" + decision.getTargetFoodUnits()
                + ", hasFood=" + hasFood
                + ", needsFood=" + needsFood);
        return Float.NEGATIVE_INFINITY;
    }

    private String describeVitals(AltoClef mod) {
        if (mod == null || mod.getPlayer() == null) {
            return "player=missing";
        }
        return "health=" + mod.getPlayer().getHealth()
                + ", hunger=" + mod.getPlayer().getHungerManager().getFoodLevel()
                + ", saturation=" + mod.getPlayer().getHungerManager().getSaturationLevel();
    }

    @Override
    public boolean isActive() {
        // We're always checking for food.
        return true;
    }

    @Override
    public String getName() {
        return "Food";
    }

    @Override
    protected void onStop() {
        super.onStop();
        eatingController.stopEating();
    }

    public boolean needsToEat() {
        return eatingPolicy.needsToEat(hasFood(), shouldStop, cachedPerfectFood, config);
    }

    // If we need to eat like, NOW.
    public boolean needsToEatCritical() {
        return false;
    }

    public boolean hasFood() {
        return hasFood;
    }

    public void shouldStop(boolean shouldStopInput) {
        if (shouldStop != shouldStopInput) {
            decisionLogger.event("shouldStop changed: " + shouldStop + " -> " + shouldStopInput);
        }
        shouldStop = shouldStopInput;
    }

    public boolean isShouldStop() {
        return shouldStop;
    }
}
