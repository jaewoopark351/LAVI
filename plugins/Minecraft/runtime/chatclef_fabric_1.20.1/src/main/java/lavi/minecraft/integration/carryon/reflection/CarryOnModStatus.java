package lavi.minecraft.integration.carryon.reflection;

//20260731_kpopmodder: Represent Carry On Fabric loader probe results without invoking Carry On APIs.
public record CarryOnModStatus(boolean loaded, String version, Throwable error) {
    static CarryOnModStatus available(boolean loaded, String version) {
        return new CarryOnModStatus(loaded, version, null);
    }

    static CarryOnModStatus failed(Throwable error) {
        return new CarryOnModStatus(false, "unknown", error);
    }
}
