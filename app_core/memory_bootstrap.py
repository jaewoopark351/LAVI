import os

from core.config_manager import config_manager#20260626_kpopmodder
from core.logger import log_print
from memory_core.derived_memory_rebuild_service import (
    DerivedMemoryRebuildService,
)#20260720_kpopmodder
from memory_core.derived_memory_sqlite_store import DerivedMemorySQLiteStore#20260626_kpopmodder
from memory_core.memory_context_builder import MemoryContextBuilder#20260621_kpopmodder
from memory_core.memory_retriever import MemoryRetriever#20260622_kpopmodder
from memory_core.memory_router import MemoryRouter#20260626_kpopmodder
from memory_core.memory_store import MemoryStore#20260621_kpopmodder
from memory_core.openai_memory_router_provider import OpenAIMemoryRouterProvider#20260626_kpopmodder


#20260630_kpopmodder: Moved memory startup wiring out of main.py while preserving raw-first recall behavior.
def memory_router_config_value(config, short_key, recommended_key, default):#20260626_kpopmodder: Accept AGENTS.md recommended keys without rewriting config.ini.
    return config.get(short_key, config.get(recommended_key, default))


def memory_router_config_bool(config, short_key, recommended_key, default):#20260626_kpopmodder
    value = memory_router_config_value(
        config,
        short_key,
        recommended_key,
        default,
    )
    return str(value).lower() not in {"0", "false", "no", "off"}


def memory_router_config_int(config, short_key, recommended_key, default):#20260720_kpopmodder
    value = memory_router_config_value(
        config,
        short_key,
        recommended_key,
        default,
    )
    try:
        return max(1, int(value))
    except Exception:
        return max(1, int(default))


DERIVED_MEMORY_INDEX_EVENT_TYPES = (#20260720_kpopmodder
    "user_message",
    "assistant_message",
    "screen_observation",
    "screen_observation_silent",
)


def memory_latest_raw_event_ts(memory_store):#20260627_kpopmodder: Cheap stale check for the optional derived recall index.
    latest_ts = None#20260720_kpopmodder: Compare derived memory only against indexable raw events.
    try:
        if hasattr(memory_store, "iter_raw_events"):
            for event in memory_store.iter_raw_events(
                limit=None,
                event_types=DERIVED_MEMORY_INDEX_EVENT_TYPES,
            ):
                try:
                    created_ts = float(event.get("created_ts", 0) or 0)
                except Exception:
                    continue
                if latest_ts is None or created_ts > latest_ts:
                    latest_ts = created_ts
            return latest_ts
    except Exception:
        pass

    try:
        events = memory_store.get_raw_events(limit=2000)
    except Exception:
        return None

    for event in reversed(events or []):
        if str(event.get("event_type", "")) not in DERIVED_MEMORY_INDEX_EVENT_TYPES:
            continue
        try:
            return float(event.get("created_ts", 0) or 0)
        except Exception:
            return None

    return None


def refresh_derived_memory_if_stale(
    derived_store,
    memory_store,
    stats,
    auto_rebuild=True,
    background=False,
):#20260720_kpopmodder: Rebuild stale derived recall index without blocking startup when requested.
    if not bool((stats or {}).get("stale")):
        return stats or {}

    if not auto_rebuild:
        log_print(
            "[Memory][Warning] derived_memory.sqlite3 is stale; "
            "auto rebuild is disabled."
        )
        return stats or {}

    if background:
        DerivedMemoryRebuildService.schedule_background_rebuild(
            derived_store,
            memory_store,
            reason="stale_startup",
        )
        return stats or {}

    try:
        rebuild_stats, refreshed_stats = DerivedMemoryRebuildService.rebuild_now(
            derived_store,
            memory_store,
        )
        log_print(
            "[Memory] derived_memory.sqlite3 rebuilt "
            f"(raw_events={rebuild_stats.get('raw_event_count', 0)}, "
            f"rows={refreshed_stats.get('row_count', 0)}, "
            f"stale={refreshed_stats.get('stale', False)})"
        )
        return refreshed_stats
    except Exception as e:
        log_print(f"[Memory][Warning] derived_memory.sqlite3 rebuild failed: {e}")
        DerivedMemoryRebuildService.schedule_background_rebuild(
            derived_store,
            memory_store,
            reason="sync_rebuild_error",
        )
        return stats or {}


def log_derived_memory_startup_state(
    derived_store,
    memory_store,
    prefer_derived_first,
):#20260627_kpopmodder: Keep derived DB visible while source-of-truth remains raw_events.
    stats = derived_store.get_stats(
        raw_latest_created_ts=memory_latest_raw_event_ts(memory_store),
    )
    log_print(
        "[Memory] SQLite derived recall index ready "
        "(retriever_fallback_enabled=False, source_of_truth=False, "
        f"prefer_derived_first={prefer_derived_first}, "
        f"rows={stats.get('row_count', 0)}, "
        f"stale={stats.get('stale', False)}): "
        f"{derived_store.db_path}"
    )
    if prefer_derived_first and int(stats.get("row_count", 0) or 0) == 0:
        log_print(
            "[Memory][Warning] prefer_derived_first=true but "
            "derived_memory.sqlite3 is empty; raw recall fallback will be used."
        )
    return stats


