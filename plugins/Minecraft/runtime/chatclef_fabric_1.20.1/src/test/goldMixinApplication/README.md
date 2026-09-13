<!-- 20260914_kpopmodder: Document the opt-in World protection transformation check separately from the eight Builder targets. -->
# Isolated gold protection Mixin application

This opt-in smoke transforms the real Minecraft 1.20.1 `World` bytecode with the
selected application's `WorldBlockModifiedMixin`. It reuses the existing isolated
Sponge service and read-only dependency caches. The eight Builder/Baritone targets
and their default launcher behavior remain unchanged.

From `C:\Vtuber_Souorce_Code\LAVI`:

```powershell
. ([scriptblock]::Create((Get-Content -LiteralPath 'plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/test/goldMixinApplication/scripts/verify_gold_protection_mixin.ps1' -Raw -Encoding UTF8))) -CompileOnly
```

Omit `-CompileOnly` to transform the freshly compiled named 1.20.1 input. To use a
particular built intermediary artifact, supply both `-ArtifactJar` and
`-MainInput '<absolute built 1.20.1 JAR path>'`. The existing launcher extracts its
nested Baritone into a new repository-local test output and uses cached matching
intermediary Minecraft classes; no installed artifact or cache is changed.

The runner derives its single-target configuration from the selected input's
actual `altoclef.mixins.json`, retaining its settings and packaged refmap while
narrowing only the Mixin lists. It writes that derived configuration into the new
test output. For intermediary artifacts, Sponge reads the actual configured
refmap using its declared intermediary context and resolves the unchanged named
injection selector; the harness does not rewrite production annotations. The
selected config and refmap hashes and resolved selector appear in `smoke.log`.

The runner checks actual application registration, the changed target bytecode,
one target-method call to the merged handler, and one handler call to the new
`AltoClef.onUserProtectionBlockChanged` method with the correct namespace-specific
descriptor. It also verifies that the called AltoClef method exists in the same
selected input, and prints hashes for the target, transformed class, production
Mixin, AltoClef class and application configuration.

Outputs are below `test/test_Isolation/minecraft/gold_mixin_application/output/`.
`-PrepareOnly` reads inputs without compiling. `-CompileOnly` compiles the test
host and records `TRANSFORMATION=NOT_RUN`. Neither is a transformation pass.

A successful transformation proves this focused injection against the chosen
input. It does not execute Minecraft, initialize game classes, exercise live block
callbacks, or prove compatibility with all other mod transformations. Live
Minecraft and live callback acceptance remain `NOT_RUN`.
