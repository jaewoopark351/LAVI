package lavi.minecraft.task.container.deposit.auto.trusted;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

//20260827_kpopmodder: Derive a stable command-safe identifier from exact trusted container identity.
public final class AutoDepositTrustedDestinationId {
    private static final int DISPLAY_HEX_LENGTH = 24;

    private AutoDepositTrustedDestinationId() {
    }

    public static String fromCanonicalIdentity(String canonicalIdentity) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonicalIdentity.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder("td_");
            for (byte value : digest) {
                result.append(Character.forDigit((value >>> 4) & 0x0f, 16));
                result.append(Character.forDigit(value & 0x0f, 16));
                if (result.length() >= 3 + DISPLAY_HEX_LENGTH) {
                    break;
                }
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
