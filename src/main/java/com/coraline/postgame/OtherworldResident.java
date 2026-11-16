package com.coraline.postgame;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;

/** A real native merchant with a custom silhouette, finite saved stock and
 * a deterministic settlement identity. No owner/run completion credentials. */
public final class OtherworldResident extends AbstractVillager {
    private static final EntityDataAccessor<Integer> ROLE=SynchedEntityData.defineId(OtherworldResident.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> TRADING=SynchedEntityData.defineId(OtherworldResident.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FREED=SynchedEntityData.defineId(OtherworldResident.class,EntityDataSerializers.BOOLEAN);
    private Tag residence=new CompoundTag(),release=new CompoundTag();private long restockAt;
    public OtherworldResident(EntityType<? extends OtherworldResident> type,Level level){super(type,level);setPersistenceRequired();}
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,24).add(Attributes.MOVEMENT_SPEED,.26).add(Attributes.FOLLOW_RANGE,24).add(Attributes.ATTACK_DAMAGE,4);}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(ROLE,0);entityData.define(TRADING,false);entityData.define(FREED,false);}
    public int role(){return entityData.get(ROLE);}
    public boolean tradingPose(){return entityData.get(TRADING);}
    public boolean freedPose(){return entityData.get(FREED);}
    @Override public void setTradingPlayer(Player player){super.setTradingPlayer(player);entityData.set(TRADING,player!=null);}
    @Override public Player getTradingPlayer(){return !level().isClientSide&&!authentic()?null:super.getTradingPlayer();}
    @Override public net.minecraft.network.chat.Component getName(){return hasCustomName()?super.getName():net.minecraft.network.chat.Component.translatable(role()>=4&&freedPose()?"entity.coraline.otherworld_resident.role_"+role()+"_freed":"entity.coraline.otherworld_resident.role_"+role());}
    private boolean released(){var record=residence();return role()>=4&&release instanceof CompoundTag n&&n.contains("Version",Tag.TAG_INT)&&n.getInt("Version")==1&&n.hasUUID("Entity")&&n.getUUID("Entity").equals(getUUID())&&n.hasUUID("Generation")&&record.hasUUID("Generation")&&n.getUUID("Generation").equals(record.getUUID("Generation"))&&n.hasUUID("Site")&&record.hasUUID("Site")&&n.getUUID("Site").equals(record.getUUID("Site"));}
    private void recordRelease(){if(!(release instanceof CompoundTag n)||!n.isEmpty())return;var proof=new CompoundTag();proof.putInt("Version",1);proof.putUUID("Entity",getUUID());proof.putUUID("Generation",residence().getUUID("Generation"));proof.putUUID("Site",residence().getUUID("Site"));release=proof;}
    CompoundTag residence(){return residence instanceof CompoundTag n?n:new CompoundTag();}
    void configure(InfiniteStructurePlan.Plan plan,int module){if(!(residence instanceof CompoundTag n)||!n.isEmpty())return;residence=InfiniteSiteResidents.metadata(plan,module,getUUID());entityData.set(ROLE,InfiniteSiteResidents.role(plan,module));var at=InfiniteSiteResidents.home(plan,module);restrictTo(at,24);restockAt=level().getGameTime()+24000;}
    boolean authentic(){return level() instanceof ServerLevel level&&level.getServer().isSameThread()&&InfiniteSiteResidents.authentic(this,level);}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(1,new PanicGoal(this,1.15){@Override public boolean canUse(){return role()!=3&&(role()<4||freedPose())&&super.canUse();}});
        goalSelector.addGoal(2,new TradeWithPlayerGoal(this));goalSelector.addGoal(3,new LookAtTradingPlayerGoal(this));
        goalSelector.addGoal(3,new MeleeAttackGoal(this,1.1,true){@Override public boolean canUse(){return role()==3&&authentic()&&super.canUse();}@Override public boolean canContinueToUse(){return role()==3&&authentic()&&super.canContinueToUse();}});
        targetSelector.addGoal(1,new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this,net.minecraft.world.entity.monster.Monster.class,10,true,false,target->role()==3&&authentic()&&target.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(getRestrictCenter()))<24*24));
        goalSelector.addGoal(4,new MoveTowardsRestrictionGoal(this,.8));goalSelector.addGoal(5,new WaterAvoidingRandomStrollGoal(this,.6){@Override public boolean canUse(){return (role()<4||freedPose())&&super.canUse();}});goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(7,new RandomLookAroundGoal(this));
    }
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){return !(role()==5&&!freedPose()&&authentic())&&super.hurt(source,amount);}
    @Override public boolean doHurtTarget(Entity target){return role()==3&&authentic()&&target instanceof net.minecraft.world.entity.monster.Monster&&super.doHurtTarget(target);}
    @Override public boolean canChangeDimensions(){return false;}
    @Override public AgeableMob getBreedOffspring(ServerLevel level,AgeableMob partner){return null;}
    @Override public boolean showProgressBar(){return false;}
    @Override public boolean canRestock(){return true;}
    @Override public net.minecraft.sounds.SoundEvent getNotifyTradeSound(){return net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME;}
    @Override protected net.minecraft.sounds.SoundEvent getTradeUpdatedSound(boolean success){return success?net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_STEP:net.minecraft.sounds.SoundEvents.DEEPSLATE_STEP;}
    @Override public InteractionResult mobInteract(Player player,InteractionHand hand){
        if(!isAlive()||isBaby()||isTrading()||player.isSpectator())return InteractionResult.PASS;
        if(role()>=3){if(!level().isClientSide&&authentic())player.displayClientMessage(net.minecraft.network.chat.Component.translatable(role()==3?"message.coraline.resistance_lookout":role()==5?(freedPose()?"message.coraline.orphan_freed":"message.coraline.orphan_bound"):freedPose()?"message.coraline.performer_freed":residence().getString("Family").equals(InfiniteStructurePlan.Family.ENDLESS_CARNIVAL.name())?"message.coraline.carnival_performer_loop":"message.coraline.performer_loop"),true);return InteractionResult.sidedSuccess(level().isClientSide);}
        if(!level().isClientSide){if(!authentic()||player.distanceToSqr(this)>36)return InteractionResult.FAIL;setTradingPlayer(player);openTradingScreen(player,getDisplayName(),1);}
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
    private void offer(ItemStack cost,ItemStack result,int uses){offers.add(new MerchantOffer(cost,result,uses,1,0));}
    private ItemStack buttons(int count){return new ItemStack(com.coraline.clothing.OtherMaterials.BUTTON.get(),count);}
    @Override protected void updateTrades(){
        // Native protochunk serialization may initialize offers on a worldgen
        // worker. Build local stock only; live trading/XP/restocking still
        // require the main-thread canonical-world attestation above.
        var record=residence();if(role()>=3||!record.contains("Version",Tag.TAG_INT)||record.getInt("Version")!=1||!record.hasUUID("Entity")||!record.getUUID("Entity").equals(getUUID())||!record.contains("Role",Tag.TAG_INT)||record.getInt("Role")!=role())return;
        if(role()==0){offer(buttons(3),new ItemStack(Items.ARROW,16),8);offer(buttons(2),new ItemStack(Items.BREAD,3),12);offer(buttons(3),new ItemStack(Items.COOKED_BEEF,3),8);offer(buttons(2),new ItemStack(InfiniteContent.block("paperwood_planks").asItem(),8),8);offer(new ItemStack(Items.STRING,8),buttons(1),8);}
        else if(role()==1){offer(buttons(8),new ItemStack(Items.BOW),4);offer(buttons(6),new ItemStack(com.coraline.clothing.OtherMaterials.OTHER_IRON.get(),2),8);offer(new ItemStack(com.coraline.clothing.OtherMaterials.OTHER_IRON.get()),buttons(2),8);offers.add(new MerchantOffer(buttons(8),new ItemStack(com.coraline.clothing.OtherMaterials.OTHER_IRON.get(),2),new ItemStack(com.coraline.clothing.ArtifactItems.OTHER_IRON_LOCKPICK.get()),4,1,0));offer(buttons(5),new ItemStack(Items.STRING,12),8);offer(buttons(12),new ItemStack(Items.SPYGLASS),2);}
        else{for(String name:new String[]{"raincoat","comfy_pajamas","star_shirt","garden_overalls"}){var item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("coraline",name));if(item!=null&&item!=Items.AIR)offer(buttons(name.equals("star_shirt")||name.equals("garden_overalls")?20:16),new ItemStack(item),4);}offer(new ItemStack(Items.STRING,8),buttons(1),8);}
    }
    @Override protected void rewardTradeXp(MerchantOffer offer){if(level() instanceof ServerLevel server&&authentic()&&offer.shouldRewardExp())ExperienceOrb.award(server,position(),1);}
    @Override public void aiStep(){
        super.aiStep();if(level().isClientSide)return;if(!authentic()){if(super.getTradingPlayer()!=null)stopTrading();return;}
        if(role()>=4){if(isAlive()&&!released()&&(role()==5?OrphanageLayout.freed(level(),residence()):residence().getString("Family").equals(InfiniteStructurePlan.Family.ENDLESS_CARNIVAL.name())?CarnivalLayout.freed(level(),residence()):PerformanceLayout.freed(level(),residence())))recordRelease();entityData.set(FREED,released());}
        if(restockAt==0){restockAt=level().getGameTime()+24000;return;}
        if(!isTrading()&&level().getGameTime()>=restockAt){if(offers!=null)for(var offer:offers)offer.resetUses();restockAt=level().getGameTime()+24000;}
    }
    @Override public void addAdditionalSaveData(CompoundTag n){super.addAdditionalSaveData(n);n.put("CoralineResidence",residence.copy());n.put("CoralinePerformanceRelease",release.copy());n.putLong("CoralineRestockAt",restockAt);}
    @Override public void readAdditionalSaveData(CompoundTag n){super.readAdditionalSaveData(n);residence=n.contains("CoralineResidence")?n.get("CoralineResidence").copy():new CompoundTag();release=n.contains("CoralinePerformanceRelease")?n.get("CoralinePerformanceRelease").copy():new CompoundTag();restockAt=n.contains("CoralineRestockAt",Tag.TAG_LONG)&&n.getLong("CoralineRestockAt")>=0?n.getLong("CoralineRestockAt"):Long.MAX_VALUE;var record=residence();if(record.contains("Role",Tag.TAG_INT))entityData.set(ROLE,Math.max(0,Math.min(5,record.getInt("Role"))));if(record.contains("Home",Tag.TAG_LONG))restrictTo(BlockPos.of(record.getLong("Home")),24);entityData.set(FREED,released());}
}
