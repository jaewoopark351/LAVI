//20260703_kpopmodder: Declares the BWAPI proxy that forwards Stardust callbacks and logs JSONL events.
#pragma once

#include <BWAPI.h>
#include <Windows.h>

#include <map>
#include <memory>
#include <set>
#include <string>

namespace LAVEventExporter
{
void setModuleHandle(HMODULE moduleHandle);
void gameInit(BWAPI::Game* game);

class EventWriter
{
public:
  EventWriter();

  void configureFromDisk();
  const std::string& wrappedAIPath() const;

  void onStart();
  void onEnd(bool isWinner);
  void onFrame();
  void onUnitDiscover(BWAPI::Unit unit);
  void onUnitShow(BWAPI::Unit unit);
  void onUnitCreate(BWAPI::Unit unit);
  void onUnitDestroy(BWAPI::Unit unit);
  void onUnitMorph(BWAPI::Unit unit);
  void onUnitComplete(BWAPI::Unit unit);

private:
  std::string dllDir_;
  std::string eventsPath_;
  std::string wrappedAIPath_;
  int snapshotIntervalFrames_;
  int combatCooldownFrames_;
  int supplyBlockCooldownFrames_;
  int lastSnapshotFrame_;
  int lastCombatFrame_;
  int lastSupplyBlockFrame_;
  int lastMinerals_;
  int lastGas_;
  int lastSupplyUsed_;
  int lastSupplyTotal_;
  bool configured_;
  std::set<int> discoveredEnemies_;

  void writeEvent(
    const std::string& eventType,
    const std::string& summary,
    const std::string& extraJson = "{}");
  void writeUnitEvent(
    const std::string& eventType,
    const std::string& summary,
    BWAPI::Unit unit);
  void writeSnapshot();
  void maybeWriteCombat();
  void maybeWriteSupplyBlock();

  std::string resourcesJson() const;
  std::string unitJson(BWAPI::Unit unit) const;
  std::string selfUnitCountsJson() const;
};

class ProxyAIModule : public BWAPI::AIModule
{
public:
  ProxyAIModule();
  virtual ~ProxyAIModule();

  virtual void onStart();
  virtual void onEnd(bool isWinner);
  virtual void onFrame();
  virtual void onSendText(std::string text);
  virtual void onReceiveText(BWAPI::Player player, std::string text);
  virtual void onPlayerLeft(BWAPI::Player player);
  virtual void onNukeDetect(BWAPI::Position target);
  virtual void onUnitDiscover(BWAPI::Unit unit);
  virtual void onUnitEvade(BWAPI::Unit unit);
  virtual void onUnitShow(BWAPI::Unit unit);
  virtual void onUnitHide(BWAPI::Unit unit);
  virtual void onUnitCreate(BWAPI::Unit unit);
  virtual void onUnitDestroy(BWAPI::Unit unit);
  virtual void onUnitMorph(BWAPI::Unit unit);
  virtual void onUnitRenegade(BWAPI::Unit unit);
  virtual void onSaveGame(std::string gameName);
  virtual void onUnitComplete(BWAPI::Unit unit);

private:
  std::unique_ptr<BWAPI::AIModule> wrappedModule_;
  EventWriter writer_;
};
}
