package com.coraline.feast;

import com.coraline.entity.CharacterEntity;
import com.coraline.otherworld.MouseEntity;
import com.coraline.secrets.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import java.util.*;

/** Read-only perception over actual surveillance, four-tier boxes and generated secret structures. */
public final class WybieDiscoveries {
    private record Hint(Vec3 position,String kind,String id,Entity entity){}
    public static void tick(ServerPlayer p,VisitData.Scene s,CharacterEntity w,CompoundTag lease){
        if(w.level()!=p.level()||!w.isAlive()||w.distanceToSqr(p)>32*32)return;long now=OtherMotherLoop.now(p);
        // Keep the brief pointing pause after the follow controller issues its next movement.
        if(now<lease.getLong("DiscoveryPauseUntil"))pause(w);
        if(p.tickCount%40!=0)return;
        var spies=p.serverLevel().getEntitiesOfClass(Entity.class,w.getBoundingBox().inflate(18),e->{var n=e.getPersistentData();return n.hasUUID("SecretOwner")&&p.getUUID().equals(n.getUUID("SecretOwner"))&&n.getInt("SecretVisit")==s.loop.secrets.visit&&((e instanceof MouseEntity rat&&rat.spying()&&n.getString("SpyKind").equals("rat"))||(e instanceof Display.ItemDisplay&&n.getString("SpyKind").equals("doll")));});
        var visibleSpies=spies.stream().filter(Entity::isAlive).filter(e->visible(p,w,e.getBoundingBox().getCenter(),18,false)).toList();
        var warnings=lease.getCompound("SpyWarnings");
        // Migrate the previous one-target cooldown without re-announcing it on reload.
        if(!lease.getString("LastSpy").isEmpty()&&now<lease.getLong("SpyAfter"))warnings.putLong(lease.getString("LastSpy"),lease.getLong("SpyAfter"));
        for(String id:new ArrayList<>(warnings.getAllKeys()))if(now>=warnings.getLong(id))warnings.remove(id);
        lease.put("SpyWarnings",warnings);
        var danger=visibleSpies.stream().filter(e->now>=warnings.getLong(e.getUUID().toString())).min(Comparator.comparingDouble(e->e.distanceToSqr(p))).orElse(null);
        // A treasure/home gesture must never suppress a newly visible spy. Cool down each
        // actual entity independently, so a familiar nearby rat cannot mask a new doll.
        if(danger!=null&&now>=lease.getLong("SpyGestureAfter")){hint(p,w,lease,new Hint(danger.getBoundingBox().getCenter(),danger.getPersistentData().getString("SpyKind"),danger.getUUID().toString(),danger),now,120);return;}
        if(!visibleSpies.isEmpty()||now<lease.getLong("GestureAfter"))return;
        var ahead=BlockPos.containing(p.position().add(p.getLookAngle().multiply(1,0,1).normalize().scale(3)));
        if(p.serverLevel().hasChunkAt(ahead)){
            boolean supported=false;for(int down=1;down<=3;down++)if(!p.serverLevel().getBlockState(ahead.below(down)).getCollisionShape(p.serverLevel(),ahead.below(down)).isEmpty())supported=true;
            if(!supported||!p.serverLevel().getFluidState(ahead).isEmpty()){w.getLookControl().setLookAt(ahead.getX()+.5,ahead.getY(),ahead.getZ()+.5,30,20);w.gameGesture(11,50);lease.putLong("GestureAfter",now+240);return;}
        }
        var realm=SecretWorld.realm(p);if(realm!=null&&realm.house!=null){
            var adventure=WybieAdventure.active(p);int time=adventure==null?Integer.MAX_VALUE:WybieAdventure.data(adventure).getInt("Remaining");
            double distance=Math.sqrt(p.distanceToSqr(realm.house.getCenter()));double threshold=Math.min(240,Math.max(80,realm.radius*.35));
            if(time<=2400||distance>threshold||SecretDialogue.secretArea(p,p.blockPosition())){
                w.getLookControl().setLookAt(realm.house.getX()+.5,realm.house.getY()+1,realm.house.getZ()+.5,30,20);w.gameGesture(time<=1200?11:10,65);lease.putLong("GestureAfter",now+(time<=1200?200:500));return;
            }
        }
        var seen=lease.getCompound("Discoveries");Hint selected=null;
        // The source map is capped at 360 boxes; inspect every five seconds at most.
        if(now>=lease.getLong("DiscoveryAfter")){
            lease.putLong("DiscoveryAfter",now+100);var boxes=s.loop.secrets.world.getCompound("WildernessLootBoxes");
            for(String key:boxes.getAllKeys()){
                if(seen.contains("box:"+key)||s.loop.secrets.claimed.contains("wilderness_box:"+key))continue;
                BlockPos at;try{at=BlockPos.of(Long.parseLong(key));}catch(NumberFormatException ignored){continue;}
                int tier=boxes.getInt(key);if(s.loop.secrets.world.getInt("WildernessLootTierFormat")<2)tier++;
                var block=switch(tier){case 1->Blocks.BARREL;case 2->Blocks.CHEST;case 3->Blocks.CYAN_SHULKER_BOX;case 4->Blocks.PURPLE_SHULKER_BOX;default->Blocks.AIR;};
                if(block==Blocks.AIR||!p.serverLevel().hasChunkAt(at)||!p.serverLevel().getBlockState(at).is(block)||!visible(p,w,at.getCenter(),28,true))continue;
                if(selected==null||w.distanceToSqr(at.getCenter())<w.distanceToSqr(selected.position))selected=new Hint(at.getCenter(),"loot","box:"+key,null);
            }
            if(selected==null&&SecretWorld.pending(s.loop.secrets)==0)for(var room:SecretWorld.rooms(s.loop.secrets)){
                var at=BlockPos.of(room.getLong("Pos"));String key="structure:"+at.asLong();if(seen.contains(key)||!p.serverLevel().hasChunkAt(at)||w.distanceToSqr(at.getCenter())>30*30)continue;
                // Point at the actual existing entrance or roof, never through a solid wall.
                for(var target:List.of(at.north(5).above(),at.above(5))){if(!p.serverLevel().hasChunkAt(target))continue;if(p.serverLevel().getBlockState(target).isAir()&&target.equals(at.above(5)))continue;if(!visible(p,w,target.getCenter(),30,true))continue;selected=new Hint(target.getCenter(),"structure",key,null);break;}if(selected!=null)break;
            }
        }
        if(selected!=null){seen.putLong(selected.id,now);lease.put("Discoveries",seen);hint(p,w,lease,selected,now,180);}
        else if(p.tickCount%200==0){w.getLookControl().setLookAt(p,20,15);w.gameGesture(9,40);lease.putLong("GestureAfter",now+160);}
    }
    private static boolean visible(ServerPlayer p,CharacterEntity w,Vec3 at,double range,boolean blockTarget){
        if(w.distanceToSqr(at)>range*range||p.distanceToSqr(at)>36*36||!p.serverLevel().hasChunkAt(BlockPos.containing(at)))return false;
        var delta=at.subtract(w.getEyePosition());if(delta.horizontalDistance()>.1&&w.getLookAngle().multiply(1,0,1).normalize().dot(delta.multiply(1,0,1).normalize())<-.2)return false;
        // Check the small chunk rectangle before raycasting, including diagonal corners.
        var from=BlockPos.containing(w.getEyePosition());var to=BlockPos.containing(at);
        for(int x=Math.min(from.getX(),to.getX())>>4;x<=(Math.max(from.getX(),to.getX())>>4);x++)for(int z=Math.min(from.getZ(),to.getZ())>>4;z<=(Math.max(from.getZ(),to.getZ())>>4);z++)if(!p.serverLevel().hasChunkAt(new BlockPos(x<<4,to.getY(),z<<4)))return false;
        var hit=p.serverLevel().clip(new ClipContext(w.getEyePosition(),at,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,w));
        // Containers/roofs may occlude their own centers. A rat or doll may not be
        // revealed through a thin wall merely because the hit is close to its center.
        return hit.getType()==HitResult.Type.MISS||blockTarget&&hit.getBlockPos().equals(BlockPos.containing(at));
    }
    private static void pause(CharacterEntity w){if(w.getVehicle() instanceof WybieBicycleEntity bike)bike.stop();else w.getNavigation().stop();}
    private static void hint(ServerPlayer p,CharacterEntity w,CompoundTag lease,Hint hint,long now,int cooldown){
        pause(w);lease.putLong("DiscoveryPauseUntil",now+40);w.getLookControl().setLookAt(hint.position.x,hint.position.y,hint.position.z,35,25);w.gameGesture(hint.entity!=null?11:2,65);
        lease.putLong("GestureAfter",now+cooldown);lease.putLong("HighlightUntil",now+80);lease.putString("LastHint",hint.kind);lease.putLong("HintPosition",BlockPos.containing(hint.position).asLong());
        if(hint.entity!=null){var warnings=lease.getCompound("SpyWarnings");warnings.putLong(hint.id,now+400);lease.put("SpyWarnings",warnings);lease.putString("LastSpy",hint.id);lease.putLong("SpyAfter",now+400);lease.putLong("SpyGestureAfter",now+40);}
        var packet=new CompoundTag();packet.putBoolean("WybieHint",true);packet.putString("Kind",hint.kind);packet.putDouble("X",hint.position.x);packet.putDouble("Y",hint.position.y);packet.putDouble("Z",hint.position.z);packet.putInt("Ticks",80);if(hint.entity!=null){packet.putInt("Entity",hint.entity.getId());packet.putUUID("EntityUuid",hint.entity.getUUID());}WybieAdventureNetwork.send(p,packet);
        // Never mutate glow, spy timers, Code Knowledge, lock state or loot claims.
    }
    private WybieDiscoveries(){}
}
