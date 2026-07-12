#20260706_kpopmodder: Unit lookup and Korean particle helpers are split from reaction policy text assembly.


def find_starcraft116_unit_mention(event, roles):
    role_set = {str(role) for role in roles}
    for mention in event.get("unit_mentions", []):
        if str(mention.get("role", "")) in role_set:
            return mention
    return None


def starcraft116_base_type(raw_type):
    base = str(raw_type or "").replace(" ", "_")
    for prefix in ("Terran_", "Protoss_", "Zerg_"):
        if base.startswith(prefix):
            base = base[len(prefix):]
            break
    return base


def starcraft116_subject(name):
    return str(name) + ("\uc774" if starcraft116_has_final_consonant(name) else "\uac00")


def starcraft116_object(name):
    return str(name) + ("\uc744" if starcraft116_has_final_consonant(name) else "\ub97c")


def starcraft116_has_final_consonant(name):
    text = str(name or "").strip()
    if not text:
        return False
    last_char = text[-1]
    codepoint = ord(last_char)
    if 0xAC00 <= codepoint <= 0xD7A3:
        return (codepoint - 0xAC00) % 28 != 0
    return False
