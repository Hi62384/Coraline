from pathlib import Path
base=Path('src/main/java/com/coraline/feast')
def edit(name,before,after):
 p=base/(name+'.java')
 s=p.read_text()
 assert before in s,name
 with p.open('w',newline='\r\n') as f:f.write(s.replace(before,after,1))
edit('RescueEpilogue','if(done){saveVision(p,s);if(p.server.getLevel(ActivityWorld.KEY)!=null){enterVision(p,s);RescueChapter.go(p,s,CELESTIAL_OPEN);}else RescueChapter.go(p,s,EYES_CLOSE);}', 'if(done){saveVision(p,s);RescueChapter.go(p,s,EYES_CLOSE);}')
edit('RescueEpilogue','''                var actors=new CinematicPropEntity[3];
                for(int i=0;i<actors.length;i++)actors[i]=ghost(p,s,i);
                for(var actor:actors)if(actor==null){
                    for(var staged:actors)if(staged!=null)staged.setInvisible(true);
                    n.putBoolean("MissingVisionActors",true);
                    n.putInt("Ticks",0);
                    return;
                }
                n.remove("MissingVisionActors");''','''                var actors=visionActors(p,s);
                if(actors==null){n.putInt("Ticks",0);return;}''')
edit('RescueEpilogue','''                if(!n.getBoolean("VisionRestored"))
                    for(int i=0;i<3;i++){var ghost=ghost(p,s,i);if(ghost!=null){ghost.setInvisible(false);ghost.frame(t>=240?5:4,t,1);}}
                if(done){''','''                if(!n.getBoolean("VisionRestored")&&!n.getBoolean("DreamDialogueComplete")){
                    var actors=visionActors(p,s);
                    if(actors==null)return;
                    for(var actor:actors){actor.setInvisible(false);actor.frame(t>=240?5:4,t,1);}
                    if(done){
                        n.putBoolean("DreamDialogueComplete",true);
                        OtherMotherLoop.changed(p);
                    }
                }
                if(n.getBoolean("DreamDialogueComplete")||n.getBoolean("VisionRestored")){''')
edit('RescueEpilogue','    private static CinematicPropEntity ghost(','''    static CinematicPropEntity[] visionActors(ServerPlayer p,VisitData.Scene s){
        var actors=new CinematicPropEntity[3];
        boolean ready=true;
        for(int i=0;i<actors.length;i++){
            actors[i]=ghost(p,s,i);
            ready&=actors[i]!=null;
        }
        var n=RescueChapter.data(s);
        if(ready){n.remove("MissingVisionActors");return actors;}
        n.putBoolean("MissingVisionActors",true);
        for(var actor:actors)if(actor!=null)actor.setInvisible(true);
        return null;
    }
    private static CinematicPropEntity ghost(''')
edit('RescueEpilogue','for(var family:FamilyData.get(p.server).families.values()){\n                    if(!s.owner.equals(family.owner)||family.bed==null||FamilyData.level(p.server,family.homeDim)!=l\n                            ||family.bed.distSqr(door.pos)>32*32)continue;\n                    target=nearReturn(l,p,pose,family.bed,6);\n                    if(target!=null)break;\n                }', '''var family=ParentAccess.family(p.server,s.owner);
                if(family!=null&&family.bed!=null&&FamilyData.level(p.server,family.homeDim)==l
                        &&family.bed.distSqr(door.pos)<=32*32)target=nearReturn(l,p,pose,family.bed,6);''')
edit('RescueEpilogue','    private static boolean safeReturn(', '    static boolean safeReturn(')
edit('RescueChapter','        String recording=sound(state);boolean done=false;', '''        if(state==Phase.FAREWELL_C){
            if(n.getBoolean("DreamDialogueComplete")||n.getBoolean("VisionRestored")){
                RescueEpilogue.tick(p,s,state,n.getInt("Ticks"),true);
                if(p.tickCount%5==0)sync(p,s);
                return;
            }
            if(p.serverLevel()!=p.server.getLevel(ActivityWorld.KEY)){
                RescueEpilogue.recoverVision(p,s);
                return;
            }
            if(RescueEpilogue.visionActors(p,s)==null){
                if(n.getBoolean("Audio")){ShowPlayback.stop(p,s);n.remove("Audio");n.remove("AudioTail");}
                if(p.tickCount%5==0)sync(p,s);
                return;
            }
        }
        String recording=sound(state);boolean done=false;''')
edit('RescueChapter','            done=ShowPlayback.tick(p,s);t=ShowPlayback.position(s);','''            done=ShowPlayback.tick(p,s);t=ShowPlayback.position(s);
            if(state.ordinal()>=Phase.REUNION_A.ordinal()&&ShowPlayback.data(s).getBoolean("SafetySkipped")){
                ShowPlayback.stop(p,s);
                n.remove("Audio");
                n.remove("AudioTail");
                n.putString("Blocker","Retrying interrupted story recording: "+recording);
                sync(p,s);
                return;
            }
            if(state.ordinal()>=Phase.REUNION_A.ordinal()&&ShowPlayback.started(s)&&ShowPlayback.data(s).getString("Blocker").isEmpty())n.remove("Blocker");''')
edit('RescueChapter','if(state==Phase.TUNNEL_CHASE||RescueBosses.fighting(state))return;', 'if(state==Phase.TUNNEL_CHASE||RescueBosses.fighting(state)||state.ordinal()>=Phase.REUNION_A.ordinal())return;')
edit('PalaceWellEncounter','''        boolean done=ShowPlayback.tick(p,s);if(!ShowPlayback.data(s).getString("Blocker").isEmpty())pause(p,s,n,ShowPlayback.data(s).getString("Blocker"));return done&&!ShowPlayback.debugSeeking(s);''','''        boolean done=ShowPlayback.tick(p,s);
        if(ShowPlayback.data(s).getBoolean("SafetySkipped")||!ShowPlayback.data(s).getString("Blocker").isEmpty()){
            pause(p,s,n,"The original well recording was interrupted. Use /coralinepalace well-resume to retry.");
            return false;
        }
        return done&&!ShowPlayback.debugSeeking(s);''')
edit('PalaceKeyCustody','&&ShowPlayback.data(s).getString("Sound").equals("crawling_hand_e");','&&ShowPlayback.data(s).getString("Sound").equals("crawling_hand_e")&&!ShowPlayback.data(s).getBoolean("SafetySkipped");')
