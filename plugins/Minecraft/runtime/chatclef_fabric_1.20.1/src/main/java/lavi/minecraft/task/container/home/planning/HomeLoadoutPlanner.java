package lavi.minecraft.task.container.home.planning;

import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositItemRole;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//20260827_kpopmodder: Apply the fixed V1 loadout and SAFE reserve policy per logical slot.
public final class HomeLoadoutPlanner {
    private static final int FOOD_RESERVE = 16;
    private static final int TORCH_RESERVE = 32;
    private static final int ARROW_RESERVE = 32;
    private long nextRevision = 1L;

    //20260828_kpopmodder: Plan from the occupied view of the authoritative activation capture.
    public HomeStoragePlan plan(HomeStorageInventorySnapshot snapshot) {
        return plan(snapshot.occupiedStacks());
    }

    public HomeStoragePlan plan(List<HomeStorageStackSnapshot> input) {
        List<HomeStorageStackSnapshot> snapshots = List.copyOf(input);
        Map<HomeStorageStackSnapshot, Decision> decisions = new LinkedHashMap<>();
        for (HomeStorageStackSnapshot snapshot : snapshots) {
            if (!snapshot.isMainInventory()) {
                decisions.put(snapshot, new Decision(
                        HomeStorageDisposition.KEEP_LOADOUT,
                        snapshot.location() == HomeStorageStackLocation.ARMOR
                                ? "currently_equipped_armor"
                                : "current_offhand"
                ));
            } else if (snapshot.explicitlyProtected()) {
                decisions.put(snapshot, new Decision(
                        HomeStorageDisposition.KEEP_EXPLICIT,
                        "existing_explicit_protection"
                ));
            } else {
                decisions.put(snapshot, new Decision(
                        HomeStorageDisposition.STORE_HOME,
                        "manual_home_surplus"
                ));
            }
        }

        keepBestRole(snapshots, decisions, AutoDepositItemRole.PICKAXE, "primary_pickaxe");
        HomeStorageStackSnapshot retainedAxe = keepBestRole(
                snapshots, decisions, AutoDepositItemRole.AXE, "primary_axe"
        );
        keepBestRole(snapshots, decisions, AutoDepositItemRole.SHOVEL, "primary_shovel");
        HomeStorageStackSnapshot retainedMelee = keepBestRole(
                snapshots, decisions, AutoDepositItemRole.MELEE_WEAPON, "primary_melee_weapon"
        );
        if (retainedMelee == null && retainedAxe != null) {
            keep(decisions, retainedAxe, HomeStorageDisposition.KEEP_LOADOUT,
                    "primary_axe_and_melee_weapon");
        }

        keepFoodReserve(snapshots, decisions);
        keepItemReserve(snapshots, decisions, "minecraft:torch", TORCH_RESERVE,
                "safe_torch_reserve");
        if (hasRetainedRangedWeapon(snapshots, decisions)) {
            keepItemReserve(snapshots, decisions, "minecraft:arrow", ARROW_RESERVE,
                    "safe_arrow_reserve");
        }
        keepItemReserve(snapshots, decisions, "minecraft:water_bucket", 1,
                "safe_water_bucket_reserve");

        long revision = nextRevision++;
        List<HomeStoragePlanEntry> entries = new ArrayList<>();
        List<HomeStorageManifestStep> steps = new ArrayList<>();
        for (HomeStorageStackSnapshot snapshot : snapshots) {
            Decision decision = decisions.get(snapshot);
            entries.add(new HomeStoragePlanEntry(
                    snapshot, decision.disposition(), decision.reason()
            ));
            if (snapshot.isMainInventory()
                    && decision.disposition() == HomeStorageDisposition.STORE_HOME) {
                steps.add(new HomeStorageManifestStep(
                        snapshot.logicalSlot(),
                        snapshot.fingerprint(),
                        snapshot.count(),
                        HomeStorageManifestStep.TransferMode.WHOLE_STACK_QUICK_MOVE,
                        decision.disposition(),
                        decision.reason(),
                        revision
                ));
            }
        }
        HomeStorageManifest manifest = new HomeStorageManifest(revision, steps);
        return new HomeStoragePlan(revision, entries, manifest);
    }

    private static HomeStorageStackSnapshot keepBestRole(
            List<HomeStorageStackSnapshot> snapshots,
            Map<HomeStorageStackSnapshot, Decision> decisions,
            AutoDepositItemRole role,
            String reason) {
        HomeStorageStackSnapshot best = snapshots.stream()
                .filter(HomeStorageStackSnapshot::isMainInventory)
                .filter(snapshot -> snapshot.role() == role)
                .max(HomeLoadoutPlanner::compareLoadoutCandidates)
                .orElse(null);
        if (best != null) {
            keep(decisions, best, HomeStorageDisposition.KEEP_LOADOUT, reason);
        }
        return best;
    }

