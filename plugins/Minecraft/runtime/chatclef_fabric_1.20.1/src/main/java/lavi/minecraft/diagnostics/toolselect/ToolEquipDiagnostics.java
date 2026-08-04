package lavi.minecraft.diagnostics.toolselect;

import adris.altoclef.AltoClef;
import adris.altoclef.util.helpers.ItemHelper;
import adris.altoclef.util.helpers.StorageHelper;
import adris.altoclef.util.slots.CursorSlot;
import adris.altoclef.util.slots.Slot;
import baritone.utils.ToolSet;
import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.diagnostics.toolselect.support.DiagnosticDeduplicator;
import lavi.minecraft.diagnostics.toolselect.support.ToolCandidateDiagnosticFormatter;
import lavi.minecraft.diagnostics.toolselect.support.ToolDiagnosticFormatter;
import lavi.minecraft.diagnostics.toolselect.support.ToolSavePolicyDiagnostics;
import lavi.minecraft.diagnostics.toolselect.support.ToolTargetDiagnosticFields;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ToolItem;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.StringJoiner;

//20260801_kpopmodder: Added tool equip boundary diagnostics without changing ChatClef engine behavior.
public final class ToolEquipDiagnostics {
    private static final DiagnosticDeduplicator DEDUPLICATOR = new DiagnosticDeduplicator();

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
        if (DEDUPLICATOR.shouldEmit("selection", fingerprint)) {
            ChatClefDiagnostics.logBoundary("TOOL_SELECTION_DECISION", "destroy_block_tool_selection", null,
                    "equipAttemptId", equipAttemptId,
                    "decisionReason", decisionReason,
                    "targetPosition", ChatClefDiagnostics.blockPos(targetPosition),
                    "targetBlockState", ToolTargetDiagnosticFields.blockState(targetState),
                    "targetBlockId", ToolTargetDiagnosticFields.blockId(targetState),
                    "targetRequiresTool", ToolTargetDiagnosticFields.requiresTool(targetState),
                    "minimumMiningRequirement", ToolTargetDiagnosticFields.minimumMiningRequirement(targetState),
                    "currentSlot", ChatClefDiagnostics.slotSummary(currentSlot),
                    "currentStack", ToolDiagnosticFormatter.basicStackDetails(currentStack),
                    "currentSuitable", suitable(currentStack, targetState),
                    "currentSpeed", speed(currentStack, targetState),
                    "chosenSlot", ChatClefDiagnostics.slotSummary(chosenSlot),
                    "chosenStack", ToolDiagnosticFormatter.basicStackDetails(chosenStack),
                    "chosenSuitable", suitable(chosenStack, targetState),
                    "chosenSpeed", speed(chosenStack, targetState),
                    "baritonePathing", ChatClefDiagnostics.safeValue(() -> mod.getClientBaritone().getPathingBehavior().isPathing()),
                    "foodChainEating", ChatClefDiagnostics.safeValue(() -> mod.getFoodChain().isTryingToEat()),
                    "cursorStack", ChatClefDiagnostics.safeValue(() -> ToolDiagnosticFormatter.basicStackDetails(StorageHelper.getItemStackInSlot(CursorSlot.SLOT))),
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
        if (!DEDUPLICATOR.shouldEmit("equip_result", resultFingerprint)) {
            return;
        }

        ChatClefDiagnostics.logBoundary("TOOL_EQUIP_RESULT", "force_equip_item_result", null,
                "equipAttemptId", equipAttemptId,
                "resultReason", resultReason,
                "requestedItem", requestedItem,
                "expectedSourceSlot", ChatClefDiagnostics.slotSummary(expectedSourceSlot),
                "expectedSourceStack", ToolDiagnosticFormatter.basicStackDetails(expectedSourceStack),
                "actualMatchingSlotCount", actualMatchingSlots == null ? 0 : actualMatchingSlots.size(),
                "actualMatchingSlots", ToolDiagnosticFormatter.slotListSummary(actualMatchingSlots),
                "sourceSlotMatchedExpected", sourceSlotMatchedExpected(expectedSourceSlot, actualMatchingSlots),
                "destinationHotbarSlot", 1,
                "inCursor", inCursor,
                "selectedSlotBefore", selectedSlotBefore,
                "selectedSlotAfter", selectedSlotAfter,
                "hotbarSlot1Before", ToolDiagnosticFormatter.basicStackDetails(hotbarSlot1Before),
                "hotbarSlot1After", ToolDiagnosticFormatter.basicStackDetails(hotbarSlot1After),
                "mainHandBefore", ToolDiagnosticFormatter.basicStackDetails(mainHandBefore),
                "mainHandAfter", ToolDiagnosticFormatter.basicStackDetails(mainHandAfter),
                "forceEquipReportedSuccess", forceEquipReportedSuccess,
                "postconditionItemMatched", postconditionItemMatched,
                "postconditionExactStackMatched", postconditionExactStackMatched,
                "baritonePathing", ChatClefDiagnostics.safeValue(() -> mod.getClientBaritone().getPathingBehavior().isPathing()));

        if (forceEquipReportedSuccess && !postconditionItemMatched && DEDUPLICATOR.shouldEmit("postcondition_warning", resultFingerprint)) {
            ChatClefDiagnostics.logWarningEvent("TOOL_EQUIP_POSTCONDITION_MISMATCH", "force_equip_reported_success_without_main_hand_match", null,
                    "equipAttemptId", equipAttemptId,
                    "requestedItem", requestedItem,
                    "mainHandAfter", ToolDiagnosticFormatter.basicStackDetails(mainHandAfter),
                    "expectedSourceSlot", ChatClefDiagnostics.slotSummary(expectedSourceSlot),
                    "actualMatchingSlots", ToolDiagnosticFormatter.slotListSummary(actualMatchingSlots));
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
        if (!DEDUPLICATOR.shouldEmit("equip_request", fingerprint)) {
            return;
        }
        ChatClefDiagnostics.logBoundary("TOOL_EQUIP_REQUEST", "better_tool_selected", null,
                "equipAttemptId", equipAttemptId,
                "targetPosition", ChatClefDiagnostics.blockPos(targetPosition),
                "targetBlockState", ToolTargetDiagnosticFields.blockState(targetState),
                "currentSlot", ChatClefDiagnostics.slotSummary(currentSlot),
                "currentStack", ToolDiagnosticFormatter.basicStackDetails(currentStack),
                "currentSuitable", suitable(currentStack, targetState),
                "currentSpeed", speed(currentStack, targetState),
                "chosenSlot", ChatClefDiagnostics.slotSummary(chosenSlot),
                "chosenStack", ToolDiagnosticFormatter.basicStackDetails(chosenStack),
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
                if (emitted < ToolDiagnosticFormatter.MAX_CANDIDATES) {
                    String saveDecision = ToolSavePolicyDiagnostics.computedDecision(mod, stack, targetState);
                    candidates.add(ToolCandidateDiagnosticFormatter.equipCandidate(
                            slot,
                            ToolDiagnosticFormatter.basicStackDetails(stack),
                            suitable(stack, targetState),
                            defaultStackSuitable(stack, targetState),
                            ToolSavePolicyDiagnostics.shouldSaveFromDecision(saveDecision),
                            selectionOutcome(stack, targetState, saveDecision),
                            saveDecision,
                            speed(stack, targetState)));
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

    private static String suitable(ItemStack stack, BlockState targetState) {
        return ChatClefDiagnostics.safeValue(() -> stack != null && targetState != null && stack.isSuitableFor(targetState));
    }

    private static String defaultStackSuitable(ItemStack stack, BlockState targetState) {
        return ChatClefDiagnostics.safeValue(() -> stack != null && targetState != null && stack.getItem().getDefaultStack().isSuitableFor(targetState));
    }

    private static String speed(ItemStack stack, BlockState targetState) {
        return ChatClefDiagnostics.safeValue(() -> stack == null || targetState == null ? "unavailable" : ToolSet.calculateSpeedVsBlock(stack, targetState));
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
                + "|" + ToolDiagnosticFormatter.equipItemFingerprint(currentStack)
                + "|" + ChatClefDiagnostics.slotSummary(chosenSlot)
                + "|" + ToolDiagnosticFormatter.equipItemFingerprint(chosenStack)
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
                + "|" + ToolDiagnosticFormatter.slotListSummary(actualMatchingSlots)
                + "|" + selectedSlotBefore
                + "|" + selectedSlotAfter
                + "|" + ToolDiagnosticFormatter.equipItemFingerprint(mainHandBefore)
                + "|" + ToolDiagnosticFormatter.equipItemFingerprint(mainHandAfter)
                + "|" + reportedSuccess
                + "|" + postconditionItemMatched
                + "|" + postconditionExactStackMatched
                + "|" + resultReason;
    }
}
