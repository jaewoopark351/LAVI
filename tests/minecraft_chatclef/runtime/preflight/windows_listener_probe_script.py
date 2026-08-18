#20260818_kpopmodder: Build the bounded read-only PowerShell listener/process probe.
from __future__ import annotations


def build_windows_listener_probe_script(ports: list[int]) -> str:
    port_values = ",".join(str(port) for port in sorted(set(ports)))
    return (
        "$ErrorActionPreference='Stop';"
        "[Console]::OutputEncoding=[System.Text.Encoding]::UTF8;"
        "$OutputEncoding=[Console]::OutputEncoding;"
        f"$targetPorts=@({port_values});"
        "$listeners=@(Get-NetTCPConnection -State Listen -ErrorAction Stop | "
        "Where-Object { $targetPorts -contains [int]$_.LocalPort } | "
        "ForEach-Object { [pscustomobject]@{local_address=$_.LocalAddress;"
        "local_port=[int]$_.LocalPort;process_id=[int]$_.OwningProcess} });"
        "$queue=New-Object System.Collections.Generic.Queue[int];"
        "$seen=@{};"
        "foreach($item in $listeners){$queue.Enqueue([int]$item.process_id)};"
        "$processes=@();$depth=0;"
        "while($queue.Count -gt 0 -and $depth -lt 1000){"
        "$processId=$queue.Dequeue();$depth++;"
        "if($processId -le 0 -or $seen.ContainsKey($processId)){continue};"
        "$seen[$processId]=$true;"
        "$process=Get-CimInstance Win32_Process -Filter (\"ProcessId = $processId\") "
        "-ErrorAction Stop;"
        "if($null -eq $process){continue};"
        "$processes+=[pscustomobject]@{process_id=[int]$process.ProcessId;"
        "parent_process_id=[int]$process.ParentProcessId;name=$process.Name;"
        "creation_date=$process.CreationDate;executable_path=$process.ExecutablePath;"
        "command_line=$process.CommandLine};"
        "if([int]$process.ParentProcessId -gt 0){"
        "$queue.Enqueue([int]$process.ParentProcessId)}};"
        "[pscustomobject]@{listeners=$listeners;processes=$processes} | "
        "ConvertTo-Json -Compress -Depth 5"
    )
