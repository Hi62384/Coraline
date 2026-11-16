package com.coraline.postgame;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

/** Void attacks collide with terrain but never destroy it or hit story NPCs. */
public final class VoidBoltEntity extends Projectile {
    private int life;
    public VoidBoltEntity(EntityType<? extends VoidBoltEntity> type,Level level){super(type,level);}
    void configure(WhiteVoidAvatarEntity owner){setOwner(owner);}
    @Override protected void defineSynchedData(){}
    @Override public void tick(){
        super.tick();if(++life>120){discard();return;}
        if(!level().isClientSide&&(!(getOwner() instanceof WhiteVoidAvatarEntity owner)||!owner.authentic())){discard();return;}
        var hit=ProjectileUtil.getHitResultOnMoveVector(this,this::canHitEntity);if(hit.getType()!=HitResult.Type.MISS&&!level().isClientSide)onHit(hit);
        setPos(position().add(getDeltaMovement()));updateRotation();
        if(level() instanceof net.minecraft.server.level.ServerLevel l)l.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,getX(),getY(),getZ(),1,0,0,0,0);
    }
    @Override protected boolean canHitEntity(Entity entity){return entity instanceof net.minecraft.world.entity.player.Player p&&!p.isCreative()&&!p.isSpectator()&&super.canHitEntity(entity);}
    @Override protected void onHitEntity(EntityHitResult hit){if(getOwner() instanceof WhiteVoidAvatarEntity owner&&owner.authentic()){hit.getEntity().hurt(damageSources().mobAttack(owner),5+owner.phase());owner.hazard(hit.getEntity().position());}discard();}
    @Override protected void onHitBlock(BlockHitResult hit){if(getOwner() instanceof WhiteVoidAvatarEntity owner&&owner.authentic())owner.hazard(hit.getLocation());discard();}
    @Override protected void addAdditionalSaveData(CompoundTag n){super.addAdditionalSaveData(n);n.putInt("VoidLife",life);}
    @Override protected void readAdditionalSaveData(CompoundTag n){super.readAdditionalSaveData(n);life=Math.max(0,Math.min(121,n.getInt("VoidLife")));}
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
}
