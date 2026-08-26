package lavi.minecraft.task.container.deposit.auto.trusted;

import adris.altoclef.Debug;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

//20260827_kpopmodder: Own trusted destinations as injected instance state, separate from operation manifests.
public final class AutoDepositTrustedDestinationRepository {
    private final AutoDepositTrustedDestinationStore store;
    private List<AutoDepositTrustedDestination> destinations;
    private long revision;
    private long observedModifiedTime;

    public AutoDepositTrustedDestinationRepository(AutoDepositTrustedDestinationStore store) {
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

    public synchronized void register(AutoDepositTrustedDestination destination) {
        Objects.requireNonNull(destination, "destination");
        Map<String, AutoDepositTrustedDestination> byKey = new LinkedHashMap<>();
        destinations.forEach(existing -> byKey.put(existing.key(), existing));
        byKey.put(destination.key(), destination);
        destinations = List.copyOf(byKey.values());
        revision++;
        saveSafely(destinations);
        observedModifiedTime = store == null ? -1L : store.modifiedTime();
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
            return store.load();
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
}
