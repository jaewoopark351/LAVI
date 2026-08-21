<!-- 20260821_kpopmodder: Added the pre-edit evidence index for the sync-finish unchanged-idle-root Java bridge fix. -->
<!-- 20260821_openai: Reviewed package v2; verified raw-v2 manifest integrity and source-preservation mechanics, and recorded the narrow evidence supplement still required before edit-only approval. -->

# ChatClef Deposit Sync-Finish Idle Root Pre-Edit Evidence - 2026-08-21

Date: 2026-08-21 KST

Status:

```text
evidence status: REVIEWED_PARTIAL_PASS_PENDING_SUPPLEMENT
review revision: reviewed-v2-package-review
review input SHA-256: 6DB56F7E24AF339578FA0CE68B7D02ADA5BB50CD457B86926C04B24FB0516A1F
package ZIP SHA-256: EFD168B537C9476F85AA3556013D234F2214E3B604CD184087021D2730461951
raw-v2 manifest integrity: PASS_11_OF_11
source/test/document target baseline mechanics: PASS
cross-document provenance: PARTIAL_PASS
source code change in this pass: none
test source change in this pass: none
Gradle/build/test execution in this pass: none
runtime/JAR/Minecraft execution in this pass: none
implementation approval granted by this document: no
next approval unit: evidence addendum, then edit-only implementation if explicitly approved
```

This document indexes the pre-edit evidence package before the edit-only
implementation pass. The supplied raw-v2 directory and manifest are now
independently reviewable. Their integrity and source-preservation mechanics pass,
but Approval unit A remains incomplete until the exact companion investigation
bytes and an exact-baseline repository/registered-command audit are added through
a narrow manifest addendum.

## Independent Package Integrity Review

```text
raw-v2 manifest SHA-256:
  5E20F0827916C059549E7E6178EC1BE01FEAA1287D127E2111F185C461785C20
manifest rows: 11
raw-v2 regular files: 11
missing manifest targets: 0
unlisted raw-v2 files: 0
duplicate manifest paths: 0
byte-size mismatches: 0
SHA-256 mismatches: 0
```

The manifest is authoritative for the 11 original raw-v2 files. The superseded
first evidence directory is represented only through the captured Git status and
excluded-file evidence; it is not merged into raw-v2.

Additional internal consistency checks passed:

```text
git status entries: 638
intentionally excluded entries: 626
incident-package entries left after exact status-line subtraction: 12
planned existing file rows: 22
already-dirty planned tracked files: 8
real diff --git sections in the path-scoped patch: 8
missing or extra dirty-file diff sections: 0
planned new file rows recorded absent: 15
```

The package also fixes the future test infrastructure decision at
`org.junit.jupiter:junit-jupiter:5.10.2` without Mockito and requires
`FabricChatClefBridgeComponents.java` to remain byte-identical in the first
edit-only pass.

## Repository Context

```text
working directory: C:\Vtuber_Souorce_Code\LAVI
git toplevel: C:/Vtuber_Souorce_Code/LAVI
branch: minecraft-plugin-fix/alto-clef-infinite-loop
HEAD: 332bc53bb86afa2efa0f973f845a55f870e2486f
worktree state: dirty
```

## Document Alignment

The repo implementation plan has been aligned byte-for-byte with the reviewed-v5
final attachment:

```text
pre-alignment repo implementation-plan SHA-256:
  B1B7EBD3B09901B25B1084B333042FE07EFEF1A0A7919D1917E211C6CFAE3C1E

reviewed-v5 final SHA-256:
  7AB95468FB3BEAFC1F522A782A8281D428DCE0DBCE1910F81939E5E24602D9D5

current repo implementation-plan SHA-256:
  7AB95468FB3BEAFC1F522A782A8281D428DCE0DBCE1910F81939E5E24602D9D5
```

The reviewed-v3 evidence-boundary pre-change report is also present in the repo:

