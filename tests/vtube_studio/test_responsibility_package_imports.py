#20260904_kpopmodder: Verify VTube Studio responsibility packages export their implementation types.
from plugins.VtubeStudio.vtube_studio_core.authentication import (
    VTubeStudioAuthManager as ExportedAuthManager,
)
from plugins.VtubeStudio.vtube_studio_core.authentication.vtube_studio_auth_manager import (
    VTubeStudioAuthManager as AuthenticationImplementation,
)
from plugins.VtubeStudio.vtube_studio_core.connection import (
    VTubeStudioConnection as ExportedConnection,
)
from plugins.VtubeStudio.vtube_studio_core.connection.vtube_studio_connection import (
    VTubeStudioConnection as ConnectionImplementation,
)
from plugins.VtubeStudio.vtube_studio_core.controllers.mouth import (
    VTubeStudioMouthController as ExportedMouthController,
)
from plugins.VtubeStudio.vtube_studio_core.controllers.mouth.vtube_studio_mouth_controller import (
    VTubeStudioMouthController as MouthControllerImplementation,
)


def test_responsibility_packages_export_implementation_types():
    assert ExportedConnection is ConnectionImplementation
    assert ExportedAuthManager is AuthenticationImplementation
    assert ExportedMouthController is MouthControllerImplementation
