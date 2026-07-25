package adris.altoclef.lavibridge.http;

//20260725_kpopmodder: Added this functional interface for route callbacks that can fail.

@FunctionalInterface
public interface LaviRouteSupplier<T> {
    T get() throws Exception;
}
