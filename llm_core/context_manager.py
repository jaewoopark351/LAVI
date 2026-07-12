import os


class LLMContextManager:#20260617_kpopmodder
    def __init__(self, context_file_path="ai_character_system_prompt.txt"):
        self.context_file_path = context_file_path
        self.system_prompt_text = ""

        self.ensure_context_file()

    def ensure_context_file(self):
        if os.path.exists(self.context_file_path):
            return

        with open(self.context_file_path, "w", encoding="utf-8") as file:
            file.write("")

    def load_content(self):
        with open(self.context_file_path, "r", encoding="utf-8") as file:
            self.system_prompt_text = file.read()

        return self.system_prompt_text

    def update_file(self, new_content):
        if new_content is None:
            new_content = ""

        self.system_prompt_text = str(new_content)

        with open(self.context_file_path, "w", encoding="utf-8") as file:
            file.write(self.system_prompt_text)
