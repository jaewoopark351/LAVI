#20260914_kpopmodder: Verify only Fabric 1.20.1 in a dedicated PowerShell window with repository-local outputs.
param([ValidateSet('Tests','Build','DryBuild','Bootstrap')][string]$Mode = 'Tests')
$ErrorActionPreference = 'Stop'
$repositoryRoot = 'C:\Vtuber_Souorce_Code\LAVI'
$runtimeRoot = Join-Path $repositoryRoot 'plugins\Minecraft\runtime\chatclef_fabric_1.20.1'
Set-Location -LiteralPath $repositoryRoot
if ((Get-Location).Path -ne $repositoryRoot -or (& git rev-parse --show-toplevel).Trim() -ne $repositoryRoot.Replace('\','/')) { throw 'Repository boundary mismatch' }
$outputRoot = Join-Path $repositoryRoot ('logs\auto_deposit_1201_' + $Mode + '_' + (Get-Date -Format 'yyyyMMdd_HHmmss_fff'))
[IO.Directory]::CreateDirectory($outputRoot) | Out-Null
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:GRADLE_USER_HOME = Join-Path $runtimeRoot '.gradle\codex-user-home'
$env:GRADLE_RO_DEP_CACHE = 'C:\Users\jaewo\.gradle\caches'
$env:TEMP = Join-Path $runtimeRoot '.gradle\codex-build-temp'
$env:TMP = $env:TEMP
[IO.Directory]::CreateDirectory($env:TEMP) | Out-Null
$arguments = @('--no-build-cache','--no-daemon','--offline','--stacktrace','--warning-mode=summary','--init-script','.gradle/codex-build-init.gradle')
if ($Mode -eq 'Bootstrap') {
    $arguments += @('--init-script','src/test/autoDeposit/auto-deposit-tests.init.gradle',':1.20.1:autoDepositVerifyMinecraftBootstrap')
} elseif ($Mode -eq 'Tests') {
    $arguments += @('--init-script','src/test/autoDeposit/auto-deposit-tests.init.gradle',':1.20.1:autoDepositFocusedTests')
} else {
    # The ordinary build task pulls other versions' compileJava through preprocessTestCode.
    # Clean/remap the sole requested runtime and use its isolated test task instead.
    $arguments += @('--init-script','src/test/autoDeposit/auto-deposit-tests.init.gradle',
        ':1.20.1:clean',':1.20.1:remapJar',':1.20.1:validateAccessWidener',
        ':1.20.1:autoDepositFocusedTests','--rerun-tasks')
    if ($Mode -eq 'DryBuild') { $arguments += '--dry-run' }
}
Set-Location -LiteralPath $runtimeRoot
$logPath = Join-Path $outputRoot 'verification.log'
$started = Get-Date
$code = 1
if ($Mode -eq 'Build') {
    $sourcePaths = @(& rg --files src/main src/test/autoDeposit src/test/java/lavi/minecraft/task/container/deposit/auto)
    $sourcePaths += @('build.gradle','root.gradle.kts','settings.gradle.kts','gradle.properties')
    $sourceManifest = foreach ($sourcePath in ($sourcePaths | Sort-Object -Unique)) {
        $resolved = [IO.Path]::GetFullPath((Join-Path $runtimeRoot $sourcePath))
        if (-not $resolved.StartsWith($runtimeRoot + '\', [StringComparison]::OrdinalIgnoreCase)) { throw 'Source manifest escaped runtime boundary' }
        [ordered]@{ path=$sourcePath; sha256=(Get-FileHash -LiteralPath $resolved -Algorithm SHA256).Hash }
    }
    [ordered]@{ repository=$repositoryRoot; branch=(& git branch --show-current).Trim(); head=(& git rev-parse HEAD).Trim(); started=$started.ToString('o'); dirty_paths=@(& git -C $repositoryRoot status --porcelain --untracked-files=all); sources=$sourceManifest } |
        ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $outputRoot 'source-manifest.json') -Encoding UTF8
}
try {
    $ErrorActionPreference = 'Continue'
    "Mode=$Mode; Runtime=$runtimeRoot; Command=gradlew.bat $($arguments -join ' ')" | Tee-Object -FilePath $logPath
    & java -version 2>&1 | Tee-Object -FilePath $logPath -Append
    & .\gradlew.bat --version 2>&1 | Tee-Object -FilePath $logPath -Append
    & .\gradlew.bat @arguments 2>&1 | Tee-Object -FilePath $logPath -Append
    $code = $LASTEXITCODE
} finally {
    $jar = Join-Path $runtimeRoot 'versions\1.20.1\build\libs\chatclef-1.20.1-0.18.23.jar'
    $artifact = if (Test-Path -LiteralPath $jar) { Get-Item -LiteralPath $jar } else { $null }
    $hash = if ($artifact -and $Mode -eq 'Build' -and $code -eq 0) { (Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash } else { '' }
    [ordered]@{ started=$started.ToString('o'); finished=(Get-Date).ToString('o'); mode=$Mode; exit_code=$code; command=($arguments -join ' '); log=$logPath; jar=$jar; sha256=$hash; bytes=$(if ($artifact) { $artifact.Length } else { 0 }); deployment='NOT_RUN'; live_runtime='NOT_RUN' } |
        ConvertTo-Json | Set-Content -LiteralPath (Join-Path $outputRoot 'result.json') -Encoding UTF8
    Write-Host "VERIFY_EXIT=$code; OUTPUT=$outputRoot"
}
exit $code
