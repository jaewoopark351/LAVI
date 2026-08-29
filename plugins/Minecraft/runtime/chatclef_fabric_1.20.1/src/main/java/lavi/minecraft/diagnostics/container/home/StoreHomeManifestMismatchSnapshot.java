package lavi.minecraft.diagnostics.container.home;

import lavi.minecraft.task.container.home.planning.HomeStorageManifestStep;
import lavi.minecraft.task.container.home.execution.session.HomeStorageManifestValidation;
import lavi.minecraft.task.container.home.planning.HomeStorageInventorySlotSnapshot;
import net.minecraft.item.ItemStack;

import java.util.Objects;
import java.util.Optional;

//20260828_kpopmodder: Preserve the exact expected and actual values used by one stale decision.
public final class StoreHomeManifestMismatchSnapshot {
    private final int manifestStepIndex;
    private final int logicalPlayerSlot;
    private final String slotLocation;
    private final int expectedSelectedMainSlot;
    private final int actualSelectedMainSlot;
    private final int expectedRemainingCount;
    private final String expectedDisposition;
    private final String expectedDispositionReason;
    private final String actualObservationSource;
    private final StoreHomeStackIdentitySnapshot expected;
    private final StoreHomeStackIdentitySnapshot actual;
    private final String itemIdEqual;
    private final String countEqual;
    private final String damageEqual;
    private final String metadataDigestEqual;
    private final String fullFingerprintEqual;
    private final String captureStatus;
    private final String errorClass;

    private StoreHomeManifestMismatchSnapshot(
            int manifestStepIndex,
            int logicalPlayerSlot,
            String slotLocation,
            int expectedSelectedMainSlot,
            int actualSelectedMainSlot,
            int expectedRemainingCount,
            String expectedDisposition,
            String expectedDispositionReason,
            String actualObservationSource,
            StoreHomeStackIdentitySnapshot expected,
            StoreHomeStackIdentitySnapshot actual,
            String itemIdEqual,
            String countEqual,
            String damageEqual,
            String metadataDigestEqual,
            String fullFingerprintEqual,
            String captureStatus,
            String errorClass) {
        this.manifestStepIndex = manifestStepIndex;
        this.logicalPlayerSlot = logicalPlayerSlot;
        this.slotLocation = Objects.requireNonNull(slotLocation, "slotLocation");
        this.expectedSelectedMainSlot = expectedSelectedMainSlot;
        this.actualSelectedMainSlot = actualSelectedMainSlot;
        this.expectedRemainingCount = Math.max(0, expectedRemainingCount);
        this.expectedDisposition = Objects.requireNonNull(expectedDisposition, "expectedDisposition");
        this.expectedDispositionReason = Objects.requireNonNull(
                expectedDispositionReason, "expectedDispositionReason"
        );
        this.actualObservationSource = Objects.requireNonNull(
                actualObservationSource, "actualObservationSource"
        );
        this.expected = Objects.requireNonNull(expected, "expected");
        this.actual = Objects.requireNonNull(actual, "actual");
        this.itemIdEqual = Objects.requireNonNull(itemIdEqual, "itemIdEqual");
        this.countEqual = Objects.requireNonNull(countEqual, "countEqual");
        this.damageEqual = Objects.requireNonNull(damageEqual, "damageEqual");
        this.metadataDigestEqual = Objects.requireNonNull(
                metadataDigestEqual, "metadataDigestEqual"
        );
        this.fullFingerprintEqual = Objects.requireNonNull(
                fullFingerprintEqual, "fullFingerprintEqual"
        );
        this.captureStatus = Objects.requireNonNull(captureStatus, "captureStatus");
        this.errorClass = Objects.requireNonNull(errorClass, "errorClass");
    }

    public static StoreHomeManifestMismatchSnapshot capture(
            HomeStorageManifestStep step,
            int manifestStepIndex,
            int expectedRemainingCount,
            ItemStack actualStack,
            boolean fingerprintMatched,
            String actualObservationSource) {
        Objects.requireNonNull(step, "step");
        StoreHomeStackIdentitySnapshot expected =
                StoreHomeStackIdentitySnapshot.captureExpected(
                        step.fingerprint(), expectedRemainingCount
                );
        StoreHomeStackIdentitySnapshot actual =
                StoreHomeStackIdentitySnapshot.captureActual(actualStack);
        return fromSnapshots(
                manifestStepIndex,
                step.logicalPlayerInventorySlot(),
                expectedRemainingCount,
                step.disposition().name(),
                step.dispositionReason(),
                actualObservationSource,
                expected,
                actual,
                Boolean.toString(fingerprintMatched)
        );
    }

