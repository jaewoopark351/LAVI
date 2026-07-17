#20260717_kpopmodder: Public GPT-SoVITS HTTP client name separated from legacy api_client module.
from plugins.GPTSoVITS.gpt_sovits_core.gpt_sovits_api_client import (
    GPTSoVITSApiClient,
)


class GPTSoVITSClient(GPTSoVITSApiClient):
    #20260717_kpopmodder: Compatibility subclass; HTTP behavior remains in GPTSoVITSApiClient.
    pass
