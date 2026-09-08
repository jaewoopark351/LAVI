#20260905_kpopmodder: Enforce focused LLM compatibility composition boundaries.
from __future__ import annotations

import ast
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parents[3]
INSTALLER_PATH = (
    PROJECT_ROOT / "llm_core/composition/llm_compatibility_graph_installer.py"
)
GRAPH_PATHS = (
    PROJECT_ROOT
    / "llm_core/composition/speech_content/"
    "llm_speech_content_component_graph.py",
    PROJECT_ROOT
    / "llm_core/composition/generation_output/"
    "llm_generation_output_component_graph.py",
    PROJECT_ROOT
    / "llm_core/composition/trusted_ingress/"
    "llm_trusted_ingress_component_graph.py",
    PROJECT_ROOT
    / "llm_core/composition/ui_lifecycle/"
    "llm_ui_lifecycle_component_graph.py",
)
OBSOLETE_PACKAGE_ROOTS = (
    PROJECT_ROOT
    / "app_core/composition_core/component_wiring/"
    "direct_game_input/composition",
    PROJECT_ROOT
    / "input_core/input_event/delivery/trusted_voice/composition",
    PROJECT_ROOT / "llm_core/chat_input/trusted_local/composition",
    PROJECT_ROOT / "llm_core/content/system_prompt",
    PROJECT_ROOT / "llm_core/input_queue/acceptance",
    PROJECT_ROOT / "llm_core/input_queue/composition",
    PROJECT_ROOT / "llm_core/input_queue/submission",
    PROJECT_ROOT / "llm_core/input_routing/composition",
    PROJECT_ROOT / "plugins/Minecraft/fabric/chatclef/extension/lifecycle",
    PROJECT_ROOT / "plugins/Minecraft/fabric/chatclef/extension/stop",
    PROJECT_ROOT / "plugins/Minecraft/fabric/chatclef/extension/status",
    PROJECT_ROOT
    / "plugins/Minecraft/fabric/chatclef/extension/natural_language/legacy",
)
PUBLIC_INSTALLER_METHODS = (
    "install",
    "ensure_speech_style_helper",
    "ensure_speech_style_state_controller",
    "ensure_speech_style_persistence",
    "ensure_speech_style_update_coordinator",
    "ensure_effective_system_prompt_builder",
    "ensure_content_repository",
    "ensure_generation_facade",
    "ensure_output_listener_registry",
    "ensure_chat_history_resetter",
    "ensure_ui_builder",
    "ensure_input_event_normalizer",
    "ensure_trusted_ingress_graph",
    "sync_trusted_ingress_compatibility_fields",
    "ensure_local_chat_input_adapter",
    "ensure_trusted_local_chat_input_dispatch_coordinator",
    "ensure_local_chat_prediction_entrypoint",
    "ensure_local_chat_interface_factory",
    "set_input_router",
    "ensure_routed_input_dispatch_coordinator",
    "ensure_routed_external_response_publisher",
    "ensure_ui_presentation_queue",
    "ensure_prediction_dispatch_coordinator",
    "ensure_text_only_generation_helper",
    "ensure_input_queue_display_updater",
    "ensure_runtime_lifecycle_coordinator",
)


def test_installer_preserves_public_api_without_reabsorbing_constructors():
    source = INSTALLER_PATH.read_text(encoding="utf-8")
    tree = ast.parse(source)
    installer = next(
        node
        for node in tree.body
        if isinstance(node, ast.ClassDef)
        and node.name == "LlmCompatibilityGraphInstaller"
    )
    public_methods = tuple(
        node.name
        for node in installer.body
        if isinstance(node, ast.FunctionDef)
        and not node.name.startswith("_")
    )

    assert public_methods == PUBLIC_INSTALLER_METHODS
    for extracted_owner in (
        "LLMContextManager",
        "LLMResponsePipeline",
        "LlmTrustedIngressGraph",
        "LlmChatUiBuilder",
        "LLMInputQueueWorker",
    ):
        assert extracted_owner not in source


def test_child_graph_modules_follow_new_module_contract():
    for path in GRAPH_PATHS:
        source = path.read_text(encoding="utf-8")
        tree = ast.parse(source)
        classes = tuple(
            node for node in tree.body if isinstance(node, ast.ClassDef)
        )
        assert source.splitlines()[0].startswith("#20260905_kpopmodder"), path
        assert len(classes) == 1, path
        assert _uses_tuple_all(tree), path
        assert not _mutable_static_literals(tree), path


def test_flattened_packages_leave_no_python_compatibility_modules():
    for package_root in OBSOLETE_PACKAGE_ROOTS:
        assert not tuple(package_root.glob("*.py")), package_root


def _uses_tuple_all(tree: ast.Module) -> bool:
    return any(
        isinstance(node, ast.Assign)
        and any(
            isinstance(target, ast.Name) and target.id == "__all__"
            for target in node.targets
        )
        and isinstance(node.value, ast.Tuple)
        for node in tree.body
    )


def _mutable_static_literals(tree: ast.Module):
    mutable_literals = (ast.List, ast.Dict, ast.Set)
    owners = (
        tree,
        *(node for node in tree.body if isinstance(node, ast.ClassDef)),
    )
    return tuple(
        node
        for owner in owners
        for node in owner.body
        if isinstance(node, (ast.Assign, ast.AnnAssign))
        and isinstance(getattr(node, "value", None), mutable_literals)
    )
