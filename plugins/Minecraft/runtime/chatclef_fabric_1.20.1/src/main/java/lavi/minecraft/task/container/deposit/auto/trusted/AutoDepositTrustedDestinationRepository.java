package lavi.minecraft.task.container.deposit.auto.trusted;

import adris.altoclef.Debug;
import adris.altoclef.util.Dimension;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.repository.AutoDepositTrustedBulkMergePlan;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.repository.AutoDepositTrustedBulkMergePlanner;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.repository.AutoDepositTrustedBulkMergeStatus;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.repository.AutoDepositTrustedBulkMutationResult;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.repository.AutoDepositTrustedBulkMutationStatus;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.topology.AutoDepositBulkLogicalDestination;
import lavi.minecraft.task.container.deposit.auto.trusted.persistence.AutoDepositTrustedConditionalSaveStatus;
import lavi.minecraft.task.container.deposit.auto.trusted.persistence.read.AutoDepositTrustedRegistryProvenance;
import lavi.minecraft.task.container.deposit.auto.trusted.persistence.read.AutoDepositTrustedRegistryReadResult;
import lavi.minecraft.task.container.deposit.auto.trusted.persistence.read.AutoDepositTrustedRegistryReadStatus;
import lavi.minecraft.task.container.deposit.auto.trusted.persistence.read.AutoDepositTrustedRegistrySnapshot;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

//20260827_kpopmodder: Own trusted destinations as injected instance state, separate from operation manifests.
public final class AutoDepositTrustedDestinationRepository {
    private final AutoDepositTrustedDestinationPersistence store;
    private final AutoDepositTrustedBulkMergePlanner bulkMergePlanner;
    private List<AutoDepositTrustedDestination> destinations;
    private long revision;
    private long observedModifiedTime;

    public AutoDepositTrustedDestinationRepository(AutoDepositTrustedDestinationPersistence store) {
        this.store = Objects.requireNonNull(store, "store");
        bulkMergePlanner = new AutoDepositTrustedBulkMergePlanner();
        destinations = loadSafely();
        observedModifiedTime = store.modifiedTime();
        revision = 1L;
    }

