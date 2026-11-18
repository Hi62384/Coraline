from pathlib import Path
base=Path('src/main/java/com/coraline/feast')
def read(name):return (base/(name+'.java')).read_text()
def put(name,s):
 with (base/(name+'.java')).open('w',newline='\r\n') as f:f.write(s)
s=read('RescueBosses')
i=s.index('    public static void retry(')
s=s[:i]+'''    static boolean resume(ServerPlayer p,VisitData.Scene s){
        var phase=RescueChapter.phase(s);
        var level=p.server.getLevel(phase==MANTIS_FIGHT?OtherWorld.KEY:ActivityWorld.KEY);
        var center=RescueEncounters.center(p,s);
        if(level==null)return false;
        if((p.serverLevel()!=level||p.distanceToSqr(center)>38*38)&&!RescueChapter.transfer(p,level,BlockPos.containing(center).south(10)))return false;
        var n=RescueChapter.data(s);
        n.putBoolean("Vulnerable",false);
        n.putInt("BossCycleStart",n.getInt("Ticks")+1);
        for(String key:new String[]{"WeakUntil","SocketCycle","SocketTier","TheatreCycle","RatAttackCycle","RatStrikeSpent"})n.remove(key);
        OtherMotherLoop.changed(p);
        return true;
    }
'''+s[i:]
put('RescueBosses',s)
s=read('RescueChapter')
s=s.replace('''        if(state==Phase.ROOM_LOCKED&&!BeldamFinale.keepPlayerInRoom''','''        if(!RescueEncounters.presentationReady(p,s,state)){if(p.tickCount%5==0)sync(p,s);return;}
        if(state==Phase.ROOM_LOCKED&&!BeldamFinale.keepPlayerInRoom''')
s=s.replace('''        if(state==Phase.TUNNEL_CHASE||RescueBosses.fighting(state)||state.ordinal()>=Phase.REUNION_A.ordinal())return;''','''        if(state==Phase.TUNNEL_CHASE||state==Phase.HAND_FALL||state==Phase.SWARM_TRANSFORM||state==Phase.FURNACE||state==Phase.BOB_TENT
                ||state==Phase.BOB_D||RescueBosses.fighting(state)||state.ordinal()>=Phase.REUNION_A.ordinal())return;''')
s=s.replace('''var state=phase(s);if(RescueBosses.fighting(state)){RescueBosses.retry(p,s,"The challenge resumes from its checkpoint.");return;}
        if(state==Phase.RAT_CHASE||state==Phase.COLLAPSE){RescueEncounters.retry(p,s,state);return;}''','''var state=phase(s);
        if(RescueBosses.fighting(state)){
            if(!RescueBosses.resume(p,s))data(s).putBoolean("Recover",true);
            return;
        }
        if(state==Phase.RAT_CHASE){RescueEncounters.retry(p,s,state);return;}
        if(state==Phase.COLLAPSE){
            var level=p.server.getLevel(OtherWorld.KEY);
            if(p.serverLevel()!=level&&!transfer(p,level,BlockPos.of(data(s).getLong("CollapseStart"))))data(s).putBoolean("Recover",true);
            return;
        }''')
