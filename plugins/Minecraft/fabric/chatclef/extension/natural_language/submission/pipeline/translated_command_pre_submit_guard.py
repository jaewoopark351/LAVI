#20260905_kpopmodder: Isolate optional translated-command pre-submit guards.
from __future__ import annotations


class TranslatedCommandPreSubmitGuard:
    def invoke(self, guard, request: object, translation: object):
        if guard is None:
            return None
        return guard(request, translation)


__all__ = ("TranslatedCommandPreSubmitGuard",)
