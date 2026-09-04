#20260904_kpopmodder: Verify token-file damage cannot prevent the VTube Studio provider notice path.
import tempfile
from pathlib import Path

from plugins.VtubeStudio.vtube_studio_core.authentication.vtube_studio_token_store import (
    VTubeStudioTokenStore,
)
from plugins.VtubeStudio.vtube_studio_core.ui import vtube_studio_ui as ui_module
from plugins.VtubeStudio.vtube_studio_core.ui.vtube_studio_ui import VTubeStudioUI


def test_corrupt_utf8_token_is_treated_as_missing_without_startup_failure(
    monkeypatch,
):
    token_path = Path(tempfile.mkdtemp(prefix="vtube-corrupt-token-")) / "token.txt"
    token_path.write_bytes(b"\xff")
    notices = []
    monkeypatch.setattr(ui_module.gr, "Info", notices.append)

    ui = VTubeStudioUI(VTubeStudioTokenStore(str(token_path)))

    assert ui.show_authentication_info() is True
    assert notices == ["Acquiring token, please continue in VTube Studio..."]
    assert token_path.read_bytes() == b"\xff"
