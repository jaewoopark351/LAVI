//20260703_kpopmodder: BWAPI AIModule exports for the LAV StarCraft 1.16 JSONL exporter.
#include <BWAPI.h>
#include <Windows.h>

#include "LAVEventExporter.h"

namespace
{
HMODULE g_moduleHandle = nullptr;
BWAPI::Game* g_game = nullptr;
}

extern "C" __declspec(dllexport) void gameInit(BWAPI::Game* game)
{
  BWAPI::BroodwarPtr = game;
  g_game = game;
  LAVEventExporter::setModuleHandle(g_moduleHandle);
  LAVEventExporter::gameInit(game);
}

BOOL APIENTRY DllMain(HANDLE hModule, DWORD reason, LPVOID)
{
  if (reason == DLL_PROCESS_ATTACH)
  {
    g_moduleHandle = reinterpret_cast<HMODULE>(hModule);
    LAVEventExporter::setModuleHandle(g_moduleHandle);
  }
  return TRUE;
}

extern "C" __declspec(dllexport) BWAPI::AIModule* newAIModule()
{
  LAVEventExporter::setModuleHandle(g_moduleHandle);
  if (g_game)
  {
    LAVEventExporter::gameInit(g_game);
  }
  return new LAVEventExporter::ProxyAIModule();
}
