package lavi.minecraft.task.container.deposit.auto.trusted.bulk;

import adris.altoclef.AltoClef;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.anchor.AutoDepositBulkAnchorKind;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.anchor.AutoDepositBulkAnchorReadResult;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.anchor.AutoDepositBulkAnchorReadStatus;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.anchor.AutoDepositBulkAnchorReader;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.anchor.AutoDepositBulkPlayerAnchor;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.anchor.MinecraftAutoDepositPlayerPositionAnchorReader;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.repository.AutoDepositTrustedBulkMutationResult;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan.AutoDepositBulkScanResult;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan.AutoDepositBulkScanner;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan.AutoDepositBulkWorldProvenance;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan.AutoDepositBulkWorldView;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan.MinecraftAutoDepositBulkWorldView;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.topology.AutoDepositBulkTopologyNormalizer;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.topology.AutoDepositBulkTopologyResult;
import lavi.minecraft.task.container.deposit.auto.trusted.command.form.AutoDepositTrustCommandForm;
import net.minecraft.util.math.BlockPos;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
public final class AutoDepositTrustedBulkRegistrationService {
    private final AutoDepositTrustedDestinationRepository repository;
    private final AutoDepositBulkAnchorKind anchorKind;
    private final AutoDepositBulkAnchorReader anchorReader;
    private final Function<AltoClef, AutoDepositBulkWorldView> worldViewFactory;
    private final AutoDepositBulkScanner scanner;
    private final AutoDepositBulkTopologyNormalizer topologyNormalizer;

    public AutoDepositTrustedBulkRegistrationService(
            AutoDepositTrustedDestinationRepository repository) {
        this(
                repository,
                new MinecraftAutoDepositPlayerPositionAnchorReader(),
                MinecraftAutoDepositBulkWorldView::new,
                new AutoDepositBulkScanner(),
                new AutoDepositBulkTopologyNormalizer()
        );
    }

    AutoDepositTrustedBulkRegistrationService(
            AutoDepositTrustedDestinationRepository repository,
            AutoDepositBulkAnchorReader anchorReader,
            Function<AltoClef, AutoDepositBulkWorldView> worldViewFactory,
            AutoDepositBulkScanner scanner,
            AutoDepositBulkTopologyNormalizer topologyNormalizer) {
        this.repository = Objects.requireNonNull(repository, "repository");
        anchorKind = AutoDepositBulkAnchorKind.PLAYER_BLOCK_POSITION;
        this.anchorReader = Objects.requireNonNull(anchorReader, "anchorReader");
        this.worldViewFactory = Objects.requireNonNull(worldViewFactory, "worldViewFactory");
        this.scanner = Objects.requireNonNull(scanner, "scanner");
        this.topologyNormalizer = Objects.requireNonNull(
                topologyNormalizer,
                "topologyNormalizer"
        );
    }

