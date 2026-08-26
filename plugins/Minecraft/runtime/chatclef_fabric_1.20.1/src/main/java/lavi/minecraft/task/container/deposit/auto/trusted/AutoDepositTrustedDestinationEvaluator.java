package lavi.minecraft.task.container.deposit.auto.trusted;

import adris.altoclef.AltoClef;

@FunctionalInterface
interface AutoDepositTrustedDestinationEvaluator {
    AutoDepositTrustedDestinationEvaluation evaluate(AltoClef mod,
                                                      AutoDepositTrustedDestination destination,
                                                      int requiredEmptySlots,
                                                      int maximumDistance);
}
