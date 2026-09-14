# Isolated FIND input lease Mixin application check

This explicit test mode reuses the existing bytecode-only Sponge host and local
cached dependencies. It changes no build dependency and launches no Minecraft,
Fabric session, world, process controller or network connection. The separate
source directory is outside Gradle's ordinary source sets.

From the repository root, run the existing launcher with `-FindInputLease`:

```powershell
. ([scriptblock]::Create((Get-Content -LiteralPath 'plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/test/mixinApplication/scripts/verify_mixin_application.ps1' -Raw -Encoding UTF8))) -FindInputLease
```

For the fresh packaged artifact, also pass `-ArtifactJar -MainInput` with its exact
repository-local JAR path. The launcher reads that JAR, its nested Baritone and
matching cached intermediary Minecraft resources. It modifies no installed JAR.

The test selects the actual registered `ownership.InputOverrideLeaseMixin` from
the application's configuration and applies the real Sponge transformer to
`baritone.utils.InputOverrideHandler`. It requires the lease interface and four
explicit lease methods, and proves both native setter/clear instruction sequences
remain identical after the observer prefix, with one observer call and no new
branch, cancellation or exception interception. Input, output, Mixin, ledger and
configuration hashes are physically recorded.

Fresh configuration, compilation arguments, classes and logs stay under
`test/test_Isolation/minecraft/find_input_lease_mixin_application/output/<UUID>`.
Transformation proves focused injection against the selected inputs. Live mod
compatibility, actual client-thread leases and gameplay remain `NOT_RUN`.
