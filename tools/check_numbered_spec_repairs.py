"""Bounded checks for numbered-spec repair work; no Gradle, game launch or voice model."""
from pathlib import Path
import hashlib
import json
import os
import subprocess
import sys
import time
import uuid

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "build-character-cast" / "numbered-repair-check" / (time.strftime("%Y%m%dT%H%M%SZ", time.gmtime()) + "-" + uuid.uuid4().hex[:8])
JAVA_HOME = Path(r"C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot")
SOURCES = [
    "src/main/java/com/coraline/feast/FamilyDinner.java",
    "src/main/java/com/coraline/feast/VisitEvents.java",
    "src/wybietest/java/com/coraline/feast/VisitEventsGameTests.java",
    "src/main/java/com/coraline/feast/TabletopGames.java",
    "src/main/java/com/coraline/feast/FamilyGameNight.java",
    "src/main/java/com/coraline/feast/FamilyOuting.java",
    "src/main/java/com/coraline/client/TabletopGameScreen.java",
    "src/main/java/com/coraline/client/renderer/ScenePropRenderer.java",
    "src/main/java/com/coraline/secrets/SecretsRuntime.java",
    "src/wybietest/java/com/coraline/feast/TabletopGamesGameTests.java",
    "src/wybietest/java/com/coraline/feast/FamilyGameNightGameTests.java",
    "src/wybietest/java/com/coraline/feast/FamilyOutingBoatGameTests.java",
    "src/activitytest/java/com/coraline/secrets/SecretsGameTests.java",
    "src/activitytest/java/com/coraline/secrets/JournalInspectionGameTests.java",
    "tools/TabletopRulesSmoke.java",
]

SHOW_RECORDINGS = {
    "wybie_adventure_intro": "voicelines/othermother/extra/WybieIntroduce.wav",
    "theatre_music": "music/eventbased/Theatre.mp3",
    **{"theatre_"+letter.lower(): "voicelines/MissSpinkAndForcible/other/"+letter+".wav" for letter in "ABCDEF"},
    "theatre_meet_parents": "voicelines/MissSpinkAndForcible/other/MeetParentsAfterEnd.wav",
    "mouse_circus_invite": "voicelines/othermother/extra/BobinskyMiceCircusInvite.wav",
    "mouse_circus_intro": "voicelines/bobinsky/other/CircusIntroduction.wav",
    "mouse_circus_music": "music/eventbased/micecircus.mp3",
    "mouse_circus_end": "voicelines/bobinsky/other/CircusEnd.wav",
}
def scan_show_recordings():
    import wave
    result={}
    for key,relative in SHOW_RECORDINGS.items():
        path=ROOT/relative
        if not path.is_file():
            result[key]={"source":relative,"status":"missing"};continue
        before=(path.stat().st_size,path.stat().st_mtime_ns)
        digest=hashlib.sha256()
        with path.open("rb") as source:
            for chunk in iter(lambda: source.read(1024*1024),b""):digest.update(chunk)
        row={"source":relative,"bytes":before[0],"sha256":digest.hexdigest()}
        try:
            if path.suffix.lower()==".wav":
                with wave.open(str(path),"rb") as audio:
                    row.update(seconds=audio.getnframes()/audio.getframerate(),sample_rate=audio.getframerate(),frames=audio.getnframes(),channels=audio.getnchannels(),sample_width=audio.getsampwidth(),decoder="Python wave header")
            else:
                from mutagen.mp3 import MP3
                info=MP3(path).info
                row.update(seconds=info.length,sample_rate=info.sample_rate,channels=info.channels,decoder="mutagen MP3 header")
            row["ticks_ceil"]=__import__("math").ceil(row["seconds"]*20)
            row["status"]="header_measured"
        except Exception as error:row.update(status="duration_unresolved",error=str(error))
        after=(path.stat().st_size,path.stat().st_mtime_ns)
        if after!=before:row.update(status="changed_during_scan",error="Source identity changed during bounded read")
        result[key]=row
    return result

