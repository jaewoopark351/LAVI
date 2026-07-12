#20260703_kpopmodder: Verify OpenAI key precedence and secret-safe settings behavior.
import os
import sys
import unittest
from pathlib import Path
from unittest import mock


PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))


class ChatGPTOpenAITests(unittest.TestCase):
    def test_get_client_prefers_environment_api_key(self):
        from plugins.ChatGPT_OpenAI.ChatGPT_OpenAI import ChatGPT_OpenAI

        plugin = ChatGPT_OpenAI()
        plugin.init()
        plugin.api_key = "config-key"

        with mock.patch.dict(
            os.environ,
            {"OPENAI_API_KEY": "env-key"},
            clear=False,
        ), mock.patch(
            "plugins.ChatGPT_OpenAI.ChatGPT_OpenAI.OpenAI",
        ) as openai_mock:
            plugin._get_client()

        openai_mock.assert_called_once_with(api_key="env-key")

    def test_api_key_is_not_saved_without_explicit_option(self):
        from plugins.ChatGPT_OpenAI.ChatGPT_OpenAI import ChatGPT_OpenAI
        import plugins.ChatGPT_OpenAI.ChatGPT_OpenAI as chatgpt_module

        plugin = ChatGPT_OpenAI()
        plugin.init()

        with mock.patch.object(
            chatgpt_module.config_manager,
            "save_config",
        ) as save_mock, mock.patch.object(
            chatgpt_module.config_manager,
            "remove_config",
        ) as remove_mock:
            plugin.update_api_key("secret-key", False)

        self.assertNotIn(
            mock.call("ChatGPT_OpenAI", "api_key", "secret-key"),
            save_mock.mock_calls,
        )
        remove_mock.assert_called_once_with("ChatGPT_OpenAI", "api_key")

    def test_api_key_is_not_logged_when_client_is_created(self):
        from plugins.ChatGPT_OpenAI.ChatGPT_OpenAI import ChatGPT_OpenAI

        plugin = ChatGPT_OpenAI()
        plugin.init()
        plugin.api_key = "secret-key"

        with mock.patch.dict(
            os.environ,
            {},
            clear=True,
        ), mock.patch(
            "plugins.ChatGPT_OpenAI.ChatGPT_OpenAI.OpenAI",
        ), mock.patch(
            "plugins.ChatGPT_OpenAI.ChatGPT_OpenAI.log_print",
        ) as log_mock:
            plugin._get_client()

        logged = "\n".join(str(call) for call in log_mock.mock_calls)
        self.assertNotIn("secret-key", logged)


if __name__ == "__main__":
    unittest.main()
