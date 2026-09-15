package lavi.minecraft.task.container.deposit.auto.policy.classification.diagnostics;

import lavi.minecraft.diagnostics.ChatClefDiagnostics;
import lavi.minecraft.task.container.deposit.auto.DepositAllInventoryPressureSnapshot;
import lavi.minecraft.task.container.deposit.auto.policy.AutoDepositPlan;
import lavi.minecraft.task.container.deposit.auto.policy.classification.support.AutoDepositClassificationFixture;
import lavi.minecraft.task.container.deposit.auto.policy.diagnostics.AutoDepositPolicyDiagnostics;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static lavi.minecraft.diagnostics.container.store.deposit.support.StoreDepositSliceATestSupport.captureOutput;
import static lavi.minecraft.task.container.deposit.auto.policy.classification.support.AutoDepositClassificationFixture.targetCounts;
import static org.junit.jupiter.api.Assertions.*;

//20260916_kpopmodder: Capture real bounded policy output; selection facts remain distinct from runtime transfers.
class AutoDepositResourcePolicyOutputTest {
    @BeforeAll
    static void bootstrap() { AutoDepositClassificationFixture.bootstrap(); }
    @BeforeEach
    void startIsolatedSession() {
        ChatClefDiagnostics.setBoundaryEnabled(false);
        ChatClefDiagnostics.resetDiagnosticSessionForTests();
    }
    @AfterEach
    void disableDiagnostics() { ChatClefDiagnostics.setBoundaryEnabled(false); }

    @Test
    void actualPolicyOutputCorrelatesEveryChangedGradeWithoutAlteringSelectionOrRepeatingDetails() {
        var fixture = new AutoDepositClassificationFixture();
        var stacks = fixture.read(new ItemStack(Items.TUFF, 26), new ItemStack(Items.GRAVEL, 45),
                new ItemStack(Items.ANDESITE, 64), new ItemStack(Items.GRANITE, 63),
                new ItemStack(Items.DIORITE, 62), new ItemStack(Items.SUGAR_CANE, 2),
                new ItemStack(Items.LAPIS_LAZULI, 14));
        var pressure = new DepositAllInventoryPressureSnapshot(35, 36);
        ChatClefDiagnostics.setBoundaryEnabled(true);
        assertTrue(ChatClefDiagnostics.isBoundaryEnabled());
        AutoDepositPlan plan = fixture.plan(stacks, 7, Map.of(), true, 260916301L);
        String output = captureOutput(() -> log(plan, pressure));
        List<String> snapshotRows = rows(output, "AUTO_DEPOSIT_POLICY_SNAPSHOT");
        List<String> itemRows = rows(output, "AUTO_DEPOSIT_POLICY_ITEM_DECISION");
        assertEquals(1, snapshotRows.size(), output);
        assertEquals(7, itemRows.size(), output);
        assertEquals(7, rows(output, "AUTO_DEPOSIT_POLICY_STACK_FACT").size(), output);
        String snapshot = snapshotRows.get(0);
        assertEquals("GENERAL_AND_TRUSTED_ONLY", field(snapshot, "destinationDecision"));
        assertEquals("260916301", field(snapshot, "policyContextEpoch"));
        assertNotEquals("UNAVAILABLE", field(snapshot, "autoPlanId"));
        Map<String, Integer> general = Map.of("minecraft:tuff", 26, "minecraft:gravel", 45,
                "minecraft:andesite", 64, "minecraft:granite", 63, "minecraft:diorite", 62,
                "minecraft:sugar_cane", 2);
        for (Map.Entry<String, Integer> entry : general.entrySet()) {
            String row = itemRow(itemRows, entry.getKey());
            assertEquals("GENERAL_SURPLUS", field(row, "classification"), row);
            assertEquals("DEPOSITABLE_SURPLUS", field(row, "disposition"), row);
            assertEquals("GENERAL_CONTAINER", field(row, "destinationClass"), row);
            assertEquals("[" + entry.getValue() + "]", field(row, "selectedGeneralTargetCounts"), row);
            assertEquals("[]", field(row, "selectedTrustedTargetCounts"), row);
        }
        String lapis = itemRow(itemRows, "minecraft:lapis_lazuli");
        assertEquals("CONDITIONAL_VALUABLE", field(lapis, "classification"), lapis);
        assertEquals("CONDITIONAL_VALUABLE", field(lapis, "disposition"), lapis);
        assertEquals("TRUSTED_ONLY", field(lapis, "destinationClass"), lapis);
        assertEquals("[]", field(lapis, "selectedGeneralTargetCounts"), lapis);
        assertEquals("[14]", field(lapis, "selectedTrustedTargetCounts"), lapis);
        for (String row : itemRows) {
            assertEquals("SELECTED", field(row, "decision"), row);
            assertEquals("0", field(row, "protectedCount"), row);
            assertEquals("0", field(row, "workingSetCount"), row);
            assertEquals("0", field(row, "categoryReserveCount"), row);
            assertEquals("none", field(row, "behavior_effect"), row);
            for (String key : List.of("autoPlanId", "inventorySnapshotId", "policyContextEpoch")) {
                assertEquals(field(snapshot, key), field(row, key), row);
            }
        }
        String repeated = captureOutput(() -> log(plan, pressure));
        assertTrue(rows(repeated, "AUTO_DEPOSIT_POLICY_SNAPSHOT").isEmpty(), repeated);
        assertTrue(rows(repeated, "AUTO_DEPOSIT_POLICY_ITEM_DECISION").isEmpty(), repeated);
        assertTrue(rows(repeated, "AUTO_DEPOSIT_POLICY_STACK_FACT").isEmpty(), repeated);

        ChatClefDiagnostics.setBoundaryEnabled(false);
        AutoDepositPlan offPlan = fixture.plan(stacks, 7, Map.of(), true, 260916301L);
        assertEquals("", captureOutput(() -> log(offPlan, pressure)));
        assertEquals(targetCounts(plan.generalTargets()), targetCounts(offPlan.generalTargets()));
        assertEquals(targetCounts(plan.trustedTargets()), targetCounts(offPlan.trustedTargets()));
        assertEquals(plan.protectedCounts(), offPlan.protectedCounts());
        assertTrue(offPlan.policyItemDecisions().isEmpty());
    }

    private static void log(AutoDepositPlan plan, DepositAllInventoryPressureSnapshot pressure) {
        AutoDepositPolicyDiagnostics.log(plan, pressure, "READY", "classification_resource_fixture", null);
    }

    private static List<String> rows(String output, String event) {
        return output.lines().filter(line -> Arrays.asList(line.split(" ")).contains("event=" + event)).toList();
    }

    private static String itemRow(List<String> rows, String itemId) {
        List<String> matching = rows.stream().filter(row -> field(row, "itemId").equals(itemId)).toList();
        assertEquals(1, matching.size(), "Expected one physical item-decision row for " + itemId);
        return matching.get(0);
    }

    private static String field(String row, String key) {
        return Arrays.stream(row.split(" ")).filter(token -> token.startsWith(key + "="))
                .findFirst().orElseThrow(() -> new AssertionError("Missing field " + key + ": " + row))
                .substring(key.length() + 1);
    }
}
