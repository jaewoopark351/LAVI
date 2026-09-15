#20260914_kpopmodder: Compile and execute pure contracts and explicitly isolated API simulation with Java 17 target.
from pathlib import Path
import json
import shutil
from plugins.Minecraft.fabric.chatclef.result.find import FindTerminalPayload
import subprocess
import pytest
from .java_api_stubs import STUBS
from .java_engine_stubs import ENGINE_STUBS

ROOT=Path(__file__).resolve().parents[3]
JAVA=ROOT/"plugins/Minecraft/runtime/chatclef_fabric_1.20.1/src/main/java"
HERE=Path(__file__).resolve().parent


def run_java(tmp_path, sources, main):
    javac, java=shutil.which("javac"),shutil.which("java")
    if not javac or not java: pytest.skip("JDK 17+ is needed for the offline Java contract harness")
    out=tmp_path/"classes";out.mkdir()
    response=tmp_path/"sources.txt"
    response.write_text("\n".join('"'+str(p).replace("\\", "/")+'"' for p in sources),encoding="utf-8")
    built=subprocess.run([javac,"--release","17","-encoding","UTF-8","-d",str(out),"@"+str(response)],capture_output=True,text=True,timeout=45)
    assert built.returncode==0, built.stdout+built.stderr
    ran=subprocess.run([java,"-cp",str(out),main],capture_output=True,text=True,timeout=45)
    assert ran.returncode==0,ran.stdout+ran.stderr
    print(ran.stdout.strip())
    return ran.stdout


def test_pure_java_find_contracts(tmp_path):
    sources=[JAVA/"lavi/minecraft/task/find"/name for name in ("FindRequest.java","FindNameIndex.java","FindOutcome.java")]
    sources.append(HERE/"java/FindContractHarness.java")
    #20260915_kpopmodder: Retain actual pure-contract output in verification runs using pytest -s.
    output=run_java(tmp_path,sources,"lavi.minecraft.task.find.FindContractHarness")
    assert "PURE_JAVA_CONTRACT_CHECKS=" in output
    print(output.strip())


def test_real_find_sources_against_simulated_runtime_apis(tmp_path):
    sources=[]
    stubs = {**STUBS, **ENGINE_STUBS}
    stubs.pop("adris/altoclef/tasksystem/Task.java")
    for name, text in stubs.items():
        p=tmp_path/"stubs"/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text,encoding="utf-8");sources.append(p)
    sources.extend((JAVA/"lavi/minecraft/task/find").glob("*.java"))
    for relative in ("tasksystem/Task.java", "tasksystem/TaskRunner.java", "tasksystem/TaskChain.java",
                     "tasksystem/ITaskCanForce.java", "chains/SingleTaskChain.java",
                     "tasks/AbstractDoToClosestObjectTask.java", "tasks/entity/DoToClosestEntityTask.java",
                     "tasks/DoToClosestBlockTask.java"):
        sources.append(JAVA/"adris/altoclef"/relative)
    result=JAVA/"lavi/minecraft/fabric/chatclef/bridge/command/result"
    sources.extend(result.glob("*.java"))
    sources.extend([result/"find/FabricChatClefFindResultProjector.java", JAVA/"lavi/minecraft/fabric/chatclef/bridge/command/FabricChatClefCommandResult.java",
                    JAVA/"adris/altoclef/commandsystem/Command.java",JAVA/"adris/altoclef/util/baritone/GoalFollowEntity.java",HERE/"java/FindRuntimeHarness.java", HERE/"java/FindExplorationHarness.java"])
    diagnostics = JAVA/"lavi/minecraft/diagnostics"
    for name in ("DiagnosticEventEmitter.java", "DiagnosticTraceState.java", "DiagnosticTaskRegistry.java", "DiagnosticEventIdentity.java"):
        sources.append(diagnostics/name)
    for name in ("DiagnosticValueFormatter.java", "DiagnosticFieldValueEncoder.java", "DiagnosticBoundedEventFormatter.java", "DiagnosticBoundedEventText.java"):
        sources.append(diagnostics/"formatting"/name)
    for directory in ("mode", "session/admission", "session/runtime", "session/emission", "session/reset"):
        sources.extend((diagnostics/directory).glob("*.java"))
    sources.append(diagnostics/"session/lifecycle/DiagnosticSessionSnapshotEventFields.java")
    sources.extend([HERE/"java/FindDiagnosticEmitterBridge.java", HERE/"java/FindDiagnosticHarness.java"])
    output=run_java(tmp_path,sources,"lavi.minecraft.task.find.FindRuntimeHarness")
    assert "SIMULATED_RUNTIME_CHECKS=" in output
    # Same compiled production Task/TaskRunner graph; only game/combat/path APIs are test doubles.
    engine = subprocess.run([shutil.which("java"), "-cp", str(tmp_path/"classes"),
                             "lavi.minecraft.task.find.FindExplorationHarness"],
                            capture_output=True, text=True, timeout=45)
    assert engine.returncode == 0, engine.stdout + engine.stderr
    print(engine.stdout.strip())
    assert "REAL_TASK_ENGINE_CHECKS=" in engine.stdout
    output += engine.stdout
    emitted = subprocess.run([shutil.which("java"), "-cp", str(tmp_path/"classes"),
                              "lavi.minecraft.task.find.FindDiagnosticHarness"],
                             capture_output=True, text=True, timeout=45)
    assert emitted.returncode == 0, emitted.stdout + emitted.stderr
    assert "ACTUAL_DIAGNOSTIC_OUTPUT_CHECKS=" in emitted.stdout
    print(emitted.stdout.strip())
    wire=[json.loads(line.split("=",1)[1]) for line in output.splitlines() if line.startswith("WIRE_FIND=")]
    assert len(wire)>=20
    for observation in wire:
        assert FindTerminalPayload.from_data({"find":observation}) is not None, observation

