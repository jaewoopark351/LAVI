#20260915_kpopmodder: Name vocabulary establishes clarification ownership, never registry or execution capability.
import re

from plugins.Minecraft.fabric.chatclef.input.ownership.item_command.item_command_minecraft_marker_matcher import ItemCommandMinecraftMarkerMatcher
from plugins.Minecraft.fabric.chatclef.input.ownership.item_command.item_command_resolver_family_matcher import ItemCommandResolverFamilyMatcher
from plugins.Minecraft.fabric.chatclef.intent.navigation.find.find_name_repository import FindNameRepository
from plugins.Minecraft.fabric.chatclef.intent.navigation.find.find_target_resolver import FindTargetResolver


class MinecraftRequestEvidence:
    def __init__(self):
        self._markers = ItemCommandMinecraftMarkerMatcher()
        self._families = ItemCommandResolverFamilyMatcher()
        self._names = None

    def matches(self, intent, command_text):
        #20260915_openai: A source-bound explicit native deposit request owns unknown item names too.
        # RejectionRequestBinding already verifies live provenance and exact original-slot replay.
        from plugins.Minecraft.fabric.chatclef.intent.grammar.item.korean_item_list_rule_parser import KoreanItemListRuleParser
        from plugins.Minecraft.fabric.chatclef.intent.chatclef_intent_type import ChatClefIntentType
        if intent.intent_type is ChatClefIntentType.DEPOSIT_ALL:
            native = KoreanItemListRuleParser().parse_native_deposit_all(command_text)
            if native is not None and native.intent_type is ChatClefIntentType.DEPOSIT_ALL:
                return True
        if self._markers.matches(command_text):
            return True
        phrases = [intent.item_phrase] + [row["phrase"] for row in intent.slots.get("items", ())]
        kind = {"scan": "block", "attack": "entity"}.get(intent.intent_type.value, "item")
        for phrase in filter(None, phrases):
            if self._families.match(phrase) or re.fullmatch(r"[a-z0-9_.-]+:[a-z0-9_./-]+", phrase):
                return True
            if self._names is None:
                repository = FindNameRepository()
                self._names = frozenset((k, FindTargetResolver.normalize(name))
                                        for (k, identifier), label in repository.names.items()
                                        for name in (label, *repository.aliases.get((k, identifier), ())))
            if (kind, FindTargetResolver.normalize(phrase)) in self._names:
                return True
        return kind == "block" and bool(re.search(r"(?:^|\s)블록(?:\s|$)", command_text))
