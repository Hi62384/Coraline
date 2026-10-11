package com.coraline.feast;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.network.syncher.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import java.util.UUID;

/** A few persistent, lightweight decorative props; never modifies copied blocks. */
public final class ScenePropEntity extends Entity {
    public static final int PIANO=0,TABLE=1,PLATE=2,GLASS=3,CHAIR=4,CAKE=5,GIFT=6,ACTIVITY_PORTAL=7,ACTIVITY_EXIT=8,RESCUE_DOOR=9;
    public static final int TARGET_STATIC=10,TARGET_SLIDER=11,TARGET_BOBBER=12,TARGET_POPUP=13,TARGET_SPINNER=14,TARGET_GOLD=15;
    public static final int CONTEST_CAKE=16;
    public static final int ARCHERY_PLATFORM=17;
    public static final int CAKE_MIXER=18;
    public static final int CAKE_WORKPIECE=19;
    public static final int PRIZE_PLATE=20;
    public static final int FEAST_CART=21;
    public static final int MAGIC_SPOT=22,MAGIC_CURTAIN=23,MAGIC_PRIZE=24,MAGIC_FINISH=25;
    public static final int GARDEN_MANTIS=26,GARDEN_PLANT=27;
    public static final int CAKE_KITCHEN=28;
    public static final int CARNIVAL_BALLOON=29,CARNIVAL_DUCK=30,CARNIVAL_WHEEL=31,CARNIVAL_STRIKER_METER=32,CARNIVAL_FOOD=33,CARNIVAL_GONDOLA=34,CARNIVAL_MOUNT=35,CARNIVAL_FERRIS_WHEEL=36,CARNIVAL_PEG=37;
    public static final int CIRCUS_CHOICE=38,CIRCUS_BET=39;
    public static final int FESTIVAL_CONSOLE=40,FESTIVAL_TIP=41;
    private static final EntityDataAccessor<Integer> KIND=SynchedEntityData.defineId(ScenePropEntity.class,EntityDataSerializers.INT),MODE=SynchedEntityData.defineId(ScenePropEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> TARGET_SCALE=SynchedEntityData.defineId(ScenePropEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CAKE_STYLE=SynchedEntityData.defineId(ScenePropEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> A=SynchedEntityData.defineId(ScenePropEntity.class,EntityDataSerializers.ITEM_STACK),B=SynchedEntityData.defineId(ScenePropEntity.class,EntityDataSerializers.ITEM_STACK),C=SynchedEntityData.defineId(ScenePropEntity.class,EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<CompoundTag> TREASURE=SynchedEntityData.defineId(ScenePropEntity.class,EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<CompoundTag> CAKE_DATA=SynchedEntityData.defineId(ScenePropEntity.class,EntityDataSerializers.COMPOUND_TAG);
    public UUID owner,storyDoor;
    public int slot;
    private int modeStartedAt;
    public int modeAge(){return tickCount-modeStartedAt;}
    public ScenePropEntity(EntityType<? extends ScenePropEntity> type,Level level){super(type,level);setNoGravity(true);getPersistentData().putBoolean("CoralineDecoration",true);}
    @Override protected void defineSynchedData(){entityData.define(KIND,0);entityData.define(MODE,0);entityData.define(TARGET_SCALE,1F);entityData.define(CAKE_STYLE,0);entityData.define(A,ItemStack.EMPTY);entityData.define(B,ItemStack.EMPTY);entityData.define(C,ItemStack.EMPTY);entityData.define(TREASURE,new CompoundTag());entityData.define(CAKE_DATA,new CompoundTag());}
    public void treasure(java.util.List<ItemStack> items){
        var tag=new CompoundTag();var list=new net.minecraft.nbt.ListTag();for(var item:items)list.add(item.save(new CompoundTag()));tag.put("Items",list);entityData.set(TREASURE,tag);
    }
    public java.util.List<ItemStack> treasure(){
        var result=new java.util.ArrayList<ItemStack>();for(var tag:entityData.get(TREASURE).getList("Items",10))result.add(ItemStack.of((CompoundTag)tag));return result;
    }
    public int kind(){return entityData.get(KIND);}public void kind(int n){entityData.set(KIND,n);refreshDimensions();}
    @Override public net.minecraft.world.phys.AABB getBoundingBoxForCulling(){
        return kind()==CARNIVAL_STRIKER_METER?new net.minecraft.world.phys.AABB(getX()-1.5,getY(),getZ()-.5,getX()+1.5,getY()+2,getZ()+.5):super.getBoundingBoxForCulling();
    }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){super.onSyncedDataUpdated(key);if(KIND.equals(key)||TARGET_SCALE.equals(key))refreshDimensions();if(MODE.equals(key))modeStartedAt=tickCount;}
    public float targetScale(){return entityData.get(TARGET_SCALE);}
    public int cakeStyle(){return entityData.get(CAKE_STYLE);}public void cakeStyle(int style){entityData.set(CAKE_STYLE,style);}
    public CompoundTag cakeData(){return entityData.get(CAKE_DATA);}public void cakeData(CompoundTag tag){entityData.set(CAKE_DATA,tag.copy());}
    public void targetScale(float scale){entityData.set(TARGET_SCALE,scale);refreshDimensions();}
    public int mode(){return entityData.get(MODE);}public void mode(int n){entityData.set(MODE,n);}
    public ItemStack food(int n){return entityData.get(n==0?A:n==1?B:C);}public void food(int n,ItemStack s){entityData.set(n==0?A:n==1?B:C,s.copy());}
@Override public EntityDimensions getDimensions(Pose pose){return switch(kind()){case FESTIVAL_CONSOLE->EntityDimensions.fixed(3.5F,1.85F);case CIRCUS_CHOICE,CIRCUS_BET->EntityDimensions.fixed(.85F,1.5F);case CARNIVAL_PEG->EntityDimensions.fixed(.42F,.9F);case CARNIVAL_FERRIS_WHEEL->EntityDimensions.fixed(36F,36F);case CARNIVAL_GONDOLA->EntityDimensions.fixed(2.2F,1.8F);case CARNIVAL_MOUNT->EntityDimensions.fixed(1.5F,2.0F);case CARNIVAL_BALLOON->EntityDimensions.fixed(.65F,1.05F);case CARNIVAL_DUCK->EntityDimensions.fixed(.72F,.72F);case CARNIVAL_WHEEL->EntityDimensions.fixed(4.7F,4.7F);case CAKE_KITCHEN->EntityDimensions.fixed(.46F,.5F);case RESCUE_DOOR->EntityDimensions.fixed(1F,1F);case ACTIVITY_PORTAL,ACTIVITY_EXIT->EntityDimensions.fixed(1.8F,3F);case MAGIC_CURTAIN->EntityDimensions.fixed(18F,9F);case GARDEN_MANTIS->EntityDimensions.fixed(3.4F,1.8F);case GARDEN_PLANT->EntityDimensions.fixed(.85F,1.2F);case MAGIC_SPOT->EntityDimensions.fixed(1.25F,1.65F);case MAGIC_PRIZE,MAGIC_FINISH->EntityDimensions.fixed(1.2F,1.5F);case GIFT->EntityDimensions.fixed(1.1F,.6F);case PIANO->EntityDimensions.fixed(2.8F,1.6F);case TABLE->EntityDimensions.fixed(3.6F,.85F);case CHAIR->EntityDimensions.fixed(.82F,2.1F);case CAKE->EntityDimensions.fixed(.9F,.5F);case CONTEST_CAKE->EntityDimensions.fixed(.9F,.9F);case FEAST_CART->EntityDimensions.fixed(1.6F,1.2F);case ARCHERY_PLATFORM->EntityDimensions.fixed(3F,.3F);case TARGET_GOLD,TARGET_STATIC,TARGET_SLIDER,TARGET_BOBBER,TARGET_POPUP,TARGET_SPINNER->EntityDimensions.fixed(1.1F*targetScale(),1.1F*targetScale());default->EntityDimensions.fixed(.58F,.22F);};}
    @Override public boolean isPickable(){return kind()!=CARNIVAL_FERRIS_WHEEL&&kind()!=CARNIVAL_FOOD&&kind()!=CARNIVAL_STRIKER_METER&&kind()!=GARDEN_MANTIS&&kind()!=MAGIC_CURTAIN&&kind()!=CAKE_MIXER&&kind()!=CAKE_WORKPIECE&&kind()!=FEAST_CART&&(kind()!=TARGET_POPUP||mode()!=0);}
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float damage){return false;}
    // Humanoid hips sit 0.75 blocks above their model origin; do not put their feet on the seat.
    @Override public double getPassengersRidingOffset(){return kind()==PIANO?-.25:kind()==ARCHERY_PLATFORM?.24:kind()==CARNIVAL_GONDOLA?.18:kind()==CARNIVAL_MOUNT?1.0:.38;}
    @Override protected boolean canAddPassenger(Entity passenger){return kind()==GARDEN_MANTIS?getPassengers().size()<2:super.canAddPassenger(passenger);}
    @Override protected void positionRider(Entity rider,MoveFunction move){
        if(kind()==PIANO){double angle=Math.toRadians(getYRot());move.accept(rider,getX()+Math.sin(angle)*.85,getY()-.25,getZ()-Math.cos(angle)*.85);rider.setYRot(getYRot());rider.setYBodyRot(getYRot());}
        else if(kind()==GARDEN_MANTIS){int seat=getPassengers().indexOf(rider);double angle=Math.toRadians(getYRot()),side=seat==0?-.62:.62;
            // Feet rest just above the skiff floor (renderer top: +0.85), while
            // the cockpit trim remains visible below a first-person eye line.
            move.accept(rider,getX()+Math.cos(angle)*side,getY()+.93,getZ()+Math.sin(angle)*side);
            if(rider instanceof com.coraline.parents.ParentEntity){rider.setYRot(getYRot());rider.setYBodyRot(getYRot());}}
        else if(kind()==ARCHERY_PLATFORM)move.accept(rider,getX(),getY()+.22,getZ());
        else {
            if(kind()==CHAIR&&rider instanceof com.coraline.parents.ParentEntity)move.accept(rider,getX(),getY()+.1,getZ());
            else super.positionRider(rider,move);
            // The chair controls every seated NPC's facing, including vanilla villagers.
            // Never overwrite a player's camera rotation.
            if(kind()==CHAIR&&rider instanceof LivingEntity living&&!(rider instanceof Player)){
                living.setYRot(getYRot());living.setYBodyRot(getYRot());living.setYHeadRot(getYRot());
            }
        }
    }
    @Override public void tick(){super.tick();if(!level().isClientSide){
        // Retire temporary rescue doors saved by older versions; recovery now goes to the quest.
        if(kind()==RESCUE_DOOR){discard();return;}
        if(LoopProps.owned(this)){if(!LoopProps.valid(this))discard();else if(com.coraline.feast.ArcheryActivity.isTarget(this))com.coraline.feast.ArcheryActivity.animate(this);return;}
        var scene=owner==null?null:VisitData.get(getServer()).scenes.get(owner);
        if(scene==null||scene.stage==VisitData.Stage.DONE||!com.coraline.otherworld.OtherWorld.isOther(level())){ejectPassengers();discard();return;}
        if(storyDoor==null){
            // Legacy unloaded props cannot attach themselves to a new family's
            // scene on another island just because the owner UUID is unchanged.
            if(distanceToSqr((kind()==PIANO?scene.piano:scene.table).getCenter())>64){ejectPassengers();discard();return;}
            storyDoor=scene.door;
        }
        if(!storyDoor.equals(scene.door)){ejectPassengers();discard();return;}
        if(kind()==PIANO){mode(scene.stage==VisitData.Stage.SONG?2:scene.stage==VisitData.Stage.FATHER_INTRO||scene.stage.ordinal()>VisitData.Stage.SONG.ordinal()?0:1);if(mode()==2)setYRot(getYRot()+2.5F);}
    }}
    @Override public InteractionResult interact(Player p,InteractionHand hand){
        if(level().isClientSide)return InteractionResult.SUCCESS;
        if(kind()==RESCUE_DOOR){discard();return InteractionResult.CONSUME;}
        if(p instanceof net.minecraft.server.level.ServerPlayer player&&owner!=null&&owner.equals(p.getUUID())&&hand==InteractionHand.MAIN_HAND)return LoopProps.owned(this)?LoopProps.interact(player,this):FirstVisit.interact(player,this);
        return InteractionResult.PASS;
    }
    @Override public InteractionResult interactAt(Player player,net.minecraft.world.phys.Vec3 hit,InteractionHand hand){
        // A backrest/armrest hit should seat the owner with the same first click as the seat.
        return kind()==CHAIR?interact(player,hand):super.interactAt(player,hit,hand);
    }
    @Override protected void addAdditionalSaveData(CompoundTag n){if(owner!=null)n.putUUID("Owner",owner);if(storyDoor!=null)n.putUUID("StoryDoor",storyDoor);n.putInt("Kind",kind());n.putInt("Slot",slot);n.putInt("Mode",mode());n.putFloat("TargetScale",targetScale());n.putInt("CakeStyle",cakeStyle());n.put("CakeData",cakeData());for(int i=0;i<3;i++)n.put("Food"+i,food(i).save(new CompoundTag()));}
    @Override protected void readAdditionalSaveData(CompoundTag n){owner=n.hasUUID("Owner")?n.getUUID("Owner"):null;storyDoor=n.hasUUID("StoryDoor")?n.getUUID("StoryDoor"):null;kind(n.getInt("Kind"));slot=n.getInt("Slot");mode(n.getInt("Mode"));targetScale(n.contains("TargetScale")?n.getFloat("TargetScale"):1F);cakeStyle(n.getInt("CakeStyle"));cakeData(n.getCompound("CakeData"));for(int i=0;i<3;i++)food(i,ItemStack.of(n.getCompound("Food"+i)));}
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
}
