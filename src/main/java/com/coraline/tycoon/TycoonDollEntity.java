package com.coraline.tycoon;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** One physical bedside doll, with a canonical per-resident capability. */
public final class TycoonDollEntity extends Entity {
    private static final EntityDataAccessor<Integer> APPEARANCE=SynchedEntityData.defineId(TycoonDollEntity.class,EntityDataSerializers.INT);
    public UUID owner,resident,doll;
    public TycoonDollEntity(EntityType<? extends TycoonDollEntity> type,Level level){super(type,level);setNoGravity(true);}
    @Override protected void defineSynchedData(){entityData.define(APPEARANCE,0);}
    public int appearance(){return entityData.get(APPEARANCE);}
    public void configure(UUID owner,UUID resident,UUID doll,int seed){this.owner=owner;this.resident=resident;this.doll=doll;entityData.set(APPEARANCE,seed);}
    @Override protected void addAdditionalSaveData(CompoundTag n){if(owner!=null)n.putUUID("Owner",owner);if(resident!=null)n.putUUID("Resident",resident);if(doll!=null)n.putUUID("Doll",doll);n.putInt("Appearance",appearance());}
    @Override protected void readAdditionalSaveData(CompoundTag n){owner=n.hasUUID("Owner")?n.getUUID("Owner"):null;resident=n.hasUUID("Resident")?n.getUUID("Resident"):null;doll=n.hasUUID("Doll")?n.getUUID("Doll"):null;entityData.set(APPEARANCE,n.getInt("Appearance"));}
    @Override public boolean isPickable(){return true;}
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){return false;}
    @Override public InteractionResult interact(Player player,InteractionHand hand){if(hand!=InteractionHand.MAIN_HAND)return InteractionResult.PASS;if(player instanceof ServerPlayer p&&p.isShiftKeyDown()&&p.distanceToSqr(this)<=25)TycoonScope.run(p,owner,()->TycoonDolls.pickup(p,this));return InteractionResult.sidedSuccess(level().isClientSide);}
    @Override public void tick(){super.tick();if(level().isClientSide||owner==null||TycoonSpies.dollTick(this))return;var h=TycoonData.get(getServer()).homes.get(owner);var r=h==null?null:h.residents.get(resident);if(r==null||!r.active()||!TycoonDolls.matches(r,doll)||!r.n.getCompound("Doll").hasUUID("Entity")||!getUUID().equals(r.n.getCompound("Doll").getUUID("Entity")))discard();}
}
