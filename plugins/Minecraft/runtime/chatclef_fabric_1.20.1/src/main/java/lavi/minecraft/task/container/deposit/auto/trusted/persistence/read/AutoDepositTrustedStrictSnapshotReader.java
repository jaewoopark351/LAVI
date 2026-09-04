package lavi.minecraft.task.container.deposit.auto.trusted.persistence.read;

import lavi.minecraft.task.container.deposit.auto.trusted.AutoDepositTrustedDestination;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
public final class AutoDepositTrustedStrictSnapshotReader {
    private final AutoDepositTrustedRegistryFileReader fileReader;
    private final AutoDepositTrustedRegistryJsonDecoder decoder;

    public AutoDepositTrustedStrictSnapshotReader() {
        this(new AutoDepositTrustedRegistryFileReader(), new AutoDepositTrustedRegistryJsonDecoder());
    }

    AutoDepositTrustedStrictSnapshotReader(
            AutoDepositTrustedRegistryFileReader fileReader,
            AutoDepositTrustedRegistryJsonDecoder decoder) {
        this.fileReader = Objects.requireNonNull(fileReader, "fileReader");
        this.decoder = Objects.requireNonNull(decoder, "decoder");
    }

    public AutoDepositTrustedRegistryReadResult read(Path path) {
        try {
            AutoDepositTrustedRegistryFileSnapshot file = fileReader.read(path);
            if (!file.provenance().exists()) {
                return AutoDepositTrustedRegistryReadResult.success(
                        AutoDepositTrustedRegistryReadStatus.FILE_MISSING_VALID_EMPTY,
                        new AutoDepositTrustedRegistrySnapshot(List.of(), file.provenance())
                );
            }
            List<AutoDepositTrustedDestination> destinations = decoder.decode(file.payload());
            AutoDepositTrustedRegistryReadStatus status = destinations.isEmpty()
                    ? AutoDepositTrustedRegistryReadStatus.VALID_EMPTY
                    : AutoDepositTrustedRegistryReadStatus.VALID_POPULATED;
            return AutoDepositTrustedRegistryReadResult.success(
                    status,
                    new AutoDepositTrustedRegistrySnapshot(destinations, file.provenance())
            );
        } catch (IOException | RuntimeException exception) {
            return AutoDepositTrustedRegistryReadResult.failure("registry_read_or_parse_failed");
        }
    }
}
