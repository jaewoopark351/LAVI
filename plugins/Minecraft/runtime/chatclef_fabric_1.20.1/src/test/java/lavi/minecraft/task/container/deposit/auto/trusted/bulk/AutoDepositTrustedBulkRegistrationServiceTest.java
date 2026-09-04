package lavi.minecraft.task.container.deposit.auto.trusted.bulk;

import adris.altoclef.AltoClef;
import adris.altoclef.ui.MessagePriority;
import adris.altoclef.util.Dimension;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.anchor.AutoDepositBulkAnchorKind;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.anchor.AutoDepositBulkAnchorReadResult;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.anchor.AutoDepositBulkAnchorReader;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.anchor.AutoDepositBulkPlayerAnchor;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan.AutoDepositBulkBlockObservation;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan.AutoDepositBulkBuildHeight;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan.AutoDepositBulkContainerKind;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan.AutoDepositBulkScanner;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan.AutoDepositBulkWorldProvenance;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan.AutoDepositBulkWorldView;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.topology.AutoDepositBulkTopologyNormalizer;
import lavi.minecraft.task.container.deposit.auto.trusted.command.form.AutoDepositTrustCommandForm;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
final class AutoDepositTrustedBulkRegistrationServiceTest {
    @Test
    void preservesTheWorldAndPlayerSnapshotReadOrderThroughPreCommit() {
        BlockPos anchor = new BlockPos(0, 64, 0);
        Object player = new Object();
        List<String> trace = new ArrayList<>();
        FakeWorldView world = new FakeWorldView(Map.of());
        world.trace = trace;
        SequencedAnchorReader reader = new SequencedAnchorReader(
                available(anchor, player, world.worldIdentity),
                available(anchor, player, world.worldIdentity)
        );
        reader.trace = trace;

        AutoDepositTrustedBulkRegistrationResult result = service(
                AutoDepositTrustedDestinationRepository.inMemoryEmpty(),
                reader,
                world
        ).execute(new AltoClef(), AutoDepositTrustCommandForm.ENGLISH_AREA);

        assertTrue(result.success());
        assertEquals(List.of(
                "provenance#1",
                "anchor#1",
                "provenance#2",
                "provenance#3",
                "provenance#4",
                "provenance#5",
                "anchor#2"
        ), trace);
    }

    @Test
    void unsupportedPlayerCenterRegistersNearbyContainersWithoutEmittingACommandLog() {
        BlockPos anchor = new BlockPos(-1, 64, -1);
        Object player = new Object();
        FakeWorldView world = new FakeWorldView(Map.of(
                new BlockPos(3, 66, 4), AutoDepositBulkContainerKind.BARREL,
                new BlockPos(-2, 64, -1), AutoDepositBulkContainerKind.BARREL
        ));
        SequencedAnchorReader reader = new SequencedAnchorReader(
                available(anchor, player, world.worldIdentity),
                available(anchor, player, world.worldIdentity)
        );
        AutoDepositTrustedDestinationRepository repository =
                AutoDepositTrustedDestinationRepository.inMemoryEmpty();
        long revisionBefore = repository.revision();
        RecordingAltoClef mod = new RecordingAltoClef();

        AutoDepositTrustedBulkRegistrationResult result = service(
                repository,
                reader,
                world
        ).execute(mod, AutoDepositTrustCommandForm.ENGLISH_AREA);

        assertTrue(result.success());
        assertEquals("UPDATED", result.status());
        assertEquals(AutoDepositBulkAnchorKind.PLAYER_BLOCK_POSITION, result.anchorSource());
        assertEquals(anchor, result.anchorPosition().orElseThrow());
        assertEquals(4096, result.scannedPositionCount());
        assertEquals(2, result.physicalSupportedBlockCount());
        assertEquals(2, result.logicalDestinationCount());
        assertEquals(2, result.newlyRegisteredCount());
        assertEquals(revisionBefore + 1, repository.revision());
        assertEquals(2, repository.destinations().size());
        assertTrue(result.downstreamAutomaticReevaluationPossible());
        assertEquals(0, mod.logCount);

        String output = AutoDepositTrustedBulkRegistrationFormatter.format(result);
        assertTrue(output.contains("anchorSource=PLAYER_BLOCK_POSITION"));
        assertTrue(output.contains("anchorPos=-1,64,-1"));
        assertFalse(output.contains("CROSSHAIR"));
        assertTrue(output.length() < 2048);
    }

