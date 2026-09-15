#20260915_kpopmodder: Own simulated admission/session state separately from trusted ingress composition.
from dataclasses import replace

from tests.minecraft_chatclef.lavi_input.test_trusted_korean_chat_voice_integration import (
    _RecordingMinecraftAdapter,
)


class ConfirmationRecordingAdapter(_RecordingMinecraftAdapter):
    def __init__(self):
        super().__init__()
        self.session = "confirmation-session"
        self.generation = 1
        self.busy = None
        self.connected = True
        self.catalogue = None

    def get_command_catalogue(self):
        return self.catalogue

    def get_status(self):
        status = super().get_status()
        status.details["commands"].update(
            active_session_id=self.session,
            active_generation=self.generation,
            active_request_id=self.busy,
        )
        return replace(status, connected=self.connected)