def bootstrap_memory(current_module_directory):#20260630_kpopmodder: Build memory services for main.py startup.
    memory_dir = os.path.join(current_module_directory, "memory")#20260621_kpopmodder
    memory_store = MemoryStore(memory_dir=memory_dir)#20260621_kpopmodder
    try:#20260622_kpopmodder: Log one startup line so SQLite raw-event storage is easy to verify.
        raw_events_db_path = memory_store.initialize_raw_event_sqlite()
        log_print(f"[Memory] SQLite raw event store ready: {raw_events_db_path}")
    except Exception as e:
        log_print(f"[Memory] SQLite raw event store unavailable: {e}")
    memory_router_config = config_manager.load_section("MemoryRouter")#20260626_kpopmodder
    memory_prefer_derived_first = memory_router_config_bool(#20260626_kpopmodder
        memory_router_config,
        "prefer_derived_first",
        "memory_router_prefer_derived_first",
        "true",
    )
    memory_auto_rebuild_derived_when_stale = memory_router_config_bool(#20260720_kpopmodder
        memory_router_config,
        "auto_rebuild_derived_when_stale",
        "memory_router_auto_rebuild_derived_when_stale",
        "true",
    )
    derived_memory_store = None#20260626_kpopmodder
    derived_memory_stats = {}#20260720_kpopmodder
    try:#20260626_kpopmodder: Derived DB is a fallback recall index; raw_events remain source of truth.
        derived_memory_store = DerivedMemorySQLiteStore(
            os.path.join(memory_dir, "derived_memory.sqlite3")
        )
        derived_memory_store.initialize()
        derived_memory_stats = log_derived_memory_startup_state(
            derived_memory_store,
            memory_store,
            memory_prefer_derived_first,
        )
        derived_memory_stats = refresh_derived_memory_if_stale(
            derived_memory_store,
            memory_store,
            derived_memory_stats,
            auto_rebuild=memory_auto_rebuild_derived_when_stale,
            background=True,
        )
    except Exception as e:
        derived_memory_db_path = os.path.join(memory_dir, "derived_memory.sqlite3")
        derived_memory_store = None
        log_print(f"[Memory] SQLite derived memory store unavailable: {e}")
        if memory_auto_rebuild_derived_when_stale:
            repair_store = DerivedMemorySQLiteStore(derived_memory_db_path)
            DerivedMemoryRebuildService.schedule_background_rebuild(
                repair_store,
                memory_store,
                reason="startup_error",
            )
    if memory_prefer_derived_first and bool(derived_memory_stats.get("stale")):
        memory_prefer_derived_first = False#20260720_kpopmodder
        log_print(
            "[Memory][Warning] derived_memory.sqlite3 is stale; "
            "prefer_derived_first is disabled for this run."
        )
    memory_allow_single_screen_observation_fallback = memory_router_config_bool(#20260627_kpopmodder
        memory_router_config,
        "allow_single_screen_observation_fallback",
        "memory_router_allow_single_screen_observation_fallback",
        "false",
    )
    memory_accuracy_first_raw_search = memory_router_config_bool(#20260627_kpopmodder
        memory_router_config,
        "accuracy_first_raw_search",
        "memory_router_accuracy_first_raw_search",
        "true",
    )
    memory_retriever_max_results = memory_router_config_int(#20260720_kpopmodder
        memory_router_config,
        "max_results",
        "memory_retriever_max_results",
        12,
    )
    memory_max_deep_recalled_items = memory_router_config_int(#20260720_kpopmodder
        memory_router_config,
        "max_deep_recalled_items",
        "memory_context_max_deep_recalled_items",
        12,
    )
    memory_retriever = MemoryRetriever(#20260622_kpopmodder
        memory_store,
        max_results=memory_retriever_max_results,#20260720_kpopmodder: Cap broad recall evidence before prompt injection.
        derived_store=derived_memory_store,#20260626_kpopmodder
        use_derived_fallback=False,#20260627_kpopmodder: Keep runtime recall raw-first; tests cover opt-in derived fallback.
        allow_single_screen_observation_fallback=memory_allow_single_screen_observation_fallback,#20260627_kpopmodder
        accuracy_first_raw_search=memory_accuracy_first_raw_search,#20260627_kpopmodder
    )
    memory_router_provider = memory_router_config_value(#20260627_kpopmodder: Safe default is rule; OpenAI router is opt-in only.
        memory_router_config,
        "provider",
        "memory_router_provider",
        "rule",
    )
    openai_memory_router_provider = None#20260627_kpopmodder: Keep rule/default router fully local.
    #20260627_kpopmodder: Build the OpenAI router callback only after explicit provider opt-in.
    if str(memory_router_provider).strip().lower() in {
        "openai",
        "chatgpt",
        "chatgpt_openai",
        "openai_router",
    }:
        openai_memory_router_provider = OpenAIMemoryRouterProvider.from_config(
            memory_router_config,
        )#20260627_kpopmodder: May send user input to OpenAI API only when explicitly configured.
    memory_router = MemoryRouter(#20260626_kpopmodder
        enabled=memory_router_config_bool(
            memory_router_config,
            "enabled",
            "memory_router_enabled",
            "true",
        ),
        provider=memory_router_provider,
        timeout_sec=memory_router_config_value(
            memory_router_config,
            "timeout_sec",
            "memory_router_timeout_sec",
            5,
        ),
        max_items=memory_router_config_value(
            memory_router_config,
            "max_items",
            "memory_router_max_items",
            5,
        ),
        fallback_to_keyword=memory_router_config_bool(
            memory_router_config,
            "fallback_to_keyword",
            "memory_router_fallback_to_keyword",
            "true",
        ),
        ai_response_callback=openai_memory_router_provider,
    )
    memory_context_builder = MemoryContextBuilder(#20260621_kpopmodder
        memory_store,
        memory_retriever=memory_retriever,#20260622_kpopmodder
        memory_router=memory_router,#20260626_kpopmodder
        prefer_derived_first=memory_prefer_derived_first,#20260626_kpopmodder
        max_deep_recalled_items=memory_max_deep_recalled_items,#20260720_kpopmodder: Avoid 100-item deep recall prompt floods.
    )
    return memory_store, memory_context_builder
