package lavi.minecraft.diagnostics.session.lifecycle.composition;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

//20260913_kpopmodder: Isolate JVM class initialization order; never let earlier tests satisfy this graph.
final class ProductionObserverCompositionTest {
    @Test void actualProductionGraphSurvivesDifferentFirstOwnersAndToolEquipLast() throws Exception {
        for (String scenario : List.of("tool-last", "reverse", "bridge-first", "tool-overflow")) {
            Path output = Files.createTempFile(Path.of(System.getProperty("java.io.tmpdir")), "observer-graph-", ".log");
            Process process = new ProcessBuilder(
                    Path.of(System.getProperty("java.home"), "bin", "java.exe").toString(),
                    "-Djava.io.tmpdir=" + System.getProperty("java.io.tmpdir"),
                    "-Duser.dir=" + System.getProperty("user.dir"),
                    "-cp", System.getProperty("java.class.path"), ProductionObserverCompositionProbe.class.getName(), scenario)
                    .redirectErrorStream(true).redirectOutput(output.toFile()).start();
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) process.destroyForcibly();
            assertTrue(finished, scenario + " timed out; " + output);
            String transcript = Files.readString(output);
            assertEquals(0, process.exitValue(), scenario + "\n" + transcript);
            assertTrue(transcript.contains("PRODUCTION_OBSERVERS_PASS scenario=" + scenario), transcript);
        }
    }
}
