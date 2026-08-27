package lavi.minecraft.task.container.deposit.auto.trusted;

import adris.altoclef.Debug;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

//20260827_kpopmodder: Own trusted destinations as injected instance state, separate from operation manifests.
public final class AutoDepositTrustedDestinationRepository {
    private final AutoDepositTrustedDestinationPersistence store;
    private List<AutoDepositTrustedDestination> destinations;
    private long revision;
    private long observedModifiedTime;

    public AutoDepositTrustedDestinationRepository(AutoDepositTrustedDestinationPersistence store) {
        this.store = Objects.requireNonNull(store, "store");
        destinations = loadSafely();
        observedModifiedTime = store.modifiedTime();
        if (observedModifiedTime < 0) {
            saveSafely(destinations);
            observedModifiedTime = store.modifiedTime();
        }
        revision = 1L;
    }

    private AutoDepositTrustedDestinationRepository() {
        store = null;
        destinations = List.of();
        revision = 1L;
        observedModifiedTime = -1L;
    }

    public static AutoDepositTrustedDestinationRepository inMemoryEmpty() {
        return new AutoDepositTrustedDestinationRepository();
    }

    public synchronized List<AutoDepositTrustedDestination> destinations() {
        reloadIfChanged();
        return destinations;
    }

    public synchronized long revision() {
        reloadIfChanged();
        return revision;
    }

    public synchronized AutoDepositTrustedDestinationMutationResult register(
            AutoDepositTrustedDestination destination) {
        Objects.requireNonNull(destination, "destination");
        reloadIfChanged();
        Map<String, AutoDepositTrustedDestination> byKey = new LinkedHashMap<>();
        destinations.forEach(existing -> byKey.put(existing.key(), existing));
        AutoDepositTrustedDestination existing = byKey.get(destination.key());
        if (existing != null && existing.enabled() == destination.enabled()) {
            return AutoDepositTrustedDestinationMutationResult.of(
                    AutoDepositTrustedDestinationMutationResult.Status.ALREADY_REGISTERED,
                    existing,
                    "trusted destination already registered"
            );
        }
        byKey.put(destination.key(), destination);
        AutoDepositTrustedDestinationMutationResult.Status status = existing == null
                ? AutoDepositTrustedDestinationMutationResult.Status.REGISTERED
                : AutoDepositTrustedDestinationMutationResult.Status.UPDATED;
        return persistMutation(List.copyOf(byKey.values()), status, destination);
    }

    public synchronized AutoDepositTrustedDestinationMutationResult unregister(
            AutoDepositTrustedDestination destination) {
        Objects.requireNonNull(destination, "destination");
        reloadIfChanged();
        AutoDepositTrustedDestination existing = destinations.stream()
                .filter(candidate -> candidate.key().equals(destination.key()))
                .findFirst()
                .orElse(null);
        if (existing == null) {
            return AutoDepositTrustedDestinationMutationResult.of(
                    AutoDepositTrustedDestinationMutationResult.Status.NOT_FOUND,
                    destination,
                    "trusted destination is not registered"
            );
        }
        List<AutoDepositTrustedDestination> updated = destinations.stream()
                .filter(candidate -> !candidate.key().equals(destination.key()))
                .toList();
        return persistMutation(
                updated,
                AutoDepositTrustedDestinationMutationResult.Status.REMOVED,
                existing
        );
    }

    public synchronized AutoDepositTrustedDestinationMutationResult unregisterById(
            String destinationId) {
        String normalized = destinationId == null ? "" : destinationId.trim();
        reloadIfChanged();
        List<AutoDepositTrustedDestination> matches = destinations.stream()
                .filter(destination -> destination.destinationId().equals(normalized))
                .toList();
        if (matches.isEmpty()) {
            return AutoDepositTrustedDestinationMutationResult.of(
                    AutoDepositTrustedDestinationMutationResult.Status.NOT_FOUND,
                    null,
                    "trusted destination ID was not found"
            );
        }
        if (matches.size() > 1) {
            return AutoDepositTrustedDestinationMutationResult.of(
                    AutoDepositTrustedDestinationMutationResult.Status.AMBIGUOUS_ID,
                    null,
                    "trusted destination ID is ambiguous"
            );
        }
        return unregister(matches.get(0));
    }

    public synchronized Optional<AutoDepositTrustedDestination> findById(String destinationId) {
        String normalized = destinationId == null ? "" : destinationId.trim();
        reloadIfChanged();
        List<AutoDepositTrustedDestination> matches = destinations.stream()
                .filter(destination -> destination.destinationId().equals(normalized))
                .toList();
        return matches.size() == 1 ? Optional.of(matches.get(0)) : Optional.empty();
    }

    public synchronized boolean containsEnabled(AutoDepositTrustedDestination destination) {
        Objects.requireNonNull(destination, "destination");
        reloadIfChanged();
        return destinations.stream().anyMatch(candidate -> candidate.enabled()
                && candidate.key().equals(destination.key())
                && candidate.destinationId().equals(destination.destinationId()));
    }

    public synchronized void reloadIfChanged() {
        if (store == null) {
            return;
        }
        long modified = store.modifiedTime();
        if (modified == observedModifiedTime) {
            return;
        }
        destinations = loadSafely();
        observedModifiedTime = modified;
        revision++;
    }

    private List<AutoDepositTrustedDestination> loadSafely() {
        if (store == null) {
            return List.of();
        }
        try {
            Map<String, AutoDepositTrustedDestination> unique = new LinkedHashMap<>();
            store.load().forEach(destination -> unique.put(destination.key(), destination));
            return List.copyOf(unique.values());
        } catch (IOException exception) {
            Debug.logError("Failed to load trusted automatic-deposit destinations; valuables remain protected: "
                    + exception.getMessage());
            return List.of();
        }
    }

    private void saveSafely(List<AutoDepositTrustedDestination> values) {
        if (store == null) {
            destinations = List.copyOf(new ArrayList<>(values));
            return;
        }
        try {
            store.save(values);
        } catch (IOException exception) {
            Debug.logError("Failed to persist trusted automatic-deposit destinations: " + exception.getMessage());
        }
    }

    private AutoDepositTrustedDestinationMutationResult persistMutation(
            List<AutoDepositTrustedDestination> updated,
            AutoDepositTrustedDestinationMutationResult.Status successStatus,
            AutoDepositTrustedDestination destination) {
        if (store != null) {
            try {
                store.save(updated);
            } catch (IOException exception) {
                Debug.logError("Failed to persist trusted automatic-deposit destinations: "
                        + exception.getMessage());
                return AutoDepositTrustedDestinationMutationResult.of(
                        AutoDepositTrustedDestinationMutationResult.Status.PERSISTENCE_FAILED,
                        destination,
                        exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage()
                );
            }
        }
        destinations = List.copyOf(updated);
        revision++;
        observedModifiedTime = store == null ? -1L : store.modifiedTime();
        return AutoDepositTrustedDestinationMutationResult.of(
                successStatus,
                destination,
                successStatus.name().toLowerCase()
        );
    }
}
