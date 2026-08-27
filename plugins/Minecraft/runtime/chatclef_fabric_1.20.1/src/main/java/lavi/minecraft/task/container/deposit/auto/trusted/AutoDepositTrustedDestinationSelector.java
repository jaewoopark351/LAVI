package lavi.minecraft.task.container.deposit.auto.trusted;

import adris.altoclef.AltoClef;
import adris.altoclef.util.Dimension;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

//20260827_kpopmodder: Snapshot all eligible trusted candidates in deterministic distance order.
public final class AutoDepositTrustedDestinationSelector {
    private static final int MAX_FINGERPRINT_STATES = 64;
    private static final int MAX_OPERATION_CANDIDATES = 64;
    private final AutoDepositTrustedDestinationRepository repository;
    private final int maximumDistance;
    private final AutoDepositTrustedDestinationEvaluator evaluator;

    public AutoDepositTrustedDestinationSelector(AutoDepositTrustedDestinationRepository repository,
                                                 int maximumDistance) {
        this(repository, maximumDistance, new AutoDepositTrustedDestinationRuntimeEvaluator());
    }

    AutoDepositTrustedDestinationSelector(AutoDepositTrustedDestinationRepository repository,
                                          int maximumDistance,
                                          AutoDepositTrustedDestinationEvaluator evaluator) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.maximumDistance = Math.max(1, maximumDistance);
        this.evaluator = Objects.requireNonNull(evaluator, "evaluator");
    }

    public AutoDepositTrustedDestinationInspection inspect(AltoClef mod,
                                                           String worldKey,
                                                           Dimension dimension,
                                                           int requiredEmptySlots) {
        repository.reloadIfChanged();
        long revision = repository.revision();
        if (worldKey == null || worldKey.equals("unavailable") || dimension == null) {
            return new AutoDepositTrustedDestinationInspection(
                    List.of(), revision, "world_key_unavailable"
            );
        }

        List<AutoDepositTrustedDestinationCandidate> candidates = new ArrayList<>();
        List<String> states = new ArrayList<>();
        for (AutoDepositTrustedDestination destination : repository.destinations()) {
            if (!destination.enabled()
                    || !destination.worldKey().equals(worldKey)
                    || destination.dimension() != dimension) {
                continue;
            }
            AutoDepositTrustedDestinationEvaluation evaluation = evaluator.evaluate(
                    mod, destination, Math.max(1, requiredEmptySlots), maximumDistance
            );
            if (evaluation.eligible()) {
                candidates.add(new AutoDepositTrustedDestinationCandidate(
                        destination,
                        evaluation.emptySlots(),
                        evaluation.distanceSquared(),
                        evaluation.state()
                ));
            }
            states.add(destination.destinationId() + "=" + evaluation.state());
        }
        candidates.sort(Comparator
                .comparingDouble(AutoDepositTrustedDestinationCandidate::distanceSquared)
                .thenComparing(Comparator.comparingInt(
                        AutoDepositTrustedDestinationCandidate::cachedEmptySlots).reversed())
                .thenComparing(AutoDepositTrustedDestinationCandidate::destinationId));
        states.sort(String::compareTo);
        String capacityState = boundedState(states);
        List<AutoDepositTrustedDestinationCandidate> boundedCandidates = List.copyOf(
                candidates.subList(0, Math.min(MAX_OPERATION_CANDIDATES, candidates.size()))
        );
        return new AutoDepositTrustedDestinationInspection(
                boundedCandidates,
                revision,
                capacityState
        );
    }

    private static String boundedState(List<String> states) {
        if (states.isEmpty()) {
            return "no_matching_registration";
        }
        int included = Math.min(MAX_FINGERPRINT_STATES, states.size());
        String result = String.join("|", states.subList(0, included));
        return included == states.size()
                ? result
                : result + "|truncated=" + (states.size() - included);
    }
}
