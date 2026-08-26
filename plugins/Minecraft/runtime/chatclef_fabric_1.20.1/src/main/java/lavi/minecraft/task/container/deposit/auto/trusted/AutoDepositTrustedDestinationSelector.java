package lavi.minecraft.task.container.deposit.auto.trusted;

import adris.altoclef.AltoClef;
import adris.altoclef.util.Dimension;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class AutoDepositTrustedDestinationSelector {
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
            return new AutoDepositTrustedDestinationInspection(null, revision, "world_key_unavailable");
        }

        List<Candidate> candidates = new ArrayList<>();
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
            Candidate candidate = new Candidate(destination, evaluation);
            candidates.add(candidate);
            states.add(destination.position().toShortString() + "=" + candidate.state);
        }
        candidates.sort(Comparator.comparingDouble(candidate -> candidate.distanceSquared));
        Candidate selected = candidates.stream().filter(candidate -> candidate.eligible).findFirst().orElse(null);
        String capacityState = states.isEmpty() ? "no_matching_registration" : String.join("|", states);
        return new AutoDepositTrustedDestinationInspection(
                selected == null ? null : new AutoDepositTrustedDestinationSelection(
                        selected.destination.position(), selected.emptySlots
                ),
                revision,
                capacityState
        );
    }

    private static final class Candidate {
        private final AutoDepositTrustedDestination destination;
        private final boolean eligible;
        private final int emptySlots;
        private final double distanceSquared;
        private final String state;

        private Candidate(AutoDepositTrustedDestination destination,
                          AutoDepositTrustedDestinationEvaluation evaluation) {
            this.destination = destination;
            eligible = evaluation.eligible();
            emptySlots = evaluation.emptySlots();
            distanceSquared = evaluation.distanceSquared();
            state = evaluation.state();
        }
    }
}
