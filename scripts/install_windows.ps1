# #20260716_kpopmodder: Project-local Windows installer; never installs system tools or edits registry.
[CmdletBinding()]
param(
    [ValidateSet("Core", "Full")]
    [string]$Profile = "Full",
    [ValidateSet("CPU", "cu130")]
    [string]$Accelerator = "cu130",
    [switch]$Dev
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = Resolve-Path -LiteralPath (Join-Path $ScriptDir "..")
$VenvDir = Join-Path $RepoRoot "venv"
$VenvPython = Join-Path $VenvDir "Scripts\python.exe"
$SupportedLocks = @{
    "Core|CPU" = "requirements\locks\windows-py314-core-cpu.txt"
    "Full|cu130" = "requirements\locks\windows-py314-full-cu130.txt"
}

function Invoke-LaviCommand {
    param(
        [Parameter(Mandatory = $true)]
        [string]$FilePath,
        [Parameter(ValueFromRemainingArguments = $true)]
        [string[]]$Arguments
    )

    Write-Host "[LAVI install] $FilePath $($Arguments -join ' ')"
    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed with exit code ${LASTEXITCODE}: $FilePath $($Arguments -join ' ')"
    }
}

function Assert-Python314 {
    param([string]$PythonPath)

    $version = & $PythonPath -c "import sys; print(f'{sys.version_info.major}.{sys.version_info.minor}')"
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to query Python version with $PythonPath"
    }
    if ($version.Trim() -ne "3.14") {
        throw "Python 3.14 is required. Found Python $($version.Trim()) at $PythonPath"
    }
}

function Get-LaviInstallLock {
    param(
        [Parameter(Mandatory = $true)]
        [string]$SelectedProfile,
        [Parameter(Mandatory = $true)]
        [string]$SelectedAccelerator
    )

    $key = "$SelectedProfile|$SelectedAccelerator"
    if (-not $SupportedLocks.ContainsKey($key)) {
        $supported = ($SupportedLocks.Keys | Sort-Object) -join ", "
        throw "Unsupported install matrix: Profile=$SelectedProfile Accelerator=$SelectedAccelerator. Supported: $supported"
    }

    $relativeLockPath = $SupportedLocks[$key]
    $lockPath = Join-Path $RepoRoot $relativeLockPath
    if (-not (Test-Path -LiteralPath $lockPath)) {
        throw "Committed install lock not found: $lockPath"
    }
    return $relativeLockPath
}

Push-Location $RepoRoot
try {
    $InstallLock = Get-LaviInstallLock $Profile $Accelerator
    Write-Host "[LAVI install] Profile=$Profile Accelerator=$Accelerator"
    Write-Host "[LAVI install] Using committed lock: $InstallLock"

    if (-not (Test-Path -LiteralPath $VenvPython)) {
        $pyLauncher = Get-Command py -ErrorAction SilentlyContinue
        if (-not $pyLauncher) {
            throw "Python launcher 'py' was not found. Install Python 3.14 manually, then rerun this script."
        }
        Invoke-LaviCommand $pyLauncher.Source "-3.14" "-m" "venv" "venv"
    }

    Assert-Python314 $VenvPython

    Invoke-LaviCommand $VenvPython "-m" "pip" "--version"
    Invoke-LaviCommand $VenvPython "-m" "pip" "install" "-r" $InstallLock

    if ($Dev) {
        Invoke-LaviCommand $VenvPython "-m" "pip" "install" "-r" "requirements\dev.txt"
    }

    Invoke-LaviCommand $VenvPython "-m" "pip" "check"
    Invoke-LaviCommand $VenvPython "scripts\preflight.py" "--profile" $Profile "--accelerator" $Accelerator
}
finally {
    Pop-Location
}
