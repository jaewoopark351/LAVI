//20260703_kpopmodder: Implements the BWAPI proxy that exports StarCraft 1.16 gameplay events to JSONL.
#include "LAVEventExporter.h"

#include <algorithm>
#include <cctype>
#include <fstream>
#include <sstream>
#include <vector>

namespace
{
typedef void (__cdecl *GameInitFn)(BWAPI::Game*);
typedef BWAPI::AIModule* (__cdecl *NewAIModuleFn)();

HMODULE g_moduleHandle = nullptr;
HMODULE g_wrappedLibrary = nullptr;
GameInitFn g_wrappedGameInit = nullptr;
NewAIModuleFn g_wrappedNewAIModule = nullptr;
BWAPI::Game* g_game = nullptr;

std::string trim(const std::string& value)
{
  size_t begin = 0;
  while (begin < value.size() && std::isspace(static_cast<unsigned char>(value[begin])))
  {
    ++begin;
  }
  size_t end = value.size();
  while (end > begin && std::isspace(static_cast<unsigned char>(value[end - 1])))
  {
    --end;
  }
  return value.substr(begin, end - begin);
}

std::string lower(std::string value)
{
  std::transform(value.begin(), value.end(), value.begin(), [](unsigned char c) {
    return static_cast<char>(std::tolower(c));
  });
  return value;
}

std::string directoryOf(const std::string& path)
{
  size_t pos = path.find_last_of("\\/");
  if (pos == std::string::npos)
  {
    return "";
  }
  return path.substr(0, pos);
}

std::string basenameOf(const std::string& path)
{
  size_t pos = path.find_last_of("\\/");
  if (pos == std::string::npos)
  {
    return path;
  }
  return path.substr(pos + 1);
}

bool isAbsolutePath(const std::string& path)
{
  return path.size() >= 3
    && std::isalpha(static_cast<unsigned char>(path[0]))
    && path[1] == ':'
    && (path[2] == '\\' || path[2] == '/');
}

std::string joinPath(const std::string& left, const std::string& right)
{
  if (right.empty() || isAbsolutePath(right))
  {
    return right;
  }
  if (left.empty())
  {
    return right;
  }
  char tail = left[left.size() - 1];
  if (tail == '\\' || tail == '/')
  {
    return left + right;
  }
  return left + "\\" + right;
}

std::string dllDirectory()
{
  char path[MAX_PATH] = {0};
  if (g_moduleHandle && GetModuleFileNameA(g_moduleHandle, path, MAX_PATH))
  {
    return directoryOf(path);
  }
  return ".";
}

std::string jsonEscape(const std::string& value)
{
  std::ostringstream out;
  for (char c : value)
  {
    switch (c)
    {
    case '\\':
      out << "\\\\";
      break;
    case '"':
      out << "\\\"";
      break;
    case '\b':
      out << "\\b";
      break;
    case '\f':
      out << "\\f";
      break;
    case '\n':
      out << "\\n";
      break;
    case '\r':
      out << "\\r";
      break;
    case '\t':
      out << "\\t";
      break;
    default:
      if (static_cast<unsigned char>(c) < 0x20)
      {
        out << "\\u00";
        const char* hex = "0123456789abcdef";
        out << hex[(c >> 4) & 0x0F] << hex[c & 0x0F];
      }
      else
      {
        out << c;
      }
      break;
    }
  }
  return out.str();
}

std::string quoted(const std::string& value)
{
  return "\"" + jsonEscape(value) + "\"";
}

std::string frameJson()
{
  int frame = BWAPI::BroodwarPtr ? BWAPI::Broodwar->getFrameCount() : 0;
  std::ostringstream out;
  out << "\"frame\":" << frame << ",\"game_time_seconds\":" << (frame / 24);
  return out.str();
}

void ensureDirectory(const std::string& path)
{
  std::string dir = directoryOf(path);
  if (dir.empty())
  {
    return;
  }

  std::string current;
  for (size_t i = 0; i < dir.size(); ++i)
  {
    char c = dir[i];
    current.push_back(c);
    if (c == '\\' || c == '/')
    {
      if (current.size() > 3)
      {
        CreateDirectoryA(current.c_str(), nullptr);
      }
    }
  }
  CreateDirectoryA(dir.c_str(), nullptr);
}

std::map<std::string, std::string> readIni(const std::string& path)
{
  std::map<std::string, std::string> values;
  std::ifstream file(path.c_str());
  std::string line;
  while (std::getline(file, line))
  {
    line = trim(line);
    if (line.empty() || line[0] == '#' || line[0] == ';')
    {
      continue;
    }
    size_t pos = line.find('=');
    if (pos == std::string::npos)
    {
      continue;
    }
    values[lower(trim(line.substr(0, pos)))] = trim(line.substr(pos + 1));
  }
  return values;
}

int intValue(
  const std::map<std::string, std::string>& values,
  const std::string& key,
  int fallback)
{
  auto it = values.find(key);
  if (it == values.end())
  {
    return fallback;
  }
  try
  {
    return std::stoi(it->second);
  }
  catch (...)
  {
    return fallback;
  }
}

std::string stringValue(
  const std::map<std::string, std::string>& values,
  const std::string& key,
  const std::string& fallback)
{
  auto it = values.find(key);
  if (it == values.end() || it->second.empty())
  {
    return fallback;
  }
  return it->second;
}

bool isSelfUnit(BWAPI::Unit unit)
{
  return unit && BWAPI::BroodwarPtr && BWAPI::Broodwar->self()
    && unit->getPlayer() == BWAPI::Broodwar->self();
}

bool isEnemyUnit(BWAPI::Unit unit)
{
  return unit && BWAPI::BroodwarPtr && BWAPI::Broodwar->self() && unit->getPlayer()
    && BWAPI::Broodwar->self()->isEnemy(unit->getPlayer());
}

std::string raceName(BWAPI::Player player)
{
  if (!player)
  {
    return "";
  }
  return player->getRace().c_str();
}

std::string unitTypeName(BWAPI::Unit unit)
{
  if (!unit)
  {
    return "Unknown";
  }
  return unit->getType().c_str();
}

bool loadWrappedModule(const std::string& wrappedAIPath)
{
  if (g_wrappedLibrary && g_wrappedNewAIModule)
  {
    return true;
  }

  std::string resolvedPath = isAbsolutePath(wrappedAIPath)
    ? wrappedAIPath
    : joinPath(dllDirectory(), wrappedAIPath);
  if (lower(basenameOf(resolvedPath)) == "laveventexporter.dll")
  {
    return false;
  }

  g_wrappedLibrary = LoadLibraryA(resolvedPath.c_str());
  if (!g_wrappedLibrary)
  {
    return false;
  }

  g_wrappedGameInit = reinterpret_cast<GameInitFn>(
    GetProcAddress(g_wrappedLibrary, "gameInit"));
  g_wrappedNewAIModule = reinterpret_cast<NewAIModuleFn>(
    GetProcAddress(g_wrappedLibrary, "newAIModule"));
  if (!g_wrappedNewAIModule)
  {
    return false;
  }
  if (g_game && g_wrappedGameInit)
  {
    g_wrappedGameInit(g_game);
  }
  return true;
}
}

