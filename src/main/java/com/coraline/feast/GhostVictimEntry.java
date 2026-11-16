package com.coraline.feast;

import java.util.UUID;
import com.coraline.otherworld.OtherWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** Manual prison exit and timed reverse deterioration, over the same accepted
 * route/escrow journal. This entry adapter owns no later rescue or restoration. */
public final class GhostVictimEntry {
    public static CompoundTag data(VisitData.Scene s){return AcceptedButtonEyes.data(s).getCompound(GhostVictimProgress.KEY);}
    public static boolean ready(VisitData.Scene s){return AcceptedButtonEyes.active(s)
        &&AcceptedButtonEyesProgress.phase(AcceptedButtonEyes.data(s))==AcceptedButtonEyesProgress.Phase.ACCEPTED_BUTTON_EYES_MIRROR_PRISON
        &&AcceptedButtonEyes.data(s).getBoolean("GhostScenePlayed");}
    public static boolean begin(ServerPlayer p,VisitData.Scene s){
        if(!ready(s)||!p.getUUID().equals(s.owner))return false;
        var accepted=AcceptedButtonEyes.data(s);var n=data(s);if(n.getBoolean("Started"))return GhostVictimMirrors.owned(p,s,n);
        GhostVictimProgress.begin(n,accepted.getString("Route"));n.putUUID("Owner",s.owner);n.putUUID("Door",s.door);n.putUUID("Session",UUID.randomUUID());
        n.putString("BodyGameMode",p.gameMode.getGameModeForPlayer().getName());accepted.put(GhostVictimProgress.KEY,n);
        OtherMotherLoop.changed(p);return true;
    }
    public static BlockPos prisonMirror(VisitData.Scene s){return FirstEscapeScene.prison(s).north(4).above();}
    public static boolean leave(ServerPlayer p,VisitData.Scene s,UUID session){
        var n=data(s);if(!GhostVictimMirrors.owned(p,s,n)||session==null||!n.hasUUID("Session")||!session.equals(n.getUUID("Session"))
            ||GhostVictimProgress.phase(n)!=GhostVictimProgress.Phase.ENTER_MIRROR||!OtherWorld.isOther(p.level()))return false;
        var mirror=prisonMirror(s).getCenter();var delta=mirror.subtract(p.getEyePosition());
        if(delta.lengthSqr()>36||delta.lengthSqr()<.01||p.getLookAngle().dot(delta.normalize())<.85)return false;
        // Only this explicit interaction releases the old prison hold. Neither
        // age, rejoin nor a generic watchdog can make this decision for players.
        if(!AcceptedButtonEyesWorld.transfer(p,s,GhostVictimWorld.hall(s)))return false;
        if(!GhostVictimProgress.leavePrison(n))return false;
        p.setGameMode(GameType.SPECTATOR);p.setCamera(p);p.stopRiding();p.setForcedPose(null);
        ShowPlayback.stop(p,s);OtherMotherLoop.changed(p);return true;
    }
    /** Returns true only while this adapter owns one of the entry stages. */
    public static boolean tick(ServerPlayer p,VisitData.Scene s){
        var n=data(s);if(!GhostVictimMirrors.owned(p,s,n))return false;
        switch(GhostVictimProgress.phase(n)){
            case ENTER_MIRROR->{
                FirstEscapeStory.acceptedPrisonStill(p,s);
                if(p.distanceToSqr(FirstEscapeScene.prison(s).getCenter())>100)AcceptedButtonEyesWorld.transfer(p,s,FirstEscapeScene.prison(s));
                return true;
            }
            case EMPTY_WORLD->{
                if(!n.getBoolean("PrisonRetired")){FirstEscapeScene.retirePrison(p,s);n.putBoolean("PrisonRetired",true);}
                hideParents(p,s,true);
                if(GhostVictimProgress.age(n)>=Math.min(3,GhostVictimConfig.REBUILD_DELAY_SECONDS.get())*20L){
                    if(!GhostVictimProgress.hasVictim(n)){UUID id=UUID.randomUUID();GhostVictimProgress.newVictim(n,id,GhostVictimActors.identity(id));}
                    if(GhostVictimWorld.prepare(p,s,n)){GhostVictimProgress.move(n,GhostVictimProgress.Phase.RECONSTRUCTION);OtherMotherLoop.changed(p);}
                }
                return true;
            }
            case RECONSTRUCTION,NEW_VICTIM_RESET->{
                hideParents(p,s,GhostVictimWorld.deterioration(n)>1.5);
                long length=GhostVictimConfig.REBUILD_SECONDS.get()*(GhostVictimProgress.phase(n)==GhostVictimProgress.Phase.NEW_VICTIM_RESET?30L:20L);
                if(GhostVictimProgress.age(n)>=length){
                    if(!GhostVictimProgress.hasVictim(n)){UUID id=UUID.randomUUID();GhostVictimProgress.newVictim(n,id,GhostVictimActors.identity(id));}
                    disguise(p,s,n);GhostVictimSchedule.today(n);GhostVictimProgress.move(n,GhostVictimProgress.Phase.WAITING_FOR_VICTIM);OtherMotherLoop.changed(p);
                }
                return true;
            }
            default->{return false;}
        }
    }
    private static void hideParents(ServerPlayer p,VisitData.Scene s,boolean hide){
        var mother=AcceptedButtonEyesWorld.mother(p,s);var father=AcceptedButtonEyesWorld.father(p,s);
        if(mother!=null){mother.setInvisible(hide);mother.getNavigation().stop();}
        if(father!=null){father.setInvisible(hide);father.getNavigation().stop();}
    }
    public static void disguise(ServerPlayer p,VisitData.Scene s,CompoundTag n){
        var mother=AcceptedButtonEyesWorld.mother(p,s);var father=AcceptedButtonEyesWorld.father(p,s);var child=GhostVictimProgress.child(n);
        // These are the Other World actors only. The actual family and its
        // original appearances remain in their separate restoration journal.
        if(mother!=null){mother.makeOtherParent(child.getInt("MotherSkin"));mother.setInvisible(false);mother.eventGaze(true);mother.familyGesture(1,5);}
        if(father!=null){father.makeOtherParent(child.getInt("FatherSkin"));father.setInvisible(false);father.eventGaze(true);father.familyGesture(1,5);}
    }
    public static void ghostControls(ServerPlayer p,VisitData.Scene s,CompoundTag n){
        if(!GhostVictimMirrors.participant(p,s,n)||GhostVictimProgress.phase(n)==GhostVictimProgress.Phase.ENTER_MIRROR)return;
        if(p.gameMode.getGameModeForPlayer()!=GameType.SPECTATOR)p.setGameMode(GameType.SPECTATOR);
        if(p.getCamera()!=p)p.setCamera(p);p.fallDistance=0;p.clearFire();p.setAirSupply(p.getMaxAirSupply());
        // Owner-local bounds expand to a leased activity only when that arena
        // is active. Phasing ordinary walls never grants access to other realms.
        var house=AcceptedButtonEyesWorld.house(s);double radius=n.contains("GhostRadius")?Math.min(512,Math.max(64,n.getDouble("GhostRadius"))):128;
        Vec3 center=n.contains("GhostCenter")?BlockPos.of(n.getLong("GhostCenter")).getCenter():house.getCenter();
        var expected=n.getString("GhostDimension").isEmpty()?OtherWorld.KEY.location().toString():n.getString("GhostDimension");
        if(p.level().dimension().location().toString().equals(expected)&&p.position().distanceToSqr(center)<radius*radius&&Math.abs(p.getY()-center.y)<96)return;
        var level=com.coraline.parents.FamilyData.level(p.server,expected);if(level==null)return;
        p.teleportTo(level,center.x,center.y+1,center.z,p.getYRot(),p.getXRot());p.setDeltaMovement(Vec3.ZERO);
    }
    private GhostVictimEntry(){}
}
