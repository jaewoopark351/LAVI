#20260706_kpopmodder: Added this helper to keep ScreenVision display hotkey registration outside the facade.
from core.logger import log_print


class ScreenVisionDisplayHotkey:
    #20260706_kpopmodder: Registration is isolated, while the public ScreenVision callback remains unchanged.
    def __init__(
        self,
        keyboard_module,
        hotkey_provider,
        callback,
        status_callback,
    ):
        self.keyboard_module = keyboard_module
        self.hotkey_provider = hotkey_provider
        self.callback = callback
        self.status_callback = status_callback

    def register(self):
        keyboard = self.keyboard_module
        hotkey = self.hotkey_provider()
        if keyboard is None:
            self.status_callback(
                "[ScreenVision] Display hotkey disabled: keyboard module not available"
            )
            return None

        try:
            handle = keyboard.add_hotkey(hotkey, self.callback)
            self.status_callback(
                f"[ScreenVision] Display hotkey registered: {hotkey}"
            )
            return handle
        except Exception as e:
            self.status_callback(
                f"[ScreenVision] Display hotkey disabled: {e}"
            )
            return None

    def unregister(self, handle):
        keyboard = self.keyboard_module
        if keyboard is None or handle is None:
            return None

        try:
            keyboard.remove_hotkey(handle)
        except Exception as e:
            log_print(f"[ScreenVision] Display hotkey unregister failed: {e}")
        return None
