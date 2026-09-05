#20260905_kpopmodder: Enforces structure for the follow-up responsibility split.
from __future__ import annotations

import ast
from pathlib import Path


_ROOTS = (
    Path("llm_core/input_queue"),
    Path("llm_core/lifecycle"),
    Path("llm_core/content"),
    Path("llm_core/routed_response"),
    Path("input_core/input_event/provenance/trusted_user_ingress"),
    Path("input_core/input_event/adapters/provider_bound"),
    Path("input_core/component/provider_selection"),
    Path("plugins/Minecraft/fabric/chatclef/input/eligibility"),
)


def _production_files():
    files = {
        path
        for root in _ROOTS
        for path in root.rglob("*.py")
    }
    files.update(
        {
            Path("llm_core/input_queue_worker.py"),
            Path("llm_core/input_queue_runtime.py"),
            Path("input_core/input_event/adapters/provider_bound_input_event_adapter.py"),
        }
    )
    return tuple(sorted(files))


def test_new_production_modules_have_exact_marker_and_tuple_exports():
    for path in _production_files():
        source = path.read_text(encoding="utf-8")
        assert any(
            line.startswith("#20260905_kpopmodder")
            for line in source.splitlines()[:5]
        ), path
        tree = ast.parse(source)
        for node in tree.body:
            if isinstance(node, (ast.Assign, ast.AnnAssign)):
                targets = (
                    node.targets
                    if isinstance(node, ast.Assign)
                    else (node.target,)
                )
                if any(
                    isinstance(target, ast.Name) and target.id == "__all__"
                    for target in targets
                ):
                    assert isinstance(node.value, ast.Tuple), path


def test_new_production_modules_keep_one_top_level_project_class():
    for path in _production_files():
        tree = ast.parse(path.read_text(encoding="utf-8"))
        classes = [node for node in tree.body if isinstance(node, ast.ClassDef)]
        assert len(classes) <= 1, path


def test_new_production_modules_have_no_mutable_module_or_class_literals():
    mutable_literals = (ast.List, ast.Dict, ast.Set, ast.ListComp, ast.DictComp, ast.SetComp)
    for path in _production_files():
        tree = ast.parse(path.read_text(encoding="utf-8"))
        owners = (tree, *(node for node in tree.body if isinstance(node, ast.ClassDef)))
        for owner in owners:
            for node in owner.body:
                if isinstance(node, (ast.Assign, ast.AnnAssign)):
                    assert not isinstance(node.value, mutable_literals), path


def test_compatibility_facades_do_not_reabsorb_extracted_dependencies():
    forbidden_imports = {
        Path("input_core/input_event/adapters/provider_bound_input_event_adapter.py"): {
            "secrets",
            "InputEventTextNormalizer",
            "InputProviderSourceResolver",
        },
        Path("plugins/Minecraft/fabric/chatclef/input/eligibility/korean_chat_microphone_eligibility_proof.py"): {
            "hashlib",
            "threading",
        },
        Path("input_core/input_event/provenance/trusted_user_ingress/consumed_ingress_evidence.py"): {
            "hashlib",
            "threading",
        },
    }
    for path, names in forbidden_imports.items():
        tree = ast.parse(path.read_text(encoding="utf-8"))
        imported = {
            alias.name.split(".")[0]
            for node in tree.body
            if isinstance(node, ast.Import)
            for alias in node.names
        }
        imported.update(
            alias.name
            for node in tree.body
            if isinstance(node, ast.ImportFrom)
            for alias in node.names
        )
        assert imported.isdisjoint(names), path
