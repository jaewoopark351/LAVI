#20260819_kpopmodder: Parse Windows process command lines without treating arbitrary tokens as entrypoints.
from __future__ import annotations


def parse_windows_command_line(value: object) -> tuple[str, ...]:
    if type(value) is not str or not value.strip():
        return ()
    arguments: list[str] = []
    index = 0
    length = len(value)
    while index < length:
        while index < length and value[index] in " \t":
            index += 1
        if index >= length:
            break
        argument: list[str] = []
        quoted = False
        while index < length:
            character = value[index]
            if character in " \t" and not quoted:
                break
            if character == "\\":
                slash_start = index
                while index < length and value[index] == "\\":
                    index += 1
                slash_count = index - slash_start
                if index < length and value[index] == '"':
                    argument.extend("\\" * (slash_count // 2))
                    if slash_count % 2:
                        argument.append('"')
                        index += 1
                        continue
                    quoted = not quoted
                    index += 1
                    continue
                argument.extend("\\" * slash_count)
                continue
            if character == '"':
                if quoted and index + 1 < length and value[index + 1] == '"':
                    argument.append('"')
                    index += 2
                    continue
                quoted = not quoted
                index += 1
                continue
            argument.append(character)
            index += 1
        if quoted:
            return ()
        arguments.append("".join(argument))
        while index < length and value[index] in " \t":
            index += 1
    return tuple(arguments)
