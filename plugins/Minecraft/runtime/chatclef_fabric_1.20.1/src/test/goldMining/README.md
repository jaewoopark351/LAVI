<!-- 20260914_kpopmodder: Keep focused gold behavior verification reproducible without cross-version test compilation. -->
# Gold mining focused verification

Run from the Fabric ChatClef runtime root with the repository's existing JDK and
offline dependency cache settings:

```powershell
.\gradlew.bat :1.20.1:goldFocusedTests --offline --no-daemon --stacktrace --init-script .gradle/codex-build-init.gradle --init-script src/test/goldMining/gold-tests.init.gradle
```

The init script registers isolated compilation and execution tasks for the real
1.20.1 production classes. It compiles the shared tests explicitly, without
changing production source sets or importing the preprocessor's cross-version
test task dependencies. Other version nodes may preprocess sources as part of
the existing conversion graph; they must not compile or package a runtime.

Coverage includes exact tool swaps and confirmation, hotbar coexistence and
finite failure, block scan ownership and protection publication, command root
termination and event ordering, existing diagnostic lifecycle/admission limits,
and automatic storage pressure boundaries. The selected source patterns are in
`gold-tests.init.gradle`. The launcher explicitly selects each test class and
fails for an empty selection, failed/aborted/skipped tests, or failed containers.

Outputs stay under `versions/1.20.1/build/gold-focused/` and
`versions/1.20.1/build/gold-test-temp/`. No Minecraft process, network connection,
world, external instance, dependency installation, or global settings change is
part of this task. Existing dependencies and versions remain unchanged.

This test task is separate from a clean remapped artifact build and isolated
Mixin application. None of these replaces live gameplay verification. Record
the exact executed tasks, test counts, input and artifact hashes, and live
runtime status in the [implementation record](../../../../../docs/gold-mining-debugging-2026-09-13/implementation.md).
