package com.coraline.feast;

import com.coraline.entity.CharacterEntity;
import com.coraline.registry.CharacterCast;
import com.coraline.secrets.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.*;
import java.util.*;

/** One borrowed original Other Wybie, shared by adventures and optional performance visits. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="coraline")
public final class WybieOptionalVisits {
    public static boolean allowed(VisitData.Scene s){return s!=null&&s.loop.story.getBoolean("WybieIntroductionComplete")&&!s.loop.story.getBoolean("WybieVisitsForbidden");}
    public static void introductionCompleted(ServerPlayer p){var s=OtherMotherLoop.scene(p);if(s!=null&&s.owner.equals(p.getUUID())){s.loop.story.putBoolean("WybieIntroductionComplete",true);OtherMotherLoop.changed(p);}}
    public static CharacterEntity actor(ServerPlayer p,CompoundTag lease){if(!lease.hasUUID("Actor"))return null;for(var level:p.server.getAllLevels())if(level.getEntity(lease.getUUID("Actor")) instanceof CharacterEntity w&&bound(w,p,lease))return w;return null;}
    public static CharacterEntity acquire(ServerPlayer p,VisitData.Scene s,CompoundTag lease,BlockPos entrance){
        if(s==null||!p.getUUID().equals(s.owner)||!allowed(s)||entrance==null||!p.serverLevel().hasChunkAt(entrance))return null;
        var current=actor(p,lease);if(current!=null)return owns(current,p,s,lease)?current:null;
        // A previous farewell must not make every later invitation silently lose
        // Wybie. Reclaim that exact retiring actor; never spawn a second copy.
        if(!lease.hasUUID("Actor")&&!s.loop.story.hasUUID("WybieLeaseToken")&&s.loop.story.hasUUID("WybieDepartingActor")){
            if(s.loop.story.contains("WybieDepartingPosition"))p.serverLevel().getChunkAt(BlockPos.of(s.loop.story.getLong("WybieDepartingPosition")));
            for(var level:p.server.getAllLevels())if(level.getEntity(s.loop.story.getUUID("WybieDepartingActor")) instanceof CharacterEntity retiring&&retiring.characterId().equals("other_wybie")&&retiring.getPersistentData().hasUUID("RealmDoor")&&s.door.equals(retiring.getPersistentData().getUUID("RealmDoor"))){
                var old=retiring.getPersistentData();boolean created=old.getBoolean("WybieCreated"),invulnerable=old.getBoolean("WybieOldInvulnerable"),silent=old.getBoolean("WybieOldSilent");
                old.remove("WybieRetiring");old.remove("WybieRetireTarget");s.loop.story.remove("WybieDepartingActor");s.loop.story.remove("WybieDepartingMissing");
                bind(retiring,p,s,lease,created);old.putBoolean("WybieOldInvulnerable",invulnerable);old.putBoolean("WybieOldSilent",silent);return retiring;
            }return null;
        }
        // A saved UUID may be in an unloaded chunk: never substitute a duplicate actor.
        if(lease.hasUUID("Actor")||s.loop.story.hasUUID("WybieLeaseToken"))return null;
        var realm=SecretWorld.realm(p);if(realm==null||realm.house==null)return null;
        var found=p.serverLevel().getEntitiesOfClass(CharacterEntity.class,new AABB(realm.house).inflate(Math.min(550,realm.radius+50)),e->e.characterId().equals("other_wybie")&&e.getPersistentData().hasUUID("RealmDoor")&&s.door.equals(e.getPersistentData().getUUID("RealmDoor")));
        // Any other event lease blocks acquisition, including a temporarily offline owner.
        if(found.stream().anyMatch(w->w.getPersistentData().hasUUID("GameNightOwner")||w.getPersistentData().hasUUID("PartyOwner")||w.getPersistentData().hasUUID("WybieCompanionOwner")))return null;
        var w=found.stream().min(Comparator.comparingDouble(e->e.distanceToSqr(entrance.getCenter()))).orElse(null);boolean created=w==null;
        if(created){var registry=CharacterCast.ENTITIES.get("other_wybie");if(registry==null)return null;w=registry.get().create(p.serverLevel());if(w==null)return null;var safe=Reconciliation.safe(p,entrance);if(safe==null)return null;w.setPos(Vec3.atBottomCenterOf(safe));if(!p.serverLevel().noCollision(w,w.getBoundingBox()))return null;}
        else if(w.distanceToSqr(entrance.getCenter())>9){
            if(!visible(p,w.position())&&!visible(p,Vec3.atBottomCenterOf(entrance))){var safe=Reconciliation.safe(p,entrance);if(safe==null)return null;w.teleportTo(safe.getX()+.5,safe.getY(),safe.getZ()+.5);}
        }
        bind(w,p,s,lease,created);
        if(created&&!p.serverLevel().addFreshEntity(w)){
            // Roll back only this allocation; a failed spawn must never lock future visits.
            clearToken(s,lease);clearLease(lease);OtherMotherLoop.changed(p);return null;
        }
        OtherMotherLoop.changed(p);return w;
    }
    private static void bind(CharacterEntity w,ServerPlayer p,VisitData.Scene s,CompoundTag lease,boolean created){
        var token=UUID.randomUUID();s.loop.story.putUUID("WybieLeaseToken",token);lease.putUUID("Actor",w.getUUID());lease.putUUID("Owner",p.getUUID());lease.putUUID("Door",s.door);lease.putUUID("Token",token);lease.putInt("Serial",s.loop.event.getInt("Serial"));
        var tag=w.getPersistentData();tag.putBoolean("WybieCreated",created);tag.putBoolean("WybieOldInvulnerable",tag.contains("GameOldInvulnerable")?tag.getBoolean("GameOldInvulnerable"):w.isInvulnerable());tag.putBoolean("WybieOldSilent",w.isSilent());tag.putUUID("WybieCompanionOwner",p.getUUID());tag.putUUID("WybieCompanionToken",token);tag.putInt("WybieCompanionSerial",s.loop.event.getInt("Serial"));tag.putUUID("RealmDoor",s.door);tag.putBoolean("CoralineDecoration",true);
        if(!tag.contains("WybieOldNoAi")){tag.putBoolean("WybieOldNoAi",w.isNoAi());tag.putBoolean("WybieOldNoGravity",w.isNoGravity());tag.putBoolean("WybieOldNoPhysics",w.noPhysics);}
        w.setNoAi(false);w.setNoGravity(false);w.noPhysics=false;w.setSilent(true);w.setInvulnerable(true);w.setPersistenceRequired();prepareNavigation(w);w.gameGesture(1,55);lease.putString("Mode","WAIT");tag.putString("WybieCompanionMode","WAIT");
    }
    /** The porch-to-table route is longer than the cast's default 16 m search
     * range. More visited nodes alone does not enlarge that search radius.
     * Preserve the borrowed actor's exact attribute, including legacy leases.
     */
    static void prepareNavigation(CharacterEntity w){
        var range=w.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);var n=w.getPersistentData();
        // A two-metre cast hitbox cannot plan through a two-block doorway over
        // carpet: the pathfinder rounds that clearance up to three nodes. Use
        // the existing visible duck pose for the borrowed house companion.
        if(!n.contains("WybieOldDoorDuck"))n.putBoolean("WybieOldDoorDuck",w.partyDoorDuck());
        w.partyDoorDuck(true);
        if(range!=null){if(!n.contains("WybieOldFollowRange"))n.putDouble("WybieOldFollowRange",range.getBaseValue());range.setBaseValue(Math.max(64,range.getBaseValue()));}
        w.setMaxUpStep(1.05F);w.getNavigation().setMaxVisitedNodesMultiplier(4);
    }
    /** Strict saved-actor migration for pre-lease Family Game Night saves; never creates a replacement. */
    static CharacterEntity adoptGameNight(ServerPlayer p,VisitData.Scene s,CompoundTag lease){
        var event=s.loop.event;if(!p.getUUID().equals(s.owner)||!allowed(s)||!event.getBoolean("GameNightPending")||VisitEvents.type(s)!=VisitEvents.Type.FAMILY_GAME_NIGHT||!event.hasUUID("GameWybie")||lease.hasUUID("Actor")||s.loop.story.hasUUID("WybieLeaseToken"))return null;
        for(var level:p.server.getAllLevels())if(level.getEntity(event.getUUID("GameWybie")) instanceof CharacterEntity w&&w.characterId().equals("other_wybie")){
            var tag=w.getPersistentData();if(!tag.hasUUID("GameNightOwner")||!p.getUUID().equals(tag.getUUID("GameNightOwner"))||!tag.hasUUID("RealmDoor")||!s.door.equals(tag.getUUID("RealmDoor"))||tag.hasUUID("WybieCompanionOwner")||tag.hasUUID("PartyOwner"))return null;
            bind(w,p,s,lease,tag.getBoolean("GameNightCreated"));OtherMotherLoop.changed(p);return w;
        }return null;
    }
    private static boolean bound(CharacterEntity w,ServerPlayer p,CompoundTag lease){
        var n=w.getPersistentData();return w.characterId().equals("other_wybie")&&lease.hasUUID("Actor")&&w.getUUID().equals(lease.getUUID("Actor"))&&lease.hasUUID("Owner")&&p.getUUID().equals(lease.getUUID("Owner"))&&n.hasUUID("WybieCompanionOwner")&&p.getUUID().equals(n.getUUID("WybieCompanionOwner"))&&lease.hasUUID("Door")&&n.hasUUID("RealmDoor")&&lease.getUUID("Door").equals(n.getUUID("RealmDoor"))&&lease.hasUUID("Token")&&n.hasUUID("WybieCompanionToken")&&lease.getUUID("Token").equals(n.getUUID("WybieCompanionToken"))&&lease.contains("Serial")&&n.contains("WybieCompanionSerial")&&lease.getInt("Serial")==n.getInt("WybieCompanionSerial");
    }
    public static boolean owns(CharacterEntity w,ServerPlayer p,VisitData.Scene s,CompoundTag lease){return w!=null&&s!=null&&p.getUUID().equals(s.owner)&&bound(w,p,lease)&&s.door.equals(lease.getUUID("Door"))&&lease.getInt("Serial")==s.loop.event.getInt("Serial")&&s.loop.story.hasUUID("WybieLeaseToken")&&lease.getUUID("Token").equals(s.loop.story.getUUID("WybieLeaseToken"));}
    private static void clearToken(VisitData.Scene s,CompoundTag lease){if(s!=null&&lease.hasUUID("Owner")&&s.owner.equals(lease.getUUID("Owner"))&&lease.hasUUID("Door")&&s.door.equals(lease.getUUID("Door"))&&lease.hasUUID("Token")&&s.loop.story.hasUUID("WybieLeaseToken")&&lease.getUUID("Token").equals(s.loop.story.getUUID("WybieLeaseToken")))s.loop.story.remove("WybieLeaseToken");}
    private static void clearLease(CompoundTag lease){for(String key:List.of("Actor","Owner","Door","Token","Serial","Mode","ActorPosition","Stuck","LastX","LastY","LastZ"))lease.remove(key);}
    /** Call once per server tick. Uses only loaded, owner-specific discoveries and existing pathfinding. */
    public static void follow(ServerPlayer p,VisitData.Scene s,CompoundTag lease){
        var w=actor(p,lease);if(w==null||w.level()!=p.level()||!owns(w,p,s,lease))return;if(!allowed(s)){release(p,lease);return;}prepareNavigation(w);
        lease.putString("Mode","FOLLOW");lease.putLong("ActorPosition",w.blockPosition().asLong());var tag=w.getPersistentData();tag.putString("WybieCompanionMode","FOLLOW");
        if(w.isPassenger())return;
        if(w.distanceToSqr(p)<6.25&&w.hasLineOfSight(p)){w.getNavigation().stop();if(w.tickCount%30==0)w.getLookControl().setLookAt(p,25,15);}
        else if(w.tickCount%10==0)navigate(w,p.blockPosition(),w.distanceToSqr(p)>36?1.75:1.35);
        recover(p,w,lease);WybieDiscoveries.tick(p,s,w,lease);
    }
    static void recover(ServerPlayer p,CharacterEntity w,CompoundTag lease){
        if(w.tickCount%40!=0)return;Vec3 old=new Vec3(lease.getDouble("LastX"),lease.getDouble("LastY"),lease.getDouble("LastZ"));boolean stuck=w.distanceToSqr(p)>9&&w.position().distanceToSqr(old)<.25;
        lease.putInt("Stuck",stuck?lease.getInt("Stuck")+40:0);lease.putDouble("LastX",w.getX());lease.putDouble("LastY",w.getY());lease.putDouble("LastZ",w.getZ());
        if(lease.getInt("Stuck")<120||visible(p,w.position()))return;
        for(int offset:new int[]{5,-5,7,-7}){var at=Reconciliation.safe(p,p.blockPosition().offset(offset,0,5));if(at==null||visible(p,Vec3.atBottomCenterOf(at))||!p.serverLevel().noCollision(w,w.getDimensions(w.getPose()).makeBoundingBox(Vec3.atBottomCenterOf(at))))continue;
            w.getNavigation().stop();w.teleportTo(at.getX()+.5,at.getY(),at.getZ()+.5);lease.putInt("Stuck",0);return;}
    }
    public static boolean visible(ServerPlayer p,Vec3 at){var target=at.add(0,1,0);return p.distanceToSqr(at)<4096&&p.getLookAngle().dot(target.subtract(p.getEyePosition()).normalize())>.05&&p.serverLevel().clip(new net.minecraft.world.level.ClipContext(p.getEyePosition(),target,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,p)).getType()==HitResult.Type.MISS;}
    /** Migrate old upstairs waiting checkpoints through real navigation or strictly offscreen staging. */
    public static void stageWaiting(ServerPlayer p,VisitData.Scene s,CompoundTag lease,BlockPos target){var w=actor(p,lease);if(target==null||w==null||w.level()!=p.level()||!owns(w,p,s,lease))return;var tag=w.getPersistentData();if(w.distanceToSqr(target.getCenter())<4){tag.putString("WybieCompanionMode","WAIT");w.getNavigation().stop();return;}tag.putString("WybieCompanionMode","STAGING");if(!visible(p,w.position())&&!visible(p,Vec3.atBottomCenterOf(target))){var safe=Reconciliation.safe(p,target);if(safe!=null&&p.serverLevel().noCollision(w,w.getDimensions(w.getPose()).makeBoundingBox(Vec3.atBottomCenterOf(safe)))){w.teleportTo(safe.getX()+.5,safe.getY(),safe.getZ()+.5);lease.putLong("ActorPosition",safe.asLong());return;}}if(w.tickCount%10==0||w.getNavigation().isDone())navigate(w,target,1.15);}
    private static void navigate(CharacterEntity w,BlockPos target,double speed){w.setMaxUpStep(1.05F);if(w.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation nav){nav.setCanOpenDoors(true);nav.setCanPassDoors(true);}var path=w.getNavigation().createPath(target,1);if(path!=null&&path.getNodeCount()>1){var end=path.getEndNode();if(path.canReach()||end!=null&&new BlockPos(end.x,end.y,end.z).distSqr(target)<w.blockPosition().distSqr(target))w.getNavigation().moveTo(path,speed);}if(w.horizontalCollision)w.getJumpControl().jump();}
    public static void release(ServerPlayer p,CompoundTag lease){
        if(!lease.hasUUID("Owner")||!p.getUUID().equals(lease.getUUID("Owner")))return;
        var w=actor(p,lease);if(w!=null)release(w);var s=OtherMotherLoop.scene(p);clearToken(s,lease);clearLease(lease);OtherMotherLoop.changed(p);
    }
    /** Wave, then leave under real navigation; deletion only occurs out of the owner's view. */
    public static void retire(ServerPlayer p,CompoundTag lease){
        var w=actor(p,lease);var s=OtherMotherLoop.scene(p);if(w==null||s==null||!owns(w,p,s,lease)){release(p,lease);return;}var realm=SecretWorld.realm(p);if(realm==null||realm.porch==null){release(p,lease);return;}
        var tag=w.getPersistentData();tag.putBoolean("WybieRetiring",true);tag.putLong("WybieRetireTarget",realm.porch.north(12).asLong());tag.putString("WybieCompanionMode","LEAVING");w.stopRiding();w.getNavigation().stop();
        s.loop.story.putUUID("WybieDepartingActor",w.getUUID());s.loop.story.putLong("WybieDepartingPosition",w.blockPosition().asLong());clearToken(s,lease);clearLease(lease);OtherMotherLoop.changed(p);
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void maintain(net.minecraftforge.event.TickEvent.PlayerTickEvent e){if(e.player instanceof net.minecraft.server.level.ServerPlayer coopPlayer&&com.coraline.multiplayer.StoryCoop.directorBlocked(coopPlayer))return;
        if(e.phase!=net.minecraftforge.event.TickEvent.Phase.END||!(e.player instanceof ServerPlayer p)||p.tickCount%20!=0)return;var s=OtherMotherLoop.scene(p);if(s==null||!s.loop.story.hasUUID("WybieDepartingActor"))return;
        CharacterEntity found=null;for(var level:p.server.getAllLevels())if(level.getEntity(s.loop.story.getUUID("WybieDepartingActor")) instanceof CharacterEntity w)found=w;
        if(found!=null){s.loop.story.putLong("WybieDepartingPosition",found.blockPosition().asLong());s.loop.story.remove("WybieDepartingMissing");}
        else if(com.coraline.otherworld.OtherWorld.isOther(p.level())&&s.loop.story.contains("WybieDepartingPosition")&&p.serverLevel().hasChunkAt(BlockPos.of(s.loop.story.getLong("WybieDepartingPosition")))){
            int ticks=s.loop.story.getInt("WybieDepartingMissing")+20;s.loop.story.putInt("WybieDepartingMissing",ticks);if(ticks>=200){s.loop.story.remove("WybieDepartingActor");s.loop.story.remove("WybieDepartingMissing");}
        }OtherMotherLoop.changed(p);
    }
    private static void leaving(CharacterEntity w,ServerPlayer p){
        var n=w.getPersistentData();if(p==null||p.level()!=w.level()){w.getNavigation().stop();return;}
        if(!visible(p,w.position())){release(w);return;}if(w.tickCount%40!=0)return;
        var at=BlockPos.of(n.getLong("WybieRetireTarget"));if(w.distanceToSqr(at.getCenter())<9){var further=Reconciliation.safe(p,at.north(12));if(further!=null){at=further;n.putLong("WybieRetireTarget",at.asLong());}}
        var path=w.getNavigation().createPath(at,0);if(path!=null&&path.canReach())w.getNavigation().moveTo(path,1);
    }
    private static void release(CharacterEntity w){
        var n=w.getPersistentData();w.stopRiding();w.getNavigation().stop();w.gameGesture(0,0);
        var scene=n.hasUUID("WybieCompanionOwner")?VisitData.get(w.getServer()).scenes.get(n.getUUID("WybieCompanionOwner")):null;
        if(scene!=null&&scene.loop.story.hasUUID("WybieDepartingActor")&&w.getUUID().equals(scene.loop.story.getUUID("WybieDepartingActor"))){scene.loop.story.remove("WybieDepartingActor");VisitData.get(w.getServer()).setDirty();}
        if(n.getBoolean("WybieCreated")||scene!=null&&scene.loop.story.getBoolean("WybieVisitsForbidden")){w.discard();return;}
        if(n.contains("GameOldHand")){w.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.of(n.getCompound("GameOldHand")));n.remove("GameOldHand");}
        w.setInvulnerable(n.getBoolean("WybieOldInvulnerable"));w.setSilent(n.getBoolean("WybieOldSilent"));w.setMaxUpStep(1);
        if(n.contains("WybieOldNoAi")){w.setNoAi(n.getBoolean("WybieOldNoAi"));w.setNoGravity(n.getBoolean("WybieOldNoGravity"));w.noPhysics=n.getBoolean("WybieOldNoPhysics");}
        var range=w.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);if(range!=null&&n.contains("WybieOldFollowRange"))range.setBaseValue(n.getDouble("WybieOldFollowRange"));
        if(n.contains("WybieOldDoorDuck"))w.partyDoorDuck(n.getBoolean("WybieOldDoorDuck"));n.remove("WybieOldDoorDuck");
        for(String key:List.of("WybieCompanionOwner","WybieCompanionToken","WybieCompanionSerial","WybieCompanionMode","WybieCreated","WybieOldInvulnerable","WybieOldSilent","WybieOldNoAi","WybieOldNoGravity","WybieOldNoPhysics","WybieOldFollowRange","WybieFarewell","WybieRetiring","WybieRetireTarget","GameNightOwner","GameNightCreated","GameOldInvulnerable"))n.remove(key);
    }
    /** Added to CharacterEntity once; suppresses idle AI without turning off navigation. */
    public static final class CompanionGoal extends Goal {
        private final CharacterEntity actor;
        public CompanionGoal(CharacterEntity actor){this.actor=actor;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
        // Equal-priority MOVE/LOOK goals cannot preempt each other. Game night
        // has its own controller; a running companion goal used to starve it.
        @Override public boolean canUse(){return actor.getPersistentData().hasUUID("WybieCompanionOwner")&&!actor.getPersistentData().hasUUID("GameNightOwner");}
        @Override public boolean requiresUpdateEveryTick(){return true;}
        @Override public void tick(){
            var n=actor.getPersistentData();var s=VisitData.get(actor.getServer()).scenes.get(n.getUUID("WybieCompanionOwner"));var p=actor.getServer().getPlayerList().getPlayer(n.getUUID("WybieCompanionOwner"));
            if(n.getBoolean("WybieRetiring")){leaving(actor,p);return;}
            if(s==null||!s.owner.equals(n.getUUID("WybieCompanionOwner"))||!n.hasUUID("RealmDoor")||!s.door.equals(n.getUUID("RealmDoor"))||!s.loop.story.hasUUID("WybieLeaseToken")||!n.hasUUID("WybieCompanionToken")||!s.loop.story.getUUID("WybieLeaseToken").equals(n.getUUID("WybieCompanionToken"))||s.loop.story.getBoolean("WybieVisitsForbidden")&&!n.getBoolean("WybieFarewell")||n.getInt("WybieCompanionSerial")!=s.loop.event.getInt("Serial")){release(actor);return;}
            if(p==null||!p.isAlive()||p.level()!=actor.level()){actor.stopRiding();actor.getNavigation().stop();return;}if(actor.isInWaterOrBubble())actor.getJumpControl().jump();
            // Owner controller drives pathfinding, the goal only reserves MOVE/LOOK.
            if(n.getString("WybieCompanionMode").equals("WAIT"))actor.getNavigation().stop();
        }
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p){var s=OtherMotherLoop.scene(p);if(s!=null&&VisitEvents.type(s)==VisitEvents.Type.FAMILY_GAME_NIGHT&&s.loop.event.getBoolean("GameNightPending")){FamilyGameNight.suspend(p,s);OtherMotherLoop.changed(p);}}}
    private WybieOptionalVisits(){}
}
