package adris.altoclef.lavibridge.commands;

//20260725_kpopmodder: Added this builder to isolate AltoClef goto command text formatting.

public class LaviGotoCommandBuilder {

    public LaviCommandSpec build(LaviGotoRequest request) {
        return new LaviCommandSpec("goto", "goto " + request.getTarget(), request.getOriginalRequest());
    }
}
