package lavi.minecraft.diagnostics.toolselect;

import adris.altoclef.AltoClef;
import adris.altoclef.util.MiningRequirement;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.CursorSlot;
import adris.altoclef.util.slots.Slot;
import baritone.utils.ToolSet;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import net.minecraft.block.BlockState;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ToolItem;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.StringJoiner;

//20260801_kpopmodder: Added tool equip boundary diagnostics without changing ChatClef engine behavior.
public final class ToolEquipDiagnostics {
    private static final int MAX_CANDIDATES = 12;
    private static String lastSelectionFingerprint = "";
    private static String lastEquipRequestFingerprint = "";
    private static String lastEquipResultFingerprint = "";
    private static String lastPostconditionWarningFingerprint = "";

    private ToolEquipDiagnostics() {
    }

    public static long logSelectionDecision(AltoClef mod,
                                            BlockPos targetPosition,
                                            BlockState targetState,
                                            Slot currentSlot,
                                            ItemStack currentStack,
                                            Slot chosenSlot,
                                            ItemStack chosenStack,
                                            String decisionReason) {
        if (!ChatClefDiagnostics.isBoundaryEnabled()) {
            return -1L;
        }

        long equipAttemptId = ChatClefDiagnostics.nextOperationId();
        String fingerprint = selectionFingerprint(targetPosition, targetState, currentSlot, currentStack, chosenSlot, chosenStack, decisionReason);
        if (!fingerprint.equals(lastSelectionFingerprint)) {
            lastSelectionFingerprint = fingerprint;
            ChatClefDiagnostics.logBoundary("TOOL_SELECTION_DECISION", "destroy_block_tool_selection", null,
                    "equipAttemptId", equipAttemptId,
                    "decisionReason", decisionReason,
                    "targetPosition", ChatClefDiagnostics.blockPos(targetPosition),
                    "targetBlockState", ChatClefDiagnostics.safeValue(() -> targetState),
                    "targetBlockId", ChatClefDiagnostics.safeValue(() -> targetState == null ? null : targetState.getBlock()),
                    "targetRequiresTool", ChatClefDiagnostics.safeValue(() -> targetState == null ? null : targetState.isToolRequired()),
                    "minimumMiningRequirement", ChatClefDiagnostics.safeValue(() -> targetState == null ? null : MiningRequirement.getMinimumRequirementForBlock(targetState.getBlock())),
                    "currentSlot", ChatClefDiagnostics.slotSummary(currentSlot),
                    "currentStack", stackDetails(currentStack),
                    "currentSuitable", suitable(currentStack, targetState),
                    "currentSpeed", speed(currentStack, targetState),
                    "chosenSlot", ChatClefDiagnostics.slotSummary(chosenSlot),
                    "chosenStack", stackDetails(chosenStack),
                    "chosenSuitable", suitable(chosenStack, targetState),
                    "chosenSpeed", speed(chosenStack, targetState),
                    "baritonePathing", ChatClefDiagnostics.safeValue(() -> mod.getClientBaritone().getPathingBehavior().isPathing()),
                    "foodChainEating", ChatClefDiagnostics.safeValue(() -> mod.getFoodChain().isTryingToEat()),
                    "cursorStack", ChatClefDiagnostics.safeValue(() -> stackDetails(StorageHelper.getItemStackInSlot(CursorSlot.SLOT))),
                    "candidateSummary", candidateSummary(mod, targetState));
        }

        if ("EQUIP_REQUEST".equals(decisionReason)) {
            logEquipRequest(equipAttemptId, targetPosition, targetState, currentSlot, currentStack, chosenSlot, chosenStack);
        }
        return equipAttemptId;
    }