    public AutoDepositTrustedBulkRegistrationResult execute(
            AltoClef mod,
            AutoDepositTrustCommandForm commandForm) {
        AutoDepositTrustCommandForm form = Objects.requireNonNull(commandForm, "commandForm");
        if (!form.bulk()) {
            return beforeScanFailure(
                    form, null, "INVALID_REQUEST",
                    "single_form_rejected_by_bulk_service", "none"
            );
        }

        AutoDepositBulkWorldView worldView;
        try {
            worldView = Objects.requireNonNull(worldViewFactory.apply(mod), "worldView");
        } catch (RuntimeException exception) {
            return beforeScanFailure(
                    form, null, "SCAN_COVERAGE_INCOMPLETE",
                    "world_view_unavailable", exceptionConflict(exception)
            );
        }

        Optional<AutoDepositBulkWorldProvenance> firstProvenance;
        try {
            firstProvenance = readProvenance(worldView);
        } catch (RuntimeException exception) {
            return beforeScanFailure(
                    form, null, "SCAN_COVERAGE_INCOMPLETE",
                    "world_provenance_read_failed_during_anchor_acquisition",
                    exceptionConflict(exception)
            );
        }
        if (firstProvenance.isEmpty()) {
            return unavailableFirstProvenanceResult(mod, form);
        }

        AutoDepositBulkAnchorReadResult initialAnchorRead;
        try {
            initialAnchorRead = readAnchor(mod);
        } catch (RuntimeException exception) {
            return beforeScanFailure(
                    form, null, "INVALID_ANCHOR",
                    "player_position_anchor_read_failed", exceptionConflict(exception)
            );
        }
        if (initialAnchorRead.status() != AutoDepositBulkAnchorReadStatus.AVAILABLE) {
            return beforeScanFailure(
                    form, null, "INVALID_ANCHOR",
                    "player_position_anchor_unavailable", "none"
            );
        }
        AutoDepositBulkPlayerAnchor initialPlayerAnchor = initialAnchorRead.anchor()
                .orElseThrow();

        Optional<AutoDepositBulkWorldProvenance> secondProvenance;
        try {
            secondProvenance = readProvenance(worldView);
        } catch (RuntimeException exception) {
            return beforeScanFailure(
                    form, initialPlayerAnchor.position(), "SCAN_COVERAGE_INCOMPLETE",
                    "world_provenance_read_failed_during_anchor_acquisition",
                    exceptionConflict(exception)
            );
        }
        if (secondProvenance.isEmpty()) {
            return beforeScanFailure(
                    form, initialPlayerAnchor.position(), "SCAN_COVERAGE_INCOMPLETE",
                    "world_provenance_unavailable_during_anchor_acquisition",
                    "provenance=unavailable"
            );
        }
        if (!firstProvenance.get().sameWorld(secondProvenance.get())) {
            return beforeScanFailure(
                    form, initialPlayerAnchor.position(), "SCAN_COVERAGE_INCOMPLETE",
                    "world_provenance_changed_during_anchor_acquisition",
                    "provenance=changed"
            );
        }

        AutoDepositBulkRegistrationSnapshot snapshot;
        try {
            snapshot = new AutoDepositBulkRegistrationSnapshot(
                    anchorKind,
                    initialPlayerAnchor,
                    secondProvenance.get()
            );
        } catch (IllegalArgumentException exception) {
            return beforeScanFailure(
                    form, initialPlayerAnchor.position(), "SCAN_COVERAGE_INCOMPLETE",
                    "world_provenance_changed_during_anchor_acquisition",
                    "player_world_provenance_mismatch"
            );
        }

        AutoDepositBulkScanResult scan;
        try {
            scan = scanner.scan(
                    worldView,
                    snapshot.playerAnchor().position(),
                    snapshot.worldProvenance()
            );
        } catch (RuntimeException exception) {
            return beforeScanFailure(
                    form, snapshot.playerAnchor().position(), "SCAN_COVERAGE_INCOMPLETE",
                    "scan_failed", exceptionConflict(exception)
            );
        }
        if (!scan.success()) {
            return AutoDepositTrustedBulkRegistrationResult.scanFailure(
                    anchorKind, form, snapshot.playerAnchor().position(), scan
            );
        }

        AutoDepositBulkTopologyResult topology;
        try {
            topology = topologyNormalizer.normalize(scan);
        } catch (RuntimeException exception) {
            return AutoDepositTrustedBulkRegistrationResult.postScanFailure(
                    anchorKind, form, scan, null, "AMBIGUOUS_DOUBLE_CHEST",
                    "topology_normalization_failed", exceptionConflict(exception)
            );
        }
        if (!topology.success()) {
            return AutoDepositTrustedBulkRegistrationResult.topologyFailure(
                    anchorKind, form, scan, topology
            );
        }

        Optional<AutoDepositBulkWorldProvenance> commitProvenance;
        try {
            commitProvenance = readProvenance(worldView);
        } catch (RuntimeException exception) {
            return postScanFailure(
                    form, scan, topology, "SCAN_COVERAGE_INCOMPLETE",
                    "world_provenance_read_failed_before_commit",
                    exceptionConflict(exception)
            );
        }
        if (commitProvenance.isEmpty() || !snapshot.sameWorld(commitProvenance.get())) {
            return postScanFailure(
                    form, scan, topology, "SCAN_COVERAGE_INCOMPLETE",
                    "world_provenance_changed_before_commit", "world_provenance_changed"
            );
        }

        AutoDepositBulkAnchorReadResult currentAnchorRead;
        try {
            currentAnchorRead = readAnchor(mod);
        } catch (RuntimeException exception) {
            return postScanFailure(
                    form, scan, topology, "INVALID_ANCHOR",
                    "player_position_anchor_read_failed_before_commit",
                    exceptionConflict(exception)
            );
        }
        if (currentAnchorRead.status() == AutoDepositBulkAnchorReadStatus.UNAVAILABLE) {
            return postScanFailure(
                    form, scan, topology, "INVALID_ANCHOR",
                    "player_position_anchor_unavailable_before_commit", "none"
            );
        }
        if (currentAnchorRead.status()
                == AutoDepositBulkAnchorReadStatus.PLAYER_WORLD_MEMBERSHIP_MISMATCH) {
            return postScanFailure(
                    form, scan, topology, "INVALID_ANCHOR",
                    "player_world_membership_changed_during_scan", "none"
            );
        }
        AutoDepositBulkPlayerAnchor currentPlayerAnchor = currentAnchorRead.anchor()
                .orElseThrow();
        if (!snapshot.playerAnchor().samePlayerIdentity(currentPlayerAnchor)) {
            return postScanFailure(
                    form, scan, topology, "INVALID_ANCHOR",
                    "player_identity_changed_during_scan", "none"
            );
        }
        if (!currentPlayerAnchor.belongsToWorld(commitProvenance.get())) {
            return postScanFailure(
                    form, scan, topology, "INVALID_ANCHOR",
                    "player_world_membership_changed_during_scan", "none"
            );
        }
        if (!snapshot.playerAnchor().position().equals(currentPlayerAnchor.position())) {
            return postScanFailure(
                    form, scan, topology, "INVALID_ANCHOR",
                    "player_position_anchor_changed_during_scan", "none"
            );
        }

        AutoDepositTrustedBulkMutationResult mutation = repository.registerBulk(
                snapshot.worldProvenance().worldKey(),
                snapshot.worldProvenance().dimension(),
                topology.logicalDestinations()
        );
        return AutoDepositTrustedBulkRegistrationResult.mutation(
                anchorKind, form, scan, topology, mutation
        );
    }

