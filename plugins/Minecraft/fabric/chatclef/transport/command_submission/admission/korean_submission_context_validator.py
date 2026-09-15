#20260915_kpopmodder: Revalidate Korean confirmation/catalogue identities under the existing command lock.
from __future__ import annotations

from collections.abc import Mapping


class KoreanSubmissionContextValidator:
    def __init__(self, ownership, catalogue_provider=None):
        self._ownership = ownership
        self._catalogue_provider = catalogue_provider

    def rejection_reason(self, request):
        metadata = request.metadata
        for context_key in ("korean_confirmation", "korean_single_trust"):
            confirmation = metadata.get(context_key)
            if confirmation is None:
                continue
            if (not isinstance(confirmation, Mapping)
                    or type(confirmation.get("expected_generation")) is not int
                    or confirmation.get("expected_session_id") != self._ownership.active_session_id
                    or confirmation.get("expected_generation") != self._ownership.active_generation):
                return context_key + "_session_changed"
        language = metadata.get("natural_language")
        translation = language.get("translation") if isinstance(language, Mapping) else None
        data = translation.get("data") if isinstance(translation, Mapping) else None
        catalogue = data.get("runtime_catalogue") if isinstance(data, Mapping) else None
        intent = translation.get("intent") if isinstance(translation, Mapping) else None
        slots = intent.get("slots", {}) if isinstance(intent, Mapping) else {}
        butler = isinstance(slots, Mapping) and slots.get("butler_user") is True
        kind = intent.get("intent_type") if isinstance(intent, Mapping) else None
        target = translation.get("resolved_target", "") if isinstance(translation, Mapping) else ""
        runtime_required = butler or kind == "attack" or (kind == "scan" and bool(intent.get("item_phrase")))
        runtime_required = runtime_required or (type(target) is str and any(c in target for c in ":./-"))
        if runtime_required and catalogue is None:
            return "korean_command_catalogue_missing"
        if catalogue is not None:
            current = self._catalogue_provider() if callable(self._catalogue_provider) else None
            if (not isinstance(catalogue, Mapping) or not isinstance(current, Mapping)
                    or catalogue.get("session_id") != self._ownership.active_session_id
                    or catalogue.get("session_id") != current.get("session_id")
                    or not catalogue.get("catalogue_sha256")
                    or catalogue.get("catalogue_sha256") != current.get("catalogue_sha256")):
                return "korean_command_catalogue_changed"
            if butler and (not data.get("butler_user_bound")
                           or data.get("butler_user_bound") != current.get("butler_user")):
                return "korean_butler_user_changed"
        return None
