# #20260716_kpopmodder: Project-local Windows installer; never installs system tools or edits registry.
[CmdletBinding()]
param(
    [switch]$Dev
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = Resolve-Path -LiteralPath (Join-Path $ScriptDir "..")
$VenvDir = Join-Path $RepoRoot "venv"
$VenvPython = Join-Path $VenvDir "Scripts\python.exe"
$TorchIndexUrl = "https://download.pytorch.org/whl/cu130"

$TorchPackages = @(
    "torch==2.13.0+cu130",
    "torchvision==0.28.0+cu130",
    "torchaudio==2.11.0+cu130"
)

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

Push-Location $RepoRoot
try {
    if (-not (Test-Path -LiteralPath $VenvPython)) {
        $pyLauncher = Get-Command py -ErrorAction SilentlyContinue
        if (-not $pyLauncher) {
            throw "Python launcher 'py' was not found. Install Python 3.14 manually, then rerun this script."
        }
        Invoke-LaviCommand $pyLauncher.Source "-3.14" "-m" "venv" "venv"
    }

    Assert-Python314 $VenvPython

    Invoke-LaviCommand $VenvPython "-m" "pip" "--version"
    Invoke-LaviCommand $VenvPython "-m" "pip" "install" "--index-url" $TorchIndexUrl @TorchPackages
    Invoke-LaviCommand $VenvPython "-m" "pip" "install" "-r" "requirements.txt"

    if ($Dev) {
        Invoke-LaviCommand $VenvPython "-m" "pip" "install" "-r" "requirements\dev.txt"
    }

    Invoke-LaviCommand $VenvPython "-m" "pip" "check"
    Invoke-LaviCommand $VenvPython "scripts\preflight.py"
}
finally {
    Pop-Location
}