def main():
    if sys.platform != "win32":
        raise SystemExit("This check must run in the actual Windows Coraline checkout.")
    import ctypes
    class Memory(ctypes.Structure):
        _fields_=[("length",ctypes.c_ulong),("load",ctypes.c_ulong),
                  ("total",ctypes.c_ulonglong),("available",ctypes.c_ulonglong),
                  ("page_total",ctypes.c_ulonglong),("page_available",ctypes.c_ulonglong),
                  ("virtual_total",ctypes.c_ulonglong),("virtual_available",ctypes.c_ulonglong),
                  ("extended_available",ctypes.c_ulonglong)]
    memory=Memory();memory.length=ctypes.sizeof(Memory)
    if not ctypes.windll.kernel32.GlobalMemoryStatusEx(ctypes.byref(memory)):
        raise SystemExit("Cannot verify available memory; no compiler started.")
    free_gib=memory.available/(1024**3)
    print(f"Free physical memory: {free_gib:.2f} GiB",flush=True)
    if free_gib < .8:
        raise SystemExit("Less than 0.8 GiB free; no compiler started. Free memory before retry.")
    OUT.mkdir(parents=True,exist_ok=False)
    print("Check output: "+str(OUT),flush=True)
    classes=OUT/"classes";classes.mkdir()
    cpfile=ROOT/"build-character-cast/classpath/runClient_minecraftClasspath.txt"
    jars=[line.strip() for line in cpfile.read_text(encoding="utf-8-sig").splitlines() if line.strip()]
    missing=[p for p in jars if not Path(p).is_file()]
    if missing:raise SystemExit("Existing generated classpath has missing entries: "+str(missing))
    inputs=[ROOT/p for p in SOURCES]
    fingerprint_inputs=[*inputs,Path(__file__).resolve(),cpfile]
    if any(not p.is_file() for p in inputs):raise SystemExit("Check source is missing.")
    cp=os.pathsep.join([str(classes),str(ROOT/"build-character-cast/classes/java/main"),*jars])
    # javac @file avoids Windows command-line limits. Separate outputs never enter release resources.
    quote=lambda value:'"'+str(value).replace("\\","/").replace('"','\\"')+'"'
    args=["-encoding","UTF-8","--release","17","-proc:none","-implicit:none",
          "-classpath",quote(cp),"-d",quote(classes),*[quote(p) for p in inputs]]
    argfile=OUT/"javac.args";argfile.write_text("\n".join(args),encoding="utf-8")
    report={"started":time.strftime("%Y-%m-%dT%H:%M:%SZ",time.gmtime()),
            "free_gib":free_gib,"source_sha256":{str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest() for p in fingerprint_inputs},
            "limits":"Targeted direct javac and real-NBT rules smoke only; not full Gradle, Forge GameTests, native visual QA or release verification."}
    def save_report():
        (OUT/"report.json").write_text(json.dumps(report,indent=2),encoding="utf-8")
    def check_inputs(stage):
        changed=[str(p.relative_to(ROOT)) for p in fingerprint_inputs
                 if not p.is_file() or hashlib.sha256(p.read_bytes()).hexdigest()!=report["source_sha256"][str(p.relative_to(ROOT))]]
        if changed:
            report["stopped"]={"stage":stage,"reason":"input fingerprint changed","paths":changed}
            save_report()
            raise SystemExit("Check inputs changed; refusing further steps: "+str(changed))
    def run(name,command,timeout):
        check_inputs("before "+name)
        if not ctypes.windll.kernel32.GlobalMemoryStatusEx(ctypes.byref(memory)) or memory.available/(1024**3)<.8:
            report["stopped"]={"stage":"before "+name,"reason":"available memory could not be verified at or above 0.8 GiB"}
            save_report()
            raise SystemExit("Memory safety cutoff; no "+name+" process started.")
        print(name+": "+" ".join(command[:4]),flush=True)
        logpath=OUT/(name+".log")
        started=time.monotonic()
        try:
            with logpath.open("w",encoding="utf-8") as log:
                process=subprocess.run(command,cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,timeout=timeout)
        except subprocess.TimeoutExpired:
            report[name]={"status":"timed_out","timeout_seconds":timeout,"seconds":time.monotonic()-started,"log":str(logpath)}
            save_report()
            print(logpath.read_text(encoding="utf-8",errors="replace")[-24000:],flush=True)
            raise SystemExit(name+" exceeded its bounded timeout; no subsequent process started.")
        except OSError as error:
            report[name]={"status":"launch_failed","error":str(error),"log":str(logpath)}
            save_report()
            raise SystemExit(name+" could not start: "+str(error))
        report[name]={"status":"passed" if process.returncode==0 else "failed","exit_code":process.returncode,"seconds":time.monotonic()-started,"log":str(logpath)}
        save_report()
        print(logpath.read_text(encoding="utf-8",errors="replace")[-24000:],flush=True)
        check_inputs("after "+name)
        if process.returncode:raise SystemExit(process.returncode)
    report["show_recordings"]=scan_show_recordings()
    save_report()
    print("SHOW_RECORDINGS: "+json.dumps(report["show_recordings"]),flush=True)
    run("javac",[str(JAVA_HOME/"bin/javac.exe"),"-J-Xmx256m","-J-XX:MaxMetaspaceSize=128m","@"+str(argfile)],240)
    run("tabletop-smoke",[str(JAVA_HOME/"bin/java.exe"),"-Xmx192m","-XX:MaxMetaspaceSize=96m","-cp",cp,"TabletopRulesSmoke"],120)
    print("CAPPED_REPAIR_CHECKS_OK",flush=True)
if __name__=="__main__":main()
