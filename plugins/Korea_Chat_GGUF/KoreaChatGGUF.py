import requests
from tqdm import tqdm
from plugin_system.interfaces import LLMPluginInterface
import gradio as gr
from llama_cpp import Llama
import os
from core.logger import log_print, debug_print#20260612_kpopmodder


class KoreaChatLLM(LLMPluginInterface):#20260610_kpopmodder
    PLUGIN_METADATA = {
        "id": "KoreaChatLLM",
        "display_name": "Korea Chat GGUF",
        "api_version": "1",
        "category": "language_model",
        "entrypoint": "plugins.Korea_Chat_GGUF.KoreaChatGGUF:KoreaChatLLM",
        "dependency_group": "Full",
        "capabilities": ("llm", "local_gguf", "korean_chat"),
        "required_python_packages": ("llama_cpp", "requests", "tqdm"),
        "required_files": (),
        "required_executables": (),
        "required_services": (),
        "supports_offline": False,
        "supports_cpu": False,
        "requires_gpu": True,
    }

    context_length = 32768
    temperature = 0.9
    def init(self):
        # Directory where the module is located
        current_module_directory = os.path.dirname(__file__)
        model_filename = "ggml-model-Q5_K_M.gguf"#20260618_kpopmodder
        #model_filename = "Korean-Bllossom-8B.Q4_K_M.gguf"#20260618_kpopmodder
        model_directory = os.path.join(current_module_directory, "models")
        model_path = os.path.join(model_directory, model_filename)

        # Check if the model file exists
        if not os.path.exists(model_path):
            # If not, create the models directory if it does not exist
            if not os.path.exists(model_directory):
                os.makedirs(model_directory)

            # URL to download the model
            url = "https://huggingface.co/heegyu/EEVE-Korean-Instruct-10.8B-v1.0-GGUF/resolve/main/ggml-model-Q5_K_M.gguf"
            
             # Download the file with progress
            log_print(f"Downloading model from {url}...")#20260612_kpopmodder
            response = requests.get(url, stream=True)
            
            if response.status_code == 200:
                total_size_in_bytes = int(response.headers.get('content-length', 0))
                block_size = 1024  # 1 Kibibyte

                progress_bar = tqdm(total=total_size_in_bytes, unit='iB', unit_scale=True)
                with open(model_path, 'wb') as file:
                    for data in response.iter_content(block_size):
                        progress_bar.update(len(data))
                        file.write(data)
                progress_bar.close()

                if total_size_in_bytes != 0 and progress_bar.n != total_size_in_bytes:
                    log_print("ERROR, something went wrong during download")#20260612_kpopmodder
                else:
                    log_print("Model downloaded successfully.")#20260612_kpopmodder
            else:
                log_print(f"Failed to download the model. Status code: {response.status_code}")#20260612_kpopmodder
                return

        # Initialize the model
        self.llm = Llama(model_path=model_path, n_ctx=self.context_length, n_gpu_layers=-1, seed=-1)

    def create_ui(self):
        with gr.Accordion("KoreaChat LLM settings", open=False):
            with gr.Row():
                self.temperature_slider = gr.Slider(minimum=0, maximum=1, value=self.temperature,label="temperature")
                
                self.temperature_slider.change(fn=self.update_temperature,inputs=self.temperature_slider)

    def update_temperature(self, t):
        self.temperature = t

    def predict(self, message, history, system_prompt):
        messages = [
            {"role": "system", "content": system_prompt},
        ]
        # for entry in history:#20260618_kpopmodder
        #     user, ai = entry
        #     messages.append({"role": "user", "content": user})
        #     messages.append({"role": "assistant", "content": ai})

        for user, ai in self._normalize_history(history):  # 20260618_kpopmodder
            if user:
                messages.append({"role": "user", "content": user})

            if ai:
                messages.append({"role": "assistant", "content": ai})

        messages.append({"role": "user", "content": message})

        # Function to count the number of tokens in the messages
        def count_tokens(msg_list):
            result = sum(len(self.llm.tokenize(
                str.encode(msg['content']))) for msg in msg_list)
            log_print(f"Tokens_in_context = {result}")#20260612_kpopmodder
            return result

        # Trim oldest messages if context length in tokens is exceeded
        while count_tokens(messages) > self.context_length and len(messages) > 1:
            # Remove the oldest message (after the system prompt)
            messages.pop(1)

        log_print(f"message: {message}")#20260612_kpopmodder
        log_print(f"history: {history}")#20260612_kpopmodder
        log_print(f"messages: {messages}")#20260612_kpopmodder
        log_print(f"---------------------------------")#20260612_kpopmodder
        log_print(f"Generating with temperature {self.temperature}")#20260612_kpopmodder

        completion_chunks = self.llm.create_chat_completion(
            messages, stream=True, temperature=self.temperature)
        output = ""
        for completion_chunk in completion_chunks:
            try:
#                text = completion_chunk['choices'][0]['delta']['content']
                text = completion_chunk['choices'][0]['delta'].get('content', "")#20260612_kpopmodder

                if not text:#20260612_kpopmodder
                    continue

                output += text
                yield output
            except Exception as e:
                log_print(f"[KoreaChatLLM] stream error: {e}")
        
        log_print(f"[KoreaChatLLM] response: {output}")

    def _extract_gradio_text(self, content):  # 20260618_kpopmodder
        if content is None:
            return ""

        if isinstance(content, str):
            return content

        # Gradio 5 content format:
        # [{"text": "안녕", "type": "text"}]
        if isinstance(content, list):
            texts = []

            for item in content:
                if isinstance(item, dict):
                    text = item.get("text", "")
                    if text:
                        texts.append(str(text))
                else:
                    texts.append(str(item))

            return "\n".join(texts)

        if isinstance(content, dict):
            return str(content.get("text", ""))

        return str(content)

    def _normalize_history(self, history):  # 20260618_kpopmodder
        if not history:
            return []

        normalized = []

        # Old Gradio format:
        # [[user, assistant], ...] or [(user, assistant), ...]
        if all(isinstance(entry, (list, tuple)) for entry in history):
            for entry in history:
                if len(entry) >= 2:
                    user = self._extract_gradio_text(entry[0])
                    ai = self._extract_gradio_text(entry[1])
                    normalized.append((user, ai))

            return normalized

        # Gradio 5 messages format:
        # [{"role": "user", "content": ...}, {"role": "assistant", "content": ...}]
        pending_user = None

        for entry in history:
            if not isinstance(entry, dict):
                continue

            role = entry.get("role")
            text = self._extract_gradio_text(entry.get("content"))

            if role == "user":
                pending_user = text

            elif role == "assistant":
                if pending_user is not None:
                    normalized.append((pending_user, text))
                    pending_user = None

        return normalized