    //20260828_kpopmodder: Materialize diagnostics from the immutable facts that caused rejection.
    public static StoreHomeManifestMismatchSnapshot fromValidation(
            HomeStorageManifestValidation validation) {
        Objects.requireNonNull(validation, "validation");
        Optional<HomeStorageInventorySlotSnapshot> expectedSlot = validation.expected();
        Optional<HomeStorageInventorySlotSnapshot> actualSlot = validation.actual();
        StoreHomeStackIdentitySnapshot expected = expectedSlot
                .map(StoreHomeStackIdentitySnapshot::capture)
                .orElseGet(() -> StoreHomeStackIdentitySnapshot.unavailable(
                        "expected_snapshot_unavailable"
                ));
        StoreHomeStackIdentitySnapshot actual = actualSlot
                .map(StoreHomeStackIdentitySnapshot::capture)
                .orElseGet(() -> StoreHomeStackIdentitySnapshot.unavailable(
                        "actual_snapshot_unavailable"
                ));
        String fingerprintEqual = expectedSlot.isPresent() && actualSlot.isPresent()
                ? Boolean.toString(expectedSlot.orElseThrow().fingerprint().equals(
                actualSlot.orElseThrow().fingerprint()))
                : StoreHomeStackIdentitySnapshot.NOT_AVAILABLE;
        return fromSnapshots(
                validation.manifestStepIndex(),
                validation.logicalSlot(),
                validation.location().map(location -> location.name())
                        .orElse(StoreHomeStackIdentitySnapshot.NOT_AVAILABLE),
                validation.expectedSelectedMainSlot().orElse(-1),
                validation.actualSelectedMainSlot().orElse(-1),
                expectedSlot.map(HomeStorageInventorySlotSnapshot::count).orElse(0),
                validation.expectedDisposition(),
                validation.expectedDispositionReason(),
                validation.observationSource(),
                expected,
                actual,
                fingerprintEqual
        );
    }

    static StoreHomeManifestMismatchSnapshot fromSnapshots(
            int manifestStepIndex,
            int logicalPlayerSlot,
            int expectedRemainingCount,
            String expectedDisposition,
            String expectedDispositionReason,
            String actualObservationSource,
            StoreHomeStackIdentitySnapshot expected,
            StoreHomeStackIdentitySnapshot actual,
            String fullFingerprintEqual) {
        return fromSnapshots(
                manifestStepIndex,
                logicalPlayerSlot,
                "MAIN",
                -1,
                -1,
                expectedRemainingCount,
                expectedDisposition,
                expectedDispositionReason,
                actualObservationSource,
                expected,
                actual,
                fullFingerprintEqual
        );
    }

    private static StoreHomeManifestMismatchSnapshot fromSnapshots(
            int manifestStepIndex,
            int logicalPlayerSlot,
            String slotLocation,
            int expectedSelectedMainSlot,
            int actualSelectedMainSlot,
            int expectedRemainingCount,
            String expectedDisposition,
            String expectedDispositionReason,
            String actualObservationSource,
            StoreHomeStackIdentitySnapshot expected,
            StoreHomeStackIdentitySnapshot actual,
            String fullFingerprintEqual) {
        boolean identitiesObserved = expected.identityObserved() && actual.identityObserved();
        boolean comparable = identitiesObserved && expected.present() && actual.present();
        String metadataEqual = comparable
                && !StoreHomeStackIdentitySnapshot.NOT_AVAILABLE.equals(expected.metadataDigest())
                && !StoreHomeStackIdentitySnapshot.NOT_AVAILABLE.equals(actual.metadataDigest())
                ? Boolean.toString(expected.metadataDigest().equals(actual.metadataDigest()))
                : comparable
                ? StoreHomeStackIdentitySnapshot.NOT_AVAILABLE
                : identitiesObserved ? "false" : StoreHomeStackIdentitySnapshot.NOT_AVAILABLE;
        String status = "complete".equals(expected.captureStatus())
                && "complete".equals(actual.captureStatus())
                ? "complete"
                : "partial";
        String errors = combineErrors(expected.errorClass(), actual.errorClass());
        return new StoreHomeManifestMismatchSnapshot(
                manifestStepIndex,
                logicalPlayerSlot,
                slotLocation,
                expectedSelectedMainSlot,
                actualSelectedMainSlot,
                expectedRemainingCount,
                expectedDisposition,
                expectedDispositionReason,
                actualObservationSource,
                expected,
                actual,
                compareCore(identitiesObserved, expected.present(), actual.present(),
                        expected.itemId().equals(actual.itemId())),
                compareCore(identitiesObserved, expected.present(), actual.present(),
                        expected.count() == actual.count()),
                compareCore(identitiesObserved, expected.present(), actual.present(),
                        expected.damage() == actual.damage()),
                metadataEqual,
                fullFingerprintEqual,
                status,
                errors
        );
    }

