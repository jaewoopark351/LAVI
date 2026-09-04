package lavi.minecraft.task.container.deposit.auto.trusted.command;

import adris.altoclef.AltoClef;
import adris.altoclef.util.Dimension;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.AutoDepositTrustedBulkRegistrationFormatter;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.AutoDepositTrustedBulkRegistrationResult;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.anchor.AutoDepositBulkAnchorKind;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.repository.AutoDepositTrustedBulkMutationResult;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.repository.AutoDepositTrustedBulkMutationStatus;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan.AutoDepositBulkBuildHeight;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan.AutoDepositBulkScanBounds;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan.AutoDepositBulkScanResult;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan.AutoDepositBulkWorldProvenance;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.topology.AutoDepositBulkTopologyNormalizer;
import lavi.minecraft.task.container.deposit.auto.trusted.bulk.topology.AutoDepositBulkTopologyResult;
import lavi.minecraft.task.container.deposit.auto.trusted.command.form.AutoDepositTrustCommandForm;
import lavi.minecraft.task.container.deposit.auto.trusted.persistence.read.AutoDepositTrustedRegistryReadStatus;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

//20260904_kpopmodder: Added focused command-adapter tests for exact bulk result log severity.
final class AutoDepositTrustedBulkRegistrationCommandOperationTest {
    @Test
    void successfulResultEmitsExactlyOneInfoLogAndNoWarning() {
        AutoDepositTrustedBulkRegistrationResult result = successfulResult();
        RecordingAltoClef mod = new RecordingAltoClef();
        AtomicInteger executions = new AtomicInteger();
        AutoDepositTrustedBulkRegistrationCommandOperation operation =
                new AutoDepositTrustedBulkRegistrationCommandOperation((actualMod, form) -> {
                    executions.incrementAndGet();
                    assertSame(mod, actualMod);
                    assertSame(AutoDepositTrustCommandForm.ENGLISH_AREA, form);
                    return result;
                });

        operation.register(mod, AutoDepositTrustCommandForm.ENGLISH_AREA);

        assertEquals(1, executions.get());
        assertEquals(1, mod.infoCount);
        assertEquals(0, mod.warningCount);
        assertEquals(AutoDepositTrustedBulkRegistrationFormatter.format(result), mod.infoMessage);
    }

    @Test
    void failedResultEmitsExactlyOneWarningAndNoInfoLog() {
        AutoDepositTrustedBulkRegistrationResult result =
                AutoDepositTrustedBulkRegistrationResult.beforeScanFailure(
                        AutoDepositBulkAnchorKind.PLAYER_BLOCK_POSITION,
                        AutoDepositTrustCommandForm.KOREAN_RADIUS,
                        null,
                        "INVALID_ANCHOR",
                        "player_position_anchor_unavailable",
                        "none"
                );
        RecordingAltoClef mod = new RecordingAltoClef();
        AtomicInteger executions = new AtomicInteger();
        AutoDepositTrustedBulkRegistrationCommandOperation operation =
                new AutoDepositTrustedBulkRegistrationCommandOperation((actualMod, form) -> {
                    executions.incrementAndGet();
                    assertSame(mod, actualMod);
                    assertSame(AutoDepositTrustCommandForm.KOREAN_RADIUS, form);
                    return result;
                });

        operation.register(mod, AutoDepositTrustCommandForm.KOREAN_RADIUS);

        assertEquals(1, executions.get());
        assertEquals(0, mod.infoCount);
        assertEquals(1, mod.warningCount);
        assertEquals(
                AutoDepositTrustedBulkRegistrationFormatter.format(result),
                mod.warningMessage
        );
    }

    private static AutoDepositTrustedBulkRegistrationResult successfulResult() {
        BlockPos anchor = new BlockPos(0, 64, 0);
        AutoDepositBulkWorldProvenance provenance = new AutoDepositBulkWorldProvenance(
                "singleplayer:command-operation-test",
                Dimension.OVERWORLD,
                "minecraft:overworld",
                new Object()
        );
        AutoDepositBulkScanBounds bounds = AutoDepositBulkScanBounds.around(
                anchor,
                new AutoDepositBulkBuildHeight(-64, 320)
        );
        AutoDepositBulkScanResult scan = AutoDepositBulkScanResult.success(
                anchor,
                provenance,
                bounds,
                List.of(),
                bounds.effectivePositionCount(),
                0
        );
        AutoDepositBulkTopologyResult topology =
                new AutoDepositBulkTopologyNormalizer().normalize(scan);
        AutoDepositTrustedBulkMutationResult mutation =
                AutoDepositTrustedBulkMutationResult.of(
                        AutoDepositTrustedBulkMutationStatus.NO_CHANGE,
                        "no_supported_destinations_found",
                        "",
                        AutoDepositTrustedRegistryReadStatus.VALID_EMPTY,
                        null,
                        1L,
                        1L,
                        0,
                        0
                );
        return AutoDepositTrustedBulkRegistrationResult.mutation(
                AutoDepositBulkAnchorKind.PLAYER_BLOCK_POSITION,
                AutoDepositTrustCommandForm.ENGLISH_AREA,
                scan,
                topology,
                mutation
        );
    }

    private static final class RecordingAltoClef extends AltoClef {
        private int infoCount;
        private int warningCount;
        private String infoMessage;
        private String warningMessage;

        @Override
        public void log(String message) {
            infoCount++;
            infoMessage = message;
        }

        @Override
        public void logWarning(String message) {
            warningCount++;
            warningMessage = message;
        }
    }
}
