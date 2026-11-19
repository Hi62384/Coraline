package com.coraline.feast;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.*;
import com.coraline.otherworld.OtherWorld;

/** Reversible owner-qualified transitions. An unloaded/failed guest remains in the saved rollback ledger. */
public final class ShowTravel {
    private static CompoundTag origin(Entity entity){var n=new CompoundTag();n.putString("Dimension",entity.level().dimension().location().toString());n.putDouble("X",entity.getX());n.putDouble("Y",entity.getY());n.putDouble("Z",entity.getZ());n.putFloat("Yaw",entity.getYRot());n.putFloat("Pitch",entity.getXRot());return n;}
    /** Preserve the saved sub-block anchor only when its original footing is still safe. */
    private static Vec3 returnPosition(ServerLevel level,Entity actor,CompoundTag saved){
        var original=new Vec3(saved.getDouble("X"),saved.getDouble("Y"),saved.getDouble("Z"));
        if(!Double.isFinite(original.x)||!Double.isFinite(original.y)||!Double.isFinite(original.z))return null;
        var block=BlockPos.containing(original);level.getChunkAt(block);
        var safe=OtherWorld.safePlayer(level,block);if(safe==null)return null;
        if(safe.equals(block)&&Math.abs(original.y-safe.getY())<.001
                &&level.noCollision(actor,actor.getDimensions(actor.getPose()).makeBoundingBox(original)))return original;
        var fallback=Vec3.atBottomCenterOf(safe);
        return level.noCollision(actor,actor.getDimensions(actor.getPose()).makeBoundingBox(fallback))?fallback:null;
    }
    /**
     * Rebuild a missing player return lease only from a known, safe Other World
     * venue entrance. A checkpoint reconstructed from the player's current
     * Overworld position would strand the next theatre/circus exit.
     */
    static boolean recoverRescueReturn(ServerPlayer p,VisitData.Scene scene,String key,BlockPos returnNear){
        if(scene==null||!scene.owner.equals(p.getUUID())||!p.isAlive()
                ||!(key.equals("TheatreReturn")||key.equals("CircusReturn"))||returnNear==null)return false;
        var source=p.server.getLevel(OtherWorld.KEY);
        if(source==null)return false;
        if(scene.loop.event.contains(key)){
            if(!scene.loop.event.contains(key,Tag.TAG_COMPOUND))return false;
            var saved=scene.loop.event.getCompound(key);
            // Never silently replace an existing return ticket belonging elsewhere.
            return source.dimension().location().toString().equals(saved.getString("Dimension"))
                    &&returnPosition(source,p,saved)!=null;
        }
        source.getChunkAt(returnNear);
        var safe=OtherWorld.safePlayer(source,returnNear);
        if(safe==null||safe.distSqr(returnNear)>25)return false;
        var point=Vec3.atBottomCenterOf(safe);
        var body=p.getDimensions(Pose.STANDING).makeBoundingBox(point);
        if(!source.getWorldBorder().isWithinBounds(safe)||source.containsAnyLiquid(body)
                ||!source.noCollision(p,body)
                ||!source.getBlockState(safe.below()).isFaceSturdy(source,safe.below(),Direction.UP))return false;
        var record=new CompoundTag();
        record.putString("Dimension",source.dimension().location().toString());
        record.putDouble("X",point.x);record.putDouble("Y",point.y);record.putDouble("Z",point.z);
        record.putFloat("Yaw",p.getYRot());record.putFloat("Pitch",p.getXRot());
        scene.loop.event.put(key,record);OtherMotherLoop.changed(p);
        try{
            p.server.overworld().getDataStorage().save();
            var file=p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                    .resolve("data/coraline_first_visits.dat");
            var disk=VisitData.load(com.coraline.rebirth.StoryRunStore.read(file).getCompound("data")).scenes.get(scene.owner);
            if(disk!=null&&scene.door.equals(disk.door)&&record.equals(disk.loop.event.getCompound(key)))return true;
        }catch(java.io.IOException|RuntimeException unavailable){
            // Leave the player at the source until the rollback checkpoint persists.
        }
        scene.loop.event.remove(key);OtherMotherLoop.changed(p);
        return false;
    }
    private static CompoundTag ledger(VisitData.Scene s){if(!s.loop.event.contains("ShowTravelGuests"))s.loop.event.put("ShowTravelGuests",new CompoundTag());return s.loop.event.getCompound("ShowTravelGuests");}
    private static void track(VisitData.Scene s,Mob actor,String key){var n=new CompoundTag();n.putUUID("Entity",actor.getUUID());n.putString("Key",key);ledger(s).put(key+":"+actor.getUUID(),n);}
    private static void untrack(VisitData.Scene s,UUID id,String key){ledger(s).remove(key+":"+id);}
    public static boolean enter(ServerPlayer p,VisitData.Scene s,String key,BlockPos destination){
        if(!s.owner.equals(p.getUUID())||!p.isAlive())return false;var level=p.server.getLevel(ActivityWorld.KEY);if(level==null)return false;level.getChunkAt(destination);var safe=OtherWorld.safePlayer(level,destination);if(safe==null)return false;
        if(!s.loop.event.contains(key))s.loop.event.put(key,origin(p));OtherMotherLoop.changed(p);p.stopRiding();p.teleportTo(level,safe.getX()+.5,safe.getY(),safe.getZ()+.5,180,0);p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);s.loop.event.putInt("ShowPortalCooldown",40);return true;
    }
    public static boolean exit(ServerPlayer p,VisitData.Scene s,String key){
        if(!s.owner.equals(p.getUUID())||!p.isAlive()||!s.loop.event.contains(key))return false;var n=s.loop.event.getCompound(key);var id=ResourceLocation.tryParse(n.getString("Dimension"));if(id==null)return false;var level=p.server.getLevel(ResourceKey.create(Registries.DIMENSION,id));if(level==null)return false;
        var at=returnPosition(level,p,n);if(at==null)return false;p.stopRiding();p.teleportTo(level,at.x,at.y,at.z,n.getFloat("Yaw"),n.getFloat("Pitch"));p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);s.loop.event.putInt("ShowPortalCooldown",60);OtherMotherLoop.changed(p);return true;
    }
    public static Mob guest(Mob actor,ServerPlayer p,VisitData.Scene s,ServerLevel target,Vec3 at,boolean save,String key){
        if(actor instanceof com.coraline.entity.CharacterEntity original&&(com.coraline.postgame.WybieRescue.reserved(original)||com.coraline.postgame.WybieRescue.guide(original)))return null;
        if(actor==null||target==null||!s.owner.equals(p.getUUID()))return null;var tag=actor.getPersistentData();
        if(tag.hasUUID("ShowTravelOwner")&&!s.owner.equals(tag.getUUID("ShowTravelOwner")))return null;
        if(tag.hasUUID("PartyOwner")&&!s.owner.equals(tag.getUUID("PartyOwner")))return null;
        if(save){if(!tag.contains(key)){tag.put(key,origin(actor));tag.putUUID("ShowTravelOwner",s.owner);}track(s,actor,key);OtherMotherLoop.changed(p);}
        actor.stopRiding();actor.getNavigation().stop();target.getChunkAt(BlockPos.containing(at));UUID oldId=actor.getUUID();Entity result=actor;
        if(actor.level()!=target)result=actor.changeDimension(target,new net.minecraftforge.common.util.ITeleporter(){@Override public Entity placeEntity(Entity entity,ServerLevel current,ServerLevel destination,float yaw,java.util.function.Function<Boolean,Entity> reposition){var transferred=reposition.apply(false);if(transferred!=null)transferred.setPos(at);return transferred;}});
        if(!(result instanceof Mob moved))return null;moved.setPos(at);moved.fallDistance=0;moved.setDeltaMovement(Vec3.ZERO);
        if(save){untrack(s,oldId,key);track(s,moved,key);}
        if(moved.getPersistentData().hasUUID("PartyGuest"))for(var g:PartyGuests.guests(s))if(g.hasUUID("Id")&&g.getUUID("Id").equals(moved.getPersistentData().getUUID("PartyGuest")))g.putUUID("Entity",moved.getUUID());
        OtherMotherLoop.changed(p);return moved;
    }
    public static Mob returnGuest(Mob actor,ServerPlayer p,VisitData.Scene s,String key){
        if(actor==null||!s.owner.equals(p.getUUID()))return null;var tag=actor.getPersistentData();if(tag.hasUUID("ShowTravelOwner")&&!s.owner.equals(tag.getUUID("ShowTravelOwner")))return null;
        if(!tag.contains(key)){untrack(s,actor.getUUID(),key);return actor;}var n=tag.getCompound(key);var id=ResourceLocation.tryParse(n.getString("Dimension"));if(id==null)return null;var level=p.server.getLevel(ResourceKey.create(Registries.DIMENSION,id));if(level==null)return null;var at=returnPosition(level,actor,n);if(at==null)return null;UUID oldId=actor.getUUID();var result=guest(actor,p,s,level,at,false,key);
        if(result!=null){result.setYRot(n.getFloat("Yaw"));result.setXRot(n.getFloat("Pitch"));result.getPersistentData().remove(key);result.getPersistentData().remove("ShowTravelOwner");untrack(s,oldId,key);untrack(s,result.getUUID(),key);OtherMotherLoop.changed(p);}return result;
    }
    static void forgetDiscarded(ServerPlayer p,VisitData.Scene s,Mob actor,String key){if(s.owner.equals(p.getUUID())){untrack(s,actor.getUUID(),key);actor.getPersistentData().remove(key);actor.getPersistentData().remove("ShowTravelOwner");OtherMotherLoop.changed(p);}}
    public static boolean returnAll(ServerPlayer p,VisitData.Scene s,String key){
        if(!s.owner.equals(p.getUUID()))return false;var all=ledger(s);
        // Include older staged saves that predate the explicit rollback ledger.
        for(var level:p.server.getAllLevels())for(var entity:level.getAllEntities())if(entity instanceof Mob mob&&mob.getPersistentData().hasUUID("ShowTravelOwner")&&s.owner.equals(mob.getPersistentData().getUUID("ShowTravelOwner"))&&mob.getPersistentData().contains(key))track(s,mob,key);
        boolean complete=true;for(String entry:new HashSet<>(all.getAllKeys())){var n=all.getCompound(entry);if(!n.getString("Key").equals(key))continue;Mob actor=null;for(var level:p.server.getAllLevels())if(level.getEntity(n.getUUID("Entity")) instanceof Mob found){actor=found;break;}if(actor==null||returnGuest(actor,p,s,key)==null)complete=false;}return complete;
    }
    private ShowTravel(){}
}
