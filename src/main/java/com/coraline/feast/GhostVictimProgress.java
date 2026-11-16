package com.coraline.feast;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/** Owner-local journal for the next-victim continuation. The runtime adapter
 * supplies actual sight/navigation observations; this model never teleports an
 * actor, restores escrow, changes tycoon relationships or rewards randomness. */
public final class GhostVictimProgress {
    public static final String KEY="NextVictim";
    public enum Phase { ENTER_MIRROR, EMPTY_WORLD, RECONSTRUCTION, WAITING_FOR_VICTIM,
        INTRODUCTION, FIRST_SUPPER, EXPLORATION, BEDTIME, NIGHT_PREPARATION,
        DAILY_ARRIVAL, DAILY_GIFT, ACTIVITY, BETWEEN_ACTIVITIES,
        VICTIM_CAPTURED, VICTIM_ESCAPED, NEW_VICTIM_RESET }
    public enum Disturbance { LIGHT, OBJECT, FOOD, KNIFE, ILLUSION, MIRROR_GLIMPSE, GIFT_SECRET, GIFT_FRIGHT }
    public enum Dialogue { FRIENDLY, TRAPPED, HELP, EVIDENCE, ACCUSATION, THREAT, LEAVE }
    public record Tuning(double regeneration, double mirrorEntry, double mirrorDrain,
                         double attentionDecay, double punishmentTrustLoss,
                         double punishmentMotherGain, int punishmentTicks,
                         double lightCost,double objectCost,double fallingCost,double illusionCost,int pointCooldownTicks) {
        public static Tuning defaults(){return new Tuning(.025,.10,.08,.005,.10,.05,1200,.08,.12,.15,.25,600);}
    }
    public record Observation(boolean childSaw, boolean motherSaw, boolean childBlamesGhost,
                              boolean previousEvidence, boolean reportsGhost) {}
    public record Effect(boolean performed, boolean investigate, boolean reported, String reaction) {
        static Effect refused(String reason){return new Effect(false,false,false,reason);}
    }

