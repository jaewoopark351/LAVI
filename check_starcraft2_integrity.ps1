# 20260713_kpopmodder: Added one-line SC2 integrity checker for match result completeness.
param(
    [string]$LogPath = "",
    [string]$LogDir = (Join-Path -Path $PSScriptRoot -ChildPath "logs"),
    [switch]$Verbose
)

$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new()
$ErrorActionPreference = "Stop"

function Show-Result {
    param([string]$Status, [string[]]$Reasons, [string]$ResultFile)
    $statusText = if ($Status -eq "PASS") { "SC2_PASS" } else { "SC2_FAIL" }
    $reasonText = if ($Reasons.Count -gt 0) { $Reasons -join "|" } else { "ok" }
    Write-Output "$statusText|file=$ResultFile|reasons=$reasonText"
}

if (-not $LogPath) {
    if (-not (Test-Path -LiteralPath $LogDir -PathType Container)) {
        Show-Result "FAIL" @("missing_log_dir:$LogDir") ""
        exit 2
    }
    $latest = Get-ChildItem -LiteralPath $LogDir -Filter '*.txt' -File | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $latest) {
        Show-Result "FAIL" @("no_log_file:$LogDir") ""
        exit 2
    }
    $LogPath = $latest.FullName
}

if (-not (Test-Path -LiteralPath $LogPath -PathType Leaf)) {
    Show-Result "FAIL" @("missing_log_file:$LogPath") ""
    exit 2
}

$lines = Get-Content -LiteralPath $LogPath
$sc2Lines = $lines | Where-Object {
    $_ -match '\[StarCraft2\] ladder_proxy stdout:' -or
    $_ -match '\[StarCraft2Reaction\]' -or
    $_ -match '\[SC2LadderProxyLauncher\]'
}

$hasMatchStart = $sc2Lines | Select-String -Pattern 'Starting the match|Creating the game' | Select-Object -First 1
$hasHumanEnded = $sc2Lines | Select-String -Pattern 'LAVHuman : Client changed status from in_game to ended' | Select-Object -First 1
$hasBotEnded = $sc2Lines | Select-String -Pattern 'changeling : Client changed status from in_game to ended' | Select-Object -First 1
$hasResultLine = $sc2Lines | Select-String -Pattern '\[LavHumanVsBot\] Finished with result: Player(1|2)Win' | Select-Object -First 1
$hasResultEvent = $sc2Lines | Select-String -Pattern 'event=game_(won|lost)' | Select-Object -First 1
$hasEngineError = $sc2Lines | Select-String -Pattern 'engine_error|RESPONSE_NOT_SET' | Select-Object -First 1
$hasJoinGameDone = $sc2Lines | Select-String -Pattern 'sending JoinGame request|received JoinGame response|Forwarding JoinGame request' | Select-Object -First 1

$reasons = @()
if (-not $hasMatchStart) { $reasons += "missing_game_start" }
if (-not $hasHumanEnded) { $reasons += "missing_human_end_status" }
if (-not $hasBotEnded) { $reasons += "missing_bot_end_status" }
if (-not $hasResultLine) { $reasons += "missing_final_result_line" }
if (-not $hasResultEvent) { $reasons += "missing_game_result_event" }
if (-not $hasJoinGameDone) { $reasons += "missing_join_game_flow" }
if ($hasEngineError) { $reasons = @("engine_error_detected") + $reasons }

if ($reasons.Count -eq 0) {
    if ($Verbose) {
        Write-Output "SC2_PASS|file=$LogPath|start='$($hasMatchStart.Line)'|result='$($hasResultLine.Line)'|result_event='$($hasResultEvent.Line)'"
    } else {
        Show-Result "PASS" @("ok") $LogPath
    }
    exit 0
}

if ($Verbose) {
    Write-Output "SC2_FAIL|file=$LogPath|reasons=$($reasons -join '|')"
}
else {
    Show-Result "FAIL" $reasons $LogPath
}
exit 1