    @Test
    void completeEmptyVolumeReturnsDistinctNoChangeWithoutMutation() {
        BlockPos anchor = new BlockPos(0, 64, 0);
        Object player = new Object();
        FakeWorldView world = new FakeWorldView(Map.of());
        AutoDepositTrustedDestinationRepository repository =
                AutoDepositTrustedDestinationRepository.inMemoryEmpty();
        long revisionBefore = repository.revision();

        AutoDepositTrustedBulkRegistrationResult result = service(
                repository,
                new SequencedAnchorReader(
                        available(anchor, player, world.worldIdentity),
                        available(anchor, player, world.worldIdentity)
                ),
                world
        ).execute(new AltoClef(), AutoDepositTrustCommandForm.KOREAN_AREA);

        assertTrue(result.success());
        assertEquals("NO_CHANGE", result.status());
        assertEquals("no_supported_destinations_found", result.reason());
        assertEquals(4096, result.scannedPositionCount());
        assertEquals(0, result.physicalSupportedBlockCount());
        assertEquals(0, result.logicalDestinationCount());
        assertEquals(0, result.newlyRegisteredCount());
        assertEquals(revisionBefore, result.repositoryRevisionAfter());
        assertEquals(revisionBefore, repository.revision());
        assertFalse(result.downstreamAutomaticReevaluationPossible());
    }

    @Test
    void unavailableInitialProvenanceDistinguishesLivePlayerFailure() {
        BlockPos anchor = new BlockPos(0, 64, 0);
        Object player = new Object();
        FakeWorldView worldWithPlayer = new FakeWorldView(Map.of());
        worldWithPlayer.unavailableProvenanceCall = 1;
        FakeWorldView worldWithoutPlayer = new FakeWorldView(Map.of());
        worldWithoutPlayer.unavailableProvenanceCall = 1;
        SequencedAnchorReader availableReader = new SequencedAnchorReader(
                available(anchor, player, worldWithPlayer.worldIdentity)
        );
        SequencedAnchorReader unavailableReader = new SequencedAnchorReader(
                AutoDepositBulkAnchorReadResult.unavailable()
        );

        AutoDepositTrustedBulkRegistrationResult provenanceUnavailable = service(
                AutoDepositTrustedDestinationRepository.inMemoryEmpty(),
                availableReader,
                worldWithPlayer
        ).execute(new AltoClef(), AutoDepositTrustCommandForm.ENGLISH_RADIUS);
        AutoDepositTrustedBulkRegistrationResult playerUnavailable = service(
                AutoDepositTrustedDestinationRepository.inMemoryEmpty(),
                unavailableReader,
                worldWithoutPlayer
        ).execute(new AltoClef(), AutoDepositTrustCommandForm.ENGLISH_RADIUS);

        assertEquals(
                "world_provenance_unavailable_during_anchor_acquisition",
                provenanceUnavailable.reason()
        );
        assertEquals(anchor, provenanceUnavailable.anchorPosition().orElseThrow());
        assertEquals("player_position_anchor_unavailable", playerUnavailable.reason());
        assertTrue(playerUnavailable.anchorPosition().isEmpty());
        assertEquals(AutoDepositBulkAnchorKind.PLAYER_BLOCK_POSITION,
                playerUnavailable.anchorSource());
        assertEquals(1, availableReader.readCalls);
        assertEquals(1, unavailableReader.readCalls);
        assertEquals(1, worldWithPlayer.provenanceCalls);
        assertEquals(1, worldWithoutPlayer.provenanceCalls);
        assertEquals(0, worldWithPlayer.observationCalls);
        assertEquals(0, worldWithoutPlayer.observationCalls);
    }

