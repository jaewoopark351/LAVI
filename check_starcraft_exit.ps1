$ErrorActionPreference = 'Stop'
$OutputEncoding = [Console]::OutputEncoding = [Text.UTF8Encoding]::new()

$logDir = 'C:\Vtuber_Souorce_Code\LAV_v0.2\logs'
$patternStrict = '\[StarCraft116MonsterLogEvents\].*event:\s*type=monster_exit_code\s+severity=(?<sev>\w+)\s+exit_code=(?<code>-?\d+)\s+cause=(?<cause>\S+)\s+reason=(?<reason>.+)$'
$patternLoose = 'monster_exit_code'

if (-not (Test-Path $logDir)) {
    Write-Host "오류: 로그 폴더가 없습니다 - $logDir"
    exit 1
}

$files = Get-ChildItem -LiteralPath $logDir -File -Filter '*.txt' |
    Where-Object { $_.Name -notlike 'check_*' } |
    Sort-Object LastWriteTime -Descending

if ($files.Count -eq 0) {
    Write-Host '로그 파일 없음'
    exit 2
}

$strictHit = $null
$looseHits = @()

foreach ($f in $files) {
    $strict = Select-String -Path $f.FullName -Pattern $patternStrict
    if ($strict -and -not $strictHit) {
        $last = $strict | Select-Object -Last 1
        $strictHit = [pscustomobject]@{ File = $f.FullName; Line = $last.Line }
    }

    $loose = Select-String -Path $f.FullName -Pattern $patternLoose
    if ($loose) {
        $looseHits += [pscustomobject]@{
            File = $f.FullName
            Line = $loose[-1].Line
        }
    }
}

if ($strictHit) {
    $m = [regex]::Match($strictHit.Line, $patternStrict)
    $sev = $m.Groups['sev'].Value
    $code = [int]$m.Groups['code'].Value
    $cause = $m.Groups['cause'].Value
    $reason = $m.Groups['reason'].Value

    if ($sev -eq 'ok' -and $code -eq 0 -and $cause -eq 'normal_exit') {
        Write-Host '결과: 정상 종료'
    }
    else {
        Write-Host '결과: 비정상 종료'
    }

    Write-Host "로그: $($strictHit.File)"
    Write-Host "event: $($strictHit.Line)"
    Write-Host "severity=$sev exit_code=$code cause=$cause reason=$reason"
    exit 0
}

Write-Host '결과: 미발생(구조화된 monster_exit_code 이벤트 없음)'

if ($looseHits.Count -gt 0) {
    Write-Host "오탐 후보 또는 구형 포맷 개수: $($looseHits.Count)"
    $looseHits | Select-Object -First 5 | ForEach-Object {
        Write-Host "[$($_.File)] $($_.Line)"
    }
}
else {
    Write-Host '오탐/구형 포맷 미발생'
}

Write-Host "점검 파일 수: $($files.Count)"
exit 2
