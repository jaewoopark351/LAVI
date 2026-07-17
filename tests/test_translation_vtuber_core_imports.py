from pathlib import Path


def test_translate_core_exports_component_class():
    from translation_core import Translate
    from translation_core.translate_component import Translate as ComponentTranslate

    assert Translate is ComponentTranslate


def test_vtuber_core_exports_component_class():
    from vtuber_core import Vtuber
    from vtuber_core.vtuber_component import Vtuber as ComponentVtuber

    assert Vtuber is ComponentVtuber


def test_llm_core_exports_component_class():
    from llm_core import LLM
    from llm_core.llm_component import LLM as ComponentLLM

    assert LLM is ComponentLLM


def test_tts_core_exports_component_class():
    from tts_core import TTS
    from tts_core.tts_component import TTS as ComponentTTS

    assert TTS is ComponentTTS


def test_app_composer_delegates_core_component_imports():
    project_root = Path(__file__).resolve().parents[1]
    app_composer_source = (project_root / "app_core" / "app_composer.py").read_text(
        encoding="utf-8",
    )
    composition_facade_source = (
        project_root / "app_core" / "core_component_composition.py"
    ).read_text(encoding="utf-8")
    composition_service_source = (
        project_root
        / "app_core"
        / "composition_core"
        / "core_component_composition_service.py"
    ).read_text(encoding="utf-8")

    assert "from llm_core.llm_component import LLM" not in app_composer_source
    assert "from tts_core.tts_component import TTS" not in app_composer_source
    assert "CoreComponentCompositionService" in app_composer_source
    assert "from llm_core.llm_component import LLM" in composition_service_source
    assert "from tts_core.tts_component import TTS" in composition_service_source
    checked_source = app_composer_source + composition_facade_source + composition_service_source
    assert "from LLM import LLM" not in checked_source
    assert "from TTS import TTS" not in checked_source


def test_legacy_root_component_files_are_removed():
    project_root = Path(__file__).resolve().parents[1]

    assert not (project_root / "LLM.py").exists()
    assert not (project_root / "TTS.py").exists()
    assert not (project_root / "Translate.py").exists()
    assert not (project_root / "VTuber.py").exists()
