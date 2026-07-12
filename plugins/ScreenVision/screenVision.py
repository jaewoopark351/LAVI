# from collections import Counter#20260621_kpopmodder: 관찰 결과 판정 로직과 함께 observation_policy.py로 이동했다.
# from difflib import SequenceMatcher#20260621_kpopmodder: 중복 판정 책임을 ObservationPolicy로 이동했다.
import threading
import time#20260621_kpopmodder

import gradio as gr

from core.event_manager import event_manager, EventType
from core.gpu_device_manager import gpu_device_manager#20260627_kpopmodder
from core.global_state import global_state, GlobalKeys#20260621_kpopmodder
from ui_core.live_textbox import LiveTextbox
from core.logger import log_print

try:#20260621_kpopmodder
    import keyboard
except Exception:
    keyboard = None

from .screen_vision_core.auto_watch_controller import AutoWatchController
from .screen_vision_core.observation_policy import ObservationPolicy
from .screen_vision_core.observation_processor import (
    ScreenObservationProcessor,
)
from .screen_vision_core.observation_dispatch import (
    ScreenObservationDispatchHelper,
)
from .screen_vision_core.observation_decision_reporter import (
    ScreenObservationDecisionReporter,
)
from .screen_vision_core.observation_prompt_builder import (
    ScreenObservationPromptBuilder,
)
from .screen_vision_core.observation_text_cleaner import (
    ScreenObservationTextCleaner,
)
from .screen_vision_core.observation_result_flow import (
    ScreenObservationResultFlow,
)
from .screen_vision_core.observation_analysis import (
    ScreenObservationAnalysisHelper,
)
from .screen_vision_core.auto_watch_observation_flow import (
    ScreenAutoWatchObservationFlow,
)
from .screen_vision_core.observation_memory_dispatch import (
    ScreenObservationMemoryDispatch,
)
from .screen_vision_core.display_hotkey import ScreenVisionDisplayHotkey
from .screen_vision_core.latest_display_runtime import (
    ScreenVisionLatestDisplayRuntime,
)
from .screen_vision_core.screen_capture import ScreenCapture


