package lavi.minecraft.task.container.deposit.auto.trusted;

import adris.altoclef.util.Dimension;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.repository.AutoDepositTrustedBulkMutationResult;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.repository.AutoDepositTrustedBulkMutationStatus;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan.AutoDepositBulkContainerKind;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.topology.AutoDepositBulkLogicalDestination;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.topology.AutoDepositDoubleChestPairKey;
import lavi.minecraft.task.container.deposit.auto.trusted.persistence.AutoDepositTrustedConditionalSaveStatus;
import lavi.minecraft.task.container.deposit.auto.trusted.persistence.read.AutoDepositTrustedRegistryProvenance;
import lavi.minecraft.task.container.deposit.auto.trusted.persistence.read.AutoDepositTrustedRegistryReadResult;
import lavi.minecraft.task.container.deposit.auto.trusted.persistence.read.AutoDepositTrustedRegistryReadStatus;
import lavi.minecraft.task.container.deposit.auto.trusted.persistence.read.AutoDepositTrustedRegistrySnapshot;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoDepositTrustedDestinationRepositoryTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void registrationIsInstanceOwnedAndAdvancesTheRepositoryRevision() {
        AutoDepositTrustedDestinationRepository repository =
                AutoDepositTrustedDestinationRepository.inMemoryEmpty();
        long before = repository.revision();

        repository.register(new AutoDepositTrustedDestination(
                "singleplayer:test",
                Dimension.OVERWORLD,
                new BlockPos(10, 64, 10),
                true
        ));

        assertEquals(1, repository.destinations().size());
        assertTrue(repository.revision() > before);
    }

    @Test
    void repeatedRegistrationIsIdempotentAndStableIdRemovalWorks() {
        AutoDepositTrustedDestinationRepository repository =
                AutoDepositTrustedDestinationRepository.inMemoryEmpty();
        AutoDepositTrustedDestination destination = new AutoDepositTrustedDestination(
                "singleplayer:test",
                Dimension.OVERWORLD,
                new BlockPos(10, 64, 10),
                true
        );

        AutoDepositTrustedDestinationMutationResult first = repository.register(destination);
        AutoDepositTrustedDestinationMutationResult second = repository.register(
                new AutoDepositTrustedDestination(
                        "singleplayer:test",
                        Dimension.OVERWORLD,
                        new BlockPos(10, 64, 10),
                        true
                )
        );
        AutoDepositTrustedDestinationMutationResult removed =
                repository.unregisterById(destination.destinationId());

        assertEquals(AutoDepositTrustedDestinationMutationResult.Status.REGISTERED, first.status());
        assertEquals(AutoDepositTrustedDestinationMutationResult.Status.ALREADY_REGISTERED, second.status());
        assertEquals(AutoDepositTrustedDestinationMutationResult.Status.REMOVED, removed.status());
        assertEquals(0, repository.destinations().size());
        assertFalse(repository.containsEnabled(destination));
    }

    @Test
    void destinationIdIsDeterministicAcrossEquivalentObjects() {
        AutoDepositTrustedDestination first = new AutoDepositTrustedDestination(
                "multiplayer:example.test",
                Dimension.NETHER,
                new BlockPos(-4, 70, 18),
                true
        );
        AutoDepositTrustedDestination second = new AutoDepositTrustedDestination(
                "multiplayer:example.test",
                Dimension.NETHER,
                new BlockPos(-4, 70, 18),
                false
        );

        assertEquals(first.destinationId(), second.destinationId());
        assertTrue(first.destinationId().startsWith("td_"));
    }

    @Test
    void persistenceFailureDoesNotMutateTheRegistryOrReportSuccess() {
        AutoDepositTrustedDestinationRepository repository =
                new AutoDepositTrustedDestinationRepository(new FailingPersistence());
        long before = repository.revision();

        AutoDepositTrustedDestinationMutationResult result = repository.register(
                new AutoDepositTrustedDestination(
                        "singleplayer:test",
                        Dimension.OVERWORLD,
                        new BlockPos(10, 64, 10),
                        true
                )
        );

        assertEquals(AutoDepositTrustedDestinationMutationResult.Status.PERSISTENCE_FAILED,
                result.status());
        assertFalse(result.success());
        assertEquals(0, repository.destinations().size());
        assertEquals(before, repository.revision());
    }

    @Test
    void bulkRegistrationSavesPublishesAndAdvancesRevisionExactlyOnce() {
        AutoDepositTrustedDestination unrelated = destination(new BlockPos(99, 64, 99), true);
        AutoDepositTrustedDestination disabled = destination(new BlockPos(5, 64, 5), false);
        TransactionalPersistence persistence = new TransactionalPersistence(List.of(unrelated, disabled));
        AutoDepositTrustedDestinationRepository repository =
                new AutoDepositTrustedDestinationRepository(persistence);
        long revisionBefore = repository.revision();

        AutoDepositTrustedBulkMutationResult result = repository.registerBulk(
                "singleplayer:test",
                Dimension.OVERWORLD,
                List.of(
                        single(new BlockPos(8, 64, 8)),
                        single(disabled.position()),
                        single(new BlockPos(-1, 64, 4))
                )
        );

        assertEquals(AutoDepositTrustedBulkMutationStatus.UPDATED, result.status());
        assertEquals(1, persistence.strictReadCount);
        assertEquals(1, persistence.conditionalSaveCount);
        assertEquals(1, persistence.persistedWriteCount);
        assertEquals(revisionBefore + 1L, repository.revision());
        assertEquals(2, result.newlyRegisteredCount());
        assertEquals(1, result.reenabledCount());
        assertEquals(List.of(
                unrelated.position(),
                disabled.position(),
                new BlockPos(-1, 64, 4),
                new BlockPos(8, 64, 8)
        ), repository.destinations().stream()
                .map(AutoDepositTrustedDestination::position)
                .toList());
    }

    @Test
    void bulkRegistrationTreatsAMissingRegistryAsValidEmptyAndCreatesItOnce() {
        Path registryPath = temporaryDirectory.resolve("missing-trusted-destinations.json");
        AutoDepositTrustedDestinationRepository repository =
                new AutoDepositTrustedDestinationRepository(
                        new AutoDepositTrustedDestinationStore(registryPath)
                );
        assertFalse(Files.exists(registryPath));
        long revisionBefore = repository.revision();

        AutoDepositTrustedBulkMutationResult result = repository.registerBulk(
                "singleplayer:test",
                Dimension.OVERWORLD,
                List.of(single(new BlockPos(2, 64, 2)))
        );

        assertEquals(AutoDepositTrustedRegistryReadStatus.FILE_MISSING_VALID_EMPTY,
                result.registryReadStatus());
        assertEquals(AutoDepositTrustedBulkMutationStatus.UPDATED, result.status());
        assertTrue(Files.exists(registryPath));
        assertEquals(revisionBefore + 1, repository.revision());
        assertEquals(1, repository.destinations().size());
    }

    @Test
    void bulkNoChangeDoesNotSavePublishOrAdvanceRevision() {
        AutoDepositTrustedDestination existing = destination(new BlockPos(5, 64, 5), true);
        TransactionalPersistence persistence = new TransactionalPersistence(List.of(existing));
        AutoDepositTrustedDestinationRepository repository =
                new AutoDepositTrustedDestinationRepository(persistence);
        List<AutoDepositTrustedDestination> before = repository.destinations();
        long revisionBefore = repository.revision();

        AutoDepositTrustedBulkMutationResult result = repository.registerBulk(
                "singleplayer:test",
                Dimension.OVERWORLD,
                List.of(single(existing.position()))
        );

        assertEquals(AutoDepositTrustedBulkMutationStatus.NO_CHANGE, result.status());
        assertEquals(1, result.alreadyRegisteredCount());
        assertEquals(0, persistence.conditionalSaveCount);
        assertEquals(0, persistence.persistedWriteCount);
        assertEquals(revisionBefore, repository.revision());
        assertTrue(before == repository.destinations());
    }

    @Test
    void bulkEmptyLogicalSetReturnsNoSupportedDestinationsWithoutMutation() {
        AutoDepositTrustedDestination existing = destination(new BlockPos(5, 64, 5), true);
        TransactionalPersistence persistence = new TransactionalPersistence(List.of(existing));
        AutoDepositTrustedDestinationRepository repository =
                new AutoDepositTrustedDestinationRepository(persistence);
        List<AutoDepositTrustedDestination> before = repository.destinations();
        long revisionBefore = repository.revision();

        AutoDepositTrustedBulkMutationResult result = repository.registerBulk(
                "singleplayer:test",
                Dimension.OVERWORLD,
                List.of()
        );

        assertEquals(AutoDepositTrustedBulkMutationStatus.NO_CHANGE, result.status());
        assertTrue(result.success());
        assertFalse(result.effectiveMutation());
        assertEquals("no_supported_destinations_found", result.reason());
        assertEquals(AutoDepositTrustedRegistryReadStatus.VALID_POPULATED,
                result.registryReadStatus());
        assertEquals(0, result.newlyRegisteredCount());
        assertEquals(0, result.reenabledCount());
        assertEquals(0, result.alreadyRegisteredCount());
        assertEquals(revisionBefore, result.repositoryRevisionBefore());
        assertEquals(revisionBefore, result.repositoryRevisionAfter());
        assertEquals(1, result.totalRegistryCountBefore());
        assertEquals(1, result.totalRegistryCountAfter());
        assertEquals(1, persistence.strictReadCount);
        assertEquals(0, persistence.conditionalSaveCount);
        assertEquals(0, persistence.persistedWriteCount);
        assertEquals(revisionBefore, repository.revision());
        assertTrue(before == repository.destinations());
    }

    @Test
    void bulkEmptyLogicalSetPreservesStrictReadFailurePrecedence() {
        AutoDepositTrustedDestination existing = destination(new BlockPos(5, 64, 5), true);
        TransactionalPersistence persistence = new TransactionalPersistence(List.of(existing));
        AutoDepositTrustedDestinationRepository repository =
                new AutoDepositTrustedDestinationRepository(persistence);
        List<AutoDepositTrustedDestination> before = repository.destinations();
        long revisionBefore = repository.revision();
        persistence.readResult = AutoDepositTrustedRegistryReadResult.failure(
                "expected_read_failure"
        );

        AutoDepositTrustedBulkMutationResult result = repository.registerBulk(
                "singleplayer:test",
                Dimension.OVERWORLD,
                List.of()
        );

        assertEquals(AutoDepositTrustedBulkMutationStatus.REGISTRY_READ_FAILED,
                result.status());
        assertFalse(result.success());
        assertEquals("expected_read_failure", result.reason());
        assertEquals(AutoDepositTrustedRegistryReadStatus.REGISTRY_READ_FAILED,
                result.registryReadStatus());
        assertEquals(0, result.newlyRegisteredCount());
        assertEquals(0, result.reenabledCount());
        assertEquals(0, result.alreadyRegisteredCount());
        assertEquals(revisionBefore, result.repositoryRevisionBefore());
        assertEquals(revisionBefore, result.repositoryRevisionAfter());
        assertEquals(1, result.totalRegistryCountBefore());
        assertEquals(1, result.totalRegistryCountAfter());
        assertEquals(1, persistence.strictReadCount);
        assertEquals(0, persistence.conditionalSaveCount);
        assertEquals(0, persistence.persistedWriteCount);
        assertEquals(revisionBefore, repository.revision());
        assertTrue(before == repository.destinations());
    }

    @Test
    void bulkEmptyLogicalSetPreservesPublishedSnapshotConflictPrecedence() {
        AutoDepositTrustedDestination externallyAdded =
                destination(new BlockPos(5, 64, 5), true);
        TransactionalPersistence persistence = new TransactionalPersistence(List.of());
        AutoDepositTrustedDestinationRepository repository =
                new AutoDepositTrustedDestinationRepository(persistence);
        List<AutoDepositTrustedDestination> before = repository.destinations();
        long revisionBefore = repository.revision();
        persistence.readResult = persistence.successfulRead(List.of(externallyAdded));

        AutoDepositTrustedBulkMutationResult result = repository.registerBulk(
                "singleplayer:test",
                Dimension.OVERWORLD,
                List.of()
        );

        assertEquals(
                AutoDepositTrustedBulkMutationStatus.EXTERNAL_MODIFICATION_CONFLICT,
                result.status()
        );
        assertFalse(result.success());
        assertEquals("published_registry_state_differs_from_strict_snapshot",
                result.reason());
        assertEquals("repository_snapshot_changed_before_no_change",
                result.firstConflict());
        assertEquals(AutoDepositTrustedRegistryReadStatus.VALID_POPULATED,
                result.registryReadStatus());
        assertEquals(0, result.newlyRegisteredCount());
        assertEquals(0, result.reenabledCount());
        assertEquals(0, result.alreadyRegisteredCount());
        assertEquals(revisionBefore, result.repositoryRevisionBefore());
        assertEquals(revisionBefore, result.repositoryRevisionAfter());
        assertEquals(1, result.totalRegistryCountBefore());
        assertEquals(1, result.totalRegistryCountAfter());
        assertEquals(1, persistence.strictReadCount);
        assertEquals(0, persistence.conditionalSaveCount);
        assertEquals(0, persistence.persistedWriteCount);
        assertEquals(revisionBefore, repository.revision());
        assertTrue(before == repository.destinations());
        assertTrue(repository.destinations().isEmpty());
    }

    @Test
    void bulkNoChangeFailsClosedWhenStrictSnapshotDiffersFromPublishedMemory() {
        AutoDepositTrustedDestination externallyAdded =
                destination(new BlockPos(5, 64, 5), true);
        TransactionalPersistence persistence = new TransactionalPersistence(List.of());
        AutoDepositTrustedDestinationRepository repository =
                new AutoDepositTrustedDestinationRepository(persistence);
        long revisionBefore = repository.revision();
        persistence.readResult = persistence.successfulRead(List.of(externallyAdded));

        AutoDepositTrustedBulkMutationResult result = repository.registerBulk(
                "singleplayer:test",
                Dimension.OVERWORLD,
                List.of(single(externallyAdded.position()))
        );

        assertEquals(
                AutoDepositTrustedBulkMutationStatus.EXTERNAL_MODIFICATION_CONFLICT,
                result.status()
        );
        assertEquals(0, persistence.conditionalSaveCount);
        assertEquals(0, persistence.persistedWriteCount);
        assertEquals(revisionBefore, repository.revision());
        assertTrue(repository.destinations().isEmpty());
    }

    @Test
    void bulkReadFailureConflictAndSaveFailureNeverPartiallyMutateMemoryOrRevision() {
        for (FailureMode mode : FailureMode.values()) {
            TransactionalPersistence persistence = new TransactionalPersistence(List.of());
            if (mode == FailureMode.READ) {
                persistence.readResult = AutoDepositTrustedRegistryReadResult.failure("expected_read_failure");
            } else if (mode == FailureMode.CONFLICT) {
                persistence.conditionalSaveStatus = AutoDepositTrustedConditionalSaveStatus.CONFLICT;
            } else {
                persistence.failConditionalSave = true;
            }
            AutoDepositTrustedDestinationRepository repository =
                    new AutoDepositTrustedDestinationRepository(persistence);
            long revisionBefore = repository.revision();

            AutoDepositTrustedBulkMutationResult result = repository.registerBulk(
                    "singleplayer:test",
                    Dimension.OVERWORLD,
                    List.of(single(new BlockPos(1, 64, 1)))
            );

            assertFalse(result.success(), mode.name());
            assertEquals(0, persistence.persistedWriteCount, mode.name());
            assertEquals(0, repository.destinations().size(), mode.name());
            assertEquals(revisionBefore, repository.revision(), mode.name());
        }
    }

    @Test
    void malformedRegistryCannotBeReinterpretedAsEmptyAndOverwrittenByBulkRegistration()
            throws Exception {
        Path path = temporaryDirectory.resolve("trusted.json");
        String malformed = "{not-json";
        Files.writeString(path, malformed);
        AutoDepositTrustedDestinationRepository repository =
                new AutoDepositTrustedDestinationRepository(
                        new AutoDepositTrustedDestinationStore(path)
                );
        long revisionBefore = repository.revision();

        AutoDepositTrustedBulkMutationResult result = repository.registerBulk(
                "singleplayer:test",
                Dimension.OVERWORLD,
                List.of(single(new BlockPos(1, 64, 1)))
        );

        assertEquals(AutoDepositTrustedBulkMutationStatus.REGISTRY_READ_FAILED, result.status());
        assertEquals(malformed, Files.readString(path));
        assertEquals(revisionBefore, repository.revision());
        assertEquals(0, repository.destinations().size());
    }

    @Test
    void zeroByteRegistryCannotAbortConstructionOrBeOverwrittenByBulkRegistration()
            throws Exception {
        Path path = temporaryDirectory.resolve("zero-byte-trusted.json");
        Files.write(path, new byte[0]);

        AutoDepositTrustedDestinationRepository repository =
                new AutoDepositTrustedDestinationRepository(
                        new AutoDepositTrustedDestinationStore(path)
                );
        long revisionBefore = repository.revision();
        AutoDepositTrustedBulkMutationResult result = repository.registerBulk(
                "singleplayer:test",
                Dimension.OVERWORLD,
                List.of(single(new BlockPos(1, 64, 1)))
        );

        assertEquals(AutoDepositTrustedBulkMutationStatus.REGISTRY_READ_FAILED, result.status());
        assertEquals(0L, Files.size(path));
        assertEquals(revisionBefore, repository.revision());
        assertTrue(repository.destinations().isEmpty());
    }

    @Test
    void bulkDoubleChestDuplicateRejectsEveryRequestedMutation() {
        BlockPos first = new BlockPos(1, 64, 1);
        BlockPos second = new BlockPos(2, 64, 1);
        TransactionalPersistence persistence = new TransactionalPersistence(List.of(
                destination(first, true),
                destination(second, true)
        ));
        AutoDepositTrustedDestinationRepository repository =
                new AutoDepositTrustedDestinationRepository(persistence);
        long revisionBefore = repository.revision();

        AutoDepositTrustedBulkMutationResult result = repository.registerBulk(
                "singleplayer:test",
                Dimension.OVERWORLD,
                List.of(
                        single(new BlockPos(10, 64, 10)),
                        AutoDepositBulkLogicalDestination.doubleChest(
                                AutoDepositBulkContainerKind.CHEST,
                                AutoDepositDoubleChestPairKey.of(first, second)
                        )
                )
        );

        assertEquals(
                AutoDepositTrustedBulkMutationStatus.PREEXISTING_DOUBLE_CHEST_DUPLICATE,
                result.status()
        );
        assertEquals(0, persistence.conditionalSaveCount);
        assertEquals(2, repository.destinations().size());
        assertEquals(revisionBefore, repository.revision());
    }

    @Test
    void bulkRegistrationDoesNotApplyTheOperationCandidateLimit() {
        TransactionalPersistence persistence = new TransactionalPersistence(List.of());
        AutoDepositTrustedDestinationRepository repository =
                new AutoDepositTrustedDestinationRepository(persistence);
        List<AutoDepositBulkLogicalDestination> logical = new ArrayList<>();
        for (int index = 0; index < 70; index++) {
            logical.add(single(new BlockPos(index, 64, 0)));
        }

        AutoDepositTrustedBulkMutationResult result = repository.registerBulk(
                "singleplayer:test",
                Dimension.OVERWORLD,
                logical
        );

        assertEquals(AutoDepositTrustedBulkMutationStatus.UPDATED, result.status());
        assertEquals(70, result.newlyRegisteredCount());
        assertEquals(70, repository.destinations().size());
        assertEquals(1, persistence.persistedWriteCount);
    }

    private static AutoDepositBulkLogicalDestination single(BlockPos position) {
        return AutoDepositBulkLogicalDestination.single(
                AutoDepositBulkContainerKind.BARREL,
                position
        );
    }

    private static AutoDepositTrustedDestination destination(BlockPos position, boolean enabled) {
        return new AutoDepositTrustedDestination(
                "singleplayer:test",
                Dimension.OVERWORLD,
                position,
                enabled
        );
    }

    private enum FailureMode {
        READ,
        CONFLICT,
        SAVE
    }

    private static final class TransactionalPersistence
            implements AutoDepositTrustedDestinationPersistence {
        private final AutoDepositTrustedRegistryProvenance provenance =
                AutoDepositTrustedRegistryProvenance.present(1L, 100L, "a".repeat(64));
        private List<AutoDepositTrustedDestination> persisted;
        private AutoDepositTrustedRegistryReadResult readResult;
        private AutoDepositTrustedConditionalSaveStatus conditionalSaveStatus =
                AutoDepositTrustedConditionalSaveStatus.SAVED;
        private boolean failConditionalSave;
        private int strictReadCount;
        private int conditionalSaveCount;
        private int persistedWriteCount;

        private TransactionalPersistence(List<AutoDepositTrustedDestination> initial) {
            persisted = List.copyOf(initial);
            readResult = successfulRead(persisted);
        }

        @Override
        public List<AutoDepositTrustedDestination> load() {
            return persisted;
        }

        @Override
        public void save(List<AutoDepositTrustedDestination> destinations) {
            persisted = List.copyOf(destinations);
        }

        @Override
        public long modifiedTime() {
            return 100L;
        }

        @Override
        public AutoDepositTrustedRegistryReadResult readStrictSnapshot() {
            strictReadCount++;
            return readResult;
        }

        @Override
        public AutoDepositTrustedConditionalSaveStatus saveIfUnchanged(
                AutoDepositTrustedRegistryProvenance expected,
                List<AutoDepositTrustedDestination> destinations) throws IOException {
            conditionalSaveCount++;
            if (failConditionalSave) {
                throw new IOException("expected conditional save failure");
            }
            if (conditionalSaveStatus == AutoDepositTrustedConditionalSaveStatus.SAVED) {
                persisted = List.copyOf(destinations);
                persistedWriteCount++;
            }
            return conditionalSaveStatus;
        }

        private AutoDepositTrustedRegistryReadResult successfulRead(
                List<AutoDepositTrustedDestination> destinations) {
            AutoDepositTrustedRegistryReadStatus status = destinations.isEmpty()
                    ? AutoDepositTrustedRegistryReadStatus.VALID_EMPTY
                    : AutoDepositTrustedRegistryReadStatus.VALID_POPULATED;
            return AutoDepositTrustedRegistryReadResult.success(
                    status,
                    new AutoDepositTrustedRegistrySnapshot(destinations, provenance)
            );
        }
    }

    private static final class FailingPersistence
            implements AutoDepositTrustedDestinationPersistence {
        @Override
        public List<AutoDepositTrustedDestination> load() {
            return List.of();
        }

        @Override
        public void save(List<AutoDepositTrustedDestination> destinations) throws IOException {
            throw new IOException("expected test failure");
        }

        @Override
        public long modifiedTime() {
            return 0L;
        }
    }
}
