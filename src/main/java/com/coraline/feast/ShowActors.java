package com.coraline.feast;

import com.coraline.entity.CharacterEntity;
import com.coraline.registry.CharacterCast;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.*;
import java.util.*;

/** Exclusive leases of actual cast models and entities; reload never clones a recorded identity. */
public final class ShowActors {
    private static CompoundTag root(VisitData.Scene s){if(!s.loop.event.contains("ShowActors"))s.loop.event.put("ShowActors",new CompoundTag());return s.loop.event.getCompound("ShowActors");}
    public static boolean storyCat(CharacterEntity actor){
        if(actor.level().isClientSide)return actor.storyCatCompanion();
        if(!actor.characterId().equals("cat")||!actor.getPersistentData().hasUUID("ShowActorOwner"))return false;
        // The owner tag is synced/saved independently; the server additionally
        // verifies the exact active lease, never an arbitrary spawned lookalike.
        var s=VisitData.get(actor.getServer()).scenes.get(actor.getPersistentData().getUUID("ShowActorOwner"));if(s==null)return false;
        var lease=s.loop.event.getCompound("ShowActors").getCompound("cat");return lease.hasUUID("Entity")&&lease.getUUID("Entity").equals(actor.getUUID());
    }
    public static Mob actor(ServerPlayer p,VisitData.Scene s,String id){var n=root(s).getCompound(id);if(!n.hasUUID("Entity"))return null;if(n.contains("Position")&&n.contains("Dimension")){var l=com.coraline.parents.FamilyData.level(p.server,n.getString("Dimension"));if(l!=null)LoopProps.hold(l,net.minecraft.core.BlockPos.of(n.getLong("Position")));}for(var l:p.server.getAllLevels())if(l.getEntity(n.getUUID("Entity")) instanceof Mob m){remember(n,m);return m;}return null;}
    private static void remember(CompoundTag record,Mob actor){record.putLong("Position",actor.blockPosition().asLong());record.putString("Dimension",actor.level().dimension().location().toString());}
    private static CompoundTag canonicalCat(VisitData.Scene s){if(!s.loop.story.contains("CanonicalStoryCat"))s.loop.story.put("CanonicalStoryCat",new CompoundTag());return s.loop.story.getCompound("CanonicalStoryCat");}
    private static void rememberCat(VisitData.Scene s,Mob actor){var record=canonicalCat(s);if(record.hasUUID("Entity")&&!record.getUUID("Entity").equals(actor.getUUID()))throw new IllegalStateException("Canonical story Cat identity changed");record.putUUID("Entity",actor.getUUID());remember(record,actor);actor.getPersistentData().putUUID("CanonicalStoryCatOwner",s.owner);}
    public static void rememberCanonicalCat(CharacterEntity actor){if(actor.getServer()==null||!actor.getPersistentData().hasUUID("CanonicalStoryCatOwner"))return;var owner=actor.getPersistentData().getUUID("CanonicalStoryCatOwner");var s=VisitData.get(actor.getServer()).scenes.get(owner);if(actor.getPersistentData().hasUUID("CanonicalStoryCatEndingArchive")&&!actor.getPersistentData().hasUUID("ShowActorOwner")){actor.getNavigation().stop();actor.stopRiding();actor.setTarget(null);actor.setNoAi(true);actor.setNoGravity(true);actor.noPhysics=true;actor.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);actor.setInvulnerable(true);actor.setInvisible(true);actor.setSilent(true);}if(s==null)return;var record=canonicalCat(s);if(record.getBoolean("FarewellDeparted")&&record.hasUUID("Entity")&&record.getUUID("Entity").equals(actor.getUUID())){actor.discard();return;}if(record.hasUUID("Entity")&&record.getUUID("Entity").equals(actor.getUUID())){remember(record,actor);if(actor.tickCount%100==0)VisitData.get(actor.getServer()).setDirty();}}
    private static boolean parkCat(ServerPlayer p,VisitData.Scene s,Mob actor,CompoundTag lease){
        var returned=ShowTravel.returnGuest(actor,p,s,"ShowActorReturn");if(returned==null)return false;rememberCat(s,returned);returned.setNoAi(lease.getBoolean("NoAI"));returned.setNoGravity(lease.getBoolean("Gravity"));returned.setInvulnerable(lease.getBoolean("Invulnerable"));returned.noPhysics=lease.getBoolean("Physics");returned.setInvisible(false);returned.getPersistentData().remove("ShowActorOwner");returned.getPersistentData().remove("ShowActorSerial");ShowPlaybackNetwork.pose(returned,0,0,1);return true;
    }
    /** Adopt a known canonical visitor identity instead of spawning a second version. */
    public static Mob adopt(ServerPlayer p,VisitData.Scene s,String id,Mob actor,ServerLevel level,Vec3 at){
        if(!s.owner.equals(p.getUUID())||actor==null)return null;
        if(root(s).contains(id))return acquire(p,s,id,level,at);
        var tag=actor.getPersistentData();if(tag.hasUUID("CanonicalRealWybieOwner")&&!s.owner.equals(tag.getUUID("CanonicalRealWybieOwner")))return null;if(tag.hasUUID("PartyOwner")||tag.hasUUID("GameNightOwner")||tag.hasUUID("ShowActorOwner"))return null;
        var n=new CompoundTag();n.putUUID("Entity",actor.getUUID());n.putBoolean("Created",false);n.putBoolean("NoAI",actor.isNoAi());n.putBoolean("Gravity",actor.isNoGravity());n.putBoolean("Invulnerable",actor.isInvulnerable());n.putBoolean("Physics",actor.noPhysics);root(s).put(id,n);
        tag.putUUID("ShowActorOwner",s.owner);tag.putInt("ShowActorSerial",s.loop.event.getInt("Serial"));tag.putUUID("RealmDoor",s.door);
        var moved=ShowTravel.guest(actor,p,s,level,at,true,"ShowActorReturn");if(moved!=null){n.putUUID("Entity",moved.getUUID());remember(n,moved);control(moved);}OtherMotherLoop.changed(p);return moved;
    }
    public static Mob acquire(ServerPlayer p,VisitData.Scene s,String id,ServerLevel level,Vec3 at){
        if(!s.owner.equals(p.getUUID()))return null;var all=root(s);if(all.contains(id)){var bound=actor(p,s,id);if(bound==null||!owned(bound,s))return null;if(bound.level()!=level){bound=ShowTravel.guest(bound,p,s,level,at,true,"ShowActorReturn");if(bound==null)return null;all.getCompound(id).putUUID("Entity",bound.getUUID());}remember(all.getCompound(id),bound);if(id.equals("cat"))rememberCat(s,bound);control(bound);return bound;}
        if(!CharacterCast.ENTITIES.containsKey(id))return null;Mob chosen=null;
        if(id.equals("cat")&&canonicalCat(s).hasUUID("Entity")){var record=canonicalCat(s);var savedLevel=com.coraline.parents.FamilyData.level(p.server,record.getString("Dimension"));if(savedLevel!=null)LoopProps.hold(savedLevel,net.minecraft.core.BlockPos.of(record.getLong("Position")));for(var world:p.server.getAllLevels())if(world.getEntity(record.getUUID("Entity")) instanceof CharacterEntity cat&&cat.characterId().equals("cat")&&cat.getPersistentData().hasUUID("CanonicalStoryCatOwner")&&s.owner.equals(cat.getPersistentData().getUUID("CanonicalStoryCatOwner"))){chosen=cat;break;}if(chosen==null)return null;chosen.getPersistentData().remove("CanonicalStoryCatEndingArchive");chosen.setInvisible(false);chosen.setSilent(false);ShowPlaybackNetwork.pose(chosen,0,0,1);}
        if(id.equals("wybie")&&CanonicalRealWybie.recorded(s)){try{CanonicalRealWybie.rebindTimeline(p,s,com.coraline.rebirth.StoryRunStore.get(p.server,p.getUUID()));}catch(java.io.IOException bad){return null;}var original=CanonicalRealWybie.find(p,s);if(original==null||!CanonicalRealWybie.bind(p,s,original))return null;chosen=original;}
        for(var l:p.server.getAllLevels())for(var entity:l.getAllEntities())if(entity instanceof CharacterEntity candidate&&candidate.characterId().equals(id)&&candidate.getPersistentData().hasUUID("RealmDoor")&&s.door.equals(candidate.getPersistentData().getUUID("RealmDoor"))){
            if(candidate.getPersistentData().hasUUID("CanonicalRealWybieOwner")&&!s.owner.equals(candidate.getPersistentData().getUUID("CanonicalRealWybieOwner")))continue;
            if(chosen!=null)break;var tag=candidate.getPersistentData();if(tag.hasUUID("PartyOwner")||tag.hasUUID("GameNightOwner")||tag.hasUUID("ShowActorOwner"))return null;chosen=candidate;break;
        }
        boolean created=chosen==null;if(created){chosen=CharacterCast.ENTITIES.get(id).get().create(level);if(chosen==null)return null;chosen.setPos(at);chosen.getPersistentData().putBoolean("CoralineDecoration",true);if(!level.addFreshEntity(chosen))return null;}
        var n=new CompoundTag();n.putUUID("Entity",chosen.getUUID());n.putBoolean("Created",created);n.putBoolean("NoAI",chosen.isNoAi());n.putBoolean("Gravity",chosen.isNoGravity());n.putBoolean("Invulnerable",chosen.isInvulnerable());n.putBoolean("Physics",chosen.noPhysics);all.put(id,n);
        chosen.getPersistentData().putUUID("ShowActorOwner",s.owner);chosen.getPersistentData().putInt("ShowActorSerial",s.loop.event.getInt("Serial"));chosen.getPersistentData().putUUID("RealmDoor",s.door);
        var transferred=ShowTravel.guest(chosen,p,s,level,at,true,"ShowActorReturn");if(transferred==null)return null;n.putUUID("Entity",transferred.getUUID());remember(n,transferred);if(id.equals("cat"))rememberCat(s,transferred);control(transferred);OtherMotherLoop.changed(p);return transferred;
    }
    public static void position(Mob actor,Vec3 at,float yaw,int pose,int elapsed,float reveal){if(actor==null)return;actor.getNavigation().stop();if(actor.level() instanceof ServerLevel level)LoopProps.hold(level,net.minecraft.core.BlockPos.containing(at));actor.setPos(at);actor.setYRot(yaw);actor.setYBodyRot(yaw);actor.setYHeadRot(yaw);actor.fallDistance=0;actor.setDeltaMovement(Vec3.ZERO);var tag=actor.getPersistentData();if(actor.getServer()!=null&&tag.hasUUID("ShowActorOwner")){var s=VisitData.get(actor.getServer()).scenes.get(tag.getUUID("ShowActorOwner"));if(s!=null&&owned(actor,s))for(String key:root(s).getAllKeys()){var record=root(s).getCompound(key);if(record.hasUUID("Entity")&&record.getUUID("Entity").equals(actor.getUUID()))remember(record,actor);}}ShowPlaybackNetwork.pose(actor,pose,elapsed,reveal);}
    public static boolean restore(ServerPlayer p,VisitData.Scene s){if(!s.owner.equals(p.getUUID()))return false;var all=root(s);for(String id:new HashSet<>(all.getAllKeys())){var actor=actor(p,s,id);if(actor==null||!owned(actor,s))continue;var n=all.getCompound(id);if(id.equals("wybie")&&actor instanceof CharacterEntity real&&CanonicalRealWybie.tagged(real)){if(!CanonicalRealWybie.park(p,s,real))continue;}else if(id.equals("cat")){if(visibleToAnyone(actor,p)||!parkCat(p,s,actor,n))continue;}else if(n.getBoolean("Created")){if(actor.level()!=p.level()||!p.hasLineOfSight(actor)||p.distanceToSqr(actor)>128*128){ShowTravel.forgetDiscarded(p,s,actor,"ShowActorReturn");actor.discard();}else continue;}else{actor=ShowTravel.returnGuest(actor,p,s,"ShowActorReturn");if(actor==null)continue;actor.setNoAi(n.getBoolean("NoAI"));actor.setNoGravity(n.getBoolean("Gravity"));actor.setInvulnerable(n.getBoolean("Invulnerable"));actor.noPhysics=n.getBoolean("Physics");actor.getPersistentData().remove("ShowActorOwner");actor.getPersistentData().remove("ShowActorSerial");ShowPlaybackNetwork.pose(actor,0,0,1);}
            all.remove(id);
        }return all.getAllKeys().isEmpty();}
    private static boolean owned(Mob actor,VisitData.Scene s){var n=actor.getPersistentData();return n.hasUUID("ShowActorOwner")&&s.owner.equals(n.getUUID("ShowActorOwner"))&&n.getInt("ShowActorSerial")==s.loop.event.getInt("Serial");}
    /** Visibility includes gaze, not just an unobstructed ray behind the camera. */
    public static boolean visibleToAnyone(Mob actor,ServerPlayer owner){
        if(inView(owner,actor))return true;
        for(var viewer:owner.server.getPlayerList().getPlayers())if(viewer!=owner&&inView(viewer,actor))return true;
        return false;
    }
    private static boolean inView(ServerPlayer viewer,Mob actor){return viewer.level()==actor.level()&&viewer.distanceToSqr(actor)<128*128&&viewer.hasLineOfSight(actor)&&viewer.getLookAngle().dot(actor.getEyePosition().subtract(viewer.getEyePosition()).normalize())>.15;}
    /** Release this lease only; unrelated unloaded show actors cannot stall Cat departure. */
    public static boolean releaseHidden(ServerPlayer p,VisitData.Scene s,String id){
        if(!s.owner.equals(p.getUUID()))return false;var all=root(s);if(!all.contains(id))return true;
        var actor=actor(p,s,id);if(actor==null||!owned(actor,s)||visibleToAnyone(actor,p))return false;var n=all.getCompound(id);
        if(id.equals("wybie")&&actor instanceof CharacterEntity real&&CanonicalRealWybie.tagged(real)){if(!CanonicalRealWybie.park(p,s,real))return false;}
        else if(id.equals("cat")){if(!parkCat(p,s,actor,n))return false;}
        else if(n.getBoolean("Created")){ShowTravel.forgetDiscarded(p,s,actor,"ShowActorReturn");actor.discard();}
        else{actor=ShowTravel.returnGuest(actor,p,s,"ShowActorReturn");if(actor==null)return false;actor.setNoAi(n.getBoolean("NoAI"));actor.setNoGravity(n.getBoolean("Gravity"));actor.setInvulnerable(n.getBoolean("Invulnerable"));actor.noPhysics=n.getBoolean("Physics");actor.getPersistentData().remove("ShowActorOwner");actor.getPersistentData().remove("ShowActorSerial");if(actor instanceof CharacterEntity c)c.gameGesture(0,0);ShowPlaybackNetwork.pose(actor,0,0,1);}
        all.remove(id);OtherMotherLoop.changed(p);return true;
    }
    private static void control(Mob actor){if(actor instanceof CharacterEntity cat&&cat.characterId().equals("cat"))cat.storyCatCrawl(false);actor.setNoAi(true);actor.setInvulnerable(true);actor.setNoGravity(true);actor.noPhysics=true;}
    /** Remove the canonical farewell Cat after the completed vision. */
    public static boolean departHiddenCat(ServerPlayer player,VisitData.Scene scene){
        if(!scene.owner.equals(player.getUUID()))return false;
        var farewell=RescueChapter.data(scene);
        if(!RescueChapter.started(scene)||!farewell.getBoolean("FarewellComplete")
            ||!farewell.getBoolean("VisionRestored")||!farewell.getBoolean("FarewellCatCleanupPending"))return false;
        var all=root(scene);var canonical=canonicalCat(scene);
        var lease=all.getCompound("cat");
        if(lease.hasUUID("Entity")){
            var retiring=actor(player,scene,"cat");
            if(retiring!=null){
                // The event serial may change during the finale. The canonical
                // owner and exact recorded UUID, not the old lease serial,
                // authenticate this one-time retirement.
                boolean canonicalIdentity=canonical.hasUUID("Entity")&&retiring.getUUID().equals(canonical.getUUID("Entity"))
                    &&retiring.getPersistentData().hasUUID("CanonicalStoryCatOwner")
                    &&scene.owner.equals(retiring.getPersistentData().getUUID("CanonicalStoryCatOwner"));
                if(!(retiring instanceof CharacterEntity cat)||!cat.characterId().equals("cat")
                    ||!retiring.getUUID().equals(lease.getUUID("Entity"))
                    ||!(canonicalIdentity||!canonical.hasUUID("Entity")&&owned(retiring,scene)))return false;
                retiring.discard();
            }
        }else if(canonical.hasUUID("Entity")){
            for(var level:player.server.getAllLevels())if(level.getEntity(canonical.getUUID("Entity")) instanceof CharacterEntity cat){
                if(!cat.characterId().equals("cat")||!cat.getPersistentData().hasUUID("CanonicalStoryCatOwner")
                    ||!scene.owner.equals(cat.getPersistentData().getUUID("CanonicalStoryCatOwner")))return false;
                cat.discard();break;
            }
        }
        canonical.putBoolean("FarewellDeparted",true);all.remove("cat");OtherMotherLoop.changed(player);return true;
    }
    public static final class ControlGoal extends net.minecraft.world.entity.ai.goal.Goal {
        private final Mob actor;
        public ControlGoal(Mob actor){this.actor=actor;setFlags(java.util.EnumSet.of(Flag.MOVE,Flag.LOOK));}
        @Override public boolean canUse(){return actor.getPersistentData().hasUUID("ShowActorOwner");}
        @Override public boolean canContinueToUse(){return canUse();}
        @Override public boolean requiresUpdateEveryTick(){return true;}
        @Override public void tick(){var n=actor.getPersistentData();if(actor.getServer()==null||!n.hasUUID("ShowActorOwner")){actor.getNavigation().stop();return;}var scene=VisitData.get(actor.getServer()).scenes.get(n.getUUID("ShowActorOwner"));if(scene==null||scene.loop.event.getInt("Serial")!=n.getInt("ShowActorSerial")){actor.getNavigation().stop();} }
    }
    private ShowActors(){}
}
