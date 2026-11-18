package com.coraline.feast;

import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.*;

/** Lightweight synchronized stage furniture; audience rows have no mob AI. */
public final class CinematicPropEntity extends Entity {
    public static final int BUTTON_GIFT=11,PIANO_HAND=12;
    public static final int TAFFY_BOWL=78;
    public static final int STUNT_CARRIER=29;
    public static final int OCEAN_SET=30;
    public static final int PARENT_MIRROR=31,PARENT_DOLLS=32,RESCUE_MEAL=33,RESCUE_POT=34;
    public static final int WATER_BUCKET=35;
    public static final int CIRCUS_CLOCK=36;
    public static final int WORLD_SIGN=77;
    /** Shared world scenery, not an owned story actor/reward or another house. */
    public void worldSign(int variant){owner=null;door=null;serial=0;syncIdentity();entityData.set(KIND,WORLD_SIGN);entityData.set(SLOT,variant);getPersistentData().putBoolean("CoralineWorldLandmark",true);}
    public static final int STORY_CANE=13,ROACH_WARDROBE=14,BUG_CHAIR=15,BUG_COUCH=16,BUG_TABLE=17,BUG_LAMP=18,BUG_CHANDELIER=19,BEETLE_BOX=20,INSECT_PARLOR=21,PRISON_BED=22,GHOST_CHILD=23,PRISON_HEAD=24,STORY_MIRROR=25,GRAB_CARRIER=26,STORY_SHOE=27,VICTIM_SUPPER_TABLE=28;
    public static final int SEAT=0,CURTAIN=1,DOG_ROW=2,DIVING=3,TRAPEZE=4,TENT=5,FERRIS=6,CANNON=7,HOOPS=8,BEAMS=9,MOUSE_TRAPEZES=10;
    public static final RegistryObject<EntityType<CinematicPropEntity>> TYPE=com.coraline.registry.ModEntities.CINEMATIC_PROP;
    private static final EntityDataAccessor<Integer> KIND=SynchedEntityData.defineId(CinematicPropEntity.class,EntityDataSerializers.INT),SLOT=SynchedEntityData.defineId(CinematicPropEntity.class,EntityDataSerializers.INT),CUE=SynchedEntityData.defineId(CinematicPropEntity.class,EntityDataSerializers.INT),TIME=SynchedEntityData.defineId(CinematicPropEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> VALUE=SynchedEntityData.defineId(CinematicPropEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Optional<UUID>> OWNER_ID=SynchedEntityData.defineId(CinematicPropEntity.class,EntityDataSerializers.OPTIONAL_UUID),DOOR_ID=SynchedEntityData.defineId(CinematicPropEntity.class,EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> SERIAL=SynchedEntityData.defineId(CinematicPropEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> GRAB_X=SynchedEntityData.defineId(CinematicPropEntity.class,EntityDataSerializers.FLOAT),GRAB_Y=SynchedEntityData.defineId(CinematicPropEntity.class,EntityDataSerializers.FLOAT),GRAB_Z=SynchedEntityData.defineId(CinematicPropEntity.class,EntityDataSerializers.FLOAT);
    public void grabTarget(Vec3 target){entityData.set(GRAB_X,(float)target.x);entityData.set(GRAB_Y,(float)target.y);entityData.set(GRAB_Z,(float)target.z);}
    public Vec3 grabTarget(){return new Vec3(entityData.get(GRAB_X),entityData.get(GRAB_Y),entityData.get(GRAB_Z));}
    /** Shared board contact geometry: the actor and the flexible tip use the same clock. */
    public static double divingBoardOffset(double ticks,int side){
        double phase=ticks*Math.PI/15+(side>0?Math.PI/2:0);
        return Math.min(0,Math.sin(phase))*.32;
    }
    public static double divingJumpHeight(double ticks,int side){
        double phase=ticks*Math.PI/15+(side>0?Math.PI/2:0);
        return Math.max(0,Math.sin(phase))*2.2;
    }
    public UUID owner,door;public int serial;
    private double lerpX,lerpY,lerpZ;private float lerpYaw,lerpPitch;private int lerpSteps;
    private long visualClockReceived;
    public double visualTime(float partial){return time()+(level().isClientSide?Math.max(0,Math.min(5,level().getGameTime()-visualClockReceived+partial)):0);}
    @Override public void lerpTo(double x,double y,double z,float yaw,float pitch,int steps,boolean teleport){if(kind()==PARENT_MIRROR)setYRot(yaw);boolean smooth=kind()==STUNT_CARRIER||kind()==GRAB_CARRIER||kind()==RescueProps.RAT_BALL||kind()==RescueProps.MANTIS;if(!smooth||teleport){super.lerpTo(x,y,z,yaw,pitch,steps,teleport);return;}lerpX=x;lerpY=y;lerpZ=z;lerpYaw=yaw;lerpPitch=pitch;lerpSteps=Math.max(1,Math.min(3,steps));}
    public CinematicPropEntity(EntityType<? extends CinematicPropEntity> type,Level level){super(type,level);visualClockReceived=level.getGameTime();setNoGravity(true);}
    @Override protected void defineSynchedData(){entityData.define(KIND,SEAT);entityData.define(SLOT,0);entityData.define(CUE,0);entityData.define(TIME,0);entityData.define(VALUE,0F);entityData.define(GRAB_X,0F);entityData.define(GRAB_Y,0F);entityData.define(GRAB_Z,0F);entityData.define(OWNER_ID,Optional.empty());entityData.define(DOOR_ID,Optional.empty());entityData.define(SERIAL,0);}
    public int kind(){return entityData.get(KIND);}public int slot(){return entityData.get(SLOT);}public int cue(){return entityData.get(CUE);}public int time(){return entityData.get(TIME);}public float value(){return entityData.get(VALUE);}public UUID owner(){return owner;}
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){super.onSyncedDataUpdated(key);if(KIND.equals(key))setBoundingBox(makeBoundingBox());if(TIME.equals(key)&&level().isClientSide)visualClockReceived=level().getGameTime();if(OWNER_ID.equals(key))owner=entityData.get(OWNER_ID).orElse(null);if(DOOR_ID.equals(key))door=entityData.get(DOOR_ID).orElse(null);if(SERIAL.equals(key))serial=entityData.get(SERIAL);}
    private void syncIdentity(){entityData.set(OWNER_ID,Optional.ofNullable(owner));entityData.set(DOOR_ID,Optional.ofNullable(door));entityData.set(SERIAL,serial);}
    public void configure(VisitData.Scene s,int kind,int slot){owner=s.owner;door=s.door;serial=s.loop.event.getInt("Serial");syncIdentity();getPersistentData().putBoolean("CoralineDecoration",true);entityData.set(KIND,kind);entityData.set(SLOT,slot);}
    public void frame(int cue,int time,float value){entityData.set(CUE,cue);entityData.set(TIME,time);entityData.set(VALUE,value);}
    public boolean canonicalGhost(VisitData.Scene s){if(s==null||kind()!=GHOST_CHILD||slot()<0||slot()>2||!Objects.equals(owner,s.owner)||!Objects.equals(door,s.door))return false;return getUUID().equals(RescueEpilogue.savedGhostReference(FirstEscapeStory.data(s),FirstEscapeScene.context(s),slot()));}
    private boolean retiredCanonicalGhost(){
        if(level().isClientSide||owner==null||kind()!=GHOST_CHILD||slot()<0||slot()>2)return false;
        var scene=VisitData.get(getServer()).scenes.get(owner);if(scene==null||!Objects.equals(door,scene.door)||canonicalGhost(scene))return false;
        var retired=scene.loop.story.getCompound("RetiredCanonicalGhosts");String key=getUUID().toString();
        if(!retired.contains(key,Tag.TAG_INT)||retired.getInt(key)!=slot())return false;
        discard();return true;
    }
    boolean boundTo(VisitData.Scene s){return s!=null&&Objects.equals(owner,s.owner)&&Objects.equals(door,s.door);}
    public boolean owned(VisitData.Scene s){return s!=null&&Objects.equals(owner,s.owner)&&Objects.equals(door,s.door)&&(serial==s.loop.event.getInt("Serial")||canonicalGhost(s));}
    private boolean retireFinishedStoryProp(){
        if(level().isClientSide||owner==null)return false;var s=VisitData.get(getServer()).scenes.get(owner);
        if(RescueEncounters.retiredRatBall(this,s)&&getPassengers().stream().noneMatch(passenger->passenger instanceof ServerPlayer)){
            ejectPassengers();
            discard();
            return true;
        }
        if(!owned(s)||!FirstEscapeStory.retiredGrab(this,s)&&!MissingParentsCat.retiredDolls(this,s)&&!MissingParentsCat.retiredMirror(this,s)&&!GhostVictimWorld.retiredSupperTable(this,s))return false;
        ejectPassengers();discard();return true;
    }
    @Override public void tick(){super.tick();if(!level().isClientSide&&(kind()==TAFFY_BOWL||kind()==RescueProps.TONGS))PalaceTransport.migrateMover(this);if(retiredCanonicalGhost()||retireFinishedStoryProp())return;if(com.coraline.rebirth.StoryRebirth.kind(kind())){if(!level().isClientSide&&tickCount%40==0)com.coraline.rebirth.StoryRebirth.reconcileProp(this);return;}if(level().isClientSide&&lerpSteps>0){setPos(getX()+(lerpX-getX())/lerpSteps,getY()+(lerpY-getY())/lerpSteps,getZ()+(lerpZ-getZ())/lerpSteps);setYRot(net.minecraft.util.Mth.rotLerp(1F/lerpSteps,getYRot(),lerpYaw));setXRot(getXRot()+(lerpPitch-getXRot())/lerpSteps);lerpSteps--;}if(!level().isClientSide&&owner!=null){var scene=VisitData.get(getServer()).scenes.get(owner);if(scene==null||!owned(scene)||kind()==TAFFY_BOWL&&!MissingParentsTea.active(scene)||kind()>=STORY_CANE&&kind()<=STORY_SHOE&&!canonicalGhost(scene)&&!AcceptedButtonEyes.active(scene)&&!ParentRescueStory.started(scene)&&(!FirstEscapeStory.engaged(scene)||FirstEscapeStory.phase(scene)==FirstEscapeTimeline.Phase.ESCAPE_COMPLETE)){ejectPassengers();discard();return;}if(kind()==RescueProps.RAT_BALL&&isVehicle()){setNoGravity(false);noPhysics=false;setMaxUpStep(1);var v=getDeltaMovement();v=new Vec3(v.x,Math.max(-.5,v.y-.08),v.z);moveRatBall(v);setDeltaMovement(v.x*.85,onGround()?0:v.y,v.z*.85);fallDistance=0;}}}
    @Override public boolean isPickable(){if(com.coraline.rebirth.StoryRebirth.kind(kind()))return !isInvisible()&&kind()!=com.coraline.rebirth.StoryRebirth.CIRCLE;if(isInvisible()&&(kind()==RescueProps.HAND||kind()==RescueProps.KEY_BUNDLE||kind()>=RescueProps.TONGS&&kind()<=RescueProps.PHOTO))return false;return RescueProps.kind(kind())||kind()==PARENT_MIRROR||kind()==SEAT||kind()==TENT||kind()==FERRIS||kind()==CANNON||kind()==BUTTON_GIFT||kind()==STORY_CANE||kind()==ROACH_WARDROBE&&!(cue()==3&&value()>.8F)||kind()==STORY_SHOE;}
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){return false;}
    /** Vanilla stepping may leave a custom mounted box overlapping a kerb.
     * Validate the complete assembly and perform a bounded, collision-tested step. */
    private void moveRatBall(Vec3 velocity){
        var before=position();move(MoverType.SELF,velocity);
        if(!level().getBlockCollisions(this,getBoundingBox().deflate(.001)).iterator().hasNext())return;
        setPos(before);var original=getBoundingBox();
        for(double rise:new double[]{.0625,.5,1.0}){
            if(level().getBlockCollisions(this,original.expandTowards(0,rise,0).deflate(.001)).iterator().hasNext())continue;
            var raised=original.move(0,rise,0);var destination=raised.move(velocity.x,0,velocity.z);
            if(level().getBlockCollisions(this,raised.expandTowards(velocity.x,0,velocity.z).deflate(.001)).iterator().hasNext())continue;
            if(!level().getBlockCollisions(this,destination.move(0,-.07,0)).iterator().hasNext())continue;
            move(MoverType.SELF,new Vec3(0,rise,0));move(MoverType.SELF,new Vec3(velocity.x,0,velocity.z));return;
        }
        setDeltaMovement(Vec3.ZERO);
    }
    @Override public InteractionResult interactAt(Player player,Vec3 hit,InteractionHand hand){if(player instanceof ServerPlayer p&&(com.coraline.rebirth.StoryRebirth.interact(p,this)||PalaceWellEncounter.interact(p,this)||FirstEscapeStory.interact(p,this)))return InteractionResult.CONSUME;return interact(player,hand);}
    @Override public boolean canBeCollidedWith(){return kind()==PalaceTransport.TRUCK||kind()==ROACH_WARDROBE&&!(cue()==3&&value()>.8F);}
    @Override protected AABB makeBoundingBox(){
        if(entityData!=null&&entityData.hasItem(KIND)&&com.coraline.rebirth.StoryRebirth.kind(kind())){
            if(kind()==com.coraline.rebirth.StoryRebirth.PORTRAIT)return new AABB(getX()-.68,getY(),getZ()-.15,getX()+.68,getY()+1.45,getZ()+.15);
            return new AABB(getX()-.18,getY(),getZ()-.18,getX()+.18,getY()+.8,getZ()+.18);
        }
        if(entityData!=null&&entityData.hasItem(KIND)&&RescueProps.kind(kind())){
            if(kind()==RescueProps.TONGS){
                double yaw=Math.toRadians(getYRot());
                double x=.19*Math.abs(Math.cos(yaw))+.67*Math.abs(Math.sin(yaw));
                double z=.67*Math.abs(Math.cos(yaw))+.19*Math.abs(Math.sin(yaw));
                return new AABB(getX()-x,getY()-.10,getZ()-z,getX()+x,getY()+.08,getZ()+z);
            }
            if(kind()==RescueProps.RAT_BALL)return new AABB(getX()-.30,getY(),getZ()-.30,getX()+.30,getY()+1.35,getZ()+.30);
            double radius=kind()==RescueProps.MANTIS?2.2:kind()==RescueProps.SWARM?1.3:kind()==RescueProps.WRAPPER?1.6:.5;
            return new AABB(getX()-radius,getY()-.15,getZ()-radius,getX()+radius,getY()+(kind()==RescueProps.MANTIS?3.8:1.2),getZ()+radius);
        }
        if(entityData!=null&&entityData.hasItem(KIND)){
            if(kind()==WORLD_SIGN)return new AABB(getX()-4,getY()-.3,getZ()-.3,getX()+4,getY()+7.4,getZ()+.3);
            if(kind()==PalaceTransport.TRUCK)return new AABB(getX()-2.0,getY(),getZ()-4.8,getX()+2.0,getY()+3.8,getZ()+4.7);
            if(kind()==PalaceTransport.MOVER)return new AABB(getX()-.3,getY(),getZ()-.3,getX()+.3,getY()+2.05,getZ()+.3);
            if(kind()==PalaceTransport.CARRIER)return new AABB(getX()-.15,getY(),getZ()-.15,getX()+.15,getY()+.3,getZ()+.15);
            if(kind()==ROACH_WARDROBE){boolean acrossX=Math.abs(Math.sin(Math.toRadians(getYRot())))>.5;double x=acrossX?1.2:.65,z=acrossX?.65:1.2;return new AABB(getX()-x,getY(),getZ()-z,getX()+x,getY()+3.3,getZ()+z);}
            if(kind()==PARENT_MIRROR){boolean acrossZ=Math.abs(Math.sin(Math.toRadians(getYRot())))>.5;double x=acrossZ?.25:1.2,z=acrossZ?1.2:.25;return new AABB(getX()-x,getY(),getZ()-z,getX()+x,getY()+2.95,getZ()+z);}
        }
        return super.makeBoundingBox();
    }
    @Override public boolean shouldRiderSit(){return kind()!=STUNT_CARRIER&&kind()!=GRAB_CARRIER&&kind()!=PalaceTransport.TRUCK&&kind()!=PalaceTransport.CARRIER;}
    @Override public boolean isPushable(){return false;}
    @Override public double getPassengersRidingOffset(){return kind()==RescueProps.RAT_BALL?.45:kind()==STUNT_CARRIER?0:kind()==BUG_COUCH?.02:kind()==BUG_CHAIR?.5:kind()==GRAB_CARRIER&&cue()==2?0:.25;}
    @Override protected void positionRider(Entity rider,MoveFunction move){
        if(kind()==GRAB_CARRIER&&cue()==2){move.accept(rider,getX(),getY(),getZ());return;}
        if(kind()==BUG_CHAIR){double a=Math.toRadians(getYRot());move.accept(rider,getX()+Math.sin(a)*.60,getY()+getPassengersRidingOffset()+rider.getMyRidingOffset(),getZ()-Math.cos(a)*.60);return;}
        if(kind()==PalaceTransport.TRUCK){move.accept(rider,getX(),getY()+.88,getZ()+2);return;}
        if(kind()==PalaceTransport.CARRIER){move.accept(rider,getX(),getY()+1,getZ());return;}
        if(kind()==BUG_COUCH){double angle=Math.toRadians(getYRot());move.accept(rider,getX()+Math.sin(angle)*.12,getY()+.02,getZ()-Math.cos(angle)*.12);}
        else super.positionRider(rider,move);if((kind()==SEAT||kind()==BUG_COUCH)&&rider instanceof net.minecraft.world.entity.LivingEntity living&&!(rider instanceof net.minecraft.world.entity.player.Player)){float facing=kind()==BUG_COUCH?getYRot()+180:180;living.setYRot(facing);living.setYBodyRot(facing);if(kind()==SEAT)living.setYHeadRot(facing);}
    }
    @Override public Vec3 getDismountLocationForPassenger(net.minecraft.world.entity.LivingEntity passenger){if(passenger instanceof ServerPlayer player){var exit=ParentRescueStory.kitchenDismount(player,this);if(exit!=null)return exit;}return kind()==PalaceTransport.TRUCK?PalaceTransport.exit(this,passenger):super.getDismountLocationForPassenger(passenger);}
    @Override protected boolean canAddPassenger(Entity entity){if(kind()==RescueProps.RAT_BALL)return getPassengers().isEmpty()&&entity instanceof com.coraline.otherworld.MouseEntity&&entity.getPersistentData().hasUUID("RescueOwner")&&Objects.equals(owner,entity.getPersistentData().getUUID("RescueOwner"));return (kind()==SEAT||kind()==STUNT_CARRIER||kind()==BUG_CHAIR||kind()==BUG_COUCH||kind()==GRAB_CARRIER||kind()==PalaceTransport.TRUCK||kind()==PalaceTransport.CARRIER)&&getPassengers().isEmpty()&&(entity instanceof ServerPlayer p?Objects.equals(p.getUUID(),owner)&&slot()==0:slot()!=0);}
    @Override public InteractionResult interact(Player actor,InteractionHand hand){if(actor instanceof ServerPlayer player&&MissingParentsCat.interact(player,this))return InteractionResult.CONSUME;if(level().isClientSide)return InteractionResult.SUCCESS;if(hand!=InteractionHand.MAIN_HAND||!(actor instanceof ServerPlayer p)||!Objects.equals(owner,p.getUUID())||distanceToSqr(p)>16)return InteractionResult.PASS;var s=OtherMotherLoop.scene(p);if(s==null||!owned(s))return InteractionResult.PASS;if(RescueChapter.interact(p,this))return InteractionResult.CONSUME;if(AcceptedButtonEyes.interact(p,s,this))return InteractionResult.CONSUME;if(BlackIsTraditional.interact(p,s,this))return InteractionResult.CONSUME;if(kind()==SEAT&&slot()==0&&PlayerParty.active(p)==s&&TheatreSite.inside(p,s)&&PlayerParty.allowed(p,position())&&!s.loop.secrets.searching()&&!com.coraline.secrets.Reconciliation.active(p)&&!com.coraline.secrets.SecretDialogue.busy(p)){return !isVehicle()&&p.startRiding(this,true)?InteractionResult.CONSUME:InteractionResult.PASS;}return TheatreVisit.interact(p,s,this)||MouseCircusVisit.interact(p,s,this)?InteractionResult.CONSUME:InteractionResult.PASS;}
    @Override public AABB getBoundingBoxForCulling(){return switch(kind()){case RescueProps.WEB->new AABB(getX()-35,getY()-2,getZ()-35,getX()+35,getY()+22,getZ()+35);case RescueProps.SWARM->getBoundingBox().inflate(12);case RescueProps.MANTIS->getBoundingBox().inflate(4);case INSECT_PARLOR->getBoundingBox().inflate(10);case OCEAN_SET->new AABB(getX()-25,getY()-1,getZ()-12,getX()+25,getY()+20,getZ()+12);case CURTAIN->new AABB(getX()-25,getY()-1,getZ()-4,getX()+25,getY()+26,getZ()+4);case DOG_ROW->new AABB(getX()-26,getY()-1,getZ()-2,getX()+26,getY()+3,getZ()+2);case DIVING,TRAPEZE,HOOPS,BEAMS,MOUSE_TRAPEZES->new AABB(getX()-20,getY()-1,getZ()-20,getX()+20,getY()+40,getZ()+20);default->getBoundingBox().inflate(kind()==TENT?4:3);};}
    @Override public boolean shouldRenderAtSqrDistance(double distance){return distance<192*192;}
    @Override protected void addAdditionalSaveData(CompoundTag n){if(owner!=null)n.putUUID("Owner",owner);if(door!=null)n.putUUID("Door",door);n.putInt("Serial",serial);n.putInt("Kind",kind());n.putInt("Slot",slot());n.putInt("Cue",cue());n.putInt("Time",time());n.putFloat("Value",value());}
    @Override protected void readAdditionalSaveData(CompoundTag n){owner=n.hasUUID("Owner")?n.getUUID("Owner"):null;door=n.hasUUID("Door")?n.getUUID("Door"):null;serial=n.getInt("Serial");syncIdentity();entityData.set(KIND,n.getInt("Kind"));entityData.set(SLOT,n.getInt("Slot"));frame(n.getInt("Cue"),n.getInt("Time"),n.getFloat("Value"));}
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
}
