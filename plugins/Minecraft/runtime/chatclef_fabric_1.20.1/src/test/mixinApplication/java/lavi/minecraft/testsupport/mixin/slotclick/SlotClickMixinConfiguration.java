package lavi.minecraft.testsupport.mixin.slotclick;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lavi.minecraft.testsupport.mixin.IsolatedMixinService;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.refmap.ReferenceMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

//20260916_kpopmodder: Narrow the actual application configuration without replacing its refmap or injector settings.
final class SlotClickMixinConfiguration {
    static final String RESOURCE = "lavi-slot-click-selected.mixins.json";
    static final String MIXIN = "adris/altoclef/mixins/SlotClickMixin";

    private SlotClickMixinConfiguration() { }

    static void prepare(IsolatedMixinService service, byte[] applicationBytes,
                        SlotClickTargetNames names, boolean intermediary) throws Exception {
        JsonObject application = JsonParser.parseString(new String(applicationBytes, StandardCharsets.UTF_8))
                .getAsJsonObject();
        if (!application.get("package").getAsString().equals("adris.altoclef.mixins"))
            throw new AssertionError("Unexpected application Mixin package");
        boolean registered = false;
        for (var entry : application.getAsJsonArray("mixins"))
            if (entry.getAsString().equals("SlotClickMixin")) registered = true;
        if (!registered) throw new AssertionError("Actual application does not register SlotClickMixin");
        JsonObject selected = application.deepCopy();
        JsonArray mixins = new JsonArray();
        mixins.add("SlotClickMixin");
        selected.add("mixins", mixins);
        selected.remove("client");
        selected.remove("server");

        String output = System.getProperty("lavi.slotclick.smoke.output");
        if (output == null) throw new AssertionError("Explicit repository-local smoke output is required");
        Path directory = Path.of(output).toAbsolutePath().normalize();
        Path boundary = Path.of("C:/Vtuber_Souorce_Code/LAVI/test/test_Isolation").toAbsolutePath().normalize();
        if (!directory.startsWith(boundary) || directory.equals(boundary) || !Files.isDirectory(directory))
            throw new AssertionError("Invalid isolated configuration output: " + directory);
        byte[] encoded = new GsonBuilder().setPrettyPrinting().create().toJson(selected)
                .getBytes(StandardCharsets.UTF_8);
        Files.write(directory.resolve(RESOURCE), encoded, StandardOpenOption.CREATE_NEW);
        System.out.println("SLOT_CLICK_CONFIG resource=" + RESOURCE + " sha256=" + SlotClickMixinApplicationSmoke.sha(encoded)
                + " source=ACTUAL_APPLICATION_CONFIG; NARROWED_LISTS=mixins,client,server");

        if (!intermediary) {
            if (selected.has("refmap"))
                throw new AssertionError("Named input contains a packaged refmap; select matching input namespaces");
            System.out.println("SLOT_CLICK_REFMAP resource=ABSENT_IN_NAMED_CONFIG; NAMESPACE=NAMED_1.20.1");
            return;
        }
        if (!selected.has("refmap")) throw new AssertionError("Artifact does not specify its actual refmap");
        String resource = selected.get("refmap").getAsString();
        byte[] bytes = SlotClickMixinApplicationSmoke.resource(service, resource);
        JsonObject refmap = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
        var contexts = refmap.getAsJsonObject("data").keySet().stream()
                .filter(value -> value.endsWith(":intermediary")).toList();
        if (contexts.size() != 1) throw new AssertionError("Ambiguous intermediary refmap contexts: " + contexts);
        String context = contexts.get(0);
        ReferenceMapper mapper = ReferenceMapper.read(resource);
        if (mapper.isDefault()) throw new AssertionError("Sponge did not load the selected artifact refmap");
        mapper.setContext(context);
        String innerSelector = SlotClickTargetNames.forArtifact(false).reference("internalOnSlotClick");
        if (!mapper.remap(MIXIN, "onSlotClick").equals(names.reference(names.outer()))
                || !mapper.remap(MIXIN, innerSelector).equals(names.reference(names.inner())))
            throw new AssertionError("Actual refmap does not resolve the outer selector and native target");
        MixinEnvironment.getDefaultEnvironment().setObfuscationContext(context);
        System.out.println("SLOT_CLICK_REFMAP resource=" + resource + " sha256=" + SlotClickMixinApplicationSmoke.sha(bytes)
                + " context=" + context + " outer=" + names.reference(names.outer())
                + " nativeTarget=" + names.reference(names.inner()));
    }
}
