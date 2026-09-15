#20260915_kpopmodder: Carry independent display and speech text without granting filter exemptions.
from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class RoutedResponseText:
    display_text: str
    speech_text: str

    def __post_init__(self) -> None:
        for value in (self.display_text, self.speech_text):
            if type(value) is not str or not value.strip() or len(value) > 32768:
                raise ValueError("routed response text must be a bounded non-empty exact str")