    public static void logEquipResult(AltoClef mod,
                                      Item requestedItem,
                                      long equipAttemptId,
                                      Slot expectedSourceSlot,
                                      ItemStack expectedSourceStack,
                                      List<Slot> actualMatchingSlots,
                                      boolean inCursor,
                                      int selectedSlotBefore,
                                      int selectedSlotAfter,
                                      ItemStack hotbarSlot1Before,
                                      ItemStack hotbarSlot1After,
                                      ItemStack mainHandBefore,
                                      ItemStack mainHandAfter,
                                      boolean forceEquipReportedSuccess,
                                      String resultReason) {
        if (!ChatClefDiagnostics.isBoundaryEnabled() || equipAttemptId < 0L) {
            return;
        }

        boolean postconditionItemMatched = mainHandAfter != null && mainHandAfter.getItem() == requestedItem;
        boolean postconditionExactStackMatched = expectedSourceStack != null && ItemStack.areEqual(mainHandAfter, expectedSourceStack);
        String resultFingerprint = equipResultFingerprint(requestedItem, expectedSourceSlot, actualMatchingSlots,
                selectedSlotBefore, selectedSlotAfter, mainHandBefore, mainHandAfter, forceEquipReportedSuccess,
                postconditionItemMatched, postconditionExactStackMatched, resultReason);
        if (resultFingerprint.equals(lastEquipResultFingerprint)) {
            return;
        }
        lastEquipResultFingerprint = resultFingerprint;

        ChatClefDiagnostics.logBoundary("TOOL_EQUIP_RESULT", "force_equip_item_result", null,
                "equipAttemptId", equipAttemptId,
                "resultReason", resultReason,
                "requestedItem", requestedItem,
                "expectedSourceSlot", ChatClefDiagnostics.slotSummary(expectedSourceSlot),
                "expectedSourceStack", stackDetails(expectedSourceStack),
                "actualMatchingSlotCount", actualMatchingSlots == null ? 0 : actualMatchingSlots.size(),
                "actualMatchingSlots", slotListSummary(actualMatchingSlots),
                "sourceSlotMatchedExpected", sourceSlotMatchedExpected(expectedSourceSlot, actualMatchingSlots),
                "destinationHotbarSlot", 1,
                "inCursor", inCursor,
                "selectedSlotBefore", selectedSlotBefore,
                "selectedSlotAfter", selectedSlotAfter,
                "hotbarSlot1Before", stackDetails(hotbarSlot1Before),
                "hotbarSlot1After", stackDetails(hotbarSlot1After),
                "mainHandBefore", stackDetails(mainHandBefore),
                "mainHandAfter", stackDetails(mainHandAfter),
                "forceEquipReportedSuccess", forceEquipReportedSuccess,
                "postconditionItemMatched", postconditionItemMatched,
                "postconditionExactStackMatched", postconditionExactStackMatched,
                "baritonePathing", ChatClefDiagnostics.safeValue(() -> mod.getClientBaritone().getPathingBehavior().isPathing()));

        if (forceEquipReportedSuccess && !postconditionItemMatched && !resultFingerprint.equals(lastPostconditionWarningFingerprint)) {
            lastPostconditionWarningFingerprint = resultFingerprint;
            ChatClefDiagnostics.logWarningEvent("TOOL_EQUIP_POSTCONDITION_MISMATCH", "force_equip_reported_success_without_main_hand_match", null,
                    "equipAttemptId", equipAttemptId,
                    "requestedItem", requestedItem,
                    "mainHandAfter", stackDetails(mainHandAfter),
                    "expectedSourceSlot", ChatClefDiagnostics.slotSummary(expectedSourceSlot),
                    "actualMatchingSlots", slotListSummary(actualMatchingSlots));
        }
    }

    private static void logEquipRequest(long equipAttemptId,
                                        BlockPos targetPosition,
                                        BlockState targetState,
                                        Slot currentSlot,
                                        ItemStack currentStack,
                                        Slot chosenSlot,
                                        ItemStack chosenStack) {
        String fingerprint = selectionFingerprint(targetPosition, targetState, currentSlot, currentStack, chosenSlot, chosenStack, "EQUIP_REQUEST");
        if (fingerprint.equals(lastEquipRequestFingerprint)) {
            return;
        }
        lastEquipRequestFingerprint = fingerprint;
        ChatClefDiagnostics.logBoundary("TOOL_EQUIP_REQUEST", "better_tool_selected", null,
                "equipAttemptId", equipAttemptId,
                "targetPosition", ChatClefDiagnostics.blockPos(targetPosition),
                "targetBlockState", ChatClefDiagnostics.safeValue(() -> targetState),
                "currentSlot", ChatClefDiagnostics.slotSummary(currentSlot),
                "currentStack", stackDetails(currentStack),
                "currentSuitable", suitable(currentStack, targetState),
                "currentSpeed", speed(currentStack, targetState),
                "chosenSlot", ChatClefDiagnostics.slotSummary(chosenSlot),
                "chosenStack", stackDetails(chosenStack),
                "chosenSuitable", suitable(chosenStack, targetState),
                "chosenSpeed", speed(chosenStack, targetState));
    }

