#20260905_kpopmodder: Owns legacy generation API delegation across existing generation engines.
from __future__ import annotations


class LlmGenerationFacade:
    def __init__(
        self,
        *,
        response_pipeline_callback,
        text_only_helper_callback,
    ) -> None:
        if not callable(response_pipeline_callback):
            raise TypeError("response_pipeline_callback must be callable")
        if not callable(text_only_helper_callback):
            raise TypeError("text_only_helper_callback must be callable")
        self._response_pipeline_callback = response_pipeline_callback
        self._text_only_helper_callback = text_only_helper_callback

    @property
    def llm_output(self):
        return self._response_pipeline_callback().LLM_output

    @llm_output.setter
    def llm_output(self, value) -> None:
        self._response_pipeline_callback().LLM_output = value

    @property
    def start_of_response(self):
        return self._response_pipeline_callback().start_of_response

    @start_of_response.setter
    def start_of_response(self, value) -> None:
        self._response_pipeline_callback().start_of_response = value

    def is_generator(self) -> bool:
        return self._response_pipeline_callback().is_generator()

    def generate_text_only(
        self,
        message,
        system_prompt,
        *,
        preferred_provider_name=None,
    ):
        return self._text_only_helper_callback().generate(
            message,
            system_prompt,
            preferred_provider_name=preferred_provider_name,
        )

    def collect_text_only_generator_output(self, result):
        return self._text_only_helper_callback().collect_generator_output(result)


__all__ = ("LlmGenerationFacade",)
