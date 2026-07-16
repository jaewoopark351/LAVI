#20260717_kpopmodder: Regression test for removing the root Input.py facade.
import unittest
from pathlib import Path


class InputCoreImportTests(unittest.TestCase):
    def test_input_core_exports_input_component(self):
        from input_core import Input
        from input_core.input_component import Input as ComponentInput

        self.assertIs(ComponentInput, Input)

    def test_root_input_facade_is_removed(self):
        project_root = Path(__file__).resolve().parents[1]

        self.assertFalse((project_root / "Input.py").exists())


if __name__ == "__main__":
    unittest.main()