    private static int compareLoadoutCandidates(
            HomeStorageStackSnapshot left,
            HomeStorageStackSnapshot right) {
        int result = Boolean.compare(
                !left.isDurabilityCritical(), !right.isDurabilityCritical()
        );
        if (result != 0) return result;
        result = Integer.compare(left.capabilityScore(), right.capabilityScore());
        if (result != 0) return result;
        result = Integer.compare(left.enchantmentScore(), right.enchantmentScore());
        if (result != 0) return result;
        result = compareDurabilityRatio(left, right);
        if (result != 0) return result;
        result = Integer.compare(left.remainingDurability(), right.remainingDurability());
        if (result != 0) return result;
        result = Boolean.compare(left.selectedMainHand(), right.selectedMainHand());
        if (result != 0) return result;
        return Integer.compare(right.logicalSlot(), left.logicalSlot());
    }

    private static int compareDurabilityRatio(
            HomeStorageStackSnapshot left,
            HomeStorageStackSnapshot right) {
        if (left.maximumDurability() == 0 || right.maximumDurability() == 0) {
            return Integer.compare(left.remainingDurability(), right.remainingDurability());
        }
        return Long.compare(
                (long) left.remainingDurability() * right.maximumDurability(),
                (long) right.remainingDurability() * left.maximumDurability()
        );
    }

    private static void keepFoodReserve(
            List<HomeStorageStackSnapshot> snapshots,
            Map<HomeStorageStackSnapshot, Decision> decisions) {
        Map<String, List<HomeStorageStackSnapshot>> byItem = new LinkedHashMap<>();
        snapshots.stream()
                .filter(HomeStorageStackSnapshot::safeGeneralFood)
                .forEach(snapshot -> byItem.computeIfAbsent(snapshot.itemId(), ignored -> new ArrayList<>())
                        .add(snapshot));
        String selectedFood = byItem.entrySet().stream()
                .max(Comparator
                        .comparing((Map.Entry<String, List<HomeStorageStackSnapshot>> entry) ->
                                entry.getValue().stream()
                                        .mapToInt(HomeStorageStackSnapshot::count)
                                        .sum() >= FOOD_RESERVE)
                        .thenComparingInt(entry ->
                                entry.getValue().stream()
                                        .mapToInt(HomeStorageStackSnapshot::foodScore)
                                        .max().orElse(0))
                        .thenComparingInt(entry -> entry.getValue().stream()
                                .mapToInt(HomeStorageStackSnapshot::count).sum())
                        .thenComparing(Map.Entry::getKey, Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .orElse(null);
        if (selectedFood != null) {
            keepReserveStacks(byItem.get(selectedFood), decisions, FOOD_RESERVE,
                    "safe_food_reserve");
        }
    }

    private static void keepItemReserve(
            List<HomeStorageStackSnapshot> snapshots,
            Map<HomeStorageStackSnapshot, Decision> decisions,
            String itemId,
            int targetCount,
            String reason) {
        List<HomeStorageStackSnapshot> matching = snapshots.stream()
                .filter(snapshot -> snapshot.itemId().equals(itemId))
                .toList();
        keepReserveStacks(matching, decisions, targetCount, reason);
    }

    private static void keepReserveStacks(
            List<HomeStorageStackSnapshot> matching,
            Map<HomeStorageStackSnapshot, Decision> decisions,
            int targetCount,
            String reason) {
        int retained = matching.stream()
                .filter(snapshot -> decisions.get(snapshot).disposition()
                        != HomeStorageDisposition.STORE_HOME)
                .mapToInt(HomeStorageStackSnapshot::count)
                .sum();
        List<HomeStorageStackSnapshot> candidates = matching.stream()
                .filter(HomeStorageStackSnapshot::isMainInventory)
                .filter(snapshot -> decisions.get(snapshot).disposition()
                        == HomeStorageDisposition.STORE_HOME)
                .sorted(Comparator
                        .comparingInt(HomeStorageStackSnapshot::count).reversed()
                        .thenComparingInt(HomeStorageStackSnapshot::logicalSlot))
                .toList();
        for (HomeStorageStackSnapshot candidate : candidates) {
            if (retained >= targetCount) {
                break;
            }
            keep(decisions, candidate, HomeStorageDisposition.KEEP_RESERVE, reason);
            retained += candidate.count();
        }
    }

    private static boolean hasRetainedRangedWeapon(
            List<HomeStorageStackSnapshot> snapshots,
            Map<HomeStorageStackSnapshot, Decision> decisions) {
        return snapshots.stream().anyMatch(snapshot ->
                snapshot.role().isRangedWeapon()
                        && decisions.get(snapshot).disposition()
                        != HomeStorageDisposition.STORE_HOME);
    }

    private static void keep(
            Map<HomeStorageStackSnapshot, Decision> decisions,
            HomeStorageStackSnapshot snapshot,
            HomeStorageDisposition disposition,
            String reason) {
        Decision current = decisions.get(snapshot);
        if (current.disposition() == HomeStorageDisposition.KEEP_EXPLICIT) {
            return;
        }
        decisions.put(snapshot, new Decision(disposition, reason));
    }

    private record Decision(HomeStorageDisposition disposition, String reason) {
    }
}
