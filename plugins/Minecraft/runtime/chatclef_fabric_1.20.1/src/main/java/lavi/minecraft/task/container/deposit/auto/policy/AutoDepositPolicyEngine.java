package lavi.minecraft.task.container.deposit.auto.policy;

import adris.altoclef.AltoClef;
import adris.altoclef.chains.UserTaskChain;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.Dimension;
import adris.altoclef.util.helpers.WorldHelper;
import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureSnapshot;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationInspection;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationSelector;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationState;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationStateReader;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositWorldKeyReader;
import lavi.minecraft.task.container.deposit.auto.working.WorkingSetSnapshot;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

//20260827_kpopmodder: Compose automatic-only protection, reserve, trusted routing, and exact planning.
public final class AutoDepositPolicyEngine {
    private final AutoDepositPolicyDefinition definition;
    private final AutoDepositStackSnapshotReader stackReader;
    private final AutoDepositHardProtectionPolicy hardProtectionPolicy;
    private final AutoDepositCategoryReservePolicy reservePolicy;
    private final AutoDepositPlanBuilder planBuilder;
    private final AutoDepositTrustedDestinationSelector trustedSelector;
    private final AutoDepositTrustedDestinationStateReader trustedStateReader;
    private final AutoDepositWorldKeyReader worldKeyReader;
    private final AutoDepositGeneralDestinationProbe destinationProbe;
    private final AtomicLong idleEpochs = new AtomicLong();

    public AutoDepositPolicyEngine(AutoDepositPolicyDefinition definition,
                                   AutoDepositTrustedDestinationRepository trustedRepository) {
        this.definition = Objects.requireNonNull(definition, "definition");
        AutoDepositItemClassificationPolicy classification =
                new AutoDepositItemClassificationPolicy(definition);
        stackReader = new AutoDepositStackSnapshotReader();
        hardProtectionPolicy = new AutoDepositHardProtectionPolicy(definition);
        reservePolicy = new AutoDepositCategoryReservePolicy(definition, classification);
        planBuilder = new AutoDepositPlanBuilder(definition, classification);
        trustedSelector = new AutoDepositTrustedDestinationSelector(
                Objects.requireNonNull(trustedRepository, "trustedRepository"),
                definition.trustedMaximumDistance()
        );
        trustedStateReader = new AutoDepositTrustedDestinationStateReader(
                trustedRepository, definition.trustedMaximumDistance()
        );
        worldKeyReader = new AutoDepositWorldKeyReader();
        destinationProbe = new AutoDepositGeneralDestinationProbe();
    }

    public static AutoDepositPolicyEngine inMemoryDefault() {
        return new AutoDepositPolicyEngine(
                new AutoDepositPolicyLoader().loadOrFailClosed(),
                AutoDepositTrustedDestinationRepository.inMemoryEmpty()
        );
    }

    public AutoDepositPlanningResult plan(AltoClef mod,
                                          DepositAllInventoryPressureSnapshot pressure,
                                          WorkingSetSnapshot workingSet) {
        Objects.requireNonNull(mod, "mod");
        Objects.requireNonNull(pressure, "pressure");
        Optional<AutoDepositContextSnapshot> contextOptional = createContext(mod, workingSet, null);
        if (contextOptional.isEmpty()) {
            return AutoDepositPlanningResult.failed(
                    AutoDepositPlanningResult.Status.CONTEXT_CHANGED,
                    "automatic_context_unavailable",
                    captureGateFingerprint(mod)
            );
        }
        AutoDepositContextSnapshot context = contextOptional.get();
        List<AutoDepositStackSnapshot> stacks = stackReader.read(mod);
        String exactBefore = exactInventoryFingerprint(stacks);
        AutoDepositPlan plan = buildPlan(mod, pressure, context, stacks);
        if (!context.matches(mod)
                || !context.taskPathFingerprint().equals(currentTaskPathFingerprint(mod))
                || !exactBefore.equals(exactInventoryFingerprint(stackReader.read(mod)))) {
            return AutoDepositPlanningResult.failed(
                    AutoDepositPlanningResult.Status.CONTEXT_CHANGED,
                    "automatic_plan_context_changed",
                    captureGateFingerprint(mod)
            );
        }
        if (!definition.loaded()) {
            return AutoDepositPlanningResult.failed(
                    AutoDepositPlanningResult.Status.POLICY_UNAVAILABLE,
                    "automatic_policy_unavailable",
                    plan.fingerprint()
            );
        }
        if (!plan.hasTargets() || plan.expectedFreedSlots() <= 0) {
            boolean conditional = plan.dispositions().containsValue(AutoDepositDisposition.CONDITIONAL_VALUABLE);
            return AutoDepositPlanningResult.noSafe(
                    conditional ? "no_trusted_destination_or_safe_surplus" : "no_safe_surplus",
                    plan
            );
        }
        return AutoDepositPlanningResult.ready(plan);
    }

    public AutoDepositDecisionFingerprint captureDecisionFingerprint(AltoClef mod,
                                                                     DepositAllInventoryPressureSnapshot pressure,
                                                                     WorkingSetSnapshot retainedWorkingSet,
                                                                     long retainedEpoch) {
        Optional<AutoDepositContextSnapshot> context = createContext(mod, retainedWorkingSet, retainedEpoch);
        if (context.isEmpty()) {
            return captureGateFingerprint(mod);
        }
        List<AutoDepositStackSnapshot> stacks = stackReader.read(mod);
        AutoDepositHardProtectionResult hardProtection = hardProtectionPolicy.evaluate(stacks);
        Map<Item, Integer> reserves = reservePolicy.allocate(stacks, hardProtection, true);
        AutoDepositPlanDraft draft = planBuilder.prepare(
                context.get(),
                stacks,
                hardProtection,
                reserves,
                pressure.occupiedSlots(),
                pressure.requiredReliefSlots()
        );
        AutoDepositTrustedDestinationState trustedState = trustedStateReader.read(
                mod, context.get().persistentWorldKey(), context.get().dimension()
        );
        return planBuilder.finish(
                draft,
                Optional.empty(),
                trustedState.repositoryRevision(),
                trustedState.capacityState()
        ).fingerprint();
    }

