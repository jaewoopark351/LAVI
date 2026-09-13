package lavi.minecraft.testsupport.mixin.gold;

import lavi.minecraft.testsupport.mixin.IsolatedMixinService;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

//20260914_kpopmodder: Read only selected class resources, including the real application's registration/configuration.
public record GoldMixinInputs(byte[] world, byte[] mixin, byte[] altoClef, byte[] applicationConfig) {
    public static final String MIXIN = "adris/altoclef/mixins/WorldBlockModifiedMixin";
    public static GoldMixinInputs read(IsolatedMixinService service, WorldProtectionNames names) throws Exception {
        byte[] configuration = resource(service, "altoclef.mixins.json");
        String text = new String(configuration, StandardCharsets.UTF_8);
        if (!text.contains("\"WorldBlockModifiedMixin\""))
            throw new AssertionError("Actual application does not register WorldBlockModifiedMixin");
        return new GoldMixinInputs(resource(service, names.world() + ".class"),
                resource(service, MIXIN + ".class"), resource(service, "adris/altoclef/AltoClef.class"), configuration);
    }
    private static byte[] resource(IsolatedMixinService service, String path) throws Exception {
        try (InputStream input = service.getResourceAsStream(path)) {
            if (input == null) throw new AssertionError("Missing actual selected-input resource: " + path);
            return input.readAllBytes();
        }
    }
    public static String sha(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
