#20260706_kpopmodder: Split ScreenVision observation policy text helpers from the policy facade.
from pathlib import Path
import re


def load_broken_exact_texts(broken_texts_path, default_config_dir, file_name):
    path = (
        Path(broken_texts_path)
        if broken_texts_path
        else Path(default_config_dir) / file_name
    )

    if not path.exists():
        return set()

    result = set()
    try:
        for line in path.read_text(encoding="utf-8").splitlines():
            text = line.split("#", 1)[0].strip()
            if text:
                result.add(text)
    except Exception:
        #20260706_kpopmodder: Keep policy fallback stable if the optional blocklist cannot be read.
        return set()

    return result


def strip_screen_prefix(text, prefixes):
    text = str(text or "").strip()
    changed = True
    while changed:
        changed = False
        for prefix in prefixes:
            if text.startswith(prefix):
                text = text[len(prefix):].strip()
                changed = True
    return text


def normalize_observation_text(observation, prefixes):
    text = str(observation or "").strip()
    text = text.replace("\u200b", " ").replace("\ufeff", " ")
    text = re.sub(r"^[\-??\d\.\)\s]+", "", text)
    text = " ".join(text.split())
    text = strip_screen_prefix(text, prefixes)
    return text.strip()


def canonical_decision_text(observation, prefixes, punctuation):
    text = normalize_observation_text(observation, prefixes).lower()
    for char in punctuation:
        text = text.replace(char, "")
    return " ".join(text.split())


def compact_decision_text(observation):
    return "".join(str(observation or "").lower().split())
