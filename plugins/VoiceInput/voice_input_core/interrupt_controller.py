#20260620_kpopmodder: VoiceInput helper modules are grouped under voice_input_core without changing behavior.
import time

from core.event_manager import event_manager, EventType
from core.logger import debug_print


class InterruptController:#20260616_kpopmodder
    def __init__(
        self,
        recorder,
        speaker_service,
        transcribe_audio_callback,
        liveTextbox,
        get_recording_callback,
        get_last_interrupt_check_time_callback,
        set_last_interrupt_check_time_callback,
        get_last_interrupt_time_callback,
        set_last_interrupt_time_callback
    ):
        self.recorder = recorder
        self.speaker_service = speaker_service
        self.transcribe_audio_callback = transcribe_audio_callback
        self.liveTextbox = liveTextbox

        self.get_recording_callback = get_recording_callback
        self.get_last_interrupt_check_time_callback = get_last_interrupt_check_time_callback
        self.set_last_interrupt_check_time_callback = set_last_interrupt_check_time_callback
        self.get_last_interrupt_time_callback = get_last_interrupt_time_callback
        self.set_last_interrupt_time_callback = set_last_interrupt_time_callback

    def handle_ai_speaking(self):
        if not self.get_recording_callback():
            return

        if time.time() - self.get_last_interrupt_check_time_callback() < 1.2:
            time.sleep(0.2)
            return

        self.set_last_interrupt_check_time_callback(time.time())

        debug_print("[VoiceInput] AI speaking branch entered")
        time.sleep(0.15)

        audio = self.recorder.listen(
            timeout=0.5,
            phrase_time_limit=1.5,
            prefix="interrupt"
        )

        if audio is None:
            return

        speaker, score = self.speaker_service.identify(
            audio,
            prefix="interrupt"
        )

        self.liveTextbox.print(f"Interrupt speaker: {speaker} / score={score:.3f}")
        debug_print(f"[SpeakerRecognition] speaker={speaker}, score={score:.3f}")

        if speaker == "user" and score >= 0.65:
            if time.time() - self.get_last_interrupt_time_callback() < 2:
                return

            self.set_last_interrupt_time_callback(time.time())

            self.liveTextbox.print(
                "User voice detected during AI speech. Interrupting TTS."
            )

            event_manager.trigger(EventType.INTERRUPT)
            time.sleep(0.2)

            self.transcribe_audio_callback(
                audio,
                prefix="interrupt"
            )

        time.sleep(0.8)
