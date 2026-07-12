from core.config_manager import config_manager#20260626_kpopmodder
from llm_core.openai_screen_question_router_provider import OpenAIScreenQuestionRouterProvider#20260628_kpopmodder
from llm_core.screen_question_router import ScreenQuestionRouter#20260628_kpopmodder


#20260630_kpopmodder: Moved ScreenQuestionRouter startup wiring out of main.py without changing provider opt-in behavior.
def screen_question_router_config_value(config, short_key, recommended_key, default):#20260628_kpopmodder: Keep screen-intent routing config separate from memory routing.
    return config.get(short_key, config.get(recommended_key, default))


def screen_question_router_config_bool(config, short_key, recommended_key, default):#20260628_kpopmodder
    value = screen_question_router_config_value(
        config,
        short_key,
        recommended_key,
        default,
    )
    return str(value).lower() not in {"0", "false", "no", "off"}


def build_screen_question_router():#20260630_kpopmodder: Build screen-question routing for main.py startup.
    screen_question_router_config = config_manager.load_section("ScreenQuestionRouter")#20260628_kpopmodder
    screen_question_router_provider = screen_question_router_config_value(#20260628_kpopmodder: Rule is the safe local default; OpenAI is explicit opt-in.
        screen_question_router_config,
        "provider",
        "screen_question_router_provider",
        "rule",
    )
    openai_screen_question_router_provider = None#20260628_kpopmodder: Do not send user input to OpenAI unless configured.
    if str(screen_question_router_provider).strip().lower() in {
        "openai",
        "chatgpt",
        "chatgpt_openai",
        "openai_router",
    }:
        openai_screen_question_router_provider = (
            OpenAIScreenQuestionRouterProvider.from_config(
                screen_question_router_config,
            )
        )#20260628_kpopmodder: Provider receives only user text/state, never screen observation text.
    return ScreenQuestionRouter(#20260628_kpopmodder
        enabled=screen_question_router_config_bool(
            screen_question_router_config,
            "enabled",
            "screen_question_router_enabled",
            "true",
        ),
        provider=screen_question_router_provider,
        timeout_sec=screen_question_router_config_value(
            screen_question_router_config,
            "timeout_sec",
            "screen_question_router_timeout_sec",
            2,
        ),
        fallback_to_keyword=screen_question_router_config_bool(
            screen_question_router_config,
            "fallback_to_keyword",
            "screen_question_router_fallback_to_keyword",
            "true",
        ),
        ai_response_callback=openai_screen_question_router_provider,
    )