namespace LAVEventExporter
{
void setModuleHandle(HMODULE moduleHandle)
{
  g_moduleHandle = moduleHandle;
}

void gameInit(BWAPI::Game* game)
{
  g_game = game;
  if (g_wrappedGameInit)
  {
    g_wrappedGameInit(game);
  }
}

EventWriter::EventWriter()
  : snapshotIntervalFrames_(144),
    combatCooldownFrames_(96),
    supplyBlockCooldownFrames_(240),
    lastSnapshotFrame_(-999999),
    lastCombatFrame_(-999999),
    lastSupplyBlockFrame_(-999999),
    lastMinerals_(-1),
    lastGas_(-1),
    lastSupplyUsed_(-1),
    lastSupplyTotal_(-1),
    configured_(false)
{
}

void EventWriter::configureFromDisk()
{
  if (configured_)
  {
    return;
  }
  configured_ = true;
  dllDir_ = dllDirectory();
  std::map<std::string, std::string> values = readIni(
    joinPath(dllDir_, "LAVEventExporter.ini"));

  wrappedAIPath_ = stringValue(values, "wrapped_ai", "Stardust.dll");
  eventsPath_ = stringValue(values, "events_path", "LAVEventExporter_events.jsonl");
  eventsPath_ = joinPath(dllDir_, eventsPath_);
  snapshotIntervalFrames_ = std::max(
    24,
    intValue(values, "snapshot_interval_frames", snapshotIntervalFrames_));
  combatCooldownFrames_ = std::max(
    24,
    intValue(values, "combat_cooldown_frames", combatCooldownFrames_));
  supplyBlockCooldownFrames_ = std::max(
    24,
    intValue(values, "supply_block_cooldown_frames", supplyBlockCooldownFrames_));
}

const std::string& EventWriter::wrappedAIPath() const
{
  return wrappedAIPath_;
}

void EventWriter::onStart()
{
  configureFromDisk();
  std::ostringstream extra;
  extra << "{\"map\":" << quoted(BWAPI::BroodwarPtr ? BWAPI::Broodwar->mapName() : "")
        << ",\"self_race\":" << quoted(raceName(BWAPI::BroodwarPtr ? BWAPI::Broodwar->self() : nullptr))
        << ",\"enemy_race\":" << quoted(raceName(BWAPI::BroodwarPtr ? BWAPI::Broodwar->enemy() : nullptr))
        << ",\"wrapped_ai\":" << quoted(basenameOf(wrappedAIPath_)) << "}";
  writeEvent("game_started", "StarCraft 1.16 BWAPI game started.", extra.str());
}

void EventWriter::onEnd(bool isWinner)
{
  std::ostringstream extra;
  extra << "{\"is_winner\":" << (isWinner ? "true" : "false") << "}";
  writeEvent(
    "game_ended",
    isWinner ? "Game ended with a win." : "Game ended.",
    extra.str());
}

void EventWriter::onFrame()
{
  if (!BWAPI::BroodwarPtr || BWAPI::Broodwar->isReplay() || BWAPI::Broodwar->isPaused())
  {
    return;
  }

  int frame = BWAPI::Broodwar->getFrameCount();
  if (frame - lastSnapshotFrame_ >= snapshotIntervalFrames_)
  {
    writeSnapshot();
    lastSnapshotFrame_ = frame;
  }
  maybeWriteCombat();
  maybeWriteSupplyBlock();
}

void EventWriter::onUnitDiscover(BWAPI::Unit unit)
{
  onUnitShow(unit);
}

void EventWriter::onUnitShow(BWAPI::Unit unit)
{
  if (!unit || !isEnemyUnit(unit))
  {
    return;
  }
  if (discoveredEnemies_.insert(unit->getID()).second)
  {
    writeUnitEvent(
      "enemy_spotted",
      "Enemy " + unitTypeName(unit) + " spotted.",
      unit);
  }
}

void EventWriter::onUnitCreate(BWAPI::Unit unit)
{
  if (!unit || !isSelfUnit(unit))
  {
    return;
  }

  std::string typeName = unitTypeName(unit);
  if (unit->getType().isBuilding())
  {
    writeUnitEvent("building_started", typeName + " started.", unit);
  }
  else if (!unit->getType().isWorker())
  {
    writeUnitEvent("unit_created", typeName + " created.", unit);
  }
}

void EventWriter::onUnitDestroy(BWAPI::Unit unit)
{
  if (!unit)
  {
    return;
  }
  if (isSelfUnit(unit))
  {
    writeUnitEvent("unit_destroyed", "Our " + unitTypeName(unit) + " was destroyed.", unit);
  }
  else if (isEnemyUnit(unit))
  {
    writeUnitEvent("unit_destroyed", "Enemy " + unitTypeName(unit) + " was destroyed.", unit);
  }
}

void EventWriter::onUnitMorph(BWAPI::Unit unit)
{
  if (!unit)
  {
    return;
  }
  if (isSelfUnit(unit) || isEnemyUnit(unit))
  {
    writeUnitEvent("unit_morphed", unitTypeName(unit) + " morphed.", unit);
  }
}

void EventWriter::onUnitComplete(BWAPI::Unit unit)
{
  if (!unit || !isSelfUnit(unit))
  {
    return;
  }
  std::string typeName = unitTypeName(unit);
  if (unit->getType().isBuilding())
  {
    writeUnitEvent("building_completed", typeName + " completed.", unit);
  }
  else if (!unit->getType().isWorker())
  {
    writeUnitEvent("unit_completed", typeName + " completed.", unit);
  }
}

void EventWriter::writeSnapshot()
{
  if (!BWAPI::BroodwarPtr || !BWAPI::Broodwar->self())
  {
    return;
  }

  BWAPI::Player self = BWAPI::Broodwar->self();
  int minerals = self->minerals();
  int gas = self->gas();
  int supplyUsed = self->supplyUsed() / 2;
  int supplyTotal = self->supplyTotal() / 2;

  std::ostringstream summary;
  summary << "Economy snapshot: " << minerals << " minerals, " << gas
          << " gas, supply " << supplyUsed << "/" << supplyTotal << ".";

  std::ostringstream extra;
  extra << "{\"resources\":" << resourcesJson()
        << ",\"units\":" << selfUnitCountsJson() << "}";
  writeEvent("state_snapshot", summary.str(), extra.str());

  lastMinerals_ = minerals;
  lastGas_ = gas;
  lastSupplyUsed_ = supplyUsed;
  lastSupplyTotal_ = supplyTotal;
}

void EventWriter::maybeWriteCombat()
{
  if (!BWAPI::BroodwarPtr || !BWAPI::Broodwar->self())
  {
    return;
  }

  int frame = BWAPI::Broodwar->getFrameCount();
  if (frame - lastCombatFrame_ < combatCooldownFrames_)
  {
    return;
  }

  for (auto unit : BWAPI::Broodwar->self()->getUnits())
  {
    if (!unit || !unit->exists() || !unit->isCompleted())
    {
      continue;
    }
    if (!(unit->isAttacking() || unit->isStartingAttack() || unit->isUnderAttack()))
    {
      continue;
    }

    BWAPI::Unit nearestEnemy = nullptr;
    int nearestDistance = 999999;
    for (auto candidate : BWAPI::Broodwar->getAllUnits())
    {
      if (!candidate || !isEnemyUnit(candidate))
      {
        continue;
      }
      int distance = unit->getDistance(candidate);
      if (distance < nearestDistance)
      {
        nearestDistance = distance;
        nearestEnemy = candidate;
      }
    }

    std::ostringstream extra;
    extra << "{\"friendly_unit\":" << unitJson(unit);
    if (nearestEnemy)
    {
      extra << ",\"enemy_unit\":" << unitJson(nearestEnemy);
    }
    extra << ",\"resources\":" << resourcesJson() << "}";

    std::string summary = "Combat started around our " + unitTypeName(unit) + ".";
    if (nearestEnemy)
    {
      summary = "Combat started: " + unitTypeName(unit)
        + " vs enemy " + unitTypeName(nearestEnemy) + ".";
    }
    writeEvent("combat_started", summary, extra.str());
    lastCombatFrame_ = frame;
    return;
  }
}

void EventWriter::maybeWriteSupplyBlock()
{
  if (!BWAPI::BroodwarPtr || !BWAPI::Broodwar->self())
  {
    return;
  }
  int frame = BWAPI::Broodwar->getFrameCount();
  if (frame - lastSupplyBlockFrame_ < supplyBlockCooldownFrames_)
  {
    return;
  }
  BWAPI::Player self = BWAPI::Broodwar->self();
  int supplyUsed = self->supplyUsed() / 2;
  int supplyTotal = self->supplyTotal() / 2;
  if (supplyTotal > 0 && supplyUsed >= supplyTotal)
  {
    std::ostringstream summary;
    summary << "Supply blocked at " << supplyUsed << "/" << supplyTotal << ".";
    std::ostringstream extra;
    extra << "{\"resources\":" << resourcesJson() << "}";
    writeEvent("supply_blocked", summary.str(), extra.str());
    lastSupplyBlockFrame_ = frame;
  }
}

void EventWriter::writeUnitEvent(
  const std::string& eventType,
  const std::string& summary,
  BWAPI::Unit unit)
{
  std::ostringstream extra;
  extra << "{\"unit\":" << unitJson(unit) << ",\"resources\":" << resourcesJson() << "}";
  writeEvent(eventType, summary, extra.str());
}

void EventWriter::writeEvent(
  const std::string& eventType,
  const std::string& summary,
  const std::string& extraJson)
{
  configureFromDisk();
  ensureDirectory(eventsPath_);

  std::ostringstream line;
  line << "{\"schema\":\"lav_starcraft116_bwapi_event_v1\","
       << "\"source\":\"LAVEventExporter\","
       << "\"event_type\":" << quoted(eventType) << ","
       << "\"summary\":" << quoted(summary) << ","
       << frameJson();
  if (BWAPI::BroodwarPtr)
  {
    line << ",\"map\":" << quoted(BWAPI::Broodwar->mapName());
  }
  if (!extraJson.empty() && extraJson != "{}")
  {
    std::string extra = extraJson;
    if (!extra.empty() && extra[0] == '{')
    {
      extra = extra.substr(1);
    }
    if (!extra.empty() && extra[extra.size() - 1] == '}')
    {
      extra.resize(extra.size() - 1);
    }
    if (!extra.empty())
    {
      line << "," << extra;
    }
  }
  line << "}\n";

  std::ofstream file(eventsPath_.c_str(), std::ios::app | std::ios::binary);
  file << line.str();
}

std::string EventWriter::resourcesJson() const
{
  if (!BWAPI::BroodwarPtr || !BWAPI::Broodwar->self())
  {
    return "{}";
  }
  BWAPI::Player self = BWAPI::Broodwar->self();
  std::ostringstream out;
  out << "{\"minerals\":" << self->minerals()
      << ",\"gas\":" << self->gas()
      << ",\"supply\":\"" << (self->supplyUsed() / 2)
      << "/" << (self->supplyTotal() / 2) << "\""
      << ",\"supply_used\":" << (self->supplyUsed() / 2)
      << ",\"supply_total\":" << (self->supplyTotal() / 2)
      << "}";
  return out.str();
}

std::string EventWriter::unitJson(BWAPI::Unit unit) const
{
  if (!unit)
  {
    return "{}";
  }
  BWAPI::Player player = unit->getPlayer();
  BWAPI::Position pos = unit->getPosition();
  std::string owner = "neutral";
  if (isSelfUnit(unit))
  {
    owner = "self";
  }
  else if (isEnemyUnit(unit))
  {
    owner = "enemy";
  }

  std::ostringstream out;
  out << "{\"id\":" << unit->getID()
      << ",\"type\":" << quoted(unitTypeName(unit))
      << ",\"owner\":" << quoted(owner)
      << ",\"player\":" << quoted(player ? player->getName() : "")
      << ",\"race\":" << quoted(raceName(player))
      << ",\"completed\":" << (unit->isCompleted() ? "true" : "false")
      << ",\"hp\":" << unit->getHitPoints()
      << ",\"shields\":" << unit->getShields()
      << ",\"position\":{\"x\":" << pos.x << ",\"y\":" << pos.y << "}}";
  return out.str();
}

std::string EventWriter::selfUnitCountsJson() const
{
  if (!BWAPI::BroodwarPtr || !BWAPI::Broodwar->self())
  {
    return "{}";
  }
  std::map<std::string, int> counts;
  for (auto unit : BWAPI::Broodwar->self()->getUnits())
  {
    if (!unit || !unit->exists())
    {
      continue;
    }
    counts[unitTypeName(unit)] += 1;
  }

  std::ostringstream out;
  out << "{";
  bool first = true;
  for (auto it = counts.begin(); it != counts.end(); ++it)
  {
    if (!first)
    {
      out << ",";
    }
    first = false;
    out << quoted(it->first) << ":" << it->second;
  }
  out << "}";
  return out.str();
}

ProxyAIModule::ProxyAIModule()
{
  writer_.configureFromDisk();
  if (loadWrappedModule(writer_.wrappedAIPath()) && g_wrappedNewAIModule)
  {
    wrappedModule_.reset(g_wrappedNewAIModule());
  }
}

ProxyAIModule::~ProxyAIModule()
{
}

void ProxyAIModule::onStart()
{
  writer_.onStart();
  if (wrappedModule_)
  {
    wrappedModule_->onStart();
  }
}

void ProxyAIModule::onEnd(bool isWinner)
{
  writer_.onEnd(isWinner);
  if (wrappedModule_)
  {
    wrappedModule_->onEnd(isWinner);
  }
}

void ProxyAIModule::onFrame()
{
  writer_.onFrame();
  if (wrappedModule_)
  {
    wrappedModule_->onFrame();
  }
}

void ProxyAIModule::onSendText(std::string text)
{
  if (wrappedModule_)
  {
    wrappedModule_->onSendText(text);
  }
}

void ProxyAIModule::onReceiveText(BWAPI::Player player, std::string text)
{
  if (wrappedModule_)
  {
    wrappedModule_->onReceiveText(player, text);
  }
}

void ProxyAIModule::onPlayerLeft(BWAPI::Player player)
{
  if (wrappedModule_)
  {
    wrappedModule_->onPlayerLeft(player);
  }
}

void ProxyAIModule::onNukeDetect(BWAPI::Position target)
{
  if (wrappedModule_)
  {
    wrappedModule_->onNukeDetect(target);
  }
}

void ProxyAIModule::onUnitDiscover(BWAPI::Unit unit)
{
  writer_.onUnitDiscover(unit);
  if (wrappedModule_)
  {
    wrappedModule_->onUnitDiscover(unit);
  }
}

void ProxyAIModule::onUnitEvade(BWAPI::Unit unit)
{
  if (wrappedModule_)
  {
    wrappedModule_->onUnitEvade(unit);
  }
}

void ProxyAIModule::onUnitShow(BWAPI::Unit unit)
{
  writer_.onUnitShow(unit);
  if (wrappedModule_)
  {
    wrappedModule_->onUnitShow(unit);
  }
}

void ProxyAIModule::onUnitHide(BWAPI::Unit unit)
{
  if (wrappedModule_)
  {
    wrappedModule_->onUnitHide(unit);
  }
}

void ProxyAIModule::onUnitCreate(BWAPI::Unit unit)
{
  writer_.onUnitCreate(unit);
  if (wrappedModule_)
  {
    wrappedModule_->onUnitCreate(unit);
  }
}

void ProxyAIModule::onUnitDestroy(BWAPI::Unit unit)
{
  writer_.onUnitDestroy(unit);
  if (wrappedModule_)
  {
    wrappedModule_->onUnitDestroy(unit);
  }
}

void ProxyAIModule::onUnitMorph(BWAPI::Unit unit)
{
  writer_.onUnitMorph(unit);
  if (wrappedModule_)
  {
    wrappedModule_->onUnitMorph(unit);
  }
}

void ProxyAIModule::onUnitRenegade(BWAPI::Unit unit)
{
  if (wrappedModule_)
  {
    wrappedModule_->onUnitRenegade(unit);
  }
}

void ProxyAIModule::onSaveGame(std::string gameName)
{
  if (wrappedModule_)
  {
    wrappedModule_->onSaveGame(gameName);
  }
}

void ProxyAIModule::onUnitComplete(BWAPI::Unit unit)
{
  writer_.onUnitComplete(unit);
  if (wrappedModule_)
  {
    wrappedModule_->onUnitComplete(unit);
  }
}
}
