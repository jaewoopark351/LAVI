#20260807_kpopmodder: Added report-only final-jar inspection for ChatClef Mixin compatibility.
[CmdletBinding()]
param(
    [string]$RuntimeRoot = "plugins/Minecraft/runtime/chatclef_fabric_1.20.1",
    [string]$JarPath,
    [string]$OutputDirectory = "artifacts/chatclef-mixin-advisory",
    [string]$MinecraftLogPath,
    [string]$CrashReportDirectory,
    [switch]$FailOnFindings
)

Set-StrictMode -Version 2.0
$ErrorActionPreference = "Stop"

$findings = New-Object System.Collections.Generic.List[object]
$nestedBaritoneEvidence = New-Object System.Collections.Generic.List[object]
$evidence = [ordered]@{
    repositoryRoot = $null
    runtimeRoot = $RuntimeRoot
    buildExitCode = $env:CHATCLEF_BUILD_EXIT_CODE
    jarPath = $null
    jarSha256 = $null
    jarLength = $null
    jarLastWriteTime = $null
    mixinJsonPresent = $false
    refmapPresent = $false
    nestedBaritone = @()
    generatedAtUtc = [DateTime]::UtcNow.ToString("o")
}

function Add-Finding {
    param(
        [Parameter(Mandatory = $true)][string]$Severity,
        [Parameter(Mandatory = $true)][string]$Area,
        [Parameter(Mandatory = $true)][string]$Message,
        [string]$Detail = ""
    )

    $findings.Add([pscustomobject]@{
        severity = $Severity
        area = $Area
        message = $Message
        detail = $Detail
    }) | Out-Null
}

function Get-HexString {
    param([byte[]]$Bytes)
    return -join ($Bytes | ForEach-Object { $_.ToString("x2") })
}

function Get-StreamSha256 {
    param([System.IO.Stream]$Stream)
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try {
        return Get-HexString -Bytes ($sha.ComputeHash($Stream))
    }
    finally {
        $sha.Dispose()
    }
}

function Invoke-Tool {
    param(
        [Parameter(Mandatory = $true)][string]$FileName,
        [Parameter(Mandatory = $true)][string[]]$Arguments
    )

    try {
        $output = & $FileName @Arguments 2>&1
        $exitCode = $LASTEXITCODE
        return [pscustomobject]@{
            exitCode = $exitCode
            text = (($output | ForEach-Object { $_.ToString() }) -join "`n")
        }
    }
    catch {
        return [pscustomobject]@{
            exitCode = 9999
            text = $_.Exception.Message
        }
    }
}

function Copy-ZipEntryToFile {
    param(
        [Parameter(Mandatory = $true)]$Entry,
        [Parameter(Mandatory = $true)][string]$Path
    )

    $inputStream = $Entry.Open()
    try {
        $outputStream = [System.IO.File]::Open($Path, [System.IO.FileMode]::Create, [System.IO.FileAccess]::Write)
        try {
            $inputStream.CopyTo($outputStream)
        }
        finally {
            $outputStream.Dispose()
        }
    }
    finally {
        $inputStream.Dispose()
    }
}

function Read-ZipEntryText {
    param(
        [Parameter(Mandatory = $true)]$Entry
    )

    $stream = $Entry.Open()
    try {
        $reader = New-Object System.IO.StreamReader($stream, [System.Text.Encoding]::UTF8)
        try {
            return $reader.ReadToEnd()
        }
        finally {
            $reader.Dispose()
        }
    }
    finally {
        $stream.Dispose()
    }
}

function Find-DefaultJar {
    param([string]$ResolvedRuntimeRoot)

    $libs = Join-Path $ResolvedRuntimeRoot "versions\1.20.1\build\libs"
    if (-not (Test-Path -LiteralPath $libs)) {
        return $null
    }

    $jars = Get-ChildItem -LiteralPath $libs -Filter "chatclef-1.20.1-*.jar" -File |
        Where-Object { $_.Name -notmatch "-sources\.jar$" } |
        Sort-Object LastWriteTime -Descending

    if ($jars.Count -eq 0) {
        return $null
    }

    return $jars[0].FullName
}

function Add-JavapFile {
    param(
        [string]$Name,
        [string]$Text
    )

    $safeName = $Name -replace "[^A-Za-z0-9_.-]", "_"
    $path = Join-Path $script:ResolvedOutputDirectory $safeName
    Set-Content -LiteralPath $path -Value $Text -Encoding UTF8
}

