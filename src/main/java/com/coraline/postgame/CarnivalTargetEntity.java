package com.coraline.postgame;

import net.minecraft.nbt.*;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;

/** Actual moving projectile hitboxes, not an animation over a stationary actor. */
public final class CarnivalTargetEntity extends Entity {
    private static final EntityDataAccessor<Integer> COLOR=SynchedEntityData.defineId(CarnivalTargetEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> LIT=SynchedEntityData.defineId(CarnivalTargetEntity.class,EntityDataSerializers.BOOLEAN);
    private Tag raw=new CompoundTag();
    public CarnivalTargetEntity(EntityType<? extends CarnivalTargetEntity> type,Level level){super(type,level);noPhysics=true;setNoGravity(true);}
    @Override protected void defineSynchedData(){entityData.define(COLOR,0);entityData.define(LIT,false);}
    public int color(){return entityData.get(COLOR);}public boolean lit(){return entityData.get(LIT);}
    CompoundTag marker(){return raw instanceof CompoundTag n?n:new CompoundTag();}
    private boolean known(){var n=marker();return n.contains("Version",Tag.TAG_INT)&&n.getInt("Version")==1&&n.hasUUID("Generation")&&n.hasUUID("Site")&&n.hasUUID("Session")&&n.contains("Control",Tag.TAG_LONG)&&n.contains("Index",Tag.TAG_INT)&&n.getInt("Index")>=0&&n.getInt("Index")<4;}
    void configure(CarnivalEntity game,CompoundTag marker){if(!(raw instanceof CompoundTag n)||!n.isEmpty())return;raw=marker.copy();entityData.set(COLOR,marker.getInt("Index"));game.position(this);}
    private CarnivalEntity root(){if(!known()||!(level() instanceof ServerLevel level))return null;var at=net.minecraft.core.BlockPos.of(marker().getLong("Control"));return level.hasChunkAt(at)&&level.getBlockEntity(at) instanceof CarnivalEntity game&&game.member(this)?game:null;}
    @Override public void tick(){super.tick();setRemainingFireTicks(0);if(level().isClientSide||!known())return;var level=(ServerLevel)level();var at=net.minecraft.core.BlockPos.of(marker().getLong("Control"));if(!level.hasChunkAt(at))return;var game=root();if(game==null){discard();return;}game.position(this);entityData.set(LIT,game.expected()==color());}
    @Override public boolean isPickable(){return true;}
    @Override public boolean isPushable(){return false;}
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){if(level().isClientSide||!(source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile)||!(source.getEntity() instanceof net.minecraft.server.level.ServerPlayer player))return false;var game=root();return game!=null&&game.hit(this,player);}
    @Override protected void addAdditionalSaveData(CompoundTag n){n.put("CoralineCarnivalTarget",raw.copy());}
    @Override protected void readAdditionalSaveData(CompoundTag n){raw=n.contains("CoralineCarnivalTarget")?n.get("CoralineCarnivalTarget").copy():new CompoundTag();if(known())entityData.set(COLOR,marker().getInt("Index"));}
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
}
