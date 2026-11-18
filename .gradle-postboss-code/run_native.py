import json, os, pathlib, subprocess
launch=json.loads(pathlib.Path("build-activity/qa-launch-runGameTestServer.json").read_text())
command=launch["command"]
command[1:1]=["-Xmx1536m","-XX:+UseSerialGC"]
env=dict(os.environ)
env.update({str(k):str(v) for k,v in launch.get("environment",{}).items()})
with open(".gradle-postboss-code/postboss-native.log","w",encoding="utf-8") as log:
 raise SystemExit(subprocess.call(command,cwd=launch["cwd"],env=env,stdout=log,stderr=subprocess.STDOUT))
