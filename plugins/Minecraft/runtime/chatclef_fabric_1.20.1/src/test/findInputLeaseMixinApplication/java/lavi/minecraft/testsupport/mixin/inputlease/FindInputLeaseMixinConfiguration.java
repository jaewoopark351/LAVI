package lavi.minecraft.testsupport.mixin.inputlease;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

//20260914_kpopmodder: Narrow the actual application's registered lists only inside a fresh repository-local smoke output.
public final class FindInputLeaseMixinConfiguration {
    public static final String RESOURCE = "lavi-find-input-lease-selected.mixins.json";
    private FindInputLeaseMixinConfiguration() { }
    public static void prepare(FindInputLeaseMixinInputs inputs) throws Exception {
        var application = JsonParser.parseString(new String(inputs.applicationConfig(), StandardCharsets.UTF_8)).getAsJsonObject();
        if (!application.get("package").getAsString().equals("adris.altoclef.mixins"))
            throw new AssertionError("Unexpected selected application's Mixin package");
        boolean registered = false;
        for (var mixin : application.getAsJsonArray("mixins"))
            if (mixin.getAsString().equals("ownership.InputOverrideLeaseMixin")) registered = true;
        if (!registered) throw new AssertionError("Selected application does not register input-lease observer");
        var selected = application.deepCopy();
        JsonArray mixins = new JsonArray(); mixins.add("ownership.InputOverrideLeaseMixin");
        selected.add("mixins", mixins); selected.remove("client"); selected.remove("server");
        String output = System.getProperty("lavi.find.input.lease.smoke.output");
        if (output == null) throw new AssertionError("Explicit repository-local smoke output is required");
        Path directory = Path.of(output).toAbsolutePath().normalize();
        Path boundary = Path.of("C:/Vtuber_Souorce_Code/LAVI/test/test_Isolation/minecraft/find_input_lease_mixin_application/output")
                .toAbsolutePath().normalize();
        if (!directory.startsWith(boundary) || directory.equals(boundary) || !Files.isDirectory(directory))
            throw new AssertionError("Invalid isolated configuration output: " + directory);
        byte[] bytes = new GsonBuilder().setPrettyPrinting().create().toJson(selected).getBytes(StandardCharsets.UTF_8);
        Files.write(directory.resolve(RESOURCE), bytes, StandardOpenOption.CREATE_NEW);
        System.out.println("FIND_INPUT_LEASE_CONFIG resource=" + RESOURCE + " sha256=" + FindInputLeaseMixinInputs.sha(bytes)
                + " source=ACTUAL_APPLICATION_CONFIG; NARROWED_LISTS=mixins,client,server; REMAP=FALSE");
    }
}
