package lavi.minecraft.testsupport.mixin.gold;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

//20260914_kpopmodder: Preserve actual application configuration and narrow only its registered target list for this isolated check.
public final class GoldMixinConfiguration {
    public static final String RESOURCE = "lavi-gold-protection-selected.mixins.json";
    private GoldMixinConfiguration() { }

    public static JsonObject prepare(GoldMixinInputs inputs) throws Exception {
        JsonObject application = JsonParser.parseString(new String(inputs.applicationConfig(), StandardCharsets.UTF_8)).getAsJsonObject();
        if (!application.get("package").getAsString().equals("adris.altoclef.mixins"))
            throw new AssertionError("Unexpected selected application's Mixin package");
        boolean registered = false;
        for (var mixin : application.getAsJsonArray("mixins"))
            if (mixin.getAsString().equals("WorldBlockModifiedMixin")) registered = true;
        if (!registered) throw new AssertionError("Selected application does not register WorldBlockModifiedMixin");
        JsonObject selected = application.deepCopy();
        JsonArray mixins = new JsonArray();
        mixins.add("WorldBlockModifiedMixin");
        selected.add("mixins", mixins);
        selected.remove("client");
        selected.remove("server");
        String output = System.getProperty("lavi.gold.smoke.output");
        if (output == null) throw new AssertionError("Explicit repository-local smoke output is required");
        Path directory = Path.of(output).toAbsolutePath().normalize();
        Path boundary = Path.of("C:/Vtuber_Souorce_Code/LAVI/test/test_Isolation/minecraft/gold_mixin_application/output").toAbsolutePath().normalize();
        if (!directory.startsWith(boundary) || directory.equals(boundary) || !Files.isDirectory(directory))
            throw new AssertionError("Invalid isolated configuration output: " + directory);
        byte[] encoded = new GsonBuilder().setPrettyPrinting().create().toJson(selected).getBytes(StandardCharsets.UTF_8);
        Files.write(directory.resolve(RESOURCE), encoded, StandardOpenOption.CREATE_NEW);
        System.out.println("GOLD_PROTECTION_CONFIG resource=" + RESOURCE + " sha256=" + GoldMixinInputs.sha(encoded)
                + " source=ACTUAL_APPLICATION_CONFIG; NARROWED_LISTS=mixins,client,server");
        return selected;
    }
}
