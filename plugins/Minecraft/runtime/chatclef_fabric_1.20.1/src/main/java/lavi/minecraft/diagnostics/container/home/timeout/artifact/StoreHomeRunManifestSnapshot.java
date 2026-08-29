package lavi.minecraft.diagnostics.container.home.timeout.artifact;

import lavi.minecraft.diagnostics.container.home.timeout.budget.StoreHomeDiagnosticLimits;
import net.fabricmc.loader.api.FabricLoader;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

//20260828_kpopmodder: Capture one immutable and honest STORE_HOME runtime/artifact identity snapshot.
public record StoreHomeRunManifestSnapshot(
        String runManifestId,
        String runManifestIdSource,
        String gitHead,
        String relevantSourceIdentity,
        String worktreeDirty,
        String dirtyDiffIdentity,
        String dirtyDiffSha256,
        String buildInputManifestSha256,
        String untrackedInputPathsAndSha256,
        String builtJarPath,
        String builtJarSize,
        String builtJarSha256,
        String deployedJarPath,
        String deployedJarSize,
        String deployedJarSha256,
        String runtimeJarSha256,
        String runtimeCodeSource,
        String runtimeCodeSourceLastModified,
        String runtimeCodeSourceIdentity,
        String artifactParity,
        String artifactParityReason,
        String javaVendor,
        String javaVersion,
        String javaRuntimePath,
        String minecraftVersion,
        String fabricLoaderVersion,
        String chatClefVersion,
        String diagnosticsMode,
        String worldSnapshotId,
        String inventorySnapshotId,
        String runStartedAt,
        String runEndedAt) {

    private static final String UNVERIFIED = "UNVERIFIED";
    private static final int MAX_PROPERTY_LENGTH = 1024;

    public static StoreHomeRunManifestSnapshot capture(
            String diagnosticsMode,
            Class<?> runtimeSourceClass) {
        String configuredRunId = property("lavi.storeHome.runManifestId");
        String runId = UNVERIFIED.equals(configuredRunId)
                ? "store-home-runtime-" + UUID.randomUUID()
                : configuredRunId;
        String runIdSource = UNVERIFIED.equals(configuredRunId)
                ? "RUNTIME_GENERATED"
                : "EXTERNAL_BUILD_DEPLOY_MANIFEST";

        String builtJar = shaProperty("lavi.storeHome.builtJarSha256");
        String deployedJar = shaProperty("lavi.storeHome.deployedJarSha256");
        String runtimeJar = shaProperty("lavi.storeHome.runtimeJarSha256");
        String repositoryHead = property("lavi.storeHome.gitHead");
        String relevantSource = property("lavi.storeHome.relevantSourceIdentity");
        String buildInputManifest = shaProperty(
                "lavi.storeHome.buildInputManifestSha256"
        );
        String parityReason = parityReason(
                repositoryHead,
                relevantSource,
                buildInputManifest,
                builtJar,
                deployedJar,
                runtimeJar
        );
        String runtimeLocation = codeSourceLocation(runtimeSourceClass);
        String runtimeLastModified = codeSourceLastModified(runtimeSourceClass);

        return new StoreHomeRunManifestSnapshot(
                runId,
                runIdSource,
                repositoryHead,
                relevantSource,
                property("lavi.storeHome.worktreeDirty"),
                property("lavi.storeHome.dirtyDiffIdentity"),
                shaProperty("lavi.storeHome.dirtyDiffSha256"),
                buildInputManifest,
                property("lavi.storeHome.untrackedInputPathsAndSha256"),
                property("lavi.storeHome.builtJarPath"),
                property("lavi.storeHome.builtJarSize"),
                builtJar,
                property("lavi.storeHome.deployedJarPath"),
                property("lavi.storeHome.deployedJarSize"),
                deployedJar,
                runtimeJar,
                runtimeLocation,
                runtimeLastModified,
                bound(runtimeLocation + "|lastModified=" + runtimeLastModified),
                "PARITY_UNPROVEN",
                parityReason,
                safeSystemProperty("java.vendor"),
                safeSystemProperty("java.version"),
                safeSystemProperty("java.home"),
                modVersion("minecraft"),
                modVersion("fabricloader"),
                modVersion("altoclef"),
                normalize(diagnosticsMode),
                property("lavi.storeHome.worldSnapshotId"),
                property("lavi.storeHome.inventorySnapshotId"),
                Instant.now().toString(),
                "NOT_RECORDED_IN_START_MANIFEST"
        );
    }

    public Object[] fields() {
        return new Object[]{
                "runId", runManifestId,
                "runManifestIdSource", runManifestIdSource,
                "gitHead", gitHead,
                "repositoryHead", gitHead,
                "relevantSourceIdentity", relevantSourceIdentity,
                "relevantSourceTreeIdentity", relevantSourceIdentity,
                "worktreeDirty", worktreeDirty,
                "dirtyDiffIdentity", dirtyDiffIdentity,
                "dirtyDiffSha256", dirtyDiffSha256,
                "buildInputManifestSha256", buildInputManifestSha256,
                "untrackedInputPathsAndSha256", untrackedInputPathsAndSha256,
                "builtJarPath", builtJarPath,
                "builtJarSize", builtJarSize,
                "builtJarSha256", builtJarSha256,
                "deployedJarPath", deployedJarPath,
                "deployedJarSize", deployedJarSize,
                "deployedJarSha256", deployedJarSha256,
                "runtimeJarSha256", runtimeJarSha256,
                "runtimeJarSha256EvidenceSource",
                "EXTERNAL_SYSTEM_PROPERTY_NOT_INDEPENDENTLY_VERIFIED",
                "runtimeCodeSource", runtimeCodeSource,
                "runtimeCodeSourcePath", runtimeCodeSource,
                "runtimeCodeSourceLastModified", runtimeCodeSourceLastModified,
                "runtimeCodeSourceIdentity", runtimeCodeSourceIdentity,
                "artifactParity", artifactParity,
                "artifactParityReason", artifactParityReason,
                "javaVendor", javaVendor,
                "javaVersion", javaVersion,
                "javaRuntimePath", javaRuntimePath,
                "minecraftVersion", minecraftVersion,
                "fabricLoaderVersion", fabricLoaderVersion,
                "chatClefVersion", chatClefVersion,
                "manifestDiagnosticsModeSnapshot", diagnosticsMode,
                "worldSnapshotId", worldSnapshotId,
                "inventorySnapshotId", inventorySnapshotId,
                "runStartedAt", runStartedAt,
                "runEndedAt", runEndedAt,
                "distanceMetricVersion", "block_pos_3d_squared_v1",
                "progressSampleIntervalClientTicks",
                StoreHomeDiagnosticLimits.PROGRESS_SAMPLE_INTERVAL_CLIENT_TICKS,
                "candidateProgressEventCap",
                StoreHomeDiagnosticLimits.CANDIDATE_PROGRESS_EVENT_CAP,
                "operationProgressEventCap",
                StoreHomeDiagnosticLimits.OPERATION_PROGRESS_EVENT_CAP,
                "operationDiagnosticHardCap",
                StoreHomeDiagnosticLimits.OPERATION_HARD_CAP,
                "operationReservedBoundaryCap",
                StoreHomeDiagnosticLimits.OPERATION_RESERVED_BOUNDARY_CAP,
                "sessionDiagnosticHardCap",
                StoreHomeDiagnosticLimits.SESSION_HARD_CAP,
                "sessionReservedBoundaryCap",
                StoreHomeDiagnosticLimits.SESSION_RESERVED_BOUNDARY_CAP,
                "boundedLoggingLimits",
                "sampleClientTicks="
                        + StoreHomeDiagnosticLimits.PROGRESS_SAMPLE_INTERVAL_CLIENT_TICKS
                        + ",candidateProgress="
                        + StoreHomeDiagnosticLimits.CANDIDATE_PROGRESS_EVENT_CAP
                        + ",operationProgress="
                        + StoreHomeDiagnosticLimits.OPERATION_PROGRESS_EVENT_CAP
                        + ",operationHard="
                        + StoreHomeDiagnosticLimits.OPERATION_HARD_CAP
                        + ",sessionHard="
                        + StoreHomeDiagnosticLimits.SESSION_HARD_CAP
        };
    }

    private static String parityReason(
            String repositoryHead,
            String relevantSource,
            String buildInputManifest,
            String builtJar,
            String deployedJar,
            String runtimeJar) {
        if (UNVERIFIED.equals(repositoryHead)
                || UNVERIFIED.equals(relevantSource)
                || UNVERIFIED.equals(buildInputManifest)) {
            return "SOURCE_OR_BUILD_INPUT_IDENTITY_UNVERIFIED";
        }
        if (UNVERIFIED.equals(builtJar)
                || UNVERIFIED.equals(deployedJar)
                || UNVERIFIED.equals(runtimeJar)) {
            return "MISSING_BUILD_DEPLOY_OR_RUNTIME_SHA256";
        }
        if (!builtJar.equalsIgnoreCase(deployedJar)
                || !builtJar.equalsIgnoreCase(runtimeJar)) {
            return "HASH_MISMATCH";
        }
        return "HASH_VALUES_EQUAL_BUT_RUNTIME_CODESOURCE_NOT_INDEPENDENTLY_VERIFIED";
    }

    private static String shaProperty(String name) {
        String value = property(name);
        return value.matches("(?i)[0-9a-f]{64}")
                ? value.toLowerCase(Locale.ROOT)
                : UNVERIFIED;
    }

    private static String property(String name) {
        try {
            String value = System.getProperty(name);
            return value == null || value.isBlank() ? UNVERIFIED : bound(value);
        } catch (RuntimeException | LinkageError ignored) {
            return UNVERIFIED;
        }
    }

    private static String safeSystemProperty(String name) {
        try {
            String value = System.getProperty(name);
            return value == null || value.isBlank() ? "unavailable" : bound(value);
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    private static String modVersion(String modId) {
        try {
            return FabricLoader.getInstance()
                    .getModContainer(modId)
                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                    .filter(version -> !version.isBlank())
                    .map(StoreHomeRunManifestSnapshot::bound)
                    .orElse("unavailable");
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    private static String codeSourceLocation(Class<?> sourceClass) {
        try {
            CodeSource source = sourceClass.getProtectionDomain().getCodeSource();
            return source == null || source.getLocation() == null
                    ? "unavailable"
                    : bound(source.getLocation().toString());
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    private static String codeSourceLastModified(Class<?> sourceClass) {
        try {
            CodeSource source = sourceClass.getProtectionDomain().getCodeSource();
            if (source == null || source.getLocation() == null) {
                return "unavailable";
            }
            URI uri = source.getLocation().toURI();
            Path path = Paths.get(uri);
            return Files.exists(path)
                    ? Files.getLastModifiedTime(path).toString()
                    : "unavailable";
        } catch (Exception | LinkageError ignored) {
            return "unavailable";
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? "unavailable" : bound(value);
    }

    private static String bound(String value) {
        String flattened = value
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ');
        return flattened.length() <= MAX_PROPERTY_LENGTH
                ? flattened
                : flattened.substring(0, MAX_PROPERTY_LENGTH);
    }
}
