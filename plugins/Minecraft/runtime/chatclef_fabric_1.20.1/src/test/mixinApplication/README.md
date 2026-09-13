# Isolated Builder diagnostic Mixin application check

This separate test source directory is compiled explicitly by the repository-local
PowerShell launcher at `scripts/verify_mixin_application.ps1` in this directory.
It uses the installed runtime's Sponge Mixin 0.17.3+mixin.0.8.7 and MixinExtras 0.5.4,
without changing the main project's dependencies. It is intentionally separate from
the ordinary JUnit source set, which compiles against the project's older Mixin API.

From `C:\Vtuber_Souorce_Code\LAVI`, run:

```powershell
. ([scriptblock]::Create((Get-Content -LiteralPath 'plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/test/mixinApplication/scripts/verify_mixin_application.ps1' -Raw -Encoding UTF8))) -MainInput 'C:/Vtuber_Souorce_Code/LAVI/plugins/Minecraft/runtime/chatclef_fabric_1.20.1/versions/1.20.1/build/libs/chatclef-1.20.1-0.18.23.jar' -ArtifactJar
```

Omit `MainInput` and `ArtifactJar` to inspect the canonical freshly compiled named
1.20.1 classes. `ArtifactJar` instead reads the selected built JAR, extracts its
nested Baritone JAR into the repository's isolated output directory, and uses the
cached matching intermediary Minecraft classes. No installed artifact is modified.
The launcher needs existing local cached dependencies; it installs or downloads nothing.

For the original broken artifact only, add `-PreviousArtifact` and select that JAR.
This is a negative control: success means Sponge reproduced the exact missing
`BuilderProcess.baritone` Shadow failure. An unrelated failure or an unexpected pass
fails that control. It does not mark the broken artifact usable.

The harness invokes the actual Sponge transformer and MixinExtras injectors for
eight targets, checks actual application configuration registration and changed
bytecode with diagnostic handlers, and records hashes. The custom service reads
Minecraft and Baritone class resources without loading or initializing those game
classes. The JVM does not launch Fabric, Minecraft, a world, or networking.

This proves focused transformation against the selected class inputs. It does not
prove live game startup, compatibility with every other mod's transformations,
gameplay callbacks, or physical game-log emission. Live Minecraft remains `NOT_RUN`.
Launcher output and logs go below the ignored repository-local isolation directory;
the Java host, smoke runner, configuration and service registrations here are kept
as reviewable test sources.
