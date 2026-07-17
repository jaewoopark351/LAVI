#20260620_kpopmodder: GPTSoVITS helper modules are grouped under gpt_sovits_core without changing behavior.
from plugins.GPTSoVITS.gpt_sovits_core.gpt_sovits_process_manager import (
    GPTSoVITSProcessManager,
)


class GPTSoVITSServerManager(GPTSoVITSProcessManager):#20260619_kpopmodder
    #20260717_kpopmodder: Compatibility name; process ownership now lives in GPTSoVITSProcessManager.
    pass
