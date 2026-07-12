#20260620_kpopmodder: VoiceInput helper modules are grouped under voice_input_core without changing behavior.
from core.event_manager import event_manager, EventType
from core.logger import log_print


try:#20260618_kpopmodder
    import keyboard
except Exception:
    keyboard = None


class VoiceInputHotkeyController:#20260618_kpopmodder
    def __init__(self, key_to_bind, liveTextbox):
        self.key_to_bind = key_to_bind
        self.liveTextbox = liveTextbox
        self.hotkey_handle = None

    def register(self):
        if keyboard is None:
            self.liveTextbox.print(
                "Keyboard hotkey disabled: keyboard module not available"
            )
            return

        try:
            self.hotkey_handle = keyboard.add_hotkey(
                self.key_to_bind,
                self.on_interrupt_key,
            )
            self.liveTextbox.print(
                f"Interrupt hotkey registered: {self.key_to_bind}"
            )
        except Exception as e:
            self.liveTextbox.print(f"Keyboard hotkey disabled: {e}")

    def on_interrupt_key(self):
        log_print(f"You pressed the '{self.key_to_bind}' key!")
        event_manager.trigger(EventType.INTERRUPT)

    def shutdown(self):#20260623_kpopmodder
        if keyboard is None or self.hotkey_handle is None:
            return

        try:
            keyboard.remove_hotkey(self.hotkey_handle)
        except Exception as e:
            log_print(f"[VoiceInput] hotkey unregister failed: {e}")
        finally:
            self.hotkey_handle = None