```text
pre-change report reviewed-v2 input SHA-256:
  982C86B8E4C107CE131AF938984C1E5BF05D17DA686008717EB13A2ED9E0C88F

current repo pre-change report reviewed-v3 evidence-boundary SHA-256:
  2D9D4D3E5BCFC404A36CA92D9FE7D2440F0288635A7BCB6BF4850DB40819F288

investigation document SHA-256:
  49628CC0AF62602E8062E1AE75D6E49603F78CAADEC53BA99F8A4B4FC0BF66B2
```


### Cross-Document Provenance Limitation

The exact investigation file whose reported SHA-256 is
`49628CC0AF62602E8062E1AE75D6E49603F78CAADEC53BA99F8A4B4FC0BF66B2`
is not included in the package, so its wire/late-event alignment cannot be
independently rechecked here.

The raw `planned-existing-file-sha256-before.txt` also captures earlier
meta-document revisions:

```text
pre-change report in raw table: 982C86B8...E0C88F
current packaged pre-change report: 2D9D4D3E...19F288
pre-edit summary in raw table: F0138B3A...1C58D
current packaged pre-edit summary review input: 6DB56F7E...16A1F
```

This sequencing does not invalidate the Java, Gradle, Java-test target, or four
behavior-document baselines. It requires a small provenance addendum before
Approval unit B.

## Evidence Files

Final raw evidence is stored under:

```text
plugins/Minecraft/docs/evidence/chatclef-deposit-sync-finish-idle-root-pre-edit-2026-08-21-v2/
```

The raw-v2 manifest is stored beside the folder:

```text
plugins/Minecraft/docs/evidence/chatclef-deposit-sync-finish-idle-root-pre-edit-2026-08-21-v2-manifest.tsv
raw-v2 manifest SHA-256:
  5E20F0827916C059549E7E6178EC1BE01FEAA1287D127E2111F185C461785C20
manifest columns:
  relative_path, byte_size, sha256
```

The raw full-status output is intentionally not inlined here because the
worktree contains a large untracked `.codex-build` cache. The complete output is
preserved in the evidence directory.

The earlier directory without the `-v2` suffix is superseded. It was retained
because deleting generated files was not separately approved; it is captured in
the v2 full status and must not be treated as implementation evidence.

```text
git-status-short-untracked-all.txt line count:
  638

git-diff-name-status.txt line count:
  69

dirty-files-intentionally-excluded-from-implementation.txt line count:
  626
```

Evidence file SHA-256 values:

```text
D366F462F986728BD9F953AF9EEE49774E51F5A3C716D792E363451994DFF02E  bridge-components-decision.txt
6C01470520634EC7545AEAF1B6275B1E5770AF430ADA7316B8356D9795595B9E  dirty-files-intentionally-excluded-from-implementation.txt
B9936B3DD94DB8E8DCFDBA6DD95C8AE187001EA19D91D4C6720B5E763812C77E  git-diff-name-status.txt
4D95775A38E48C7E208143C39B162149350EB7D157529CA72EDE9195D8DD934D  git-status-short-untracked-all.txt
DF3D467753636DA0C822881EB261863B3870E6B1DDF2A1F88180D827AF7E0302  implementation-plan-alignment.txt
3E5A56042604035434D7F006371DB405CD8233566870E8515EC985BCC5AB97A0  junit-dependency-decision.txt
998F542AC16F07262A10A335FDFD485EDAFF746B451A67EFFABFAEA90A771963  planned-existing-file-path-scoped-diffs.patch
099975A397DB23E30CAB02B6AECC5B66E4F2929D9BB726A97CE4A2D5272D78AF  planned-existing-file-sha256-before.txt
F69494DCFB88CF245663BDA03D0A3F8409FF07F6BA58B10840BA65F2A9471AFF  planned-new-files-baseline.txt
```

## Planned Existing File Baseline

The full table is preserved in:

```text
planned-existing-file-sha256-before.txt
planned-existing-file-path-scoped-diffs.patch
```

Notable incident-relevant baseline hashes:

