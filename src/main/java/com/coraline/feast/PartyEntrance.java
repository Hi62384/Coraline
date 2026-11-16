package com.coraline.feast;

import com.coraline.parents.ParentEntity;
import com.coraline.secrets.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Physical front-door admissions for Mother's party, not an automatic door-opening goal. */
@Mod.EventBusSubscriber(modid="coraline")
public final class PartyEntrance {
    public static void prepare(ServerPlayer p,VisitData.Scene s){
        var n=HouseParty.data(s);if(n.contains("PartyFrontDoor"))return;
        var realm=SecretWorld.realm(p);if(realm==null||realm.house==null||realm.porch==null)return;
        BlockPos best=null;double distance=Double.MAX_VALUE;
        for(var at:BlockPos.betweenClosed(realm.porch.offset(-6,-2,-6),realm.porch.offset(6,4,8))){
            if(!p.serverLevel().hasChunkAt(at))continue;var state=p.serverLevel().getBlockState(at);
            if(!(state.getBlock() instanceof DoorBlock)||state.getValue(DoorBlock.HALF)!=DoubleBlockHalf.LOWER)continue;
            if(at.distSqr(realm.porch)<distance){best=at.immutable();distance=at.distSqr(realm.porch);}
        }
        if(best==null)return;
        var inward=p.serverLevel().getBlockState(best).getValue(DoorBlock.FACING);
        if(best.relative(inward).distSqr(realm.house)>best.relative(inward.getOpposite()).distSqr(realm.house))inward=inward.getOpposite();
        n.putLong("PartyFrontDoor",best.asLong());n.putString("PartyInward",inward.getName());
    }
    private static Direction inward(CompoundTag n){var d=Direction.byName(n.getString("PartyInward"));return d!=null&&d.getAxis().isHorizontal()?d:Direction.SOUTH;}
    /** Once welcomed, guests may use the whole house, but not drift back onto the porch. */
    static boolean keepInside(Mob actor,ServerPlayer p,VisitData.Scene s,CompoundTag g){
        if(PlayerParty.isPlayerParty(s)||!g.getBoolean("Admitted")||java.util.Set.of("LEAVING","GONE","ARRIVING","WAITING_AT_DOOR","ENTERING").contains(g.getString("State")))return false;
        if(HouseParty.insideHouse(p,actor.position())){if(actor.onGround()&&actor.tickCount%10==0&&PartyRegions.allowed(p,actor.position())){g.putDouble("IndoorX",actor.getX());g.putDouble("IndoorY",actor.getY());g.putDouble("IndoorZ",actor.getZ());}return false;}
        VisitAudio.chatter(actor,false);actor.getNavigation().stop();g.remove("Target");g.remove("Group");
        if(g.contains("IndoorX")){var safe=new Vec3(g.getDouble("IndoorX"),g.getDouble("IndoorY"),g.getDouble("IndoorZ"));if(HouseParty.insideHouse(p,safe)&&p.serverLevel().noCollision(actor,actor.getDimensions(actor.getPose()).makeBoundingBox(safe))){actor.setPos(safe);g.putString("State",g.contains("FeastSeat")?"MOVING_TO_FEAST":"WANDERING");return true;}}
        // Old saves have no indoor anchor yet; re-enter without another bell/admission.
        arrive(actor,p,s,g);return true;
    }
    private static boolean frontLeaf(ServerPlayer p,BlockPos primary,BlockPos candidate){var first=p.serverLevel().getBlockState(primary);var state=p.serverLevel().getBlockState(candidate);return first.getBlock() instanceof DoorBlock&&state.is(first.getBlock())&&state.getValue(DoorBlock.HALF)==DoubleBlockHalf.LOWER&&state.getValue(DoorBlock.FACING).getAxis()==first.getValue(DoorBlock.FACING).getAxis();}
    private static boolean open(ServerPlayer p,BlockPos door,Direction in){for(int side=-1;side<=1;side++){var leaf=door.relative(in.getClockWise(),side);if(frontLeaf(p,door,leaf)&&p.serverLevel().getBlockState(leaf).getValue(DoorBlock.OPEN))return true;}return false;}
    private static void setDoors(ServerPlayer p,CompoundTag n,net.minecraft.world.entity.LivingEntity opener,boolean value){
        var door=BlockPos.of(n.getLong("PartyFrontDoor"));
        for(int side=-1;side<=1;side++){var leaf=door.relative(inward(n).getClockWise(),side);if(frontLeaf(p,door,leaf))((DoorBlock)p.serverLevel().getBlockState(leaf).getBlock()).setOpen(opener,p.serverLevel(),p.serverLevel().getBlockState(leaf),leaf,value);}
        if(value){n.putBoolean("PartyAdmissionOpen",true);n.remove("PartyCloseAt");}
        else{n.remove("PartyAdmissionOpen");n.remove("PartyCloseAt");}
    }
    public static void tick(ServerPlayer p,VisitData.Scene s){
        var n=HouseParty.data(s);if(!n.contains("PartyFrontDoor")||!n.getBoolean("PartyAdmissionOpen"))return;
        if(n.getInt("WaveRemaining")>0||PartyGuests.guests(s).stream().anyMatch(g->java.util.Set.of("ARRIVING","WAITING_AT_DOOR","ENTERING").contains(g.getString("State"))&&PartyGuests.entity(p,g)!=null)){n.remove("PartyCloseAt");return;}
        long now=n.getLong("Clock");if(!n.contains("PartyCloseAt")){n.putLong("PartyCloseAt",now+20);return;}
        if(now<n.getLong("PartyCloseAt"))return;
        var at=BlockPos.of(n.getLong("PartyFrontDoor"));
        for(int side=-1;side<=1;side++){var leaf=at.relative(inward(n).getClockWise(),side);if(frontLeaf(p,at,leaf)&&!p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,new net.minecraft.world.phys.AABB(leaf).expandTowards(0,1,0).inflate(.35,0,.35)).isEmpty())return;}
        setDoors(p,n,null,false);OtherMotherLoop.changed(p);
    }
    static boolean admitted(CompoundTag guest,boolean open,long openedAt){return guest.contains("RangAt")&&open&&(guest.getBoolean("HostAdmitted")||openedAt>=guest.getLong("RangAt"));}
    public static void arrive(Mob actor,ServerPlayer p,VisitData.Scene s,CompoundTag g){
        prepare(p,s);var n=HouseParty.data(s);if(!n.contains("PartyFrontDoor")){actor.getNavigation().stop();return;}
        var door=BlockPos.of(n.getLong("PartyFrontDoor"));var in=inward(n);long now=n.getLong("Clock");
        if(actor.getNavigation() instanceof GroundPathNavigation nav){nav.setCanOpenDoors(false);nav.setCanPassDoors(true);}
        if(!g.getBoolean("Admitted")){
            var outside=door.relative(in,-2).relative(in.getClockWise(),g.getInt("Order")%3-1);
            if(actor.distanceToSqr(Vec3.atBottomCenterOf(outside))>4){if(actor.tickCount%20==0)actor.getNavigation().moveTo(outside.getX()+.5,outside.getY(),outside.getZ()+.5,.95);return;}
            actor.getNavigation().stop();actor.getLookControl().setLookAt(door.getX()+.5,door.getY()+1,door.getZ()+.5);
            if(open(p,door,in)){g.putBoolean("Admitted",true);g.putString("State","ENTERING");}
            if(!g.getBoolean("Admitted")){
            g.putString("State","WAITING_AT_DOOR");
            if(!g.contains("RangAt")||now>=g.getLong("NextRing")){
                p.serverLevel().playSound(null,door,SoundEvents.NOTE_BLOCK_BELL.value(),SoundSource.BLOCKS,1.2F,1);
                if(!g.contains("RangAt")){g.putLong("RangAt",now);p.displayClientMessage(net.minecraft.network.chat.Component.literal(g.getString("Name")+" is ringing the front doorbell."),true);}
                g.putLong("NextRing",now+240);
            }
            boolean open=open(p,door,in);
            long openedAt=n.contains("PartyOwnerOpenedAt")?n.getLong("PartyOwnerOpenedAt"):-1;
            if(!admitted(g,open,openedAt))return;
            g.putBoolean("Admitted",true);g.putString("State","ENTERING");
            }
        }
        if(actor instanceof com.coraline.entity.CharacterEntity cast)cast.partyDoorDuck(true);
        double depth=actor.position().subtract(Vec3.atBottomCenterOf(door)).dot(Vec3.atLowerCornerOf(in.getNormal()));
        if(depth>2.5&&PartyRegions.allowed(p,actor.position())){if(actor instanceof com.coraline.entity.CharacterEntity cast)cast.partyDoorDuck(false);g.putString("State","WANDERING");g.remove("Target");PartyGuests.gesture(actor,1,40);return;}
        var inside=door.relative(in,4);
        if(actor.tickCount%8==0){actor.setNoAi(false);actor.setNoGravity(false);actor.noPhysics=false;actor.getNavigation().moveTo(inside.getX()+.5,inside.getY(),inside.getZ()+.5,1.1);}
    }
    public static boolean host(ParentEntity mother,ServerPlayer p,VisitData.Scene s){
        if(PlayerParty.isPlayerParty(s))return false;var n=HouseParty.data(s);
        if(!n.contains("PartyFrontDoor"))return false;
        var waiting=PartyGuests.guests(s).stream().filter(g->g.getString("State").equals("WAITING_AT_DOOR")&&g.contains("RangAt")&&!g.getBoolean("Admitted")&&PartyGuests.entity(p,g)!=null).toList();
        if(waiting.isEmpty())return false;
        long now=n.getLong("Clock");if(waiting.stream().allMatch(g->now-g.getLong("RangAt")<60))return false;
        var door=BlockPos.of(n.getLong("PartyFrontDoor"));var inside=door.relative(inward(n),2);
        if(mother.distanceToSqr(Vec3.atBottomCenterOf(door))>9){if(mother.tickCount%20==0)mother.getNavigation().moveTo(inside.getX()+.5,inside.getY(),inside.getZ()+.5,1.05);return true;}
        mother.getNavigation().stop();mother.getLookControl().setLookAt(door.getX()+.5,door.getY()+1,door.getZ()+.5);
        var state=p.serverLevel().getBlockState(door);
        if(!(state.getBlock() instanceof DoorBlock block))return true;
        setDoors(p,n,mother,true);
        for(var g:waiting)g.putBoolean("HostAdmitted",true);
        OtherMotherLoop.changed(p);return true;
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void opened(PlayerInteractEvent.RightClickBlock e){
        if(e.isCanceled()||e.getHand()!=net.minecraft.world.InteractionHand.MAIN_HAND||!(e.getEntity() instanceof ServerPlayer p))return;
        var s=HouseParty.active(p);if(s==null||PlayerParty.isPlayerParty(s))return;
        var n=HouseParty.data(s);if(!n.contains("PartyFrontDoor"))return;var at=BlockPos.of(n.getLong("PartyFrontDoor"));
        var clicked=e.getPos();var clickedState=p.serverLevel().getBlockState(clicked);if(clickedState.getBlock() instanceof DoorBlock&&clickedState.getValue(DoorBlock.HALF)==DoubleBlockHalf.UPPER)clicked=clicked.below();
        boolean matches=false;for(int side=-1;side<=1;side++)if(clicked.equals(at.relative(inward(n).getClockWise(),side))&&frontLeaf(p,at,clicked))matches=true;
        if(matches&&p.distanceToSqr(at.getCenter())<36){
            var state=p.serverLevel().getBlockState(clicked);
            if(state.getBlock() instanceof DoorBlock){
                boolean opening=!state.getValue(DoorBlock.OPEN);
                if(opening)n.putLong("PartyOwnerOpenedAt",n.getLong("Clock"));
                setDoors(p,n,p,opening);OtherMotherLoop.changed(p);
                e.setCanceled(true);e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            }
        }
    }
    private PartyEntrance(){}
}
