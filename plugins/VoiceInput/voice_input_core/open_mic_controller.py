#20260620_kpopmodder: VoiceInput helper modules are grouped under voice_input_core without changing behavior.
from core.global_state import global_state, GlobalKeys
from core.logger import debug_print, log_print


class OpenMicController:#20260616_kpopmodder
    def __init__(
        self,
        recorder,
        speaker_service,
        transcribe_audio_callback,
        liveTextbox,
        min_user_speaker_score=0.58,
    ):
        self.recorder = recorder
        self.speaker_service = speaker_service
        self.transcribe_audio_callback = transcribe_audio_callback
        self.liveTextbox = liveTextbox
        self.min_user_speaker_score = min_user_speaker_score#20260707_kpopmodder: Suppress low-confidence ambient clips before Whisper.

    def handle_open_mic(self):
        audio = self.recorder.listen(
            timeout=3,
            phrase_time_limit=10,
            prefix="normal"
        )

        if audio is None:
            self.liveTextbox.print(
                "normal microphone audio not captured. Whisper skipped."
            )#20260625_kpopmodder
            log_print(
                "[OpenMicController normal] "
                "microphone audio not captured. Whisper skipped."
            )#20260625_kpopmodder
            return

        if global_state.get_value(GlobalKeys.IS_SONG_PLAYING, False):
            log_print(
                "[OpenMicController normal] "
                "song playing state blocked microphone audio."
            )#20260628_kpopmodder
            return

        if global_state.get_value(GlobalKeys.IS_AI_SPEAKING, False):
            self.liveTextbox.print("AI voice leaked into mic. Ignored.")
            log_print(
                "[OpenMicController normal] "
                "AI speaking state blocked microphone audio."
            )#20260625_kpopmodder
            return

        speaker, score = self.speaker_service.identify(
            audio,
            prefix="normal"
        )

        self.liveTextbox.print(f"Speaker detected: {speaker} / score={score:.3f}")
        debug_print(f"[SpeakerRecognition] speaker={speaker}, score={score:.3f}")

        if speaker == "ai":
            self.liveTextbox.print("AI voice detected. Ignored.")
            log_print(
                "[OpenMicController normal] "
                f"speaker filter blocked AI voice: score={score:.3f}"
            )#20260625_kpopmodder
            return

        if speaker == "unknown" and 0.0 < score < self.min_user_speaker_score:
            self.liveTextbox.print(
                f"Unknown low-confidence speaker ignored: score={score:.3f}"
            )
            log_print(
                "[OpenMicController normal] "
                f"speaker filter blocked unknown voice/noise: "
                f"score={score:.3f}, threshold={self.min_user_speaker_score:.3f}"
            )#20260707_kpopmodder
            return

        self.liveTextbox.print("recording complete, sending to whisper")
        log_print(
            "[OpenMicController normal] recording complete, sending to whisper"
        )#20260625_kpopmodder

        self.transcribe_audio_callback(
            audio,
            prefix="normal"
        )