class ScreenVision:#20260620_kpopmodder
    AUTO_WATCH_MIN_SEND_INTERVAL_SECONDS = 12.0#20260621_kpopmodder#자동 화면 설명은 TTS 폭주 방지를 위해 최소 간격을 둔다.
    AUTO_WATCH_DUPLICATE_SIMILARITY = 0.96#20260621_kpopmodder#비슷한 화면 설명 반복 차단
    AUTO_WATCH_DEFAULT_ENABLED = True#20260629_kpopmodder: Start Auto Watch by default when the app starts.
    #def __init__(self):#20260621_kpopmodder
    def __init__(self, memory_store=None):#20260621_kpopmodder
        self.memory_store = memory_store#20260621_kpopmodder
        self.output_event_listeners = []
        self.live_textbox = LiveTextbox()
        self.capture_service = ScreenCapture(
            max_width=1280,
            max_height=720,
        )
        self._vision_analyzer = None
        self.observation_policy = ObservationPolicy(
            duplicate_similarity=self.AUTO_WATCH_DUPLICATE_SIMILARITY,
        )#20260621_kpopmodder
        #20260621_kpopmodder: 자동/최신/수동 관찰의 중복 판정 흐름을 한곳에서 유지한다.
        self.observation_processor = ScreenObservationProcessor(
            self.observation_policy,
        )
        self.observation_dispatcher = ScreenObservationDispatchHelper(
            self.memory_store,
        )
        self.observation_decision_reporter = (
            ScreenObservationDecisionReporter(
                self._record_raw_screen_event,
                self.live_textbox,
            )
        )
        self.observation_prompt_builder = ScreenObservationPromptBuilder()
        self.observation_text_cleaner = ScreenObservationTextCleaner(
            self.observation_policy,
        )
        self.observation_result_flow = self._create_observation_result_flow()
        self.observation_analysis_helper = self._create_observation_analysis_helper()
        self.observation_memory_dispatch = ScreenObservationMemoryDispatch(self)#20260706_kpopmodder
        self.auto_watch_observation_flow = ScreenAutoWatchObservationFlow(self)#20260706_kpopmodder
        self.analysis_lock = threading.Lock()

        self.latest_display_interval_seconds = 10.0#20260621_kpopmodder#자동 최신 화면 설명은 TTS보다 느리게 돈다.
        self.last_auto_observation = ""#20260621_kpopmodder
        self.last_auto_sent_time = 0.0#20260621_kpopmodder

        self.last_screen_observation = ""#20260622_kpopmodder: Auto Watch 관찰 결과를 LLM/TTS로 바로 보내지 않고 저장한다.
        self.last_screen_observation_source = ""#20260622_kpopmodder
        self.last_screen_observation_time = 0.0#20260622_kpopmodder

        self.latest_display_stop_event = threading.Event()#20260621_kpopmodder
        self.latest_display_thread = None#20260621_kpopmodder
        self.display_hotkey = "ctrl+shift+alt+d"#20260621_kpopmodder
        self.display_hotkey_handle = None#20260623_kpopmodder
        self.display_hotkey_runtime = ScreenVisionDisplayHotkey(
            keyboard_module=keyboard,
            hotkey_provider=lambda: self.display_hotkey,
            callback=self.on_display_hotkey,
            status_callback=self.live_textbox.print,
        )
        self.latest_display_runtime = ScreenVisionLatestDisplayRuntime(self)
        self._shutdown = False#20260623_kpopmodder
        self.ignore_next_own_interrupt = False#20260621_kpopmodder: ScreenVision이 직접 발생시킨 interrupt는 자기 분석을 끊지 않게 한다.

        self.auto_watch_controller = AutoWatchController(
            capture_callback=self.capture_service.capture,
            difference_callback=self.capture_service.calculate_difference,
            change_callback=self.handle_auto_watch_change,
            status_callback=self.live_textbox.print,
        )

        self._interrupt_subscription = event_manager.subscribe(
            EventType.INTERRUPT,
            self.handle_interrupt,
        )

        self.register_display_hotkey()#20260621_kpopmodder

    def create_ui(self):
        with gr.Tab("Screen Vision"):
            gr.Markdown(
                "PC 화면을 메모리에서 한 장 캡처해 로컬 비전 모델로 분석합니다. "#20260621_kpopmodder
                "스크린샷 파일은 저장하지 않습니다."
            )

            self.question_input = gr.Textbox(
                label="화면에 대해 물어볼 내용",#20260621_kpopmodder
                #value="화면에서 무엇이 보이는지 알려줘.",#20260621_kpopmodder
                value=self._build_screen_question(),#20260621_kpopmodder
                lines=2,
            )
            self.look_button = gr.Button("Look at screen")
            self.auto_watch_checkbox = gr.Checkbox(
                label="Auto Watch",
                value=self.AUTO_WATCH_DEFAULT_ENABLED,
                info=(
                    # "1초마다 화면 변화를 확인하고, 큰 변화가 감지되면 "#20260621_kpopmodder
                    # "최대 10초에 한 번 분석합니다."#20260621_kpopmodder
                    #"7초마다 기존 화면 설명 큐를 끊고, "#20260621_kpopmodder
                    #"현재 화면을 다시 캡처해 최신 화면만 설명합니다."#20260621_kpopmodder
                    "1초마다 화면 변화를 확인하고, 큰 변화가 감지되면 "#20260621_kpopmodder
                    #"최소 12초 간격으로만 LLM/TTS에 전달합니다."#20260622_kpopmodder
                    "최소 12초 간격으로 화면 변화를 분석하고, LLM/TTS에는 바로 전달하지 않고 최근 화면 관찰 기록으로 저장합니다."#20260622_kpopmodder
                ),
            )
            self.preview = gr.Image(
                label="Captured screen",
                type="pil",
                interactive=False,
            )
            self.console_box = self.live_textbox.create_ui(
                lines=8,
                max_lines=20,
                label="Screen Vision Console",
            )

            self.look_button.click(
                fn=self.look_at_screen,
                inputs=[self.question_input],
                outputs=[self.console_box, self.preview],
            )
            self.auto_watch_checkbox.change(
                fn=self.set_auto_watch,
                inputs=[self.auto_watch_checkbox],
                outputs=[self.console_box],
                queue=False,
            )

            self.console_timer = gr.Timer(1.0)
            self.console_timer.tick(
                fn=self.live_textbox.get_text,
                outputs=[self.console_box],
                show_progress=False,
                queue=False,
            )
            if self.AUTO_WATCH_DEFAULT_ENABLED:
                #20260629_kpopmodder: Default checked state must also start the watcher thread.
                self.set_auto_watch(True)

    def add_output_event_listener(self, function):
        if function in self.output_event_listeners:
            return
        self.output_event_listeners.append(function)

    def _get_observation_dispatcher(self):
        dispatcher = getattr(self, "observation_dispatcher", None)
        if dispatcher is None:
            dispatcher = ScreenObservationDispatchHelper(
                getattr(self, "memory_store", None),
            )
            self.observation_dispatcher = dispatcher
        return dispatcher

    def _get_observation_decision_reporter(self):
        reporter = getattr(self, "observation_decision_reporter", None)
        if reporter is None:
            reporter = ScreenObservationDecisionReporter(
                self._record_raw_screen_event,
                self.live_textbox,
            )
            self.observation_decision_reporter = reporter
        return reporter

    def _get_observation_prompt_builder(self):
        builder = getattr(self, "observation_prompt_builder", None)
        if builder is None:
            builder = ScreenObservationPromptBuilder()
            self.observation_prompt_builder = builder
        return builder

    def _get_observation_text_cleaner(self):
        cleaner = getattr(self, "observation_text_cleaner", None)
        if cleaner is None:
            cleaner = ScreenObservationTextCleaner(self.observation_policy)
            self.observation_text_cleaner = cleaner
        return cleaner

    def _create_observation_result_flow(self):
        return ScreenObservationResultFlow(
            normalize_callback=self.observation_processor.normalize,
            evaluate_callback=self._evaluate_observation,
            record_raw_event_callback=self._record_raw_screen_event,
            record_decision_callback=self._record_observation_decision,
            record_ignored_callback=self._record_ignored_observation,
            report_ignored_callback=self._report_ignored_observation,
            report_accepted_callback=self._report_accepted_observation,
            live_textbox=self.live_textbox,
        )

    def _get_observation_result_flow(self):
        flow = getattr(self, "observation_result_flow", None)
        if flow is None:
            flow = self._create_observation_result_flow()
            self.observation_result_flow = flow
        return flow

    def _create_observation_analysis_helper(self):
        return ScreenObservationAnalysisHelper(
            analyze_callback=self._analyze_with_vision,
            result_flow_provider=self._get_observation_result_flow,
        )

    def _get_observation_analysis_helper(self):
        helper = getattr(self, "observation_analysis_helper", None)
        if helper is None:
            helper = self._create_observation_analysis_helper()
            self.observation_analysis_helper = helper
        return helper

    def _get_display_hotkey_runtime(self):
        runtime = getattr(self, "display_hotkey_runtime", None)
        if runtime is None:
            runtime = ScreenVisionDisplayHotkey(
                keyboard_module=keyboard,
                hotkey_provider=lambda: self.display_hotkey,
                callback=self.on_display_hotkey,
                status_callback=self.live_textbox.print,
            )
            self.display_hotkey_runtime = runtime
        return runtime

    def _get_latest_display_runtime(self):
        runtime = getattr(self, "latest_display_runtime", None)
        if runtime is None:
            runtime = ScreenVisionLatestDisplayRuntime(self)
            self.latest_display_runtime = runtime
        return runtime

    def _get_auto_watch_observation_flow(self):
        flow = getattr(self, "auto_watch_observation_flow", None)
        if flow is None:
            flow = ScreenAutoWatchObservationFlow(self)
            self.auto_watch_observation_flow = flow
        return flow

    def _get_vision_analyzer(self):
        analyzer = getattr(self, "_vision_analyzer", None)
        if analyzer is None:
            try:
                from .screen_vision_core.vision_analyzer import VisionAnalyzer
            except Exception as e:
                log_print(
                    "[ScreenVision] VisionAnalyzer dependency unavailable: "
                    f"{type(e).__name__}: {e}"
                )
                raise

            analyzer = VisionAnalyzer()
            self._vision_analyzer = analyzer
        return analyzer

    def _analyze_with_vision(self, image, question, **analyze_kwargs):
        return self._get_vision_analyzer().analyze(
            image=image,
            question=question,
            **analyze_kwargs,
        )

    def _request_vision_interrupt(self):
        analyzer = getattr(self, "_vision_analyzer", None)
        if analyzer is None:
            return
        analyzer.request_interrupt()

    def remove_output_event_listener(self, function):
        removed = False
        while function in self.output_event_listeners:
            self.output_event_listeners.remove(function)
            removed = True
        return removed

    def send_output(self, output):
        for listener in list(self.output_event_listeners):
            try:
                listener(output)
            except Exception as e:
                log_print(f"[ScreenVision] output listener failed: {e}")#20260703_kpopmodder

    # def handle_interrupt(self):#20260621_kpopmodder
    #     self._request_vision_interrupt()

    def handle_interrupt(self):#20260621_kpopmodder
        if getattr(self, "ignore_next_own_interrupt", False):#20260621_kpopmodder
            self.ignore_next_own_interrupt = False#20260623_kpopmodder: Consume this one-shot guard inside the handler too.
            log_print("[ScreenVision] own interrupt ignored")
            return

        self._request_vision_interrupt()

    def register_display_hotkey(self):#20260621_kpopmodder
        self.display_hotkey_handle = (
            self._get_display_hotkey_runtime().register()
        )

    def unregister_display_hotkey(self):#20260623_kpopmodder
        self.display_hotkey_handle = (
            self._get_display_hotkey_runtime().unregister(
                self.display_hotkey_handle,
            )
        )

    def shutdown(self):
        if self._shutdown:
            return

        self._shutdown = True
        #20260623_kpopmodder: Match interrupt subscription and global hotkey with shutdown cleanup.
        if self._interrupt_subscription is not None:
            self._interrupt_subscription.unsubscribe()
            self._interrupt_subscription = None

        try:
            self._request_vision_interrupt()
        except Exception as e:
            log_print(f"[ScreenVision shutdown] analyzer interrupt error: {e}")

        try:
            self.auto_watch_controller.shutdown()
        except KeyboardInterrupt:
            log_print("[ScreenVision shutdown] auto watch stop skipped during Ctrl+C shutdown.")#20260630_kpopmodder
        except Exception as e:
            log_print(f"[ScreenVision shutdown] auto watch stop error: {e}")

        self._get_latest_display_runtime().shutdown()

        self.unregister_display_hotkey()
        self.output_event_listeners.clear()

    def on_display_hotkey(self):#20260621_kpopmodder
        log_print(f"[ScreenVision] Display hotkey pressed: {self.display_hotkey}")
        self.live_textbox.print(
            "[ScreenVision] Display hotkey pressed. Force latest screen."
        )

        thread = threading.Thread(
            target=self.force_latest_screen,
            kwargs={
                "source": "단축키 최신 화면 관찰",
                "block_for_lock": True,
            },
            daemon=True,
        )
        thread.start()

    def _is_no_important_change(self, observation):#20260621_kpopmodder
        #20260621_kpopmodder: 기존 호출 구조는 유지하고 판정 책임만 정책 클래스로 위임한다.
        return self.observation_policy.is_no_important_change(observation)

    def _normalize_observation(self, observation):#20260621_kpopmodder
        return self._get_observation_text_cleaner().normalize(observation)

    def _normalize_decision_text(self, observation):#20260621_kpopmodder
        return self._get_observation_text_cleaner().normalize_for_decision(
            observation,
        )

    def _is_broken_observation(self, observation):#20260621_kpopmodder
        return self.observation_policy.is_broken(observation)

    def _is_duplicate_auto_observation(self, observation):#20260621_kpopmodder
        return self.observation_policy.is_duplicate(
            self.last_auto_observation,
            observation,
        )

    def _record_raw_screen_event(#20260621_kpopmodder
        self,
        event_type,
        value,
        source="ScreenVision",
        metadata=None,
    ):#20260621_kpopmodder
        """ScreenVision 원본 관찰값을 raw_events.jsonl에 저장한다.

        long_term_memory 승격이 아니라, 나중에 요약/분석할 원본 로그다.
        broken/noise, duplicate/similar로 버려진 값도 기록할 수 있다.
        """
        self.observation_memory_dispatch.record_raw_screen_event(
            event_type=event_type,
            value=value,
            source=source,
            metadata=metadata,
        )

    def _can_send_auto_observation_to_llm(self):#20260621_kpopmodder
        elapsed = time.time() - self.last_auto_sent_time
        if elapsed < self.AUTO_WATCH_MIN_SEND_INTERVAL_SECONDS:
            remaining = self.AUTO_WATCH_MIN_SEND_INTERVAL_SECONDS - elapsed
            self.live_textbox.print(
                f"[ScreenVision] Auto LLM/TTS skipped: cooldown {remaining:.1f}s left."
            )
            return False
        return True

    def _is_ai_speaking(self):#20260621_kpopmodder
        try:
            return bool(global_state.get_value(GlobalKeys.IS_AI_SPEAKING, False))
        except Exception:
            return False

    def _evaluate_observation(
        self,
        raw_observation,
        reject_no_important_change=False,
        check_duplicate=False,
        check_ai_speaking=False,
        check_cooldown=False,
    ):#20260621_kpopmodder
        return self.observation_processor.evaluate(
            raw_observation=raw_observation,
            previous_observation=self.last_auto_observation,
            reject_no_important_change=reject_no_important_change,
            check_duplicate=check_duplicate,
            is_ai_speaking_callback=(
                self._is_ai_speaking
                if check_ai_speaking
                else None
            ),
            can_send_callback=(
                self._can_send_auto_observation_to_llm
                if check_cooldown
                else None
            ),
        )

    def _record_ignored_observation(
        self,
        decision,
        raw_observation,
        event_source,
        question,
        metadata=None,
    ):#20260621_kpopmodder
        #20260705_kpopmodder: Keep facade method stable; helper owns decision metadata assembly.
        self._get_observation_decision_reporter().record_ignored(
            decision=decision,
            raw_observation=raw_observation,
            event_source=event_source,
            question=question,
            last_auto_observation=self.last_auto_observation,
            metadata=metadata,
        )

    #20260623_kpopmodder: Record accept/reject details before changing filter sensitivity.
    def _record_observation_decision(
        self,
        decision,
        raw_observation,
        event_source,
        question,
        metadata=None,
    ):#20260623_kpopmodder
        #20260705_kpopmodder: Keep facade method stable; helper owns decision metadata assembly.
        self._get_observation_decision_reporter().record_decision(
            decision=decision,
            raw_observation=raw_observation,
            event_source=event_source,
            question=question,
            last_auto_observation=self.last_auto_observation,
            metadata=metadata,
        )

    def _format_decision_detail(self, decision):#20260623_kpopmodder
        return self._get_observation_decision_reporter().format_detail(decision)

    def _report_ignored_observation(self, decision, label):
        self._get_observation_decision_reporter().report_ignored(
            decision,
            label,
        )

    #20260623_kpopmodder: Surface accepted-path details in logs without changing behavior.
    def _report_accepted_observation(self, decision, label):#20260623_kpopmodder
        self._get_observation_decision_reporter().report_accepted(
            decision,
            label,
        )

    def _build_screen_question(self, detail_hint=""):#20260621_kpopmodder
        #20260705_kpopmodder: Preserve facade method while helper owns prompt assembly.
        return self._get_observation_prompt_builder().build_screen_question(
            detail_hint,
        )

        hint = str(detail_hint or "").strip()

        if hint:
            hint = hint + " "

        # return (
        #     "화면에서 보이는 핵심 대상만 설명해줘. "#20260621_kpopmodder
        #     f"{hint}"
        #     "문장을 '현재 화면에는', 'PC 화면에는', '화면에는'으로 시작하지 마. "
        #     "대상 이름으로 바로 시작해. "
        #     "반드시 '~이 보입니다.' 또는 '~가 보입니다.' 형식의 한 문장으로만 말해줘. "
        #     "예: 'YouTube 영상이 보입니다.' "
        #     "예: 'ChatGPT 시작화면이 보입니다.' "
        #     "보이지 않는 내용은 추측하지 마."
        # )
        
        return (
            "당신은 PC 화면 관찰 기록 도우미입니다. "#20260622_kpopmodder
            "사용자에게 바로 말할 문장이 아니라, 나중에 사용자가 질문했을 때 참고할 내부 관찰 기록을 작성하세요. "
            f"{hint}"
            "화면에 실제로 보이는 내용만 근거로 기록하세요. "
            "열려 있는 프로그램, 창 제목, 읽을 수 있는 텍스트, 코드, 문서, 오류 메시지, 버튼, 메뉴, 게임 상태, 눈에 띄는 변화를 가능한 자세히 기록하세요. "
            "불확실한 내용은 추측하지 말고 '확실하지 않음'이라고 쓰세요. "
            "'현재 화면에는', 'PC 화면에는', '화면에는' 같은 시작 표현은 쓰지 마세요."
        )

    def _strip_screen_prefix(self, text):#20260621_kpopmodder
        #20260705_kpopmodder: Keep facade method stable; helper preserves the old prefix list exactly.
        return self._get_observation_text_cleaner().strip_screen_prefix(text)

        text = str(text or "").strip()

        prefixes = (
            "현재 화면에는 ",
            "PC 화면에는 ",
            "화면에는 ",
            "현재 화면에 ",
            "PC 화면에 ",
            "화면에 ",
        )

        changed = True
        while changed:
            changed = False
            for prefix in prefixes:
                if text.startswith(prefix):
                    text = text[len(prefix):].strip()
                    changed = True

        return text

    # def set_auto_watch(self, enabled):#20260621_kpopmodder
    #     if enabled:
    #         self.auto_watch_controller.start()
    #     else:
    #         self.auto_watch_controller.stop()
    #         self.vision_analyzer.request_interrupt()

    #     return self.live_textbox.get_text()

    # def set_auto_watch(self, enabled):#20260621_kpopmodder
    #     if enabled:
    #         #self.start_latest_display_loop()#20260621_kpopmodder
    #         # self.auto_watch_controller.start()#20260621_kpopmodder
    #         self.auto_watch_controller.start()#20260621_kpopmodder#1초 캡처/변화 감지는 이 컨트롤러가 담당한다.
    #     else:
    #         self.stop_latest_display_loop()#20260621_kpopmodder#1초 캡처/변화 감지는 이 컨트롤러가 담당한다.
    #         # self.auto_watch_controller.stop()#20260621_kpopmodder
    #         self.auto_watch_controller.stop()#20260621_kpopmodder
    #         self.vision_analyzer.request_interrupt()

    #     return self.live_textbox.get_text()

    # def set_auto_watch(self, enabled):#20260621_kpopmodder
    #     if enabled:
    #         self.auto_watch_controller.start()#20260621_kpopmodder#1초 캡처/변화 감지는 이 컨트롤러가 담당한다.
    #     else:
    #         self.auto_watch_controller.stop()#20260621_kpopmodder
    #         self.vision_analyzer.request_interrupt()

    #     return self.live_textbox.get_text()

    def set_auto_watch(self, enabled):#20260621_kpopmodder
        if enabled:
            if gpu_device_manager.should_delay_screenvision_auto_load():
                message = gpu_device_manager.screenvision_auto_load_warning()
                log_print(message)
                self.live_textbox.print(message)
                return self.live_textbox.get_text()
            self.auto_watch_controller.start()
        else:
            self.stop_latest_display_loop()#20260621_kpopmodder
            self.auto_watch_controller.stop()
            self._request_vision_interrupt()

        return self.live_textbox.get_text()

    def _build_auto_watch_question(self):
        #20260706_kpopmodder: Keep Auto Watch prompt construction separate from change handling.
        return self._build_screen_question(#20260621_kpopmodder
            "새로 나타난 프로그램, 문서, 코드, 오류, 게임 상태 등 "
            #"사용자에게 알려줄 가치가 있는 내용만 짧게 설명해줘. "
            "사용자에게 알려줄 가치가 있는 내용을 가능한 자세히 관찰 기록으로 작성해줘. "#20260622_kpopmodder
            "'변화가 있습니다', '중요한 변화', '주목할 만한 변화' 같은 표현은 쓰지 마."
        )

    def handle_auto_watch_change(self, image, difference):
        question = self._build_auto_watch_question()
        return self._get_auto_watch_observation_flow().analyze_and_save(
            image=image,
            difference=difference,
            question=question,
            source="자동 화면 변화 감지",
        )

        if not self.analysis_lock.acquire(blocking=False):
            self.live_textbox.print(
                "[ScreenVision] Screen change skipped: analysis is busy."
            )
            return

        try:
            # question = (#20260621_kpopmodder
            #     "방금 PC 화면에 중요한 변화가 생겼습니다. "
            #     "새로 나타난 프로그램, 문서, 코드, 오류, 게임 상태 등 "
            #     "사용자에게 알려줄 가치가 있는 변화만 설명해줘."
            # )
            # question = (#20260621_kpopmodder
            #     "PC 화면에 무엇이 보이는지 설명해줘. "#20260621_kpopmodder
            #     "새로 나타난 프로그램, 문서, 코드, 오류, 게임 상태 등 "
            #     "사용자에게 알려줄 가치가 있는 내용만 짧게 설명해줘. "
            #     "'변화가 있습니다', '중요한 변화', '주목할 만한 변화' 같은 표현은 쓰지 말고, "
            #     "'...이 보입니다' 형식으로 설명해줘."#20260621_kpopmodder
            # )
            question = self._build_screen_question(#20260621_kpopmodder
                "새로 나타난 프로그램, 문서, 코드, 오류, 게임 상태 등 "
                #"사용자에게 알려줄 가치가 있는 내용만 짧게 설명해줘. "
                "사용자에게 알려줄 가치가 있는 내용을 가능한 자세히 관찰 기록으로 작성해줘. "#20260622_kpopmodder
                "'변화가 있습니다', '중요한 변화', '주목할 만한 변화' 같은 표현은 쓰지 마."
            )
            # observation = self.vision_analyzer.analyze(
            #     image=image,
            #     question=question,
            # )

            # if observation is None:
            #     return

            # # self.live_textbox.print(#20260621_kpopmodder
            # #     f"[ScreenVision] Auto observation: {observation}"
            # # )
            # # self.send_observation_to_llm(
            # #     observation=observation,
            # #     question=question,
            # #     source="자동 화면 변화 감지",
            # # )

            # observation = self._normalize_observation(observation)#20260621_kpopmodder
            # observation = self._strip_screen_prefix(observation)#20260621_kpopmodder

            #20260621_kpopmodder: 기존 자동 관찰 필터 순서는 ScreenObservationProcessor가 유지한다.
            result = self._get_observation_analysis_helper().analyze_and_process(
                image=image,
                event_source="auto_watch",
                question=question,
                label="Auto",
                decision_kwargs={
                    "reject_no_important_change": True,
                    "check_duplicate": True,
                    "check_ai_speaking": True,
                    "check_cooldown": True,
                },
                raw_metadata={
                    "difference": difference,
                },
                print_observation_prefix="[ScreenVision] Auto observation",
            )
            if result is None:#20260621_kpopmodder
                return

            observation = result.observation
            decision = result.decision

            # if self._is_broken_observation(observation):#20260621_kpopmodder
            #     self.live_textbox.print(
            #         f"[ScreenVision] Auto observation ignored: broken/noise: {observation}"
            #     )
            #     return

            # if self._is_broken_observation(observation):#20260621_kpopmodder
            #     #log_print(f"[ScreenVision] ignored: broken/noise observation={observation!r}")#20260621_kpopmodder
            #     log_print(f"[ScreenVision] Auto ignored: broken/noise observation={observation!r}")#20260621_kpopmodder
            #     self.live_textbox.print(
            #         f"[ScreenVision] Auto observation ignored: broken/noise: {observation}"
            #     )
            #     return

            if not decision.accepted:#20260621_kpopmodder
                return

            # if self._is_no_important_change(observation):#20260621_kpopmodder
            #     self.live_textbox.print(
            #         "[ScreenVision] No important change. Skip LLM/TTS."
            #     )
            #     return

            # if self._is_duplicate_auto_observation(observation):#20260621_kpopmodder
            #     self.live_textbox.print(
            #         f"[ScreenVision] Auto observation ignored: duplicate/similar: {observation}"
            #     )
            #     return

            # if self._is_ai_speaking():#20260621_kpopmodder
            #     self.live_textbox.print(
            #         "[ScreenVision] Auto observation skipped: AI is speaking."
            #     )
            #     return

            # if not self._can_send_auto_observation_to_llm():#20260621_kpopmodder
            #     return

            # self.last_auto_observation = observation#20260622_kpopmodder
            # self.last_auto_sent_time = time.time()#20260622_kpopmodder

            # self.send_observation_to_llm(#20260622_kpopmodder
            #     observation=observation,
            #     question=question,
            #     source="자동 화면 변화 감지",
            # )

            self.last_auto_observation = observation#20260622_kpopmodder
            self.last_auto_sent_time = time.time()#20260622_kpopmodder: silent auto watch throttle 기준으로 사용

            self.remember_screen_observation(#20260622_kpopmodder
                observation=observation,
                question=question,
                source="자동 화면 변화 감지",
            )

            self.live_textbox.print(
                "[ScreenVision] Auto observation saved silently. Ask about the screen to use it."
            )

            return

        finally:
            self.analysis_lock.release()

    def look_at_screen(self, question):
        question = str(question or "").strip()
        if not question:
            #question = "화면에서 무엇이 보이는지 알려줘."#20260621_kpopmodder
            question = self._build_screen_question()#20260621_kpopmodder

        try:
            self.live_textbox.print("[ScreenVision] Capturing screen...")
            image = self.capture_service.capture()

            self.live_textbox.print("[ScreenVision] Analyzing screen...")
            with self.analysis_lock:
            #     observation = self.vision_analyzer.analyze(
            #         image=image,
            #         question=question,
            #     )

            # if observation is None:
            #     self.live_textbox.print("[ScreenVision] Analysis interrupted.")
            #     return self.live_textbox.get_text(), image

            # observation = self._normalize_observation(observation)#20260621_kpopmodder
            # observation = self._strip_screen_prefix(observation)#20260621_kpopmodder
                result = self._get_observation_analysis_helper().analyze_and_process(
                    image=image,
                    event_source="manual_look",
                    question=question,
                    label="Manual",
                    analyze_kwargs={
                        "max_new_tokens": 512,#20260622_kpopmodder: 수동 화면 관찰은 사용자가 직접 요청한 것이므로 더 자세히 본다.
                    },
                )

            if result is None:#20260621_kpopmodder
                self.live_textbox.print("[ScreenVision] Analysis interrupted.")
                return self.live_textbox.get_text(), image

            #20260621_kpopmodder: 수동 관찰은 기존처럼 broken/noise만 차단한다.
            observation = result.observation
            decision = result.decision

            # if self._is_broken_observation(observation):#20260621_kpopmodder
            #     self.live_textbox.print(
            #         f"[ScreenVision] Manual observation ignored: broken/noise: {observation}"
            #     )
            #     return self.live_textbox.get_text(), image

            if not decision.accepted:#20260621_kpopmodder
                return self.live_textbox.get_text(), image

            self.live_textbox.print(
                f"[ScreenVision] Observation: {observation}"
            )
            self.send_observation_to_llm(
                observation=observation,
                question=question,
                source="수동 화면 관찰",
            )
            return self.live_textbox.get_text(), image

        except Exception as e:
            log_print(f"[ScreenVision error] {e}")
            self.live_textbox.print(f"[ScreenVision] Error: {e}")
            return self.live_textbox.get_text(), None

    # def send_observation_to_llm(self, observation, question, source):#20260621_kpopmodder
    #     llm_input = (
    #         f"[{source}]\n"
    #         f"{observation}\n\n"
    #         "[사용자 질문 또는 관찰 목적]\n"
    #         f"{question}\n\n"
    #         "위 화면 관찰 결과에만 근거해 자연스럽고 짧게 반응하세요. "
    #         "보이지 않는 내용은 추측하지 마세요."
    #     )
    #     self.send_output(llm_input)

    # def send_observation_to_llm(self, observation, question, source):#20260621_kpopmodder
    #     if self.memory_store is not None:#20260621_kpopmodder
    #         try:
    #             self.memory_store.add_screen_observation(
    #                 observation=observation,
    #                 source=source,
    #             )
    #         except Exception as e:#20260621_kpopmodder
    #             log_print(f"[Memory] screen observation save failed: {e}")
                
    #     llm_input = (
    #         f"[{source}]\n"
    #         f"{observation}\n\n"
    #         "[사용자 질문 또는 관찰 목적]\n"
    #         f"{question}\n\n"
    #         # "위 화면 관찰 결과에만 근거해 반드시 한 문장으로만 대답하세요. "#20260621_kpopmodder
    #         # "반드시 '...이 보입니다' 형식으로 말하세요. "
    #         # "두 문장 이상 쓰지 마세요. 번호 목록을 쓰지 마세요. "
    #         # "자세한 설명, 감상, 추측은 하지 마세요. "
    #         # "보이지 않는 내용은 추측하지 마세요."
    #         "위 화면 관찰 결과에만 근거해 반드시 한 문장으로만 대답하세요. "#20260621_kpopmodder
    #         "문장을 '현재 화면에는', 'PC 화면에는', '화면에는'으로 시작하지 마세요. "
    #         "관찰 결과에 해당 표현이 있어도 최종 답변에서는 제거하세요. "
    #         "대상 이름으로 바로 시작해서 반드시 '~이 보입니다.' 또는 '~가 보입니다.' 형식으로 말하세요. "
    #         "예: 'YouTube 영상이 보입니다.' "
    #         "두 문장 이상 쓰지 마세요. 번호 목록을 쓰지 마세요. "
    #         "자세한 설명, 감상, 추측은 하지 마세요. "
    #         "보이지 않는 내용은 추측하지 마세요."
    #     )
    #     self.send_output(llm_input)

    def remember_screen_observation(self, observation, question, source):#20260622_kpopmodder
        """Auto Watch 관찰 결과를 LLM/TTS로 즉시 보내지 않고 최근 화면 기록으로만 저장한다."""
        self.observation_memory_dispatch.remember_screen_observation(
            observation=observation,
            question=question,
            source=source,
        )

    def send_observation_to_llm(self, observation, question, source):#20260621_kpopmodder
        # if self.memory_store is not None:#20260621_kpopmodder
        #     try:
        #         self.memory_store.add_screen_observation(
        #             observation=observation,
        #             source=source,
        #         )
        #     except Exception as e:#20260621_kpopmodder
        #         log_print(f"[Memory] screen observation save failed: {e}")
        self.observation_memory_dispatch.send_observation_to_llm(
            observation=observation,
            question=question,
            source=source,
        )

        # llm_input = (#20260622_kpopmodder
        #     f"[{source}]\n"
        #     f"{observation}\n\n"
        #     "[사용자 질문 또는 관찰 목적]\n"
        #     f"{question}\n\n"
        #     "위 화면 관찰 결과에만 근거해 반드시 한 문장으로만 대답하세요. "#20260621_kpopmodder
        #     "문장을 '현재 화면에는', 'PC 화면에는', '화면에는'으로 시작하지 마세요. "
        #     "관찰 결과에 해당 표현이 있어도 최종 답변에서는 제거하세요. "
        #     "대상 이름으로 바로 시작해서 반드시 '~이 보입니다.' 또는 '~가 보입니다.' 형식으로 말하세요. "
        #     "예: 'YouTube 영상이 보입니다.' "
        #     "두 문장 이상 쓰지 마세요. 번호 목록을 쓰지 마세요. "
        #     "자세한 설명, 감상, 추측은 하지 마세요. "
        #     "보이지 않는 내용은 추측하지 마세요."
        # )

        #20260621_kpopmodder: ScreenVision 입력은 LLM/TTS에는 전달하지만 일반 chat history에는 저장하지 않는다.
    def start_latest_display_loop(self):#20260621_kpopmodder
        self._get_latest_display_runtime().start()

    def stop_latest_display_loop(self):#20260621_kpopmodder
        self._get_latest_display_runtime().stop()

    def latest_display_loop(self):#20260621_kpopmodder
        return self._get_latest_display_runtime().loop()

    def force_latest_screen(
        self,
        source="자동 최신 화면 관찰",
        block_for_lock=False,
    ):#20260621_kpopmodder
        #20260621_kpopmodder: 과거 LLM/TTS 큐를 끊고 현재 화면 설명을 우선한다.
        #event_manager.trigger(EventType.INTERRUPT)#20260621_kpopmodder

        # is_manual_force = source != "자동 최신 화면 관찰"#20260621_kpopmodder

        # if is_manual_force:#20260621_kpopmodder: 자동 화면 설명은 기존 TTS를 끊지 않는다. 단축키/수동 최신 화면만 끊는다.
        #     event_manager.trigger(EventType.INTERRUPT)
        # elif self._is_ai_speaking():
        #     self.live_textbox.print(
        #         "[ScreenVision] Latest screen skipped: AI is speaking."
        #     )
        #     return

        is_manual_force = source != "자동 최신 화면 관찰"#20260621_kpopmodder

        #20260621_kpopmodder: 분석 전에 interrupt를 날리면 ScreenVision 자신의 vision_analyzer까지 끊길 수 있다.
        # 자동 최신 화면만 AI 발화 중이면 스킵하고, 수동/단축키 최신 화면은 분석 완료 후 전송 직전에만 TTS/LLM을 끊는다.
        if not is_manual_force and self._is_ai_speaking():
            self.live_textbox.print(
                "[ScreenVision] Latest screen skipped: AI is speaking."
            )
            return

        acquired = self.analysis_lock.acquire(blocking=block_for_lock)
        if not acquired:
            self.live_textbox.print(
                "[ScreenVision] Latest screen skipped: analysis is busy."
            )
            return

        try:
            # question = (#20260621_kpopmodder
            #     "PC 화면에 무엇이 보이는지 설명해줘. "#20260621_kpopmodder
            #     "사용자에게 알려줄 가치가 있는 내용만 짧게 설명해줘. "
            #     "'변화가 있습니다', '중요한 변화', '주목할 만한 변화' 같은 표현은 쓰지 말고, "
            #     "'...이 보입니다' 형식으로 한 문장만 말해줘."
            # )
            question = self._build_screen_question(#20260621_kpopmodder
                #"사용자에게 알려줄 가치가 있는 내용만 짧게 설명해줘. "
                "사용자에게 알려줄 가치가 있는 내용을 가능한 자세히 관찰 기록으로 작성해줘. "#20260622_kpopmodder
                "'변화가 있습니다', '중요한 변화', '주목할 만한 변화' 같은 표현은 쓰지 마."
            )

            self.live_textbox.print("[ScreenVision] Force latest screen capture...")
            image = self.capture_service.capture()

            self.live_textbox.print("[ScreenVision] Force latest screen analysis...")
            # observation = self.vision_analyzer.analyze(
            #     image=image,
            #     question=question,
            # )

            # if observation is None:
            #     self.live_textbox.print("[ScreenVision] Latest analysis interrupted.")
            #     return

            # observation = self._normalize_observation(observation)#20260621_kpopmodder
            # observation = self._strip_screen_prefix(observation)#20260621_kpopmodder

            #20260621_kpopmodder: 수동 최신 화면은 중복/쿨다운을 건너뛰는 기존 동작을 보존한다.
            result = self._get_observation_analysis_helper().analyze_and_process(
                image=image,
                event_source="latest_screen",
                question=question,
                label="Latest",
                decision_kwargs={
                    "reject_no_important_change": True,
                    "check_duplicate": not is_manual_force,
                    "check_cooldown": not is_manual_force,
                },
                raw_metadata={
                    "manual_force": is_manual_force,
                },
                decision_metadata={
                    "manual_force": is_manual_force,
                },
                print_observation_prefix="[ScreenVision] Latest observation",
            )
            if result is None:#20260621_kpopmodder
                self.live_textbox.print("[ScreenVision] Latest analysis interrupted.")
                return

            observation = result.observation
            decision = result.decision

            # if self._is_broken_observation(observation):#20260621_kpopmodder
            #     self.live_textbox.print(
            #         f"[ScreenVision] Latest observation ignored: broken/noise: {observation}"
            #     )
            #     return

            if not decision.accepted:#20260621_kpopmodder
                return

            #20260621_kpopmodder: 최신 결과를 보내기 직전에 한 번 더 과거 TTS 큐를 비운다.
            #event_manager.trigger(EventType.INTERRUPT)
            if not is_manual_force:#20260621_kpopmodder
                # if self._is_duplicate_auto_observation(observation):#20260621_kpopmodder
                #     self.live_textbox.print(
                #         f"[ScreenVision] Latest observation ignored: duplicate/similar: {observation}"
                #     )
                #     return
                self.last_auto_observation = observation#20260621_kpopmodder
                self.last_auto_sent_time = time.time()#20260621_kpopmodder

            #20260621_kpopmodder: 최신 결과를 보내기 직전에도 자동 모드는 기존 TTS 큐를 비우지 않는다.
            # if is_manual_force:#20260621_kpopmodder
            #     event_manager.trigger(EventType.INTERRUPT)

            if is_manual_force:#20260621_kpopmodder
                self.ignore_next_own_interrupt = True#20260621_kpopmodder
                try:
                    event_manager.trigger(EventType.INTERRUPT)
                finally:
                    self.ignore_next_own_interrupt = False#20260621_kpopmodder

            # self.send_observation_to_llm(#20260622_kpopmodder
            #     observation=observation,
            #     question=question,
            #     source=source,
            # )

            if not is_manual_force:#20260622_kpopmodder
                self.remember_screen_observation(
                    observation=observation,
                    question=question,
                    source=source,
                )
                self.live_textbox.print(
                    "[ScreenVision] Latest observation saved silently."
                )
                return

            self.send_observation_to_llm(#20260622_kpopmodder
                observation=observation,
                question=question,
                source=source,
            )

        except Exception as e:
            log_print(f"[ScreenVision latest screen error] {e}")
            self.live_textbox.print(f"[ScreenVision] Latest screen error: {e}")

        finally:
            self.analysis_lock.release()
