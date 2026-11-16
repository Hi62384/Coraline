package com.coraline.tea;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="coraline")
public final class TeaSeatEntity extends Entity {
    private static final EntityDataAccessor<Boolean> LOCKED=SynchedEntityData.defineId(TeaSeatEntity.class,EntityDataSerializers.BOOLEAN),DRINK=SynchedEntityData.defineId(TeaSeatEntity.class,EntityDataSerializers.BOOLEAN);
    public TeaSeatEntity(EntityType<?> type,Level level){super(type,level);setNoGravity(true);setInvisible(true);}
    @Override protected void defineSynchedData(){entityData.define(LOCKED,false);entityData.define(DRINK,false);}@Override protected void readAdditionalSaveData(CompoundTag n){}@Override protected void addAdditionalSaveData(CompoundTag n){}
    public void policy(boolean locked,boolean drink){entityData.set(LOCKED,locked);entityData.set(DRINK,drink);}
    public boolean locked(){return entityData.get(LOCKED);}public boolean canDrink(){return entityData.get(DRINK);}
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void mount(net.minecraftforge.event.entity.EntityMountEvent e){if(e.isDismounting()&&e.getEntityMounting() instanceof net.minecraft.server.level.ServerPlayer p&&p.isAlive()&&e.getEntityBeingMounted() instanceof TeaSeatEntity seat&&seat.locked()){e.setCanceled(true);if(p.connection!=null)p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetPassengersPacket(seat));}}
    @Override protected boolean canAddPassenger(Entity passenger){return passenger instanceof LivingEntity&&getPassengers().isEmpty();}
    @Override public double getPassengersRidingOffset(){return -.25;}
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){return false;}
    @Override public void tick(){super.tick();if(!level().isClientSide){var tag=getPersistentData();var v=tag.hasUUID("TeaOwner")?com.coraline.bobinsky.BobinskyVisitData.get(getServer()).teaVisits.get(tag.getUUID("TeaOwner")):null;if(v==null||!v.getBoolean("Active")||v.getBoolean("Cancelled")||!(v.hasUUID("Seat")&&v.getUUID("Seat").equals(getUUID())||v.hasUUID("SpinkSeat")&&v.getUUID("SpinkSeat").equals(getUUID()))){policy(false,false);ejectPassengers();discard();}else if(com.coraline.feast.MissingParentsTea.isVisit(v))policy(com.coraline.feast.MissingParentsTea.seatLocked(v),false);else policy(v.getBoolean("Seated")&&!v.getBoolean("Drank"),v.getInt("Phase")==TeaVisit.DRINK);}}
}
