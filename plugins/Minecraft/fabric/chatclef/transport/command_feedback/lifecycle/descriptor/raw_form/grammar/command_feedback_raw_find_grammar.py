#20260914_kpopmodder: Decode FIND's own closed namespaced/player grammar.
from plugins.Minecraft.fabric.chatclef.intent.find.find_command_compiler import FindCommandCompiler


class CommandFeedbackRawFindGrammar:
    GRAMMAR_IDS = ("find_target",)

    def decode(self, grammar_id, command_name, arguments):
        if grammar_id != "find_target" or command_name != "find" or len(arguments) not in {2, 3}:
            return None
        mode = arguments[2] if len(arguments) == 3 else "report"
        try:
            FindCommandCompiler.compile_slots(arguments[0], arguments[1], mode)
        except (TypeError, ValueError):
            return None
        return "find_report" if mode == "report" else "find_approach"
