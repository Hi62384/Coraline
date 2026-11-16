package com.coraline.postgame;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;

/** A short-lived ground fracture; it stores no original blocks to lose. */
public final class VoidHazardEntity extends Entity {
    private UUID owner;private int life;
    public VoidHazardEntity(EntityType<? extends VoidHazardEntity> type,Level level){super(type,level);noPhysics=true;}
    void configure(WhiteVoidAvatarEntity owner){this.owner=owner.getUUID();}
    public float fade(float partial){return Math.max(0,1-(tickCount+partial)/100F);}
    @Override protected void defineSynchedData(){}
    @Override public void tick(){super.tick();if(++life>100){discard();return;}if(level().isClientSide)return;
        var entity=owner==null?null:((net.minecraft.server.level.ServerLevel)level()).getEntity(owner);if(!(entity instanceof WhiteVoidAvatarEntity avatar)||!avatar.authentic()){discard();return;}
        if(life%10==0)for(var p:level().getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,getBoundingBox().inflate(.3,.5,.3),p->p.isAlive()&&!p.isCreative()&&!p.isSpectator()&&p.onGround()))p.hurt(damageSources().magic(),3);
    }
    @Override protected void addAdditionalSaveData(CompoundTag n){if(owner!=null)n.putUUID("VoidOwner",owner);n.putInt("VoidLife",life);}
    @Override protected void readAdditionalSaveData(CompoundTag n){owner=n.hasUUID("VoidOwner")?n.getUUID("VoidOwner"):null;life=Math.max(0,Math.min(101,n.getInt("VoidLife")));}
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
}