$repoResult = Invoke-Tool -FileName "git" -Arguments @("rev-parse", "--show-toplevel")
if ($repoResult.exitCode -eq 0) {
    $evidence.repositoryRoot = $repoResult.text.Trim()
}
else {
    Add-Finding -Severity "WARN" -Area "repo" -Message "Could not resolve git repository root." -Detail $repoResult.text
}

try {
    $resolvedRuntimeRoot = (Resolve-Path -LiteralPath $RuntimeRoot).Path
    $evidence.runtimeRoot = $resolvedRuntimeRoot
}
catch {
    Add-Finding -Severity "ERROR" -Area "runtime" -Message "Runtime root does not exist." -Detail $RuntimeRoot
    $resolvedRuntimeRoot = $RuntimeRoot
}

$script:ResolvedOutputDirectory = $OutputDirectory
if (-not [System.IO.Path]::IsPathRooted($script:ResolvedOutputDirectory)) {
    $script:ResolvedOutputDirectory = Join-Path (Get-Location).Path $script:ResolvedOutputDirectory
}
New-Item -ItemType Directory -Force -Path $script:ResolvedOutputDirectory | Out-Null

if ([string]::IsNullOrWhiteSpace($JarPath)) {
    $JarPath = Find-DefaultJar -ResolvedRuntimeRoot $resolvedRuntimeRoot
}

