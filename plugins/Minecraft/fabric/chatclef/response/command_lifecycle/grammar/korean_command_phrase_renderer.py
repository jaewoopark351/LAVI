#20260907_kpopmodder: Preserve phrase rendering behind focused boundary collaborators.
from __future__ import annotations

from .arguments import (
    KoreanCommandHungerGoalRenderer,
    KoreanCommandLocationRenderer,
    KoreanCommandSubjectRenderer,
)
from .boundary import (
    KoreanCommandStartRenderer,
    KoreanCommandStatusRenderer,
    KoreanCommandTerminalRenderer,
)
from .korean_particle_renderer import KoreanParticleRenderer
from .korean_quantity_renderer import KoreanQuantityRenderer


class KoreanCommandPhraseRenderer:
    def __init__(self, *, quantity_renderer=None, particle_renderer=None) -> None:
        quantity = quantity_renderer or KoreanQuantityRenderer()
        particle = particle_renderer or KoreanParticleRenderer()
        subjects = KoreanCommandSubjectRenderer(
            quantity_renderer=quantity,
            particle_renderer=particle,
        )
        collaborators = {
            "subject_renderer": subjects,
            "location_renderer": KoreanCommandLocationRenderer(),
            "hunger_renderer": KoreanCommandHungerGoalRenderer(),
            "particle_renderer": particle,
        }
        self._starts = KoreanCommandStartRenderer(**collaborators)
        self._statuses = KoreanCommandStatusRenderer(**collaborators)
        self._terminals = KoreanCommandTerminalRenderer(**collaborators)

    def start(self, descriptor: object, profile: object) -> str:
        return self._starts.render(descriptor, profile)

    def status(self, descriptor: object, profile: object, state: str) -> str:
        return self._statuses.render(descriptor, profile, state)

    def terminal(
        self,
        descriptor: object,
        profile: object,
        *,
        status: str,
        verified: bool,
        dispatch_started: bool,
        evidence_projection: object = None,
    ) -> str:
        return self._terminals.render(
            descriptor,
            profile,
            status=status,
            verified=verified,
            dispatch_started=dispatch_started,
            evidence_projection=evidence_projection,
        )


__all__ = ("KoreanCommandPhraseRenderer",)