s=s.replace('if(n.getBoolean("Recover")){n.remove("Recover");recover(p,s);state=phase(s);}', 'if(n.getBoolean("Recover")){n.remove("Recover");recover(p,s);if(n.getBoolean("Recover"))return;state=phase(s);}')
s=s.replace('else recover(p,s);}', 'else if(phase(s)==Phase.COLLAPSE)RescueEncounters.retry(p,s,phase(s));else recover(p,s);}',1)
put('RescueChapter',s)
s=read('RescueEncounters')
i=s.index('    public static void tick(')
s=s[:i]+'''    static boolean presentationReady(ServerPlayer p,VisitData.Scene s,RescueChapter.Phase phase){
        if(phase!=BOB_A&&phase!=BOB_B&&phase!=BOB_C&&phase!=BOB_D&&phase!=BOB_E)return true;
        var level=p.server.getLevel(phase==BOB_E?ActivityWorld.KEY:OtherWorld.KEY);
        if(level==null||p.serverLevel()!=level)return false;
        var tent=MouseCircusSite.tent(p,s);
        if(phase!=BOB_E&&tent==null)return false;
        var at=phase==BOB_E?Vec3.atBottomCenterOf(MouseCircusSite.center(p,s)).add(0,2,0):Vec3.atBottomCenterOf(tent.south(6));
        var bob=ShowActors.acquire(p,s,"evil_other_bobinsky",level,at);
        if(bob==null||bob.level()!=level){
            var n=RescueChapter.data(s);
            if(n.getBoolean("Audio")){ShowPlayback.stop(p,s);n.remove("Audio");n.remove("AudioTail");}
            n.putString("Blocker","Waiting for the original Bobinsky before starting his dialogue.");
            return false;
        }
        return true;
    }
'''+s[i:]
put('RescueEncounters',s)
s=read('BeldamFinale')
s=s.replace('''case HAND_FALL->{RescuePassage.prepare(p,s);var hand=RescueProps.find(p,s,"SeveredHand");if(hand!=null){hand.frame(1,t,1);if(t<20)hand.setPos(hand.position().add(0,-.015,0));}if(t>=60&&RescuePassage.prepare(p,s)){n.remove("PassageEntered");RescueChapter.go(p,s,TUNNEL_CHASE);}}''','''case HAND_FALL->{
                RescuePassage.prepare(p,s);
                var hand=RescueProps.find(p,s,"SeveredHand");
                if(hand==null||hand.kind()!=RescueProps.HAND||hand.level()!=p.level()){
                    n.putInt("Ticks",Math.max(0,t-1));
                    n.putString("Blocker","Waiting for the original severed Hand.");
                    return;
                }
                hand.frame(1,t,1);
                if(t<20)hand.setPos(hand.position().add(0,-.015,0));
                if(t>=60&&RescuePassage.prepare(p,s)){n.remove("PassageEntered");RescueChapter.go(p,s,TUNNEL_CHASE);}
            }''')
s=s.replace('''        var state=RescueChapter.phase(s);var n=RescueChapter.data(s);
        if(state==SHOW_EYES''','''        var state=RescueChapter.phase(s);var n=RescueChapter.data(s);
        if(roomLocked(s)&&(p.serverLevel()!=p.server.getLevel(OtherWorld.KEY)||!FirstEscapeScene.fullyInRoom(s,p.getBoundingBox())))return;
        if(state==SHOW_EYES''')
s=s.replace('''else if(state==DOOR_STRUGGLE&&action.equals("struggle"))n.putLong("PressUntil",p.serverLevel().getGameTime()+6);''','''else if(state==DOOR_STRUGGLE&&action.equals("struggle")&&p.serverLevel()==p.server.getLevel(ActivityWorld.KEY)
                &&p.distanceToSqr(RescueWebSite.at(p,s,1,1))<9)n.putLong("PressUntil",p.serverLevel().getGameTime()+6);''')
s=s.replace('''if(!frame.is(ModBlocks.TINY_DOOR.get()))return true;p.serverLevel().setBlock(at,frame.setValue(TinyDoorBlock.OPEN,false).setValue(TinyDoorBlock.PORTAL,false),3);n.putBoolean''','''if(!frame.is(ModBlocks.TINY_DOOR.get())||!(p.serverLevel().getBlockEntity(at) instanceof DoorBlockEntity tile)||!s.door.equals(tile.doorId))return true;
        var closed=frame.setValue(TinyDoorBlock.OPEN,false).setValue(TinyDoorBlock.PORTAL,false);
        p.serverLevel().setBlock(at,closed,3);
        if(!p.serverLevel().getBlockState(at).equals(closed))return true;
        n.putBoolean''')
put('BeldamFinale',s)