    public static StoreHomeManifestMismatchSnapshot unavailableForStep(
            HomeStorageManifestStep step,
            int manifestStepIndex,
            int expectedRemainingCount,
            String errorClass) {
        Objects.requireNonNull(step, "step");
        return fromSnapshots(
                manifestStepIndex,
                step.logicalPlayerInventorySlot(),
                expectedRemainingCount,
                step.disposition().name(),
                step.dispositionReason(),
                "not_available",
                StoreHomeStackIdentitySnapshot.captureExpected(
                        step.fingerprint(), expectedRemainingCount
                ),
                StoreHomeStackIdentitySnapshot.unavailable(errorClass),
                StoreHomeStackIdentitySnapshot.NOT_AVAILABLE
        );
    }

    private static String compareCore(
            boolean identitiesObserved,
            boolean expectedPresent,
            boolean actualPresent,
            boolean valueEqual) {
        if (!identitiesObserved) {
            return StoreHomeStackIdentitySnapshot.NOT_AVAILABLE;
        }
        if (!expectedPresent || !actualPresent) {
            return Boolean.toString(expectedPresent == actualPresent);
        }
        return Boolean.toString(valueEqual);
    }

    public static StoreHomeManifestMismatchSnapshot unavailable(String errorClass) {
        StoreHomeStackIdentitySnapshot unavailable =
                StoreHomeStackIdentitySnapshot.unavailable(errorClass);
        return new StoreHomeManifestMismatchSnapshot(
                -1,
                -1,
                StoreHomeStackIdentitySnapshot.NOT_AVAILABLE,
                -1,
                -1,
                0,
                StoreHomeStackIdentitySnapshot.NOT_AVAILABLE,
                StoreHomeStackIdentitySnapshot.NOT_AVAILABLE,
                StoreHomeStackIdentitySnapshot.NOT_AVAILABLE,
                unavailable,
                unavailable,
                StoreHomeStackIdentitySnapshot.NOT_AVAILABLE,
                StoreHomeStackIdentitySnapshot.NOT_AVAILABLE,
                StoreHomeStackIdentitySnapshot.NOT_AVAILABLE,
                StoreHomeStackIdentitySnapshot.NOT_AVAILABLE,
                StoreHomeStackIdentitySnapshot.NOT_AVAILABLE,
                "partial",
                errorClass == null ? "unknown" : errorClass
        );
    }

    private static String combineErrors(String expected, String actual) {
        if ("none".equals(expected)) {
            return actual;
        }
        if ("none".equals(actual) || expected.equals(actual)) {
            return expected;
        }
        return expected + "," + actual;
    }

    public int manifestStepIndex() {
        return manifestStepIndex;
    }

    public int logicalPlayerSlot() {
        return logicalPlayerSlot;
    }

    public String slotLocation() {
        return slotLocation;
    }

    public Object expectedSelectedMainSlotValue() {
        return expectedSelectedMainSlot >= 0
                ? expectedSelectedMainSlot
                : StoreHomeStackIdentitySnapshot.NOT_AVAILABLE;
    }

    public Object actualSelectedMainSlotValue() {
        return actualSelectedMainSlot >= 0
                ? actualSelectedMainSlot
                : StoreHomeStackIdentitySnapshot.NOT_AVAILABLE;
    }

    public int expectedRemainingCount() {
        return expectedRemainingCount;
    }

    public Object expectedRemainingCountValue() {
        return logicalPlayerSlot >= 0
                ? expectedRemainingCount
                : StoreHomeStackIdentitySnapshot.NOT_AVAILABLE;
    }

    public boolean expectedPresent() {
        return expectedRemainingCount > 0;
    }

    public String expectedDisposition() {
        return expectedDisposition;
    }

    public String expectedDispositionReason() {
        return expectedDispositionReason;
    }

    public String actualObservationSource() {
        return actualObservationSource;
    }

    public StoreHomeStackIdentitySnapshot expected() {
        return expected;
    }

    public StoreHomeStackIdentitySnapshot actual() {
        return actual;
    }

    public String itemIdEqual() {
        return itemIdEqual;
    }

    public String countEqual() {
        return countEqual;
    }

    public String damageEqual() {
        return damageEqual;
    }

    public String metadataDigestEqual() {
        return metadataDigestEqual;
    }

    public String fullFingerprintEqual() {
        return fullFingerprintEqual;
    }

    public String captureStatus() {
        return captureStatus;
    }

    public String errorClass() {
        return errorClass;
    }
}
