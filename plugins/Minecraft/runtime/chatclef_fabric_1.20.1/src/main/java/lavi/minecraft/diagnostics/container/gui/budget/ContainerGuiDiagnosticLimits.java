package lavi.minecraft.diagnostics.container.gui.budget;

//20260904_kpopmodder: Keep local GUI-flow shaping subordinate to the shared diagnostics session cap.
public final class ContainerGuiDiagnosticLimits {
    public static final int DETAIL_LIMIT_PER_ACTIVATION = 256;
    public static final int MAX_SEMANTIC_BUCKETS = 32;
    public static final long REPEAT_SUMMARY_INTERVAL_TICKS = 200L;
    public static final int MAX_EVENT_UTF8_BYTES = 8192;

    private ContainerGuiDiagnosticLimits() {
    }
}
