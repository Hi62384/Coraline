package com.coraline.tycoon;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;

/** Local deterministic dialogue, not external AI/audio generation. Ordinary
 * snapshots contain prose and choice IDs, never the hidden relationship meters.
 */
public final class TycoonDialogue {
    private static final Map<String,String[]> LINES=Map.of(
        "empty",new String[]{"Where does the rest of this place go?","It's very quiet with nothing beyond the stone.","Did you make the door before you made the world?","I can see the edge. What keeps us safe?","It feels like a story waiting for its pictures.","Is this little platform really all yours?","I wasn't expecting so much white.","Will there be somewhere to explore next time?"},
        "grass",new String[]{"The hills make this feel more like outside.","The grass is real under my shoes, isn't it?","Did you decide how every hill should look?","I like having a path away from the door.","There must be more beyond those little hills.","We could imagine something for that empty edge."},
        "garden",new String[]{"Those flowers are wonderful. Who planted them?","Can we walk among the trees for a while?","This garden feels carefully looked after.","I noticed another flower I hadn't seen before.","The paths make it easier to find our way back.","Could we come back to see these flowers again?"},
        "candy",new String[]{"That tree looks as though it belongs in a sweet shop!","Is the pink landscape meant to be candy?","How did you imagine colors like these?","I've never seen a place quite like this.","It's a little like walking through a picture book.","Did you build the striped decorations yourself?"},
        "enchanted",new String[]{"The little lights make the plants seem magical.","I keep noticing something glowing between the trees.","It feels like the sort of forest you'd dream about.","Are these strange trees safe to walk beneath?","The colors change how this place feels.","I want to remember the lights when we go home."},
        "house",new String[]{"Did you build that house around the little door?","It helps to have somewhere sheltered to sit.","Can we look around the rooms together?","Will the door still be easy to find from inside?","The windows make it feel less lonely.","This house looks different from home, but familiar."},
        "sky",new String[]{"How does the sky stay like that?","The colors above us are beautiful.","Is that sky something you made too?","It feels strange not to watch the sky change normally.","I could look up at that for a long time.","The clouds make this feel like a larger place."},
        "uneasy",new String[]{"I still have questions about how this place works.","Please tell me honestly: can we leave whenever we need to?","I don't like being told not to ask questions.","I'd feel better if you explained what happens next.","I'm not sure I understand why you brought us here.","Will we be back by our own beds tonight?"}
    );
    public static double trust(TycoonData.Resident r){return unit(r.n.getDouble("Trustness"));}
    public static double suspicion(TycoonData.Resident r){return unit(r.n.getDouble("Suspicion"));}
    private static double unit(double value){return Double.isFinite(value)?Math.max(0,Math.min(1,value)):0;}
    public static boolean initialize(TycoonData.Resident r){if(r.n.getBoolean("RelationshipInitialized"))return false;r.n.putDouble("Trustness",TycoonPhaseTwoConfig.INITIAL_TRUST.get());r.n.putDouble("Suspicion",.5);r.n.putBoolean("RelationshipInitialized",true);return true;}
    public static void set(TycoonData.Resident r,double trust,double suspicion){r.n.putDouble("Trustness",unit(trust));r.n.putDouble("Suspicion",r.n.getBoolean("KnowsSoulFarm")?1:unit(suspicion));}
    private static String context(TycoonData.Home h,TycoonData.Resident r){
        if(suspicion(r)>.75)return "uneasy";var chunks=h.state.getCompound("Terrain");int best=0;for(String key:chunks.getAllKeys())best=Math.max(best,chunks.getCompound(key).getInt("Tier"));
        int memories=r.n.getInt("Conversations");if(h.state.getInt("HouseTier")>0&&memories%4==1)return "house";if(h.state.getInt("SkyTier")>0&&memories%4==2)return "sky";
        return best>=5?"enchanted":best==4?"candy":best>=2?"garden":chunks.isEmpty()?"empty":"grass";
    }
    public static void arrival(ServerPlayer p,TycoonData.Resident r){var h=TycoonWorld.home(p);String prefix=r.trait("Curiosity")>.65?"Where are we? ":r.trait("Sensitivity")>.65?"It's so different here. ":r.trait("Activity")>.6?"Can we explore? ":"So this is your world. ";String ctx=context(h,r);TycoonWorld.message(p,r.name()+": "+prefix+LINES.get(ctx)[Math.floorMod(r.id.hashCode()+r.n.getInt("VisitCount"),LINES.get(ctx).length)]);}
    public static CompoundTag open(ServerPlayer p,TycoonData.Resident r){
        var h=TycoonWorld.home(p);var n=r.n;var result=new CompoundTag();result.putString("VisitorName",r.name());
        if(h.clock()<n.getLong("TalkAfter")){result.putString("Dialogue","Let's look around a little before talking again.");result.put("Choices",new ListTag());return result;}
        if(!n.hasUUID("Question")||n.getBoolean("QuestionUsed")){String ctx=context(h,r);var variants=LINES.get(ctx);int index=Math.floorMod(r.id.hashCode()+n.getInt("Conversations")*3+n.getInt("VisitCount"),variants.length);n.putUUID("Question",UUID.randomUUID());n.putBoolean("QuestionUsed",false);n.putString("QuestionContext",ctx);String tone=r.trait("Curiosity")>.7?"I've been wondering: ":r.trait("Sensitivity")>.7?"Can I ask something? ":"";n.putString("QuestionLine",tone+variants[index]);}
        result.putUUID("Question",n.getUUID("Question"));result.putString("Dialogue",TycoonSouls.clue(r)+" "+n.getString("QuestionLine"));var choices=new ListTag();String[] labels={"I'm still building it. I'll explain what I know.","Let's enjoy it together, and keep the door in sight.","You're not supposed to ask questions about that."};for(int i=0;i<labels.length;i++){var c=new CompoundTag();c.putInt("Id",i);c.putString("Text",labels[i]);choices.add(c);}result.put("Choices",choices);TycoonWorld.dirty(p);return result;
    }
    public static String respond(ServerPlayer p,TycoonData.Resident r,UUID question,int choice){
        var h=TycoonWorld.home(p);var n=r.n;if(!TycoonVisits.inside(r)||!n.hasUUID("Question")||!n.getUUID("Question").equals(question)||n.getBoolean("QuestionUsed")||h.clock()<n.getLong("TalkAfter")||choice<0||choice>2)return "That conversation has already ended.";
        n.putBoolean("QuestionUsed",true);n.putLong("TalkAfter",h.clock()+200);double t=0,s=0;
        if(choice==0){t=.018+.014*r.trait("Curiosity");s=-.012-.012*r.trait("Suspiciousness");}
        if(choice==1){t=.008+.02*r.trait("Trustingness");s=.008*r.trait("Suspiciousness")-.006*r.trait("Trustingness");}
        if(choice==2){t=-.025-.025*r.trait("Sensitivity");s=.035+.025*r.trait("Sensitivity");}
        set(r,trust(r)+t,suspicion(r)+s);n.putInt("Conversations",n.getInt("Conversations")+1);var history=n.getList("DialogueHistory",10);var entry=new CompoundTag();entry.putString("Context",n.getString("QuestionContext"));entry.putInt("Choice",choice);entry.putLong("At",h.clock());history.add(entry);while(history.size()>40)history.remove(0);n.put("DialogueHistory",history);
        if(n.hasUUID("Entity")&&p.serverLevel().getEntity(n.getUUID("Entity")) instanceof TycoonNpc a){a.getNavigation().stop();a.getLookControl().setLookAt(p,30,30);a.gesture(choice==2?5:2,60);}
        TycoonDollAnalysis.observe(r,"talk:"+question,"A conversation about "+n.getString("QuestionContext"),100,h.clock());
        TycoonWorld.dirty(p);return choice==0?"Thank you for telling me honestly.":choice==1?(r.trait("Suspiciousness")>.6?"All right, but I still want to understand.":"I'd like that. Let's stay together."):"I only wanted to understand...";
    }
    public static void passive(ServerPlayer p,TycoonData.Resident r,TycoonNpc actor){
        var h=TycoonWorld.home(p);var n=r.n.getCompound("Visit");double quality=TycoonLandscape.goodness(h).total()/100.;double preference=.8+r.trait("Curiosity")*.15+r.trait("Activity")*.1;double trustDelta=20*(quality-.18)*.000012*preference,suspicionDelta=20*(.3-quality)*.00001*(.7+r.trait("Suspiciousness")*.5);set(r,trust(r)+trustDelta,suspicion(r)+suspicionDelta);
        if(h.clock()<h.state.getLong("SocialAfter")||h.clock()-n.getLong("EnteredAt")<200)return;
        var friends=actor.level().getEntitiesOfClass(TycoonNpc.class,actor.getBoundingBox().inflate(5),a->a!=actor&&!a.staff()&&TycoonWorld.owner(p).equals(a.owner)&&TycoonCare.resident(a)!=null&&TycoonVisits.inside(TycoonCare.resident(a)));
        if(!friends.isEmpty()&&r.trait("Sociability")>.4){var friend=friends.get(0);var other=TycoonCare.resident(friend);actor.getNavigation().stop();friend.getNavigation().stop();actor.getLookControl().setLookAt(friend,30,30);friend.getLookControl().setLookAt(actor,30,30);actor.gesture(2,50);friend.gesture(2,50);String ctx=context(h,r);TycoonWorld.message(p,r.name()+": "+LINES.get(ctx)[Math.floorMod(r.id.hashCode()+(int)(h.clock()/600),LINES.get(ctx).length)]);TycoonWorld.message(p,other.name()+": "+(suspicion(other)>.6?"I like looking around, but I want to know more.":quality>.25?"We can explore a little together.":"Let's keep the little door where we can see it."));double influence=(trust(r)-suspicion(r))*.002;set(other,trust(other)+Math.max(0,influence),suspicion(other)-influence);h.state.putLong("SocialAfter",h.clock()+600);}
        else if(h.clock()>=n.getLong("RemarkAfter")){String ctx=context(h,r);TycoonWorld.message(p,r.name()+": "+LINES.get(ctx)[Math.floorMod((int)(h.clock()/1200)+r.id.hashCode(),LINES.get(ctx).length)]);n.putLong("RemarkAfter",h.clock()+1200);h.state.putLong("SocialAfter",h.clock()+400);actor.gesture(2,35);}
    }
    private TycoonDialogue(){}
}
