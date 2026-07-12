#20260622_kpopmodder: Verify canonical ui_core imports after removing root liveTextbox.py.
import unittest


class UiCoreImportTests(unittest.TestCase):
    def test_live_textbox_exports_canonical_class(self):
        from ui_core.live_textbox import LiveTextbox
        from ui_core import LiveTextbox as PackageLiveTextbox

        self.assertIs(LiveTextbox, PackageLiveTextbox)

if __name__ == "__main__":
    unittest.main()
