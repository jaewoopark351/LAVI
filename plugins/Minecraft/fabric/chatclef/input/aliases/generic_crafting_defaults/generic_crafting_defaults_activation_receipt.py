#20260905_kpopmodder: Carry one opaque non-serializable Feature-B activation capability.
from __future__ import annotations


class GenericCraftingDefaultsActivationReceipt:
    __slots__ = (
        "_event_id",
        "_source",
        "_provider_id",
        "_event_kind",
        "_final",
        "_raw_event_text",
        "_translation_input_text",
        "_expected_intent_original_text",
        "_policy_id",
        "_rule_id",
        "_item_phrase",
        "_canonical_target",
        "_quantity",
        "_registry_token",
        "_nonce",
        "_sealed",
    )

    def __init__(
        self,
        *,
        event_id: str,
        source: str,
        provider_id: str,
        event_kind: str,
        final: bool,
        raw_event_text: str,
        translation_input_text: str,
        expected_intent_original_text: str,
        policy_id: str,
        rule_id: str,
        item_phrase: str,
        canonical_target: str,
        quantity: int,
        registry_token: object,
        nonce: object,
    ):
        object.__setattr__(self, "_event_id", event_id)
        object.__setattr__(self, "_source", source)
        object.__setattr__(self, "_provider_id", provider_id)
        object.__setattr__(self, "_event_kind", event_kind)
        object.__setattr__(self, "_final", final)
        object.__setattr__(self, "_raw_event_text", raw_event_text)
        object.__setattr__(self, "_translation_input_text", translation_input_text)
        object.__setattr__(
            self,
            "_expected_intent_original_text",
            expected_intent_original_text,
        )
        object.__setattr__(self, "_policy_id", policy_id)
        object.__setattr__(self, "_rule_id", rule_id)
        object.__setattr__(self, "_item_phrase", item_phrase)
        object.__setattr__(self, "_canonical_target", canonical_target)
        object.__setattr__(self, "_quantity", quantity)
        object.__setattr__(self, "_registry_token", registry_token)
        object.__setattr__(self, "_nonce", nonce)
        object.__setattr__(self, "_sealed", True)

    def __setattr__(self, name: str, value: object) -> None:
        if getattr(self, "_sealed", False):
            raise AttributeError("generic crafting activation receipt is immutable")
        object.__setattr__(self, name, value)

    @property
    def event_id(self) -> str:
        return self._event_id

    @property
    def source(self) -> str:
        return self._source

    @property
    def provider_id(self) -> str:
        return self._provider_id

    @property
    def event_kind(self) -> str:
        return self._event_kind

    @property
    def final(self) -> bool:
        return self._final

    @property
    def raw_event_text(self) -> str:
        return self._raw_event_text

    @property
    def translation_input_text(self) -> str:
        return self._translation_input_text

    @property
    def expected_intent_original_text(self) -> str:
        return self._expected_intent_original_text

    @property
    def policy_id(self) -> str:
        return self._policy_id

    @property
    def rule_id(self) -> str:
        return self._rule_id

    @property
    def item_phrase(self) -> str:
        return self._item_phrase

    @property
    def canonical_target(self) -> str:
        return self._canonical_target

    @property
    def quantity(self) -> int:
        return self._quantity

    def __repr__(self) -> str:
        return "GenericCraftingDefaultsActivationReceipt(<opaque>)"

    def __reduce__(self):
        raise TypeError("generic_crafting_defaults_activation_is_not_serializable")

    def __reduce_ex__(self, _protocol):
        raise TypeError("generic_crafting_defaults_activation_is_not_serializable")
