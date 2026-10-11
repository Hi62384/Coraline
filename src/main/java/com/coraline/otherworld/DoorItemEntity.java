package com.coraline.otherworld;

public final class DoorItemEntity extends net.minecraft.world.entity.item.ItemEntity {
    private boolean damage;
    public DoorItemEntity(net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.item.ItemEntity> type,net.minecraft.world.level.Level level){super(type,level);}
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){damage=true;try{return super.hurt(source,amount);}finally{damage=false;}}
    @Override public void remove(RemovalReason reason){
        if(level() instanceof net.minecraft.server.level.ServerLevel sl&&!getItem().isEmpty()&&(reason==RemovalReason.KILLED||(reason==RemovalReason.DISCARDED&&(damage||getAge()>=lifespan||getY()<level().getMinBuildHeight()-64))))DoorData.get(sl.getServer()).destroyed(DoorData.id(getItem()));
        super.remove(reason);
    }
    @Override public void tick(){
        if(level() instanceof net.minecraft.server.level.ServerLevel sl&&DoorData.get(sl.getServer()).revoked(getItem())){discard();return;}
        super.tick();if(!isRemoved()&&!getItem().isEmpty()&&tickCount%20==0&&level() instanceof net.minecraft.server.level.ServerLevel sl)DoorData.get(sl.getServer()).track(getItem(),sl,"entity",blockPosition(),getUUID());
    }
}