if ([string]::IsNullOrWhiteSpace($JarPath) -or -not (Test-Path -LiteralPath $JarPath)) {
    Add-Finding -Severity "ERROR" -Area "jar" -Message "No final ChatClef 1.20.1 jar was found." -Detail "Run the advisory build or pass -JarPath."
}
else {
    $jarItem = Get-Item -LiteralPath $JarPath
    $evidence.jarPath = $jarItem.FullName
    $evidence.jarLength = $jarItem.Length
    $evidence.jarLastWriteTime = $jarItem.LastWriteTime.ToString("o")
    $evidence.jarSha256 = (Get-FileHash -LiteralPath $jarItem.FullName -Algorithm SHA256).Hash.ToLowerInvariant()

    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem

    $zip = [System.IO.Compression.ZipFile]::OpenRead($jarItem.FullName)
    try {
        $entries = @($zip.Entries | ForEach-Object { $_.FullName })

        $mixinEntry = $zip.GetEntry("altoclef.mixins.json")
        if ($null -eq $mixinEntry) {
            Add-Finding -Severity "ERROR" -Area "jar" -Message "Final jar does not contain altoclef.mixins.json."
        }
        else {
            $evidence.mixinJsonPresent = $true
            $mixinJson = Read-ZipEntryText -Entry $mixinEntry
            Set-Content -LiteralPath (Join-Path $script:ResolvedOutputDirectory "altoclef.mixins.json") -Value $mixinJson -Encoding UTF8

            try {
                $mixinConfig = $mixinJson | ConvertFrom-Json
                if ($mixinConfig.required -eq $true -and $mixinConfig.injectors.defaultRequire -eq 1) {
                    $diagnosticMixins = @()
                    foreach ($field in @("mixins", "client", "server")) {
                        $property = $mixinConfig.PSObject.Properties[$field]
                        if ($null -ne $property -and $null -ne $property.Value) {
                            $diagnosticMixins += @($property.Value | Where-Object { $_ -match "diagnostics" })
                        }
                    }
                    if ($diagnosticMixins.Count -gt 0) {
                        Add-Finding -Severity "WARN" -Area "diagnostics" -Message "Diagnostic Mixins are present in the strict core config." -Detail ($diagnosticMixins -join ", ")
                    }
                }
            }
            catch {
                Add-Finding -Severity "WARN" -Area "mixin-json" -Message "Could not parse final altoclef.mixins.json." -Detail $_.Exception.Message
            }
        }

        if ($entries -contains "chatclef-refmap.json") {
            $evidence.refmapPresent = $true
        }
        else {
            Add-Finding -Severity "ERROR" -Area "refmap" -Message "Final jar does not contain chatclef-refmap.json."
        }

        $nestedBaritoneEntries = @($zip.Entries | Where-Object {
            $_.FullName -like "META-INF/jars/*.jar" -and $_.FullName -match "baritone"
        })

        if ($nestedBaritoneEntries.Count -eq 0) {
            Add-Finding -Severity "ERROR" -Area "baritone" -Message "No nested Baritone jar was found under META-INF/jars."
        }
        else {
            foreach ($entry in $nestedBaritoneEntries) {
                $stream = $entry.Open()
                try {
                    $hash = Get-StreamSha256 -Stream $stream
                }
                finally {
                    $stream.Dispose()
                }
                $nestedBaritoneEvidence.Add([pscustomobject]@{
                    name = $entry.FullName
                    length = $entry.Length
                    sha256 = $hash
                }) | Out-Null
            }
        }

        $entityJavap = Invoke-Tool -FileName "javap" -Arguments @(
            "-classpath", $jarItem.FullName,
            "-p", "-v",
            "adris.altoclef.mixins.EntityAnimationSwungMixin"
        )
        Add-JavapFile -Name "EntityAnimationSwungMixin.javap.txt" -Text $entityJavap.text
        if ($entityJavap.exitCode -ne 0) {
            Add-Finding -Severity "ERROR" -Area "javap" -Message "javap failed for EntityAnimationSwungMixin." -Detail $entityJavap.text
        }
        else {
            if ($entityJavap.text -notmatch [regex]::Escape("method_11160(Lnet/minecraft/class_2616;)V")) {
                Add-Finding -Severity "ERROR" -Area "EntityAnimationSwungMixin" -Message "Expected 1.20.1 runtime selector was not found in final annotation."
            }
            if ($entityJavap.text -notmatch "remap=false") {
                Add-Finding -Severity "ERROR" -Area "EntityAnimationSwungMixin" -Message "Expected remap=false was not found in final annotation."
            }
        }

        $movementJavap = Invoke-Tool -FileName "javap" -Arguments @(
            "-classpath", $jarItem.FullName,
            "-p", "-v",
            "adris.altoclef.mixins.MovementHelperMixin"
        )
        Add-JavapFile -Name "MovementHelperMixin.javap.txt" -Text $movementJavap.text
        if ($movementJavap.exitCode -ne 0) {
            Add-Finding -Severity "ERROR" -Area "javap" -Message "javap failed for MovementHelperMixin." -Detail $movementJavap.text
        }
        else {
            $expectedTarget = "Lnet/minecraft/class_2680;method_26204()Lnet/minecraft/class_2248;"
            if ($movementJavap.text -notmatch [regex]::Escape($expectedTarget)) {
                Add-Finding -Severity "ERROR" -Area "MovementHelperMixin" -Message "Expected Baritone redirect target was not found in final annotation."
            }
            if ($movementJavap.text -notmatch "ordinal=1") {
                Add-Finding -Severity "ERROR" -Area "MovementHelperMixin" -Message "Expected ordinal=1 was not found in final annotation."
            }
            if ($movementJavap.text -notmatch "remap=false") {
                Add-Finding -Severity "ERROR" -Area "MovementHelperMixin" -Message "Expected remap=false was not found in final annotation."
            }
        }

        if ($nestedBaritoneEntries.Count -gt 0) {
            $firstBaritone = $nestedBaritoneEntries[0]
            $tempNested = Join-Path ([System.IO.Path]::GetTempPath()) ("chatclef-nested-baritone-" + [guid]::NewGuid().ToString() + ".jar")
            try {
                Copy-ZipEntryToFile -Entry $firstBaritone -Path $tempNested
                $baritoneJavap = Invoke-Tool -FileName "javap" -Arguments @(
                    "-classpath", $tempNested,
                    "-c", "-p",
                    "baritone.pathing.movement.MovementHelper"
                )
                Add-JavapFile -Name "BaritoneMovementHelper.javap.txt" -Text $baritoneJavap.text
                if ($baritoneJavap.exitCode -ne 0) {
                    Add-Finding -Severity "WARN" -Area "baritone" -Message "javap failed for nested Baritone MovementHelper." -Detail $baritoneJavap.text
                }
                else {
                    $callCount = ([regex]::Matches($baritoneJavap.text, "class_2680\.method_26204")).Count
                    if ($callCount -lt 2) {
                        Add-Finding -Severity "ERROR" -Area "baritone" -Message "MovementHelper bytecode does not expose enough method_26204 calls for ordinal=1." -Detail "Observed call count: $callCount"
                    }
                }
            }
            finally {
                Remove-Item -LiteralPath $tempNested -Force -ErrorAction SilentlyContinue
            }
        }
    }
    finally {
        $zip.Dispose()
    }
}

$fatalPatterns = @(
    "Mixin apply failed",
    "Critical injection failure",
    "InvalidInjectionException",
    "InjectionError",
    "failed injection check",
    "could not find any targets matching",
    "MixinApplyError",
    "MixinTransformerError"
)

