//20260705_kpopmodder: Standalone BWAPI 4.2.0 observer client for Monster.exe JSONL game events.
#include <BWAPI.h>
#include <BWAPI/Client.h>
#include <Windows.h>

#include <algorithm>
#include <chrono>
#include <cctype>
#include <fstream>
#include <iostream>
#include <map>
#include <set>
#include <sstream>
#include <string>
#include <thread>
#include <vector>

namespace
{
struct ObserverConfig
{
  std::string eventsPath;
  int snapshotIntervalFrames = 144;
  int combatCooldownFrames = 96;
  int supplyBlockCooldownFrames = 240;
  bool completeMapInformation = false;
};

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

std::string directoryOf(const std::string& path)
{
  size_t pos = path.find_last_of("\\/");
  if (pos == std::string::npos)
  {
    return "";
  }
  return path.substr(0, pos);
}

std::string exeDirectory()
{
  char path[MAX_PATH] = {0};
  if (GetModuleFileNameA(nullptr, path, MAX_PATH))
  {
    return directoryOf(path);
  }
  return ".";
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

std::string envValue(const char* name)
{
  char buffer[32767] = {0};
  DWORD size = GetEnvironmentVariableA(name, buffer, sizeof(buffer));
  if (size == 0 || size >= sizeof(buffer))
  {
    return "";
  }
  return trim(buffer);
}

int intArg(const std::string& value, int fallback)
{
  try
  {
    return std::stoi(value);
  }
  catch (...)
  {
    return fallback;
  }
}

bool boolArg(const std::string& value)
{
  std::string lower = value;
  std::transform(lower.begin(), lower.end(), lower.begin(), [](unsigned char c) {
    return static_cast<char>(std::tolower(c));
  });
  return lower == "1" || lower == "true" || lower == "yes" || lower == "on";
}

ObserverConfig parseArgs(int argc, const char* argv[])
{
  ObserverConfig config;
  config.eventsPath = envValue("LAV_STARCRAFT116_EVENTS_PATH");
  for (int index = 1; index < argc; ++index)
  {
    std::string arg = argv[index] ? argv[index] : "";
    std::string next = index + 1 < argc && argv[index + 1] ? argv[index + 1] : "";
    if (arg == "--events-path" && !next.empty())
    {
      config.eventsPath = next;
      ++index;
    }
    else if (arg == "--snapshot-frames" && !next.empty())
    {
      config.snapshotIntervalFrames = std::max(24, intArg(next, config.snapshotIntervalFrames));
      ++index;
    }
    else if (arg == "--combat-cooldown-frames" && !next.empty())
    {
      config.combatCooldownFrames = std::max(24, intArg(next, config.combatCooldownFrames));
      ++index;
    }
    else if (arg == "--supply-block-cooldown-frames" && !next.empty())
    {
      config.supplyBlockCooldownFrames = std::max(24, intArg(next, config.supplyBlockCooldownFrames));
      ++index;
    }
    else if (arg == "--complete-map-info" && !next.empty())
    {
      config.completeMapInformation = boolArg(next);
      ++index;
    }
  }
  if (config.eventsPath.empty())
  {
    config.eventsPath = joinPath(exeDirectory(), "starcraft116_game_events.jsonl");
  }
  return config;
}

std::string jsonEscape(const std::string& value)
{
  std::ostringstream out;
  for (char c : value)
  {
    unsigned char uc = static_cast<unsigned char>(c);
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
      if (uc < 0x20)
      {
        const char* hex = "0123456789abcdef";
        out << "\\u00" << hex[(uc >> 4) & 0x0F] << hex[uc & 0x0F];
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
    if ((c == '\\' || c == '/') && current.size() > 3)
    {
      CreateDirectoryA(current.c_str(), nullptr);
    }
  }
  CreateDirectoryA(dir.c_str(), nullptr);
}

std::string raceName(BWAPI::Player player)
{
  return player ? player->getRace().c_str() : "";
}

std::string unitTypeName(BWAPI::Unit unit)
{
  return unit ? unit->getType().c_str() : "Unknown";
}

bool isSelfUnit(BWAPI::Unit unit)
{
  return unit && BWAPI::Broodwar->self() && unit->getPlayer() == BWAPI::Broodwar->self();
}

bool isEnemyUnit(BWAPI::Unit unit)
{
  return unit && BWAPI::Broodwar->self() && unit->getPlayer()
    && BWAPI::Broodwar->self()->isEnemy(unit->getPlayer());
}

class ObserverEventWriter
{
public:
  explicit ObserverEventWriter(const ObserverConfig& config)
    : config_(config),
      lastSnapshotFrame_(-999999),
      lastCombatFrame_(-999999),
      lastSupplyBlockFrame_(-999999)
  {
  }

  void writeGameStarted()
  {
    std::ostringstream extra;
    extra << "{\"map\":" << quoted(BWAPI::Broodwar->mapName())
          << ",\"self_race\":" << quoted(raceName(BWAPI::Broodwar->self()))
          << ",\"enemy_race\":" << quoted(raceName(BWAPI::Broodwar->enemy()))
          << ",\"observer\":\"LAVBWAPIObserverClient\"}";
    writeEvent("game_started", "StarCraft 1.16 BWAPI observer connected.", extra.str());
  }

  void writeGameEnded(bool isWinner)
  {
    std::ostringstream extra;
    extra << "{\"is_winner\":" << (isWinner ? "true" : "false") << "}";
    writeEvent("game_ended", isWinner ? "Game ended with a win." : "Game ended.", extra.str());
  }

  void onFrame()
  {
    if (BWAPI::Broodwar->isReplay() || BWAPI::Broodwar->isPaused() || !BWAPI::Broodwar->self())
    {
      return;
    }
    int frame = BWAPI::Broodwar->getFrameCount();
    if (frame - lastSnapshotFrame_ >= config_.snapshotIntervalFrames)
    {
      writeSnapshot();
      lastSnapshotFrame_ = frame;
    }
    maybeWriteCombat();
    maybeWriteSupplyBlock();
  }

  void onUnitShow(BWAPI::Unit unit)
  {
    if (!unit || !isEnemyUnit(unit))
    {
      return;
    }
    if (discoveredEnemies_.insert(unit->getID()).second)
    {
      writeUnitEvent("enemy_spotted", "Enemy " + unitTypeName(unit) + " spotted.", unit);
    }
  }

  void onUnitCreate(BWAPI::Unit unit)
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

  void onUnitComplete(BWAPI::Unit unit)
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

  void onUnitDestroy(BWAPI::Unit unit)
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

  void onUnitMorph(BWAPI::Unit unit)
  {
    if (unit && (isSelfUnit(unit) || isEnemyUnit(unit)))
    {
      writeUnitEvent("unit_morphed", unitTypeName(unit) + " morphed.", unit);
    }
  }

private:
  ObserverConfig config_;
  int lastSnapshotFrame_;
  int lastCombatFrame_;
  int lastSupplyBlockFrame_;
  std::set<int> discoveredEnemies_;

  void writeSnapshot()
  {
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
  }

  void maybeWriteCombat()
  {
    int frame = BWAPI::Broodwar->getFrameCount();
    if (frame - lastCombatFrame_ < config_.combatCooldownFrames || !BWAPI::Broodwar->self())
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

  void maybeWriteSupplyBlock()
  {
    int frame = BWAPI::Broodwar->getFrameCount();
    if (frame - lastSupplyBlockFrame_ < config_.supplyBlockCooldownFrames || !BWAPI::Broodwar->self())
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

  void writeUnitEvent(const std::string& eventType, const std::string& summary, BWAPI::Unit unit)
  {
    std::ostringstream extra;
    extra << "{\"unit\":" << unitJson(unit) << ",\"resources\":" << resourcesJson() << "}";
    writeEvent(eventType, summary, extra.str());
  }

  void writeEvent(const std::string& eventType, const std::string& summary, const std::string& extraJson)
  {
    ensureDirectory(config_.eventsPath);
    int frame = BWAPI::Broodwar->getFrameCount();
    std::ostringstream line;
    line << "{\"schema\":\"lav_starcraft116_bwapi_event_v1\","
         << "\"source\":\"LAVBWAPIObserverClient\","
         << "\"event_type\":" << quoted(eventType) << ","
         << "\"summary\":" << quoted(summary) << ","
         << "\"frame\":" << frame << ",\"game_time_seconds\":" << (frame / 24)
         << ",\"map\":" << quoted(BWAPI::Broodwar->mapName());
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

    std::ofstream file(config_.eventsPath.c_str(), std::ios::app | std::ios::binary);
    file << line.str();
  }

  std::string resourcesJson() const
  {
    if (!BWAPI::Broodwar->self())
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

  std::string unitJson(BWAPI::Unit unit) const
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

  std::string selfUnitCountsJson() const
  {
    if (!BWAPI::Broodwar->self())
    {
      return "{}";
    }
    std::map<std::string, int> counts;
    for (auto unit : BWAPI::Broodwar->self()->getUnits())
    {
      if (unit && unit->exists())
      {
        counts[unitTypeName(unit)] += 1;
      }
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
};

void reconnect()
{
  while (!BWAPI::BWAPIClient.connect())
  {
    std::cout << "Waiting for BWAPI client connection..." << std::endl;
    std::this_thread::sleep_for(std::chrono::milliseconds(1000));
  }
}
}

int main(int argc, const char* argv[])
{
  ObserverConfig config = parseArgs(argc, argv);
  std::cout << "LAV BWAPI observer starting." << std::endl;
  std::cout << "events_path=" << config.eventsPath << std::endl;

  reconnect();
  ObserverEventWriter writer(config);
  while (true)
  {
    std::cout << "Waiting to enter match..." << std::endl;
    while (!BWAPI::Broodwar->isInGame())
    {
      BWAPI::BWAPIClient.update();
      if (!BWAPI::BWAPIClient.isConnected())
      {
        std::cout << "Reconnecting..." << std::endl;
        reconnect();
      }
      std::this_thread::sleep_for(std::chrono::milliseconds(100));
    }

    std::cout << "Observer joined match." << std::endl;
    if (config.completeMapInformation)
    {
      BWAPI::Broodwar->enableFlag(BWAPI::Flag::CompleteMapInformation);
    }
    writer.writeGameStarted();

    while (BWAPI::Broodwar->isInGame())
    {
      for (auto& event : BWAPI::Broodwar->getEvents())
      {
        switch (event.getType())
        {
        case BWAPI::EventType::MatchEnd:
          writer.writeGameEnded(event.isWinner());
          break;
        case BWAPI::EventType::UnitDiscover:
        case BWAPI::EventType::UnitShow:
          writer.onUnitShow(event.getUnit());
          break;
        case BWAPI::EventType::UnitCreate:
          writer.onUnitCreate(event.getUnit());
          break;
        case BWAPI::EventType::UnitComplete:
          writer.onUnitComplete(event.getUnit());
          break;
        case BWAPI::EventType::UnitDestroy:
          writer.onUnitDestroy(event.getUnit());
          break;
        case BWAPI::EventType::UnitMorph:
          writer.onUnitMorph(event.getUnit());
          break;
        default:
          break;
        }
      }

      writer.onFrame();
      BWAPI::BWAPIClient.update();
      if (!BWAPI::BWAPIClient.isConnected())
      {
        std::cout << "Reconnecting..." << std::endl;
        reconnect();
      }
    }
    std::cout << "Game ended." << std::endl;
  }
}