    @Test
    void initialAcquisitionSeparatesProvenanceReadFailureAndWorldChange() {
        BlockPos anchor = new BlockPos(0, 64, 0);
        Object player = new Object();
        FakeWorldView thrownWorld = new FakeWorldView(Map.of());
        thrownWorld.throwProvenanceCall = 1;
        FakeWorldView changedWorld = new FakeWorldView(Map.of());
        changedWorld.replacementProvenanceCall = 2;

        AutoDepositTrustedBulkRegistrationResult thrown = service(
                AutoDepositTrustedDestinationRepository.inMemoryEmpty(),
                new SequencedAnchorReader(),
                thrownWorld
        ).execute(new AltoClef(), AutoDepositTrustCommandForm.KOREAN_RADIUS);
        AutoDepositTrustedBulkRegistrationResult changed = service(
                AutoDepositTrustedDestinationRepository.inMemoryEmpty(),
                new SequencedAnchorReader(
                        available(anchor, player, changedWorld.worldIdentity)
                ),
                changedWorld
        ).execute(new AltoClef(), AutoDepositTrustCommandForm.KOREAN_RADIUS);

        assertEquals(
                "world_provenance_read_failed_during_anchor_acquisition",
                thrown.reason()
        );
        assertEquals("exception=IllegalStateException", thrown.boundedFirstConflict());
        assertEquals(
                "world_provenance_changed_during_anchor_acquisition",
                changed.reason()
        );
        assertEquals(0, thrownWorld.observationCalls);
        assertEquals(0, changedWorld.observationCalls);
    }

    @Test
    void scannerStartWorldMismatchStopsBeforeBuildHeightAndBlocks() {
        BlockPos anchor = new BlockPos(0, 64, 0);
        Object player = new Object();
        FakeWorldView world = new FakeWorldView(Map.of());
        world.replacementProvenanceCall = 3;

        AutoDepositTrustedBulkRegistrationResult result = service(
                AutoDepositTrustedDestinationRepository.inMemoryEmpty(),
                new SequencedAnchorReader(
                        available(anchor, player, world.worldIdentity)
                ),
                world
        ).execute(new AltoClef(), AutoDepositTrustCommandForm.ENGLISH_AREA);

        assertEquals("world_provenance_changed_before_scan", result.reason());
        assertFalse(result.coverageComplete());
        assertEquals(0, world.buildHeightCalls);
        assertEquals(0, world.chunkCalls);
        assertEquals(0, world.observationCalls);
    }

    @Test
    void preCommitWorldFailurePrecedesAPlayerFailureAndPreservesScanEvidence() {
        BlockPos anchor = new BlockPos(0, 64, 0);
        Object player = new Object();
        FakeWorldView world = new FakeWorldView(Map.of(
                new BlockPos(1, 64, 0), AutoDepositBulkContainerKind.BARREL
        ));
        world.replacementProvenanceCall = 5;
        SequencedAnchorReader reader = new SequencedAnchorReader(
                available(anchor, player, world.worldIdentity),
                new IllegalStateException("must not be read")
        );
        AutoDepositTrustedDestinationRepository repository =
                AutoDepositTrustedDestinationRepository.inMemoryEmpty();
        long revisionBefore = repository.revision();

        AutoDepositTrustedBulkRegistrationResult result = service(
                repository,
                reader,
                world
        ).execute(new AltoClef(), AutoDepositTrustCommandForm.ENGLISH_RADIUS);

        assertEquals("world_provenance_changed_before_commit", result.reason());
        assertTrue(result.coverageComplete());
        assertEquals(4096, result.scannedPositionCount());
        assertEquals(1, reader.readCalls);
        assertEquals(0, repository.destinations().size());
        assertEquals(revisionBefore, repository.revision());
    }

