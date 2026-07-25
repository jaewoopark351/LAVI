package adris.altoclef.lavibridge.commands;

//20260725_kpopmodder: Added this builder to isolate LAVI equip action text formatting.

public class LaviEquipCommandBuilder {

    public LaviCommandSpec build(LaviEquipRequest request) {
        return new LaviCommandSpec(
                "equip",
                "equip " + request.getItem(),
                request.getOriginalRequest()
        );
    }
}
