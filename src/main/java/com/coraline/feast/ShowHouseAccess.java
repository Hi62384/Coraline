package com.coraline.feast;

import com.coraline.otherworld.*;
import com.coraline.secrets.HouseCabinets;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.phys.*;
import java.util.*;

/** Temporarily opens only invited physical doors; key ownership, cabinets and secret knowledge stay intact. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="coraline")
public final class ShowHouseAccess {
    private static final String DEFERRED="ShowDoorCleanup";
    private static boolean owned(CompoundTag n,VisitData.Scene s){return n.hasUUID("Owner")&&s.owner.equals(n.getUUID("Owner"))&&n.hasUUID("Door")&&s.door.equals(n.getUUID("Door"));}
    private static boolean atDoor(CompoundTag n,VisitData.Scene s,String dimension,BlockPos at){return owned(n,s)&&dimension.equals(n.getString("Dimension"))&&n.getLong("Position")==at.asLong();}
    private static boolean matches(net.minecraft.server.level.ServerLevel level,BlockPos at,CompoundTag n){
        return n.contains("ExpectedLower")&&n.contains("ExpectedUpper")&&NbtUtils.writeBlockState(level.getBlockState(at)).equals(n.getCompound("ExpectedLower"))&&NbtUtils.writeBlockState(level.getBlockState(at.above())).equals(n.getCompound("ExpectedUpper"));
    }
    /** Acquire a coherent two-half door before changing either half. Pre-existing openings are untouched. */
    public static void open(ServerPlayer p,VisitData.Scene s,String roomId){
        if(!s.owner.equals(p.getUUID())||!OtherWorld.isOther(p.level()))return;
        var realm=RealmData.get(p.server).realms.get(s.door);if(realm==null||realm.house==null)return;
        var level=p.serverLevel();String dimension=level.dimension().location().toString();
        var active=s.loop.event.getList("ShowOpenedDoors",Tag.TAG_COMPOUND);var deferred=s.loop.story.getList(DEFERRED,Tag.TAG_COMPOUND);
        for(var room:HouseCabinets.ROOMS)if(room.id().equals(roomId))for(var xyz:room.doors()){
            var at=HouseCabinets.at(realm.house,xyz[0],xyz[1],xyz[2]);if(!level.hasChunkAt(at))continue;
            var state=level.getBlockState(at);if(!(state.getBlock() instanceof DoorBlock))continue;
            if(state.getValue(DoorBlock.HALF)==net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER)at=at.below();
            if(!level.hasChunkAt(at)||!level.hasChunkAt(at.above()))continue;
            state=level.getBlockState(at);if(!(state.getBlock() instanceof DoorBlock door)||state.getValue(DoorBlock.HALF)!=net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER)continue;
            CompoundTag retained=null;
            // A newer invitation inherits an unresolved opening instead of racing old cleanup.
            for(int i=deferred.size()-1;i>=0;i--){var n=deferred.getCompound(i);if(atDoor(n,s,dimension,at)){if(matches(level,at,n))retained=n;deferred.remove(i);}}
            for(int i=active.size()-1;i>=0;i--){var n=active.getCompound(i);if(atDoor(n,s,dimension,at)){if(matches(level,at,n))retained=n;active.remove(i);}}
            if(retained!=null){active.add(retained);continue;}
            if(state.getValue(DoorBlock.OPEN))continue;
            var upper=level.getBlockState(at.above());if(!state.setValue(DoorBlock.HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER).equals(upper))continue;
            var n=new CompoundTag();n.putUUID("Owner",s.owner);n.putUUID("Door",s.door);n.putInt("Serial",s.loop.event.getInt("Serial"));n.putString("Dimension",dimension);n.putLong("Position",at.asLong());
            n.put("OriginalLower",NbtUtils.writeBlockState(state));n.put("OriginalUpper",NbtUtils.writeBlockState(upper));
            n.put("ExpectedLower",NbtUtils.writeBlockState(state.setValue(DoorBlock.OPEN,true)));n.put("ExpectedUpper",NbtUtils.writeBlockState(upper.setValue(DoorBlock.OPEN,true)));
            active.add(n);s.loop.event.put("ShowOpenedDoors",active);OtherMotherLoop.changed(p);
            door.setOpen(p,level,state,at,true);
        }
        s.loop.event.put("ShowOpenedDoors",active);if(deferred.isEmpty())s.loop.story.remove(DEFERRED);else s.loop.story.put(DEFERRED,deferred);OtherMotherLoop.changed(p);
    }
    /** Transfer cleanup out of transient event state before the director advances. */
    public static void restore(ServerPlayer p,VisitData.Scene s){
        if(!s.owner.equals(p.getUUID()))return;var deferred=s.loop.story.getList(DEFERRED,Tag.TAG_COMPOUND);
        for(var t:s.loop.event.getList("ShowOpenedDoors",Tag.TAG_COMPOUND)){var n=(CompoundTag)t;
            // Legacy per-half records have no expected state; leave those doors alone.
            if(!owned(n,s)||!n.contains("ExpectedLower")||!n.contains("ExpectedUpper"))continue;
            boolean duplicate=false;for(var saved:deferred)if(saved.equals(n)){duplicate=true;break;}if(!duplicate)deferred.add(n.copy());
        }
        if(!deferred.isEmpty())s.loop.story.put(DEFERRED,deferred);s.loop.event.remove("ShowOpenedDoors");OtherMotherLoop.changed(p);recover(p,s);
    }
    private static boolean restoreDoor(ServerPlayer p,CompoundTag n){
        var id=net.minecraft.resources.ResourceLocation.tryParse(n.getString("Dimension"));if(id==null)return true;
        var level=p.server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,id));if(level==null)return false;
        var at=BlockPos.of(n.getLong("Position"));if(!level.hasChunkAt(at)||!level.hasChunkAt(at.above()))return false;
        if(!matches(level,at,n))return true;
        var state=level.getBlockState(at);var upper=level.getBlockState(at.above());
        if(!(state.getBlock() instanceof DoorBlock door)||state.getValue(DoorBlock.HALF)!=net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER||!state.getValue(DoorBlock.OPEN))return true;
        if(!level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,new AABB(at).expandTowards(0,1,0).inflate(1,1,1)).isEmpty())return false;
        door.setOpen(null,level,state,at,false);
        return state.setValue(DoorBlock.OPEN,false).equals(level.getBlockState(at))&&upper.setValue(DoorBlock.OPEN,false).equals(level.getBlockState(at.above()));
    }
    public static void recover(ServerPlayer p,VisitData.Scene s){
        if(s==null||!s.owner.equals(p.getUUID())||!s.loop.story.contains(DEFERRED))return;
        var deferred=s.loop.story.getList(DEFERRED,Tag.TAG_COMPOUND);boolean changed=false;
        for(int i=deferred.size()-1;i>=0;i--){var n=deferred.getCompound(i);if(owned(n,s)&&restoreDoor(p,n)){deferred.remove(i);changed=true;}}
        if(deferred.isEmpty()){s.loop.story.remove(DEFERRED);changed=true;}if(changed)OtherMotherLoop.changed(p);
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.PlayerTickEvent event){if(event.player instanceof net.minecraft.server.level.ServerPlayer coopPlayer&&com.coraline.multiplayer.StoryCoop.directorBlocked(coopPlayer))return;
        if(event.phase==net.minecraftforge.event.TickEvent.Phase.END&&event.player instanceof ServerPlayer p&&p.tickCount%20==0)recover(p,OtherMotherLoop.scene(p));
    }
    private static AABB authored(BlockPos house,int... xyz){var a=HouseCabinets.at(house,xyz[0],xyz[1],xyz[2]);var b=HouseCabinets.at(house,xyz[3],xyz[4],xyz[5]);return new AABB(a.getX(),a.getY(),a.getZ(),b.getX()+1,b.getY()+1,b.getZ()+1);}
    /** Envelopes from repair_rosewood_house.py, including both actual exterior flights. */
    public static List<AABB> atticRoute(ServerPlayer p,VisitData.Scene s){var r=RealmData.get(p.server).realms.get(s.door);if(r==null||r.house==null)return List.of();var h=r.house;return List.of(authored(h,19,6,8,29,18,11),authored(h,6,14,8,20,18,19),authored(h,5,14,17,9,18,53),authored(h,5,14,54,9,27,65),authored(h,4,23,55,10,27,67));}
    public static List<AABB> basementRoute(ServerPlayer p,VisitData.Scene s){var r=RealmData.get(p.server).realms.get(s.door);if(r==null||r.house==null)return List.of();return List.of(authored(r.house,25,-1,-1,28,7,12));}
    public static boolean onRoute(ServerPlayer p,VisitData.Scene s,Vec3 at,boolean attic){return s.owner.equals(p.getUUID())&&OtherWorld.isOther(p.level())&&(attic?atticRoute(p,s):basementRoute(p,s)).stream().anyMatch(b->b.inflate(.6,0,.6).contains(at));}
    private ShowHouseAccess(){}
}
