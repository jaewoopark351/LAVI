package lavi.minecraft.task.container.deposit.auto.admission.conditions;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

//20260914_kpopmodder: Separate exact plan revalidation from cause-related retry evidence.
public final class AutoDepositConditions {
    private final String scope;
    private final String inventoryKey;
    private final String workingKey;
    private final String destinationKey;
    private final String trustedDestinationKey;
    private final String generalDestinationKey;
    private final String planKey;
    private final String survivalGainKey;
    private final String workingFailureKey;
    private final boolean available;
    private final String readReason;

    public AutoDepositConditions(String scope, Map<String, Integer> inventoryCounts,
                                  Map<String, Integer> workingRequired,
                                  Map<String, Integer> planTargetsAndReserves,
                                  List<String> destinationStates) {
        this(scope, inventoryCounts, workingRequired, planTargetsAndReserves, destinationStates, "");
    }

    public AutoDepositConditions(String scope, Map<String, Integer> inventoryCounts,
                                  Map<String, Integer> workingRequired,
                                  Map<String, Integer> planTargetsAndReserves,
                                  List<String> destinationStates, String survivalGainEvidence) {
        this(scope, inventoryCounts, workingRequired, planTargetsAndReserves, destinationStates, survivalGainEvidence, null);
    }

    public AutoDepositConditions(String scope, Map<String, Integer> inventoryCounts,
                                  Map<String, Integer> workingRequired,
                                  Map<String, Integer> planTargetsAndReserves,
                                  List<String> destinationStates, String survivalGainEvidence, String workingFailureEvidence) {
        this.scope = Objects.requireNonNull(scope, "scope");
        survivalGainKey = Objects.requireNonNull(survivalGainEvidence, "survivalGainEvidence");
        workingFailureKey = workingFailureEvidence;
        inventoryKey = canonicalCounts(inventoryCounts);
        workingKey = canonicalCounts(workingRequired);
        planKey = canonicalCounts(planTargetsAndReserves);
        List<String> states = Objects.requireNonNull(destinationStates, "destinationStates");
        destinationKey = canonicalDestinations(states);
        trustedDestinationKey = canonicalDestinations(states.stream().filter(value -> value.startsWith("trusted:")).toList());
        generalDestinationKey = canonicalDestinations(states.stream().filter(value -> value.startsWith("general:")).toList());
        available = true;
        readReason = "observed";
    }

    private static String canonicalDestinations(List<String> destinationStates) {
        return destinationStates
                .stream().map(value -> Objects.requireNonNull(value, "destination state"))
                .distinct().sorted().map(AutoDepositConditions::frame).reduce("", String::concat);
    }

    private AutoDepositConditions(String reason) {
        scope = "automatic:unavailable";
        inventoryKey = workingKey = destinationKey = planKey = "";
        trustedDestinationKey = generalDestinationKey = "";
        survivalGainKey = "";
        workingFailureKey = null;
        available = false;
        readReason = Objects.requireNonNull(reason, "reason");
    }

    public static AutoDepositConditions unavailable(String reason) { return new AutoDepositConditions(reason); }
    public boolean available() { return available; }
    public String scope() { return scope; }
    public String readReason() { return readReason; }
    public String planKey() { return available ? planKey : null; }

    /** Compare the failure with a fresh observation using the same reason family. */
    public String conditionKey(String failureReason) {
        if (!available) return null;
        String reason = failureReason == null ? "" : failureReason.toLowerCase(Locale.ROOT);
        if (reason.contains("working_set_deficit")) return workingFailureKey;
        if (destinationFailure(reason)) {
            String relevant = reason.contains("trusted") ? trustedDestinationKey
                    : reason.contains("general") ? generalDestinationKey : destinationKey;
            return "destination:" + relevant;
        }
        return "inventory:" + inventoryKey + "working:" + workingKey
                + "survival_gain:" + frame(survivalGainKey) + "destination:" + destinationKey;
    }

    private static boolean destinationFailure(String reason) {
        if (reason.contains("no_safe") || reason.contains("safe_surplus") || reason.contains("no_slot_relief")) {
            return false;
        }
        return reason.contains("access") || reason.contains("unreachable") || reason.contains("container_full")
                || reason.contains("destination") || reason.contains("trusted_child")
                || reason.contains("general_child") || reason.contains("child_stopped")
                || reason.contains("budget") || reason.contains("tick_limit")
                || reason.contains("progress_limit") || reason.contains("followup_limit");
    }

    private static String canonicalCounts(Map<String, Integer> counts) {
        Objects.requireNonNull(counts, "counts");
        TreeMap<String, Integer> ordered = new TreeMap<>();
        counts.forEach((id, count) -> {
            Objects.requireNonNull(id, "item id");
            Objects.requireNonNull(count, "item count");
            if (count < 0) throw new IllegalArgumentException("Negative item count");
            if (count > 0) ordered.put(id, count);
        });
        StringBuilder key = new StringBuilder();
        ordered.forEach((id, count) -> key.append(frame(id)).append(count).append(';'));
        return key.toString();
    }

    private static String frame(String value) { return value.length() + ":" + value; }
}