    private static String candidateSummary(AltoClef mod, BlockState targetState) {
        if (targetState == null) {
            return "unavailable";
        }
        try {
            int candidateCount = 0;
            int emitted = 0;
            StringJoiner candidates = new StringJoiner(",", "[", "]");
            for (Slot slot : Slot.getCurrentScreenSlots()) {
                if (slot == null || !slot.isSlotInPlayerInventory()) {
                    continue;
                }
                ItemStack stack = StorageHelper.getItemStackInSlot(slot);
                Item item = stack.getItem();
                if (!(item instanceof ToolItem) && item != Items.SHEARS) {
                    continue;
                }
                candidateCount++;
                if (emitted < MAX_CANDIDATES) {
                    String saveDecision = saveDecision(mod, stack, targetState);
                    candidates.add(ChatClefDiagnostics.slotSummary(slot)
                            + "#stack=" + stackDetails(stack)
                            + "#suitable=" + suitable(stack, targetState)
                            + "#defaultStackSuitable=" + defaultStackSuitable(stack, targetState)
                            + "#shouldSave=" + shouldSaveFromDecision(saveDecision)
                            + "#selectionOutcome=" + selectionOutcome(stack, targetState, saveDecision)
                            + "#saveDecision=" + saveDecision
                            + "#speed=" + speed(stack, targetState));
                    emitted++;
                }
            }
            return "candidateCount=" + candidateCount
                    + "#truncatedCandidateCount=" + Math.max(0, candidateCount - emitted)
                    + "#candidates=" + candidates;
        } catch (RuntimeException | LinkageError ignored) {
            return "unavailable";
        }
    }

    private static String stackDetails(ItemStack stack) {
        if (stack == null) {
            return "none";
        }
        return ChatClefDiagnostics.itemStackSummary(stack)
                + "#damage=" + ChatClefDiagnostics.safeValue(stack::getDamage)
                + "#maxDamage=" + ChatClefDiagnostics.safeValue(stack::getMaxDamage)
                + "#stackString=" + ChatClefDiagnostics.safeValue(stack::toString);
    }

    private static String suitable(ItemStack stack, BlockState targetState) {
        return ChatClefDiagnostics.safeValue(() -> stack != null && targetState != null && stack.isSuitableFor(targetState));
    }

    private static String defaultStackSuitable(ItemStack stack, BlockState targetState) {
        return ChatClefDiagnostics.safeValue(() -> stack != null && targetState != null && stack.getItem().getDefaultStack().isSuitableFor(targetState));
    }

    private static String speed(ItemStack stack, BlockState targetState) {
        return ChatClefDiagnostics.safeValue(() -> stack == null || targetState == null ? "unavailable" : ToolSet.calculateSpeedVsBlock(stack, targetState));
    }

    private static String shouldSaveFromDecision(String saveDecision) {
        if (saveDecision == null || !saveDecision.startsWith("result=")) {
            return "unavailable";
        }
        int delimiter = saveDecision.indexOf('#');
        return delimiter < 0 ? saveDecision.substring("result=".length()) : saveDecision.substring("result=".length(), delimiter);
    }

    private static String selectionOutcome(ItemStack stack, BlockState targetState, String saveDecision) {
        return ChatClefDiagnostics.safeValue(() -> {
            if (stack == null) {
                return "NO_STACK";
            }
            if (targetState == null) {
                return "NO_TARGET_STATE";
            }
            Item item = stack.getItem();
            if (item == Items.SHEARS) {
                return ItemHelper.areShearsEffective(targetState.getBlock()) ? "SHEARS_EFFECTIVE" : "SHEARS_NOT_EFFECTIVE";
            }
            if (!(item instanceof ToolItem)) {
                return "NOT_TOOL";
            }
            if (!item.getDefaultStack().isSuitableFor(targetState)) {
                return "SKIP_NOT_SUITABLE";
            }
            if (saveDecision != null && saveDecision.startsWith("result=true")) {
                return "SKIP_SHOULD_SAVE";
            }
            return "ELIGIBLE";
        });
    }

