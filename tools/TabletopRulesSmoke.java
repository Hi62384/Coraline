import com.coraline.feast.TabletopGames;
import net.minecraft.nbt.CompoundTag;

/** CPU-only real-NBT rules regression. Does not bootstrap a server or touch any save. */
public final class TabletopRulesSmoke {
    private static int checks;
    private static void require(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    public static void main(String[] args){
        int completed=0;
        // Faster card turns finish both former timeout seeds normally.
        java.util.Set<String> expectedSafetyTimeouts=java.util.Set.of();
        java.util.Set<String> actualSafetyTimeouts=new java.util.HashSet<>();
        for(var game:TabletopGames.Game.values())for(int seed=0;seed<12;seed++){
            var n=TabletopGames.start(game,seed);
            for(int tick=0;tick<4250&&!n.getBoolean("Done");tick++){
                if(tick%120==0)n=n.copy();
                if(n.getInt("Physics")==0&&n.getInt("Turn")==0&&game!=TabletopGames.Game.SILENT_CHARADES&&tick%50==0)require(TabletopGames.move(n,TabletopGames.ai(n)),"Legal player move failed: "+game);
                if(game==TabletopGames.Game.SILENT_CHARADES&&tick%100==0)TabletopGames.move(n,n.getInt("Actor")==0?"clue:"+n.getInt("Topic")+":"+n.getInt("Topic"):"guess:"+n.getInt("Topic"));
                TabletopGames.tick(n);
            }
            System.out.println("MATCH "+game+" seed="+seed+" elapsed="+n.getInt("Elapsed")+" moves="+n.getInt("Moves")+" timedOut="+n.getBoolean("TimedOut")+" winner="+n.getInt("Winner"));
            require(n.getBoolean("Done"),"Unbounded game: "+game+" seed="+seed);
            if(TabletopGames.completedNormally(n))completed++;
            else {require(n.getBoolean("TimedOut")&&n.getInt("Winner")==-1,"Non-normal match was not a safe timeout");actualSafetyTimeouts.add(game.name()+":"+seed);for(int who=0;who<4;who++)require(!TabletopGames.participantWon(n,who),"Safety timeout awarded a winner");}
            require(n.getInt("Winner")>=-1&&n.getInt("Winner")<4,"Invalid winner: "+game);
        }
        for(var game:TabletopGames.Game.values()){
            var n=TabletopGames.start(game,1);n.putInt("Elapsed",4199);TabletopGames.tick(n);
            require(n.getBoolean("TimedOut")&&n.getInt("Winner")==-1&&!TabletopGames.completedNormally(n),"Timeout fabricated normal result: "+game);
            for(int who=0;who<4;who++)require(!TabletopGames.participantWon(n,who),"Timeout awarded winner");
            require(TabletopGames.resultText(n).equals("Time is up. No winner."),"Timeout caption wrong");
        }
        var thread=TabletopGames.start(TabletopGames.Game.THREAD_FOUR,2);thread.putBoolean("Done",true);
        for(int team=0;team<2;team++){
            thread.putInt("Winner",team);
            for(int who=0;who<4;who++)require(TabletopGames.participantWon(thread,who)==(team==((who==0||who==3)?0:1)),"Team outcome wrong");
        }
        thread.putInt("Winner",0);require(TabletopGames.resultText(thread).equals("You and Wybie won!"),"Team caption wrong");
        for(int loser=0;loser<4;loser++){
            var n=TabletopGames.start(TabletopGames.Game.WOBBLY_TOWER,3);n.putBoolean("Done",true);n.putInt("Loser",loser);
            for(int who=0;who<4;who++)require(TabletopGames.participantWon(n,who)==(who!=loser),"Tower survivor result wrong");
            require(TabletopGames.resultText(n).contains("The others win!"),"Tower caption wrong");
        }
        var flick=TabletopGames.start(TabletopGames.Game.BUTTON_FLICK,4);
        require(!TabletopGames.move(flick,"flick:NaN:1")&&flick.getInt("ActionCount")==0,"Rejected action counted");
        require(TabletopGames.move(flick,"flick:0:.4")&&flick.getInt("ActionCount")==1&&flick.getInt("Moves")==0,"Flick start reaction missing");
        require(!TabletopGames.move(flick,"flick:0:.4")&&flick.getInt("ActionCount")==1,"Concurrent flick counted");
        var cards=TabletopGames.start(TabletopGames.Game.BUTTON_CARDS,5);var visible=TabletopGames.visible(cards);
        require(!visible.contains("Hand1")&&!visible.contains("Deck")&&!visible.contains("Seed"),"Private cards leaked");
        var charades=TabletopGames.start(TabletopGames.Game.SILENT_CHARADES,6);require(!TabletopGames.visible(charades).contains("Topic"),"Secret charade answer leaked");
        require(completed==72&&actualSafetyTimeouts.equals(expectedSafetyTimeouts),"Unexpected timeout in the faster family-game baseline: "+actualSafetyTimeouts);
        System.out.println("TABLETOP_RULES_SMOKE_OK: "+checks+" assertions; 72 normal deterministic matches; timeout/team/tower/privacy/action regressions");
    }
}
