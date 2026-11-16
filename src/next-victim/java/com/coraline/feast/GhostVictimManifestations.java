package com.coraline.feast;

import com.coraline.tycoon.TycoonNpc;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.phys.Vec3;

/** Bounded real light/object/food effects at authored owner-local points. No
 * trust is awarded by an arbitrary button without a corresponding world effect.
 * Display entities contain no obtainable items; effects restore only their own
 * expected states and never overwrite another player's block modifications. */
public final class GhostVictimManifestations {
    private static CompoundTag points(CompoundTag n){if(!n.contains("Points"))n.put("Points",new CompoundTag());return n.getCompound("Points");}
    public static boolean display(ServerPlayer p,VisitData.Scene s,CompoundTag n,String id,Vec3 at,GhostVictimProgress.Disturbance kind,ItemStack normal){
        if(!GhostVictimMirrors.owned(p,s,n)||id==null||!id.matches("[a-z_]{1,60}")||kind==null||normal.isEmpty())return false;
        var all=points(n);var row=all.getCompound(id);var level=AcceptedButtonEyesWorld.level(p);if(level==null)return false;
        if(row.hasUUID("Entity")){level.getChunkAt(BlockPos.of(row.getLong("Position")));return row.getLong("Position")==BlockPos.containing(at).asLong()&&kind.name().equals(row.getString("Kind"))&&actor(level,row,s,n)!=null;}
        var actor=EntityType.ITEM_DISPLAY.create(level);if(actor==null)return false;
        var visual=new CompoundTag();visual.put("item",normal.save(new CompoundTag()));visual.putString("item_display","ground");actor.load(visual);actor.setPos(at);actor.setNoGravity(true);
        actor.getPersistentData().putUUID("GhostManifestationOwner",s.owner);actor.getPersistentData().putUUID("GhostManifestationDoor",s.door);
        actor.getPersistentData().putUUID("GhostManifestationSession",n.getUUID("Session"));
        if(!level.addFreshEntity(actor))return false;
        row.putUUID("Entity",actor.getUUID());row.putLong("Position",BlockPos.containing(at).asLong());row.putDouble("X",at.x);row.putDouble("Y",at.y);row.putDouble("Z",at.z);
        row.putString("Kind",kind.name());row.put("NormalItem",normal.save(new CompoundTag()));all.put(id,row);OtherMotherLoop.changed(p);return true;
    }
    public static boolean light(ServerPlayer p,VisitData.Scene s,CompoundTag n,String id,BlockPos actualLight){
        if(!GhostVictimMirrors.owned(p,s,n)||id==null||!id.matches("[a-z_]{1,60}"))return false;
        if(points(n).contains(id))return points(n).getCompound(id).getLong("Position")==actualLight.asLong()&&points(n).getCompound(id).getString("Kind").equals("LIGHT");
        var level=AcceptedButtonEyesWorld.level(p);if(level==null||!level.hasChunkAt(actualLight)||!(level.getBlockState(actualLight).getBlock() instanceof LightBlock)||level.getBlockState(actualLight).getValue(LightBlock.LEVEL)<=0)return false;
        var row=new CompoundTag();row.putLong("Position",actualLight.asLong());row.putString("Kind",GhostVictimProgress.Disturbance.LIGHT.name());row.put("Original",NbtUtils.writeBlockState(level.getBlockState(actualLight)));
        points(n).put(id,row);return true;
    }
    private static Display.ItemDisplay actor(ServerLevel level,CompoundTag row,VisitData.Scene s,CompoundTag n){
        if(!row.hasUUID("Entity"))return null;var entity=level.getEntity(row.getUUID("Entity"));
        if(!(entity instanceof Display.ItemDisplay display))return null;var tag=display.getPersistentData();
        return tag.hasUUID("GhostManifestationOwner")&&s.owner.equals(tag.getUUID("GhostManifestationOwner"))&&tag.hasUUID("GhostManifestationDoor")&&s.door.equals(tag.getUUID("GhostManifestationDoor"))
            &&n.hasUUID("Session")&&tag.hasUUID("GhostManifestationSession")&&n.getUUID("Session").equals(tag.getUUID("GhostManifestationSession"))?display:null;
    }
    private static void item(Display.ItemDisplay actor,ItemStack item){var saved=actor.saveWithoutId(new CompoundTag());saved.put("item",item.save(new CompoundTag()));actor.load(saved);}
    public static boolean perform(ServerPlayer p,VisitData.Scene s,CompoundTag n,String id,TycoonNpc child){
        if(!GhostVictimMirrors.owned(p,s,n)||!points(n).contains(id)||!GhostVictimActors.owns(child,s,n))return false;
        var row=points(n).getCompound(id);var level=AcceptedButtonEyesWorld.level(p);
        if(level==null||p.level()!=level||!p.isSpectator()||!level.hasChunkAt(BlockPos.of(row.getLong("Position"))))return false;
        var position=BlockPos.of(row.getLong("Position"));var target=position.getCenter();var delta=target.subtract(p.getEyePosition());
        if(delta.lengthSqr()>36||delta.lengthSqr()<.001||p.getLookAngle().dot(delta.normalize())<.92)return false;
        var phase=GhostVictimProgress.phase(n);
        if(phase!=GhostVictimProgress.Phase.FIRST_SUPPER&&phase!=GhostVictimProgress.Phase.EXPLORATION&&phase!=GhostVictimProgress.Phase.DAILY_GIFT&&phase!=GhostVictimProgress.Phase.ACTIVITY)return false;
        GhostVictimProgress.Disturbance kind;try{kind=GhostVictimProgress.Disturbance.valueOf(row.getString("Kind"));}catch(IllegalArgumentException invalid){return false;}
        var display=actor(level,row,s,n);
        if(kind==GhostVictimProgress.Disturbance.LIGHT){if(!(level.getBlockState(position).getBlock() instanceof LightBlock)||row.contains("EffectUntil"))return false;}
        else if(display==null||row.contains("EffectUntil"))return false;
        boolean witnessed=visible(child,target,10,130);var mother=AcceptedButtonEyesWorld.mother(p,s);
        boolean motherSaw=mother!=null&&visible(mother,target,14,120);
        var traits=GhostVictimProgress.child(n).getCompound("Traits");boolean blame=GhostVictimProgress.remembers(n,"ghost_threat")||GhostVictimProgress.ghostTrust(n)<.2&&traits.getDouble("Sensitivity")>.65;
        boolean reports=witnessed&&blame&&mother!=null&&GhostVictimNavigation.canSee(child,mother,12,180);
        var observation=new GhostVictimProgress.Observation(witnessed,motherSaw,blame,GhostVictimProgress.child(n).getInt("EvidenceCount")>0,reports);
        var snapshot=n.copy();var result=GhostVictimProgress.disturb(n,id,kind,observation,true,GhostVictimConfig.tuning());if(!result.performed())return false;
        boolean applied=true;
        if(kind==GhostVictimProgress.Disturbance.LIGHT){var old=level.getBlockState(position);row.put("EffectOriginal",NbtUtils.writeBlockState(old));var dark=old.setValue(LightBlock.LEVEL,0);applied=level.setBlock(position,dark,3);row.put("Expected",NbtUtils.writeBlockState(dark));}
        else if(kind==GhostVictimProgress.Disturbance.FOOD||kind==GhostVictimProgress.Disturbance.ILLUSION||kind==GhostVictimProgress.Disturbance.GIFT_SECRET)item(display,new ItemStack(Items.ROTTEN_FLESH));
        if(!applied){for(String key:new java.util.HashSet<>(n.getAllKeys()))n.remove(key);n.merge(snapshot);return false;}
        row.putLong("EffectStart",GhostVictimProgress.clock(n));row.putLong("EffectUntil",GhostVictimProgress.clock(n)+(kind==GhostVictimProgress.Disturbance.LIGHT?32:100));
        if(witnessed){n.putString("ChildWords",result.reaction());n.putLong("ChildTalkingUntil",GhostVictimProgress.clock(n)+80);if(result.investigate())GhostVictimRoutines.investigate(n,position);}
        if(motherSaw||reports){n.putLong("MotherInvestigation",position.asLong());n.putLong("MotherInvestigateUntil",GhostVictimProgress.clock(n)+200);}
        OtherMotherLoop.changed(p);return true;
    }
    private static boolean visible(LivingEntity observer,Vec3 target,double distance,double fov){
        Vec3 eye=observer.getEyePosition(),delta=target.subtract(eye);if(delta.lengthSqr()>distance*distance)return false;
        if(delta.lengthSqr()>.001&&observer.getLookAngle().dot(delta.normalize())<Math.cos(Math.toRadians(fov/2)))return false;
        return observer.level().clip(new net.minecraft.world.level.ClipContext(eye,target,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,observer)).getType()==net.minecraft.world.phys.HitResult.Type.MISS;
    }
    public static void tick(ServerPlayer p,VisitData.Scene s,CompoundTag n){
        var level=AcceptedButtonEyesWorld.level(p);if(level==null||!GhostVictimMirrors.owned(p,s,n))return;
        for(String id:points(n).getAllKeys()){
            var row=points(n).getCompound(id);if(!row.contains("EffectUntil"))continue;var pos=BlockPos.of(row.getLong("Position"));if(!level.hasChunkAt(pos))continue;
            long age=GhostVictimProgress.clock(n)-row.getLong("EffectStart");boolean done=GhostVictimProgress.clock(n)>=row.getLong("EffectUntil");
            if(row.getString("Kind").equals("LIGHT")){
                var expected=NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK),row.getCompound("Expected"));
                if(level.getBlockState(pos).equals(expected)){
                    var original=NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK),row.getCompound("EffectOriginal"));
                    var next=done?original:original.setValue(LightBlock.LEVEL,age%8<4?0:original.getValue(LightBlock.LEVEL));level.setBlock(pos,next,3);row.put("Expected",NbtUtils.writeBlockState(next));
                }else done=true;
            }else{
                var display=actor(level,row,s,n);if(display==null)done=true;
                else if(done){display.setPos(row.getDouble("X"),row.getDouble("Y"),row.getDouble("Z"));item(display,ItemStack.of(row.getCompound("NormalItem")));}
                else if(row.getString("Kind").equals("OBJECT")||row.getString("Kind").equals("KNIFE")||row.getString("Kind").equals("GIFT_FRIGHT"))display.setPos(row.getDouble("X")+Math.sin(age*.14)*.3,row.getDouble("Y")+Math.sin(Math.min(1,age/100.)*Math.PI)*.55,row.getDouble("Z"));
            }
            if(done){row.remove("EffectUntil");row.remove("EffectStart");row.remove("Expected");row.remove("EffectOriginal");}
        }
    }
    private GhostVictimManifestations(){}
}
