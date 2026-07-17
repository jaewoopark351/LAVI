#20260717_kpopmodder: Isolates ScreenVision model loading and analysis behavior.
import os
import threading

import torch
from transformers import (
    AutoModelForMultimodalLM,
    AutoProcessor,
    StoppingCriteriaList,
)

from core.gpu_device_manager import gpu_device_manager#20260626_kpopmodder
from core.logger import log_print

from .vision_interrupt_stopping_criteria import VisionInterruptStoppingCriteria


class VisionAnalyzer:#20260620_kpopmodder
    MODEL_ID = "Qwen/Qwen2.5-VL-3B-Instruct"

    def __init__(self):
        self.processor = None
        self.model = None
        self._load_lock = threading.Lock()
        self._generation_lock = threading.Lock()
        self._active_stop_events = set()
        self.model_cache_dir = self._resolve_model_cache_dir()
        self.device = None#20260626_kpopmodder: Keep image/text tensors on the configured ScreenVision GPU.

    def _resolve_model_cache_dir(self):
        core_dir = os.path.dirname(os.path.abspath(__file__))
        plugin_dir = os.path.dirname(core_dir)
        model_dir = os.path.join(plugin_dir, "model")
        os.makedirs(model_dir, exist_ok=True)
        return model_dir

    def load(self):
        if self.processor is not None and self.model is not None:
            return

        with self._load_lock:
            if self.processor is not None and self.model is not None:
                return

            log_print(f"[ScreenVision] Loading processor: {self.MODEL_ID}")
            self.processor = AutoProcessor.from_pretrained(
                self.MODEL_ID,
                cache_dir=self.model_cache_dir,
                trust_remote_code=True,
            )

            resolved_device = gpu_device_manager.get_device("ScreenVision")#20260626_kpopmodder
            resolved_device_map = gpu_device_manager.get_device_map(
                "ScreenVision",
                default="auto",
            )#20260626_kpopmodder
            resolved_max_memory = gpu_device_manager.get_max_memory(
                "ScreenVision"
            )#20260626_kpopmodder

            log_print(f"[ScreenVision] Loading model: {self.MODEL_ID}")
            log_print(f"[ScreenVision] resolved device={resolved_device}")#20260626_kpopmodder
            log_print(
                f"[ScreenVision] resolved device_map={resolved_device_map}"
            )#20260626_kpopmodder

            model_dtype = (
                torch.float32
                if resolved_device == "cpu"
                else torch.float16
            )#20260626_kpopmodder
            model_kwargs = {
                "cache_dir": self.model_cache_dir,
                "dtype": model_dtype,
                "device_map": resolved_device_map,
                "trust_remote_code": True,
            }#20260626_kpopmodder

            if resolved_max_memory is not None:
                model_kwargs["max_memory"] = resolved_max_memory#20260626_kpopmodder

            self.model = AutoModelForMultimodalLM.from_pretrained(
                self.MODEL_ID,
                **model_kwargs,
            )
            self.model.eval()

            try:
                loaded_device = self.model.device
            except Exception:
                try:
                    loaded_device = next(self.model.parameters()).device
                except Exception:
                    loaded_device = torch.device(resolved_device)

            self.device = (
                torch.device(resolved_device)
                if resolved_device_map != "auto"
                else loaded_device
            )#20260626_kpopmodder
            log_print(f"[ScreenVision] Model loaded on {loaded_device}")

    def request_interrupt(self):
        with self._generation_lock:
            stop_events = list(self._active_stop_events)

        for stop_event in stop_events:
            stop_event.set()

        if stop_events:
            log_print("[ScreenVision] analysis interrupt requested")

    #def analyze(self, image, question):#20260622_kpopmodder
    def analyze(self, image, question, max_new_tokens=384):#20260622_kpopmodder
        self.load()

        stop_event = threading.Event()
        messages = [
            {
                "role": "user",
                "content": [
                    {"type": "image", "image": image},
                    {
                        "type": "text",
                        # "text": (#20260622_kpopmodder
                        #     "당신은 PC 화면 관찰 도우미입니다. "
                        #     "화면에 실제로 보이는 내용만 근거로 답하세요. "
                        #     "읽을 수 있는 중요한 UI, 프로그램, 문서, 코드, 오류 메시지를 "
                        #     "한국어로 간결하게 설명하고 불확실한 내용은 추측하지 마세요.\n\n"
                        #     f"사용자 질문: {question}"
                        # ),
                        "text": (#20260622_kpopmodder
                            "당신은 PC 화면 관찰 기록 도우미입니다. "#20260622_kpopmodder
                            "사용자에게 바로 말할 문장이 아니라, 나중에 사용자가 질문했을 때 참고할 내부 관찰 기록을 작성하세요. "
                            "화면에 실제로 보이는 내용만 근거로 답하세요. "
                            "열려 있는 프로그램, 창 제목, 읽을 수 있는 텍스트, 코드, 문서, 오류 메시지, "
                            "버튼, 메뉴, 게임 상태, 알림, 눈에 띄는 변화를 가능한 자세히 기록하세요. "
                            "단, 보이지 않는 내용은 추측하지 마세요. "
                            "불확실한 내용은 '확실하지 않음'이라고 쓰세요. "
                            "문장을 '현재 화면에는', 'PC 화면에는', '화면에는'으로 시작하지 마세요.\n\n"
                            f"사용자 질문 또는 관찰 목적: {question}"
                        ),
                    },
                ],
            }
        ]

        prompt = self.processor.apply_chat_template(
            messages,
            tokenize=False,
            add_generation_prompt=True,
        )
        inputs = self.processor(
            text=[prompt],
            images=[image],
            padding=True,
            return_tensors="pt",
        )
        inputs = inputs.to(self.device or self.model.device)#20260626_kpopmodder

        with self._generation_lock:
            self._active_stop_events.add(stop_event)

        try:
            with torch.inference_mode():
                generated_ids = self.model.generate(
                    **inputs,
                    #max_new_tokens=192,#20260622_kpopmodder
                    max_new_tokens=max_new_tokens,#20260622_kpopmodder: 화면 관찰 기록을 더 자세히 작성하기 위해 출력 토큰 증가
                    do_sample=False,
                    stopping_criteria=StoppingCriteriaList([
                        VisionInterruptStoppingCriteria(stop_event)
                    ]),
                )

            if stop_event.is_set():
                log_print("[ScreenVision] analysis interrupted")
                return None

            input_token_count = inputs["input_ids"].shape[1]
            generated_ids = generated_ids[:, input_token_count:]
            result = self.processor.batch_decode(
                generated_ids,
                skip_special_tokens=True,
                clean_up_tokenization_spaces=False,
            )[0].strip()

            log_print(f"[ScreenVision] observation: {result}")
            return result

        finally:
            with self._generation_lock:
                self._active_stop_events.discard(stop_event)
