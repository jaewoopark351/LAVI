//20260705_kpopmodder: Diagnostic BWAPI.dll proxy probe for Monster.exe load-path testing only.
#include <Windows.h>

#include <BWAPI/Client/GameData.h>
#include <BWAPI/Client/GameTable.h>

#include <cstdio>
#include <cstring>

namespace
{
HMODULE g_realBwapi = nullptr;
char g_moduleDir[MAX_PATH] = {0};
char g_modulePath[MAX_PATH] = {0};
char g_processPath[MAX_PATH] = {0};
char g_currentDir[MAX_PATH] = {0};
volatile LONG g_workerStarted = 0;

struct UnitTrack
{
  bool initialized;
  bool exists;
  bool completed;
  bool visibleToSelf;
  bool attacking;
  int unitId;
  int replayId;
  int player;
  int type;
  int x;
  int y;
  int hitPoints;
  int shields;
  int order;
  int lastEmitFrame;
  int lastUnderAttackFrame;
  int lastCombatFrame;
  int lastSeenFrame;
};

UnitTrack g_unitTracks[10000] = {};
bool g_gameSnapshotInitialized = false;
int g_lastSupplyBlockedFrame = -100000;

void appendRawLine(const char* logPath, const char* line)
{
  if (!logPath || !logPath[0] || !line)
  {
    return;
  }

  HANDLE file = CreateFileA(
    logPath,
    FILE_APPEND_DATA,
    FILE_SHARE_READ | FILE_SHARE_WRITE,
    nullptr,
    OPEN_ALWAYS,
    FILE_ATTRIBUTE_NORMAL,
    nullptr);
  if (file == INVALID_HANDLE_VALUE)
  {
    return;
  }

  DWORD written = 0;
  WriteFile(file, line, static_cast<DWORD>(std::strlen(line)), &written, nullptr);
  CloseHandle(file);
}

void appendJsonEscaped(char* out, size_t outSize, const char* value)
{
  if (!out || outSize == 0)
  {
    return;
  }

  size_t offset = std::strlen(out);
  const unsigned char* cursor =
    reinterpret_cast<const unsigned char*>(value ? value : "");
  while (*cursor && offset + 2 < outSize)
  {
    unsigned char ch = *cursor++;
    if (ch == '\\' || ch == '"')
    {
      if (offset + 3 >= outSize)
      {
        break;
      }
      out[offset++] = '\\';
      out[offset++] = static_cast<char>(ch);
    }
    else if (ch == '\r')
    {
      if (offset + 3 >= outSize)
      {
        break;
      }
      out[offset++] = '\\';
      out[offset++] = 'r';
    }
    else if (ch == '\n')
    {
      if (offset + 3 >= outSize)
      {
        break;
      }
      out[offset++] = '\\';
      out[offset++] = 'n';
    }
    else if (ch < 0x20)
    {
      if (offset + 7 >= outSize)
      {
        break;
      }
      int written = std::snprintf(
        out + offset,
        outSize - offset,
        "\\u%04x",
        static_cast<unsigned int>(ch));
      if (written <= 0)
      {
        break;
      }
      offset += static_cast<size_t>(written);
    }
    else
    {
      out[offset++] = static_cast<char>(ch);
    }
    out[offset] = '\0';
  }
  out[outSize - 1] = '\0';
}

void appendJsonStringField(
  char* out,
  size_t outSize,
  const char* name,
  const char* value,
  bool prependComma)
{
  if (!out || outSize == 0)
  {
    return;
  }
  std::strncat(out, prependComma ? ",\"" : "\"", outSize - std::strlen(out) - 1);
  appendJsonEscaped(out, outSize, name);
  std::strncat(out, "\":\"", outSize - std::strlen(out) - 1);
  appendJsonEscaped(out, outSize, value);
  std::strncat(out, "\"", outSize - std::strlen(out) - 1);
}

void directoryOf(const char* path, char* out, DWORD outSize)
{
  if (!out || outSize == 0)
  {
    return;
  }
  out[0] = '\0';
  if (!path || !path[0])
  {
    return;
  }
  lstrcpynA(out, path, outSize);
  for (int index = lstrlenA(out) - 1; index >= 0; --index)
  {
    if (out[index] == '\\' || out[index] == '/')
    {
      out[index] = '\0';
      return;
    }
  }
  out[0] = '\0';
}

void appendJsonEvent(
  const char* moduleDir,
  const char* eventType,
  const char* summary,
  const char* severity,
  const char* modulePath,
  const char* processPath,
  const char* currentDir)
{
  char eventPath[MAX_PATH] = {0};
  if (moduleDir && moduleDir[0])
  {
    std::snprintf(
      eventPath,
      sizeof(eventPath),
      "%s\\bwapi_proxy_events.jsonl",
      moduleDir);
  }
  else
  {
    lstrcpynA(eventPath, "bwapi_proxy_events.jsonl", sizeof(eventPath));
  }

  SYSTEMTIME now = {};
  GetLocalTime(&now);
  char timeText[64] = {0};
  std::snprintf(
    timeText,
    sizeof(timeText),
    "%04u-%02u-%02uT%02u:%02u:%02u.%03u+09:00",
    now.wYear,
    now.wMonth,
    now.wDay,
    now.wHour,
    now.wMinute,
    now.wSecond,
    now.wMilliseconds);

  char pidText[32] = {0};
  std::snprintf(pidText, sizeof(pidText), "%lu", GetCurrentProcessId());

  char json[8192] = {0};
  std::strncat(json, "{", sizeof(json) - std::strlen(json) - 1);
  appendJsonStringField(json, sizeof(json), "schema", "lav_starcraft116_bwapi_proxy_event_v1", false);
  appendJsonStringField(json, sizeof(json), "source", "BWAPI.dll proxy", true);
  appendJsonStringField(json, sizeof(json), "event_type", eventType, true);
  appendJsonStringField(json, sizeof(json), "summary", summary, true);
  appendJsonStringField(json, sizeof(json), "severity", severity, true);
  appendJsonStringField(json, sizeof(json), "time", timeText, true);
  appendJsonStringField(json, sizeof(json), "pid", pidText, true);
  std::strncat(json, ",\"tts_eligible\":true,\"details\":{", sizeof(json) - std::strlen(json) - 1);
  appendJsonStringField(json, sizeof(json), "module_path", modulePath, false);
  appendJsonStringField(json, sizeof(json), "process_path", processPath, true);
  appendJsonStringField(json, sizeof(json), "cwd", currentDir, true);
  std::strncat(json, "}}\r\n", sizeof(json) - std::strlen(json) - 1);

  appendRawLine(eventPath, json);

  char publicPath[MAX_PATH] =
    "C:\\Users\\Public\\Documents\\ESTsoft\\CreatorTemp\\bwapi_proxy_events.jsonl";
  appendRawLine(publicPath, json);
}

const char* unitTypeName(int type)
{
  switch (type)
  {
  case 0: return "Terran Marine";
  case 7: return "Terran SCV";
  case 32: return "Terran Firebat";
  case 34: return "Terran Medic";
  case 37: return "Zerg Zergling";
  case 38: return "Zerg Hydralisk";
  case 39: return "Zerg Ultralisk";
  case 41: return "Zerg Drone";
  case 42: return "Zerg Overlord";
  case 43: return "Zerg Mutalisk";
  case 65: return "Protoss Probe";
  case 66: return "Protoss Zealot";
  case 67: return "Protoss Dragoon";
  default: return "Unit";
  }
}

const char* ownerName(int player, int self)
{
  return player == self ? "self" : "enemy";
}

bool isWorkerType(int type)
{
  return type == 7 || type == 41 || type == 65;
}

bool isBuildingType(int type)
{
  return type >= 106 && type <= 201;
}

bool isCombatUnitType(int type)
{
  return type >= 0 && type < 228 && !isWorkerType(type) && !isBuildingType(type);
}

bool isAttackOrder(int order)
{
  return order == 10 || order == 11 || order == 14;
}

void appendUnitStateEvent(
  const char* eventType,
  const char* summarySuffix,
  const char* severity,
  int frame,
  int unitIndex,
  int unitId,
  int replayId,
  int player,
  int self,
  int type,
  int fromX,
  int fromY,
  int toX,
  int toY,
  int hitPoints,
  int shields,
  bool isMoving,
  bool isAttacking,
  bool isCompleted,
  int order,
  int targetX,
  int targetY,
  bool ttsEligible)
{
  char eventPath[MAX_PATH] = {0};
  if (g_moduleDir[0])
  {
    std::snprintf(
      eventPath,
      sizeof(eventPath),
      "%s\\bwapi_proxy_events.jsonl",
      g_moduleDir);
  }
  else
  {
    lstrcpynA(eventPath, "bwapi_proxy_events.jsonl", sizeof(eventPath));
  }

  SYSTEMTIME now = {};
  GetLocalTime(&now);
  char timeText[64] = {0};
  std::snprintf(
    timeText,
    sizeof(timeText),
    "%04u-%02u-%02uT%02u:%02u:%02u.%03u+09:00",
    now.wYear,
    now.wMonth,
    now.wDay,
    now.wHour,
    now.wMinute,
    now.wSecond,
    now.wMilliseconds);

  char pidText[32] = {0};
  std::snprintf(pidText, sizeof(pidText), "%lu", GetCurrentProcessId());

  const char* typeName = unitTypeName(type);
  char summary[256] = {0};
  std::snprintf(
    summary,
    sizeof(summary),
    "%s %s",
    typeName,
    summarySuffix ? summarySuffix : "event detected.");

  char json[8192] = {0};
  std::strncat(json, "{", sizeof(json) - std::strlen(json) - 1);
  appendJsonStringField(json, sizeof(json), "schema", "lav_starcraft116_bwapi_proxy_event_v1", false);
  appendJsonStringField(json, sizeof(json), "source", "BWAPI.dll proxy shared-memory poller", true);
  appendJsonStringField(json, sizeof(json), "event_type", eventType, true);
  appendJsonStringField(json, sizeof(json), "summary", summary, true);
  appendJsonStringField(json, sizeof(json), "severity", severity, true);
  appendJsonStringField(json, sizeof(json), "time", timeText, true);
  appendJsonStringField(json, sizeof(json), "pid", pidText, true);
  std::strncat(
    json,
    ttsEligible ? ",\"tts_eligible\":true,\"details\":{" : ",\"tts_eligible\":false,\"details\":{",
    sizeof(json) - std::strlen(json) - 1);
  appendJsonStringField(json, sizeof(json), "module_path", g_modulePath, false);
  appendJsonStringField(json, sizeof(json), "process_path", g_processPath, true);
  appendJsonStringField(json, sizeof(json), "cwd", g_currentDir, true);
  std::strncat(json, ",\"unit\":{", sizeof(json) - std::strlen(json) - 1);
  appendJsonStringField(json, sizeof(json), "type", typeName, false);
  appendJsonStringField(json, sizeof(json), "owner", ownerName(player, self), true);
  std::snprintf(
    json + std::strlen(json),
    sizeof(json) - std::strlen(json),
    ",\"id\":%d,\"replay_id\":%d,\"unit_index\":%d,"
    "\"player\":%d,\"type_id\":%d,\"hit_points\":%d,\"shields\":%d,"
    "\"is_moving\":%s,\"is_attacking\":%s,\"is_completed\":%s,"
    "\"from\":{\"x\":%d,\"y\":%d},\"to\":{\"x\":%d,\"y\":%d},"
    "\"order\":%d,\"target\":{\"x\":%d,\"y\":%d},\"frame\":%d",
    unitId,
    replayId,
    unitIndex,
    player,
    type,
    hitPoints,
    shields,
    isMoving ? "true" : "false",
    isAttacking ? "true" : "false",
    isCompleted ? "true" : "false",
    fromX,
    fromY,
    toX,
    toY,
    order,
    targetX,
    targetY,
    frame);
  std::strncat(json, "}}}\r\n", sizeof(json) - std::strlen(json) - 1);

  appendRawLine(eventPath, json);
  appendRawLine(
    "C:\\Users\\Public\\Documents\\ESTsoft\\CreatorTemp\\bwapi_proxy_events.jsonl",
    json);
}

void appendSimpleUnitEvent(
  const char* eventType,
  const char* summarySuffix,
  const char* severity,
  int frame,
  int unitIndex,
  const BWAPI::UnitData& unit,
  int self,
  bool ttsEligible)
{
  appendUnitStateEvent(
    eventType,
    summarySuffix,
    severity,
    frame,
    unitIndex,
    unit.id,
    unit.replayID,
    unit.player,
    self,
    unit.type,
    unit.positionX,
    unit.positionY,
    unit.positionX,
    unit.positionY,
    unit.hitPoints,
    unit.shields,
    unit.isMoving,
    unit.isAttacking || unit.isStartingAttack,
    unit.isCompleted,
    unit.order,
    unit.orderTargetPositionX,
    unit.orderTargetPositionY,
    ttsEligible);
}

void appendUnitMovementEvent(
  int frame,
  int unitIndex,
  const BWAPI::UnitData& unit,
  const UnitTrack& track,
  int self)
{
  appendUnitStateEvent(
    "unit_moved",
    "movement detected.",
    "info",
    frame,
    unitIndex,
    unit.id,
    unit.replayID,
    unit.player,
    self,
    unit.type,
    track.x,
    track.y,
    unit.positionX,
    unit.positionY,
    unit.hitPoints,
    unit.shields,
    unit.isMoving,
    unit.isAttacking || unit.isStartingAttack,
    unit.isCompleted,
    unit.order,
    unit.orderTargetPositionX,
    unit.orderTargetPositionY,
    unit.player == self && isCombatUnitType(unit.type));
}

void appendSupplyBlockedEvent(int frame, int self, int used, int total)
{
  char eventPath[MAX_PATH] = {0};
  if (g_moduleDir[0])
  {
    std::snprintf(eventPath, sizeof(eventPath), "%s\\bwapi_proxy_events.jsonl", g_moduleDir);
  }
  else
  {
    lstrcpynA(eventPath, "bwapi_proxy_events.jsonl", sizeof(eventPath));
  }

  SYSTEMTIME now = {};
  GetLocalTime(&now);
  char timeText[64] = {0};
  std::snprintf(
    timeText,
    sizeof(timeText),
    "%04u-%02u-%02uT%02u:%02u:%02u.%03u+09:00",
    now.wYear,
    now.wMonth,
    now.wDay,
    now.wHour,
    now.wMinute,
    now.wSecond,
    now.wMilliseconds);

  char pidText[32] = {0};
  std::snprintf(pidText, sizeof(pidText), "%lu", GetCurrentProcessId());

  char json[4096] = {0};
  std::strncat(json, "{", sizeof(json) - std::strlen(json) - 1);
  appendJsonStringField(json, sizeof(json), "schema", "lav_starcraft116_bwapi_proxy_event_v1", false);
  appendJsonStringField(json, sizeof(json), "source", "BWAPI.dll proxy shared-memory poller", true);
  appendJsonStringField(json, sizeof(json), "event_type", "supply_blocked", true);
  appendJsonStringField(json, sizeof(json), "summary", "Supply is blocked.", true);
  appendJsonStringField(json, sizeof(json), "severity", "warning", true);
  appendJsonStringField(json, sizeof(json), "time", timeText, true);
  appendJsonStringField(json, sizeof(json), "pid", pidText, true);
  std::snprintf(
    json + std::strlen(json),
    sizeof(json) - std::strlen(json),
    ",\"tts_eligible\":true,\"details\":{");
  appendJsonStringField(json, sizeof(json), "module_path", g_modulePath, false);
  std::snprintf(
    json + std::strlen(json),
    sizeof(json) - std::strlen(json),
    ",\"player\":%d,\"supply_used\":%d,\"supply_total\":%d,\"frame\":%d}}\r\n",
    self,
    used,
    total,
    frame);

  appendRawLine(eventPath, json);
  appendRawLine(
    "C:\\Users\\Public\\Documents\\ESTsoft\\CreatorTemp\\bwapi_proxy_events.jsonl",
    json);
}

void appendLogLine(const char* moduleDir, const char* message)
{
  SYSTEMTIME now = {};
  GetLocalTime(&now);
  char line[2048] = {0};
  std::snprintf(
    line,
    sizeof(line),
    "%04u-%02u-%02u %02u:%02u:%02u.%03u pid=%lu %s\r\n",
    now.wYear,
    now.wMonth,
    now.wDay,
    now.wHour,
    now.wMinute,
    now.wSecond,
    now.wMilliseconds,
    GetCurrentProcessId(),
    message ? message : "");

  OutputDebugStringA("[LAV BWAPI PROXY] ");
  OutputDebugStringA(line);

  char logPath[MAX_PATH] = {0};
  if (moduleDir && moduleDir[0])
  {
    std::snprintf(logPath, sizeof(logPath), "%s\\bwapi_proxy_probe_log.txt", moduleDir);
    appendRawLine(logPath, line);

    char monsterLogPath[MAX_PATH] = {0};
    std::snprintf(monsterLogPath, sizeof(monsterLogPath), "%s\\monster_log.txt", moduleDir);
    appendRawLine(monsterLogPath, "[BWAPI_PROXY_PROBE] ");
    appendRawLine(monsterLogPath, line);
  }
  else
  {
    appendRawLine("bwapi_proxy_probe_log.txt", line);
  }

  char tempDir[MAX_PATH] = {0};
  if (GetTempPathA(sizeof(tempDir), tempDir) > 0)
  {
    char tempLogPath[MAX_PATH] = {0};
    std::snprintf(tempLogPath, sizeof(tempLogPath), "%sbwapi_proxy_probe_log.txt", tempDir);
    appendRawLine(tempLogPath, line);
  }

  appendRawLine(
    "C:\\Users\\Public\\Documents\\ESTsoft\\CreatorTemp\\bwapi_proxy_probe_log.txt",
    line);

  appendRawLine(
    "C:\\Vtuber_Souorce_Code\\StarCraft_1.16\\Monster\\monster_log.txt",
    "[BWAPI_PROXY_PROBE] ");
  appendRawLine(
    "C:\\Vtuber_Souorce_Code\\StarCraft_1.16\\Monster\\monster_log.txt",
    line);
}

int findServerProcessIdFromGameTable()
{
  HANDLE tableHandle = OpenFileMappingA(
    FILE_MAP_READ,
    FALSE,
    "Local\\bwapi_shared_memory_game_list");
  if (!tableHandle)
  {
    return -1;
  }

  BWAPI::GameTable* table = static_cast<BWAPI::GameTable*>(
    MapViewOfFile(tableHandle, FILE_MAP_READ, 0, 0, sizeof(BWAPI::GameTable)));
  if (!table)
  {
    CloseHandle(tableHandle);
    return -1;
  }

  const DWORD currentPid = GetCurrentProcessId();
  int fallbackPid = -1;
  __try
  {
    for (int i = 0; i < BWAPI::GameTable::MAX_GAME_INSTANCES; ++i)
    {
      const unsigned int pid = table->gameInstances[i].serverProcessID;
      if (!pid)
      {
        continue;
      }
      if (pid == currentPid)
      {
        fallbackPid = static_cast<int>(pid);
        break;
      }
      fallbackPid = static_cast<int>(pid);
    }
  }
  __except (EXCEPTION_EXECUTE_HANDLER)
  {
    fallbackPid = -1;
  }

  UnmapViewOfFile(table);
  CloseHandle(tableHandle);
  return fallbackPid;
}

void resetUnitTracks()
{
  for (int i = 0; i < 10000; ++i)
  {
    g_unitTracks[i].initialized = false;
    g_unitTracks[i].exists = false;
  }
  g_gameSnapshotInitialized = false;
}

void updateTrackFromUnit(UnitTrack& track, const BWAPI::UnitData& unit, bool visibleToSelf, bool attackingNow, int frame)
{
  track.initialized = true;
  track.exists = unit.exists;
  track.completed = unit.isCompleted;
  track.visibleToSelf = visibleToSelf;
  track.attacking = attackingNow;
  track.unitId = unit.id;
  track.replayId = unit.replayID;
  track.player = unit.player;
  track.type = unit.type;
  track.x = unit.positionX;
  track.y = unit.positionY;
  track.hitPoints = unit.hitPoints;
  track.shields = unit.shields;
  track.order = unit.order;
  if (track.lastEmitFrame == 0)
  {
    track.lastEmitFrame = frame;
  }
  if (track.lastUnderAttackFrame == 0)
  {
    track.lastUnderAttackFrame = frame;
  }
  if (track.lastCombatFrame == 0)
  {
    track.lastCombatFrame = frame;
  }
  if (visibleToSelf)
  {
    track.lastSeenFrame = frame;
  }
}

void pollGameState(BWAPI::GameData* data)
{
  if (!data)
  {
    return;
  }

  __try
  {
    if (!data->isInGame || data->self < 0)
    {
      resetUnitTracks();
      return;
    }

    const int frame = data->frameCount;
    const int self = data->self;
    if (self >= 0 && self < 12)
    {
      const int used = data->players[self].supplyUsed[0];
      const int total = data->players[self].supplyTotal[0];
      if (total > 0 && used >= total && frame - g_lastSupplyBlockedFrame >= 720)
      {
        appendSupplyBlockedEvent(frame, self, used, total);
        g_lastSupplyBlockedFrame = frame;
      }
    }

    for (int i = 0; i < 10000; ++i)
    {
      const BWAPI::UnitData& unit = data->units[i];
      UnitTrack& track = g_unitTracks[i];
      const bool visibleToSelf =
        self >= 0 && self < 9 && unit.exists && unit.isVisible[self];
      const bool attackingNow =
        unit.exists && (unit.isAttacking || unit.isStartingAttack || isAttackOrder(unit.order));

      if (!unit.exists)
      {
        if (g_gameSnapshotInitialized && track.initialized && track.exists)
        {
          BWAPI::UnitData previous = {};
          previous.id = track.unitId;
          previous.replayID = track.replayId;
          previous.player = track.player;
          previous.type = track.type;
          previous.positionX = track.x;
          previous.positionY = track.y;
          previous.hitPoints = track.hitPoints;
          previous.shields = track.shields;
          previous.isCompleted = track.completed;
          appendSimpleUnitEvent(
            "unit_destroyed",
            "destroyed.",
            track.player == self ? "warning" : "info",
            frame,
            i,
            previous,
            self,
            track.player == self || isCombatUnitType(track.type));
        }
        track.exists = false;
        continue;
      }

      if (!track.initialized || !track.exists || track.replayId != unit.replayID)
      {
        if (g_gameSnapshotInitialized)
        {
          appendSimpleUnitEvent(
            "unit_created",
            "created.",
            "info",
            frame,
            i,
            unit,
            self,
            unit.player == self && !isWorkerType(unit.type));
        }
        track.lastEmitFrame = frame;
        track.lastUnderAttackFrame = frame;
        track.lastCombatFrame = frame;
        track.lastSeenFrame = visibleToSelf ? frame : -100000;
        updateTrackFromUnit(track, unit, visibleToSelf, attackingNow, frame);
        continue;
      }

      if (g_gameSnapshotInitialized && track.type != unit.type)
      {
        appendSimpleUnitEvent(
          "unit_morphed",
          "morphed.",
          "info",
          frame,
          i,
          unit,
          self,
          unit.player == self);
      }

      if (g_gameSnapshotInitialized && !track.completed && unit.isCompleted)
      {
        appendSimpleUnitEvent(
          "unit_completed",
          "completed.",
          "info",
          frame,
          i,
          unit,
          self,
          unit.player == self);
      }

      if (
        g_gameSnapshotInitialized &&
        unit.player != self &&
        visibleToSelf &&
        !track.visibleToSelf &&
        frame - track.lastSeenFrame >= 240)
      {
        appendSimpleUnitEvent(
          "enemy_spotted",
          "spotted.",
          "warning",
          frame,
          i,
          unit,
          self,
          isCombatUnitType(unit.type));
      }

      if (
        g_gameSnapshotInitialized &&
        unit.player == self &&
        (unit.hitPoints + unit.shields) < (track.hitPoints + track.shields) &&
        frame - track.lastUnderAttackFrame >= 120)
      {
        appendSimpleUnitEvent(
          "unit_under_attack",
          "under attack.",
          "warning",
          frame,
          i,
          unit,
          self,
          true);
        track.lastUnderAttackFrame = frame;
      }

      if (
        g_gameSnapshotInitialized &&
        unit.player == self &&
        isCombatUnitType(unit.type) &&
        attackingNow &&
        !track.attacking &&
        frame - track.lastCombatFrame >= 240)
      {
        appendSimpleUnitEvent(
          "combat_started",
          "entered combat.",
          "warning",
          frame,
          i,
          unit,
          self,
          true);
        track.lastCombatFrame = frame;
      }

      const int dx = unit.positionX - track.x;
      const int dy = unit.positionY - track.y;
      const int distanceSquared = dx * dx + dy * dy;
      if (
        g_gameSnapshotInitialized &&
        unit.player == self &&
        unit.isCompleted &&
        isCombatUnitType(unit.type) &&
        distanceSquared >= 4096 &&
        frame - track.lastEmitFrame >= 192)
      {
        appendUnitMovementEvent(frame, i, unit, track, self);
        track.lastEmitFrame = frame;
      }

      updateTrackFromUnit(track, unit, visibleToSelf, attackingNow, frame);
    }
    g_gameSnapshotInitialized = true;
  }
  __except (EXCEPTION_EXECUTE_HANDLER)
  {
    appendLogLine(g_moduleDir, "Shared-memory game-state polling hit an access violation and recovered.");
    Sleep(1000);
  }
}

DWORD WINAPI proxyWorkerThread(LPVOID)
{
  appendLogLine(g_moduleDir, "BWAPI proxy shared-memory game-state poller started.");

  HANDLE mapHandle = nullptr;
  BWAPI::GameData* gameData = nullptr;
  int mappedPid = -1;
  while (true)
  {
    if (!gameData)
    {
      const int serverPid = findServerProcessIdFromGameTable();
      if (serverPid > 0)
      {
        char sharedMemoryName[128] = {0};
        std::snprintf(
          sharedMemoryName,
          sizeof(sharedMemoryName),
          "Local\\bwapi_shared_memory_%d",
          serverPid);
        mapHandle = OpenFileMappingA(FILE_MAP_READ, FALSE, sharedMemoryName);
        if (mapHandle)
        {
          gameData = static_cast<BWAPI::GameData*>(
            MapViewOfFile(mapHandle, FILE_MAP_READ, 0, 0, 0));
          if (gameData)
          {
            mappedPid = serverPid;
            char line[256] = {0};
            std::snprintf(
              line,
              sizeof(line),
              "Mapped BWAPI shared memory read-only for pid=%d.",
              mappedPid);
            appendLogLine(g_moduleDir, line);
          }
          else
          {
            CloseHandle(mapHandle);
            mapHandle = nullptr;
          }
        }
      }
      Sleep(500);
      continue;
    }

    pollGameState(gameData);
    Sleep(250);

    const int serverPid = findServerProcessIdFromGameTable();
    if (serverPid > 0 && mappedPid > 0 && serverPid != mappedPid)
    {
      UnmapViewOfFile(gameData);
      CloseHandle(mapHandle);
      gameData = nullptr;
      mapHandle = nullptr;
      mappedPid = -1;
      appendLogLine(g_moduleDir, "BWAPI shared memory pid changed; remapping.");
    }
  }
  return 0;
}

void startProxyWorker()
{
  if (InterlockedCompareExchange(&g_workerStarted, 1, 0) != 0)
  {
    return;
  }
  HANDLE thread = CreateThread(nullptr, 0, proxyWorkerThread, nullptr, 0, nullptr);
  if (thread)
  {
    CloseHandle(thread);
  }
  else
  {
    appendLogLine(g_moduleDir, "Failed to start BWAPI proxy shared-memory game-state poller.");
  }
}

void loadRealBwapi(HMODULE module)
{
  char modulePath[MAX_PATH] = {0};
  char moduleDir[MAX_PATH] = {0};
  char processPath[MAX_PATH] = {0};
  char currentDir[MAX_PATH] = {0};
  GetModuleFileNameA(module, modulePath, sizeof(modulePath));
  GetModuleFileNameA(nullptr, processPath, sizeof(processPath));
  GetCurrentDirectoryA(sizeof(currentDir), currentDir);
  directoryOf(modulePath, moduleDir, sizeof(moduleDir));
  lstrcpynA(g_moduleDir, moduleDir, sizeof(g_moduleDir));
  lstrcpynA(g_modulePath, modulePath, sizeof(g_modulePath));
  lstrcpynA(g_processPath, processPath, sizeof(g_processPath));
  lstrcpynA(g_currentDir, currentDir, sizeof(g_currentDir));

  appendLogLine(moduleDir, "BWAPI proxy probe loaded as BWAPI.dll.");
  appendJsonEvent(
    moduleDir,
    "bwapi_proxy_loaded",
    "BWAPI proxy loaded inside StarCraft.",
    "info",
    modulePath,
    processPath,
    currentDir);

  char contextLine[2048] = {0};
  std::snprintf(
    contextLine,
    sizeof(contextLine),
    "Context module='%s' process='%s' cwd='%s'",
    modulePath,
    processPath,
    currentDir);
  appendLogLine(moduleDir, contextLine);

  char realPath[MAX_PATH] = {0};
  std::snprintf(realPath, sizeof(realPath), "%s\\BWAPI_real.dll", moduleDir);
  g_realBwapi = LoadLibraryA(realPath);
  if (g_realBwapi)
  {
    appendLogLine(moduleDir, "Loaded BWAPI_real.dll successfully.");
    appendJsonEvent(
      moduleDir,
      "bwapi_real_loaded",
      "Original BWAPI_real.dll loaded successfully.",
      "info",
      modulePath,
      processPath,
      currentDir);
    startProxyWorker();
    return;
  }

  char errorLine[512] = {0};
  std::snprintf(
    errorLine,
    sizeof(errorLine),
    "Failed to load BWAPI_real.dll. GetLastError=%lu",
    GetLastError());
  appendLogLine(moduleDir, errorLine);
  appendJsonEvent(
    moduleDir,
    "bwapi_real_load_failed",
    "Failed to load original BWAPI_real.dll.",
    "error",
    modulePath,
    processPath,
    currentDir);
  startProxyWorker();
}
}

BOOL APIENTRY DllMain(HMODULE module, DWORD reason, LPVOID reserved)
{
  (void)reserved;
  if (reason == DLL_PROCESS_ATTACH)
  {
    DisableThreadLibraryCalls(module);
    loadRealBwapi(module);
  }
  return TRUE;
}
