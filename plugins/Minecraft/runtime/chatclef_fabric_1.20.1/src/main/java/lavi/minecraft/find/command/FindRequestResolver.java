//#if MC == 12001
//$$ package lavi.minecraft.find.command;

//$$ import java.text.Normalizer;
//$$ import adris.altoclef.Debug;
//$$ import adris.altoclef.commandsystem.CommandException;
//$$ import lavi.minecraft.find.catalog.FindCatalogRuntime;
//$$ import lavi.minecraft.find.catalog.FindCatalogSnapshot;
//$$ import lavi.minecraft.find.catalog.FindEntityEligibility;
//$$ import lavi.minecraft.find.model.FindRequest;
//$$ import net.minecraft.registry.Registries;
//$$ import net.minecraft.util.Identifier;

//$$ //20260914_kpopmodder: Validate the FIND domain before any task or movement is submitted.
//$$ public final class FindRequestResolver {
//$$     private FindRequestResolver() { }
//$$     public static FindRequest resolve(String kind, String target, String mode) throws CommandException {
//$$         validateSyntax(kind, target, mode);
//$$         if (kind.equals("player")) return new FindRequest(kind, target, mode, "", 0);
//$$         Identifier id = new Identifier(target);
//$$         boolean registered = switch (kind) {
//$$             case "entity" -> Registries.ENTITY_TYPE.containsId(id);
//$$             case "block" -> Registries.BLOCK.containsId(id);
//$$             default -> Registries.ITEM.containsId(id);
//$$         };
//$$         if (!registered) throw new CommandException("FIND INVALID_TARGET: unregistered_id");
//$$         if (kind.equals("item") && target.equals("minecraft:air")) throw new CommandException("FIND INVALID_TARGET: empty_item_type");
//$$         FindCatalogSnapshot catalog = FindCatalogRuntime.instance().currentSnapshot();
//$$         if (catalog == null || !catalog.complete()) {
//$$             if (kind.equals("entity") && "non_mob".equals(FindEntityEligibility.captureDeclaredVanillaTypes().get(Registries.ENTITY_TYPE.get(id)))) {
//$$                 throw new CommandException("FIND INVALID_TARGET: non_mob_entity_type");
//$$             }
//$$             try { Debug.logWarning("LAVI FIND native canonical request operation=UNBOUND reason=catalog_not_ready kind=" + kind); }
//$$             catch (RuntimeException | LinkageError ignored) { }
//$$             return new FindRequest(kind, target, mode, "", 0);
//$$         }
//$$         var record = catalog.record(kind, target);
//$$         if (record == null) throw new CommandException("FIND INVALID_TARGET: catalog_target_unavailable");
//$$         if (kind.equals("entity") && record.eligibility().equals("non_mob")) throw new CommandException("FIND INVALID_TARGET: non_mob_entity_type");
//$$         return new FindRequest(kind, target, mode, catalog.digest(), catalog.resourceGeneration());
//$$     }
//$$     public static void validateSyntax(String kind, String target, String mode) throws CommandException {
//$$         if (kind == null || target == null || mode == null || (!kind.equals("entity") && !kind.equals("player") && !kind.equals("block") && !kind.equals("item"))) {
//$$             throw new CommandException("FIND INVALID_TARGET: invalid_kind");
//$$         }
//$$         if (!mode.equals("report") && !mode.equals("approach")) throw new CommandException("FIND INVALID_TARGET: invalid_mode");
//$$         if (kind.equals("item") && mode.equals("approach")) throw new CommandException("FIND INVALID_TARGET: item_approach_unsupported");
//$$         if (kind.equals("player")) {
//$$             if (!target.matches("[A-Za-z0-9_]{3,16}")) throw new CommandException("FIND INVALID_TARGET: invalid_literal_player_name");
//$$         } else if (target.length() > 128 || !Normalizer.isNormalized(target, Normalizer.Form.NFC)
//$$                 || !target.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
//$$             throw new CommandException("FIND INVALID_TARGET: invalid_namespaced_id");
//$$         }
//$$     }
//$$ }

//#endif
