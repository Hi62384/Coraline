package com.coraline.doll;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;

/** Normal item physics/pickup; destruction, unlike chunk unloading or pickup, starts recovery. */
public final class BoundDollEntity extends ItemEntity {
    private boolean takingDamage;
    public BoundDollEntity(EntityType<? extends ItemEntity> type,Level level){super(type,level);}
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){
        takingDamage=true;try{return super.hurt(source,amount);}finally{takingDamage=false;}
    }
    @Override public void tick(){
        super.tick();
        if(!isRemoved()&&level() instanceof ServerLevel sl&&tickCount%20==0&&!getItem().isEmpty()){
            var data=DollBondData.get(sl.getServer());
            if(!data.current(getItem())){setItem(net.minecraft.world.item.ItemStack.EMPTY);discard();return;}
            data.present(getItem());
        }
    }
    @Override public void remove(RemovalReason reason){
        // Hoppers discard a nonempty source entity after copying its stack into storage.
        // Only positive destruction evidence should bind a replacement, never discard alone.
        boolean destroyed=reason==RemovalReason.KILLED||(reason==RemovalReason.DISCARDED&&(takingDamage||getAge()>=lifespan||getY()<level().getMinBuildHeight()-64));
        if(level() instanceof ServerLevel sl && destroyed&&!getItem().isEmpty())
            DollBondData.get(sl.getServer()).lost(getItem(),sl);
        super.remove(reason);
    }
}
