package com.coraline.entity;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

/** A previewable cast actor: normal navigation drives the existing limb animation clock. */
public class CharacterEntity extends PathfinderMob {
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> GAME_GESTURE=net.minecraft.network.syncher.SynchedEntityData.defineId(CharacterEntity.class,net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> DOOR_DUCK=net.minecraft.network.syncher.SynchedEntityData.defineId(CharacterEntity.class,net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> STORY_CAT=net.minecraft.network.syncher.SynchedEntityData.defineId(CharacterEntity.class,net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> CAT_CRAWL=net.minecraft.network.syncher.SynchedEntityData.defineId(CharacterEntity.class,net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    public void storyCatCrawl(boolean crawl){if(crawl!=entityData.get(CAT_CRAWL)){entityData.set(CAT_CRAWL,crawl);refreshDimensions();}}
    public boolean storyCatCompanion(){return entityData!=null&&entityData.hasItem(STORY_CAT)&&entityData.get(STORY_CAT);}
    public void partyDoorDuck(boolean duck){if(duck!=entityData.get(DOOR_DUCK)){entityData.set(DOOR_DUCK,duck);refreshDimensions();}}
    public boolean partyDoorDuck(){return entityData.get(DOOR_DUCK);}
    // An open door leaf occupies 3/16 of its cell. A centered .70m body still
    // intersects it; the standard .60m doorway footprint clears that leaf.
    @Override public EntityDimensions getDimensions(Pose pose){var d=super.getDimensions(pose);if(entityData!=null&&entityData.hasItem(CAT_CRAWL)&&entityData.get(CAT_CRAWL))return EntityDimensions.scalable(Math.min(.5F,d.width),Math.min(.7F,d.height));return entityData!=null&&entityData.hasItem(DOOR_DUCK)&&entityData.get(DOOR_DUCK)?EntityDimensions.scalable(Math.min(.60F,d.width),Math.min(1.85F,d.height)):d;}
    @Override public void onSyncedDataUpdated(net.minecraft.network.syncher.EntityDataAccessor<?> key){super.onSyncedDataUpdated(key);if(DOOR_DUCK.equals(key)||CAT_CRAWL.equals(key))refreshDimensions();}
    private int gameGestureUntil;
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(GAME_GESTURE,0);entityData.define(DOOR_DUCK,false);entityData.define(STORY_CAT,false);entityData.define(CAT_CRAWL,false);}
    public int gameGesture(){return entityData.get(GAME_GESTURE);}
    public void gameGesture(int gesture,int ticks){entityData.set(GAME_GESTURE,gesture);gameGestureUntil=tickCount+ticks;}
    @Override public void tick(){super.tick();if(!level().isClientSide){entityData.set(STORY_CAT,com.coraline.feast.ShowActors.storyCat(this));if(gameGesture()!=0&&tickCount>=gameGestureUntil)gameGesture(0,0);if(tickCount%20==0)com.coraline.feast.ShowActors.rememberCanonicalCat(this);com.coraline.feast.CanonicalRealWybie.remember(this);if(getPersistentData().hasUUID("CatEncounterOwner")){var p=getServer().getPlayerList().getPlayer(getPersistentData().getUUID("CatEncounterOwner"));if(p==null?tickCount>100:!com.coraline.secrets.CatEncounter.active(p))discard();}}}
    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag n){super.readAdditionalSaveData(n);if(getPersistentData().getBoolean("CanonicalWybieStored")||getPersistentData().hasUUID("CanonicalStoryCatEndingArchive")){noPhysics=true;setNoAi(true);setNoGravity(true);setInvisible(true);setSilent(true);setInvulnerable(true);setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);}}
    @Override public boolean isPushable(){return !com.coraline.feast.ShowActors.storyCat(this)&&!com.coraline.feast.CanonicalRealWybie.dormant(this)&&super.isPushable();}
    @Override public boolean isPickable(){return !com.coraline.feast.CanonicalRealWybie.dormant(this)&&super.isPickable();}
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){return !com.coraline.feast.ShowActors.storyCat(this)&&!com.coraline.feast.CanonicalRealWybie.dormant(this)&&!com.coraline.feast.FirstEscapeStory.protectedWybie(this)&&super.hurt(source,amount);}
    @Override public boolean canBeCollidedWith(){return !com.coraline.feast.ShowActors.storyCat(this)&&!com.coraline.feast.CanonicalRealWybie.dormant(this)&&super.canBeCollidedWith();}
    // Cover both directions of entity collision and the separate living push path.
    @Override public boolean canCollideWith(Entity other){return !com.coraline.feast.ShowActors.storyCat(this)&&!(other instanceof CharacterEntity cat&&com.coraline.feast.ShowActors.storyCat(cat))&&super.canCollideWith(other);}
    @Override public void push(Entity other){if(!com.coraline.feast.ShowActors.storyCat(this))super.push(other);}
    @Override public void remove(RemovalReason reason){
        if((reason==RemovalReason.DISCARDED||reason==RemovalReason.KILLED)&&com.coraline.feast.CanonicalRealWybie.preserveCleanup(this,reason==RemovalReason.KILLED))return;
        super.remove(reason);
    }
    public CharacterEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type,level);setPersistenceRequired();setMaxUpStep(1);
    }
    public String characterId(){return ForgeRegistries.ENTITY_TYPES.getKey(getType()).getPath().substring("character_".length());}
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.MOVEMENT_SPEED,.23).add(Attributes.FOLLOW_RANGE,16);}
    @Override protected void registerGoals(){
        goalSelector.addGoal(-3,new com.coraline.postgame.WybieRescue.EscortGoal(this));
        goalSelector.addGoal(-3,new com.coraline.postgame.WybieRescue.GuideGoal(this));
        goalSelector.addGoal(-1,new com.coraline.secrets.CatEncounter.ControlGoal(this));
        goalSelector.addGoal(0,new com.coraline.feast.FamilyGameNight.ControlGoal(this));
        goalSelector.addGoal(0,new com.coraline.feast.ShowActors.ControlGoal(this));
        goalSelector.addGoal(0,new com.coraline.feast.WybieOptionalVisits.CompanionGoal(this));
        goalSelector.addGoal(-2,new com.coraline.feast.PartyGuestGoal(this));
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(1,new OpenDoorGoal(this,true));
        goalSelector.addGoal(5,new WaterAvoidingRandomStrollGoal(this,.8));
        goalSelector.addGoal(6,new LookAtPlayerGoal(this,Player.class,8));
        goalSelector.addGoal(7,new RandomLookAroundGoal(this));
    }
}
