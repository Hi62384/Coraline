package com.coraline.feast;

import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.gametest.*;
import java.util.*;

/** Focused roster rules; native Forge execution is still required in addition to isolated checks. */
@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class TabletopRosterGameTests {
    static List<TabletopGames.Participant> roster(int count){
        var roster=new ArrayList<TabletopGames.Participant>();
        for(int i=0;i<count;i++)roster.add(new TabletopGames.Participant("actual-"+i,i==0?"You":"Guest "+i,i==0||i==3?0:1));
        return roster;
    }
    static void rejects(GameTestHelper h,Runnable action,String message){
        boolean rejected=false;try{action.run();}catch(IllegalArgumentException expected){rejected=true;}
        h.assertTrue(rejected,message);
    }
    @GameTest(template="empty") public static void allSixRosterSizesHaveOnlyActualSeats(GameTestHelper h){
        for(var game:TabletopGames.Game.values())for(int count=2;count<=4;count++){
            var n=TabletopGames.start(game,47,roster(count));
            h.assertTrue(TabletopGames.participantCount(n)==count&&TabletopGames.validRoster(n),"Wrong roster count for "+game);
            h.assertTrue(n.getIntArray("Scores").length==count,"Phantom score slots");
            for(int i=0;i<count;i++)h.assertTrue(TabletopGames.participantName(n,i).equals(i==0?"You":"Guest "+i),"Participant identity lost");
            if(game==TabletopGames.Game.BUTTON_CARDS){
                for(int i=0;i<count;i++)h.assertTrue(n.getIntArray("Hand"+i).length==5,"Wrong deal");
                for(int i=count;i<4;i++)h.assertFalse(n.contains("Hand"+i),"Cards dealt to absent seat");
                h.assertTrue(n.getInt("DrawCursor")==count*5+1,"Wrong deck cursor");
            }
            if(game==TabletopGames.Game.MOUSEBOARD_DASH)h.assertTrue(n.getIntArray("Mouse").length==count,"Phantom mice");
            if(game==TabletopGames.Game.SILENT_CHARADES)h.assertTrue(n.getInt("Actor")==count-1,"Absent starting actor");
            h.assertFalse(TabletopGames.participantWon(n,-1)||TabletopGames.participantWon(n,count),"Absent winner");
        }h.succeed();
    }
    @GameTest(template="empty") public static void invalidRostersAreRejectedAndCorruptSavesCancel(GameTestHelper h){
        for(int count:new int[]{0,1,5})rejects(h,()->TabletopGames.start(TabletopGames.Game.BUTTON_CARDS,1,roster(count)),"Invalid participant count accepted");
        rejects(h,()->TabletopGames.start(TabletopGames.Game.BUTTON_CARDS,1,null),"Null roster accepted");
        rejects(h,()->new TabletopGames.Participant("","Guest",0),"Empty ID accepted");
        rejects(h,()->new TabletopGames.Participant("id","\n",0),"Blank name accepted");
        rejects(h,()->new TabletopGames.Participant("id","Guest",2),"Invalid team accepted");
        var duplicate=List.of(new TabletopGames.Participant("same","You",0),new TabletopGames.Participant("same","Guest",1));
        rejects(h,()->TabletopGames.start(TabletopGames.Game.BUTTON_CARDS,1,duplicate),"Duplicate actual participant accepted");
        for(var teams:List.of(new int[]{0,0},new int[]{1,0},new int[]{0,-1})){
            var bad=List.of(new TabletopGames.Participant("owner","You",teams[0]),new TabletopGames.Participant("guest","Guest",teams[1]));
            rejects(h,()->TabletopGames.start(TabletopGames.Game.THREAD_FOUR,1,bad),"Invalid Thread Four teams accepted");
        }
        var n=TabletopGames.start(TabletopGames.Game.BUTTON_CARDS,1,roster(2));n.putInt("ParticipantCount",3);
        h.assertFalse(TabletopGames.move(n,"draw"),"Corrupt roster accepted a move");
        h.assertTrue(n.getBoolean("Cancelled")&&!TabletopGames.completedNormally(n),"Corrupt roster was not safely cancelled");
        var thread=TabletopGames.start(TabletopGames.Game.THREAD_FOUR,1,roster(3));thread.putIntArray("TeamLast",new int[0]);TabletopGames.tick(thread);
        h.assertTrue(thread.getBoolean("Cancelled"),"Corrupt team rotation was not safely cancelled");
        h.succeed();
    }
    @GameTest(template="empty") public static void threadAlternatesTeamsAndRotatesRealMembersAcrossCopy(GameTestHelper h){
        for(int count=2;count<=4;count++){
            var n=TabletopGames.start(TabletopGames.Game.THREAD_FOUR,1,roster(count));
            int[] seen=new int[count];
            for(int step=0;step<12;step++){
                int who=n.getInt("Turn"),team=TabletopGames.participantTeam(n,who);seen[who]++;
                int[] empty=new int[42];Arrays.fill(empty,-1);n.putIntArray("Board",empty);
                h.assertTrue(TabletopGames.move(n,"column:3"),"Thread Four legal move failed");
                h.assertTrue(TabletopGames.participantTeam(n,n.getInt("Turn"))!=team,"Same team played twice");
                n=n.copy();
            }
            for(int times:seen)h.assertTrue(times>0,"An actual participant never played");
            n.putBoolean("Done",true);n.putInt("Winner",0);
            for(int who=0;who<count;who++)h.assertTrue(TabletopGames.participantWon(n,who)==(who==0||who==3),"Wrong Thread Four teammate win");
        }
        var wybie=List.of(new TabletopGames.Participant("owner","You",0),new TabletopGames.Participant("wybie","Other Wybie",0),new TabletopGames.Participant("spink","Other Spink",1),new TabletopGames.Participant("forcible","Other Forcible",1));
        var n=TabletopGames.start(TabletopGames.Game.THREAD_FOUR,1,wybie);
        int[] expected={0,2,1,3};for(int who:expected){h.assertTrue(n.getInt("Turn")==who,"Team rotation depended on legacy seats");TabletopGames.advance(n,0);}
        n.putBoolean("Done",true);n.putInt("Winner",0);
        h.assertTrue(TabletopGames.resultText(n).equals("You + Other Wybie won!"),"Custom team label lost");
        h.succeed();
    }
    @GameTest(template="empty") public static void cardsReverseSkipAndPenaltyStayInsideRoster(GameTestHelper h){
        for(int count=2;count<=4;count++){
            var n=TabletopGames.start(TabletopGames.Game.BUTTON_CARDS,1,roster(count));
            n.putInt("Top",0);n.putInt("Color",0);n.putIntArray("Hand0",new int[]{5,0});
            h.assertTrue(TabletopGames.move(n,"card:0:0")&&n.getInt("Turn")==count-1,"Reverse selected absent seat");
            n.putInt("Turn",0);n.putIntArray("Hand0",new int[]{4,0});
            h.assertTrue(TabletopGames.move(n,"card:0:0")&&n.getInt("Turn")==Math.floorMod(-2,count),"Skip selected absent seat");
            n.putInt("Turn",0);n.putIntArray("Hand0",new int[]{6,0});int before=n.getIntArray("Hand"+(count-1)).length;
            h.assertTrue(TabletopGames.move(n,"card:0:0")&&n.getIntArray("Hand"+(count-1)).length==before+1,"Penalty hit wrong participant");
            for(int i=count;i<4;i++)h.assertFalse(n.contains("Hand"+i),"Penalty created phantom hand");
        }h.succeed();
    }
    @GameTest(template="empty") public static void charadesRotateEachRealActorTwiceAndKeepAnswersPrivate(GameTestHelper h){
        for(int count=2;count<=4;count++){
            var n=TabletopGames.start(TabletopGames.Game.SILENT_CHARADES,1,roster(count));int[] acted=new int[count];
            for(int round=0;round<count*2;round++){
                int actor=n.getInt("Actor");acted[actor]++;
                h.assertFalse(TabletopGames.spectatorVisible(n).contains("Topic"),"World prop leaked actor secret");
                var visible=TabletopGames.visible(n);h.assertTrue(visible.contains("Topic")== (actor==0),"Charade privacy mismatch");
                int topic=n.getInt("Topic");h.assertTrue(TabletopGames.move(n,actor==0?"clue:"+topic+":"+topic:"guess:"+topic),"Valid charade action failed");n=n.copy();
            }
            for(int times:acted)h.assertTrue(times==2,"Unequal real actor rotation");
            h.assertTrue(TabletopGames.completedNormally(n)&&n.getInt("ActionCount")==count*2,"Charades did not end at full roster rounds");
        }h.succeed();
    }
    @GameTest(template="empty") public static void privateHandsAndNbtCopiesStayIndependent(GameTestHelper h){
        for(int count=2;count<=4;count++){
            var n=TabletopGames.start(TabletopGames.Game.BUTTON_CARDS,7,roster(count));n.putIntArray("Hand9",new int[]{31});
            var world=TabletopGames.spectatorVisible(n);h.assertFalse(world.contains("Hand0")||world.contains("Topic")||world.contains("Hand1"),"World prop exposed a private hand");h.assertTrue(world.getInt("HandCount0")==5,"Owner public count lost");
            var view=TabletopGames.visible(n);h.assertFalse(view.contains("Deck")||view.contains("Seed")||view.contains("Hand9"),"Private state leaked");
            for(int i=1;i<count;i++)h.assertTrue(!view.contains("Hand"+i)&&view.getInt("HandCount"+i)==5,"Opponent hand leaked or count lost");
            view.getIntArray("Hand0")[0]=-1;view.getList("Participants",10).getCompound(1).putString("Name","Changed");
            h.assertTrue(n.getIntArray("Hand0")[0]>=0&&TabletopGames.participantName(n,1).equals("Guest 1"),"Snapshot changed server state");
        }h.succeed();
    }
    @GameTest(template="empty") public static void flickHasExactlyThreeTurnsPerRealParticipant(GameTestHelper h){
        for(int count=2;count<=4;count++){
            var n=TabletopGames.start(TabletopGames.Game.BUTTON_FLICK,3,roster(count));
            for(int tick=0;tick<4200&&!n.getBoolean("Done");tick++){
                if(n.getInt("Physics")==0&&n.getInt("Turn")==0)h.assertTrue(TabletopGames.move(n,"flick:0:.4"),"Owner flick rejected");
                TabletopGames.tick(n);if(tick%97==0)n=n.copy();
            }
            int[] throwsBy=new int[count];for(var tag:n.getList("Pucks",10)){int who=((CompoundTag)tag).getInt("Owner");h.assertTrue(who>=0&&who<count,"Phantom puck owner");throwsBy[who]++;}
            for(int throwsCount:throwsBy)h.assertTrue(throwsCount==3,"Wrong number of flicks per real player");
            h.assertTrue(TabletopGames.completedNormally(n)&&n.getInt("Moves")==count*3,"Flick timed out or ended early");
            h.assertTrue(n.getInt("ActionCount")==count*3,"Reaction count did not match throws");
        }h.succeed();
    }
    @GameTest(template="empty") public static void scoresTieInsteadOfRewardingFirstSeat(GameTestHelper h){
        for(int count=2;count<=4;count++){
            int[] scores=new int[count];Arrays.fill(scores,5);h.assertTrue(TabletopGames.bestScore(scores)==-1,"Tie awarded first player");
            scores[count-1]=6;h.assertTrue(TabletopGames.bestScore(scores)==count-1,"Unique winner lost");
            var n=TabletopGames.start(TabletopGames.Game.BUTTON_FLICK,1,roster(count));n.putInt("Moves",count*3-1);n.putInt("Physics",1);
            TabletopGames.tick(n);h.assertTrue(n.getBoolean("Done")&&n.getInt("Winner")==-1&&TabletopGames.resultText(n).equals("A draw!"),"Zero-score flick tie not a draw");
            for(int who=0;who<count;who++)h.assertFalse(TabletopGames.participantWon(n,who),"Draw gave a win");
            var charades=TabletopGames.start(TabletopGames.Game.SILENT_CHARADES,1,roster(count));charades.putInt("Round",count*2-1);charades.putInt("Actor",1);
            TabletopGames.move(charades,"guess:"+((charades.getInt("Topic")+1)%6));
            h.assertTrue(charades.getInt("Winner")==-1&&TabletopGames.completedNormally(charades),"Charade tie not a draw");
        }h.succeed();
    }
    @GameTest(template="empty") public static void towerRewardsEveryRealSurvivorOnly(GameTestHelper h){
        for(int count=2;count<=4;count++)for(int loser=0;loser<count;loser++){
            var n=TabletopGames.start(TabletopGames.Game.WOBBLY_TOWER,1,roster(count));n.putInt("Turn",loser);n.putDouble("Stability",.001);
            h.assertTrue(TabletopGames.move(n,"pull:8:0:1")&&TabletopGames.completedNormally(n),"Tower did not really collapse");
            for(int who=0;who<count;who++)h.assertTrue(TabletopGames.participantWon(n,who)==(who!=loser),"Tower survivor result wrong");
            h.assertFalse(TabletopGames.participantWon(n,count),"Absent tower survivor won");
            h.assertTrue(TabletopGames.resultText(n).startsWith(TabletopGames.participantName(n,loser)+" toppled"),"Wrong losing participant label");
        }h.succeed();
    }
    @GameTest(template="empty") public static void timeoutAndCancellationNeverRewardOrResume(GameTestHelper h){
        for(var game:TabletopGames.Game.values())for(int count=2;count<=4;count++)for(boolean timeout:new boolean[]{false,true}){
            var n=TabletopGames.start(game,3,roster(count));
            if(timeout){n.putInt("Elapsed",4199);TabletopGames.tick(n);}else{TabletopGames.cancel(n);TabletopGames.cancel(n);}
            h.assertTrue(n.getBoolean("Done")&&n.getInt("Winner")==-1&&!TabletopGames.completedNormally(n),"Aborted game rewarded normal completion");
            var saved=n.copy();int elapsed=n.getInt("Elapsed"),actions=n.getInt("ActionCount");TabletopGames.tick(saved);
            h.assertFalse(TabletopGames.move(saved,TabletopGames.ai(saved)),"Terminal game accepted input");
            h.assertTrue(saved.getInt("Elapsed")==elapsed&&saved.getInt("ActionCount")==actions,"Terminal save resumed");
            for(int who=0;who<count+1;who++)h.assertFalse(TabletopGames.participantWon(saved,who),"Aborted participant won");
            h.assertTrue(saved.getBoolean(timeout?"TimedOut":"Cancelled"),"Abort reason lost across copy");
            if(game==TabletopGames.Game.WOBBLY_TOWER)h.assertFalse(saved.contains("Loser"),"Abort fabricated tower collapse");
        }h.succeed();
    }
    @GameTest(template="empty") public static void historicalFamilySavesKeepTheirRosterAndOrder(GameTestHelper h){
        var n=TabletopGames.start(TabletopGames.Game.THREAD_FOUR,1);n.remove("Participants");n.remove("ParticipantCount");n.remove("FamilyRoster");
        h.assertTrue(TabletopGames.validRoster(n)&&TabletopGames.participantCount(n)==4,"Historical save rejected");
        for(int who:new int[]{0,1,3,2,0}){h.assertTrue(n.getInt("Turn")==who,"Historical family order changed");TabletopGames.advance(n,0);}
        n.putBoolean("Done",true);n.putInt("Winner",0);h.assertTrue(TabletopGames.resultText(n).equals("You and Wybie won!"),"Historical team text changed");
        h.succeed();
    }
    @GameTest(template="empty") public static void allSixSaveCopiesContinueDeterministicallyWithinSafetyBound(GameTestHelper h){
        for(var game:TabletopGames.Game.values())for(int count=2;count<=4;count++)for(int seed=0;seed<12;seed++){
            var original=TabletopGames.start(game,seed,roster(count));var restored=original.copy();
            for(int tick=0;tick<4201&&!original.getBoolean("Done");tick++){
                if(game==TabletopGames.Game.SILENT_CHARADES&&tick%100==0){int topic=original.getInt("Topic");String action=original.getInt("Actor")==0?"clue:"+topic+":"+topic:"guess:"+topic;h.assertTrue(TabletopGames.move(original,action)&&TabletopGames.move(restored,action),"Legal charade failed");}
                else if(game!=TabletopGames.Game.SILENT_CHARADES&&original.getInt("Physics")==0&&original.getInt("Turn")==0&&tick%50==0){String action=TabletopGames.ai(original);h.assertTrue(TabletopGames.move(original,action)&&TabletopGames.move(restored,action),"Legal owner move failed");}
                TabletopGames.tick(original);TabletopGames.tick(restored);
                h.assertTrue(original.equals(restored),"Save copy changed deterministic result for "+game+" seats="+count+" seed="+seed);
                h.assertTrue(original.getInt("Turn")>=0&&original.getInt("Turn")<count,"Turn moved to absent actor");
                if(tick%127==0)restored=restored.copy();
            }
            h.assertTrue(original.getBoolean("Done"),"Game exceeded unchanged safety deadline");
            if(original.getBoolean("TimedOut")){
                h.assertFalse(TabletopGames.completedNormally(original),"Timeout counted as normal play");
                for(int who=0;who<count;who++)h.assertFalse(TabletopGames.participantWon(original,who),"Timeout had a winner");
            }
        }h.succeed();
    }
}
