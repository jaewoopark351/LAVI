#20260622_kpopmodder: Canonical UI helper for live Gradio textbox output.
import gradio as gr
import threading


class LiveTextbox:#20260616_kpopmodder
    def __init__(self) -> None:
        self.messages = []
        self.lock = threading.Lock()
        self.textbox = None

    def create_ui(self, lines=10, max_lines=10, label=None):
        self.textbox = gr.Textbox(
            value="",
            lines=lines,
            max_lines=max_lines,
            container=label is not None,
            label=label,
            show_label=label is not None,
            interactive=False,
            autoscroll=True,
        )

        return self.textbox

    def print(self, new_message, append_to_last=False):
        new_message = str(new_message)

        with self.lock:
            if append_to_last and self.messages:
                self.messages[-1] += new_message
            else:
                self.messages.append(new_message)

        return self.get_text()

    def set(self, new_message):
        with self.lock:
            if isinstance(new_message, list):
                self.messages = [str(item) for item in new_message]
            else:
                self.messages = [str(new_message)]

        return self.get_text()

    def clear(self):
        with self.lock:
            self.messages.clear()

        return ""

    def get_text(self):
        with self.lock:
            return "\n".join(self.messages)

# import gradio as gr
# import threading
# import time

# from core.logger import log_print, debug_print#20260612_kpopmodder

# class LiveTextbox():
#     def __init__(self) -> None:
#         self.messages = []
#         self.lock = threading.Lock()

#     # def create_ui(self, lines=10, max_lines=10, label=None):#20260615_kpopmodder
#     #     textbox = gr.Textbox(lines=lines, container=label != None, label=label,
#     #                          show_label=True, interactive=True, autoscroll=True)
#     #     gr.Interface(#20260615_kpopmodder
#     #         fn=self.message_generator,
#     #         inputs=[],
#     #         outputs=gr.Textbox(label=self.label),
#     #         live=True,
#     #         flagging_mode="never"
#     #     )
#     #     #gr.Interface(fn=self.message_generator, inputs=[],#20260615_kpopmodder
#     #     #             outputs=[textbox], live=True, allow_flagging=False, submit_btn=gr.Button(visible=False), stop_btn=gr.Button(visible=False), clear_btn=gr.Button(visible=False))

#     def create_ui(self, lines=10, max_lines=10, label=None):#20260615_kpopmodder
#         textbox = gr.Textbox(
#             lines=lines,
#             max_lines=max_lines,
#             container=label is not None,
#             label=label,
#             show_label=label is not None,
#             interactive=True,
#             autoscroll=True,
#         )

#         gr.Interface(
#             fn=self.message_generator,
#             inputs=[],
#             outputs=[textbox],
#             live=True,
#             flagging_mode="never",
#             submit_btn=gr.Button(visible=False),
#             stop_btn=gr.Button(visible=False),
#             clear_btn=gr.Button(visible=False),
#         )

#     def print(self, new_message, append_to_last=False):#20260615_kpopmodder
#         new_message = str(new_message)

#         with self.lock:
#             if append_to_last and self.messages:
#                 self.messages[-1] += new_message
#             else:
#                 self.messages.append(new_message)

#     # def print(self, new_message, append_to_last=False):#20260612_kpopmodder
#     #     with self.lock:
#     #         if append_to_last and self.messages:
#     #             self.messages[-1] += new_message
#     #         else:
#     #             self.messages.append(new_message)

#     def set(self, new_message):#20260615_kpopmodder
#         with self.lock:
#             if isinstance(new_message, list):
#                 self.messages = [str(item) for item in new_message]
#             else:
#                 self.messages = [str(new_message)]
                
#     # def set(self, new_message:str):
#     #     with self.lock:
#     #         #self.messages = new_message#20260615_kpopmodder
#     #         self.messages = [new_message]#20260615_kpopmodder

    

#     def clear(self):
#         self.messages.clear()

#     # Generator function for the Gradio interface
#     def message_generator(self):
#         last_yielded = None
#         while True:
#             with self.lock:
#                 concatenated_messages = "\n".join(self.messages)
#             if concatenated_messages != last_yielded:
#                 yield concatenated_messages
#                 last_yielded = concatenated_messages
#             time.sleep(0.1)  # Short sleep to prevent tight loop
