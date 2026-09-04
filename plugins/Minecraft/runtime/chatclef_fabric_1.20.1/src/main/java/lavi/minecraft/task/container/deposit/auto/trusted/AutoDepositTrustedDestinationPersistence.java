package lavi.minecraft.task.container.deposit.auto.trusted;

import lavi.minecraft.task.container.deposit.auto.trusted.persistence.AutoDepositTrustedConditionalSaveStatus;
import lavi.minecraft.task.container.deposit.auto.trusted.persistence.read.AutoDepositTrustedRegistryProvenance;
import lavi.minecraft.task.container.deposit.auto.trusted.persistence.read.AutoDepositTrustedRegistryReadResult;

import java.io.IOException;
import java.util.List;

//20260827_kpopmodder: Isolate trusted destination persistence from registry mutation results.
public interface AutoDepositTrustedDestinationPersistence {
    List<AutoDepositTrustedDestination> load() throws IOException;

    void save(List<AutoDepositTrustedDestination> destinations) throws IOException;

    long modifiedTime();

    default AutoDepositTrustedRegistryReadResult readStrictSnapshot() {
        return AutoDepositTrustedRegistryReadResult.failure("strict_read_unsupported");
    }

    default AutoDepositTrustedConditionalSaveStatus saveIfUnchanged(
            AutoDepositTrustedRegistryProvenance expected,
            List<AutoDepositTrustedDestination> destinations) throws IOException {
        return AutoDepositTrustedConditionalSaveStatus.UNSUPPORTED;
    }
}