```text
FabricChatClefCommandExecution.java:
  D8570E0E9FB9D05909A76DC40A35D51DF74F2D54E008AF989B11710E696BAEE9

FabricChatClefCommandExecutionState.java:
  D0627EE1B99C64AF10E4E71EC3EB4FEF92879A60BED32E3EDF54C832F997C69F

FabricChatClefCommandResultFactory.java:
  25D3D78E033730CCAD487FFB9E448D65E51FBB2C72FB4605D441D7098B0CAE5B

FabricChatClefCommandLifecycleCoordinator.java:
  654E7A6C0D384DA2335899C3A87280962BCFAF343C991319F2242FFE83033728

FabricChatClefTaskOwnershipSnapshot.java:
  BF94E2BAE5C9E24D4A50C0B562DB714C12A8A6A4793A4B4300F8FA733D22F0F1

FabricChatClefBridgeComponents.java:
  169A6F0B31DDE43DFF29B85D9639D514A7E2E997716F7F978FCBA69AD525D396
```

## Planned New File Baseline

The full planned-new-file baseline is preserved in:

```text
planned-new-files-baseline.txt
```

All proposed new Java helper and JUnit test files listed there are absent at the
time of this evidence capture. If any of those paths appear before the approved
edit-only implementation pass, the baseline must be refreshed before editing.

## JUnit Decision

The exact future test dependency is pinned as:

```text
coordinate: org.junit.jupiter:junit-jupiter:5.10.2
Gradle dependencies hunk: testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
Gradle test task hunk: tasks.withType(Test).configureEach { useJUnitPlatform() }
additional mocking framework: not planned
```

This document does not approve dependency resolution or test execution.

## Bridge Components Decision

`FabricChatClefBridgeComponents.java` is dirty in the current worktree. The
first edit-only implementation pass must keep it byte-identical.

If implementation proves production object-graph wiring is unavoidable, Codex
must stop and report that conflict instead of changing the file silently.

## Dirty Files Excluded From Implementation

The full excluded dirty list is preserved in:

```text
dirty-files-intentionally-excluded-from-implementation.txt
```

This list includes unrelated Python, test, generated resource, documentation,
and `.codex-build` cache paths present in the current worktree snapshot. The
edit-only implementation must not revert, overwrite, clean, or reinterpret those
files.

## Remaining Evidence Supplement Before Approval Unit B

Approval unit A still needs:

```text
1. exact investigation document bytes matching 49628CC0...66B2
2. repository-and-registered-command-audit.txt (or equivalent) with:
   capture time/timezone
   exact repo root, branch, HEAD, and worktree command outputs
   AltoClefCommands.java and OverlayCommandRegistrar.java hashes
   exact registered-command audit output
   finish-then-async-root result
   SetGammaCommand no-callback exclusion
3. a manifest/provenance addendum listing the supplemental files and current
   plan/report/summary/investigation hashes
```

Keep the 11-file raw-v2 directory immutable. Add the supplement beside it rather
than rewriting historical evidence silently. No new causal-analysis or
implementation-plan revision is needed.

## Approval Boundary

This package does not yet grant implementation approval. Current verdict:

```text
raw-v2 integrity: PASS
source/test/document target baseline preservation: PASS
implementation-plan alignment: PASS
cross-document provenance: PARTIAL_PASS
Approval unit A: PARTIAL_PASS_PENDING_SUPPLEMENT
Approval unit B: HOLD
```

After the narrow supplement is attached and verified, the next approval may be
limited to:

```text
edit-only LAVI-owned Java bridge changes
exact pinned JUnit infrastructure and scoped Java test-source creation
the four related protocol/lifecycle/diagnostic documentation updates
no test execution
no Gradle execution or build
no JAR copy
no Minecraft launch
no adris/** changes
no Python reconciliation mutation
no retired-root ledger
no commit or push
```

If the actual worktree changes before implementation starts, refresh only the
affected pre-edit hashes/path-scoped diffs or explicitly supersede them. Do not
silently reuse stale baseline evidence.
