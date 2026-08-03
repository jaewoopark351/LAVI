package lavi.minecraft.diagnostics;

//20260803_kpopmodder: Keep diagnostics runtime identity fields out of the public diagnostics facade.
final class RuntimeIdentityDiagnostics {
    private RuntimeIdentityDiagnostics() {
    }

    static Object[] fields(DiagnosticOutputMode outputMode, Class<?> sourceClass) {
        return new Object[]{
                "outputMode", outputMode.name(),
                "diagnosticsSourceMarker", "20260731_post_place_handoff_p2",
                "diagnosticsClassCodeSource", codeSourceLocation(sourceClass),
                "diagnosticsCodeSourceLastModified", codeSourceLastModified(sourceClass),
                "diagnosticsImplementationVersion", implementationVersion(sourceClass)
        };
    }

    private static String codeSourceLocation(Class<?> sourceClass) {
        try {
            java.security.CodeSource source = sourceClass.getProtectionDomain().getCodeSource();
            return source == null || source.getLocation() == null ? "unavailable" : source.getLocation().toString();
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    private static String codeSourceLastModified(Class<?> sourceClass) {
        try {
            java.security.CodeSource source = sourceClass.getProtectionDomain().getCodeSource();
            if (source == null || source.getLocation() == null) {
                return "unavailable";
            }
            java.nio.file.Path path = java.nio.file.Paths.get(source.getLocation().toURI());
            if (!java.nio.file.Files.exists(path)) {
                return "unavailable";
            }
            return java.nio.file.Files.getLastModifiedTime(path).toString();
        } catch (Exception | LinkageError ignored) {
            return "unavailable";
        }
    }

    private static String implementationVersion(Class<?> sourceClass) {
        try {
            Package packageInfo = sourceClass.getPackage();
            String implementationVersion = packageInfo == null ? null : packageInfo.getImplementationVersion();
            return implementationVersion == null || implementationVersion.isBlank() ? "unavailable" : implementationVersion;
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }
}
