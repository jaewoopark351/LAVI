package lavi.minecraft.task.container.home.execution.candidate;

import adris.altoclef.AltoClef;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedContainerSupport;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationCandidate;
import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestinationRepository;

import java.util.Objects;
import java.util.Optional;

//20260828_kpopmodder: Decide whether one trusted destination remains usable before activation.
public final class StoreHomeCandidateValidator {
    private final AutoDepositTrustedDestinationRepository repository;

    public StoreHomeCandidateValidator(
            AutoDepositTrustedDestinationRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public Optional<String> refusal(
            AltoClef mod,
            AutoDepositTrustedDestinationCandidate candidate) {
        Objects.requireNonNull(candidate, "candidate");
        if (!repository.containsEnabled(candidate.destination())) {
            return Optional.of("registration_removed_or_disabled");
        }
        if (mod == null || mod.getWorld() == null || mod.getPlayer() == null) {
            return Optional.of("runtime_unavailable");
        }
        if (mod.getBlockScanner().isUnreachable(candidate.position())) {
            return Optional.of("known_unreachable");
        }
        if (mod.getChunkTracker().isChunkLoaded(candidate.position())
                && !AutoDepositTrustedContainerSupport.isSupported(
                mod.getWorld().getBlockState(candidate.position()).getBlock())) {
            return Optional.of("container_missing");
        }
        return Optional.empty();
    }
}
