package adris.altoclef.lavibridge.commands;

//20260725_kpopmodder: Added this builder to isolate get-then-equip command text formatting.

public class LaviGetAndEquipCommandBuilder {

    public LaviCommandSpec build(LaviGetItemRequest request) {
        return new LaviCommandSpec(
                "get-and-equip",
                "get " + request.getItem() + " " + request.getCount(),
                request.getOriginalRequest()
        );
    }
}