    @Test
    void preCommitWorldReadExceptionPreservesCompleteCoverageWithoutMutation() {
        BlockPos anchor = new BlockPos(0, 64, 0);
        Object player = new Object();
        FakeWorldView world = new FakeWorldView(Map.of());
        world.throwProvenanceCall = 5;
        AutoDepositTrustedDestinationRepository repository =
                AutoDepositTrustedDestinationRepository.inMemoryEmpty();
        long revisionBefore = repository.revision();

        AutoDepositTrustedBulkRegistrationResult result = service(
                repository,
                new SequencedAnchorReader(
                        available(anchor, player, world.worldIdentity)
                ),
                world
        ).execute(new AltoClef(), AutoDepositTrustCommandForm.ENGLISH_RADIUS);

        assertEquals("world_provenance_read_failed_before_commit", result.reason());
        assertTrue(result.coverageComplete());
        assertEquals("exception=IllegalStateException", result.boundedFirstConflict());
        assertEquals(revisionBefore, repository.revision());
    }

    @Test
    void preCommitPlayerFailuresHaveDistinctTerminalReasons() {
        assertPlayerFailure(
                AutoDepositBulkAnchorReadResult.unavailable(),
                "player_position_anchor_unavailable_before_commit"
        );
        assertPlayerFailure(
                new IllegalStateException("expected reader failure"),
                "player_position_anchor_read_failed_before_commit"
        );
        assertPlayerFailure(
                AutoDepositBulkAnchorReadResult.playerWorldMembershipMismatch(),
                "player_world_membership_changed_during_scan"
        );
    }

    @Test
    void preCommitPlayerIdentityAndBlockPositionChangesAreSeparated() {
        BlockPos anchor = new BlockPos(0, 64, 0);
        assertPlayerFailure(
                available(anchor, new Object(), null),
                "player_identity_changed_during_scan"
        );
        assertPlayerFailure(
                new ChangedPositionOutcome(anchor.east()),
                "player_position_anchor_changed_during_scan"
        );
    }

    private static void assertPlayerFailure(Object currentOutcome, String expectedReason) {
        BlockPos initialPosition = new BlockPos(0, 64, 0);
        Object initialPlayer = new Object();
        FakeWorldView world = new FakeWorldView(Map.of(
                new BlockPos(1, 64, 0), AutoDepositBulkContainerKind.BARREL
        ));
        Object normalizedCurrent = currentOutcome;
        if (currentOutcome instanceof AutoDepositBulkAnchorReadResult readResult
                && readResult.anchor().isPresent()
                && !readResult.anchor().orElseThrow().belongsToWorld(world.baseProvenance())) {
            normalizedCurrent = available(
                    readResult.anchor().orElseThrow().position(),
                    new Object(),
                    world.worldIdentity
            );
        } else if (currentOutcome instanceof ChangedPositionOutcome changed) {
            normalizedCurrent = available(
                    changed.position,
                    initialPlayer,
                    world.worldIdentity
            );
        }
        SequencedAnchorReader reader = new SequencedAnchorReader(
                available(initialPosition, initialPlayer, world.worldIdentity),
                normalizedCurrent
        );
        AutoDepositTrustedDestinationRepository repository =
                AutoDepositTrustedDestinationRepository.inMemoryEmpty();
        long revisionBefore = repository.revision();

        AutoDepositTrustedBulkRegistrationResult result = service(
                repository,
                reader,
                world
        ).execute(new AltoClef(), AutoDepositTrustCommandForm.KOREAN_RADIUS);

        assertFalse(result.success());
        assertEquals("INVALID_ANCHOR", result.status());
        assertEquals(expectedReason, result.reason());
        assertTrue(result.coverageComplete());
        assertEquals(4096, result.scannedPositionCount());
        assertEquals(0, repository.destinations().size());
        assertEquals(revisionBefore, repository.revision());
    }

    private static AutoDepositBulkAnchorReadResult available(
            BlockPos position,
            Object playerIdentity,
            Object worldIdentity) {
        Object checkedWorld = worldIdentity == null ? new Object() : worldIdentity;
        return AutoDepositBulkAnchorReadResult.available(
                new AutoDepositBulkPlayerAnchor(position, playerIdentity, checkedWorld)
        );
    }

