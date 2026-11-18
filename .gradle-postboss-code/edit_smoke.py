from pathlib import Path
p=Path('tools/PalaceWellSmoke.java')
s=p.read_text().replace('state.originalWybie().isPresent()?DEAD_AT_WELL:DEAD_BEFORE_WELL','state.originalWybie().isPresent()?LOST_AT_WELL:DEAD_BEFORE_WELL')
s=s.replace('state.wybieLife()==DEAD_AT_WELL','state.wybieLife()==LOST_AT_WELL')
s=s.replace('Deadline did not permanently record well death','Deadline did not record the original Wybie as lost')
s=s.replace('''                state=reload(state);
                for(int retry''','''                state=reload(state);
                var legacy=state.save();
                legacy.putString("WybieLife","DEAD_AT_WELL");
                var historical=PalaceWellProgress.load(OWNER,DOOR,legacy);
                check(historical.blocker().isEmpty()&&historical.wybieLife()==DEAD_AT_WELL
                        &&!historical.wybieCanAssist(),"Legacy recorded death was overwritten or could assist");
                for(int retry''')
p.write_text(s)
