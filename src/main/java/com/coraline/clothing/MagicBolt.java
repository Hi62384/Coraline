package com.coraline.clothing;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraft.core.particles.*;
import com.coraline.registry.ModEntities;

public final class MagicBolt extends ThrowableProjectile {
    public MagicBolt(EntityType<? extends MagicBolt> type,Level level){super(type,level);}
    public MagicBolt(Level level,LivingEntity owner){super(ModEntities.MAGIC_BOLT.get(),owner,level);}
    @Override protected void defineSynchedData(){}
    @Override protected float getGravity(){return 0;}
    @Override public void tick(){
        super.tick();
        if(level().isClientSide){
            var motion=getDeltaMovement();
            for(int i=0;i<3;i++)level().addParticle(new DustParticleOptions(new org.joml.Vector3f(.35F,.7F,1F),.8F),getX()-motion.x*i/3,getY()-motion.y*i/3,getZ()-motion.z*i/3,0,0,0);
            level().addParticle(ParticleTypes.END_ROD,getX(),getY(),getZ(),0,0,0);
        }else if(tickCount>80)discard();
    }
    @Override protected void onHitEntity(EntityHitResult hit){
        super.onHitEntity(hit);
        if(!level().isClientSide&&hit.getEntity() instanceof LivingEntity target){
            var owner=getOwner();if(owner!=null&&!target.isAlliedTo(owner)&&!owner.isAlliedTo(target))target.hurt(damageSources().indirectMagic(this,owner),WeaponConfig.WAND_DAMAGE.get().floatValue());
        }
    }
    @Override protected void onHit(HitResult hit){super.onHit(hit);if(!level().isClientSide){((net.minecraft.server.level.ServerLevel)level()).sendParticles(ParticleTypes.END_ROD,getX(),getY(),getZ(),8,.15,.15,.15,.025);discard();}}
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
}
