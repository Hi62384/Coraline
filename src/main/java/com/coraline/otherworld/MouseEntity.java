package com.coraline.otherworld;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import java.util.UUID;
import com.coraline.config.CoralineConfig;

public final class MouseEntity extends PathfinderMob {
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> CIRCUS_STYLE=net.minecraft.network.syncher.SynchedEntityData.defineId(MouseEntity.class,net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> SHOW_SLOT=net.minecraft.network.syncher.SynchedEntityData.defineId(MouseEntity.class,net.minecraft.network.syncher.EntityDataSerializers.INT);
    public int showSlot(){return entityData.get(SHOW_SLOT);}public void showSlot(int value){entityData.set(SHOW_SLOT,value);}
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> SPY=net.minecraft.network.syncher.SynchedEntityData.defineId(MouseEntity.class,net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> CAT_POSE=net.minecraft.network.syncher.SynchedEntityData.defineId(MouseEntity.class,net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Float> UGLY=net.minecraft.network.syncher.SynchedEntityData.defineId(MouseEntity.class,net.minecraft.network.syncher.EntityDataSerializers.FLOAT);
    public int catPose(){return entityData.get(CAT_POSE);}public float ugly(){return entityData.get(UGLY);}public void catPose(int pose,float ugly){entityData.set(CAT_POSE,pose);entityData.set(UGLY,net.minecraft.util.Mth.clamp(ugly,0,1));}
    public boolean spying(){return entityData.get(SPY);} public void spying(boolean value){entityData.set(SPY,value);}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(CIRCUS_STYLE,-1);entityData.define(SHOW_SLOT,-1);entityData.define(SPY,false);entityData.define(CAT_POSE,0);entityData.define(UGLY,0F);}
    public int circusStyle(){return entityData.get(CIRCUS_STYLE);}
    private boolean planningRescueBall,tycoonGoals;
    @Override public EntityDimensions getDimensions(Pose pose){return getPersistentData().hasUUID("RescueOwner")?EntityDimensions.scalable(.60F,planningRescueBall?1.35F:.90F):super.getDimensions(pose);}
    /** Plan for the mounted assembly, not for a mouse that fits under a low lintel. */
    public net.minecraft.world.level.pathfinder.Path rescueBallPath(net.minecraft.core.BlockPos target){
        return rescueBallPath(target,position());
    }
    public net.minecraft.world.level.pathfinder.Path rescueBallPath(net.minecraft.core.BlockPos target,net.minecraft.world.phys.Vec3 ballFeet){
        var passengerAt=position();planningRescueBall=true;refreshDimensions();
        // A scoped planner origin, restored before this call returns: passenger
        // feet are 0.45 m above the vehicle and otherwise seed an airborne node.
        // Neither the ball nor the visible/persisted rat is advanced by planning.
        try{setPos(ballFeet);return getNavigation().createPath(target,0);}finally{planningRescueBall=false;refreshDimensions();setPos(passengerAt);}
    }
    @Override public boolean shouldRenderAtSqrDistance(double distance){return circusStyle()>=0?distance<128*128:super.shouldRenderAtSqrDistance(distance);}
    public void circusStyle(int style){entityData.set(CIRCUS_STYLE,style);setNoAi(true);setInvulnerable(true);}
    public UUID doorId;
    public boolean chasing;
    private long spawnNight;
    private int seenTicks;
    public MouseEntity(EntityType<? extends PathfinderMob> type,Level level){super(type,level);setPersistenceRequired();setMaxUpStep(1);spawnNight=Math.floorDiv(level.getDayTime(),24000);}
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,3).add(Attributes.MOVEMENT_SPEED,.36).add(Attributes.FOLLOW_RANGE,40);}
    @Override public void tick(){super.tick();if(!level().isClientSide&&getPersistentData().hasUUID("TycoonOwner")){if(!tycoonGoals){tycoonGoals=true;if(getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation nav)nav.setCanOpenDoors(true);goalSelector.addGoal(0,new net.minecraft.world.entity.ai.goal.FloatGoal(this));goalSelector.addGoal(1,new net.minecraft.world.entity.ai.goal.OpenDoorGoal(this,true));}if(com.coraline.tycoon.TycoonVisits.mouseTick(this))return;}if(!level().isClientSide&&getPersistentData().hasUUID("CatEncounterOwner")){var p=getServer().getPlayerList().getPlayer(getPersistentData().getUUID("CatEncounterOwner"));if(p==null||!com.coraline.secrets.CatEncounter.active(p))discard();return;}if(level().isClientSide||circusStyle()>=0)return;
        if(com.coraline.secrets.SecretSpies.mouse(this))return;
        if(tickCount>12000||doorId==null){discard();return;}
        var d=DoorData.get(getServer()).doors.get(doorId);
        if(d==null||!d.placed||!d.exists||!OtherWorld.night(level())||spawnNight!=Math.floorDiv(level().getDayTime(),24000)){discard();return;}
        if(!chasing){
            getNavigation().stop();
            var owner=getServer().getPlayerList().getPlayer(d.owner);
            boolean seen=owner!=null&&owner.level()==level()&&owner.isAlive()&&!owner.isSpectator()&&owner.distanceToSqr(this)<Math.pow(CoralineConfig.MOUSE_SEEN_RANGE.get(),2)&&owner.hasLineOfSight(this)
                &&owner.getLookAngle().dot(getBoundingBox().getCenter().subtract(owner.getEyePosition()).normalize())>.92;
            seenTicks=seen?seenTicks+1:0;
            if(seenTicks<CoralineConfig.MOUSE_SEEN_TICKS.get())return;
            for(var mouse:level().getEntitiesOfClass(MouseEntity.class,getBoundingBox().inflate(40)))if(doorId.equals(mouse.doorId))mouse.chasing=true;
            chasing=true;
            SceneAudio.play(owner,com.coraline.registry.ModSounds.MICE_CHASE.get());
            playSound(com.coraline.registry.ModSounds.MOUSE_SQUEAK.get(),.8F,1.1F);
        }
        double horizontal=Math.pow(getX()-d.pos.getX()-.5,2)+Math.pow(getZ()-d.pos.getZ()-.5,2);
        if(horizontal<.49&&Math.abs(getY()-d.pos.getY())<1){OtherWorld.arrived((net.minecraft.server.level.ServerLevel)level(),d);discard();return;}
        if(horizontal<2.25){getNavigation().stop();getMoveControl().setWantedPosition(d.pos.getX()+.5,d.pos.getY(),d.pos.getZ()+.5,CoralineConfig.MOUSE_CHASE_SPEED.get());}
        else if(getNavigation().isDone()&&tickCount%5==0)getNavigation().moveTo(d.pos.getX()+.5,d.pos.getY(),d.pos.getZ()+.5,CoralineConfig.MOUSE_CHASE_SPEED.get());
        if(tickCount%70==0&&random.nextBoolean())playSound(com.coraline.registry.ModSounds.MOUSE_SQUEAK.get(),.45F,.9F+random.nextFloat()*.3F);
    }
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);if(doorId!=null)tag.putUUID("Door",doorId);tag.putBoolean("Spy",spying());tag.putBoolean("Chasing",chasing);tag.putLong("SpawnNight",spawnNight);tag.putInt("CircusStyle",circusStyle());}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);doorId=tag.hasUUID("Door")?tag.getUUID("Door"):null;spying(tag.getBoolean("Spy"));chasing=tag.getBoolean("Chasing");spawnNight=tag.contains("SpawnNight")?tag.getLong("SpawnNight"):Long.MIN_VALUE;if(tag.contains("CircusStyle")&&tag.getInt("CircusStyle")>=0)circusStyle(tag.getInt("CircusStyle"));}
}
