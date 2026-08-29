$ErrorActionPreference = "Continue"
Set-StrictMode -Version Latest

$runtimeRoot = Split-Path -Parent $PSCommandPath
Set-Location -LiteralPath $runtimeRoot

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$logDir = Join-Path $runtimeRoot "codex-build-logs"
New-Item -ItemType Directory -Path $logDir -Force | Out-Null
$logPath = Join-Path $logDir "chatclef-fabric-1.20.1-build-$timestamp.log"

function Write-LogLine {
    param([Parameter(Mandatory = $true)][string]$Message)
    $line = "[$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')] $Message"
    Add-Content -LiteralPath $logPath -Value $line
    Write-Host $line
}

function Invoke-LoggedCommand {
    param(
        [Parameter(Mandatory = $true)][string]$Label,
        [Parameter(Mandatory = $true)][scriptblock]$Command
    )

    Write-LogLine ""
    Write-LogLine $Label
    & $Command 2>&1 | Tee-Object -FilePath $logPath -Append | Out-Host
    $exitCode = $LASTEXITCODE
    if ($null -eq $exitCode) {
        $exitCode = 0
    }
    Write-LogLine "$Label exit code: $exitCode"
    return $exitCode
}

Write-LogLine "Repository root expected: C:\Vtuber_Souorce_Code\LAVI"
Write-LogLine "Runtime working directory: $runtimeRoot"
Write-LogLine "Exact Gradle command: .\gradlew.bat clean build --rerun-tasks"

$adoptiumRoot = "C:\Program Files\Eclipse Adoptium"
$buildJdk = $null
if (Test-Path -LiteralPath $adoptiumRoot) {
    $buildJdk = Get-ChildItem -LiteralPath $adoptiumRoot -Directory -Filter "jdk-21*" |
        Sort-Object Name -Descending |
        Select-Object -First 1
}

if ($null -ne $buildJdk) {
    $env:JAVA_HOME = $buildJdk.FullName
    $env:Path = (Join-Path $buildJdk.FullName "bin") + ";" + $env:Path
    Write-LogLine "Process-local JAVA_HOME: $($env:JAVA_HOME)"
} else {
    Write-LogLine "No JDK 21 found under C:\Program Files\Eclipse Adoptium; using existing Java environment."
}

$windowsRootTrustStoreOption = "-Djavax.net.ssl.trustStoreType=Windows-ROOT"
$existingJavaToolOptions = [Environment]::GetEnvironmentVariable("JAVA_TOOL_OPTIONS", "Process")
if ([string]::IsNullOrWhiteSpace($existingJavaToolOptions)) {
    $env:JAVA_TOOL_OPTIONS = $windowsRootTrustStoreOption
} elseif ($existingJavaToolOptions -notmatch "javax\.net\.ssl\.trustStore(Type)?=") {
    $env:JAVA_TOOL_OPTIONS = "$existingJavaToolOptions $windowsRootTrustStoreOption"
}
Write-LogLine "Process-local JAVA_TOOL_OPTIONS: $($env:JAVA_TOOL_OPTIONS)"

Invoke-LoggedCommand "Current directory" { Get-Location | Format-List }
Invoke-LoggedCommand "Git repository root" { git rev-parse --show-toplevel }
Invoke-LoggedCommand "java -version" { java -version }
Invoke-LoggedCommand "Gradle JVM" { .\gradlew.bat -version }

$buildExitCode = Invoke-LoggedCommand "Gradle clean forced build" { .\gradlew.bat clean build --rerun-tasks }

if ($buildExitCode -eq 0) {
    $expectedJar = Join-Path $runtimeRoot "versions\1.20.1\build\libs\chatclef-1.20.1-0.18.23.jar"
    if (Test-Path -LiteralPath $expectedJar) {
        Invoke-LoggedCommand "1.20.1 jar item" { Get-Item -LiteralPath $expectedJar | Format-List FullName,Length,LastWriteTime }
        Invoke-LoggedCommand "1.20.1 jar SHA-256" { Get-FileHash -LiteralPath $expectedJar -Algorithm SHA256 | Format-List }
    } else {
        Write-LogLine "Expected jar not found: $expectedJar"
        Invoke-LoggedCommand "Available 1.20.1 build libs" { Get-ChildItem -LiteralPath (Join-Path $runtimeRoot "versions\1.20.1\build\libs") -Filter "*.jar" | Format-List FullName,Length,LastWriteTime }
    }
}

Write-LogLine "Build script complete. Gradle build exit code: $buildExitCode"
Write-Host ""
Write-Host "Codex build log:"
Write-Host $logPath
Write-Host ""
Read-Host "Press Enter to close this PowerShell window"

exit $buildExitCode
