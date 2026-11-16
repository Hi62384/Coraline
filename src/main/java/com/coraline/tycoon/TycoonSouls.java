package com.coraline.tycoon;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;

/** One original resident -> one canonical soul; no immediate Essence payout. */
public final class TycoonSouls {
    public static CompoundTag collection(TycoonData.Home h){return h.state.getCompound("Souls");}
    public static boolean usable(CompoundTag soul){return soul.getString("Status").equals("CAPTURED");}
    public static org.joml.Vector3f color(UUID identity){int rgb=java.awt.Color.HSBtoRGB(Math.floorMod(identity.hashCode(),360)/360F,.55F,1);return new org.joml.Vector3f(((rgb>>16)&255)/255F,((rgb>>8)&255)/255F,(rgb&255)/255F);}
    public static String clue(TycoonData.Resident r){
        double t=TycoonDialogue.trust(r);if(TycoonDialogue.suspicion(r)>.7)return "I still need to know that the door is safe.";
        if(r.n.getInt("ProposalRejections")>0&&t<.8)return "I haven't forgotten what you asked me about the buttons.";
        // Overlapping ranges make prose useful, not a threshold oracle.
        int variant=Math.floorMod(r.id.hashCode()+r.n.getInt("Conversations"),3);
        if(t>.90&&variant!=0)return r.trait("Socialness")>.6?"I wish we could all stay together here forever.":"Sometimes this place feels more like home.";
        if(t>.70)return r.trait("Explorativeness")>.6?"I always look forward to discovering something here.":"I wish we had somewhere like this at home.";
        return t>.45?"I like visiting. Could we try my favorite place again?":"I'd like to understand this place better first.";
    }
    public static String propose(ServerPlayer p,TycoonData.Resident r,boolean confirm){
        var h=TycoonWorld.home(p);if(!r.active()||!r.n.getCompound("Visit").getString("Phase").equals("INSIDE")||r.n.getBoolean("KnowsSoulFarm"))return "This visitor is not available for a proposal.";
        if(h.clock()<r.n.getLong("ProposalAfter")||r.n.contains("ConversionRitual"))return "They need time before discussing that again.";
        if(!confirm){r.n.putLong("ProposalConfirmUntil",h.clock()+600);TycoonWorld.dirty(p);return "You may stay in this Other World forever, but there is one condition: your eyes would be replaced with buttons. Confirm only if you want to make that offer.";}
        if(r.n.getLong("ProposalConfirmUntil")<=h.clock())return "Explain the condition before confirming your offer.";
        r.n.remove("ProposalConfirmUntil");r.n.putLong("ProposalAfter",h.clock()+TycoonSoulConfig.PROPOSAL_COOLDOWN.get());var memories=r.n.getList("ProposalMemories",10);var memory=new CompoundTag();memory.putLong("At",h.clock());boolean accepted=TycoonDialogue.trust(r)>=1.0;memory.putBoolean("Accepted",accepted);memories.add(memory);while(memories.size()>32)memories.remove(0);r.n.put("ProposalMemories",memories);
        if(!accepted){
            TycoonDialogue.set(r,TycoonDialogue.trust(r)-TycoonSoulConfig.REJECT_TRUST.get(),TycoonDialogue.suspicion(r)+TycoonSoulConfig.REJECT_SUSPICION.get());r.n.putInt("ProposalRejections",r.n.getInt("ProposalRejections")+1);r.n.putBoolean("QuestionUsed",true);r.n.putLong("TalkAfter",h.clock()+1200);TycoonAttractions.cancel(r);var visit=r.n.getCompound("Visit");visit.putString("Phase","LEAVING");visit.putLong("LeavingAt",h.clock());TycoonWorld.dirty(p);return r.trait("EmotionalSensitivity")>.6?"I thought it was safe here! I want to go home.":"Replace my eyes? No. I don't trust this offer. I'm leaving.";
        }
        var ritual=new CompoundTag();ritual.putUUID("SoulId",UUID.nameUUIDFromBytes((h.owner+":"+r.id+":soul").getBytes(java.nio.charset.StandardCharsets.UTF_8)));ritual.putInt("Progress",0);ritual.putLong("Started",h.clock());r.n.put("ConversionRitual",ritual);TycoonAttractions.cancel(r);TycoonWorld.dirty(p);return "I accept the condition. I want to stay. The magical thread begins to turn…";
    }
    public static CompoundTag derive(TycoonData.Home h,TycoonData.Resident r,UUID id){
        var n=new CompoundTag();n.putUUID("Id",id);n.putUUID("Resident",r.id);n.putUUID("Owner",h.owner);n.putString("Name",r.name());n.putInt("Appearance",r.n.getInt("Appearance"));n.put("Personality",TycoonPersonality.exact(r));n.put("History",r.save());n.putString("Status","CAPTURED");n.putLong("CapturedAt",h.clock());
        String[] traits={"Explorativeness","Socialness","Artisticness","Aweness","Playfulness","FoodInterest","Independence","Curiosity"};String[] names={"Explorer","Social","Artistic","Spectacle","Playful","Culinary","Independent","Restless"};var indices=new ArrayList<Integer>();for(int i=0;i<traits.length;i++)indices.add(i);indices.sort(Comparator.<Integer>comparingDouble(i->r.trait(traits[i])).reversed());n.putString("Primary",names[indices.get(0)]);n.putString("Secondary",names[indices.get(1)]);
        double aptitude=r.trait("WorkerAptitude"),art=r.trait("Artisticness"),awe=r.trait("Aweness"),social=r.trait("Socialness"),curiosity=r.trait("Curiosity"),independence=r.trait("Independence");
        double potential=.45*aptitude+.2*awe+.15*independence+.2*r.trait("FoodInterest"),min=TycoonSoulConfig.BASE_MIN.get(),max=Math.max(min,TycoonSoulConfig.BASE_MAX.get());
        n.putLong("BaseMicros",Math.round((min+(max-min)*potential)*TycoonSoulLedger.SCALE));n.putDouble("Stability",unit(.35+.45*art+.2*r.trait("Patience")-.15*awe));n.putDouble("Resonance",unit(.25+.55*social+.2*art));n.putDouble("Efficiency",unit(.3+.4*aptitude+.3*r.trait("FoodInterest")));n.putDouble("Restlessness",unit(.45*curiosity+.35*independence+.2*r.trait("Playfulness")));
        n.putDouble("Quality",unit(.3*potential+.25*n.getDouble("Stability")+.2*n.getDouble("Efficiency")+.15*n.getDouble("Resonance")+.1*(1-n.getDouble("Restlessness"))));return n;
    }
    private static double unit(double value){return Math.max(0,Math.min(1,value));}
    public static boolean actorTick(ServerPlayer p,TycoonData.Resident r,TycoonNpc actor){
        if(!r.n.contains("ConversionRitual")){actor.ritual(0);return false;}var h=TycoonWorld.home(p);var ritual=r.n.getCompound("ConversionRitual");
        if(!r.active()||!TycoonVisits.inside(r)||actor.level().dimension()!=TycoonWorld.KEY){r.n.remove("ConversionRitual");actor.setInvisible(false);TycoonWorld.dirty(p);return false;}
        actor.getNavigation().stop();actor.gesture(2,25);int ticks=ritual.getInt("Progress")+20;ritual.putInt("Progress",ticks);actor.ritual(ticks);r.n.putString("Task","BUTTON_EYE_RITUAL");
        var level=(net.minecraft.server.level.ServerLevel)actor.level();double phase=ticks*.1;
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,actor.getX()+Math.sin(phase)*.45,actor.getY()+.6,actor.getZ()+Math.cos(phase)*.45,8,.08,.6,.08,.01);
        if(ticks>=80)level.sendParticles(new net.minecraft.core.particles.DustParticleOptions(color(ritual.getUUID("SoulId")),1.2F),actor.getX(),actor.getY()+1.25,actor.getZ(),12,.13,.13,.13,0);
        if(ticks>=60){actor.setInvisible(false);level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL,actor.getX(),actor.getY()+1.3,actor.getZ(),8,.2,.25,.2,.02);}
        if(ticks<160){TycoonWorld.dirty(p);return true;}
        UUID id=ritual.getUUID("SoulId");var all=collection(h);if(!all.contains(id.toString()))all.put(id.toString(),derive(h,r,id));h.state.put("Souls",all);
        r.n.putString("Status","CONVERTED");r.n.putUUID("SoulId",id);r.n.remove("Bed");r.n.remove("Activity");r.n.remove("ConversionRitual");r.n.remove("VisitActorSnapshot");r.n.getCompound("Visit").putString("Phase","CONVERTED");r.n.getCompound("Doll").putString("Status","INACTIVE");r.n.putString("Task","CAPTURED_SOUL");r.n.putBoolean("FirstVisitEnded",true);TycoonWorld.dirty(p);p.server.overworld().getDataStorage().save();actor.discard();return true;
    }
    public static String assign(ServerPlayer p,UUID soulId,UUID farmId){
        var h=TycoonWorld.home(p);var soul=collection(h).getCompound(soulId.toString());if(!usable(soul)||!soul.hasUUID("Owner")||!h.owner.equals(soul.getUUID("Owner")))return "Soul unavailable.";
        if(soul.hasUUID("Farm")&&TycoonSoulFarms.breaching(TycoonSoulFarms.farms(h).getCompound(soul.getUUID("Farm").toString())))return "Secure or concede the active breach before moving its souls.";
        var farm=TycoonSoulFarms.farms(h).getCompound(farmId.toString());if(farm.isEmpty()||farm.getBoolean("Damaged")||TycoonSoulFarms.breaching(farm))return "Farm unavailable.";
        if(soul.hasUUID("Farm")&&farmId.equals(soul.getUUID("Farm")))return "Already assigned here.";
        if(TycoonSoulFarms.assigned(h,farmId).size()>=TycoonSoulFarms.capacity(farm))return "Farm capacity is full.";
        soul.putUUID("Farm",farmId);TycoonWorld.dirty(p);return "Soul moved atomically to the selected farm.";
    }
    public static String unassign(ServerPlayer p,UUID id){var h=TycoonWorld.home(p);var soul=collection(h).getCompound(id.toString());if(!usable(soul))return "Soul unavailable.";if(soul.hasUUID("Farm")&&TycoonSoulFarms.breaching(TycoonSoulFarms.farms(h).getCompound(soul.getUUID("Farm").toString())))return "Secure or concede this breach first.";soul.remove("Farm");TycoonWorld.dirty(p);return "Soul returned to the unassigned collection.";}
    public static String release(ServerPlayer p,UUID id,boolean confirm){var h=TycoonWorld.home(p);var soul=collection(h).getCompound(id.toString());if(!usable(soul))return "Soul unavailable.";if(soul.hasUUID("Farm")&&TycoonSoulFarms.breaching(TycoonSoulFarms.farms(h).getCompound(soul.getUUID("Farm").toString())))return "Secure or concede this breach first.";if(!confirm){h.state.putUUID("SoulRelease",id);h.state.putLong("SoulReleaseUntil",h.clock()+600);TycoonWorld.dirty(p);return "Release this soul permanently? No Essence, refund or ordinary resident will be created.";}if(!h.state.hasUUID("SoulRelease")||!id.equals(h.state.getUUID("SoulRelease"))||h.clock()>=h.state.getLong("SoulReleaseUntil"))return "Preview the permanent release first.";soul.remove("Farm");soul.putString("Status","RELEASED");soul.putLong("ReleasedAt",h.clock());h.state.remove("SoulRelease");p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL,p.getX(),p.getY()+1,p.getZ(),24,.5,1,.5,.04);TycoonWorld.dirty(p);return "The soul has been permanently released.";}
    private TycoonSouls(){}
}