    private AutoDepositTrustedBulkRegistrationResult unavailableFirstProvenanceResult(
            AltoClef mod,
            AutoDepositTrustCommandForm form) {
        AutoDepositBulkAnchorReadResult anchorRead;
        try {
            anchorRead = readAnchor(mod);
        } catch (RuntimeException exception) {
            return beforeScanFailure(
                    form, null, "INVALID_ANCHOR",
                    "player_position_anchor_read_failed", exceptionConflict(exception)
            );
        }
        if (anchorRead.status() != AutoDepositBulkAnchorReadStatus.AVAILABLE) {
            return beforeScanFailure(
                    form, null, "INVALID_ANCHOR",
                    "player_position_anchor_unavailable", "none"
            );
        }
        BlockPos anchor = anchorRead.anchor().orElseThrow().position();
        return beforeScanFailure(
                form, anchor, "SCAN_COVERAGE_INCOMPLETE",
                "world_provenance_unavailable_during_anchor_acquisition",
                "provenance=unavailable"
        );
    }

    private Optional<AutoDepositBulkWorldProvenance> readProvenance(
            AutoDepositBulkWorldView worldView) {
        return Objects.requireNonNull(worldView.provenance(), "worldView.provenance()");
    }

    private AutoDepositBulkAnchorReadResult readAnchor(AltoClef mod) {
        return Objects.requireNonNull(anchorReader.read(mod), "anchorReader.read(mod)");
    }

    private AutoDepositTrustedBulkRegistrationResult beforeScanFailure(
            AutoDepositTrustCommandForm form,
            BlockPos anchor,
            String status,
            String reason,
            String conflict) {
        return AutoDepositTrustedBulkRegistrationResult.beforeScanFailure(
                anchorKind, form, anchor, status, reason, conflict
        );
    }

    private AutoDepositTrustedBulkRegistrationResult postScanFailure(
            AutoDepositTrustCommandForm form,
            AutoDepositBulkScanResult scan,
            AutoDepositBulkTopologyResult topology,
            String status,
            String reason,
            String conflict) {
        return AutoDepositTrustedBulkRegistrationResult.postScanFailure(
                anchorKind, form, scan, topology, status, reason, conflict
        );
    }

    private static String exceptionConflict(RuntimeException exception) {
        return "exception=" + exception.getClass().getSimpleName();
    }
}
