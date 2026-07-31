package lavi.minecraft.integration.carryon.snapshot;

//20260731_kpopmodder: Group ChatClef task state fields for Carry On diagnostics.
public record CarryOnTaskSnapshot(String topLevelTask,
                                  String childTask,
                                  String currentChain,
                                  String taskChain,
                                  String taskRunnerActive,
                                  String userTaskChainActive,
                                  String paused,
                                  String chatClefEnabled,
                                  String playerMode) {
}
