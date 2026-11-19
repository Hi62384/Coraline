package com.coraline.feast;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import java.util.UUID;

/** A real passenger vehicle, with the same bounded server physics for both riders. */
public final class WybieBicycleEntity extends Entity {
    private static final EntityDataAccessor<Float> STEER=SynchedEntityData.defineId(WybieBicycleEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SPEED=SynchedEntityData.defineId(WybieBicycleEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> FRIEND=SynchedEntityData.defineId(WybieBicycleEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<java.util.Optional<UUID>> SESSION=SynchedEntityData.defineId(WybieBicycleEntity.class,EntityDataSerializers.OPTIONAL_UUID);
    private UUID owner,door,rider;private int serial,inputAt=-100;private float throttle,steering;private boolean brake=true;
    private double speed;private int lerpSteps;private double lerpX,lerpY,lerpZ;private float lerpYaw,lerpPitch;
    long storyDrivenAt=Long.MIN_VALUE;
    public float wheelRotation,previousWheelRotation;public int hardCollisionAt=-100,stuckTicks,navigationBlockedTicks,navigationRetryAt,lastNavigationTick;
    public WybieBicycleEntity(EntityType<? extends WybieBicycleEntity> type,Level level){super(type,level);setMaxUpStep(.6F);}
    @Override protected void defineSynchedData(){entityData.define(STEER,0F);entityData.define(SPEED,0F);entityData.define(FRIEND,false);entityData.define(SESSION,java.util.Optional.empty());}
    public void configure(UUID owner,UUID door,UUID session,int serial,UUID rider,boolean friend){this.owner=owner;this.door=door;this.serial=serial;this.rider=rider;entityData.set(SESSION,java.util.Optional.of(session));entityData.set(FRIEND,friend);}
    /**
     * Re-authorize the same well-story bicycle after the owner's saved
     * encounter session rotates on reconnect. No new bike or rider is created.
     */
    public boolean resumeWellStory(UUID expectedOwner,UUID expectedDoor,UUID expectedWybie,UUID nextSession){
        var tag=getPersistentData();
        var record=new com.coraline.palace.PalaceWellResumeRules.Bicycle(
                owner,door,rider,session(),
                tag.hasUUID("PalaceWellOwner")?tag.getUUID("PalaceWellOwner"):null,friend());
        if(!com.coraline.palace.PalaceWellResumeRules.canRebind(
                record,expectedOwner,expectedDoor,expectedWybie,nextSession))return false;
        entityData.set(SESSION,java.util.Optional.of(nextSession));
        return true;
    }
    public float visualSteering(){return entityData.get(STEER);}
    public UUID session(){return entityData.get(SESSION).orElse(null);}public boolean friend(){return entityData.get(FRIEND);}public double speed(){return level().isClientSide?entityData.get(SPEED):speed;}
    public boolean ownedBy(ServerPlayer p,VisitData.Scene s){return p.getUUID().equals(owner)&&s.door.equals(door)&&serial==s.loop.event.getInt("Serial")&&session()!=null&&session().equals(WybieAdventure.data(s).getUUID("Session"));}
    public void input(ServerPlayer p,UUID session,float forward,float turn,boolean braking){
        if(!p.getUUID().equals(owner)||!p.getUUID().equals(rider)||friend()||p.getVehicle()!=this||!java.util.Objects.equals(session,session())||inputAt==tickCount||!Float.isFinite(forward)||!Float.isFinite(turn))return;
        drive(forward,turn,braking);
    }
    public void drive(float forward,float turn,boolean braking){throttle=Mth.clamp(forward,0,1);steering=Mth.clamp(turn,-1,1);brake=braking;inputAt=tickCount;}
    public void stop(){drive(0,0,true);if(FirstEscapeStory.storyBike(this)||getPersistentData().hasUUID("PalaceWellOwner")){speed=0;entityData.set(SPEED,0F);setDeltaMovement(Vec3.ZERO);}}
    void storySettle(Vec3 feet){stop();setPos(feet);setBoundingBox(makeBoundingBox());setOnGround(true);}
    boolean storyAdvance(Vec3 target){
        var delta=target.subtract(position());float heading=StoryBikeRoute.yaw(delta);double distance=delta.horizontalDistance();
        var next=distance<.001?position():position().add(delta.x/distance*Math.min(.72,distance),0,delta.z/distance*Math.min(.72,distance));
        next=StoryBikeRoute.step(this,position(),next.x,next.z,heading);
        if(next==null){speed=0;entityData.set(SPEED,0F);setDeltaMovement(Vec3.ZERO);return false;}
        var before=position();setYRot(heading);setPos(next);setBoundingBox(makeBoundingBox());setOnGround(true);setDeltaMovement(next.subtract(before));speed=next.subtract(before).horizontalDistance();
        wheelRotation+=(float)(speed/.34);entityData.set(SPEED,(float)speed);entityData.set(STEER,0F);
        return speed>.001;
    }
    final java.util.Deque<Vec3> wellRoute=new java.util.ArrayDeque<>();
    long wellReplanAt;
    int wellStallTicks;
    @Override public boolean isControlledByLocalInstance(){return false;}
    @Override protected AABB makeBoundingBox(){double angle=Math.toRadians(getYRot()),hx=Math.abs(Math.cos(angle))*.45+Math.abs(Math.sin(angle))*1.03,hz=Math.abs(Math.sin(angle))*.45+Math.abs(Math.cos(angle))*1.03;return new AABB(getX()-hx,getY(),getZ()-hz,getX()+hx,getY()+1.15,getZ()+hz);}
    @Override public boolean isPickable(){return true;}
    @Override public boolean isPushable(){return false;}
    @Override public boolean canBeCollidedWith(){return true;}
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){return false;}
    @Override protected boolean canAddPassenger(Entity passenger){return getPassengers().isEmpty()&&rider!=null&&rider.equals(passenger.getUUID());}
    @Override public InteractionResult interact(Player player,InteractionHand hand){
        if(level().isClientSide)return InteractionResult.SUCCESS;
        if(!(player instanceof ServerPlayer p)||hand!=InteractionHand.MAIN_HAND||friend()||!p.getUUID().equals(owner)||!p.getUUID().equals(rider)||p.distanceToSqr(this)>9)return InteractionResult.PASS;
        var s=WybieAdventure.active(p);if(s==null||!ownedBy(p,s)||!WybieAdventureLedger.timed(WybieAdventure.data(s))||WybieAdventureLedger.mode(WybieAdventure.data(s))==WybieAdventureLedger.Mode.ENDING)return InteractionResult.PASS;
        return p.startRiding(this)?InteractionResult.CONSUME:InteractionResult.PASS;
    }
    @Override public double getPassengersRidingOffset(){return .28;}
    @Override protected void positionRider(Entity passenger,MoveFunction move){
        if(!hasPassenger(passenger))return;move.accept(passenger,getX(),getY()+getPassengersRidingOffset()+passenger.getMyRidingOffset(),getZ());
        // Never rotate the player's camera. NPC hips/head face their handlebars.
        if(passenger instanceof LivingEntity living&&!(passenger instanceof Player)){living.setYRot(getYRot());living.setYBodyRot(getYRot());living.setYHeadRot(getYRot());}
    }
    @Override public Vec3 getDismountLocationForPassenger(LivingEntity passenger){
        // Search both sides, then front/back and diagonals. A floor ray also handles
        // the course's half slabs and shallow changes in grade without floating exits.
        double yaw=Math.toRadians(getYRot());
        for(Pose pose:new Pose[]{Pose.STANDING,Pose.CROUCHING})for(double radius:new double[]{1.2,2.0,3.0})for(int turn:new int[]{0,4,2,6,1,7,3,5}){
            double angle=yaw+turn*Math.PI/4;Vec3 column=position().add(Math.cos(angle)*radius,0,Math.sin(angle)*radius);
            Vec3 at=dismountAt(passenger,pose,column);if(at!=null){passenger.setPose(pose);return at;}
        }
        // If every nearby exit is enclosed, retain vanilla's emergency dismount;
        // never deny Shift or teleport the rider to a distant, unverified location.
        return super.getDismountLocationForPassenger(passenger);
    }
    private Vec3 dismountAt(LivingEntity passenger,Pose pose,Vec3 column){
        if(!level().hasChunkAt(BlockPos.containing(column)))return null;
        var floor=level().clip(new ClipContext(column.add(0,1.25,0),column.add(0,-1.5,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,passenger));
        if(floor.getType()!=HitResult.Type.BLOCK||floor.getDirection()!=Direction.UP)return null;
        Vec3 at=new Vec3(column.x,floor.getLocation().y,column.z);var box=passenger.getDimensions(pose).makeBoundingBox(at);
        var low=BlockPos.containing(box.minX,box.minY,box.minZ);var high=BlockPos.containing(box.maxX,box.maxY,box.maxZ);
        for(int x:new int[]{low.getX(),high.getX()})for(int z:new int[]{low.getZ(),high.getZ()}){
            var edge=new BlockPos(x,low.getY(),z);if(!level().hasChunkAt(edge)||!level().getWorldBorder().isWithinBounds(edge))return null;
        }
        var support=level().getBlockState(floor.getBlockPos());
        if(!support.getFluidState().isEmpty()||support.is(Blocks.MAGMA_BLOCK)||support.is(Blocks.CACTUS)||support.is(Blocks.CAMPFIRE)||support.is(Blocks.SOUL_CAMPFIRE))return null;
        if(!level().noCollision(passenger,box)||level().containsAnyLiquid(box))return null;
        // A clear tile on the far side of a wall is not an exit from this bicycle.
        var path=level().clip(new ClipContext(position().add(0,.9,0),at.add(0,.9,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,passenger));if(path.getType()!=HitResult.Type.MISS)return null;
        for(var pos:BlockPos.betweenClosed(low,high)){var state=level().getBlockState(pos);if(state.is(Blocks.FIRE)||state.is(Blocks.SOUL_FIRE)||state.is(Blocks.SWEET_BERRY_BUSH)||state.is(Blocks.WITHER_ROSE)||state.is(Blocks.POWDER_SNOW))return null;}
        return at;
    }
    @Override public void tick(){
        super.tick();previousWheelRotation=wheelRotation;
        if(level().isClientSide){
            if(lerpSteps>0){setPos(getX()+(lerpX-getX())/lerpSteps,getY()+(lerpY-getY())/lerpSteps,getZ()+(lerpZ-getZ())/lerpSteps);setYRot(getYRot()+Mth.wrapDegrees(lerpYaw-getYRot())/lerpSteps);setXRot(getXRot()+(lerpPitch-getXRot())/lerpSteps);lerpSteps--;}
            wheelRotation+=(float)(speed()/ .34);return;
        }
        boolean wellStory=getPersistentData().hasUUID("PalaceWellOwner");
        boolean escapeStory=FirstEscapeStory.storyBike(this);
        if(escapeStory)FirstEscapeStory.driveStoryBike(this);
        var scene=owner==null?null:VisitData.get(getServer()).scenes.get(owner);
        if(scene==null||!java.util.Objects.equals(door,scene.door)||!escapeStory&&!wellStory&&(!scene.loop.event.getBoolean("WybiePending")||serial!=scene.loop.event.getInt("Serial")||session()==null||!session().equals(WybieAdventure.data(scene).getUUID("Session")))){ejectPassengers();discard();return;}
        if(escapeStory)return; // Exact collision-checked route stepping above; never walker steering.
        var n=wellStory?scene.loop.story.getCompound("PinkPalaceJourney").getCompound("WellStage"):WybieAdventure.data(scene);String key=wellStory?"Bike":friend()?"FriendBike":"PlayerBike";
        if(!escapeStory&&(!n.hasUUID(key)||!getUUID().equals(n.getUUID(key)))){ejectPassengers();discard();return;}
        n.putLong(key+"Position",blockPosition().asLong());
        if(wellStory){PalaceWellEncounter.driveBike(this);return;}
        var player=getServer().getPlayerList().getPlayer(owner);
        if(player==null||!player.isAlive()||player.level()!=level()){ejectPassengers();throttle=0;brake=true;}
        if(player==null||!player.isAlive()||player.level()!=level()||!escapeStory&&!wellStory&&!WybieAdventureLedger.timed(n)||tickCount-inputAt>10||!isVehicle()){throttle=0;steering=0;brake=true;}
        var motion=escapeStory?WybieBicyclePhysics.departure(speed,getYRot(),throttle,steering,brake):WybieBicyclePhysics.step(speed,getYRot(),throttle,steering,brake);speed=motion.speed();setYRot(motion.yaw());setBoundingBox(makeBoundingBox());
        if(isInWaterOrBubble()){speed*=.65;throttle=0;}
        double radians=Math.toRadians(getYRot()),oldX=getX(),oldZ=getZ(),vertical=getDeltaMovement().y;
        if(!isNoGravity())vertical=Math.max(-.8,vertical-.08);
        setDeltaMovement(-Math.sin(radians)*speed,vertical,Math.cos(radians)*speed);move(MoverType.SELF,getDeltaMovement());
        if(onGround())setDeltaMovement(getDeltaMovement().multiply(1,0,1));
        double moved=Math.hypot(getX()-oldX,getZ()-oldZ);
        if(horizontalCollision){if(speed>.19&&moved<speed*.3)hardCollisionAt=tickCount;speed*=.25;}
        stuckTicks=throttle>.1&&speed>.025&&moved<.008?stuckTicks+1:0;
        wheelRotation+=(float)(moved/.34);entityData.set(SPEED,(float)speed);entityData.set(STEER,steering);
        // A bicycle cannot drown/softlock a rider; dismounting remains vanilla Shift.
        if(isInWaterOrBubble()&&isVehicle())ejectPassengers();
    }
    @Override public void lerpTo(double x,double y,double z,float yaw,float pitch,int steps,boolean teleport){lerpX=x;lerpY=y;lerpZ=z;lerpYaw=yaw;lerpPitch=pitch;lerpSteps=Math.max(1,Math.min(5,steps));}
    @Override protected void addAdditionalSaveData(CompoundTag n){if(owner!=null)n.putUUID("Owner",owner);if(door!=null)n.putUUID("Door",door);if(rider!=null)n.putUUID("Rider",rider);if(session()!=null)n.putUUID("Session",session());n.putInt("Serial",serial);n.putBoolean("Friend",friend());}
    @Override protected void readAdditionalSaveData(CompoundTag n){owner=n.hasUUID("Owner")?n.getUUID("Owner"):null;door=n.hasUUID("Door")?n.getUUID("Door"):null;rider=n.hasUUID("Rider")?n.getUUID("Rider"):null;serial=n.getInt("Serial");entityData.set(FRIEND,n.getBoolean("Friend"));entityData.set(SESSION,n.hasUUID("Session")?java.util.Optional.of(n.getUUID("Session")):java.util.Optional.empty());speed=0;}
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
}
