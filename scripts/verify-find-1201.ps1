#20260914_kpopmodder: Keep canonical and isolated FIND build evidence separate, with repository-local UTF-8 logs.
param([switch]$Canonical, [switch]$FocusedOnly)
$ErrorActionPreference = 'Stop'
$repositoryRoot = 'C:\Vtuber_Souorce_Code\LAVI'
Set-Location -LiteralPath $repositoryRoot
if ((Get-Location).Path -ne $repositoryRoot -or (& git rev-parse --show-toplevel).Trim() -ne $repositoryRoot.Replace('\','/')) { throw 'Repository boundary mismatch' }
$runtimeRoot = Join-Path $repositoryRoot 'plugins\Minecraft\runtime\chatclef_fabric_1.20.1'
$outputRoot = Join-Path $repositoryRoot ('logs\find_verification_' + (Get-Date -Format 'yyyyMMdd_HHmmss_fff'))
[IO.Directory]::CreateDirectory($outputRoot) | Out-Null
$changedPaths = @(& git -c core.quotepath=false diff HEAD --name-only)
$changedPaths += @(& git -c core.quotepath=false ls-files --others --exclude-standard)
$sourceManifest = foreach ($sourcePath in ($changedPaths | Sort-Object -Unique)) {
    $resolved = [IO.Path]::GetFullPath((Join-Path $repositoryRoot $sourcePath))
    if (-not $resolved.StartsWith($repositoryRoot + '\', [StringComparison]::OrdinalIgnoreCase)) { throw 'Source manifest escaped repository' }
    if (Test-Path -LiteralPath $resolved -PathType Leaf) {
        [ordered]@{ path=$sourcePath; sha256=(Get-FileHash -LiteralPath $resolved -Algorithm SHA256).Hash }
    }
}
[ordered]@{ head=(& git rev-parse HEAD).Trim(); staged_paths=@(& git diff --cached --name-only);
    sources=$sourceManifest; snapshot_time=(Get-Date).ToString('o') } | ConvertTo-Json -Depth 5 |
    Set-Content -LiteralPath (Join-Path $outputRoot 'source-manifest.json') -Encoding UTF8
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:JAVA_TOOL_OPTIONS = '-Djavax.net.ssl.trustStoreType=Windows-ROOT'
Set-Location -LiteralPath $runtimeRoot
$commonArguments = @('--rerun-tasks','--no-build-cache','--no-daemon','--stacktrace','--warning-mode=summary')
$runs = @()
if ($Canonical) { $runs += @{ Name='canonical'; Arguments=@('clean','build') + $commonArguments } }
# The existing multiversion preprocessTestCode task also compiles the unrelated root1.21.1.
# Reuse the established raw focused selector runner for the requested1.20.1 runtime.
if ($FocusedOnly) {
    $runs += @{ Name='fabric12001-focused'; Arguments=@('--init-script','src/test/find/find-tests.init.gradle',
            ':1.20.1:findFocusedTests','--no-build-cache','--no-daemon','--stacktrace','--warning-mode=summary') }
} else {
    $runs += @{ Name='fabric12001'; Arguments=@('--init-script','src/test/find/find-tests.init.gradle',
            ':1.20.1:clean',':1.20.1:remapJar',':1.20.1:validateAccessWidener',':1.20.1:findFocusedTests') + $commonArguments }
}
$results = @()
foreach ($run in $runs) {
    $logPath = Join-Path $outputRoot ($run.Name + '.log')
    $writer = [IO.StreamWriter]::new($logPath, $false, [Text.UTF8Encoding]::new($false))
    $started = Get-Date
    $exitCode = 1
    try {
        $writer.WriteLine('Command=gradlew.bat ' + ($run.Arguments -join ' '))
        $ErrorActionPreference = 'Continue'
        & java -version 2>&1 | ForEach-Object { $writer.WriteLine($_.ToString()) }
        & .\gradlew.bat @($run.Arguments) 2>&1 | ForEach-Object { $writer.WriteLine($_.ToString()); $writer.Flush() }
        $exitCode = $LASTEXITCODE
    } finally {
        $writer.Dispose()
        $ErrorActionPreference = 'Stop'
        $results += [ordered]@{ mode=$run.Name; started=$started.ToString('o'); finished=(Get-Date).ToString('o');
            exit_code=$exitCode; command=($run.Arguments -join ' '); log=$logPath }
        Write-Output "VERIFY_MODE=$($run.Name); EXIT=$exitCode; LOG=$logPath"
    }
}
$jarPath = Join-Path $runtimeRoot 'versions\1.20.1\build\libs\chatclef-1.20.1-0.18.23.jar'
$targetSucceeded = $results[-1].exit_code -eq 0
$artifact = if ($targetSucceeded -and (Test-Path -LiteralPath $jarPath)) { Get-Item -LiteralPath $jarPath } else { $null }
[ordered]@{ head=(& git rev-parse HEAD).Trim(); branch=(& git branch --show-current).Trim(); runs=$results;
    jar=$jarPath; bytes=$(if ($artifact) { $artifact.Length } else { 0 });
    sha256=$(if ($artifact) { (Get-FileHash -LiteralPath $jarPath -Algorithm SHA256).Hash } else { '' });
    deployment='NOT_RUN'; live_runtime='NOT_RUN' } | ConvertTo-Json -Depth 6 |
    Set-Content -LiteralPath (Join-Path $outputRoot 'result.json') -Encoding UTF8
Write-Output "VERIFY_OUTPUT=$outputRoot"
exit $results[-1].exit_code
