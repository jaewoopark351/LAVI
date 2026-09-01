package lavi.minecraft.diagnostics.session.emission;

import java.io.PrintStream;
import java.util.Objects;

public final class PrintStreamDiagnosticSink implements DiagnosticPhysicalSink {
    private final PrintStream output;

    public PrintStreamDiagnosticSink(PrintStream output) {
        this.output = Objects.requireNonNull(output, "output");
    }

    @Override
    public void emit(String encodedRecord) {
        output.println(Objects.requireNonNull(encodedRecord, "encodedRecord"));
    }
}
