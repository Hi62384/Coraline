package com.coraline.tycoon;

import java.util.UUID;
import com.coraline.feast.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/** A permanent agency listing, separate from the resettable story journal.
 * Registration does not adopt, waive a fee or grant a continuing care exemption. */
public final class GhostRescueCandidates {
    public static boolean register(ServerPlayer p,VisitData.Scene scene){
        if(scene==null||!p.getUUID().equals(scene.owner)||!GhostVictimDirector.active(scene))return false;
        var ghost=GhostVictimEntry.data(scene);var escape=ghost.getCompound(GhostEscapeProgress.KEY);
        if(!GhostEscapeProgress.owns(ghost,escape)||!escape.getBoolean("RestorationCommitted")||!escape.getBoolean("WagerWon")
            ||GhostEscapeProgress.phase(escape)!=GhostEscapeProgress.Phase.FAREWELL||!escape.getBoolean("ChildDepartureWitnessed"))return false;
        var data=TycoonData.get(p.server);var home=data.home(p.getUUID());
        TycoonAgency.candidates(home); // Preserve the ordinary agency pool too.
        boolean added=registerCandidate(home,GhostVictimProgress.child(ghost),escape.getUUID("Victim"),escape.getUUID("Attempt"),GhostVictimConfig.RESCUED_NEGLECT.get());
        if(added)data.setDirty();String key=escape.getUUID("Victim").toString();var receipts=home.state.getCompound("GhostRescueCandidates");
        if(!receipts.hasUUID(key)||!receipts.getUUID(key).equals(escape.getUUID("Attempt")))return false;
        var child=GhostVictimActors.find(p,scene,ghost);if(child==null)return false;
        if(!bindOriginalActorReceipt(home,escape.getUUID("Victim"),escape.getUUID("Attempt"),child.getUUID(),child.level().dimension().location().toString(),child.blockPosition()))return false;
        child.getPersistentData().putUUID("GhostRescueCandidateOwner",p.getUUID());child.getPersistentData().putUUID("GhostRescueCandidateAttempt",escape.getUUID("Attempt"));
        child.getNavigation().stop();child.setInvisible(true);child.setNoAi(true);child.setNoGravity(true);child.noPhysics=true;child.setInvulnerable(true);child.setPersistenceRequired();data.setDirty();
        try{
            if(!escape.getBoolean("CandidateActorSaved")){p.server.saveEverything(true,true,true);escape.putBoolean("CandidateActorSaved",true);}
            if(!TycoonCheckpoints.home(p))return false;
            escape.putBoolean("AgencyRegistrationSaved",true);OtherMotherLoop.changed(p);return true;
        }catch(java.io.IOException failure){escape.putString("Blocker","The rescued child's agency listing is waiting for its durable checkpoint.");return false;}
    }
    public static boolean bindOriginalActorReceipt(TycoonData.Home home,UUID child,UUID attempt,UUID actor,String dimension,net.minecraft.core.BlockPos position){
        if(home==null||child==null||attempt==null||actor==null||position==null||dimension==null||net.minecraft.resources.ResourceLocation.tryParse(dimension)==null)return false;
        String key=child.toString();var receipts=home.state.getCompound("GhostRescueCandidates");var candidate=home.state.getCompound("Candidates").getCompound(key);
        if(!receipts.hasUUID(key)||!attempt.equals(receipts.getUUID(key))||!candidate.hasUUID("Id")||!child.equals(candidate.getUUID("Id"))
            ||!candidate.getBoolean("OriginalGhostHelper")||!candidate.hasUUID("GhostRescueAttempt")||!attempt.equals(candidate.getUUID("GhostRescueAttempt"))
            ||candidate.hasUUID("GhostHelperActor")&&!actor.equals(candidate.getUUID("GhostHelperActor")))return false;
        candidate.putUUID("GhostHelperActor",actor);candidate.putString("GhostHelperDimension",dimension);candidate.putLong("GhostHelperPosition",position.asLong());return true;
    }
    /** Lookup the recorded original only. Unloaded/missing helpers are never a
     * request to instantiate another NPC from their saved appearance. */
    static TycoonNpc placementActor(ServerPlayer p,TycoonData.Resident candidate){
        var n=candidate.n;if(!n.getBoolean("OriginalGhostHelper")||!n.hasUUID("GhostRescueAttempt"))return null;
        if(!n.hasUUID("GhostHelperActor")&&!recoverLegacyReference(p,candidate))return null;
        var level=com.coraline.parents.FamilyData.level(p.server,n.getString("GhostHelperDimension"));if(level==null)return null;
        level.getChunkAt(net.minecraft.core.BlockPos.of(n.getLong("GhostHelperPosition")));
        for(var world:p.server.getAllLevels())if(world.getEntity(n.getUUID("GhostHelperActor")) instanceof TycoonNpc actor){
            var tag=actor.getPersistentData();
            if(!TycoonWorld.owner(p).equals(actor.owner)||!candidate.id.equals(actor.resident))return null;
            if(tag.hasUUID("ShowTravelOwner")&&!TycoonWorld.owner(p).equals(tag.getUUID("ShowTravelOwner")))return null;
            if(tag.hasUUID("GhostRescueCandidateOwner")&&!TycoonWorld.owner(p).equals(tag.getUUID("GhostRescueCandidateOwner"))||tag.hasUUID("GhostRescueCandidateAttempt")&&!n.getUUID("GhostRescueAttempt").equals(tag.getUUID("GhostRescueCandidateAttempt")))return null;
            if((!tag.hasUUID("GhostRescueCandidateOwner")||!tag.hasUUID("GhostRescueCandidateAttempt"))&&!recoverLegacyReference(p,candidate))return null;
            try{p.server.saveEverything(true,true,true);return TycoonCheckpoints.home(p)?actor:null;}catch(java.io.IOException failure){return null;}
        }
        return null;
    }
    public static boolean legacyLineageMatches(CompoundTag ghost,CompoundTag candidate){
        var escape=ghost.getCompound(GhostEscapeProgress.KEY);
        return candidate.getBoolean("OriginalGhostHelper")&&candidate.hasUUID("Id")&&candidate.hasUUID("GhostRescueAttempt")
            &&GhostEscapeProgress.owns(ghost,escape)&&escape.hasUUID("Attempt")&&GhostEscapeProgress.phase(escape)==GhostEscapeProgress.Phase.COMPLETE
            &&escape.getBoolean("RestorationCommitted")&&escape.getBoolean("WagerWon")&&escape.getBoolean("ChildDepartureWitnessed")&&escape.getBoolean("AgencyRegistrationSaved")
            &&candidate.getUUID("Id").equals(escape.getUUID("Victim"))&&candidate.getUUID("GhostRescueAttempt").equals(escape.getUUID("Attempt"));
    }
    private static boolean recoverLegacyReference(ServerPlayer p,TycoonData.Resident candidate){
        var owner=TycoonWorld.owner(p);if(!owner.equals(p.getUUID())){var actual=p.server.getPlayerList().getPlayer(owner);return actual!=null&&TycoonScope.call(actual,owner,()->recoverLegacyReference(actual,candidate),false);}
        var scene=OtherMotherLoop.scene(p);if(scene==null)return false;var ghost=GhostVictimEntry.data(scene);
        if(!GhostVictimMirrors.owned(p,scene,ghost)||!legacyLineageMatches(ghost,candidate.n))return false;
        var actor=GhostVictimActors.find(p,scene,ghost);if(actor==null)return false;
        var data=TycoonData.get(p.server);var home=data.home(p.getUUID());var attempt=candidate.n.getUUID("GhostRescueAttempt");
        var tag=actor.getPersistentData();if(tag.hasUUID("GhostRescueCandidateOwner")&&!p.getUUID().equals(tag.getUUID("GhostRescueCandidateOwner"))||tag.hasUUID("GhostRescueCandidateAttempt")&&!attempt.equals(tag.getUUID("GhostRescueCandidateAttempt")))return false;
        if(!bindOriginalActorReceipt(home,candidate.id,attempt,actor.getUUID(),actor.level().dimension().location().toString(),actor.blockPosition()))return false;
        var saved=home.state.getCompound("Candidates").getCompound(candidate.id.toString());
        candidate.n.putUUID("GhostHelperActor",saved.getUUID("GhostHelperActor"));candidate.n.putString("GhostHelperDimension",saved.getString("GhostHelperDimension"));candidate.n.putLong("GhostHelperPosition",saved.getLong("GhostHelperPosition"));
        actor.getPersistentData().putUUID("GhostRescueCandidateOwner",p.getUUID());actor.getPersistentData().putUUID("GhostRescueCandidateAttempt",attempt);
        actor.getNavigation().stop();actor.setInvisible(true);actor.setNoAi(true);actor.setNoGravity(true);actor.noPhysics=true;actor.setInvulnerable(true);actor.setPersistenceRequired();data.setDirty();
        try{p.server.saveEverything(true,true,true);return TycoonCheckpoints.home(p);}catch(java.io.IOException failure){return false;}
    }
    public static boolean registerCandidate(TycoonData.Home home,CompoundTag identity,UUID child,UUID attempt,double startingNeglect){
        if(home==null||identity==null||child==null||attempt==null||!identity.hasUUID("Id")||!child.equals(identity.getUUID("Id"))
            ||!Double.isFinite(startingNeglect)||startingNeglect<0||startingNeglect>5||home.residents.containsKey(child))return false;
        var receipts=home.state.getCompound("GhostRescueCandidates");var pool=home.state.getCompound("Candidates");String key=child.toString();
        if(receipts.contains(key)||pool.contains(key))return false;
        var saved=identity.copy();saved.putString("Status","CANDIDATE");saved.putBoolean("OriginalGhostHelper",true);saved.putUUID("GhostRescueAttempt",attempt);
        saved.putDouble("Neglect",startingNeglect);saved.putString("Conversion","NONE");
        for(String field:new String[]{"MotherTrust","GhostTrust","Outcome","RetiredActor","PendingGhostReport","Entity","Bed","SpawnPending"})saved.remove(field);
        pool.put(key,saved);home.state.put("Candidates",pool);receipts.putUUID(key,attempt);home.state.put("GhostRescueCandidates",receipts);return true;
    }
    private GhostRescueCandidates(){}
}