    public AutoDepositDecisionFingerprint captureGateFingerprint(AltoClef mod) {
        Object world = mod.getWorld() == null ? mod : mod.getWorld();
        Dimension dimension = mod.getWorld() == null ? Dimension.OVERWORLD : WorldHelper.getCurrentDimension();
        UserTaskChain chain = mod.getUserTaskChain();
        boolean active = chain != null && chain.isActive() && !chain.isRunningIdleTask();
        Task root = active ? chain.getCurrentTask() : null;
        String worldKey = worldKeyReader.read().orElse("unavailable");
        List<String> semantics = new ArrayList<>(currentTaskPathFingerprint(mod));
        stackReader.read(mod).stream()
                .map(AutoDepositStackSnapshot::semanticIdentity)
                .sorted()
                .forEach(semantics::add);
        return new AutoDepositDecisionFingerprint(
                world,
                root,
                dimension,
                worldKey,
                definition.revision(),
                0L,
                "gate_not_evaluated",
                semantics
        );
    }

    private AutoDepositPlan buildPlan(AltoClef mod,
                                      DepositAllInventoryPressureSnapshot pressure,
                                      AutoDepositContextSnapshot context,
                                      List<AutoDepositStackSnapshot> stacks) {
        AutoDepositHardProtectionResult hardProtection = hardProtectionPolicy.evaluate(stacks);
        boolean destinationResourceNeeded = !destinationProbe.hasUsableDestination(mod);
        Map<Item, Integer> reserves = reservePolicy.allocate(
                stacks, hardProtection, destinationResourceNeeded
        );
        AutoDepositPlanDraft draft = planBuilder.prepare(
                context,
                stacks,
                hardProtection,
                reserves,
                pressure.occupiedSlots(),
                pressure.requiredReliefSlots()
        );
        int trustedSlotsRequired = draft.trustedEmptySlotsRequired();
        AutoDepositTrustedDestinationInspection trusted = trustedSlotsRequired > 0
                ? trustedSelector.inspect(
                        mod,
                        context.persistentWorldKey(),
                        context.dimension(),
                        trustedSlotsRequired
                )
                : AutoDepositTrustedDestinationInspection.notRequired();
        Optional<BlockPos> trustedPosition = draft.conditionalItems().isEmpty()
                ? Optional.empty()
                : trusted.selection().map(selection -> selection.position());
        return planBuilder.finish(
                draft,
                trustedPosition,
                trusted.repositoryRevision(),
                trusted.capacityState()
        );
    }

    private Optional<AutoDepositContextSnapshot> createContext(AltoClef mod,
                                                                WorkingSetSnapshot workingSet,
                                                                Long retainedEpoch) {
        if (mod.getWorld() == null || mod.getPlayer() == null) {
            return Optional.empty();
        }
        UserTaskChain chain = mod.getUserTaskChain();
        boolean active = chain != null && chain.isActive() && !chain.isRunningIdleTask();
        if (active) {
            if (workingSet == null
                    || chain.getCurrentTask() != workingSet.userTaskRoot()
                    || mod.getWorld() != workingSet.worldIdentity()
                    || WorldHelper.getCurrentDimension() != workingSet.dimension()) {
                return Optional.empty();
            }
        } else if (workingSet != null) {
            return Optional.empty();
        }
        long epoch = retainedEpoch != null
                ? retainedEpoch
                : workingSet == null ? idleEpochs.incrementAndGet() : workingSet.epoch();
        return Optional.of(new AutoDepositContextSnapshot(
                mod.getWorld(),
                WorldHelper.getCurrentDimension(),
                worldKeyReader.read().orElse("unavailable"),
                epoch,
                active ? workingSet.userTaskRoot() : null,
                active ? workingSet : null,
                currentTaskPathFingerprint(mod)
        ));
    }

    private static List<String> currentTaskPathFingerprint(AltoClef mod) {
        List<String> result = new ArrayList<>();
        if (mod.getTaskRunner() != null && mod.getTaskRunner().getCurrentTaskChain() != null) {
            Object selectedChain = mod.getTaskRunner().getCurrentTaskChain();
            result.add("selectedChain=" + selectedChain.getClass().getName()
                    + "@" + System.identityHashCode(selectedChain));
        } else {
            result.add("selectedChain=none");
        }
        UserTaskChain chain = mod.getUserTaskChain();
        if (chain == null || !chain.isActive() || chain.isRunningIdleTask()) {
            result.add("idle");
            return List.copyOf(result);
        }
        chain.getTasks().stream()
                .map(task -> task.getClass().getName() + "@" + System.identityHashCode(task))
                .forEach(result::add);
        return List.copyOf(result);
    }

    private static String exactInventoryFingerprint(List<AutoDepositStackSnapshot> stacks) {
        return stacks.stream()
                .map(stack -> stack.semanticIdentity() + ":count=" + stack.count())
                .sorted()
                .reduce("", (left, right) -> left + "|" + right);
    }
}
