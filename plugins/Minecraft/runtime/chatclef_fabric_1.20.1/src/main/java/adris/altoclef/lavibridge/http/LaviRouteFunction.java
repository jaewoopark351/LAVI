package adris.altoclef.lavibridge.http;

//20260725_kpopmodder: Added this functional interface for request-body route callbacks that can fail.

@FunctionalInterface
public interface LaviRouteFunction<T, R> {
    R apply(T value) throws Exception;
}
