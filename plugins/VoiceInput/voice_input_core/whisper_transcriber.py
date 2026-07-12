#20260620_kpopmodder: VoiceInput helper modules are grouped under voice_input_core without changing behavior.
import os
import tempfile

from core.logger import log_print


class WhisperTranscriber:#20260616_kpopmodder
    def __init__(
        self,
        stt_backend,
        filter_list,
        liveTextbox,
        language_callback=None,
    ):
        self.stt_backend = stt_backend
        self.filter_list = filter_list
        self.liveTextbox = liveTextbox
        self.language_callback = language_callback

    def _language(self):#20260707_kpopmodder: Resolve STT language lazily so UI changes affect both mic modes.
        if self.language_callback is None:
            return "ko"

        try:
            language = self.language_callback()
        except Exception as e:
            log_print(f"[WhisperTranscriber] language callback error: {e}")
            return "ko"

        return language or "ko"

    def transcribe(self, audio, prefix="normal"):
        temp_path = None

        try:
            with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as temp_file:
                temp_path = temp_file.name
                wav_bytes = audio.get_wav_data()
                temp_file.write(wav_bytes)
                log_print(
                    f"[WhisperTranscriber {prefix}] temp wav "
                    f"bytes={len(wav_bytes)}, file={os.path.basename(temp_path)}"
                )#20260625_kpopmodder

            self.liveTextbox.print(f"{prefix} whisper start")#20260625_kpopmodder
            log_print(f"[WhisperTranscriber {prefix}] whisper start")#20260625_kpopmodder
            result = self.stt_backend.transcribe(
                audio_path=temp_path,
                language=self._language(),
                task="transcribe",
            )
            language = getattr(result, "language", "unknown")#20260625_kpopmodder
            language_probability = getattr(
                result,
                "language_probability",
                0.0,
            )#20260625_kpopmodder
            language_probability_available = getattr(
                result,
                "language_probability_available",
                True,
            )#20260707_kpopmodder
            avg_logprob_available = getattr(
                result,
                "avg_logprob_available",
                True,
            )#20260707_kpopmodder
            no_speech_prob_available = getattr(
                result,
                "no_speech_prob_available",
                True,
            )#20260707_kpopmodder
            self.liveTextbox.print(
                f"{prefix} whisper done: "
                f"language={language}, prob={language_probability:.3f}"
            )#20260625_kpopmodder
            log_print(
                f"[WhisperTranscriber {prefix}] whisper done: "
                f"language={language}, prob={language_probability:.3f}"
            )#20260625_kpopmodder

            if (
                not language_probability_available
                or not avg_logprob_available
                or not no_speech_prob_available
            ):
                log_print(
                    f"[WhisperTranscriber {prefix}] "
                    "backend confidence metrics unavailable; "
                    "skipping language_probability/avg_logprob/"
                    "no_speech_prob filters and relying on "
                    "empty text, speech duration, hallucination filter list, "
                    "and speaker score filters."
                )#20260707_kpopmodder

            if language_probability_available and language_probability < 0.5:
                self.liveTextbox.print(
                    f"{prefix} low language probability ignored: "#20260625_kpopmodder
                    f"language={language}, prob={language_probability:.3f}"
                )
                log_print(
                    f"[WhisperTranscriber {prefix}] "
                    f"low language probability ignored: "
                    f"language={language}, prob={language_probability:.3f}"
                )#20260625_kpopmodder
                return None

            valid_segments = []
            total_speech_duration = 0.0

            for segment in result.segments:
                text = segment.text.strip()

                if text == "":
                    self.liveTextbox.print(
                        f"{prefix} empty Whisper segment ignored."
                    )#20260625_kpopmodder
                    log_print(
                        f"[WhisperTranscriber {prefix}] "
                        f"empty Whisper segment ignored."
                    )#20260625_kpopmodder
                    continue

                avg_logprob = getattr(segment, "avg_logprob", 0.0)
                no_speech_prob = getattr(segment, "no_speech_prob", 0.0)
                segment_avg_logprob_available = getattr(
                    segment,
                    "avg_logprob_available",
                    avg_logprob_available,
                )#20260707_kpopmodder
                segment_no_speech_prob_available = getattr(
                    segment,
                    "no_speech_prob_available",
                    no_speech_prob_available,
                )#20260707_kpopmodder
                duration = max(0.0, segment.end - segment.start)
                avg_logprob_text = (
                    f"{avg_logprob:.3f}"
                    if segment_avg_logprob_available
                    else f"unavailable(default={avg_logprob:.3f})"
                )#20260707_kpopmodder
                no_speech_prob_text = (
                    f"{no_speech_prob:.3f}"
                    if segment_no_speech_prob_available
                    else f"unavailable(default={no_speech_prob:.3f})"
                )#20260707_kpopmodder
                log_print(
                    f"[WhisperTranscriber {prefix}] segment: "
                    f"text={text!r}, avg_logprob={avg_logprob_text}, "
                    f"no_speech_prob={no_speech_prob_text}, "
                    f"duration={duration:.3f}s"
                )#20260625_kpopmodder

                if segment_avg_logprob_available and avg_logprob < -1.0:
                    self.liveTextbox.print(
                        f"{prefix} low confidence STT segment ignored: "#20260625_kpopmodder
                        f"avg_logprob={avg_logprob:.3f}, text={text!r}"
                    )
                    log_print(
                        f"[WhisperTranscriber {prefix}] "
                        f"low confidence STT segment ignored: "
                        f"avg_logprob={avg_logprob:.3f}, text={text!r}"
                    )#20260625_kpopmodder
                    continue

                if segment_no_speech_prob_available and no_speech_prob > 0.6:
                    self.liveTextbox.print(
                        f"{prefix} no speech STT segment ignored: "#20260625_kpopmodder
                        f"no_speech_prob={no_speech_prob:.3f}, text={text!r}"
                    )
                    log_print(
                        f"[WhisperTranscriber {prefix}] "
                        f"no speech STT segment ignored: "
                        f"no_speech_prob={no_speech_prob:.3f}, text={text!r}"
                    )#20260625_kpopmodder
                    continue

                valid_segments.append(text)
                total_speech_duration += duration

            if not valid_segments:
                self.liveTextbox.print(
                    f"{prefix} empty STT result ignored: no valid segments."
                )#20260625_kpopmodder
                log_print(
                    f"[WhisperTranscriber {prefix}] "
                    f"empty STT result ignored: no valid segments."
                )#20260625_kpopmodder
                return None

            if total_speech_duration < 0.15:
                self.liveTextbox.print(#20260625_kpopmodder
                    f"{prefix} too short STT ignored: "
                    f"speech_duration={total_speech_duration:.3f}s"
                )
                log_print(
                    f"[WhisperTranscriber {prefix}] "
                    f"too short STT ignored: "
                    f"speech_duration={total_speech_duration:.3f}s"
                )#20260625_kpopmodder
                return None

            transcribed_text = " ".join(valid_segments).strip()

            if transcribed_text == "":
                self.liveTextbox.print(
                    f"{prefix} empty STT result ignored."
                )#20260625_kpopmodder
                log_print(
                    f"[WhisperTranscriber {prefix}] "
                    f"empty STT result ignored."
                )#20260625_kpopmodder
                return None

            log_print(
                f"looking for {transcribed_text.strip().lower()} in {self.filter_list}"
            )

            if transcribed_text.strip().lower() in self.filter_list:
                self.liveTextbox.print(f"Input {transcribed_text} was filtered.")
                log_print(
                    f"[WhisperTranscriber {prefix}] "
                    f"filtered STT ignored: text={transcribed_text!r}"
                )#20260625_kpopmodder
                return None

            log_print(
                f"[WhisperTranscriber {prefix}] "
                f"accepted STT: text={transcribed_text!r}"
            )#20260625_kpopmodder
            return transcribed_text

        except Exception as e:
            log_print(f"[WhisperTranscriber {prefix} STT error] {e}")
            self.liveTextbox.print(f"{prefix} STT error: {e}")
            return None

        finally:
            if temp_path:
                try:
                    os.remove(temp_path)
                except Exception:
                    pass