    private static String saveDecision(AltoClef mod, ItemStack stack, BlockState targetState) {
        return ChatClefDiagnostics.safeValue(() -> {
            if (stack == null) {
                return "result=unavailable#reason=NO_STACK";
            }
            if (targetState == null) {
                return "result=unavailable#reason=NO_TARGET_STATE";
            }

            Item item = stack.getItem();
            if (item != Items.IRON_PICKAXE) {
                return "result=false#reason=NOT_IRON_PICKAXE";
            }

            boolean hasDiamondPickaxe = mod.getItemStorage().hasItem(Items.DIAMOND_PICKAXE);
            if (hasDiamondPickaxe) {
                return "result=false#reason=HAS_DIAMOND_PICKAXE";
            }

            Block block = targetState.getBlock();
            boolean diamondRelatedBlock = block.equals(Blocks.DIAMOND_BLOCK)
                    || block.equals(Blocks.DIAMOND_ORE)
                    || block.equals(Blocks.DEEPSLATE_DIAMOND_ORE);
            int damage = stack.getDamage();
            int maxDamage = stack.getMaxDamage();
            boolean criticalDurability = damage + 8 > maxDamage;
            boolean lowDurability = damage + 30 > maxDamage;
            MiningRequirement minimumRequirement = MiningRequirement.getMinimumRequirementForBlock(block);
            boolean shouldSave = StorageHelper.shouldSaveStack(mod, block, stack);

            String reason = "NOT_LOW_DURABILITY";
            if (criticalDurability) {
                reason = diamondRelatedBlock ? "CRITICAL_DURABILITY_DIAMOND_RELATED" : "CRITICAL_DURABILITY_NON_DIAMOND";
            } else if (lowDurability) {
                reason = minimumRequirement.equals(MiningRequirement.IRON)
                        ? "LOW_DURABILITY_IRON_REQUIRED"
                        : "LOW_DURABILITY_BLOCK_NOT_IRON_REQUIRED";
            }

            return "result=" + shouldSave
                    + "#reason=" + reason
                    + "#hasDiamondPickaxe=" + hasDiamondPickaxe
                    + "#damage=" + damage
                    + "#maxDamage=" + maxDamage
                    + "#damagePlus8=" + (damage + 8)
                    + "#damagePlus30=" + (damage + 30)
                    + "#diamondRelatedBlock=" + diamondRelatedBlock
                    + "#minimumMiningRequirement=" + minimumRequirement;
        });
    }

    private static String slotListSummary(List<Slot> slots) {
        if (slots == null) {
            return "none";
        }
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        int emitted = 0;
        for (Slot slot : slots) {
            if (emitted >= MAX_CANDIDATES) {
                break;
            }
            joiner.add(ChatClefDiagnostics.slotSummary(slot));
            emitted++;
        }
        if (slots.size() > emitted) {
            joiner.add("truncated=" + (slots.size() - emitted));
        }
        return joiner.toString();
    }

    private static boolean sourceSlotMatchedExpected(Slot expectedSourceSlot, List<Slot> actualMatchingSlots) {
        return expectedSourceSlot != null && actualMatchingSlots != null && actualMatchingSlots.stream().anyMatch(expectedSourceSlot::equals);
    }

    private static String selectionFingerprint(BlockPos targetPosition,
                                               BlockState targetState,
                                               Slot currentSlot,
                                               ItemStack currentStack,
                                               Slot chosenSlot,
                                               ItemStack chosenStack,
                                               String decisionReason) {
        return ChatClefDiagnostics.blockPos(targetPosition)
                + "|" + ChatClefDiagnostics.safeValue(() -> targetState == null ? null : targetState.getBlock())
                + "|" + ChatClefDiagnostics.slotSummary(currentSlot)
                + "|" + itemFingerprint(currentStack)
                + "|" + ChatClefDiagnostics.slotSummary(chosenSlot)
                + "|" + itemFingerprint(chosenStack)
                + "|" + decisionReason;
    }

    private static String equipResultFingerprint(Item requestedItem,
                                                 Slot expectedSourceSlot,
                                                 List<Slot> actualMatchingSlots,
                                                 int selectedSlotBefore,
                                                 int selectedSlotAfter,
                                                 ItemStack mainHandBefore,
                                                 ItemStack mainHandAfter,
                                                 boolean reportedSuccess,
                                                 boolean postconditionItemMatched,
                                                 boolean postconditionExactStackMatched,
                                                 String resultReason) {
        return requestedItem
                + "|" + ChatClefDiagnostics.slotSummary(expectedSourceSlot)
                + "|" + slotListSummary(actualMatchingSlots)
                + "|" + selectedSlotBefore
                + "|" + selectedSlotAfter
                + "|" + itemFingerprint(mainHandBefore)
                + "|" + itemFingerprint(mainHandAfter)
                + "|" + reportedSuccess
                + "|" + postconditionItemMatched
                + "|" + postconditionExactStackMatched
                + "|" + resultReason;
    }

    private static String itemFingerprint(ItemStack stack) {
        if (stack == null) {
            return "none";
        }
        return ChatClefDiagnostics.safeValue(() -> stack.getItem())
                + "#count=" + ChatClefDiagnostics.safeValue(stack::getCount)
                + "#damage=" + ChatClefDiagnostics.safeValue(stack::getDamage)
                + "#maxDamage=" + ChatClefDiagnostics.safeValue(stack::getMaxDamage)
                + "#stackString=" + ChatClefDiagnostics.safeValue(stack::toString)
                + "#empty=" + ChatClefDiagnostics.safeValue(stack::isEmpty);
    }
}
