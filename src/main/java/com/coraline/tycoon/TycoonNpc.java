package com.coraline.tycoon;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Procedural resident/staff rig; canonical identity comes from TycoonData, never family data. */
public final class TycoonNpc extends PathfinderMob {
    private static final EntityDataAccessor<Integer> APPEARANCE=SynchedEntityData.defineId(TycoonNpc.class,EntityDataSerializers.INT),GESTURE=SynchedEntityData.defineId(TycoonNpc.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> STAFF=SynchedEntityData.defineId(TycoonNpc.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> RITUAL=SynchedEntityData.defineId(TycoonNpc.class,EntityDataSerializers.INT);
    public UUID owner,resident;private int gestureUntil;
    public TycoonNpc(EntityType<? extends PathfinderMob> t,Level l){super(t,l);setPersistenceRequired();setMaxUpStep(1);if(getNavigation() instanceof GroundPathNavigation nav)nav.setCanOpenDoors(true);}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(APPEARANCE,0);entityData.define(GESTURE,0);entityData.define(STAFF,false);entityData.define(RITUAL,0);}
    public int ritual(){return entityData.get(RITUAL);}public void ritual(int ticks){entityData.set(RITUAL,Math.max(0,Math.min(160,ticks)));}
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.MOVEMENT_SPEED,.23).add(Attributes.FOLLOW_RANGE,32);}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(1,new com.coraline.feast.GhostVictimDoorGoal(this));
        goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,6){
            @Override public boolean canUse(){return !com.coraline.feast.GhostVictimActors.controlsLook(TycoonNpc.this)&&super.canUse();}
            @Override public boolean canContinueToUse(){return !com.coraline.feast.GhostVictimActors.controlsLook(TycoonNpc.this)&&super.canContinueToUse();}
        });
        goalSelector.addGoal(9,new RandomLookAroundGoal(this){
            @Override public boolean canUse(){return !com.coraline.feast.GhostVictimActors.controlsLook(TycoonNpc.this)&&super.canUse();}
            @Override public boolean canContinueToUse(){return !com.coraline.feast.GhostVictimActors.controlsLook(TycoonNpc.this)&&super.canContinueToUse();}
        });
    }
    public void configure(UUID owner,UUID resident,boolean staff,int appearance){this.owner=owner;this.resident=resident;var current=TycoonData.get(getServer()).homes.get(owner);if(current!=null&&current.state.hasUUID("CapabilityEpoch"))getPersistentData().putUUID("TycoonEpoch",current.state.getUUID("CapabilityEpoch"));entityData.set(STAFF,staff);entityData.set(APPEARANCE,appearance);if(!staff){var home=TycoonData.get(getServer()).homes.get(owner);var r=home==null?null:home.residents.get(resident);if(r!=null)setCustomName(Component.literal(r.name()));}else setCustomName(Component.literal("Agent Rowan"));}
    @Override public EntityDimensions getDimensions(Pose pose){return pose==Pose.SWIMMING&&gesture()==6?EntityDimensions.scalable(.55F,.55F):super.getDimensions(pose);}
    public boolean staff(){return entityData.get(STAFF);}public int appearance(){return entityData.get(APPEARANCE);}public int gesture(){return entityData.get(GESTURE);}
    public void gesture(int id,int ticks){entityData.set(GESTURE,id);gestureUntil=tickCount+ticks;}
    @Override public void tick(){super.tick();if(!level().isClientSide){var current=owner==null?null:TycoonData.get(getServer()).homes.get(owner);if(current!=null&&current.state.hasUUID("CapabilityEpoch")&&(!getPersistentData().hasUUID("TycoonEpoch")||!current.state.getUUID("CapabilityEpoch").equals(getPersistentData().getUUID("TycoonEpoch")))){discard();return;}if(gesture()!=0&&tickCount>gestureUntil)gesture(0,0);if(tickCount%20==0)TycoonCare.tickNpc(this);}}
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){
        if(hand!=InteractionHand.MAIN_HAND)return InteractionResult.PASS;
        if(player instanceof ServerPlayer p&&p.isAlive()&&!p.isSpectator()&&p.distanceToSqr(this)<=25)TycoonScope.run(p,owner,()->{if(staff()){if(p.isShiftKeyDown())TycoonAgency.request(p);else TycoonNetwork.open(p,"phone",null);}else if(TycoonWelcomeCake.give(p,this)){}else if(TycoonDolls.give(p,this)){}else if(TycoonGifts.manual(p,this)){}else if(TycoonCare.resident(this)!=null&&TycoonVisits.inside(TycoonCare.resident(this)))TycoonNetwork.visitor(p,this);else if(!TycoonCare.serve(p,this))TycoonNetwork.child(p,this);});
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
    @Override public void addAdditionalSaveData(CompoundTag n){super.addAdditionalSaveData(n);if(owner!=null)n.putUUID("TycoonOwner",owner);if(resident!=null)n.putUUID("Resident",resident);n.putInt("Appearance",appearance());n.putBoolean("Staff",staff());}
    @Override public void readAdditionalSaveData(CompoundTag n){super.readAdditionalSaveData(n);owner=n.hasUUID("TycoonOwner")?n.getUUID("TycoonOwner"):null;resident=n.hasUUID("Resident")?n.getUUID("Resident"):null;entityData.set(APPEARANCE,n.getInt("Appearance"));entityData.set(STAFF,n.getBoolean("Staff"));}
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){return false;}
    @Override public boolean canBeLeashed(Player p){return false;}
    @Override protected void dropAllDeathLoot(net.minecraft.world.damagesource.DamageSource source){}
}
