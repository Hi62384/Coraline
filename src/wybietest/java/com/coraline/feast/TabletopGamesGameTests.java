package com.coraline.feast;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import java.util.*;
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class TabletopGamesGameTests {
    @GameTest(template="empty") public static void allSixGamesFinishAndSurviveSave(GameTestHelper h){for(var game:TabletopGames.Game.values())for(int seed=0;seed<12;seed++){var n=TabletopGames.start(game,seed);for(int tick=0;tick<4250&&!n.getBoolean("Done");tick++){if(tick%120==0)n=n.copy();if(n.getInt("Physics")==0&&n.getInt("Turn")==0&&game!=TabletopGames.Game.SILENT_CHARADES&&tick%50==0)h.assertTrue(TabletopGames.move(n,TabletopGames.ai(n)),"Player/NPC legal move failed in "+game);if(game==TabletopGames.Game.SILENT_CHARADES&&tick%100==0)TabletopGames.move(n,n.getInt("Actor")==0?"clue:"+n.getInt("Topic")+":"+n.getInt("Topic"):"guess:"+n.getInt("Topic"));TabletopGames.tick(n);}h.assertTrue(n.getBoolean("Done")&&n.getInt("Winner")>=-1&&n.getInt("Winner")<4,"Unbounded game "+game);}h.succeed();}
    @GameTest(template="empty") public static void cardsHideOtherHandsAndRejectIllegalMoves(GameTestHelper h){var n=TabletopGames.start(TabletopGames.Game.BUTTON_CARDS,9);n.putInt("Top",0);n.putInt("Color",0);n.putIntArray("Hand0",new int[]{9,15,4});h.assertFalse(TabletopGames.move(n,"card:0:0"),"Illegal color/number accepted");h.assertTrue(TabletopGames.move(n,"card:1:2")&&n.getInt("Color")==2,"Wild card failed");var visible=TabletopGames.visible(n);h.assertFalse(visible.contains("Hand1")||visible.contains("Deck")||visible.contains("Seed"),"Opponent private cards leaked");h.succeed();}
    @GameTest(template="empty") public static void threadTeamsAndStrategicBlock(GameTestHelper h){var n=TabletopGames.start(TabletopGames.Game.THREAD_FOUR,10);var b=n.getIntArray("Board");b[0]=b[1]=b[2]=0;n.putIntArray("Board",b);n.putInt("Turn",1);h.assertTrue(TabletopGames.ai(n).equals("column:3"),"Mother failed obvious block");TabletopGames.move(n,"column:3");h.assertTrue(n.getInt("Turn")==3,"Wybie wasn't team's next participant");n.putInt("Turn",0);n.getIntArray("Board")[3]=-1;TabletopGames.move(n,"column:3");h.assertTrue(n.getBoolean("Done")&&n.getInt("Winner")==0,"Connect-four win not detected");h.succeed();}
    @GameTest(template="empty") public static void towerSupportAndPrecisionMatter(GameTestHelper h){var n=TabletopGames.start(TabletopGames.Game.WOBBLY_TOWER,10);var rows=n.getIntArray("Tower");h.assertTrue(TabletopGames.towerCost(rows,0,1,.2)>TabletopGames.towerCost(rows,8,0,1),"Support/precision don't affect stability");h.assertTrue(TabletopGames.move(n,"pull:8:0:1"),"Loose block rejected");double safe=n.getDouble("Stability");n.putInt("Turn",0);TabletopGames.move(n,"pull:8:1:0");h.assertTrue(n.getDouble("Stability")<safe-.2,"Rough extraction didn't affect stability");h.assertFalse(TabletopGames.move(n,"pull:8:1:1"),"Removed block pulled twice");h.succeed();}
    @GameTest(template="empty") public static void flickUsesPhysicsAndThreeTurnsEach(GameTestHelper h){var n=TabletopGames.start(TabletopGames.Game.BUTTON_FLICK,8);h.assertFalse(TabletopGames.move(n,"flick:NaN:1"),"Invalid aim accepted");h.assertTrue(TabletopGames.move(n,"flick:0:.4"),"Flick failed");h.assertFalse(TabletopGames.move(n,"flick:0:.4"),"Second flick while puck moving accepted");double before=n.getList("Pucks",10).getCompound(0).getDouble("Z");TabletopGames.tick(n);h.assertTrue(n.getList("Pucks",10).getCompound(0).getDouble("Z")>before,"Puck didn't move");for(int i=0;i<4250&&!n.getBoolean("Done");i++){if(n.getInt("Physics")==0&&n.getInt("Turn")==0)TabletopGames.move(n,"flick:0:.4");TabletopGames.tick(n);}h.assertTrue(n.getList("Pucks",10).size()==12&&n.getBoolean("Done"),"Not exactly three flicks per actor");h.assertTrue(TabletopGames.puckScore(0,.35)==5&&TabletopGames.puckScore(.8,-.8)==0,"Scoring doesn't follow landing position");h.succeed();}
    @GameTest(template="empty") public static void charadesHideAnswersAndScoreActing(GameTestHelper h){var n=TabletopGames.start(TabletopGames.Game.SILENT_CHARADES,3);h.assertFalse(TabletopGames.visible(n).contains("Topic"),"Wybie's secret answer leaked");TabletopGames.move(n,"guess:"+n.getInt("Topic"));TabletopGames.move(n,"guess:"+n.getInt("Topic"));h.assertTrue(n.getInt("Actor")==0&&TabletopGames.visible(n).contains("Topic"),"Player didn't receive acting turn");int before=n.getIntArray("Scores")[0],topic=n.getInt("Topic");TabletopGames.move(n,"clue:"+topic+":"+topic);h.assertTrue(n.getIntArray("Scores")[0]>=before+2,"Good clue sequence wasn't scored");h.succeed();}

    @GameTest(template="empty")
    public static void resultsDescribeTeamsAndAllTowerSurvivors(GameTestHelper h){
        var thread=TabletopGames.start(TabletopGames.Game.THREAD_FOUR,1);
        thread.putBoolean("Done",true);thread.putInt("Winner",0);
        h.assertTrue(TabletopGames.resultText(thread).equals("You and Wybie won!"),"Player team result was shown as a solo win");
        h.assertTrue(TabletopGames.participantWon(thread,0)&&TabletopGames.participantWon(thread,3),"Thread Four teammate victory missing");
        h.assertFalse(TabletopGames.participantWon(thread,1)||TabletopGames.participantWon(thread,2),"Losing parents counted as winners");
        thread.putInt("Winner",1);
        h.assertTrue(TabletopGames.resultText(thread).equals("Your Other Parents won!")&&TabletopGames.participantWon(thread,1)&&TabletopGames.participantWon(thread,2),"Parent team result incorrect");
        thread.putInt("Winner",-1);
        h.assertTrue(TabletopGames.resultText(thread).equals("A draw!")&&!TabletopGames.participantWon(thread,0),"Draw awarded a team win");
        for(int loser=0;loser<4;loser++){
            var tower=TabletopGames.start(TabletopGames.Game.WOBBLY_TOWER,2);
            tower.putBoolean("Done",true);tower.putInt("Loser",loser);tower.putInt("Winner",(loser+1)%4);
            h.assertTrue(TabletopGames.resultText(tower).contains("toppled the tower. The others win!"),"Tower result invented a single winner");
            for(int who=0;who<4;who++)h.assertTrue(TabletopGames.participantWon(tower,who)==(who!=loser),"Wrong tower survivor outcome");
        }
        h.succeed();
    }
    @GameTest(template="empty")
    public static void safetyTimeoutDoesNotInventWinnersOrCollapse(GameTestHelper h){
        for(var game:TabletopGames.Game.values()){
            var n=TabletopGames.start(game,3);n.putInt("Elapsed",4199);n.putInt("TurnTicks",0);
            TabletopGames.tick(n);
            h.assertTrue(n.getBoolean("Done")&&n.getBoolean("TimedOut")&&n.getInt("Winner")==-1,"Deadline fabricated a winner for "+game);
            h.assertFalse(TabletopGames.completedNormally(n),"Timed out game qualified for completion rewards");
            for(int who=0;who<4;who++)h.assertFalse(TabletopGames.participantWon(n,who),"Timeout awarded a participant win");
            h.assertTrue(TabletopGames.resultText(n).equals("Time is up. No winner."),"Timeout shown as normal draw/win");
            if(game==TabletopGames.Game.WOBBLY_TOWER)h.assertTrue(!n.contains("Loser")&&n.getDouble("Stability")==1,"Timeout fabricated a tower collapse");
            var loaded=n.copy();h.assertTrue(loaded.getBoolean("TimedOut")&&!TabletopGames.completedNormally(loaded),"Timeout state lost on save/reload");
        }
        h.succeed();
    }
    @GameTest(template="empty")
    public static void actionCountTracksAcceptedActionsAndFlickStart(GameTestHelper h){
        var n=TabletopGames.start(TabletopGames.Game.BUTTON_FLICK,4);
        h.assertFalse(TabletopGames.move(n,"flick:NaN:1"),"Malformed flick accepted");
        h.assertTrue(n.getInt("ActionCount")==0,"Rejected action changed reaction counter");
        h.assertTrue(TabletopGames.move(n,"flick:0:.4")&&n.getInt("ActionCount")==1&&n.getInt("LastActor")==0,"Accepted flick did not trigger its actor reaction");
        h.assertTrue(n.getInt("Moves")==0,"Flick advanced turn before physics settled");
        h.assertFalse(TabletopGames.move(n,"flick:0:.4"),"Concurrent flick accepted");
        h.assertTrue(n.getInt("ActionCount")==1,"Rejected concurrent flick changed reaction counter");
        h.succeed();
    }
}
