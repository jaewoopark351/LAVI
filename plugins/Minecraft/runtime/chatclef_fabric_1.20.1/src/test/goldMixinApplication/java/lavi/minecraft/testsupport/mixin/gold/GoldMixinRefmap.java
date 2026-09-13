package lavi.minecraft.testsupport.mixin.gold;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lavi.minecraft.testsupport.mixin.IsolatedMixinService;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.refmap.ReferenceMapper;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

//20260914_kpopmodder: Select the actual packaged refmap context and verify Sponge resolves the unchanged injection selector.
public final class GoldMixinRefmap {
    private GoldMixinRefmap() { }

    public static void activate(IsolatedMixinService service, JsonObject configuration,
                                WorldProtectionNames names, boolean intermediary) throws Exception {
        if (!intermediary) {
            if (configuration.has("refmap"))
                throw new AssertionError("Named input unexpectedly contains a packaged refmap; select the matching artifact namespace");
            System.out.println("GOLD_PROTECTION_REFMAP resource=ABSENT_IN_NAMED_CONFIG; NAMESPACE=NAMED_1.20.1");
            return;
        }
        if (!configuration.has("refmap")) throw new AssertionError("Actual artifact configuration does not specify its refmap");
        String resource = configuration.get("refmap").getAsString();
        byte[] bytes;
        try (InputStream input = service.getResourceAsStream(resource)) {
            if (input == null) throw new AssertionError("Missing actual artifact refmap: " + resource);
            bytes = input.readAllBytes();
        }
        JsonObject refmap = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
        List<String> contexts = refmap.getAsJsonObject("data").keySet().stream()
                .filter(value -> value.endsWith(":intermediary")).toList();
        if (contexts.size() != 1) throw new AssertionError("Artifact must identify one unambiguous intermediary refmap context: " + contexts);
        String context = contexts.get(0);
        ReferenceMapper mapper = ReferenceMapper.read(resource);
        if (mapper.isDefault()) throw new AssertionError("Sponge did not load actual artifact refmap: " + resource);
        mapper.setContext(context);
        String resolved = mapper.remap(GoldMixinInputs.MIXIN, "onBlockChanged");
        String expected = "L" + names.world() + ";" + names.changedMethod() + names.changedDescriptor();
        if (!resolved.equals(expected)) throw new AssertionError("Actual refmap selector does not match selected target: " + resolved);
        MixinEnvironment.getDefaultEnvironment().setObfuscationContext(context);
        System.out.println("GOLD_PROTECTION_REFMAP resource=" + resource + " sha256=" + GoldMixinInputs.sha(bytes)
                + " context=" + context + " selector=onBlockChanged resolved=" + resolved);
    }
}
