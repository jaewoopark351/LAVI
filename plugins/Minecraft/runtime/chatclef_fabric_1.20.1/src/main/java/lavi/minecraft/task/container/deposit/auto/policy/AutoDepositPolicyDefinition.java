package lavi.minecraft.task.container.deposit.auto.policy;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class AutoDepositPolicyDefinition {
    private final int revision;
    private final Set<String> alwaysHardIds;
    private final Set<String> rareFunctionalIds;
    private final Set<String> valuableIds;
    private final Set<String> valuablePrefixes;
    private final Set<String> generalSurplusIds;
    private final Set<String> generalSurplusSuffixes;
    private final Set<String> safeBuildingIds;
    private final Set<String> harmfulOrSpecialFoodIds;
    private final int safeBuildingReserve;
    private final int logReserve;
    private final int plankFallbackReserve;
    private final int fuelReserve;
    private final int foodReserve;
    private final int torchReserve;
    private final int arrowReserve;
    private final int fireworkReserve;
    private final int waterBucketReserve;
    private final int destinationContainerReserve;
    private final int trustedMaximumDistance;

    public AutoDepositPolicyDefinition(int revision,
                                       Set<String> alwaysHardIds,
                                       Set<String> rareFunctionalIds,
                                       Set<String> valuableIds,
                                       Set<String> valuablePrefixes,
                                       Set<String> generalSurplusIds,
                                       Set<String> generalSurplusSuffixes,
                                       Set<String> safeBuildingIds,
                                       Set<String> harmfulOrSpecialFoodIds,
                                       int safeBuildingReserve,
                                       int logReserve,
                                       int plankFallbackReserve,
                                       int fuelReserve,
                                       int foodReserve,
                                       int torchReserve,
                                       int arrowReserve,
                                       int fireworkReserve,
                                       int waterBucketReserve,
                                       int destinationContainerReserve,
                                       int trustedMaximumDistance) {
        this.revision = revision;
        this.alwaysHardIds = immutableSet(alwaysHardIds);
        this.rareFunctionalIds = immutableSet(rareFunctionalIds);
        this.valuableIds = immutableSet(valuableIds);
        this.valuablePrefixes = immutableSet(valuablePrefixes);
        this.generalSurplusIds = immutableSet(generalSurplusIds);
        this.generalSurplusSuffixes = immutableSet(generalSurplusSuffixes);
        this.safeBuildingIds = immutableSet(safeBuildingIds);
        this.harmfulOrSpecialFoodIds = immutableSet(harmfulOrSpecialFoodIds);
        this.safeBuildingReserve = nonNegative(safeBuildingReserve);
        this.logReserve = nonNegative(logReserve);
        this.plankFallbackReserve = nonNegative(plankFallbackReserve);
        this.fuelReserve = nonNegative(fuelReserve);
        this.foodReserve = nonNegative(foodReserve);
        this.torchReserve = nonNegative(torchReserve);
        this.arrowReserve = nonNegative(arrowReserve);
        this.fireworkReserve = nonNegative(fireworkReserve);
        this.waterBucketReserve = nonNegative(waterBucketReserve);
        this.destinationContainerReserve = nonNegative(destinationContainerReserve);
        this.trustedMaximumDistance = Math.max(1, trustedMaximumDistance);
    }

    public static AutoDepositPolicyDefinition failClosed() {
        return new AutoDepositPolicyDefinition(
                -1,
                Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of(),
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1
        );
    }

    public int revision() {
        return revision;
    }

    public boolean loaded() {
        return revision >= 0;
    }

    public boolean isAlwaysHard(String itemId) {
        return alwaysHardIds.contains(itemId);
    }

    public boolean isRareFunctional(String itemId) {
        return rareFunctionalIds.contains(itemId);
    }

    public boolean isValuable(String itemId) {
        return valuableIds.contains(itemId) || valuablePrefixes.stream().anyMatch(itemId::startsWith);
    }

    public boolean isKnownGeneral(String itemId) {
        return generalSurplusIds.contains(itemId)
                || generalSurplusSuffixes.stream().anyMatch(itemId::endsWith);
    }

    public boolean isSafeBuilding(String itemId) {
        return safeBuildingIds.contains(itemId);
    }

    public boolean isHarmfulOrSpecialFood(String itemId) {
        return harmfulOrSpecialFoodIds.contains(itemId);
    }

    public int safeBuildingReserve() {
        return safeBuildingReserve;
    }

    public int logReserve() {
        return logReserve;
    }

    public int plankFallbackReserve() {
        return plankFallbackReserve;
    }

    public int fuelReserve() {
        return fuelReserve;
    }

    public int foodReserve() {
        return foodReserve;
    }

    public int torchReserve() {
        return torchReserve;
    }

    public int arrowReserve() {
        return arrowReserve;
    }

    public int fireworkReserve() {
        return fireworkReserve;
    }

    public int waterBucketReserve() {
        return waterBucketReserve;
    }

    public int destinationContainerReserve() {
        return destinationContainerReserve;
    }

    public int trustedMaximumDistance() {
        return trustedMaximumDistance;
    }

    private static Set<String> immutableSet(Set<String> source) {
        Objects.requireNonNull(source, "source");
        return Collections.unmodifiableSet(new LinkedHashSet<>(source));
    }

    private static int nonNegative(int value) {
        return Math.max(0, value);
    }
}
