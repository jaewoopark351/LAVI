package adris.altoclef.lavibridge.commands;

//20260725_kpopmodder: Added this builder to isolate AltoClef get command text formatting.

public class LaviGetItemCommandBuilder {

    public LaviCommandSpec build(LaviGetItemRequest request) {
        return new LaviCommandSpec(
                "get-item",
                "get " + request.getItem() + " " + request.getCount(),
                request.getOriginalRequest()
        );
    }
}
