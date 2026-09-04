package lavi.minecraft.task.container.deposit.auto.trusted.persistence.read;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

//20260904_kpopmodder: Added this type file to keep one primary Java type per file.
public final class AutoDepositTrustedRegistryFileReader {
    public AutoDepositTrustedRegistryFileSnapshot read(Path path) throws IOException {
        Path normalizedPath = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        BasicFileAttributes before;
        try {
            before = Files.readAttributes(normalizedPath, BasicFileAttributes.class);
        } catch (NoSuchFileException missing) {
            return new AutoDepositTrustedRegistryFileSnapshot(
                    new byte[0],
                    AutoDepositTrustedRegistryProvenance.missing()
            );
        }

        if (!before.isRegularFile()) {
            throw new IOException("trusted destination registry is not a regular file");
        }
        byte[] payload = Files.readAllBytes(normalizedPath);
        BasicFileAttributes after = Files.readAttributes(normalizedPath, BasicFileAttributes.class);
        if (!sameFileState(before, after) || after.size() != payload.length) {
            throw new IOException("trusted destination registry changed during read");
        }
        return new AutoDepositTrustedRegistryFileSnapshot(
                payload,
                AutoDepositTrustedRegistryProvenance.present(
                        after.size(),
                        after.lastModifiedTime().toMillis(),
                        sha256(payload)
                )
        );
    }

    private static boolean sameFileState(BasicFileAttributes before, BasicFileAttributes after) {
        return before.isRegularFile() == after.isRegularFile()
                && before.size() == after.size()
                && before.lastModifiedTime().equals(after.lastModifiedTime())
                && Objects.equals(before.fileKey(), after.fileKey());
    }

    private static String sha256(byte[] payload) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(payload);
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                result.append(Character.forDigit((value >>> 4) & 0x0f, 16));
                result.append(Character.forDigit(value & 0x0f, 16));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
