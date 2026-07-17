#20260717_kpopmodder: Split smoke startup helper from legacy multi-class script for AGENTS 29.1.

from dataclasses import dataclass, field

from .smoke_error import SmokeError

@dataclass
class AttemptCounters:
    network_attempts: int = 0
    download_attempts: int = 0
    model_load_attempts: int = 0
    external_process_attempts: int = 0
    disabled_optional_imports: int = 0
    disabled_optional_constructions: int = 0
    disabled_optional_initializations: int = 0
    ffmpeg_download_attempts: int = 0
    first_attempt_stacks: dict = field(default_factory=dict, repr=False)

    def as_dict(self):
        return {
            "network_attempts": self.network_attempts,
            "download_attempts": self.download_attempts,
            "model_load_attempts": self.model_load_attempts,
            "external_process_attempts": self.external_process_attempts,
            "disabled_optional_imports": self.disabled_optional_imports,
            "disabled_optional_constructions": (
                self.disabled_optional_constructions
            ),
            "disabled_optional_initializations": (
                self.disabled_optional_initializations
            ),
            "ffmpeg_download_attempts": self.ffmpeg_download_attempts,
        }

    def assert_zero(self):
        attempts = {
            key: value
            for key, value in self.as_dict().items()
            if int(value or 0) != 0
        }
        if attempts:
            raise SmokeError(
                f"Core smoke side effects detected: {attempts}\n"
                f"first_attempt_stacks={self.first_attempt_stacks}"
            )

    def record_stack(self, label, stack):
        self.first_attempt_stacks.setdefault(label, stack)
