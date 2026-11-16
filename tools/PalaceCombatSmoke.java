import com.coraline.palace.PalaceHandCombat;
import java.util.UUID;

public final class PalaceCombatSmoke {
    private static int checks;
    private static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
    public static int run(){
        for(int run=0;run<24;run++){
            var owner=UUID.randomUUID();var door=UUID.randomUUID();var hand=UUID.randomUUID();var token=UUID.randomUUID();var foreign=UUID.randomUUID();
            var fight=PalaceHandCombat.start(owner,door,hand,token);int strikes=0;
            for(int t=0;t<9000&&!fight.complete();t++){
                check(!fight.tick(foreign,token,t,true)&&!fight.tick(owner,token,t,false),"Unauthorized/inactive combat advanced");
                check(fight.tick(owner,token,t,true)&&!fight.tick(owner,token,t,true),"Combat tick duplicated");
                check(!fight.hit(owner,token,foreign,t,true,true,true,true)&&!fight.hit(owner,token,hand,t,false,true,true,true),"Foreign/projectile combat strike");
                check(!fight.hit(owner,token,hand,t,true,false,true,true)&&!fight.hit(owner,token,hand,t,true,true,false,true)&&!fight.hit(owner,token,hand,t,true,true,true,false),"Unreachable/occluded/spammed strike");
                if(fight.beat()==PalaceHandCombat.Beat.RECOVERY&&fight.hit(owner,token,hand,t,true,true,true,true)){
                    strikes++;check(!fight.hit(owner,token,hand,t,true,true,true,true),"More than one hit per recovery");
                }
                if(t%113==0){var before=fight.save();fight=PalaceHandCombat.load(owner,door,hand,before);check(fight.blocker().isEmpty()&&fight.save().equals(before),"Reload changed combat cursor");}
            }
            check(fight.complete()&&fight.phase()==3&&strikes==36,"Three physical-hit phases missing");
            check(fight.totalTicks()>=3600&&fight.totalTicks()<=6000,"Ideal fight outside 3-5 minute target");
            check(!fight.resume(owner,foreign)&&!fight.tick(owner,token,99999,true),"Completed combat restarted");
            var bad=fight.save();bad.putInt("Hits",12);bad.putString("Evidence","keep");var blocked=PalaceHandCombat.load(owner,door,hand,bad);
            check(!blocked.blocker().isEmpty()&&blocked.save().equals(bad),"Corrupt combat evidence overwritten");
            var paused=PalaceHandCombat.start(owner,door,hand,token);paused.tick(owner,token,5,true);int before=paused.totalTicks();
            check(paused.resume(owner,foreign)&&!paused.tick(owner,token,500000,true)&&paused.tick(owner,foreign,500000,true)&&paused.totalTicks()==before+1,"Reconnect caught up offline time or accepted stale token");
        }
        var owner=UUID.randomUUID();var door=UUID.randomUUID();var hand=UUID.randomUUID();var token=UUID.randomUUID();var idle=PalaceHandCombat.start(owner,door,hand,token);
        for(int t=0;t<10000;t++)idle.tick(owner,token,t,true);
        check(!idle.complete()&&idle.phase()==1&&idle.hits()==12,"Timer alone won the boss");
        var order=PalaceHandCombat.start(owner,door,hand,token);
        check(!order.hit(owner,token,hand,0,true,true,true,true),"Input before first active tick won a strike");
        for(int t=0;t<43;t++)order.tick(owner,token,t,true);
        var before=PalaceHandCombat.load(owner,door,hand,order.save());var after=PalaceHandCombat.load(owner,door,hand,order.save());
        check(before.hit(owner,token,hand,43,true,true,true,true)&&before.tick(owner,token,43,true),"Before-tick input at recovery start rejected");
        check(after.tick(owner,token,43,true)&&after.hit(owner,token,hand,43,true,true,true,true),"After-tick input at recovery start rejected");
        check(before.save().equals(after.save()),"Packet/tick order changed legitimate hit outcome");
        check(!after.hit(owner,token,hand,44,true,true,true,true),"Next packet repeated the same recovery hit");return checks;
    }
}
