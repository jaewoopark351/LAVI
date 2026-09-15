package lavi.minecraft.fabric.chatclef.bridge.command.result.effect.equip.profile;

import adris.altoclef.commandsystem.Arg;
import adris.altoclef.commandsystem.ArgParser;
import adris.altoclef.commandsystem.CommandException;
import adris.altoclef.commandsystem.ItemList;
import adris.altoclef.util.ItemTarget;
import adris.altoclef.util.helpers.ItemHelper;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Equipment;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

//20260915_kpopmodder: Freeze the native EQUIP grammar and alternative sets without executing a task.
public record EquipEffectProfile(String command, boolean tracked, String reason, List<EquipTarget> targets) {
    public static final int MAX_TARGETS = 32;
    public static final int MAX_MATCHES = 32;
    public EquipEffectProfile { targets = List.copyOf(targets); }

    public static EquipEffectProfile capture(String command) {
        String value = command == null ? "" : command.trim();
        if (value.startsWith("@")) value = value.substring(1);
        if (!value.equals("equip") && !value.startsWith("equip "))
            return new EquipEffectProfile(value, false, "not_applicable", List.of());
        if (value.length() > 8192) return unavailable(value.substring(0, 8192), "profile_limit_exceeded");
        try {
            ArgParser parser = new ArgParser(new Arg<>(ItemList.class, "[equippable_items]"));
            parser.loadArgs(value, true);
            ItemTarget[] targets = null;
            if (parser.getArgUnits().length == 1) {
                targets = switch (parser.getArgUnits()[0].toLowerCase(Locale.ROOT)) {
                    case "leather" -> ItemTarget.of(ItemHelper.LEATHER_ARMORS);
                    case "iron" -> ItemTarget.of(ItemHelper.IRON_ARMORS);
                    case "gold" -> ItemTarget.of(ItemHelper.GOLDEN_ARMORS);
                    case "diamond" -> ItemTarget.of(ItemHelper.DIAMOND_ARMORS);
                    case "netherite" -> ItemTarget.of(ItemHelper.NETHERITE_ARMORS);
                    default -> null;
                };
            }
            if (targets == null) targets = parser.get(ItemList.class).items;
            return fromTargets(value, targets);
        } catch (CommandException | RuntimeException | LinkageError error) {
            return unavailable(value, "target_profile_unavailable");
        }
    }

    public static EquipEffectProfile fromTargets(String command, ItemTarget[] nativeTargets) {
        if (nativeTargets == null || nativeTargets.length == 0 || nativeTargets.length > MAX_TARGETS)
            return unavailable(command, "profile_limit_exceeded");
        List<EquipTarget> targets = new ArrayList<>();
        for (ItemTarget target : nativeTargets) {
            if (target == null || target.getTargetCount() <= 0)
                return unavailable(command, "unsupported_native_quantity");
            Item[] items = target.getMatches();
            if (items.length == 0 || items.length > MAX_MATCHES)
                return unavailable(command, "profile_limit_exceeded");
            // The native shield branch casts the FIRST candidate when vanilla SHIELD occurs anywhere.
            boolean hasShield = java.util.Arrays.asList(items).contains(Items.SHIELD);
            if (hasShield && items[0] != Items.SHIELD)
                return unavailable(command, "unsupported_native_target");
            List<EquipTargetMatch> matches = new ArrayList<>();
            for (Item item : items) {
                if (!(item instanceof Equipment) || (!(item instanceof ArmorItem) && item != Items.SHIELD))
                    return unavailable(command, "unsupported_native_target");
                String id = Registries.ITEM.getId(item).toString();
                String slot = item == Items.SHIELD ? "offhand" : ((ArmorItem) item).getSlotType().getName();
                if (id.length() > 256 || !List.of("head", "chest", "legs", "feet", "offhand").contains(slot))
                    return unavailable(command, "unsupported_native_target");
                EquipTargetMatch match = new EquipTargetMatch(id, slot);
                if (!matches.contains(match)) matches.add(match);
            }
            matches.sort(Comparator.comparing(EquipTargetMatch::itemId).thenComparing(EquipTargetMatch::slot));
            String nativeTarget = target.isCatalogueItem() ? target.getCatalogueName() : matches.get(0).itemId();
            if (nativeTarget == null || nativeTarget.length() > 256) return unavailable(command, "profile_limit_exceeded");
            targets.add(new EquipTarget(targets.size(), nativeTarget, target.getTargetCount(), matches));
        }
        return new EquipEffectProfile(command, true, "available", targets);
    }

    private static EquipEffectProfile unavailable(String command, String reason) {
        return new EquipEffectProfile(command, true, reason, List.of());
    }
}
