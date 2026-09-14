package lavi.minecraft.task.container.deposit.auto.admission.conditions;

import adris.altoclef.AltoClef;
import adris.altoclef.multiversion.item.ItemVer;
import adris.altoclef.util.ItemTarget;
import lavi.minecraft.task.container.deposit.auto.admission.conditions.gain.AutoDepositGainEvidence;
import lavi.minecraft.task.container.deposit.auto.admission.conditions.working.AutoDepositWorkingFailureEvidence;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositItemRoleClassifier;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPlan;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.working.PlayerInventorySnapshotReader;
import lavi.minecraft.task.container.deposit.auto.working.WorkingSetSnapshot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
//#if MC >= 11903
import net.minecraft.registry.Registries;
//#else
//$$ import net.minecraft.util.registry.Registry;
//#endif

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

//20260914_kpopmodder: Derive recheck evidence from gameplay values, never diagnostic fingerprints.
public final class AutoDepositConditionReader {
    private final AutoDepositDestinationConditionReader destinations;
    private final AutoDepositItemRoleClassifier roles = new AutoDepositItemRoleClassifier();
    private final AutoDepositGainEvidence survivalGains = new AutoDepositGainEvidence();
    private final AutoDepositWorkingFailureEvidence failedWorkingSet = new AutoDepositWorkingFailureEvidence();
    private final PlayerInventorySnapshotReader workingInventory = new PlayerInventorySnapshotReader();

    public AutoDepositConditionReader(AutoDepositTrustedDestinationRepository repository) {
        // Observation range matches the checked-in policy; callers with another policy pass its value.
        this(repository, 128);
    }

    public AutoDepositConditionReader(AutoDepositTrustedDestinationRepository repository, int trustedDistance) {
        destinations = new AutoDepositDestinationConditionReader(repository, trustedDistance);
    }

    public AutoDepositConditions read(AltoClef mod, AutoDepositPlan plan) {
        return read(mod, plan, 0);
    }

    public AutoDepositConditions read(AltoClef mod, AutoDepositPlan plan, long pressureEpisode) {
        if (mod == null || plan == null || mod.getPlayer() == null || mod.getWorld() == null
                || mod.getPlayer().getInventory() == null || mod.getWorld() != plan.context().worldIdentity()) {
            return AutoDepositConditions.unavailable("context_unavailable");
        }
        try {
            Map<String, Integer> inventory = new TreeMap<>();
            Map<String, Integer> survivalCounts = new TreeMap<>();
            for (ItemStack stack : mod.getPlayer().getInventory().main) addInventory(inventory, survivalCounts, stack);
            // Equipping armor or moving a weapon between hands preserves the observed total.
            for (ItemStack stack : mod.getPlayer().getInventory().armor) addInventory(inventory, survivalCounts, stack);
            for (ItemStack stack : mod.getPlayer().getInventory().offHand) addInventory(inventory, survivalCounts, stack);
            Map<String, Integer> required = new TreeMap<>();
            if (plan.context().workingSet() != null) {
                plan.context().workingSet().requiredCounts().forEach((item, count) -> {
                    if (includeRecoveryCount(item)) required.put(itemId(item), count);
                });
            }
            Map<String, Integer> exactPlan = new TreeMap<>();
            addTargets(exactPlan, "general:", plan.generalTargets());
            addTargets(exactPlan, "trusted:", plan.trustedTargets());
            plan.protectedCounts().forEach((item, count) -> exactPlan.put("protected:" + itemId(item), count));
            AutoDepositDestinationConditions destination = destinations.read(mod, plan);
            if (mod.getWorld() != plan.context().worldIdentity()) {
                return AutoDepositConditions.unavailable("context_changed_during_observation");
            }
            String gains = survivalGains.observe(plan.context().worldIdentity(), pressureEpisode,
                    survivalCounts, safeSurvivalTargets(plan));
            String restoredWorking = null;
            if (failedWorkingSet.matchesContext(plan.context().worldIdentity(), plan.context().dimension(),
                    plan.context().userTaskRoot())) {
                Map<String, Integer> mainAndCursor = new TreeMap<>();
                workingInventory.readMainAndCursor(mod).forEach((item, count) -> mainAndCursor.put(itemId(item), count));
                restoredWorking = failedWorkingSet.observe(plan.context().worldIdentity(), plan.context().dimension(),
                        plan.context().userTaskRoot(), mainAndCursor);
            }
            return new AutoDepositConditions(destination.scope(), inventory, required, exactPlan,
                    destination.states(), gains, restoredWorking);
        } catch (RuntimeException observationFailure) {
            // An unavailable observation cannot grant execution or disguise itself as zero capacity.
            return AutoDepositConditions.unavailable("observation_failed:" + observationFailure.getClass().getSimpleName());
        }
    }

    /** Called only for an authoritative WORKING_SET_DEFICIT terminal; use its return value for settlement. */
    public String captureWorkingSetFailure(WorkingSetSnapshot snapshot) {
        if (snapshot == null) return failedWorkingSet.capture(null, null, null, Map.of());
        Map<String, Integer> reserved = new TreeMap<>();
        snapshot.reservedCounts().forEach((item, count) -> reserved.put(itemId(item), count));
        return failedWorkingSet.capture(snapshot.worldIdentity(), snapshot.dimension(), snapshot.userTaskRoot(), reserved);
    }

    private void addInventory(Map<String, Integer> ordinary, Map<String, Integer> survival, ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            Map<String, Integer> counts = includeRecoveryCount(stack.getItem()) ? ordinary : survival;
            counts.merge(itemId(stack.getItem()), stack.getCount(), Integer::sum);
        }
    }

    private Set<String> safeSurvivalTargets(AutoDepositPlan plan) {
        Set<String> targets = new TreeSet<>();
        for (ItemTarget target : plan.allTargets()) {
            if (target == null || target.getMatches() == null) throw new IllegalArgumentException("Missing target");
            if (target.getTargetCount() <= 0) continue;
            for (Item item : target.getMatches()) {
                if (!includeRecoveryCount(item)) targets.add(itemId(item));
            }
        }
        return targets;
    }

    private boolean includeRecoveryCount(Item item) {
        Objects.requireNonNull(item, "item");
        // Survival equipment/durability and eating are not recovery from a storage failure.
        return !roles.classify(itemId(item)).isRecognizedEquipment()
                && !ItemVer.isFood(item.getDefaultStack());
    }

    private static void addTargets(Map<String, Integer> values, String route, ItemTarget[] targets) {
        for (ItemTarget target : targets) {
            if (target == null || target.getMatches() == null) throw new IllegalArgumentException("Missing target");
            String id = java.util.Arrays.stream(target.getMatches()).map(AutoDepositConditionReader::itemId)
                    .sorted().reduce((left, right) -> left + "|" + right).orElse("none");
            values.merge(route + id, target.getTargetCount(), Integer::sum);
        }
    }

    private static String itemId(Item item) {
//#if MC >= 11903
        return String.valueOf(Registries.ITEM.getId(item));
//#else
//$$         return String.valueOf(Registry.ITEM.getId(item));
//#endif
    }
}
