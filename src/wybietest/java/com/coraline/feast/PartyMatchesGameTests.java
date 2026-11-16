package com.coraline.feast;

import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("coraline") @PrefixGameTestTemplate(false)
public final class PartyMatchesGameTests {
    static List<CompoundTag> guests(int count,int wybie){
        var result=new ArrayList<CompoundTag>();for(int i=0;i<count;i++){var g=new CompoundTag();g.putUUID("Id",UUID.nameUUIDFromBytes(("guest-"+i).getBytes(java.nio.charset.StandardCharsets.UTF_8)));g.putString("Name",i==wybie?"Other Wybie":"Guest "+i);g.putString("Kind",i==wybie?"other_wybie":"resident");result.add(g);}return result;
    }
    @GameTest(template="empty") public static void partyTeamsKeepOwnerAndActualWybieTogether(GameTestHelper h){
        for(int count=1;count<=3;count++)for(int wybie=-1;wybie<count;wybie++){
            var list=guests(count,wybie);var roster=PartyMatches.participants(UUID.randomUUID(),list,TabletopGames.Game.THREAD_FOUR);
            var rules=TabletopGames.start(TabletopGames.Game.THREAD_FOUR,1,roster);
            h.assertTrue(TabletopGames.participantCount(rules)==count+1,"Party fabricated a participant");
            for(int i=0;i<count;i++)h.assertTrue(roster.get(i+1).id().equals(list.get(i).getUUID("Id").toString()),"Party changed actual guest identity");
            if(count>1&&wybie>=0)h.assertTrue(TabletopGames.participantTeam(rules,wybie+1)==0,"Owner and actual Wybie were split");
            if(count==1)h.assertTrue(TabletopGames.participantTeam(rules,1)==1,"Duel has no opposing team");
        }h.succeed();
    }
    @GameTest(template="empty") public static void settlementsUseActualTeamsSurvivorsDrawsAndAbortFlags(GameTestHelper h){
        for(int count=2;count<=4;count++){
            var thread=TabletopGames.start(TabletopGames.Game.THREAD_FOUR,1,PartyMatches.participants(UUID.randomUUID(),guests(count-1,0),TabletopGames.Game.THREAD_FOUR));
            thread.putBoolean("Done",true);thread.putInt("Winner",0);h.assertTrue(PartyMatches.outcome(thread)==PartyEscrow.Outcome.WIN,"Owner team did not win");
            thread.putInt("Winner",1);h.assertTrue(PartyMatches.outcome(thread)==PartyEscrow.Outcome.LOSS,"Opponent team not a loss");
            thread.putInt("Winner",-1);h.assertTrue(PartyMatches.outcome(thread)==PartyEscrow.Outcome.DRAW,"Draw would forfeit stake");
            for(int loser=0;loser<count;loser++){
                var tower=TabletopGames.start(TabletopGames.Game.WOBBLY_TOWER,1,PartyMatches.participants(UUID.randomUUID(),guests(count-1,-1),TabletopGames.Game.WOBBLY_TOWER));
                tower.putBoolean("Done",true);tower.putInt("Loser",loser);tower.putInt("Winner",(loser+1)%count);
                h.assertTrue(PartyMatches.outcome(tower)==(loser==0?PartyEscrow.Outcome.LOSS:PartyEscrow.Outcome.WIN),"Tower settlement used representative winner rather than survivors");
                tower.putBoolean("TimedOut",true);h.assertTrue(PartyMatches.outcome(tower)==PartyEscrow.Outcome.CANCEL,"Timeout could lose stake or gain prize");
                tower.remove("TimedOut");tower.putBoolean("Cancelled",true);h.assertTrue(PartyMatches.outcome(tower)==PartyEscrow.Outcome.CANCEL,"Cancellation could lose stake or gain prize");
            }
        }h.succeed();
    }
    @GameTest(template="empty") public static void fixedSeatsAreDistinctAndClearOfTableFootprint(GameTestHelper h){
        for(boolean longTable:new boolean[]{false,true}){
            var center=new Vec3(0,64,0);
            for(int i=0;i<4;i++){
                var at=PartyMatches.seatPosition(center,longTable,i);
                h.assertTrue(Math.abs(at.x)>=2.35||Math.abs(at.z)>=(longTable?5.05:2.35),"Chair intrudes into table bounds");
                for(int j=0;j<i;j++)h.assertTrue(at.distanceToSqr(PartyMatches.seatPosition(center,longTable,j))>4,"Party seats overlap");
            }
        }h.succeed();
    }
}
