#20260914_kpopmodder: Opt in to the separate gold protection target using the existing isolated bytecode-only host.
param([switch] $PrepareOnly, [switch] $CompileOnly, [string] $MainInput = '', [switch] $ArtifactJar)
$ErrorActionPreference = 'Stop'
$goldSmokeLauncher = 'C:\Vtuber_Souorce_Code\LAVI\plugins\Minecraft\runtime\chatclef_fabric_1.20.1\src\test\mixinApplication\scripts\verify_mixin_application.ps1'
. ([scriptblock]::Create((Get-Content -LiteralPath $goldSmokeLauncher -Raw -Encoding UTF8))) `
    -GoldProtection -PrepareOnly:$PrepareOnly -CompileOnly:$CompileOnly -MainInput $MainInput -ArtifactJar:$ArtifactJar