    private AutoDepositTrustedDestinationRepository() {
        store = null;
        bulkMergePlanner = new AutoDepositTrustedBulkMergePlanner();
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

    //20260904_kpopmodder: Commit one complete H5 logical-destination set without partial publication.
    public synchronized AutoDepositTrustedBulkMutationResult registerBulk(
            String worldKey,
            Dimension dimension,
            List<AutoDepositBulkLogicalDestination> logicalDestinations) {
        long revisionBefore = revision;
        AutoDepositTrustedRegistryReadResult readResult = strictSnapshot();
        if (!readResult.usable()) {
            return bulkResult(
                    AutoDepositTrustedBulkMutationStatus.REGISTRY_READ_FAILED,
                    readResult.reason(),
                    "",
                    readResult.status(),
                    null,
                    revisionBefore,
                    destinations.size(),
                    destinations.size()
            );
        }

        AutoDepositTrustedRegistrySnapshot snapshot = readResult.snapshot().orElseThrow();
        AutoDepositTrustedBulkMergePlan plan = bulkMergePlanner.plan(
                snapshot.destinations(),
                worldKey,
                dimension,
                logicalDestinations
        );
        if (!plan.status().valid()) {
            AutoDepositTrustedBulkMutationStatus failureStatus =
                    plan.status() == AutoDepositTrustedBulkMergeStatus.PREEXISTING_DOUBLE_CHEST_DUPLICATE
                            ? AutoDepositTrustedBulkMutationStatus.PREEXISTING_DOUBLE_CHEST_DUPLICATE
                            : AutoDepositTrustedBulkMutationStatus.INVALID_REQUEST;
            return bulkResult(
                    failureStatus,
                    plan.status().name().toLowerCase(),
                    plan.firstConflict(),
                    readResult.status(),
                    plan,
                    revisionBefore,
                    snapshot.destinations().size(),
                    snapshot.destinations().size()
            );
        }
        if (!plan.mutationRequired()) {
            if (!samePublishedDestinations(snapshot.destinations())) {
                return bulkResult(
                        AutoDepositTrustedBulkMutationStatus.EXTERNAL_MODIFICATION_CONFLICT,
                        "published_registry_state_differs_from_strict_snapshot",
                        "repository_snapshot_changed_before_no_change",
                        readResult.status(),
                        plan,
                        revisionBefore,
                        snapshot.destinations().size(),
                        snapshot.destinations().size()
                );
            }
            if (store != null) {
                observedModifiedTime = snapshot.provenance().modifiedTimeMillis();
            }
            return bulkResult(
                    AutoDepositTrustedBulkMutationStatus.NO_CHANGE,
                    logicalDestinations.isEmpty()
                            ? "no_supported_destinations_found"
                            : "all_destinations_already_enabled",
                    "",
                    readResult.status(),
                    plan,
                    revisionBefore,
                    snapshot.destinations().size(),
                    snapshot.destinations().size()
            );
        }

        if (store != null) {
            AutoDepositTrustedConditionalSaveStatus saveStatus;
            try {
                saveStatus = store.saveIfUnchanged(
                        snapshot.provenance(),
                        plan.finalDestinations()
                );
            } catch (IOException exception) {
                Debug.logError("Failed to persist bulk trusted automatic-deposit destinations: "
                        + exception.getMessage());
                return bulkResult(
                        AutoDepositTrustedBulkMutationStatus.PERSISTENCE_FAILED,
                        "persistence_failed",
                        "",
                        readResult.status(),
                        null,
                        revisionBefore,
                        snapshot.destinations().size(),
                        snapshot.destinations().size()
                );
            }
            if (saveStatus == AutoDepositTrustedConditionalSaveStatus.CONFLICT) {
                return bulkResult(
                        AutoDepositTrustedBulkMutationStatus.EXTERNAL_MODIFICATION_CONFLICT,
                        "external_modification_conflict",
                        "registry_provenance_changed",
                        readResult.status(),
                        null,
                        revisionBefore,
                        snapshot.destinations().size(),
                        snapshot.destinations().size()
                );
            }
            if (saveStatus != AutoDepositTrustedConditionalSaveStatus.SAVED) {
                return bulkResult(
                        AutoDepositTrustedBulkMutationStatus.PERSISTENCE_FAILED,
                        "conditional_save_unsupported",
                        "",
                        readResult.status(),
                        null,
                        revisionBefore,
                        snapshot.destinations().size(),
                        snapshot.destinations().size()
                );
            }
        }

        destinations = plan.finalDestinations();
        revision++;
        observedModifiedTime = store == null ? -1L : store.modifiedTime();
        return bulkResult(
                AutoDepositTrustedBulkMutationStatus.UPDATED,
                "bulk_registration_updated",
                "",
                readResult.status(),
                plan,
                revisionBefore,
                snapshot.destinations().size(),
                destinations.size()
        );
    }

    private boolean samePublishedDestinations(
            List<AutoDepositTrustedDestination> snapshotDestinations) {
        if (destinations.size() != snapshotDestinations.size()) {
            return false;
        }
        for (int index = 0; index < destinations.size(); index++) {
            AutoDepositTrustedDestination published = destinations.get(index);
            AutoDepositTrustedDestination snapshot = snapshotDestinations.get(index);
            if (!published.key().equals(snapshot.key())
                    || published.enabled() != snapshot.enabled()) {
                return false;
            }
        }
        return true;
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

    private AutoDepositTrustedRegistryReadResult strictSnapshot() {
        if (store != null) {
            return store.readStrictSnapshot();
        }
        AutoDepositTrustedRegistryReadStatus status = destinations.isEmpty()
                ? AutoDepositTrustedRegistryReadStatus.FILE_MISSING_VALID_EMPTY
                : AutoDepositTrustedRegistryReadStatus.VALID_POPULATED;
        return AutoDepositTrustedRegistryReadResult.success(
                status,
                new AutoDepositTrustedRegistrySnapshot(
                        destinations,
                        AutoDepositTrustedRegistryProvenance.missing()
                )
        );
    }

    private AutoDepositTrustedBulkMutationResult bulkResult(
            AutoDepositTrustedBulkMutationStatus status,
            String reason,
            String firstConflict,
            AutoDepositTrustedRegistryReadStatus readStatus,
            AutoDepositTrustedBulkMergePlan plan,
            long revisionBefore,
            int totalBefore,
            int totalAfter) {
        return AutoDepositTrustedBulkMutationResult.of(
                status,
                reason,
                firstConflict,
                readStatus,
                plan,
                revisionBefore,
                revision,
                totalBefore,
                totalAfter
        );
    }
}
