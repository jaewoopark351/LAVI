package lavi.minecraft.task.container.deposit.auto.trusted.bulk;

import lavi.minecraft.task.container.deposit.auto.trusted.bulk.scan.AutoDepositBulkScanBounds;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
public final class AutoDepositTrustedBulkRegistrationFormatter {
    private AutoDepositTrustedBulkRegistrationFormatter() {
    }

    public static String format(AutoDepositTrustedBulkRegistrationResult result) {
        String anchor = result.anchorPosition()
                .map(position -> position.getX() + "," + position.getY() + "," + position.getZ())
                .orElse("unavailable");
        String requestedRange = result.bounds()
                .map(AutoDepositTrustedBulkRegistrationFormatter::requestedRange)
                .orElse("unavailable");
        String effectiveRange = result.bounds()
                .map(AutoDepositTrustedBulkRegistrationFormatter::effectiveRange)
                .orElse("unavailable");
        return "[AutoDepositBulkTrust] lifecycle=REGISTRATION_TERMINAL"
                + " status=" + result.status()
                + " reason=" + result.reason()
                + " boundedFirstConflict=" + result.boundedFirstConflict()
                + " commandForm=\"" + result.commandForm().canonicalInvocation() + "\""
                + " registryReadStatus=" + result.registryReadStatus()
                + " anchorSource=" + result.anchorSource().name()
                + " anchorPos=" + anchor
                + " requestedRange=" + requestedRange
                + " effectiveRange=" + effectiveRange
                + " coverageComplete=" + result.coverageComplete()
                + " scannedPositionCount=" + result.scannedPositionCount()
                + " physicalSupportedBlockCount=" + result.physicalSupportedBlockCount()
                + " logicalDestinationCount=" + result.logicalDestinationCount()
                + " newlyRegisteredCount=" + result.newlyRegisteredCount()
                + " reenabledCount=" + result.reenabledCount()
                + " alreadyRegisteredCount=" + result.alreadyRegisteredCount()
                + " doubleChestCollapsedHalfCount=" + result.doubleChestCollapsedHalfCount()
                + " repositoryRevisionBefore=" + result.repositoryRevisionBefore()
                + " repositoryRevisionAfter=" + result.repositoryRevisionAfter()
                + " totalRegistryCountBefore=" + result.totalRegistryCountBefore()
                + " totalRegistryCountAfter=" + result.totalRegistryCountAfter()
                + " downstreamAutomaticReevaluationPossible="
                + result.downstreamAutomaticReevaluationPossible();
    }

    private static String requestedRange(AutoDepositBulkScanBounds bounds) {
        return interval(bounds.requestedMinX(), bounds.requestedMaxXExclusive())
                + "x" + interval(bounds.requestedMinY(), bounds.requestedMaxYExclusive())
                + "x" + interval(bounds.requestedMinZ(), bounds.requestedMaxZExclusive());
    }

    private static String effectiveRange(AutoDepositBulkScanBounds bounds) {
        return interval(bounds.requestedMinX(), bounds.requestedMaxXExclusive())
                + "x" + interval(bounds.effectiveMinY(), bounds.effectiveMaxYExclusive())
                + "x" + interval(bounds.requestedMinZ(), bounds.requestedMaxZExclusive());
    }

    private static String interval(int minimum, int maximumExclusive) {
        return "[" + minimum + "," + maximumExclusive + ")";
    }
}
