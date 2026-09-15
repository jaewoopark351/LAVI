#20260913_kpopmodder: Apply real installed Sponge/MixinExtras to named or packaged 1.20.1 bytecode in an isolated JVM.
param([switch] $PrepareOnly, [string] $MainInput = '', [switch] $PreviousArtifact, [switch] $ArtifactJar,
      [switch] $GoldProtection, [switch] $CompileOnly, [switch] $SlotClick)
$ErrorActionPreference = 'Stop'
$taskRepository = 'C:\Vtuber_Souorce_Code\LAVI'
if ((Get-Location).Path -ne $taskRepository -or (git rev-parse --show-toplevel).Trim() -ne $taskRepository.Replace('\', '/')) {
    throw 'Repository/cwd mismatch.'
}
$taskRuntime = Join-Path $taskRepository 'plugins/Minecraft/runtime/chatclef_fabric_1.20.1'
$taskHarness = Join-Path $taskRepository 'test/test_Isolation/minecraft/mixin_application'
$taskSourceRoot = Join-Path $taskRuntime 'src/test/mixinApplication'
#20260914_kpopmodder: The gold target is an explicit opt-in; default eight-target Builder checks are unchanged.
if ($GoldProtection -and $PreviousArtifact) { throw 'GoldProtection does not support the unrelated Builder negative control.' }
if ($SlotClick -and ($GoldProtection -or $PreviousArtifact)) { throw 'SlotClick is a separate explicit target.' }
$taskGoldSourceRoot = Join-Path $taskRuntime 'src/test/goldMixinApplication'
if ($GoldProtection) { $taskHarness = Join-Path $taskRepository 'test/test_Isolation/minecraft/gold_mixin_application' }
if ($SlotClick) { $taskHarness = Join-Path $taskRepository 'test/test_Isolation/minecraft/slot_click_mixin_application' }
if ([string]::IsNullOrWhiteSpace($MainInput)) { $MainInput = Join-Path $taskRuntime 'versions/1.20.1/build/classes/java/main' }
$taskMain = [IO.Path]::GetFullPath($MainInput)
if (-not (Test-Path -LiteralPath $taskMain)) { throw "Missing main bytecode input: $taskMain" }
$taskGameCache = 'C:/Users/jaewo/.gradle/caches/fabric-loom'
$taskNamedGame = Join-Path $taskGameCache 'minecraftMaven/net/minecraft/minecraft-merged/1.20.1-net.fabricmc.yarn.1_20_1.1.20.1+build.10-v2/minecraft-merged-1.20.1-net.fabricmc.yarn.1_20_1.1.20.1+build.10-v2.jar'
$taskNamedMods = Join-Path $taskRuntime '.gradle/loom-cache/remapped_mods/net_fabricmc_yarn_1_20_1_1_20_1_build_10_v2'
$taskBase = @($taskMain, $taskNamedGame) + @(Get-ChildItem -LiteralPath $taskNamedMods -Recurse -File -Filter '*.jar' |
    Where-Object { $_.Name -notmatch 'mixinextras|-(sources|javadoc)\.jar$' } | ForEach-Object FullName)
if ((Get-Item -LiteralPath $taskMain).PSIsContainer) {
    $taskBase += Join-Path $taskRuntime 'versions/1.20.1/build/resources/main'
}
$taskManifestGame = Get-Content -LiteralPath (Join-Path $taskGameCache '1.20.1/minecraft-info.json') -Raw -Encoding UTF8 | ConvertFrom-Json
foreach ($taskLibrary in $taskManifestGame.libraries) {
    $taskCoordinates = $taskLibrary.name -split ':'
    if ($taskCoordinates.Count -ne 3) { continue }
    $taskLibraryDirectory = Join-Path 'C:/Users/jaewo/.gradle/caches/modules-2/files-2.1' ($taskCoordinates -join '/')
    $taskLibraryName = $taskCoordinates[1] + '-' + $taskCoordinates[2] + '.jar'
    $taskCached = @(Get-ChildItem -LiteralPath $taskLibraryDirectory -Recurse -File -Filter $taskLibraryName -ErrorAction SilentlyContinue)
    if ($taskCached.Count -gt 0) { $taskBase += $taskCached[0].FullName }
}
$taskInstalledLibraries = 'C:/Users/jaewo/curseforge/minecraft/Install/libraries'
$taskMixin = Join-Path $taskInstalledLibraries 'net/fabricmc/sponge-mixin/0.17.3+mixin.0.8.7/sponge-mixin-0.17.3+mixin.0.8.7.jar'
$taskLoader = Join-Path $taskInstalledLibraries 'net/fabricmc/fabric-loader/0.19.3/fabric-loader-0.19.3.jar'
$taskAsm = @('asm', 'asm-tree', 'asm-util', 'asm-analysis', 'asm-commons') | ForEach-Object {
    Join-Path $taskInstalledLibraries "org/ow2/asm/$_/9.10.1/$_-9.10.1.jar"
}
foreach ($taskInput in @($taskMixin, $taskLoader) + $taskAsm) {
    if (-not (Test-Path -LiteralPath $taskInput -PathType Leaf)) { throw "Missing cached runtime input: $taskInput" }
}
if ($PrepareOnly) { Write-Output 'MIXIN_SMOKE_PREPARED; VERSION=1.20.1; NO_EXECUTION'; return }
$taskOutput = Join-Path $taskHarness ('output/' + [guid]::NewGuid().ToString('N'))
if (-not [IO.Path]::GetFullPath($taskOutput).StartsWith($taskRepository + '\test\', [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Output boundary mismatch.'
}
[IO.Directory]::CreateDirectory($taskOutput) | Out-Null
$taskTemp = Join-Path $taskOutput 'tmp'
[IO.Directory]::CreateDirectory($taskTemp) | Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem
$taskExtras = Join-Path $taskOutput 'mixinextras-fabric-0.5.4.jar'
$taskArchive = [IO.Compression.ZipFile]::OpenRead($taskLoader)
try {
    $taskEntry = $taskArchive.GetEntry('META-INF/jars/mixinextras-fabric-0.5.4.jar')
    if ($null -eq $taskEntry) { throw 'Installed Fabric loader does not contain expected MixinExtras 0.5.4.' }
    [IO.Compression.ZipFileExtensions]::ExtractToFile($taskEntry, $taskExtras, $false)
} finally { $taskArchive.Dispose() }
if ($ArtifactJar) {
    if ([IO.Path]::GetExtension($taskMain) -ne '.jar') { throw 'ArtifactJar requires a concrete JAR MainInput.' }
    $taskIntermediaryGame = Join-Path $taskGameCache 'minecraftMaven/net/minecraft/minecraft-merged-intermediary/1.20.1-net.fabricmc.yarn.1_20_1.1.20.1+build.10-v2/minecraft-merged-intermediary-1.20.1-net.fabricmc.yarn.1_20_1.1.20.1+build.10-v2.jar'
    if (-not (Test-Path -LiteralPath $taskIntermediaryGame)) { throw 'Missing cached intermediary 1.20.1 game.' }
    $taskPackagedBaritone = Join-Path $taskOutput 'baritone-unoptimized-fabric-1.20.1.jar'
    $taskAppArchive = [IO.Compression.ZipFile]::OpenRead($taskMain)
    try {
        $taskBaritoneEntry = $taskAppArchive.GetEntry('META-INF/jars/baritone-unoptimized-fabric-1.20.1.jar')
        if ($null -eq $taskBaritoneEntry) { throw 'Main JAR does not package the expected 1.20.1 Baritone.' }
        [IO.Compression.ZipFileExtensions]::ExtractToFile($taskBaritoneEntry, $taskPackagedBaritone, $false)
    } finally { $taskAppArchive.Dispose() }
    $taskBase = @($taskMain, $taskPackagedBaritone, $taskIntermediaryGame) + @($taskBase | Where-Object {
        $_ -ne $taskMain -and $_ -ne $taskNamedGame -and -not $_.StartsWith($taskNamedMods, [StringComparison]::OrdinalIgnoreCase)
    })
}
$taskClasspath = (@($taskOutput, (Join-Path $taskSourceRoot 'resources'), $taskMixin, $taskExtras) + $taskAsm + $taskBase) -join ';'
$taskSources = @(Get-ChildItem -LiteralPath (Join-Path $taskSourceRoot 'java') -Recurse -File -Filter '*.java' | ForEach-Object FullName)
if ($GoldProtection) {
    $taskSources += @(Get-ChildItem -LiteralPath (Join-Path $taskGoldSourceRoot 'java') -Recurse -File -Filter '*.java' | ForEach-Object FullName)
}
$taskCompile = @('--release', '17', '-encoding', 'UTF-8', '-proc:none', '-implicit:none', '-classpath', $taskClasspath, '-d', $taskOutput) + $taskSources
$taskSmokeMain = 'lavi.minecraft.testsupport.mixin.BuilderMixinApplicationSmoke'
if ($GoldProtection) { $taskSmokeMain = 'lavi.minecraft.testsupport.mixin.gold.GoldProtectionMixinApplicationSmoke' }
if ($SlotClick) { $taskSmokeMain = 'lavi.minecraft.testsupport.mixin.slotclick.SlotClickMixinApplicationSmoke' }
$taskRun = @(('-Djava.io.tmpdir=' + $taskTemp), '-Dmixin.debug.export=false', '-Dmixin.dumpTargetOnFailure=false',
    '-Dmixin.debug.verify=true', '-classpath', $taskClasspath, $taskSmokeMain)
if ($GoldProtection) { $taskRun = @('-Dlavi.gold.smoke.output=' + $taskOutput) + $taskRun }
if ($SlotClick) { $taskRun = @('-Dlavi.slotclick.smoke.output=' + $taskOutput) + $taskRun }
if ($PreviousArtifact) { $taskRun += '--previous-artifact' }
if ($ArtifactJar) { $taskRun += '--intermediary-artifact' }
foreach ($taskArgumentSet in @(@{Name='javac.args';Values=$taskCompile}, @{Name='java.args';Values=$taskRun})) {
    $taskLines = $taskArgumentSet.Values | ForEach-Object { '"' + ([string]$_).Replace('\', '/').Replace('"', '\"') + '"' }
    [IO.File]::WriteAllLines((Join-Path $taskOutput $taskArgumentSet.Name), $taskLines, [Text.UTF8Encoding]::new($false))
}
$taskManifest = @('SCOPE=REAL_MIXIN_APPLICATION; NAMESPACE=NAMED_1.20.1; MINECRAFT_LAUNCH=NOT_RUN', "HEAD=$(git rev-parse HEAD)")
if ($GoldProtection) {
    if ($ArtifactJar) { $taskManifest[0] = 'SCOPE=REAL_MIXIN_APPLICATION; NAMESPACE=INTERMEDIARY_1.20.1; MINECRAFT_LAUNCH=NOT_RUN' }
    $taskManifest += 'OPT_IN_TARGET=WORLD_BLOCK_PROTECTION; DEFAULT_BUILDER_TARGETS=UNCHANGED'
    $taskManifest += 'TEST_CONFIG=DERIVED_FROM_ACTUAL_APPLICATION_CONFIG; REF_MAP=ACTUAL_SELECTED_INPUT; CONFIG_HASH=TRANSFORMATION_LOG'
}
if ($SlotClick) {
    if ($ArtifactJar) { $taskManifest[0] = 'SCOPE=REAL_MIXIN_APPLICATION; NAMESPACE=INTERMEDIARY_1.20.1; MINECRAFT_LAUNCH=NOT_RUN' }
    $taskManifest += 'OPT_IN_TARGET=SLOT_CLICK; TEST_CONFIG=DERIVED_FROM_ACTUAL_APPLICATION_CONFIG; REF_MAP=ACTUAL_SELECTED_INPUT'
}
$taskManifest += (@($taskMixin, $taskLoader, $taskExtras) + $taskAsm + $taskSources) | ForEach-Object {
    "SHA256=$((Get-FileHash -LiteralPath $_ -Algorithm SHA256).Hash) PATH=$_"
}
$taskManifest += 'CLASSPATH=' + $taskClasspath
$taskManifest += "MAIN_INPUT=$taskMain"
if ((Get-Item -LiteralPath $taskMain).PSIsContainer) {
    if ($SlotClick) {
        $taskManifest += @('adris/altoclef/mixins/SlotClickMixin.class', 'lavi/minecraft/inventory/slotclick/SlotClickEventBridge.class') |
            ForEach-Object { $taskClass = Join-Path $taskMain $_; "MAIN_CLASS_SHA256=$((Get-FileHash -LiteralPath $taskClass -Algorithm SHA256).Hash) PATH=$taskClass" }
    } elseif ($GoldProtection) {
        $taskManifest += @('adris/altoclef/mixins/WorldBlockModifiedMixin.class', 'adris/altoclef/AltoClef.class') |
            ForEach-Object { $taskClass = Join-Path $taskMain $_; "MAIN_CLASS_SHA256=$((Get-FileHash -LiteralPath $taskClass -Algorithm SHA256).Hash) PATH=$taskClass" }
    } else {
        $taskManifest += Get-ChildItem -LiteralPath (Join-Path $taskMain 'adris/altoclef/mixins/diagnostics') -File -Filter '*.class' |
            ForEach-Object { "MAIN_CLASS_SHA256=$((Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash) PATH=$($_.FullName)" }
    }
} else { $taskManifest += "MAIN_JAR_SHA256=$((Get-FileHash -LiteralPath $taskMain -Algorithm SHA256).Hash)" }
if ($ArtifactJar) {
    $taskManifest += "PACKAGED_BARITONE_SHA256=$((Get-FileHash -LiteralPath $taskPackagedBaritone -Algorithm SHA256).Hash)"
    if ($GoldProtection -or $SlotClick) {
        $taskManifest += 'NAMESPACE=INTERMEDIARY_1.20.1; TARGET_SOURCE=ACTUAL_CACHED_INTERMEDIARY_MINECRAFT; MIXIN_SOURCE=ACTUAL_MAIN_JAR'
    } else {
        $taskManifest += 'NAMESPACE=INTERMEDIARY_1.20.1; TARGET_SOURCE=ACTUAL_MAIN_JAR_NESTED_BARITONE'
    }
}
$taskManifest | Set-Content -LiteralPath (Join-Path $taskOutput 'inputs.txt') -Encoding UTF8
Write-Output "MIXIN_SMOKE_OUTPUT=$taskOutput"
$taskJdkBin = Split-Path (Get-Command javac.exe).Source -Parent
$ErrorActionPreference = 'Continue'
& (Join-Path $taskJdkBin 'javac.exe') ('@' + (Join-Path $taskOutput 'javac.args')) 2>&1 |
    Tee-Object -FilePath (Join-Path $taskOutput 'compile.log') | Out-Null
$taskCompileExit = $LASTEXITCODE
$ErrorActionPreference = 'Stop'
if ($taskCompileExit -ne 0) {
    Get-Content -LiteralPath (Join-Path $taskOutput 'compile.log') -Tail 40
    throw "Mixin smoke compilation failed: $taskCompileExit"
}
if ($CompileOnly) {
    $taskCompilationResult = "MIXIN_SMOKE_COMPILE_RESULT=PASS; GOLD_PROTECTION=$GoldProtection; TRANSFORMATION=NOT_RUN; LIVE_MINECRAFT=NOT_RUN"
    $taskCompilationResult | Set-Content -LiteralPath (Join-Path $taskOutput 'compile-result.txt') -Encoding UTF8
    Write-Output $taskCompilationResult
    return
}
$ErrorActionPreference = 'Continue'
& (Join-Path $taskJdkBin 'java.exe') ('@' + (Join-Path $taskOutput 'java.args')) 2>&1 |
    Tee-Object -FilePath (Join-Path $taskOutput 'smoke.log') | Out-Null
$taskRunExit = $LASTEXITCODE
$ErrorActionPreference = 'Stop'
Get-Content -LiteralPath (Join-Path $taskOutput 'smoke.log') -Tail 45
if ($taskRunExit -ne 0) { throw "Mixin smoke transformation failed: $taskRunExit" }
