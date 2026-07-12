import importlib
import json
import sys
import unittest
from pathlib import Path

#20260627_kpopmodder: Transformers_LLM is kept disabled/removed in current runtime.

PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))


class TransformersLLMImportTests(unittest.TestCase):#20260626_kpopmodder
    def test_transformers_llm_is_disabled_in_modules_json(self):#20260627_kpopmodder
        modules_path = PROJECT_ROOT / "modules.json"
        modules = json.loads(modules_path.read_text(encoding="utf-8"))

        self.assertIs(modules.get("Transformers_LLM"), False)

    def test_transformers_llm_runtime_classes_are_not_exported(self):#20260627_kpopmodder
        client_module = importlib.import_module(
            "plugins.Transformers_LLM.transformers_llm_core.transformers_client"
        )
        settings_module = importlib.import_module(
            "plugins.Transformers_LLM.transformers_llm_core.transformers_settings"
        )

        self.assertFalse(hasattr(client_module, "TransformersClient"))
        self.assertFalse(hasattr(settings_module, "TransformersSettings"))


if __name__ == "__main__":
    unittest.main()