    public static boolean begin(CompoundTag n,String route){
        if(n.getBoolean("Started"))return false;
        n.putInt("Schema",1);n.putBoolean("Started",true);n.putString("EntryRoute",route);
        n.putDouble("Energy",1);n.putDouble("Attention",0);n.putInt("Day",1);
        n.putLong("ActiveTicks",0);move(n,Phase.ENTER_MIRROR);return true;
    }
    public static Phase phase(CompoundTag n){
        try{return Phase.valueOf(n.getString("Phase"));}catch(IllegalArgumentException invalid){return Phase.ENTER_MIRROR;}
    }
    public static long clock(CompoundTag n){return Math.max(0,n.getLong("ActiveTicks"));}
    public static long age(CompoundTag n){return Math.max(0,clock(n)-n.getLong("PhaseStarted"));}
    public static void move(CompoundTag n,Phase phase){
        n.putString("Phase",phase.name());n.putLong("PhaseStarted",clock(n));
        n.remove("VoiceStarted");n.remove("TravelTarget");n.remove("TravelStarted");
        n.remove("ArrivalConfirmed");
        if(phase==Phase.BEDTIME)for(String key:new String[]{"BedtimeArrived","BedtimeWindowStarted","MotherBedtimeExit","FatherBedtimeExit"})n.remove(key);
    }
    public static boolean leavePrison(CompoundTag n){
        if(phase(n)!=Phase.ENTER_MIRROR||!n.getBoolean("Started"))return false;
        n.putBoolean("OutsideMirror",true);move(n,Phase.EMPTY_WORLD);return true;
    }
    public static CompoundTag child(CompoundTag n){return n.getCompound("Victim");}
    public static boolean hasVictim(CompoundTag n){return child(n).hasUUID("Id");}
    public static boolean newVictim(CompoundTag n,UUID id,CompoundTag identity){
        if(hasVictim(n)||id==null||identity==null)return false;
        clearRetiredRoutine(n);
        var c=identity.copy();c.putUUID("Id",id);c.putDouble("MotherTrust",.5);
        c.putDouble("GhostTrust",0);c.put("Memories",new ListTag());
        c.put("UsedStatements",new CompoundTag());c.putInt("EvidenceVersion",0);
        c.putString("Outcome","ACTIVE");n.put("Victim",c);
        n.putInt("VictimsEncountered",n.getInt("VictimsEncountered")+1);
        n.putInt("Day",1);n.remove("PointCooldowns");n.remove("PointUses");n.remove("DayPlans");n.remove("GiftUsed");return true;
    }
    public static double unit(double v){return Double.isFinite(v)?Math.max(0,Math.min(1,v)):0;}
    public static double energy(CompoundTag n){return unit(n.getDouble("Energy"));}
    public static double attention(CompoundTag n){return unit(n.getDouble("Attention"));}
    public static double motherTrust(CompoundTag n){return unit(child(n).getDouble("MotherTrust"));}
    public static double ghostTrust(CompoundTag n){return unit(child(n).getDouble("GhostTrust"));}
    public static void trust(CompoundTag n,double mother,double ghost){
        if(!hasVictim(n))return;var c=child(n);double m=unit(mother),g=unit(ghost);
        c.putDouble("MotherTrust",m);c.putDouble("GhostTrust",m>=.60?Math.min(.99,g):g);
        // Deliberately no rescue/escape/body/inventory transition at g==1.
    }
    public static boolean imprisoned(CompoundTag n){return clock(n)<n.getLong("PunishedUntil");}
    public static boolean suppressed(CompoundTag n){return clock(n)<n.getLong("SuppressedUntil");}
    public static boolean mirror(CompoundTag n){return n.getBoolean("InsideMirror");}
    public static boolean spend(CompoundTag n,double cost){
        if(!Double.isFinite(cost)||cost<0||energy(n)+1E-9<cost)return false;
        n.putDouble("Energy",Math.max(0,energy(n)-cost));n.putLong("LastManifestation",clock(n));return true;
    }
    public static boolean enterMirror(CompoundTag n,String mirror,Tuning tuning){
        if(!n.getBoolean("OutsideMirror")||imprisoned(n)||suppressed(n)||mirror(n)||mirror==null||mirror.isBlank()||!spend(n,tuning.mirrorEntry()))return false;
        n.putBoolean("InsideMirror",true);n.putString("Mirror",mirror);
        n.putUUID("Conversation",UUID.randomUUID());n.putLong("ConversationAfter",clock(n));return true;
    }
    public static void leaveMirror(CompoundTag n){
        n.remove("ConversationPlayer");n.remove("InsideMirror");n.remove("Mirror");n.remove("Conversation");n.remove("ConversationAfter");
    }
    /** Called once per authoritative, online owner tick, even during punishment.
     * Active ticks survive restart; offline absence never silently consumes an
     * entire victim or skips unseen ceremonies. */
    public static boolean tick(CompoundTag n,Tuning tuning){
        if(!n.getBoolean("Started"))return false;
        n.putLong("ActiveTicks",clock(n)+1);boolean newlyPunished=false;
        if(GhostEscapeProgress.bodyRestored(n)){
            leaveMirror(n);for(String key:new String[]{"PunishedUntil","PunishmentActive","SuppressedUntil","ReturningFromPunishment"})n.remove(key);
            return false; // Farewell speech keeps its clock; ghost punishment has ended.
        }
        var escape=n.getCompound(GhostEscapeProgress.KEY);boolean hunt=GhostEscapeProgress.owns(n,escape)&&GhostEscapeProgress.hunting(escape);
        if(hunt&&imprisoned(n)){n.putLong("SuppressedUntil",Math.max(n.getLong("SuppressedUntil"),n.getLong("PunishedUntil")));n.remove("PunishedUntil");n.remove("PunishmentActive");}
        if(n.getBoolean("PunishmentActive")&&!imprisoned(n)){
            n.remove("PunishmentActive");n.putDouble("Attention",.15);n.remove("PunishedUntil");
        }
        if(hunt&&attention(n)>=1){
            leaveMirror(n);n.putLong("SuppressedUntil",clock(n)+Math.max(20,tuning.punishmentTicks()));n.putDouble("Energy",0);n.putDouble("Attention",.15);n.putInt("Suppressions",n.getInt("Suppressions")+1);
        }else if(!imprisoned(n)&&attention(n)>=1){
            leaveMirror(n);n.putLong("PunishedUntil",clock(n)+Math.max(20,tuning.punishmentTicks()));
            n.putBoolean("PunishmentActive",true);n.putDouble("Energy",0);
            trust(n,motherTrust(n)+tuning.punishmentMotherGain(),ghostTrust(n)-tuning.punishmentTrustLoss());
            n.putInt("Punishments",n.getInt("Punishments")+1);newlyPunished=true;
        }
        if(imprisoned(n)){n.putDouble("Energy",0);return newlyPunished;}
        if(suppressed(n)){n.putDouble("Energy",0);n.putDouble("Attention",unit(attention(n)-Math.max(0,tuning.attentionDecay())/20));return false;}else n.remove("SuppressedUntil");
        if(mirror(n)){
            n.putDouble("Energy",Math.max(0,energy(n)-Math.max(0,tuning.mirrorDrain())/20));
            if(energy(n)<=0)leaveMirror(n);
        }else if(clock(n)>=n.getLong("ManifestingUntil")&&clock(n)-n.getLong("LastManifestation")>=20){
            n.putDouble("Energy",unit(energy(n)+Math.max(0,tuning.regeneration())/20));
        }
        n.putDouble("Attention",unit(attention(n)-Math.max(0,tuning.attentionDecay())/20));
        return newlyPunished;
    }
    public static boolean remember(CompoundTag n,String memory){
        if(!hasVictim(n)||memory==null||memory.isBlank())return false;
        var c=child(n);var memories=c.getList("Memories",Tag.TAG_STRING);
        for(Tag old:memories)if(old.getAsString().equals(memory))return false;
        memories.add(StringTag.valueOf(memory));c.put("Memories",memories);
        c.putInt("MemoryVersion",c.getInt("MemoryVersion")+1);return true;
    }
    public static boolean remembers(CompoundTag n,String memory){
        for(Tag old:child(n).getList("Memories",Tag.TAG_STRING))if(old.getAsString().equals(memory))return true;
        return false;
    }
    public static double cost(Disturbance kind,Tuning tuning){return switch(kind){
        case LIGHT->tuning.lightCost();case OBJECT,MIRROR_GLIMPSE->tuning.objectCost();
        case KNIFE,GIFT_FRIGHT->tuning.fallingCost();case FOOD,ILLUSION,GIFT_SECRET->tuning.illusionCost();
    };}
    public static Effect disturb(CompoundTag n,String point,Disturbance kind,Observation seen,boolean available){
        return disturb(n,point,kind,seen,available,Tuning.defaults());
    }
    public static Effect disturb(CompoundTag n,String point,Disturbance kind,Observation seen,boolean available,Tuning tuning){
        if(!available||point==null||point.isBlank()||point.length()>100||kind==null||seen==null||!hasVictim(n)||imprisoned(n)||suppressed(n)||mirror(n))return Effect.refused("Unavailable");
        String key=n.getInt("Day")+":"+point;var cooldowns=n.getCompound("PointCooldowns");var uses=n.getCompound("PointUses");
        if(clock(n)<cooldowns.getLong(key)||uses.getInt(key)>=2)return Effect.refused("Let the room settle.");
        if(!spend(n,cost(kind,tuning)))return Effect.refused("Not enough manifestation energy.");
        cooldowns.putLong(key,clock(n)+Math.max(20,tuning.pointCooldownTicks()));uses.putInt(key,uses.getInt(key)+1);
        n.put("PointCooldowns",cooldowns);n.put("PointUses",uses);
        boolean frightening=kind==Disturbance.KNIFE||kind==Disturbance.GIFT_FRIGHT;
        boolean evidence=kind==Disturbance.FOOD||kind==Disturbance.ILLUSION||kind==Disturbance.GIFT_SECRET;
        boolean fresh=seen.childSaw()&&remember(n,point+":"+kind.name());
        if(fresh&&evidence){var c=child(n);c.putInt("EvidenceVersion",c.getInt("EvidenceVersion")+1);c.putInt("EvidenceCount",c.getInt("EvidenceCount")+1);}
        double m=motherTrust(n),g=ghostTrust(n);String reaction="A quiet disturbance.";
        if(seen.childSaw()){
            reaction=frightening?"What was that? Please make it stop!":evidence?"That wasn't what it looked like before...":"Did you see that move?";
            if(fresh){
                if(evidence)m-=seen.motherSaw()?.025:.055;
                else if(frightening){if(seen.childBlamesGhost()){g-=.07;m+=.025;}else m-=.025;}
                else if(seen.previousEvidence())m-=.012;
            }
        }
        trust(n,m,g);
        boolean reported=seen.childSaw()&&seen.reportsGhost();
        n.putDouble("Attention",unit(attention(n)+(seen.motherSaw()?(frightening?.20:.10):0)+(reported?.12:0)));
        return new Effect(true,seen.childSaw()&&!frightening,reported,reaction);
    }
    public static boolean dialogue(CompoundTag n,UUID conversation,Dialogue choice,boolean childAtMirror){
        if(!mirror(n)||imprisoned(n)||!childAtMirror||choice==null||!n.hasUUID("Conversation")||!n.getUUID("Conversation").equals(conversation)||clock(n)<n.getLong("ConversationAfter"))return false;
        if(choice==Dialogue.LEAVE){leaveMirror(n);return true;}
        remember(n,"met_ghost");
        var c=child(n);String key=choice.name()+":"+c.getInt("EvidenceVersion");var used=c.getCompound("UsedStatements");
        if(used.getBoolean(key))return false;
        used.putBoolean(key,true);c.put("UsedStatements",used);n.putLong("ConversationAfter",clock(n)+60);
        double m=motherTrust(n),g=ghostTrust(n);boolean evidence=c.getInt("EvidenceCount")>0;
        switch(choice){
            case FRIENDLY,HELP->g+=.06;
            case TRAPPED->g+=evidence?.10:.025;
            case EVIDENCE->{if(evidence){g+=.12*(1-m*.75);m-=.025;}else g-=.015;}
            case ACCUSATION->{if(evidence){g+=.08*(1-m*.85);m-=.015;}else g-=.03*(.5+m);}
            case THREAT->{g-=.08;m+=.025;remember(n,"ghost_threat");c.putBoolean("PendingGhostReport",true);}
            default->{}
        }
        trust(n,m,g);return true;
    }
    public static boolean archiveVictim(CompoundTag n,String outcome){
        if(!hasVictim(n)||!java.util.Set.of("CAPTURED","ESCAPED").contains(outcome))return false;
        var c=child(n);if(!c.getString("Outcome").equals("ACTIVE"))return false;
        c.putString("Outcome",outcome);if(n.hasUUID("VictimActor"))c.putUUID("RetiredActor",n.getUUID("VictimActor"));
        if(n.getCompound(GhostEscapeProgress.KEY).hasUUID("Attempt")){c.put(GhostEscapeProgress.KEY,n.getCompound(GhostEscapeProgress.KEY).copy());n.remove(GhostEscapeProgress.KEY);}
        var history=n.getList("VictimHistory",Tag.TAG_COMPOUND);
        history.add(c.copy());n.put("VictimHistory",history);n.remove("Victim");leaveMirror(n);
        for(String key:new String[]{"VictimActor","ActorPosition","ActorDimension","ChildRoutine","Investigation","InvestigateUntil","ChildWords","ChildTalkingUntil","VoiceStarted","SupperSeated","BedtimeArrived","BedtimeWindowStarted","MotherBedtimeExit","FatherBedtimeExit","ChildAtHome","CrawlExit","GiftPlaced","GiftPresentedAt","CaptureSeatedAt","MotherInvestigation","MotherInvestigateUntil","MotherInvestigationRoute"})n.remove(key);
        // These routes belong to the retired child's routine. Parent outing
        // transfer journals remain until the existing actors return home.
        clearRetiredRoutine(n);
        String count=outcome.equals("CAPTURED")?"CapturedCount":"EscapedCount";
        n.putInt(count,n.getInt(count)+1);move(n,Phase.NEW_VICTIM_RESET);return true;
    }
    private static void clearRetiredRoutine(CompoundTag n){for(String key:new String[]{"MotherArrivalRoute","MotherSupperSeatRoute","FatherSupperSeatRoute","MotherRoute","FatherRoute","ReactionLook","ReactionLookUntil"})n.remove(key);}
    private GhostVictimProgress(){}
}
