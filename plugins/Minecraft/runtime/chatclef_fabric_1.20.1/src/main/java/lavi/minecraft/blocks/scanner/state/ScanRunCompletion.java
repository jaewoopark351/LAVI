//#if MC == 12001
package lavi.minecraft.blocks.scanner.state;

import lavi.minecraft.blocks.scanner.snapshot.ScanResultSnapshot;

//20260913_kpopmodder: Completion remains bound to the originating run, including explicit unsuccessful worker exits.
public record ScanRunCompletion(ScanRunLease run, ScanResultSnapshot result, String failure) { }
//#endif
