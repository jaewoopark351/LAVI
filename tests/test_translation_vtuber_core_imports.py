from pathlib import Path


def test_translate_core_exports_component_class():
    from translation_core import Translate
    from translation_core.translate_component import Translate as ComponentTranslate

    assert Translate is ComponentTranslate


def test_vtuber_core_exports_component_class():
    from vtuber_core import Vtuber
    from vtuber_core.vtuber_component import Vtuber as ComponentVtuber

    assert Vtuber is ComponentVtuber


def test_translate_and_vtuber_root_files_are_removed():
    project_root = Path(__file__).resolve().parents[1]

    assert not (project_root / "Translate.py").exists()
    assert not (project_root / "VTuber.py").exists()
