package lavi.minecraft.testsupport.mixin.inputlease;

import lavi.minecraft.testsupport.mixin.IsolatedMixinService;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;

//20260914_kpopmodder: Read actual selected classes/configuration without loading any Minecraft or Baritone class.
public record FindInputLeaseMixinInputs(byte[] target, byte[] mixin, byte[] ledger, byte[] applicationConfig) {
    public static final String TARGET = "baritone.utils.InputOverrideHandler";
    public static final String MIXIN = "adris/altoclef/mixins/ownership/InputOverrideLeaseMixin";
    public static FindInputLeaseMixinInputs read(IsolatedMixinService service) throws Exception {
        return new FindInputLeaseMixinInputs(resource(service, TARGET.replace('.', '/') + ".class"),
                resource(service, MIXIN + ".class"),
                resource(service, "lavi/minecraft/integration/input/lease/ForcedInputLeaseLedger.class"),
                resource(service, "altoclef.mixins.json"));
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
