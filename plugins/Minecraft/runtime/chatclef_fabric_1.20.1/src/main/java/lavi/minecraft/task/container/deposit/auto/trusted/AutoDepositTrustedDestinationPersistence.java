package lavi.minecraft.task.container.deposit.auto.trusted;

import java.io.IOException;
import java.util.List;

//20260827_kpopmodder: Isolate trusted destination persistence from registry mutation results.
public interface AutoDepositTrustedDestinationPersistence {
    List<AutoDepositTrustedDestination> load() throws IOException;

    void save(List<AutoDepositTrustedDestination> destinations) throws IOException;

    long modifiedTime();
}
