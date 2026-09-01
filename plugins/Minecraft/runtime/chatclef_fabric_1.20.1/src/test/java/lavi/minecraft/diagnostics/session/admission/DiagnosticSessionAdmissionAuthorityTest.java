package lavi.minecraft.diagnostics.session.admission;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnosticSessionAdmissionAuthorityTest {
    @Test
    void ordinaryCeilingClaimsAndAdmitsTheCanonicalCapExactlyOnce() {
        DiagnosticSessionAdmissionAuthority authority =
                new DiagnosticSessionAdmissionAuthority("ordinary-ceiling-session");

        for (int index = 0; index < DiagnosticSessionLimits.ORDINARY_CEILING; index++) {
            assertTrue(authority.admit(eligible(DiagnosticEventFamily.ORDINARY_DETAIL)).admitted());
        }

        DiagnosticAdmissionDecision firstRejected =
                authority.admit(eligible(DiagnosticEventFamily.ORDINARY_DETAIL));
        assertFalse(firstRejected.admitted());
        assertEquals(DiagnosticAdmissionDecision.RejectionReason.ORDINARY_CEILING_REACHED,
                firstRejected.rejectionReason());
        assertEquals(DiagnosticCapTrigger.ORDINARY_CEILING_RESERVE_ACTIVE,
                firstRejected.newlyClaimedCapTrigger());
        assertNotNull(firstRejected.canonicalCapToken());
        assertEquals(4_937, firstRejected.snapshot().admittedSlots());
        assertEquals(4_936, firstRejected.snapshot().ordinarySlotsUsed());
        assertEquals(1, firstRejected.snapshot().criticalSlotsUsed());

        for (int index = 0; index < 1_000; index++) {
            DiagnosticAdmissionDecision repeated =
                    authority.admit(eligible(DiagnosticEventFamily.ORDINARY_DETAIL));
            assertFalse(repeated.admitted());
            assertNull(repeated.canonicalCapToken());
            assertEquals(DiagnosticCapTrigger.NONE, repeated.newlyClaimedCapTrigger());
        }

        DiagnosticFamilySnapshot cap = authority.snapshot().family(DiagnosticEventFamily.CANONICAL_CAP);
        assertEquals(1, cap.admittedRequests());
        assertEquals(1, cap.admittedSlots());
        assertEquals(1, cap.emissionPending());
        assertTrue(authority.snapshot().capEventClaimed());

        fillCriticalPools(authority);
        assertEquals(4_999, authority.snapshot().admittedSlots());
        assertTrue(authority.admitFinalSnapshot(true).admitted());
        assertEquals(5_000, authority.snapshot().admittedSlots());
    }

    @Test
    void hardCapFixtureReservesCapAt4999AndFinalSnapshotAt5000() {
        DiagnosticSessionAdmissionAuthority authority =
                new DiagnosticSessionAdmissionAuthority("hard-cap-session");
        fillNormalCapacity(authority);

        DiagnosticSessionSnapshot beforeCap = authority.snapshot();
        assertEquals(4_998, beforeCap.admittedSlots());
        assertFalse(beforeCap.capEventClaimed());
        assertFalse(beforeCap.finalSnapshotAdmitted());

        DiagnosticAdmissionDecision hardRejected =
                authority.admit(eligible(DiagnosticEventFamily.EXCEPTION_COVERAGE));
        assertFalse(hardRejected.admitted());
        assertEquals(DiagnosticAdmissionDecision.RejectionReason.SHARED_HARD_CAP_REACHED,
                hardRejected.rejectionReason());
        assertEquals(DiagnosticCapTrigger.SHARED_HARD_CAP, hardRejected.newlyClaimedCapTrigger());
        assertNotNull(hardRejected.canonicalCapToken());
        assertEquals(4_999, hardRejected.snapshot().admittedSlots());

        DiagnosticAdmissionDecision finalSnapshot = authority.admitFinalSnapshot(true);
        assertTrue(finalSnapshot.admitted());
        assertEquals(DiagnosticEventFamily.FINAL_SNAPSHOT, finalSnapshot.token().family());
        assertEquals(5_000, finalSnapshot.snapshot().admittedSlots());
        assertEquals(64, finalSnapshot.snapshot().criticalSlotsUsed());
        assertEquals(0, finalSnapshot.snapshot().criticalReserveRemaining());

        DiagnosticAdmissionDecision repeatedFinalSnapshot = authority.admitFinalSnapshot(true);
        assertFalse(repeatedFinalSnapshot.admitted());
        assertEquals(DiagnosticAdmissionDecision.RejectionReason.FINAL_SNAPSHOT_ALREADY_ADMITTED,
                repeatedFinalSnapshot.rejectionReason());
    }

    @Test
    void groupPoolsAdmitAllFourOrSuppressAllFourWithoutBorrowing() {
        DiagnosticSessionAdmissionAuthority authority =
                new DiagnosticSessionAdmissionAuthority("group-session");

        for (int index = 0; index < 2; index++) {
            DiagnosticAdmissionDecision routine =
                    authority.admit(eligible(DiagnosticEventFamily.ROUTINE_STORE_TERMINAL));
            assertTrue(routine.admitted());
            assertEquals(4, routine.token().admittedSlots());
        }
        DiagnosticAdmissionDecision routineOverflow =
                authority.admit(eligible(DiagnosticEventFamily.ROUTINE_STORE_TERMINAL));
        assertFalse(routineOverflow.admitted());
        assertEquals(DiagnosticAdmissionDecision.RejectionReason.FAMILY_QUOTA_EXHAUSTED,
                routineOverflow.rejectionReason());
        assertEquals(8, authority.snapshot()
                .family(DiagnosticEventFamily.ROUTINE_STORE_TERMINAL).admittedSlots());

        for (int index = 0; index < 4; index++) {
            assertTrue(authority.admit(eligible(DiagnosticEventFamily.ABNORMAL_STORE_TERMINAL)).admitted());
        }
        DiagnosticAdmissionDecision abnormalOverflow =
                authority.admit(eligible(DiagnosticEventFamily.ABNORMAL_STORE_TERMINAL));
        assertFalse(abnormalOverflow.admitted());
        assertEquals(DiagnosticAdmissionDecision.RejectionReason.FAMILY_QUOTA_EXHAUSTED,
                abnormalOverflow.rejectionReason());
        assertEquals(16, authority.snapshot()
                .family(DiagnosticEventFamily.ABNORMAL_STORE_TERMINAL).admittedSlots());
    }

    @Test
    void modeIneligibleRequestDoesNotMutateAnySessionAccounting() {
        DiagnosticSessionAdmissionAuthority authority =
                new DiagnosticSessionAdmissionAuthority("off-session");
        assertTrue(authority.admit(eligible(DiagnosticEventFamily.ORDINARY_DETAIL)).admitted());
        DiagnosticSessionSnapshot before = authority.snapshot();

        DiagnosticAdmissionDecision ordinaryOff = authority.admit(
                DiagnosticAdmissionRequest.modeIneligible(DiagnosticEventFamily.ORDINARY_DETAIL));
        DiagnosticAdmissionDecision snapshotOff = authority.admitFinalSnapshot(false);

        assertFalse(ordinaryOff.admitted());
        assertFalse(snapshotOff.admitted());
        assertEquals(DiagnosticAdmissionDecision.RejectionReason.MODE_INELIGIBLE,
                ordinaryOff.rejectionReason());
        assertEquals(DiagnosticAdmissionDecision.RejectionReason.MODE_INELIGIBLE,
                snapshotOff.rejectionReason());
        assertEquals(before, authority.snapshot());
    }

    private static DiagnosticAdmissionRequest eligible(DiagnosticEventFamily family) {
        return DiagnosticAdmissionRequest.eligible(family);
    }

    private static void fillNormalCapacity(DiagnosticSessionAdmissionAuthority authority) {
        for (int index = 0; index < DiagnosticSessionLimits.ORDINARY_CEILING; index++) {
            assertTrue(authority.admit(eligible(DiagnosticEventFamily.ORDINARY_DETAIL)).admitted());
        }
        fillCriticalPools(authority);
    }

    private static void fillCriticalPools(DiagnosticSessionAdmissionAuthority authority) {
        admitTimes(authority, DiagnosticEventFamily.ABNORMAL_STORE_TERMINAL, 4);
        admitTimes(authority, DiagnosticEventFamily.ROUTINE_STORE_TERMINAL, 2);
        admitTimes(authority, DiagnosticEventFamily.EXCEPTION_COVERAGE, 16);
        admitTimes(authority, DiagnosticEventFamily.AGGREGATE_CHECKPOINT, 8);
        admitTimes(authority, DiagnosticEventFamily.NON_STORE_TERMINAL, 8);
        admitTimes(authority, DiagnosticEventFamily.SUPPRESSION_CONTROL, 6);
    }

    private static void admitTimes(DiagnosticSessionAdmissionAuthority authority,
                                   DiagnosticEventFamily family,
                                   int count) {
        for (int index = 0; index < count; index++) {
            assertTrue(authority.admit(eligible(family)).admitted(), family.name());
        }
    }
}
