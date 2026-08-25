[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[0-9]{8}-[0-9]{6}$')]
    [string]$RunId,

    [switch]$Offline
)

$ErrorActionPreference = 'Stop'

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$runtimeRoot = Join-Path $repositoryRoot 'plugins\Minecraft\runtime\chatclef_fabric_1.20.1'
$logDirectory = Join-Path $repositoryRoot 'logs\build'
$logPath = Join-Path $logDirectory "chatclef-fabric-clean-build-$RunId.log"
$resultPath = Join-Path $logDirectory "chatclef-fabric-clean-build-$RunId.result.json"
$gradleCommand = '.\gradlew.bat clean build --rerun-tasks --no-build-cache --no-daemon --stacktrace'
if ($Offline) {
    $gradleCommand += ' --offline'
}

New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null
Set-Location -LiteralPath $runtimeRoot

$repositoryTopLevel = (& git rev-parse --show-toplevel).Trim()
if ($LASTEXITCODE -ne 0 -or $repositoryTopLevel -ne $repositoryRoot.Replace('\', '/')) {
    throw "Repository root mismatch. Expected '$repositoryRoot', found '$repositoryTopLevel'."
}

Write-Host "Repository root: $repositoryTopLevel"
Write-Host "Runtime root:    $runtimeRoot"
Write-Host "Command:         $gradleCommand"
Write-Host "Build log:       $logPath"
Write-Host ''

$startedAt = Get-Date
$buildExitCode = 1
$failureMessage = ''

try {
    & $env:ComSpec /d /c "$gradleCommand 2>&1" |
        Tee-Object -FilePath $logPath
    $buildExitCode = $LASTEXITCODE
} catch {
    $failureMessage = $_.Exception.Message
    $_ | Out-String | Tee-Object -FilePath $logPath -Append | Write-Host
    $buildExitCode = 1
}

$jarPath = ''
$jarBytes = $null
$jarModified = ''
$jarSha256 = ''

if ($buildExitCode -eq 0) {
    $jar = Get-ChildItem -LiteralPath (Join-Path $runtimeRoot 'versions\1.20.1\build\libs') -File |
        Where-Object {
            $_.Name -like 'chatclef-1.20.1-*.jar' -and
            $_.Name -notlike '*-sources.jar' -and
            $_.Name -notlike '*-dev.jar'
        } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1

    if ($null -eq $jar) {
        $failureMessage = 'The clean build succeeded, but no 1.20.1 runtime JAR was found.'
        $buildExitCode = 2
    } else {
        $jarPath = $jar.FullName
        $jarBytes = $jar.Length
        $jarModified = $jar.LastWriteTime.ToString('o')
        $jarSha256 = (Get-FileHash -LiteralPath $jar.FullName -Algorithm SHA256).Hash
    }
}

$result = [ordered]@{
    run_id = $RunId
    repository_root = $repositoryTopLevel
    runtime_root = $runtimeRoot
    command = $gradleCommand
    started_at = $startedAt.ToString('o')
    finished_at = (Get-Date).ToString('o')
    exit_code = $buildExitCode
    failure_message = $failureMessage
    log_path = $logPath
    jar_path = $jarPath
    jar_bytes = $jarBytes
    jar_modified = $jarModified
    jar_sha256 = $jarSha256
}

$result | ConvertTo-Json -Depth 3 | Set-Content -LiteralPath $resultPath -Encoding UTF8

Write-Host ''
if ($buildExitCode -eq 0) {
    Write-Host 'BUILD VERIFIED: Gradle completed successfully.' -ForegroundColor Green
    Write-Host "JAR:    $jarPath"
    Write-Host "SHA256: $jarSha256"
} else {
    Write-Host "BUILD FAILED: exit code $buildExitCode" -ForegroundColor Red
    if ($failureMessage) {
        Write-Host "Reason: $failureMessage" -ForegroundColor Red
    }
}
Write-Host "Result: $resultPath"

exit $buildExitCode
