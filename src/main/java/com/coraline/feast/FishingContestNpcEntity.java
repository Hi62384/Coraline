package com.coraline.feast;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Human-skinned, button-eyed festival contestant or judge. */
public final class FishingContestNpcEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> SKIN=SynchedEntityData.defineId(FishingContestNpcEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FEMALE=SynchedEntityData.defineId(FishingContestNpcEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> JUDGE=SynchedEntityData.defineId(FishingContestNpcEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> SLOT=SynchedEntityData.defineId(FishingContestNpcEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FISHING=SynchedEntityData.defineId(FishingContestNpcEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BAKING=SynchedEntityData.defineId(FishingContestNpcEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<CompoundTag> PARTY_PROFILE=SynchedEntityData.defineId(FishingContestNpcEntity.class,EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Integer> PARTY_GESTURE=SynchedEntityData.defineId(FishingContestNpcEntity.class,EntityDataSerializers.INT);
    private int partyGestureUntil;private com.mojang.authlib.GameProfile cachedPartyProfile;
    @Override protected void registerGoals(){goalSelector.addGoal(0,new PartyGuestGoal(this));}
    public int partyGesture(){return entityData.get(PARTY_GESTURE);}
    public void partyGesture(int gesture,int ticks){entityData.set(PARTY_GESTURE,gesture);partyGestureUntil=tickCount+ticks;}
    public void partyProfile(CompoundTag profile){entityData.set(PARTY_PROFILE,profile.copy());cachedPartyProfile=null;}
    public com.mojang.authlib.GameProfile partyProfile(){if(entityData.get(PARTY_PROFILE).isEmpty())return null;if(cachedPartyProfile==null)cachedPartyProfile=net.minecraft.nbt.NbtUtils.readGameProfile(entityData.get(PARTY_PROFILE));return cachedPartyProfile;}
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){super.onSyncedDataUpdated(key);if(PARTY_PROFILE.equals(key))cachedPartyProfile=null;}
    @Override public void tick(){super.tick();if(!level().isClientSide&&partyGesture()!=0&&tickCount>=partyGestureUntil)partyGesture(0,0);}
    public FishingContestNpcEntity(EntityType<? extends PathfinderMob> type,Level level){super(type,level);setPersistenceRequired();}
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.MOVEMENT_SPEED,.28).add(Attributes.FOLLOW_RANGE,32);}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(SKIN,0);entityData.define(FEMALE,false);entityData.define(JUDGE,false);entityData.define(SLOT,0);entityData.define(FISHING,false);entityData.define(BAKING,false);entityData.define(PARTY_PROFILE,new CompoundTag());entityData.define(PARTY_GESTURE,0);}
    public int skin(){return entityData.get(SKIN);}public boolean female(){return entityData.get(FEMALE);}public boolean judge(){return entityData.get(JUDGE);}
    public int slot(){return entityData.get(SLOT);}public boolean fishing(){return entityData.get(FISHING);}
    public void setFishing(boolean fishing){entityData.set(FISHING,fishing);}
    public boolean baking(){return entityData.get(BAKING);}public void setBaking(boolean baking){entityData.set(BAKING,baking);}
    public void configure(int skin,boolean female,boolean judge,int slot){entityData.set(SKIN,Math.floorMod(skin,64));entityData.set(FEMALE,female);entityData.set(JUDGE,judge);entityData.set(SLOT,slot);
        if(!judge)setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.FISHING_ROD));setDropChance(EquipmentSlot.MAINHAND,0);setInvulnerable(true);
    }
    @Override public boolean hurt(DamageSource source,float amount){return false;}
    @Override public boolean isPushable(){return false;}
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){
        if(!level().isClientSide&&player instanceof net.minecraft.server.level.ServerPlayer p&&getPersistentData().getBoolean("CoralineCircusBookie")&&hand==InteractionHand.MAIN_HAND)
            com.coraline.feast.MiceCircusActivity.interactBookie(p,this);
        return InteractionResult.CONSUME;
    }
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("Skin",skin());tag.putBoolean("Female",female());tag.putBoolean("Judge",judge());tag.putInt("Slot",slot());tag.putBoolean("Fishing",fishing());tag.putBoolean("Baking",baking());tag.put("PartyProfile",entityData.get(PARTY_PROFILE).copy());}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);entityData.set(SKIN,Math.floorMod(tag.getInt("Skin"),64));entityData.set(FEMALE,tag.getBoolean("Female"));entityData.set(JUDGE,tag.getBoolean("Judge"));entityData.set(SLOT,tag.getInt("Slot"));entityData.set(FISHING,tag.getBoolean("Fishing"));entityData.set(BAKING,tag.getBoolean("Baking"));partyProfile(tag.getCompound("PartyProfile"));}
}
