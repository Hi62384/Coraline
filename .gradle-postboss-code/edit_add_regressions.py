from pathlib import Path
base=Path('src/main/java/com/coraline/feast')
def read(name):return (base/(name+'.java')).read_text()
def put(name,s):
 with (base/(name+'.java')).open('w',newline='\r\n') as f:f.write(s)
s=read('RescueBosses')
s=s.replace('''        if(weak!=null){n.putDouble''','''        if(state==SWARM_FIGHT&&weak==null){
            n.putInt("Ticks",Math.max(0,time-1));
            n.putString("Blocker","Waiting for a required Ratmaster attack prop.");
            return;
        }
        if(weak!=null){n.putDouble''')
s=s.replace('''            if(ball!=null){ball.setPos(ballAt);ball.frame(0,beat,1);}''','''            if(ball==null)return null;
            ball.setPos(ballAt);
            ball.frame(0,beat,1);''')
put('RescueBosses',s)
s=read('BeldamFinale').replace('    private static boolean enterWeb(', '    static boolean enterWeb(')
put('BeldamFinale',s)
s=read('RescueChapter')
s=s.replace('state.ordinal()>=Phase.REUNION_A.ordinal()&&ShowPlayback','state.ordinal()>=Phase.BOB_A.ordinal()&&ShowPlayback')
a=s.index('        if(state==Phase.TUNNEL_CHASE||state==Phase.HAND_FALL')
b=s.index('        boolean motion=',a)
s=s[:a]+'''        if(RescueBosses.fighting(state)||state.ordinal()>=Phase.BOB_A.ordinal())return;
'''+s[b:]
put('RescueChapter',s)
p=Path('src/wybietest/java/com/coraline/feast/RescueRatGameTests.java')
s=p.read_text().replace('l.addFreshEntity(rat);int[] ticks={0};','''l.addFreshEntity(rat);
        RescueChapter.data(s).putUUID("ChaseRat",rat.getUUID());
        int[] ticks={0};
        java.util.UUID[] discarded={null};''')
s=s.replace('''if(ball!=null){h.assertTrue(rat.getVehicle()==ball,"Actual rat lost its ball during movement");''','''if(ball!=null){
                if(discarded[0]==null){
                    discarded[0]=ball.getUUID();
                    ball.ejectPassengers();
                    ball.discard();
                    return;
                }
                h.assertFalse(ball.getUUID().equals(discarded[0]),"Destroyed ball UUID was reused");
                h.assertTrue(rat.getVehicle()==ball,"Actual rat lost its ball during movement");''')
p.write_text(s)
p=Path('src/wybietest/java/com/coraline/feast/RescueBossGameTests.java')
s=p.read_text()
i=s.rfind('}')
s=s[:i]+'''    @GameTest(template="empty") public static void legacyWebEntryCannotCommitWithoutTheOriginalMother(GameTestHelper h){
        var p=player(h);
        var s=OtherMotherLoop.scene(p);
        var n=RescueChapter.data(s);
        n.putString("Phase","FINAL_P");
        n.putBoolean("WebEntered",true);
        try{
            h.assertFalse(BeldamFinale.enterWeb(p,s,null),"Missing Mother allowed the web confrontation to start");
            h.assertTrue(!n.getBoolean("WebEntered")&&!n.getBoolean("WebEntryReady")&&n.contains("Blocker"),
                    "Legacy partial transfer retained a misleading ready receipt");
        }finally{clean(p);}
        h.succeed();
    }
    @GameTest(template="empty") public static void lostCatPausesCollapseInsteadOfConsumingTheEscapeClock(GameTestHelper h){
        var p=player(h);
        var s=OtherMotherLoop.scene(p);
        var other=p.server.getLevel(com.coraline.otherworld.OtherWorld.KEY);
        var body=new ServerPlayer(p.server,other,p.getGameProfile());
        body.connection=p.connection;
        var n=RescueChapter.data(s);
        n.putString("Phase","COLLAPSE");
        n.putInt("CollapseElapsed",123);
        n.putInt("CollapseDuration",1500);
        n.putDouble("CollapseRadius",82);
        try{
            RescueEncounters.tick(body,s,RescueChapter.Phase.COLLAPSE,1600,false);
            h.assertTrue(n.getInt("CollapseElapsed")==123&&n.getDouble("CollapseRadius")==82
                    &&!n.getBoolean("CatRescued")&&n.contains("Blocker"),"Missing Cat spent the clock or granted a rescue");
        }finally{clean(p);}
        h.succeed();
    }
'''+s[i:]
p.write_text(s)