if (-not [string]::IsNullOrWhiteSpace($MinecraftLogPath)) {
    if (Test-Path -LiteralPath $MinecraftLogPath) {
        $matches = Select-String -LiteralPath $MinecraftLogPath -Pattern $fatalPatterns -SimpleMatch -ErrorAction SilentlyContinue
        if ($matches) {
            Add-Finding -Severity "ERROR" -Area "latest-log" -Message "Minecraft latest.log contains Mixin fatal patterns." -Detail (($matches | Select-Object -First 20 | ForEach-Object { "$($_.LineNumber): $($_.Line)" }) -join "`n")
        }
    }
    else {
        Add-Finding -Severity "WARN" -Area "latest-log" -Message "Minecraft log path was provided but does not exist." -Detail $MinecraftLogPath
    }
}

if (-not [string]::IsNullOrWhiteSpace($CrashReportDirectory)) {
    if (Test-Path -LiteralPath $CrashReportDirectory) {
        $crashFiles = Get-ChildItem -LiteralPath $CrashReportDirectory -Filter "*.txt" -File | Sort-Object LastWriteTime -Descending | Select-Object -First 10
        foreach ($crashFile in $crashFiles) {
            $matches = Select-String -LiteralPath $crashFile.FullName -Pattern $fatalPatterns -SimpleMatch -ErrorAction SilentlyContinue
            if ($matches) {
                Add-Finding -Severity "ERROR" -Area "crash-report" -Message "Crash report contains Mixin fatal patterns." -Detail "$($crashFile.FullName)`n$((($matches | Select-Object -First 10 | ForEach-Object { "$($_.LineNumber): $($_.Line)" }) -join "`n"))"
            }
        }
    }
    else {
        Add-Finding -Severity "WARN" -Area "crash-report" -Message "Crash report directory was provided but does not exist." -Detail $CrashReportDirectory
    }
}

$evidence["nestedBaritone"] = @($nestedBaritoneEvidence.ToArray())

$result = [ordered]@{
    evidence = $evidence
    findings = @($findings.ToArray())
}

$jsonPath = Join-Path $script:ResolvedOutputDirectory "chatclef-mixin-advisory-report.json"
$mdPath = Join-Path $script:ResolvedOutputDirectory "chatclef-mixin-advisory-report.md"

$result | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $jsonPath -Encoding UTF8

$markdown = New-Object System.Collections.Generic.List[string]
$markdown.Add("# ChatClef Mixin Advisory Report")
$markdown.Add("")
$markdown.Add("Generated UTC: $($evidence.generatedAtUtc)")
$markdown.Add("")
$markdown.Add("## Evidence")
$markdown.Add("")
foreach ($key in $evidence.Keys) {
    if ($key -eq "nestedBaritone") {
        continue
    }
    $markdown.Add("- $key`: $($evidence[$key])")
}
$markdown.Add("")
$markdown.Add("## Nested Baritone")
$markdown.Add("")
if ($evidence.nestedBaritone.Count -eq 0) {
    $markdown.Add("- none")
}
else {
    foreach ($entry in $evidence.nestedBaritone) {
        $markdown.Add("- $($entry.name) size=$($entry.length) sha256=$($entry.sha256)")
    }
}
$markdown.Add("")
$markdown.Add("## Findings")
$markdown.Add("")
if ($findings.Count -eq 0) {
    $markdown.Add("- No findings.")
}
else {
    foreach ($finding in $findings) {
        $markdown.Add("- [$($finding.severity)] $($finding.area): $($finding.message)")
        if (-not [string]::IsNullOrWhiteSpace($finding.detail)) {
            $markdown.Add("")
            $markdown.Add('```text')
            $markdown.Add($finding.detail)
            $markdown.Add('```')
            $markdown.Add("")
        }
    }
}

$markdown -join "`n" | Set-Content -LiteralPath $mdPath -Encoding UTF8

if (-not [string]::IsNullOrWhiteSpace($env:GITHUB_STEP_SUMMARY)) {
    Add-Content -LiteralPath $env:GITHUB_STEP_SUMMARY -Value (($markdown -join "`n") + "`n") -Encoding UTF8
}

$errorCount = @($findings | Where-Object { $_.severity -eq "ERROR" }).Count
if ($FailOnFindings -and $errorCount -gt 0) {
    exit 1
}

exit 0
