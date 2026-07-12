#20260626_kpopmodder: Disabled unused manual MemoryCommandHandler; keep old code commented for rollback.
# #20260621_kpopmodder: Manual memory commands for shared long-term memory.
# import hashlib
# import re
#
#
# class MemoryCommandHandler:
#     """사용자 명령으로 장기기억을 저장/삭제/조회한다.
#
#     이 핸들러는 LLM 플러그인 바깥의 공통 파이프라인에서 실행된다.
#     따라서 ChatGPT_OpenAI, Transformers_LLM, Hybrid_LLM 모두 같은 long_term_memory.json을 공유한다.
#     """
#
#     REMEMBER_PREFIXES = (
#         "기억해줘",
#         "기억해 둬",
#         "기억해둬",
#         "기억해",
#         "장기기억",
#         "장기 기억",
#         "remember",
#         "save memory",
#         "long term memory",
#         "long-term memory",
#     )
#
#     FORGET_PREFIXES = (
#         "잊어줘",
#         "잊어",
#         "삭제해줘",
#         "기억 삭제",
#         "장기기억 삭제",
#         "장기 기억 삭제",
#         "forget",
#         "delete memory",
#         "remove memory",
#     )
#
#     LIST_COMMANDS = {
#         "기억 목록",
#         "기억 보여줘",
#         "기억 보여 줘",
#         "장기기억 목록",
#         "장기 기억 목록",
#         "장기기억 보여줘",
#         "장기 기억 보여줘",
#         "memory list",
#         "list memory",
#         "list memories",
#         "show memories",
#         "show memory",
#     }
#
#     CLEAR_COMMANDS = {
#         "기억 전부 삭제",
#         "장기기억 전부 삭제",
#         "장기 기억 전부 삭제",
#         "모든 기억 삭제",
#         "모든 장기기억 삭제",
#         "clear memories",
#         "clear memory",
#         "delete all memories",
#     }
#
#     def __init__(self, memory_store):
#         self.memory_store = memory_store
#
#     def try_handle(self, message):
#         text = str(message or "").strip()
#
#         if not text or self.memory_store is None:
#             return None
#
#         remember_value = self._match_remember_command(text)
#         if remember_value is not None:
#             return self._remember(remember_value)
#
#         forget_value = self._match_forget_command(text)
#         if forget_value is not None:
#             return self._forget(forget_value)
#
#         if self._is_list_command(text):
#             return self._list_memories()
#
#         if self._is_clear_command(text):
#             return self._clear_memories()
#
#         return None
#
#     def _make_key(self, value):
#         normalized = " ".join(str(value or "").split())
#         digest = hashlib.sha1(normalized.encode("utf-8")).hexdigest()[:12]
#         return f"manual_{digest}"
#
#     def _strip_prefix(self, text, prefixes):
#         text = str(text or "").strip()
#
#         for prefix in prefixes:
#             #20260621_kpopmodder: "기억해줘: 내용", "기억해줘 내용" 둘 다 허용한다.
#             pattern = rf"^{re.escape(prefix)}(?:\s*[:：]\s*|\s+)(.+)$"
#             match = re.match(pattern, text, re.IGNORECASE)
#             if match:
#                 return match.group(1).strip()
#
#         return None
#
#     def _strip_postfix(self, text, postfixes):
#         text = str(text or "").strip()
#
#         for postfix in postfixes:
#             #20260621_kpopmodder: "사용자는 Ubuntu 선호한다고 기억해줘" 형태도 허용한다.
#             pattern = rf"^(.+?)\s*{re.escape(postfix)}[.!?。！？\s]*$"
#             match = re.match(pattern, text, re.IGNORECASE)
#             if match:
#                 return match.group(1).strip()
#
#         return None
#
#     def _match_remember_command(self, text):
#         value = self._strip_prefix(text, self.REMEMBER_PREFIXES)
#         if value:
#             return self._clean_memory_value(value)
#
#         value = self._strip_postfix(
#             text,
#             (
#                 "기억해줘",
#                 "기억해 둬",
#                 "기억해둬",
#                 "기억해",
#             ),
#         )
#         if value:
#             return self._clean_memory_value(value)
#
#         return None
#
#     def _match_forget_command(self, text):
#         value = self._strip_prefix(text, self.FORGET_PREFIXES)
#         if value:
#             return self._clean_memory_value(value)
#
#         value = self._strip_postfix(
#             text,
#             (
#                 "잊어줘",
#                 "잊어",
#                 "삭제해줘",
#                 "기억 삭제해줘",
#             ),
#         )
#         if value:
#             return self._clean_memory_value(value)
#
#         return None
#
#     def _is_list_command(self, text):
#         normalized = self._normalize_command_text(text)
#         return normalized in {
#             self._normalize_command_text(command)
#             for command in self.LIST_COMMANDS
#         }
#
#     def _is_clear_command(self, text):
#         normalized = self._normalize_command_text(text)
#         return normalized in {
#             self._normalize_command_text(command)
#             for command in self.CLEAR_COMMANDS
#         }
#
#     def _normalize_command_text(self, text):
#         return " ".join(str(text or "").strip().lower().split())
#
#     def _clean_memory_value(self, value):
#         value = str(value or "").strip()
#         value = value.strip(" \"'“”‘’`")
#         value = " ".join(value.split())
#         return value
#
#     def _remember(self, value):
#         value = self._clean_memory_value(value)
#
#         if not value:
#             return "기억할 내용이 비어 있습니다."
#
#         key = self._make_key(value)
#
#         self.memory_store.set_long_term_memory(
#             key=key,
#             value=value,
#             source="user_command",
#             confidence=0.95,
#         )
#
#         return f"장기기억에 저장했습니다: {value}"
#
#     def _forget(self, query):
#         query = self._clean_memory_value(query)
#
#         if not query:
#             return "삭제할 기억 내용을 알려주세요."
#
#         removed = self.memory_store.delete_long_term_memory_by_query(query)
#
#         if not removed:
#             return f"삭제할 장기기억을 찾지 못했습니다: {query}"
#
#         return f"장기기억 {len(removed)}개를 삭제했습니다."
#
#     def _clear_memories(self):
#         long_term_memory = self.memory_store.get_long_term_memory()
#
#         if not long_term_memory:
#             return "삭제할 장기기억이 없습니다."
#
#         removed_count = 0
#
#         for key in list(long_term_memory.keys()):
#             removed = self.memory_store.delete_long_term_memory(key)
#             if removed is not None:
#                 removed_count += 1
#
#         return f"장기기억 {removed_count}개를 모두 삭제했습니다."
#
#     def _list_memories(self):
#         long_term_memory = self.memory_store.get_long_term_memory()
#
#         if not long_term_memory:
#             return "저장된 장기기억이 아직 없습니다."
#
#         lines = ["현재 장기기억 목록입니다."]
#
#         for index, item in enumerate(long_term_memory.values(), start=1):
#             value = str(item.get("value", "")).strip()
#             updated_at = item.get("updated_at") or item.get("created_at") or ""
#             source = str(item.get("source", "unknown"))
#
#             if not value:
#                 continue
#
#             if updated_at:
#                 lines.append(f"{index}. {value} ({source}, {updated_at})")
#             else:
#                 lines.append(f"{index}. {value} ({source})")
#
#         if len(lines) == 1:
#             return "저장된 장기기억이 아직 없습니다."
#
#         return "\n".join(lines)