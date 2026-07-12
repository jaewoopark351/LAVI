//20260710_kpopmodder: Added the LAV-owned Sharky entry point for direct ladder execution.
using System;
using System.Collections.Generic;
using Sharky;
using Sharky.DefaultBot;
using SC2APIProtocol;

Console.WriteLine("Starting SharkyLAVBot");

var gameConnection = new GameConnection();
var defaultBot = new DefaultSharkyBot(gameConnection);
var bot = new LAVObservingBot(defaultBot.CreateBot());

// Sharky parses the standard ladder arguments: --GamePort, --StartPort,
// --LadderServer and --OpponentId.
await gameConnection.RunLadder(bot, Race.Protoss, args);

//20260710_kpopmodder: Emit low-frequency SC2 observation summaries so LAV's
// existing stdout observer can turn Sharky game state into TTS commentary.
sealed class LAVObservingBot : ISharkyBot
{
    private readonly ISharkyBot inner;
    private uint lastReportFrame;

    public LAVObservingBot(ISharkyBot inner) => this.inner = inner;

    public void OnStart(ResponseGameInfo gameInfo, ResponseData data, ResponsePing pingResponse,
        ResponseObservation observation, uint playerId, string opponentId)
    {
        Console.WriteLine("LAV_OBSERVATION game_started bot=SharkyLAVBot race=Protoss");
        inner.OnStart(gameInfo, data, pingResponse, observation, playerId, opponentId);
    }

    public IEnumerable<SC2APIProtocol.Action> OnFrame(ResponseObservation observation)
    {
        var core = observation?.Observation;
        if (core == null)
        {
            return inner.OnFrame(observation);
        }
        var frame = core.GameLoop;
        if (frame >= lastReportFrame + 224)
        {
            lastReportFrame = frame;
            var units = core.RawData?.Units;
            var selfUnits = units == null ? 0 : CountAlliance(units, Alliance.Self);
            var minerals = core.PlayerCommon?.Minerals ?? 0;
            var supply = core.PlayerCommon?.FoodUsed ?? 0;
            Console.WriteLine($"LAV_OBSERVATION frame={frame} minerals={minerals} supply={supply} units={selfUnits}");
        }
        return inner.OnFrame(observation);
    }

    public void OnEnd(ResponseObservation observation, Result result)
    {
        Console.WriteLine($"LAV_OBSERVATION game_ended result={result}");
        inner.OnEnd(observation, result);
    }

    private static int CountAlliance(IEnumerable<Unit> units, Alliance alliance)
        => System.Linq.Enumerable.Count(units, unit => unit.Alliance == alliance);

}
