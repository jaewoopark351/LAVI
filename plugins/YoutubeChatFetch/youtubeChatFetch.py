from queue import Queue
import threading
import traceback
import pytchat
import time
from threading import Thread
from plugin_system.interfaces import InputPluginInterface
import gradio as gr
from ui_core.live_textbox import LiveTextbox
from core.global_state import global_state, GlobalKeys
import LAV_utils
from core.logger import log_print, debug_print#20260612_kpopmodder

class YoutubeChatFetch(InputPluginInterface):
    PLUGIN_METADATA = {
        "id": "YoutubeChatFetch",
        "display_name": "YouTube Chat Fetch",
        "api_version": "1",
        "dependency_group": "Full",
        "capabilities": ("chat_input", "youtube_chat"),
        "required_python_packages": ("pytchat",),
        "required_files": (),
        "required_executables": (),
        "required_services": ("YouTube live chat",),
        "supports_offline": False,
        "supports_cpu": True,
    }

    read_chat_youtube_thread = None
    read_chat_youtube_thread_running = False

    excluded_users_list = []
    prompt_format = "a viewer named ([name]) send message ([message]) to stream chat. Repeat the message and then your response."

    def __init__(self):
        super().__init__()
        self.liveTextbox = LiveTextbox()
        self.console_textbox = LiveTextbox()
        self.queue_textbox = LiveTextbox()
        self.chatlog = Queue(maxsize=3)
        self.chat_process_thread = None

    def create_ui(self):
        with gr.Accordion(label="Youtube Chat Fetch", open=False):
            with gr.Row():
                self.youtube_video_id_textbox = gr.Textbox(
                    label="youtube_video_id", show_label=True)
                self.start_fetch_button = gr.Button("Start Fetching Chat")
                self.stop_fetch_button = gr.Button("Stop Fetching Chat")
                self.prompt_format_textbox = gr.Textbox(label= "Prompt format")

                self.start_fetch_button.click(self.read_chat_youtube, inputs=[
                                              self.youtube_video_id_textbox])
                self.stop_fetch_button.click(self.stop_read_chat_youtube)

                self.prompt_format_textbox.change(fn=self.update_prompt, inputs=self.prompt_format_textbox)
            self.liveTextbox.create_ui()
            self.console_textbox.create_ui()
            self.queue_textbox.create_ui()


    def update_prompt(self, text):
        self.prompt_format = text
    def read_chat_youtube(self, youtube_video_id):
        gr.Info("starting chat fetching...")
        # log_print("starting chat fetching...")#20260612_kpopmodder
        chat = None
        try:
            chat = pytchat.create(
                video_id=youtube_video_id, interruptable=False, topchat_only=True)
        except:
            log_print("failed to fetch chat")#20260612_kpopmodder
            log_print(traceback.format_exc())#20260612_kpopmodder
            return
        self.read_chat_youtube_thread = Thread(
            target=self.read_chat_loop, args=[chat,])
        self.read_chat_youtube_thread.start()
        self.read_chat_youtube_thread_running = True

    def read_chat_loop(self, chat):
        log_print("Chat fetching started")#20260612_kpopmodder
        self.liveTextbox.print("Chat fetching started")
        while self.read_chat_youtube_thread_running and chat.is_alive():
            for c in chat.get().sync_items():
                if c.author.name not in self.excluded_users_list:
                    # log_print(f"{c.datetime} [{c.author.name}]- {c.message}")#20260612_kpopmodder
                    self.read_chat_loop
                    self.liveTextbox.print(
                        f"{c.datetime} [{c.author.name}]- {c.message}")
                    self.add_to_chat_log(c.author.name, c.message)
            time.sleep(5)
        log_print("Chat fetching ended")#20260612_kpopmodder
        self.liveTextbox.print("Chat fetching started")

    def stop_read_chat_youtube(self):
        gr.Info("stopping chat fetching...")
        log_print("stopping chat fetching...")#20260612_kpopmodder
        self.read_chat_youtube_thread_running = False
        # log_print("Process stopped.")#20260612_kpopmodder
        self.liveTextbox.print("Process stopped.")

    
    def add_to_chat_log(self, author, message):
        if self.chatlog.full():
            self.chatlog.get()
        
        self.chatlog.put([author, message])
        self.process_chat_log()

    def process_chat_log(self):
        def generate_response():
            while (not self.chatlog.empty()):
                self.queue_textbox.set(LAV_utils.queue_to_list(self.chatlog))
                if(global_state.get_value(GlobalKeys.IS_IDLE)):
                    input = self.chatlog.get()
                    prompt = self.prompt_format.replace("[name]", input[0]).replace("[message]", input[1])
                    self.process_input(prompt)
                    self.console_textbox.print(f"Sending: {prompt}")
                    self.queue_textbox.set(LAV_utils.queue_to_list(self.chatlog))
                time.sleep(5)    

        # Check if the current thread is alive
        if self.chat_process_thread is None or not self.chat_process_thread.is_alive():
            # Create and start a new thread
            self.chat_process_thread = threading.Thread(target=generate_response)
            self.chat_process_thread.start()
