package lavi.minecraft.diagnostics.resources.loot;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.helpers.WorldHelper;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;

import java.util.Optional;

//20260814_kpopmodder: Observe kill-and-loot discovery and item progress for cooked beef stalls.
public final class KillAndLootDiagnostics {
    private KillAndLootDiagnostics() {
    }

    public static void logStart(AltoClef mod,
                                Task task,
                                Class<?> targetClass,
                                ItemTarget[] itemTargets,
                                Task killTask) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        String fingerprint = KillAndLootDiagnosticEmitter.joinFingerprint(
                "KILL_AND_LOOT_LIFECYCLE",
                "START",
                targetClassName(targetClass),
                ChatClefDiagnostics.itemTargets(itemTargets),
                KillAndLootDiagnosticEmitter.taskClass(killTask)
        );
        KillAndLootDiagnosticEmitter.emit("KILL_AND_LOOT_LIFECYCLE", "kill_and_loot_start", task,
                "kill_and_loot_lifecycle|" + System.identityHashCode(task),
                fingerprint,
                new Object[]{
                        "owner", "kill_and_loot_lifecycle_observer",
                        "phase", "START",
                        "targetClass", targetClassName(targetClass),
                        "itemTargets", ChatClefDiagnostics.itemTargets(itemTargets),
                        "killTask", ChatClefDiagnostics.taskSummary(killTask),
                        "playerPosition", mod == null ? "unavailable" : ChatClefDiagnostics.playerPosition(mod)
                });
    }

    public static void logDecision(AltoClef mod,
                                   Task task,
                                   Class<?> targetClass,
                                   ItemTarget[] itemTargets,
                                   boolean entityFound,
                                   String decision,
                                   Task selectedTask) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        Observation observation = observe(mod, targetClass, itemTargets, entityFound);
        String normalizedDecision = normalize(decision);
        String fingerprint = KillAndLootDiagnosticEmitter.joinFingerprint(
                "KILL_AND_LOOT_ENTITY_DISCOVERY",
                normalizedDecision,
                Boolean.toString(entityFound),
                observation.closestEntitySummary,
                observation.closestDropSummary,
                observation.itemTargetCounts,
                observation.itemTargetsMetNoCursor,
                KillAndLootDiagnosticEmitter.taskClass(selectedTask)
        );
        KillAndLootDiagnosticEmitter.emit("KILL_AND_LOOT_ENTITY_DISCOVERY", "kill_and_loot_entity_discovery", task,
                "kill_and_loot_entity_discovery|" + System.identityHashCode(task),
                fingerprint,
                new Object[]{
                        "owner", "kill_and_loot_entity_discovery_observer",
                        "trigger", "resource_tick_decision_or_summary",
                        "decision", normalizedDecision,
                        "targetClass", targetClassName(targetClass),
                        "itemTargets", ChatClefDiagnostics.itemTargets(itemTargets),
                        "entityFound", entityFound,
                        "closestEntityPresent", observation.closestEntityPresent,
                        "closestEntity", observation.closestEntitySummary,
                        "closestEntityDistanceSqr", observation.closestEntityDistanceSqr,
                        "closestEntityReachable", observation.closestEntityReachable,
                        "targetItemDropped", observation.targetItemDropped,
                        "closestDropPresent", observation.closestDropPresent,
                        "closestDrop", observation.closestDropSummary,
                        "closestDropDistanceSqr", observation.closestDropDistanceSqr,
                        "itemTargetCounts", observation.itemTargetCounts,
                        "itemTargetsMetNoCursor", observation.itemTargetsMetNoCursor,
                        "selectedTaskClass", KillAndLootDiagnosticEmitter.taskClass(selectedTask),
                        "selectedTaskInstanceId", KillAndLootDiagnosticEmitter.instanceId(selectedTask),
                        "selectedTask", ChatClefDiagnostics.taskSummary(selectedTask),
                        "dimension", ChatClefDiagnostics.safeValue(WorldHelper::getCurrentDimension),
                        "playerPosition", mod == null ? "unavailable" : ChatClefDiagnostics.playerPosition(mod),
                        "baritonePathing", ChatClefDiagnostics.safeValue(() -> mod != null && mod.getClientBaritone().getPathingBehavior().isPathing()),
                        "customGoalActive", ChatClefDiagnostics.safeValue(() -> mod != null && mod.getClientBaritone().getCustomGoalProcess().isActive()),
                        "exploreActive", ChatClefDiagnostics.safeValue(() -> mod != null && mod.getClientBaritone().getExploreProcess().isActive())
                });
    }

    public static void logStop(AltoClef mod,
                               Task task,
                               Class<?> targetClass,
                               ItemTarget[] itemTargets,
                               Task killTask,
                               Task interruptTask) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return;
        }
        String fingerprint = KillAndLootDiagnosticEmitter.joinFingerprint(
                "KILL_AND_LOOT_LIFECYCLE",
                "STOP",
                targetClassName(targetClass),
                ChatClefDiagnostics.itemTargets(itemTargets),
                KillAndLootDiagnosticEmitter.taskClass(interruptTask)
        );
        KillAndLootDiagnosticEmitter.emit("KILL_AND_LOOT_LIFECYCLE", "kill_and_loot_stop", task,
                "kill_and_loot_lifecycle|" + System.identityHashCode(task),
                fingerprint,
                new Object[]{
                        "owner", "kill_and_loot_lifecycle_observer",
                        "phase", "STOP",
                        "targetClass", targetClassName(targetClass),
                        "itemTargets", ChatClefDiagnostics.itemTargets(itemTargets),
                        "killTask", ChatClefDiagnostics.taskSummary(killTask),
                        "interruptTask", ChatClefDiagnostics.taskSummary(interruptTask),
                        "playerPosition", mod == null ? "unavailable" : ChatClefDiagnostics.playerPosition(mod)
                });
    }

    private static Observation observe(AltoClef mod,
                                       Class<?> targetClass,
                                       ItemTarget[] itemTargets,
                                       boolean entityFound) {
        if (mod == null || mod.getPlayer() == null) {
            return Observation.unavailable(itemTargetCounts(mod, itemTargets), "unavailable");
        }
        Optional<Entity> closestEntity = entityFound && targetClass != null
                ? mod.getEntityTracker().getClosestEntity(mod.getPlayer().getPos(), targetClass)
                : Optional.empty();
        boolean targetItemDropped = itemTargets != null && itemTargets.length > 0 && mod.getEntityTracker().itemDropped(itemTargets);
        Optional<ItemEntity> closestDrop = targetItemDropped
                ? mod.getEntityTracker().getClosestItemDrop(mod.getPlayer().getPos(), itemTargets)
                : Optional.empty();
        String itemTargetsMetNoCursor = ChatClefDiagnostics.safeValue(() -> StorageHelper.itemTargetsMetInventoryNoCursor(itemTargets));
        return new Observation(
                closestEntity.isPresent(),
                closestEntity.map(ChatClefDiagnostics::entitySummary).orElse("none"),
                closestEntity.map(entity -> ChatClefDiagnostics.entityDistanceSqrToPlayer(mod, entity)).orElse("unavailable"),
                closestEntity.map(entity -> ChatClefDiagnostics.safeValue(() -> mod.getEntityTracker().isEntityReachable(entity))).orElse("unavailable"),
                targetItemDropped,
                closestDrop.isPresent(),
                closestDrop.map(ChatClefDiagnostics::entitySummary).orElse("none"),
                closestDrop.map(drop -> ChatClefDiagnostics.entityDistanceSqrToPlayer(mod, drop)).orElse("unavailable"),
                itemTargetCounts(mod, itemTargets),
                itemTargetsMetNoCursor
        );
    }

    private static String itemTargetCounts(AltoClef mod, ItemTarget[] itemTargets) {
        if (mod == null || itemTargets == null || itemTargets.length == 0) {
            return "none";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < itemTargets.length; i++) {
            ItemTarget target = itemTargets[i];
            if (i > 0) {
                builder.append(";");
            }
            if (target == null) {
                builder.append("null");
                continue;
            }
            int inventoryOnly = mod.getItemStorage().getItemCountInventoryOnly(target.getMatches());
            int total = mod.getItemStorage().getItemCount(target.getMatches());
            builder.append(target)
                    .append("#inventoryOnly=").append(inventoryOnly)
                    .append("#total=").append(total)
                    .append("#target=").append(target.getTargetCount());
        }
        return builder.toString();
    }

    private static String targetClassName(Class<?> targetClass) {
        return targetClass == null ? "none" : targetClass.getName();
    }

    private static String normalize(String value) {
        return value == null || value.isEmpty() ? "none" : value;
    }

    private static final class Observation {
        final boolean closestEntityPresent;
        final String closestEntitySummary;
        final String closestEntityDistanceSqr;
        final String closestEntityReachable;
        final boolean targetItemDropped;
        final boolean closestDropPresent;
        final String closestDropSummary;
        final String closestDropDistanceSqr;
        final String itemTargetCounts;
        final String itemTargetsMetNoCursor;

        private Observation(boolean closestEntityPresent,
                            String closestEntitySummary,
                            String closestEntityDistanceSqr,
                            String closestEntityReachable,
                            boolean targetItemDropped,
                            boolean closestDropPresent,
                            String closestDropSummary,
                            String closestDropDistanceSqr,
                            String itemTargetCounts,
                            String itemTargetsMetNoCursor) {
            this.closestEntityPresent = closestEntityPresent;
            this.closestEntitySummary = closestEntitySummary;
            this.closestEntityDistanceSqr = closestEntityDistanceSqr;
            this.closestEntityReachable = closestEntityReachable;
            this.targetItemDropped = targetItemDropped;
            this.closestDropPresent = closestDropPresent;
            this.closestDropSummary = closestDropSummary;
            this.closestDropDistanceSqr = closestDropDistanceSqr;
            this.itemTargetCounts = itemTargetCounts;
            this.itemTargetsMetNoCursor = itemTargetsMetNoCursor;
        }

        static Observation unavailable(String itemTargetCounts, String itemTargetsMetNoCursor) {
            return new Observation(false, "unavailable", "unavailable", "unavailable",
                    false, false, "unavailable", "unavailable", itemTargetCounts, itemTargetsMetNoCursor);
        }
    }
}