    private static AutoDepositTrustedBulkRegistrationService service(
            AutoDepositTrustedDestinationRepository repository,
            AutoDepositBulkAnchorReader anchorReader,
            AutoDepositBulkWorldView world) {
        return new AutoDepositTrustedBulkRegistrationService(
                repository,
                anchorReader,
                ignored -> world,
                new AutoDepositBulkScanner(),
                new AutoDepositBulkTopologyNormalizer()
        );
    }

    private record ChangedPositionOutcome(BlockPos position) {
    }

    private static final class SequencedAnchorReader implements AutoDepositBulkAnchorReader {
        private final Queue<Object> outcomes = new ArrayDeque<>();
        private int readCalls;
        private List<String> trace;

        private SequencedAnchorReader(Object... outcomes) {
            for (Object outcome : outcomes) {
                this.outcomes.add(outcome);
            }
        }

        @Override
        public AutoDepositBulkAnchorReadResult read(AltoClef mod) {
            readCalls++;
            if (trace != null) {
                trace.add("anchor#" + readCalls);
            }
            Object outcome = outcomes.remove();
            if (outcome instanceof RuntimeException exception) {
                throw exception;
            }
            return (AutoDepositBulkAnchorReadResult) outcome;
        }
    }

    private static final class FakeWorldView implements AutoDepositBulkWorldView {
        private final Object worldIdentity = new Object();
        private final Object replacementWorldIdentity = new Object();
        private final Map<BlockPos, AutoDepositBulkContainerKind> containers = new HashMap<>();
        private int provenanceCalls;
        private int buildHeightCalls;
        private int chunkCalls;
        private int observationCalls;
        private int unavailableProvenanceCall = -1;
        private int throwProvenanceCall = -1;
        private int replacementProvenanceCall = -1;
        private List<String> trace;

        private FakeWorldView(Map<BlockPos, AutoDepositBulkContainerKind> containers) {
            containers.forEach((position, kind) ->
                    this.containers.put(position.toImmutable(), kind));
        }

        @Override
        public Optional<AutoDepositBulkWorldProvenance> provenance() {
            provenanceCalls++;
            if (trace != null) {
                trace.add("provenance#" + provenanceCalls);
            }
            if (provenanceCalls == throwProvenanceCall) {
                throw new IllegalStateException("expected provenance failure");
            }
            if (provenanceCalls == unavailableProvenanceCall) {
                return Optional.empty();
            }
            Object identity = provenanceCalls == replacementProvenanceCall
                    ? replacementWorldIdentity
                    : worldIdentity;
            return Optional.of(provenance(identity));
        }

        @Override
        public Optional<AutoDepositBulkBuildHeight> buildHeight() {
            buildHeightCalls++;
            return Optional.of(new AutoDepositBulkBuildHeight(-64, 320));
        }

        @Override
        public boolean isChunkLoaded(int chunkX, int chunkZ) {
            chunkCalls++;
            return true;
        }

        @Override
        public Optional<AutoDepositBulkBlockObservation> observeLoaded(BlockPos position) {
            observationCalls++;
            AutoDepositBulkContainerKind kind = containers.getOrDefault(
                    position,
                    AutoDepositBulkContainerKind.OTHER
            );
            return Optional.of(AutoDepositBulkBlockObservation.nonChest(
                    position,
                    kind,
                    false
            ));
        }

        private AutoDepositBulkWorldProvenance baseProvenance() {
            return provenance(worldIdentity);
        }

        private static AutoDepositBulkWorldProvenance provenance(Object identity) {
            return new AutoDepositBulkWorldProvenance(
                    "singleplayer:h5-service-test",
                    Dimension.OVERWORLD,
                    "minecraft:overworld",
                    identity
            );
        }
    }

    private static final class RecordingAltoClef extends AltoClef {
        private int logCount;

        @Override
        public void log(String message) {
            logCount++;
        }

        @Override
        public void log(String message, MessagePriority priority) {
            logCount++;
        }

        @Override
        public void logWarning(String message) {
            logCount++;
        }

        @Override
        public void logWarning(String message, MessagePriority priority) {
            logCount++;
        }
    }
}
